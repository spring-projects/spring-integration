/*
 * Copyright 2015-2021 the original author or authors.
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

package org.springframework.integration.channel;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;

import org.springframework.integration.IntegrationPatternType;
import org.springframework.integration.dispatcher.AbstractDispatcher;
import org.springframework.integration.support.MessagingExceptionWrapper;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.messaging.support.MessageHandlingRunnable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * The {@link AbstractSubscribableChannel} base implementation for those inheritors
 * which logic may be based on the {@link Executor}.
 * <p>
 * Utilizes common operations for the {@link AbstractDispatcher}.
 * <p>
 * Implements the {@link ExecutorChannelInterceptor}s logic when the message handling
 * is handed to the {@link Executor#execute(Runnable)}.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.2
 *
 * @see ExecutorChannel
 * @see PublishSubscribeChannel
 *
 */
public abstract class AbstractExecutorChannel extends AbstractSubscribableChannel
		implements ExecutorChannelInterceptorAware {

	protected Executor executor; // NOSONAR

	protected AbstractDispatcher dispatcher; // NOSONAR

	protected Integer maxSubscribers; // NOSONAR

	protected int executorInterceptorsSize; // NOSONAR

	public AbstractExecutorChannel(@Nullable Executor executor) {
		this.executor = executor;
	}

	/**
	 * Specify the maximum number of subscribers supported by the
	 * channel's dispatcher.
	 * @param maxSubscribers The maximum number of subscribers allowed.
	 */
	public void setMaxSubscribers(int maxSubscribers) {
		this.maxSubscribers = maxSubscribers;
		this.dispatcher.setMaxSubscribers(maxSubscribers);
	}

	@Override
	public void setInterceptors(List<ChannelInterceptor> interceptors) {
		super.setInterceptors(interceptors);
		for (ChannelInterceptor interceptor : interceptors) {
			if (interceptor instanceof ExecutorChannelInterceptor) {
				this.executorInterceptorsSize++;
			}
		}
	}

	@Override
	public void addInterceptor(ChannelInterceptor interceptor) {
		super.addInterceptor(interceptor);
		if (interceptor instanceof ExecutorChannelInterceptor) {
			this.executorInterceptorsSize++;
		}
	}

	@Override
	public void addInterceptor(int index, ChannelInterceptor interceptor) {
		super.addInterceptor(index, interceptor);
		if (interceptor instanceof ExecutorChannelInterceptor) {
			this.executorInterceptorsSize++;
		}
	}

	@Override
	public boolean removeInterceptor(ChannelInterceptor interceptor) {
		boolean removed = super.removeInterceptor(interceptor);
		if (removed && interceptor instanceof ExecutorChannelInterceptor) {
			this.executorInterceptorsSize--;
		}
		return removed;
	}

	@Override
	@Nullable
	public ChannelInterceptor removeInterceptor(int index) {
		ChannelInterceptor interceptor = super.removeInterceptor(index);
		if (interceptor instanceof ExecutorChannelInterceptor) {
			this.executorInterceptorsSize--;
		}
		return interceptor;
	}

	@Override
	public boolean hasExecutorInterceptors() {
		return this.executorInterceptorsSize > 0;
	}

	@Override
	public IntegrationPatternType getIntegrationPatternType() {
		return IntegrationPatternType.executor_channel;
	}

	protected class MessageHandlingTask implements Runnable {

		private final MessageHandlingRunnable delegate;

		public MessageHandlingTask(MessageHandlingRunnable task) {
			this.delegate = task;
		}

		@Override
		public void run() {
			Message<?> message = this.delegate.getMessage();
			MessageHandler messageHandler = this.delegate.getMessageHandler();
			Assert.notNull(messageHandler, "'messageHandler' must not be null");
			Deque<ExecutorChannelInterceptor> interceptorStack = null;
			try {
				if (AbstractExecutorChannel.this.executorInterceptorsSize > 0) {
					interceptorStack = new ArrayDeque<>();
					message = applyBeforeHandle(message, interceptorStack);
					if (message == null) {
						return;
					}
				}
				messageHandler.handleMessage(message);
				if (!CollectionUtils.isEmpty(interceptorStack)) {
					triggerAfterMessageHandled(message, null, interceptorStack);
				}
			}
			catch (Exception ex) {
				if (!CollectionUtils.isEmpty(interceptorStack)) {
					triggerAfterMessageHandled(message, ex, interceptorStack);
				}
				if (ex instanceof MessagingException) { // NOSONAR
					throw new MessagingExceptionWrapper(message, (MessagingException) ex);
				}
				String description = "Failed to handle " + message + " to " + this + " in " + messageHandler;
				throw new MessageDeliveryException(message, description, ex);
			}
			catch (Error ex) { //NOSONAR - ok, we re-throw below
				if (!CollectionUtils.isEmpty(interceptorStack)) {
					String description = "Failed to handle " + message + " to " + this + " in " + messageHandler;
					triggerAfterMessageHandled(message, new MessageDeliveryException(message, description, ex),
							interceptorStack);
				}
				throw ex;
			}
		}

		@Nullable
		private Message<?> applyBeforeHandle(Message<?> message, Deque<ExecutorChannelInterceptor> interceptorStack) {
			Message<?> theMessage = message;
			for (ChannelInterceptor interceptor : AbstractExecutorChannel.this.interceptors.interceptors) {
				if (interceptor instanceof ExecutorChannelInterceptor) {
					ExecutorChannelInterceptor executorInterceptor = (ExecutorChannelInterceptor) interceptor;
					theMessage = executorInterceptor.beforeHandle(theMessage, AbstractExecutorChannel.this,
							this.delegate.getMessageHandler());
					if (theMessage == null) {
						if (isLoggingEnabled()) {
							logger.debug(() -> executorInterceptor.getClass().getSimpleName()
									+ " returned null from beforeHandle, i.e. precluding the send.");
						}
						triggerAfterMessageHandled(null, null, interceptorStack);
						return null;
					}
					interceptorStack.add(executorInterceptor);
				}
			}
			return theMessage;
		}

		private void triggerAfterMessageHandled(@Nullable Message<?> message, @Nullable Exception ex,
				Deque<ExecutorChannelInterceptor> interceptorStack) {
			Iterator<ExecutorChannelInterceptor> iterator = interceptorStack.descendingIterator();
			while (iterator.hasNext()) {
				ExecutorChannelInterceptor interceptor = iterator.next();
				try {
					interceptor.afterMessageHandled(message, AbstractExecutorChannel.this, //NOSONAR
							this.delegate.getMessageHandler(), ex);
				}
				catch (Throwable ex2) { //NOSONAR
					logger.error(ex2, () -> "Exception from afterMessageHandled in " + interceptor);
				}
			}
		}

	}

}
