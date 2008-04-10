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

package org.springframework.integration.adapter;

import java.util.concurrent.Executors;

import org.springframework.context.Lifecycle;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageMapper;
import org.springframework.integration.scheduling.MessagingTask;
import org.springframework.integration.scheduling.MessagingTaskScheduler;
import org.springframework.integration.scheduling.MessagingTaskSchedulerAware;
import org.springframework.integration.scheduling.PollingSchedule;
import org.springframework.integration.scheduling.Schedule;
import org.springframework.integration.scheduling.SimpleMessagingTaskScheduler;
import org.springframework.util.Assert;

/**
 * A channel adapter that retrieves objects from a {@link PollableSource},
 * delegates to a {@link MessageMapper} to create messages from those objects,
 * and then sends the resulting messages to the provided {@link MessageChannel}.
 * 
 * @author Mark Fisher
 */
public class PollingSourceAdapter<T> extends AbstractSourceAdapter<T> implements MessagingTaskSchedulerAware, Lifecycle {

	private volatile PollableSource<T> source;

	private volatile PollingSchedule schedule = new PollingSchedule(1000);

	private volatile MessagingTaskScheduler scheduler;

	private volatile int maxMessagesPerTask = 1;

	private volatile boolean starting;

	private volatile boolean running;


	/**
	 * Create a new adapter for the given source.
	 */
	public PollingSourceAdapter(PollableSource<T> source) {
		this.setSource(source);
	}

	/**
	 * No-arg constructor for providing source after construction.
	 */
	public PollingSourceAdapter() {
	}


	public void setSource(PollableSource<T> source) {
		Assert.notNull(source, "'source' must not be null");
		this.source = source;
	}

	public void setInitialDelay(long intialDelay) {
		Assert.isTrue(intialDelay >= 0, "'intialDelay' must not be negative");
		this.schedule.setInitialDelay(intialDelay);
	}

	public void setPeriod(long period) {
		this.schedule.setPeriod(period);
	}

	public void setMaxMessagesPerTask(int maxMessagesPerTask) {
		Assert.isTrue(maxMessagesPerTask > 0, "'maxMessagesPerTask' must be at least one");
		this.maxMessagesPerTask = maxMessagesPerTask;
	}

	public void setMessagingTaskScheduler(MessagingTaskScheduler scheduler) {
		Assert.notNull(scheduler, "scheduler must not be null");
		this.scheduler = scheduler;
	}

	protected PollableSource<T> getSource() {
		return this.source;
	}

	public boolean isRunning() {
		return this.running;
	}

	@Override
	protected void initialize() {
		if (this.source == null) {
			throw new ConfigurationException("source must not be null");
		}
	}

	public void start() {
		if (this.isRunning() || this.starting) {
			return;
		}
		this.starting = true;
		if (!this.isInitialized()) {
			this.afterPropertiesSet();
		}
		if (this.scheduler == null) {
			if (logger.isInfoEnabled()) {
				logger.info("no task scheduler has been provided, will create one");
			}
			this.scheduler = new SimpleMessagingTaskScheduler(Executors.newSingleThreadScheduledExecutor());
		}
		if (!this.scheduler.isRunning()) {
			this.scheduler.start();
		}
		this.scheduler.schedule(new PollingSourceAdapterTask());
		this.running = true;
		this.starting = false;
	}

	public void stop() {
		this.running = false;
	}

	public int processMessages() {
		if (!this.isRunning()) {
			if (logger.isDebugEnabled()) {
				logger.debug("source adapter not polling since it has not yet been started");
			}
			return 0;
		}
		int messagesProcessed = 0;
		int limit = this.maxMessagesPerTask;
		while (messagesProcessed < limit) {
			Message<T> result = this.source.poll();
			if (result != null && this.sendToChannel(result)) {
				messagesProcessed++;
				this.onSend(result);
			}
			else {
				return messagesProcessed;
			}
		}
		return messagesProcessed;
	}

	/**
	 * Callback method invoked after a message is sent to the channel.
	 * <p>
	 * Subclasses may override. The default implementation does nothing.
	 */
	protected void onSend(Message<T> sentMessage) {
	}


	private class PollingSourceAdapterTask implements MessagingTask {

		public void run() {
			processMessages();
		}

		public Schedule getSchedule() {
			return schedule;
		}
	}

}
