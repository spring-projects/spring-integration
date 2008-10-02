/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.endpoint;

import java.util.concurrent.ScheduledFuture;

import org.springframework.context.Lifecycle;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.channel.SubscribableChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageConsumer;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.scheduling.IntervalTrigger;
import org.springframework.integration.scheduling.Trigger;
import org.springframework.integration.util.ErrorHandler;
import org.springframework.util.Assert;

/**
 * The base class for Message Endpoint implementations that consume Messages.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractMessageConsumingEndpoint extends AbstractEndpoint implements MessageConsumer, Lifecycle {

	private volatile MessageChannel inputChannel;

	private volatile Trigger trigger = new IntervalTrigger(0);

	private volatile ChannelPoller poller;

	private volatile ScheduledFuture<?> pollerFuture;

	private volatile TaskExecutor taskExecutor;

	private volatile int maxMessagesPerPoll = -1;

	private volatile ErrorHandler errorHandler;

	private volatile boolean initialized;

	private volatile boolean running;

	private final Object lifecycleMonitor = new Object();


	public void setInputChannel(MessageChannel inputChannel) {
		this.inputChannel = inputChannel;
	}

	public void setTrigger(Trigger trigger) {
		this.trigger = trigger;
	}

	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Provide an error handler for any Exceptions that occur
	 * upon invocation of this endpoint. If none is provided,
	 * the Exception messages will be logged (at warn level),
	 * and the Exception rethrown.
	 */
	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	public void setMaxMessagesPerPoll(int maxMessagesPerPoll) {
		this.maxMessagesPerPoll = maxMessagesPerPoll;
		if (this.poller != null) {
			this.poller.setMaxMessagesPerPoll(maxMessagesPerPoll);
		}
	}

	public final boolean isRunning() {
		return this.running;
	}

	@Override
	protected void initialize()  throws Exception {
		synchronized (this.lifecycleMonitor) {
			if (this.inputChannel instanceof PollableChannel && this.poller == null) {
				this.poller = new ChannelPoller((PollableChannel) this.inputChannel, this.trigger);
				this.poller.setMaxMessagesPerPoll(this.maxMessagesPerPoll);
				this.configureTransactionSettingsForPoller(this.poller);
				if (this.taskExecutor != null) {
					this.poller.setTaskExecutor(this.taskExecutor);
				}
				this.poller.setConsumer(this);
			}
			this.initialized = true;
		}
	}

	public final void start() {
		synchronized (this.lifecycleMonitor) {
			if (this.running) {
				return;
			}
			if (!this.initialized) {
				this.afterPropertiesSet();
			}
			Assert.notNull(this.inputChannel, "failed to start endpoint, inputChannel is required");
			if (this.inputChannel instanceof SubscribableChannel) {
				((SubscribableChannel) inputChannel).subscribe(this);
			}
			else if (this.inputChannel instanceof PollableChannel) {
				Assert.notNull(this.getTaskScheduler(),
						"failed to start endpoint, no taskScheduler available");
				this.pollerFuture = this.getTaskScheduler().schedule(this.poller, this.poller.getTrigger());
			}
			this.running = true;
		}
	}

	public final void stop() {
		synchronized (this.lifecycleMonitor) {
			if (!this.running) {
				return;
			}
			if (this.inputChannel instanceof SubscribableChannel) {
				((SubscribableChannel) inputChannel).unsubscribe(this);
			}
			else if (this.pollerFuture != null) {
				this.pollerFuture.cancel(true);
			}
			this.running = false;
		}
	}

	public final void onMessage(Message<?> message) {
		if (message == null || message.getPayload() == null) {
			throw new IllegalArgumentException("Message and its payload must not be null");
		}
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("endpoint '" + this + "' processing message: " + message);
		}
		try {
			this.onMessageInternal(message);
		}
		catch (Exception e) {
			if (e instanceof MessagingException) {
				this.handleException((MessagingException) e);
			}
			else {
				this.handleException(new MessageHandlingException(message,
						"failure occurred in endpoint '" + this.toString() + "'", e));
			}
		}
	}

	protected void handleException(MessagingException exception) {
		if (this.errorHandler == null) {
			if (this.logger.isWarnEnabled()) {
				this.logger.warn("exception occurred in endpoint '" + this + "'", exception);
			}
			throw exception;
		}
		this.errorHandler.handle(exception);
	}

	protected abstract void onMessageInternal(Message<?> message);

}
