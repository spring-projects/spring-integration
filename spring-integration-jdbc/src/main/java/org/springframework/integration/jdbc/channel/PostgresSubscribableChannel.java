/*
 * Copyright 2022-2023 the original author or authors.
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

import java.util.Optional;
import java.util.concurrent.Executor;

import org.springframework.core.log.LogAccessor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.integration.channel.AbstractSubscribableChannel;
import org.springframework.integration.dispatcher.MessageDispatcher;
import org.springframework.integration.dispatcher.UnicastingDispatcher;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

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
 *
 * @since 6.0
 */
public class PostgresSubscribableChannel extends AbstractSubscribableChannel
		implements PostgresChannelMessageTableSubscriber.Subscription {

	private static final LogAccessor LOGGER = new LogAccessor(PostgresSubscribableChannel.class);

	private final JdbcChannelMessageStore jdbcChannelMessageStore;

	private final Object groupId;

	private final PostgresChannelMessageTableSubscriber messageTableSubscriber;

	private final UnicastingDispatcher dispatcher = new UnicastingDispatcher();

	private TransactionTemplate transactionTemplate;

	private RetryTemplate retryTemplate = RetryTemplate.builder().maxAttempts(1).build();

	private Executor executor;

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
	 * Set the retry template to use for retries in case of exception in downstream processing
	 * @param retryTemplate The retry template to use
	 * @since 6.0.5
	 * @see RetryTemplate
	 */
	public void setRetryTemplate(RetryTemplate retryTemplate) {
		Assert.notNull(retryTemplate, "A retry template must be provided.");
		this.retryTemplate = retryTemplate;
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
			notifyUpdate();
		}
		return subscribed;
	}

	@Override
	public boolean unsubscribe(MessageHandler handle) {
		boolean unsubscribed = super.unsubscribe(handle);
		if (this.dispatcher.getHandlerCount() == 0) {
			this.messageTableSubscriber.unsubscribe(this);
		}
		return unsubscribed;
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
			try {
				Optional<Message<?>> dispatchedMessage;
				do {
					if (this.transactionTemplate != null) {
						dispatchedMessage =
								this.retryTemplate.execute(context ->
										this.transactionTemplate.execute(status ->
												pollMessage()
														.map(this::dispatch)));
					}
					else {
						dispatchedMessage =
								pollMessage()
										.map(message -> this.retryTemplate.execute(context -> dispatch(message)));
					}
				} while (dispatchedMessage.isPresent());
			}
			catch (Exception ex) {
				LOGGER.error(ex, "Exception during message dispatch");
			}
		});
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
