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

import java.util.ArrayList;
import java.util.List;

import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageDeliveryAware;
import org.springframework.integration.message.MessageDeliveryException;
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

	private final PollingSchedule schedule;

	private volatile long sendTimeout = 0;

	private volatile int maxMessagesPerTask = 1;


	public PollingSourceEndpoint(PollableSource<?> source, MessageChannel channel, PollingSchedule schedule) {
		super(source, channel);
		Assert.notNull(schedule, "schedule must not be null");
		this.schedule = schedule;
	}


	public void setSendTimeout(long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	public void setMaxMessagesPerTask(int maxMessagesPerTask) {
		Assert.isTrue(maxMessagesPerTask > 0, "'maxMessagesPerTask' must be at least one");
		this.maxMessagesPerTask = maxMessagesPerTask;
	}

	public Schedule getSchedule() {
		return this.schedule;
	}

	public List<Message<?>> poll(int limit) {
		List<Message<?>> results = new ArrayList<Message<?>>();
		int count = 0;
		while (count < limit) {
			Message<?> message = ((PollableSource<?>) this.getSource()).receive();
			if (message == null) {
				break;
			}
			results.add(message);
			count++;
		}
		return results;
	}

	protected boolean sendMessage(Message<?> message) {
		if (message == null) {
			throw new IllegalArgumentException("message must not be null");
		}
		boolean sent = (this.sendTimeout < 0) ? this.getChannel().send(message) : this.getChannel().send(message, this.sendTimeout);
		if (this.getSource() instanceof MessageDeliveryAware) {
			if (sent) {
				((MessageDeliveryAware) this.getSource()).onSend(message);
			}
			else {
				((MessageDeliveryAware) this.getSource()).onFailure(new MessageDeliveryException(message, "failed to send message"));
			}
		}
		return sent;
	}

	public void run() {
		int messagesProcessed = 0;
		List<Message<?>> messages = this.poll(this.maxMessagesPerTask);
		for (Message<?> message : messages) {
			if (this.sendMessage(message)) {
				messagesProcessed++;
			}
			else {
				break;
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("polling source task processed " + messagesProcessed + " messages");
		}
	}

}
