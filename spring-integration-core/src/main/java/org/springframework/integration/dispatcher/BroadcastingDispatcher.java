/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.dispatcher;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.Executor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.integration.MessageDispatchingException;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.support.MessageDecorator;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.MessageHandlingRunnable;
import org.springframework.util.Assert;

/**
 * A broadcasting dispatcher implementation. If the 'ignoreFailures' property is set to <code>false</code> (the
 * default), it will fail fast such that any Exception thrown by a MessageHandler may prevent subsequent handlers from
 * receiving the Message. However, when an Executor is provided, the Messages may be dispatched in separate Threads so
 * that other handlers are invoked even when the 'ignoreFailures' flag is <code>false</code>.
 * <p>
 * If the 'ignoreFailures' flag is set to <code>true</code> on the other hand, it will make a best effort to send the
 * message to each of its handlers. In other words, when 'ignoreFailures' is <code>true</code>, if it fails to send to
 * any one handler, it will simply log a warn-level message but continue to send the Message to any other handlers.
 * <p>
 * If the 'requireSubscribers' flag is set to <code>true</code>, the sent message is considered as non-dispatched
 * and rejected to the caller with the {@code "Dispatcher has no subscribers"} {@link MessageDispatchingException}.
 *
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Gary Russell
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 */
public class BroadcastingDispatcher extends AbstractDispatcher implements BeanFactoryAware {

	private final boolean requireSubscribers;

	private volatile boolean ignoreFailures;

	private volatile boolean applySequence;

	private final Executor executor;

	private volatile int minSubscribers;

	private MessageHandlingTaskDecorator messageHandlingTaskDecorator = task -> task;

	private BeanFactory beanFactory;

	private volatile MessageBuilderFactory messageBuilderFactory = new DefaultMessageBuilderFactory();

	private volatile boolean messageBuilderFactorySet;


	public BroadcastingDispatcher() {
		this(null, false);
	}

	public BroadcastingDispatcher(Executor executor) {
		this(executor, false);
	}

	public BroadcastingDispatcher(boolean requireSubscribers) {
		this(null, requireSubscribers);
	}

	public BroadcastingDispatcher(Executor executor, boolean requireSubscribers) {
		this.requireSubscribers = requireSubscribers;
		this.executor = executor;
	}

	/**
	 * Specify whether failures for one or more of the handlers should be ignored. By default this is <code>false</code>
	 * meaning that an Exception will be thrown when a handler fails. To override this and suppress Exceptions, set the
	 * value to <code>true</code>.
	 * <p>
	 * Keep in mind that when using an Executor, even without ignoring the failures, other handlers may be invoked after
	 * one throws an Exception. Since the Executor is most likely using a different thread, this flag would only affect
	 * whether an error Message is sent to the error channel or not in the case that such an Executor has been
	 * configured.
	 *
	 * @param ignoreFailures true when failures are to be ignored.
	 */
	public void setIgnoreFailures(boolean ignoreFailures) {
		this.ignoreFailures = ignoreFailures;
	}

	/**
	 * Specify whether to apply sequence numbers to the messages prior to sending to the handlers. By default, sequence
	 * numbers will <em>not</em> be applied.
	 *
	 * @param applySequence true when sequence information should be applied.
	 */
	public void setApplySequence(boolean applySequence) {
		this.applySequence = applySequence;
	}

	/**
	 * If at least this number of subscribers receive the message, {@link #dispatch(Message)}
	 * will return true. Default: 0.
	 * @param minSubscribers The minimum number of subscribers.
	 */
	public void setMinSubscribers(int minSubscribers) {
		this.minSubscribers = minSubscribers;
	}

	public void setMessageHandlingTaskDecorator(MessageHandlingTaskDecorator messageHandlingTaskDecorator) {
		Assert.notNull(messageHandlingTaskDecorator, "'messageHandlingTaskDecorator' must not be null.");
		this.messageHandlingTaskDecorator = messageHandlingTaskDecorator;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	protected MessageBuilderFactory getMessageBuilderFactory() {
		if (!this.messageBuilderFactorySet) {
			if (this.beanFactory != null) {
				this.messageBuilderFactory = IntegrationUtils.getMessageBuilderFactory(this.beanFactory);
			}
			this.messageBuilderFactorySet = true;
		}
		return this.messageBuilderFactory;
	}

	@Override // NOSONAR complexity
	public boolean dispatch(Message<?> message) {
		int dispatched = 0;
		int sequenceNumber = 1;
		Collection<MessageHandler> handlers = this.getHandlers();
		if (this.requireSubscribers && handlers.size() == 0) {
			throw new MessageDispatchingException(message, "Dispatcher has no subscribers");
		}
		int sequenceSize = handlers.size();
		Message<?> messageToSend = message;
		UUID sequenceId = null;
		if (this.applySequence) {
			sequenceId = message.getHeaders().getId();
		}
		for (MessageHandler handler : handlers) {
			if (this.applySequence) {
				messageToSend = getMessageBuilderFactory()
						.fromMessage(message)
						.pushSequenceDetails(sequenceId, sequenceNumber++, sequenceSize)
						.build();
				if (message instanceof MessageDecorator) {
					messageToSend = ((MessageDecorator) message).decorateMessage(messageToSend);
				}
			}

			if (this.executor != null) {
				Runnable task = createMessageHandlingTask(handler, messageToSend);
				this.executor.execute(task);
				dispatched++;
			}
			else {
				if (this.invokeHandler(handler, messageToSend)) {
					dispatched++;
				}
			}
		}
		if (dispatched == 0 && this.minSubscribers == 0 && logger.isDebugEnabled()) {
			if (sequenceSize > 0) {
				logger.debug("No subscribers received message, default behavior is ignore");
			}
			else {
				logger.debug("No subscribers, default behavior is ignore");
			}
		}
		return dispatched >= this.minSubscribers;
	}


	private Runnable createMessageHandlingTask(final MessageHandler handler, final Message<?> message) {
		MessageHandlingRunnable task = new MessageHandlingRunnable() {

			private final MessageHandler delegate = message1 -> invokeHandler(handler, message1);

			@Override
			public void run() {
				invokeHandler(handler, message);
			}

			@Override
			public Message<?> getMessage() {
				return message;
			}

			@Override
			public MessageHandler getMessageHandler() {
				return this.delegate;
			}

		};

		return this.messageHandlingTaskDecorator.decorate(task);
	}

	private boolean invokeHandler(MessageHandler handler, Message<?> message) {
		try {
			handler.handleMessage(message);
			return true;
		}
		catch (RuntimeException e) {
			if (!this.ignoreFailures) {
				if (e instanceof MessagingException
						&& ((MessagingException) e).getFailedMessage() == null) { // NOSONAR

					throw new MessagingException(message, "Failed to handle Message", e);
				}
				throw e;
			}
			else if (this.logger.isWarnEnabled()) {
				logger.warn("Suppressing Exception since 'ignoreFailures' is set to TRUE.", e);
			}
			return false;
		}
	}

}
