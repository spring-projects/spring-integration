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

import org.springframework.integration.channel.DispatcherPolicy;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.dispatcher.PollingDispatcher;
import org.springframework.integration.message.PollableSource;
import org.springframework.integration.scheduling.MessagingTask;
import org.springframework.integration.scheduling.PollingSchedule;
import org.springframework.integration.scheduling.Schedule;
import org.springframework.util.Assert;

/**
 * A channel adapter that retrieves messages from a {@link PollableSource}
 * and then sends the resulting messages to the provided {@link MessageChannel}.
 * 
 * @author Mark Fisher
 */
public class PollingSourceEndpoint extends AbstractSourceEndpoint implements MessagingTask {

	private final DispatcherPolicy dispatcherPolicy = new DispatcherPolicy();

	private final PollingDispatcher dispatcher;


	public PollingSourceEndpoint(PollableSource<?> source, MessageChannel channel, PollingSchedule schedule) {
		super(source, channel);
		Assert.notNull(schedule, "schedule must not be null");
		this.dispatcher = new PollingDispatcher(source, this.dispatcherPolicy, schedule);
		this.dispatcher.subscribe(this.getChannel());
	}


	public void setMaxMessagesPerTask(int maxMessagesPerTask) {
		this.dispatcherPolicy.setMaxMessagesPerTask(maxMessagesPerTask);
	}

	public void setSendTimeout(long sendTimeout) {
		this.dispatcher.setSendTimeout(sendTimeout);
	}

	public Schedule getSchedule() {
		return this.dispatcher.getSchedule();
	}

	public void run() {
		this.dispatcher.run();
	}

}
