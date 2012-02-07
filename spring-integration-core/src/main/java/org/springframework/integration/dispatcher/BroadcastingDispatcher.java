/*
 * Copyright 2002-2012 the original author or authors.
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

import java.util.List;
import java.util.concurrent.Executor;

import org.springframework.integration.Message;
import org.springframework.integration.MessageDispatchingException;
import org.springframework.integration.MessagingException;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.support.MessageBuilder;

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
 */
public class BroadcastingDispatcher extends AbstractDispatcher {

	private final boolean requireSubscribers;

	private volatile boolean ignoreFailures;

	private volatile boolean applySequence;

	private final Executor executor;

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
	 */
	public void setIgnoreFailures(boolean ignoreFailures) {
		this.ignoreFailures = ignoreFailures;
	}

	/**
	 * Specify whether to apply sequence numbers to the messages prior to sending to the handlers. By default, sequence
	 * numbers will <em>not</em> be applied
	 */
	public void setApplySequence(boolean applySequence) {
		this.applySequence = applySequence;
	}

	public boolean dispatch(Message<?> message) {
		boolean dispatched = false;
		int sequenceNumber = 1;
		List<MessageHandler> handlers = this.getHandlers();
		if (this.requireSubscribers && handlers.size() == 0) {
			throw new MessageDispatchingException(message, "Dispatcher has no subscribers");
		}
		int sequenceSize = handlers.size();
		for (final MessageHandler handler : handlers) {
			final Message<?> messageToSend = (!this.applySequence) ? message : MessageBuilder.fromMessage(message)
					.pushSequenceDetails(message.getHeaders().getId(), sequenceNumber++, sequenceSize).build();
			if (this.executor != null) {
				this.executor.execute(new Runnable() {
					public void run() {
						invokeHandler(handler, messageToSend);
					}
				});
				dispatched = true;
			}
			else {
				boolean success = this.invokeHandler(handler, messageToSend);
				dispatched = (success || dispatched);
			}
		}
		return dispatched;
	}

	private boolean invokeHandler(MessageHandler handler, Message<?> message) {
		try {
			handler.handleMessage(message);
			return true;
		}
		catch (RuntimeException e) {
			if (!this.ignoreFailures) {
				if (e instanceof MessagingException && ((MessagingException) e).getFailedMessage() == null) {
					((MessagingException) e).setFailedMessage(message);
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
