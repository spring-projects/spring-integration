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

import org.springframework.integration.ConfigurationException;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.bus.MessageBusAware;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.endpoint.EndpointRegistry;
import org.springframework.integration.endpoint.MessagingGateway;
import org.springframework.integration.endpoint.SimpleEndpoint;
import org.springframework.integration.handler.ReplyMessageCorrelator;
import org.springframework.integration.message.DefaultMessageCreator;
import org.springframework.integration.message.DefaultMessageMapper;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageCreator;
import org.springframework.integration.message.MessageDeliveryException;
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
public class SimpleMessagingGateway extends MessagingGatewaySupport implements MessagingGateway, MessageBusAware {

	private volatile MessageChannel requestChannel;

	private volatile PollableChannel replyChannel;

	private volatile long replyTimeout = 5000;

	private volatile int replyMapCapacity = 100;

	private volatile MessageCreator messageCreator = new DefaultMessageCreator();

	private volatile MessageMapper messageMapper = new DefaultMessageMapper();

	private volatile ReplyMessageCorrelator replyMessageCorrelator;

	private volatile EndpointRegistry endpointRegistry;

	private final Object replyMessageCorrelatorMonitor = new Object();


	public SimpleMessagingGateway(MessageChannel requestChannel) {
		this.requestChannel = requestChannel;
	}

	public SimpleMessagingGateway() {
		super();
	}


	/**
	 * Set the request channel.
	 * 
	 * @param requestChannel the channel to which request messages will be sent
	 */
	public void setRequestChannel(MessageChannel requestChannel) {
		this.requestChannel = requestChannel;
	}

	/**
	 * Set the reply channel. If no reply channel is provided, this template will
	 * always use an anonymous, temporary channel for handling replies.
	 * 
	 * @param replyChannel the channel from which reply messages will be received
	 */
	public void setReplyChannel(PollableChannel replyChannel) {
		this.replyChannel = replyChannel;
	}

	/**
	 * Set the max capacity for the map that is used to store replies
	 * until requested by the correlationId. The default value is 100.
	 */
	public void setReplyMapCapacity(int replyMapCapacity) {
		this.replyMapCapacity = replyMapCapacity;
	}

	public void setMessageCreator(MessageCreator<?, ?> messageCreator) {
		Assert.notNull(messageCreator, "messageCreator must not be null");
		this.messageCreator = messageCreator;
	}

	public void setMessageMapper(MessageMapper<?, ?> messageMapper) {
		Assert.notNull(messageMapper, "messageMapper must not be null");
		this.messageMapper = messageMapper;
	}

	public void setMessageBus(MessageBus messageBus) {
		this.endpointRegistry = messageBus;
	}

	public void setReplyTimeout(long replyTimeout) {
		this.replyTimeout = replyTimeout;
		super.setReplyTimeout(replyTimeout);
	}

	public void send(Object object) {
		if (this.requestChannel == null) {
			throw new IllegalStateException(
					"send is not supported, because no request channel has been configured");
		}
		Message<?> message = (object instanceof Message) ? (Message) object :
				this.messageCreator.createMessage(object);
		if (message != null) {
			if (!this.getMessageExchangeTemplate().send(message, this.requestChannel)) {
				throw new MessageDeliveryException(message, "failed to send Message to channel");
			}
		}
	}

	public Object receive() {
		if (this.replyChannel == null) {
			throw new IllegalStateException(
					"no-arg receive is not supported, because no reply channel has been configured");
		}
		Message<?> message = this.getMessageExchangeTemplate().receive(this.replyChannel);
		return (message != null) ? this.messageMapper.mapMessage(message) : null;
	}

	public void receiveAndForward() {
		if (this.replyChannel == null || this.requestChannel == null) {
			throw new IllegalStateException(
					"receiveAndForward is not supported, because either the request or reply channel"
					+ " has not been configured");
		}
		this.getMessageExchangeTemplate().receiveAndForward(this.replyChannel, this.requestChannel);
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
		Message<?> reply = this.sendAndReceiveMessage(request);
		if (!shouldMapMessage) {
			return reply;
		}
		return (reply != null) ? this.messageMapper.mapMessage(reply) : null;
	}

	private Message<?> sendAndReceiveMessage(Message<?> message) {
		if (this.requestChannel == null) {
			throw new MessageDeliveryException(message,
					"No request channel available. Cannot send request message.");
		}
		if (this.replyChannel != null) {
			return this.sendAndReceiveWithReplyMessageCorrelator(message);
		}
		else {
			return this.getMessageExchangeTemplate().sendAndReceive(message, this.requestChannel);
		}
	}

	private Message<?> sendAndReceiveWithReplyMessageCorrelator(Message<?> message) {
		if (this.replyMessageCorrelator == null) {
			this.registerReplyMessageCorrelator();
		}
		message = MessageBuilder.fromMessage(message).setReturnAddress(this.replyChannel).build();
		this.send(message);
		return (this.replyTimeout >= 0)
				? this.replyMessageCorrelator.getReply(message.getHeaders().getId(), this.replyTimeout)
				: this.replyMessageCorrelator.getReply(message.getHeaders().getId());
	}

	private void registerReplyMessageCorrelator() {
		synchronized (this.replyMessageCorrelatorMonitor) {
			if (this.replyMessageCorrelator != null) {
				return;
			}
			if (this.endpointRegistry == null) {
				throw new ConfigurationException("No EndpointRegistry available. Cannot register ReplyMessageCorrelator.");
			}
			ReplyMessageCorrelator correlator = new ReplyMessageCorrelator(this.replyMapCapacity);
			SimpleEndpoint<ReplyMessageCorrelator> endpoint = new SimpleEndpoint<ReplyMessageCorrelator>(correlator);
			endpoint.setBeanName("internal.correlator." + this);
			endpoint.setSource(this.replyChannel);
			this.endpointRegistry.registerEndpoint(endpoint);
			this.replyMessageCorrelator = correlator;
		}
	}

}
