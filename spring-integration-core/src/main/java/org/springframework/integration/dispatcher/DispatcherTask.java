/*
 * Copyright 2002-2007 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.handler.MessageHandlerNotRunningException;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.selector.MessageSelectorRejectedException;
import org.springframework.integration.scheduling.MessagingTask;
import org.springframework.integration.scheduling.Schedule;
import org.springframework.util.Assert;

/**
 * A task for polling {@link MessageDispatcher MessageDispatchers}. If
 * {@link #broadcast} is set to <code>false</code> (the default), each message
 * will be sent to a single {@link MessageHandler}. Otherwise, each
 * retrieved {@link Message} will be sent to all of the handlers.
 * 
 * @author Mark Fisher
 */
public class DispatcherTask implements MessagingTask {

	private Log logger = LogFactory.getLog(this.getClass());

	private Schedule schedule;

	private DispatcherPolicy dispatcherPolicy = new DispatcherPolicy();

	private MessageRetriever retriever;

	private List<MessageHandler> handlers = new CopyOnWriteArrayList<MessageHandler>();


	public DispatcherTask(MessageChannel channel) {
		Assert.notNull(channel, "'channel' must not be null");
		this.retriever = new ChannelPollingMessageRetriever(channel);
		DispatcherPolicy dispatcherPolicy = channel.getDispatcherPolicy();
		if (dispatcherPolicy != null) {
			this.dispatcherPolicy = dispatcherPolicy;
		}
	}


	public void setSchedule(Schedule schedule) {
		Assert.notNull(schedule, "'schedule' must not be null");
		this.schedule = schedule;
	}

	public Schedule getSchedule() {
		return this.schedule;
	}

	public void addHandler(MessageHandler handler) {
		Assert.notNull(handler, "'handler' must not be null");
		this.handlers.add(handler);
	}

	/**
	 * Retrieves messages and dispatches to the executors.
	 * 
	 * @return the number of messages processed
	 */
	public int dispatch() {
		int messagesProcessed = 0;
		Collection<Message<?>> messages = this.retriever.retrieveMessages();
		if (messages == null) {
			return 0;
		}
		for (Message<?> message : messages) {
			if (dispatchMessage(message)) {
				messagesProcessed++;
			}
		}
		return messagesProcessed;
	}

	protected boolean dispatchMessage(Message<?> message) {
		int attempts = 0;
		List<MessageHandler> targets = new ArrayList<MessageHandler>(this.handlers);
		while (attempts < this.dispatcherPolicy.getRejectionLimit()) {
			if (attempts > 0) {
				if (logger.isDebugEnabled()) {
					logger.debug("handler(s) rejected message after " + attempts
							+ " attempt(s), will try again after 'retryInterval' of " +
							this.dispatcherPolicy.getRetryInterval() + " milliseconds");
				}
				try {
					Thread.sleep(this.dispatcherPolicy.getRetryInterval());
				}
				catch (InterruptedException iex) {
					Thread.currentThread().interrupt();
					return false;
				}
			}
			Iterator<MessageHandler> iter = targets.iterator();
			if (!iter.hasNext()) {
				if (logger.isWarnEnabled()) {
					logger.warn("dispatcher has no active handlers");
				}
				return false;
			}
			boolean encounteredHandlerException = false;
			while (iter.hasNext()) {
				MessageHandler handler = iter.next();
				try {
					handler.handle(message);
					if (!this.dispatcherPolicy.isPublishSubscribe()) {
						return true;
					}
					iter.remove();
				}
				catch (MessageSelectorRejectedException e) {
					if (logger.isDebugEnabled()) {
						logger.debug("selector rejected task, continuing with other handlers if available", e);
					}
				}
				catch (MessageHandlerNotRunningException e) {
					if (logger.isDebugEnabled()) {
						logger.debug("handler not running, continuing with other handlers if available", e);
					}					
				}
				catch (MessageHandlingException e) {
					encounteredHandlerException = true;
					if (logger.isDebugEnabled()) {
						logger.debug("handler threw exception, continuing with other handlers if available", e);
					}
				}
			}
			if (!encounteredHandlerException) {
				return true;
			}
			attempts++;
		}
		if (this.dispatcherPolicy.getShouldFailOnRejectionLimit()) {
			throw new MessageDeliveryException("Dispatcher reached rejection limit of " + this.dispatcherPolicy.getRejectionLimit()
					+ ". Consider increasing the handler's concurrency and/or the dispatcherPolicy's 'rejectionLimit'.");
		}
		return false;
	}

	public void run() {
		this.dispatch();
	}

}
