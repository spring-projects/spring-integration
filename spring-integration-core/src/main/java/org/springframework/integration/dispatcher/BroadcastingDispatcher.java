/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.dispatcher;

import java.util.Collection;
import java.util.concurrent.Executor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.integration.MessageDispatchingException;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;

/**
 * A broadcasting dispatcher implementation. If the 'ignoreFailures' property is set to <code>false</code> (the
 * default), it will fail fast such that any Exception thrown by a MessageHandler may prevent subsequent handlers from
 * receiving the Message. However, when an Executor is provided, the Messages may be dispatched in separate Threads so
 * that other handlers are invoked even when the 'ignoreFailures' flag is <code>false</code>.
 * <p>
 * If the 'ignoreFailures' flag is set to <code>true</code> on the other hand, it will make a best effort to send the
 * message to each of its handlers. In other words, when 'ignoreFailures' is <code>true</code>, if it fails to send to
 * any one handler, it will simply log a warn-level message but continue to send the Message to any other handlers.
 *
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Gary Russell
 * @author Oleg Zhurakousky
 */
public class BroadcastingDispatcher extends AbstractDispatcher implements BeanFactoryAware {

	private final boolean requireSubscribers;

	private volatile boolean ignoreFailures;

	private volatile boolean applySequence;

	private final Executor executor;

	private volatile int minSubscribers;

	private volatile MessageBuilderFactory messageBuilderFactory = new DefaultMessageBuilderFactory();


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

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.messageBuilderFactory = IntegrationUtils.getMessageBuilderFactory(beanFactory);
	}

	@Override
	public boolean dispatch(Message<?> message) {
		int dispatched = 0;
		int sequenceNumber = 1;
		Collection<MessageHandler> handlers = this.getHandlers();
		if (this.requireSubscribers && handlers.size() == 0) {
			throw new MessageDispatchingException(message, "Dispatcher has no subscribers");
		}
		int sequenceSize = handlers.size();
		for (final MessageHandler handler : handlers) {
			final Message<?> messageToSend = (!this.applySequence) ? message : this.messageBuilderFactory.fromMessage(message)
					.pushSequenceDetails(message.getHeaders().getId(), sequenceNumber++, sequenceSize).build();
			if (this.executor != null) {
				this.executor.execute(new Runnable() {
					@Override
					public void run() {
						invokeHandler(handler, messageToSend);
					}
				});
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
		return dispatched >= minSubscribers;
	}

	private boolean invokeHandler(MessageHandler handler, Message<?> message) {
		try {
			handler.handleMessage(message);
			return true;
		}
		catch (RuntimeException e) {
			if (!this.ignoreFailures) {
				if (e instanceof MessagingException && ((MessagingException) e).getFailedMessage() == null) {
					throw new MessagingException(message, e);
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
