/*
 * Copyright 2002-2007 the original author or authors.
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

import java.util.Collection;

import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.bus.ConsumerPolicy;
import org.springframework.integration.bus.MessageDispatcher;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageMapper;
import org.springframework.integration.message.SimplePayloadMessageMapper;
import org.springframework.util.Assert;

/**
 * A channel adapter that retrieves objects from a {@link PollableSource},
 * delegates to a {@link MessageMapper} to create messages from those objects,
 * and then sends the resulting messages to the provided {@link MessageChannel}.
 * 
 * @author Mark Fisher
 */
public class PollingSourceAdapter<T> implements SourceAdapter, MessageDispatcher {

	private static int DEFAULT_PERIOD = 1000;

	private PollableSource<T> source;

	private MessageChannel channel;

	private MessageMapper<?,T> mapper = new SimplePayloadMessageMapper<T>();

	private ConsumerPolicy policy = ConsumerPolicy.newPollingPolicy(DEFAULT_PERIOD);

	private long sendTimeout = -1;


	public PollingSourceAdapter(PollableSource<T> source) {
		Assert.notNull(source, "'source' must not be null");
		this.source = source;
	}

	public void setChannel(MessageChannel channel) {
		Assert.notNull(channel, "'channel' must not be null");
		this.channel = channel;
	}

	public void setPeriod(int period) {
		Assert.isTrue(period > 0, "'period' must be a positive value");
		this.policy.setPeriod(period);
	}

	public void setSendTimeout(long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	public void setMessageMapper(MessageMapper<?,T> mapper) {
		Assert.notNull(mapper, "'mapper' must not be null");
		this.mapper = mapper;
	}

	protected MessageMapper<?,T> getMessageMapper() {
		return this.mapper;
	}

	public void setMaxMessagesPerTask(int maxMessagesPerTask) {
		Assert.isTrue(maxMessagesPerTask > 0, "'maxMessagesPerTask' must be a positive value");
		this.policy.setMaxMessagesPerTask(maxMessagesPerTask);
	}

	public ConsumerPolicy getConsumerPolicy() {
		return this.policy;
	}

	public int receiveAndDispatch() {
		int messagesProcessed = 0;
		int limit = this.policy.getMaxMessagesPerTask();
		Collection<T> results = this.source.poll(limit);
		if (results != null) {
			if (results.size() > limit) {
				throw new MessageHandlingException("source returned too many results, the limit is " + limit);
			}
			for (T next : results) {
				Message<?> message = this.mapper.toMessage(next);
				if (this.sendTimeout < 0) {
					if (this.channel.send(message)) {
						messagesProcessed++;
					}
				}
				else {
					if (this.channel.send(message, this.sendTimeout)) {
						messagesProcessed++;
					}
				}
			}
		}
		return messagesProcessed;
	}

}
