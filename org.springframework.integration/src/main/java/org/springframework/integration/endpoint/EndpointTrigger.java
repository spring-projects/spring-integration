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

import org.springframework.integration.dispatcher.BroadcastingDispatcher;
import org.springframework.integration.dispatcher.PollingDispatcher;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.PollableSource;
import org.springframework.integration.scheduling.PollingSchedule;
import org.springframework.integration.scheduling.Schedule;

/**
 * A {@link PollingDispatcher} implementation that sends a message
 * to trigger endpoint polling.
 * 
 * @author Mark Fisher
 */
public class EndpointTrigger extends PollingDispatcher {

	/**
	 * Create an endpoint trigger with the specified {@link Schedule}.
	 */
	public EndpointTrigger(Schedule schedule) {
		super(new TriggerSource(), new BroadcastingDispatcher(), schedule);
	}

	/**
	 * Create an endpoint trigger. A {@link PollingSchedule} will be
	 * created with the specified interval.
	 */
	public EndpointTrigger(long interval) {
		this(new PollingSchedule(interval));
	}

	/**
	 * Create an endpoint trigger that will run one time only when submitted to
	 * a {@link org.springframework.integration.scheduling.TaskScheduler}.
	 */
	public EndpointTrigger() {
		this(null);
	}


	private static class TriggerSource implements PollableSource<EndpointPoller> {

		public Message<EndpointPoller> receive() {
			return new TriggerMessage();
		}
	}

}
