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

import java.util.List;

import org.springframework.integration.channel.DispatcherPolicy;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.PollableSource;
import org.springframework.integration.scheduling.MessagingTask;
import org.springframework.integration.scheduling.Schedule;
import org.springframework.util.Assert;

/**
 * A subclass of {@link SimpleDispatcher} that adds message retrieval
 * capabilities and may be scheduled to run as a task.
 * 
 * @author Mark Fisher
 */
public class PollingDispatcher extends SimpleDispatcher implements MessagingTask {

	private final SourcePoller poller;

	private final Schedule schedule;


	public PollingDispatcher(MessageChannel channel, Schedule schedule) {
		this(channel, channel.getDispatcherPolicy(), schedule);
	}

	public PollingDispatcher(PollableSource<?> source, DispatcherPolicy dispatcherPolicy, Schedule schedule) {
		super(dispatcherPolicy);
		Assert.notNull(source, "source must not be null");
		this.poller = new SourcePoller(source);
		if (!(source instanceof MessageChannel)) {
			this.poller.setMaxMessagesPerTask(dispatcherPolicy.getMaxMessagesPerTask());
			this.poller.setTimeout(dispatcherPolicy.getReceiveTimeout());
		}
		this.schedule = schedule;
	}


	public Schedule getSchedule() {
		return this.schedule;
	}

	public void run() {
		List<Message<?>> messages = this.poller.poll();
		for (Message<?> message : messages) {
			this.dispatch(message);
		}
	}

}
