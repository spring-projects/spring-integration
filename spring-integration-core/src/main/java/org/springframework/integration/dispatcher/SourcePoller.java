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
import org.springframework.integration.message.PollableSource;
import org.springframework.util.Assert;

/**
 * Polls a source for {@link Message Messages}. The number of messages
 * retrieved per poll is limited by the '<em>maxMessagesPerTask</em>'
 * property, and the timeout for each receive call is determined by the '<em>timeout</em>'
 * property. In general, it is recommended to use a value of 1 (the default) for
 * 'maxMessagesPerTask' whenever a significant timeout is provided. Otherwise
 * the poller may be holding on to available messages while waiting for
 * additional messages. Note that the 'timeout' value is only relevant if the
 * specified source is an implementation of {@link BlockingSource}. The default
 * timeout value is 0 indicating that the method should return immediately
 * rather than waiting for a {@link Message} to become available.
 * 
 * @author Mark Fisher
 */
public class SourcePoller {

	private final PollableSource<?> source;

	private volatile int maxMessagesPerTask = 1;

	private volatile long timeout = 0;


	public SourcePoller(PollableSource<?> source) {
		Assert.notNull(source, "source must not be null");
		if (source instanceof MessageChannel) {
			DispatcherPolicy dispatcherPolicy = ((MessageChannel) source).getDispatcherPolicy();
			this.setMaxMessagesPerTask(dispatcherPolicy.getMaxMessagesPerTask());
			this.setTimeout(dispatcherPolicy.getReceiveTimeout());
		}
		this.source = source;
	}


	public void setMaxMessagesPerTask(int maxMessagesPerTask) {
		Assert.isTrue(maxMessagesPerTask > 0, "'maxMessagesPerTask' must be a positive value");
		this.maxMessagesPerTask = maxMessagesPerTask;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	public List<Message<?>> poll() {
		List<Message<?>> messages = new LinkedList<Message<?>>();
		while (messages.size() < this.maxMessagesPerTask) {
			Message<?> message = null;
			if (this.source instanceof BlockingSource && this.timeout >= 0) {
				message = ((BlockingSource<?>) this.source).receive(this.timeout);
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

}
