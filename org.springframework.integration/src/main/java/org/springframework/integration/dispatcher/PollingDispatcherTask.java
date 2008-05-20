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

import org.springframework.integration.message.Message;
import org.springframework.integration.scheduling.MessagingTask;
import org.springframework.integration.scheduling.Schedule;
import org.springframework.util.Assert;

/**
 * A {@link MessagingTask} that combines polling and dispatching.
 * 
 * @author Mark Fisher
 */
public class PollingDispatcherTask implements MessagingTask {

	private final PollingDispatcher dispatcher;

	private final Schedule schedule;


	public PollingDispatcherTask(PollingDispatcher dispatcher, Schedule schedule) {
		Assert.notNull(dispatcher, "dispatcher must not be null");
		this.dispatcher = dispatcher;
		this.schedule = schedule;
	}


	public PollingDispatcher getDispatcher() {
		return this.dispatcher;
	}

	public Schedule getSchedule() {
		return this.schedule;
	}

	public void run() {
		List<Message<?>> messages = this.dispatcher.poll();
		for (Message<?> message : messages) {
			this.dispatcher.dispatch(message);
		}
	}

}
