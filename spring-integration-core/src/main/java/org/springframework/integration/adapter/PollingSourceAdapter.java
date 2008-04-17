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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.dispatcher.SynchronousChannel;
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
public class PollingSourceAdapter extends AbstractSourceAdapter implements MessagingTask, InitializingBean {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final PollableSource<?> source;

	private final PollingSchedule schedule;

	private volatile int maxMessagesPerTask = 1;

	private volatile boolean initialized;


	/**
	 * Create a new adapter for the given source.
	 */
	public PollingSourceAdapter(PollableSource<?> source, MessageChannel channel, PollingSchedule schedule) {
		super(channel);
		Assert.notNull(source, "source must not be null");
		Assert.notNull(schedule, "schedule must not be null");
		this.source = source;
		this.schedule = schedule;
	}


	public void setMaxMessagesPerTask(int maxMessagesPerTask) {
		Assert.isTrue(maxMessagesPerTask > 0, "'maxMessagesPerTask' must be at least one");
		this.maxMessagesPerTask = maxMessagesPerTask;
	}

	public Schedule getSchedule() {
		return this.schedule;
	}

	public void afterPropertiesSet() {
		if (this.getChannel() instanceof SynchronousChannel) {
			((SynchronousChannel) this.getChannel()).setSource(this.source);
		}
		this.initialized = true;
	}

	public List<Message<?>> poll(int limit) {
		List<Message<?>> results = new ArrayList<Message<?>>();
		int count = 0;
		while (count < limit) {
			Message<?> message = this.source.receive();
			if (message == null) {
				break;
			}
			results.add(message);
			count++;
		}
		return results;
	}

	protected boolean sendMessage(Message<?> message) {
		if (!this.initialized) {
			this.afterPropertiesSet();
		}
		boolean sent = super.sendToChannel(message);
		if (this.source instanceof MessageDeliveryAware) {
			if (sent) {
				((MessageDeliveryAware) this.source).onSend(message);
			}
			else {
				((MessageDeliveryAware) this.source).onFailure(new MessageDeliveryException(message, "failed to send message"));
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
