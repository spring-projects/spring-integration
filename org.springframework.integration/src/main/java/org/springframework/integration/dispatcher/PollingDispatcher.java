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

import org.springframework.integration.message.BlockingSource;
import org.springframework.integration.message.BlockingTarget;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageTarget;
import org.springframework.integration.message.PollableSource;
import org.springframework.integration.scheduling.SchedulableTask;
import org.springframework.integration.scheduling.Schedule;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 */
public class PollingDispatcher implements SchedulableTask {

	private final PollableSource<?> source;

	private final MessageDispatcher dispatcher;

	private final Schedule schedule;

	private volatile long receiveTimeout = 5000;

	private volatile int maxMessagesPerTask = 1;


	/**
	 * Create a PollingDispatcher for the provided {@link PollableSource}.
	 * It can be scheduled according to the specified {@link Schedule}.
	 */
	public PollingDispatcher(PollableSource<?> source, MessageDispatcher dispatcher, Schedule schedule) {
		Assert.notNull(source, "source must not be null");
		Assert.notNull(dispatcher, "dispatcher must not be null");
		this.source = source;
		this.dispatcher = dispatcher;
		this.schedule = schedule;
	}


	/**
	 * Specify the timeout to use when receiving from the source (in milliseconds).
	 * Note that this value will only be applicable if the source is an instance
	 * of {@link BlockingSource}.
	 * <p/>
	 * A negative value indicates that receive calls should block indefinitely.
	 * The default value is 5000 (5 seconds).
	 */
	public void setReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	/**
	 * Specify the timeout to use when sending to a target (in milliseconds).
	 * Note that this value will only be applicable if the target is an instance
	 * of {@link BlockingTarget}. The default value is 0.
	 */
	public void setSendTimeout(long sendTimeout) {
		this.dispatcher.setTimeout(sendTimeout);
	}

	/**
	 * Set the maximum number of messages to receive for each poll.
	 * A non-positive value indicates that polling should repeat as long
	 * as non-null messages are being received and successfully sent.
	 */
	public void setMaxMessagesPerTask(int maxMessagesPerTask) {
		this.maxMessagesPerTask = maxMessagesPerTask;
	}

	public boolean addTarget(MessageTarget target) {
		return this.dispatcher.addTarget(target);
	}

	public boolean removeTarget(MessageTarget target) {
		return this.dispatcher.removeTarget(target);
	}

	public Schedule getSchedule() {
        return this.schedule;
    }

	public void run() {
		int count = 0;
		while (this.maxMessagesPerTask <= 0 || count < this.maxMessagesPerTask) {
			if (!this.dispatch()) {
				return;
			}
			count++;
		}
	}

	private boolean dispatch() {
		final Message<?> message = (this.source instanceof BlockingSource && this.receiveTimeout >= 0)
				? ((BlockingSource<?>) this.source).receive(this.receiveTimeout)
				: this.source.receive();
		if (message == null) {
			return false;
		}
		return this.dispatcher.send(message);
	}

}
