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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.Message;
import org.springframework.util.Assert;

/**
 * Message retriever that polls a {@link MessageChannel}. The number of
 * messages retrieved per poll is limited by the '<em>maxMessagesPerTask</em>'
 * property of the channel's dispatcher policy, and the timeout for each receive
 * call is determined by the dispatcher policy's '<em>receiveTimeout</em>'
 * property. In general, it is recommended to use a value of 1 for
 * 'maxMessagesPerTask' whenever a significant timeout is provided. Otherwise the
 * retriever may be holding on to available messages while waiting for
 * additional messages.
 * 
 * @author Mark Fisher
 */
public class ChannelPollingMessageRetriever implements MessageRetriever {

	private MessageChannel channel;


	public ChannelPollingMessageRetriever(MessageChannel channel) {
		Assert.notNull(channel, "'channel' must not be null");
		this.channel = channel;
	}


	public Collection<Message<?>> retrieveMessages() {
		List<Message<?>> messages = new LinkedList<Message<?>>();
		while (messages.size() < this.channel.getDispatcherPolicy().getMaxMessagesPerTask()) {
			Message<?> message = null;
			long timeout = this.channel.getDispatcherPolicy().getReceiveTimeout();
			if (timeout < 0) {
				message = this.channel.receive();
			}
			else {
				message = this.channel.receive(timeout);
			}
			if (message == null) {
				return messages;
			}
			messages.add(message);
		}
		return messages;
	}

}
