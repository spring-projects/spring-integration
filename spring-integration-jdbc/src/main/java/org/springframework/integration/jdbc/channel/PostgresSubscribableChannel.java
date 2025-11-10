/*
 * Copyright 2022-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.jdbc.channel;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jspecify.annotations.Nullable;

import org.springframework.core.log.LogAccessor;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.core.retry.Retryable;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.integration.channel.AbstractSubscribableChannel;
import org.springframework.integration.dispatcher.MessageDispatcher;
import org.springframework.integration.dispatcher.UnicastingDispatcher;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;
import org.springframework.util.ErrorHandler;
import org.springframework.util.ReflectionUtils;

/**
 * An {@link AbstractSubscribableChannel} for receiving push notifications for
 * messages send to a group id of a {@link JdbcChannelMessageStore}. Receiving
 * such push notifications is only possible if using a Postgres database.
 * <p/>
 * In order to function, the Postgres database that is used must define a trigger
 * for sending notifications upon newly arrived messages. This trigger is defined
 * in the <i>schema-postgresql.sql</i> file within this artifact but commented
 * out.
 *
 * @author Rafael Winterhalter
 * @author Artem Bilan
 * @author Igor Lovich
 * @author Norbert Schneider
 *
 * @since 6.0
 */
public class PostgresSubscribableChannel extends AbstractSubscribableChannel
		implements PostgresChannelMessageTableSubscriber.Subscription {

	private static final LogAccessor LOGGER = new LogAccessor(PostgresSubscribableChannel.class);

	private static final Optional<?> FALLBACK_STUB = Optional.of(new Object());

	private final JdbcChannelMessageStore jdbcChannelMessageStore;

	private final Object groupId;

	private final PostgresChannelMessageTableSubscriber messageTableSubscriber;

	private final UnicastingDispatcher dispatcher = new UnicastingDispatcher();

	private final ReentrantReadWriteLock hasHandlersLock = new ReentrantReadWriteLock(true);

	private @Nullable TransactionTemplate transactionTemplate;

	private RetryTemplate retryTemplate =
			new RetryTemplate(RetryPolicy.builder().maxRetries(1).delay(Duration.ZERO).build());

	private ErrorHandler errorHandler = ReflectionUtils::rethrowRuntimeException;

	@SuppressWarnings("NullAway.Init")
	private Executor executor;

	private volatile boolean hasHandlers;

	/**
	 * Create a subscribable channel for a Postgres database.
	 * @param jdbcChannelMessageStore The message store to use for the relevant region.
	 * @param groupId The group id that is targeted by the subscription.
	 * @param messageTableSubscriber The subscriber to use for receiving notifications.
	 */
	public PostgresSubscribableChannel(JdbcChannelMessageStore jdbcChannelMessageStore,
			Object groupId, PostgresChannelMessageTableSubscriber messageTableSubscriber) {

		Assert.notNull(jdbcChannelMessageStore, "A jdbcChannelMessageStore must be provided.");
		Assert.notNull(groupId, "A groupId must be set.");
		Assert.notNull(messageTableSubscriber, "A messageTableSubscriber must be set.");
		this.jdbcChannelMessageStore = jdbcChannelMessageStore;
		this.groupId = groupId;
		this.messageTableSubscriber = messageTableSubscriber;
	}

	/**
	 * Set the executor to use for dispatching newly received messages.
	 * @param executor The executor to use.
	 */
	public void setDispatcherExecutor(Executor executor) {
		Assert.notNull(executor, "An executor must be provided.");
		this.executor = executor;
	}

	/**
	 * Set the transaction manager to use for message processing. Each message will be processed in a
	 * separate transaction
	 * @param transactionManager The transaction manager to use
	 * @since 6.0.5
	 * @see PlatformTransactionManager
	 */
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		Assert.notNull(transactionManager, "A platform transaction manager must be provided.");
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	/**
	 * Set the retry template to use it for retries in case of exception in downstream processing
	 * @param retryTemplate The retry template to use
	 * @since 6.0.5
	 * @see RetryTemplate
	 */
	public void setRetryTemplate(RetryTemplate retryTemplate) {
		Assert.notNull(retryTemplate, "A retry template must be provided.");
		this.retryTemplate = retryTemplate;
	}

	/**
	 * Set a {@link ErrorHandler} for messages which cannot be dispatched by this channel.
	 * Used as a recovery callback after {@link RetryTemplate} execution throws an exception.
	 * @param errorHandler the {@link ErrorHandler} to use.
	 * @since 6.0.9
	 */
	public void setErrorHandler(ErrorHandler errorHandler) {
		Assert.notNull(errorHandler, "'errorHandler' must not be null.");
		this.errorHandler = errorHandler;
	}

	@Override
	protected void onInit() {
		super.onInit();
		if (this.executor == null) {
			this.executor = new SimpleAsyncTaskExecutor(getBeanName() + "-dispatcher-");
		}
	}

	@Override
	public boolean subscribe(MessageHandler handler) {
		boolean subscribed = super.subscribe(handler);
		if (this.dispatcher.getHandlerCount() == 1) {
			this.messageTableSubscriber.subscribe(this);
			this.hasHandlers = true;
			notifyUpdate();
		}
		return subscribed;
	}

	@Override
	public boolean unsubscribe(MessageHandler handle) {
		this.hasHandlersLock.writeLock().lock();
		try {
			boolean unsubscribed = super.unsubscribe(handle);
			if (this.dispatcher.getHandlerCount() == 0) {
				this.messageTableSubscriber.unsubscribe(this);
				this.hasHandlers = false;
			}
			return unsubscribed;
		}
		finally {
			this.hasHandlersLock.writeLock().unlock();
		}
	}

	@Override
	protected MessageDispatcher getDispatcher() {
		return this.dispatcher;
	}

	@Override
	protected boolean doSend(Message<?> message, long timeout) {
		this.jdbcChannelMessageStore.addMessageToGroup(this.groupId, message);
		return true;
	}

	@Override
	public void notifyUpdate() {
		this.executor.execute(() -> {
			Optional<?> dispatchedMessage;
			do {
				dispatchedMessage = pollAndDispatchMessage();
			} while (dispatchedMessage.isPresent());
		});
	}

	private Optional<?> pollAndDispatchMessage() {
		try {
			return doPollAndDispatchMessage();
		}
		catch (Exception ex) {
			try {
				this.errorHandler.handleError(ex);
			}
			catch (Exception ex1) {
				LOGGER.error(ex, "Exception during message dispatch");
			}
			return FALLBACK_STUB;
		}
	}

	private Optional<?> doPollAndDispatchMessage() {
		this.hasHandlersLock.readLock().lock();
		try {
			if (this.hasHandlers) {
				TransactionTemplate transactionTemplateToUse = this.transactionTemplate;
				if (transactionTemplateToUse != null) {
					return executeWithRetry(() ->
							transactionTemplateToUse.execute(status ->
									pollMessage().map(this::dispatch)));
				}
				else {
					return pollMessage()
							.map(message -> executeWithRetry(() -> dispatch(message)));
				}
			}
		}
		finally {
			this.hasHandlersLock.readLock().unlock();
		}
		return Optional.empty();
	}

	private <T> T executeWithRetry(Retryable<T> retryable) {
		try {
			return this.retryTemplate.execute(retryable);
		}
		catch (RetryException ex) {
			Throwable cause = ex.getCause();
			if (cause instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			throw new IllegalStateException(cause);
		}
	}

	private Optional<Message<?>> pollMessage() {
		return Optional.ofNullable(this.jdbcChannelMessageStore.pollMessageFromGroup(this.groupId));
	}

	private Message<?> dispatch(Message<?> message) {
		this.dispatcher.dispatch(message);
		return message;
	}

	@Override
	public String getRegion() {
		return this.jdbcChannelMessageStore.getRegion();
	}

	@Override
	public Object getGroupId() {
		return this.groupId;
	}

}
