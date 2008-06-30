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
import org.springframework.integration.message.Target;
import org.springframework.integration.scheduling.MessagingTask;
import org.springframework.integration.scheduling.Schedule;
import org.springframework.util.Assert;

/**
 * A {@link MessagingTask} that combines polling and dispatching.
 * 
 * @author Mark Fisher
 */
public class PollingDispatcherTask implements MessagingTask, Subscribable {

	private final MessageChannel channel;

	private final Schedule schedule;

	private final SimpleDispatcher dispatcher;


	public PollingDispatcherTask(MessageChannel channel, Schedule schedule) {
		Assert.notNull(channel, "channel must not be null");
		this.channel = channel;
		this.schedule = schedule;
		this.dispatcher = new SimpleDispatcher(this.channel.getDispatcherPolicy());
	}


	public boolean subscribe(Target target) {
		return this.dispatcher.subscribe(target);
	}

	public boolean unsubscribe(Target target) {
		return this.dispatcher.unsubscribe(target);
	}

	public Schedule getSchedule() {
		return this.schedule;
	}

	public void run() {
		long timeout = this.channel.getDispatcherPolicy().getReceiveTimeout();
		int limit = this.channel.getDispatcherPolicy().getMaxMessagesPerTask();
		int count = 0;
		while (count < limit) {
			Message<?> message = (timeout < 0) ? this.channel.receive() : this.channel.receive(timeout);
			if (message == null) {
				return;
			}
			this.dispatcher.dispatch(message);
			count++;
		}
	}

}
