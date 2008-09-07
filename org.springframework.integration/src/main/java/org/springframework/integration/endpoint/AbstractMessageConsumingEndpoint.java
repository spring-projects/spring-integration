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

import org.springframework.context.Lifecycle;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageConsumer;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.message.Subscribable;
import org.springframework.integration.scheduling.PollingSchedule;
import org.springframework.integration.scheduling.Schedule;

/**
 * The base class for Message Endpoint implementations that consume Messages.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractMessageConsumingEndpoint extends AbstractEndpoint implements MessageConsumer, Lifecycle {

	private volatile MessageChannel inputChannel;

	private volatile Schedule schedule = new PollingSchedule(0);

	private volatile ChannelPoller poller;

	private volatile TaskExecutor taskExecutor;

	private volatile int maxMessagesPerPoll = -1;

	private volatile boolean initialized;

	private volatile boolean running;

	private final Object lifecycleMonitor = new Object();


	public void setInputChannel(MessageChannel inputChannel) {
		this.inputChannel = inputChannel;
	}

	public void setSchedule(Schedule schedule) {
		this.schedule = schedule;
	}

	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
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
				this.poller = new ChannelPoller((PollableChannel) this.inputChannel, this.schedule);
				this.poller.setMaxMessagesPerPoll(this.maxMessagesPerPoll);
				this.configureTransactionSettingsForPoller(this.poller);
				if (this.taskExecutor != null) {
					this.poller.setTaskExecutor(this.taskExecutor);
				}
				this.poller.subscribe(this);
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
			if (this.inputChannel == null) {
				throw new ConfigurationException("failed to start endpoint, inputChannel is required");
			}
			if (this.inputChannel instanceof Subscribable) {
				((Subscribable) inputChannel).subscribe(this);
			}
			else if (this.inputChannel instanceof PollableChannel) {
				if (this.getTaskScheduler() == null) {
					throw new ConfigurationException("failed to start endpoint, no taskScheduler available");
				}
				this.getTaskScheduler().schedule(poller);
			}
			this.running = true;
		}
	}

	public final void stop() {
		synchronized (this.lifecycleMonitor) {
			if (!this.running) {
				return;
			}
			if (this.inputChannel instanceof Subscribable) {
				((Subscribable) inputChannel).unsubscribe(this);
			}
			else if (this.poller != null) {
				this.getTaskScheduler().cancel(poller, true);
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
			this.processMessage(message);
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

	protected abstract void processMessage(Message<?> message);

}
