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

import java.util.LinkedList;
import java.util.List;

import org.springframework.integration.channel.DispatcherPolicy;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.BlockingSource;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageDeliveryAware;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.PollableSource;
import org.springframework.integration.scheduling.MessagingTask;
import org.springframework.integration.scheduling.Schedule;
import org.springframework.util.Assert;

/**
 * A subclass of {@link SimpleDispatcher} that adds message retrieval
 * capabilities and may be scheduled to run as a task. It polls a source for
 * {@link Message Messages}. The number of messages retrieved per poll is
 * limited by the '<em>maxMessagesPerTask</em>' property of the provided
 * {@link DispatcherPolicy}, and the timeout for each receive call is
 * determined by the policy's '<em>receiveTimeout</em>' property. In
 * general, it is recommended to use a value of 1 (the default) for
 * 'maxMessagesPerTask' whenever a significant timeout is provided. Otherwise
 * the poller may be holding on to available messages while waiting for
 * additional messages. Note that the 'timeout' value is only relevant if the
 * specified source is an implementation of {@link BlockingSource}. The default
 * timeout value is 0 indicating that the method should return immediately
 * rather than waiting for a {@link Message} to become available.
 * 
 * @author Mark Fisher
 */
public class PollingDispatcher extends SimpleDispatcher implements MessagingTask {

	private final PollableSource<?> source;

	private final Schedule schedule;


	public PollingDispatcher(MessageChannel channel, Schedule schedule) {
		this(channel, channel.getDispatcherPolicy(), schedule);
	}

	public PollingDispatcher(PollableSource<?> source, DispatcherPolicy dispatcherPolicy, Schedule schedule) {
		super(dispatcherPolicy);
		Assert.notNull(source, "source must not be null");
		this.source = source;
		this.schedule = schedule;
		this.dispatcherPolicy.setReceiveTimeout(0);
	}


	public Schedule getSchedule() {
		return this.schedule;
	}

	public List<Message<?>> poll() {
		List<Message<?>> messages = new LinkedList<Message<?>>();
		int limit = this.dispatcherPolicy.getMaxMessagesPerTask();
		while (messages.size() < limit) {
			Message<?> message = null;
			long timeout = this.dispatcherPolicy.getReceiveTimeout();
			if (this.source instanceof BlockingSource && timeout >= 0) {
				message = ((BlockingSource<?>) this.source).receive(timeout);
			}
			else {
				message = this.source.receive();
			}
			if (message == null) {
				return messages;
			}
			messages.add(message);
		}
		return messages;
	}

	public void run() {
		List<Message<?>> messages = this.poll();
		for (Message<?> message : messages) {
			boolean sent = this.dispatch(message);
			if (this.source instanceof MessageDeliveryAware) {
				if (sent) {
					((MessageDeliveryAware) this.source).onSend(message);
				}
				else {
					((MessageDeliveryAware) this.source).onFailure(new MessageDeliveryException(message, "failed to send message"));
				}
			}
		}
	}

}
