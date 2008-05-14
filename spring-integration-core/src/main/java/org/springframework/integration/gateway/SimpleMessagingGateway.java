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

package org.springframework.integration.gateway;

import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.DefaultMessageCreator;
import org.springframework.integration.message.DefaultMessageMapper;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageCreator;
import org.springframework.integration.message.MessageMapper;
import org.springframework.util.Assert;

/**
 * A general purpose class that supports a variety of message exchanges. Useful for connecting application code to
 * {@link MessageChannel MessageChannels} for sending, receiving, or request-reply operations. The sending methods
 * accept any Object as the parameter value (i.e. it is not required to be a Message). A custom {@link MessageCreator}
 * may be provided for creating Messages from the Objects. Likewise return values may be any Object and a custom
 * implementation of the {@link MessageMapper} strategy may be provided for mapping a reply Message to an Object.
 * 
 * @author Mark Fisher
 */
public class SimpleMessagingGateway extends MessagingGatewaySupport {

	private MessageCreator messageCreator = new DefaultMessageCreator();

	private MessageMapper messageMapper = new DefaultMessageMapper();


	public SimpleMessagingGateway(MessageChannel requestChannel) {
		super(requestChannel);
	}

	public SimpleMessagingGateway() {
		super();
	}


	public void setMessageCreator(MessageCreator<?, ?> messageCreator) {
		Assert.notNull(messageCreator, "messageCreator must not be null");
		this.messageCreator = messageCreator;
	}

	public void setMessageMapper(MessageMapper<?, ?> messageMapper) {
		Assert.notNull(messageMapper, "messageMapper must not be null");
		this.messageMapper = messageMapper;
	}

	public boolean send(Object object) {
		Message<?> message = (object instanceof Message) ? (Message) object :
				this.messageCreator.createMessage(object);
		if (message == null) {
			return false;
		}
		return this.getRequestReplyTemplate().send(message);
	}

	public Object receive() {
		Message<?> message = this.getRequestReplyTemplate().receive();
		return (message != null) ? this.messageMapper.mapMessage(message) : null;
	}

	public Object sendAndReceive(Object object) {
		return this.sendAndReceive(object, true);
	}

	public Message<?> sendAndReceiveMessage(Object object) {
		return (Message<?>) this.sendAndReceive(object, false);
	}

	private Object sendAndReceive(Object object, boolean shouldMapMessage) {
		Message<?> request = (object instanceof Message) ? (Message) object :
				this.messageCreator.createMessage(object);
		if (request == null) {
			return null;
		}
		Message<?> reply = this.getRequestReplyTemplate().request(request);
		if (!shouldMapMessage) {
			return reply;
		}
		return (reply != null) ? this.messageMapper.mapMessage(reply) : null;
	}

}
