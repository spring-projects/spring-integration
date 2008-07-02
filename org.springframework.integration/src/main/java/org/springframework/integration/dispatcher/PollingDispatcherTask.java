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

package org.springframework.integration.dispatcher;

import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.Subscribable;
import org.springframework.integration.message.MessageTarget;
import org.springframework.integration.scheduling.SchedulableTask;
import org.springframework.integration.scheduling.Schedule;
import org.springframework.util.Assert;

/**
 * A {@link SchedulableTask} that combines polling and dispatching.
 * 
 * @author Mark Fisher
 */
public class PollingDispatcherTask implements SchedulableTask, Subscribable {

	private final MessageChannel channel;

	private final Schedule schedule;

	private final SimpleDispatcher dispatcher;

	private volatile long receiveTimeout = -1;

	private volatile int maxMessagesPerTask = 1;


	public PollingDispatcherTask(MessageChannel channel, Schedule schedule) {
		Assert.notNull(channel, "channel must not be null");
		this.channel = channel;
		this.schedule = schedule;
		this.dispatcher = new SimpleDispatcher(this.channel.getDispatcherPolicy());
	}


	/**
	 * Set the maximum amount of time in milliseconds to wait for a message to be available. 
	 * A negative value indicates that receive calls should block indefinitely.
	 */
	public void setReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	/**
	 * Set the maximum number of messages for each retrieval attempt.
	 */
	public void setMaxMessagesPerTask(int maxMessagesPerTask) {
		Assert.isTrue(maxMessagesPerTask > 0, "'maxMessagePerTask' must be at least 1");
		this.maxMessagesPerTask = maxMessagesPerTask;
	}

	public boolean subscribe(MessageTarget target) {
		return this.dispatcher.subscribe(target);
	}

	public boolean unsubscribe(MessageTarget target) {
		return this.dispatcher.unsubscribe(target);
	}

	public Schedule getSchedule() {
		return this.schedule;
	}

	public void run() {
		int count = 0;
		while (count < this.maxMessagesPerTask) {
			Message<?> message = (this.receiveTimeout < 0) ?
					this.channel.receive() : this.channel.receive(this.receiveTimeout);
			if (message == null) {
				return;
			}
			this.dispatcher.send(message);
			count++;
		}
	}

}
