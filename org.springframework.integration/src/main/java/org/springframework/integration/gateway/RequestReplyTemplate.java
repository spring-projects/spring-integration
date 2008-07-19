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

import java.util.List;

import org.springframework.integration.ConfigurationException;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.bus.MessageBusAware;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.RendezvousChannel;
import org.springframework.integration.endpoint.EndpointRegistry;
import org.springframework.integration.endpoint.HandlerEndpoint;
import org.springframework.integration.handler.ReplyMessageCorrelator;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.MessageTarget;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.message.selector.MessageSelector;

/**
 * A template that facilitates the implementation of request-reply usage
 * scenarios above one-way {@link MessageChannel MessageChannels}.
 * 
 * @author Mark Fisher
 */
public class RequestReplyTemplate implements MessageBusAware {

	private MessageChannel requestChannel;

	private MessageChannel replyChannel;

	private volatile long requestTimeout = -1;

	private volatile long replyTimeout = -1;

	private ReplyMessageCorrelator replyMessageCorrelator;

	private EndpointRegistry endpointRegistry;

	private final Object replyMessageCorrelatorMonitor = new Object();


	/**
	 * Create a RequestReplyTemplate.
	 * 
	 * @param requestChannel the channel to which request messages will be sent
	 * @param replyChannel the channel from which reply messages will be received
	 */
	public RequestReplyTemplate(MessageChannel requestChannel, MessageChannel replyChannel) {
		this.requestChannel = requestChannel;
		this.replyChannel = replyChannel;
	}

	/**
	 * Create a RequestReplyTemplate that will use anonymous temporary channels for replies.
	 * 
	 * @param requestChannel the channel to which request messages will be sent
	 */
	public RequestReplyTemplate(MessageChannel requestChannel) {
		this(requestChannel, null);
	}

	public RequestReplyTemplate() {
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
	public void setReplyChannel(MessageChannel replyChannel) {
		this.replyChannel = replyChannel;
	}

	/**
	 * Set the timeout value for sending request messages. If not
	 * explicitly configured, the default is an indefinite timeout.
	 * 
	 * @param requestTimeout the timeout value in milliseconds
	 */
	public void setRequestTimeout(long requestTimeout) {
		this.requestTimeout = requestTimeout;
	}

	/**
	 * Set the timeout value for receiving reply messages. If not
	 * explicitly configured, the default is an indefinite timeout.
	 * 
	 * @param replyTimeout the timeout value in milliseconds
	 */
	public void setReplyTimeout(long replyTimeout) {
		this.replyTimeout = replyTimeout;
	}

	public void setEndpointRegistry(EndpointRegistry endpointRegistry) {
		this.endpointRegistry = endpointRegistry;
	}

	public void setMessageBus(MessageBus messageBus) {
		if (this.endpointRegistry == null) {
			this.setEndpointRegistry(messageBus);
		}
	}

	public boolean send(Message<?> message) {
		if (message == null) {
			throw new MessageDeliveryException(message, "Message must not be null.");
		}
		if (this.requestChannel == null) {
			throw new MessageDeliveryException(message,
					"No request channel has been configured. Cannot send message.");
		}
		boolean sent = (this.requestTimeout >= 0) ?
				this.requestChannel.send(message, this.requestTimeout) : this.requestChannel.send(message);
		if (!sent) {
			throw new MessageDeliveryException(message, "Failed to send request message.");
		}
		return true;
	}

	public Message<?> receive() {
		if (this.replyChannel == null) {
			throw new MessagingException(
					"No reply channel has been configured. Cannot perform receive only operation.");
		}
		return this.receiveResponse(this.replyChannel);
	}

	/**
	 * Send a request message whose reply should be sent to the provided target.
	 */
	public boolean request(Message<?> message, MessageTarget target) {
		MessageChannel replyChannelAdapter = new ReplyHandlingChannelAdapter(target);
		Message<?> requestMessage = MessageBuilder.fromMessage(message)
				.setReturnAddress(replyChannelAdapter).build();
		return this.send(requestMessage);
	}

	/**
	 * Send a request message and wait for a reply message using the configured
	 * timeout values.
	 * 
	 * @param requestMessage the request message to send
	 * 
	 * @return the reply message or <code>null</code>
	 */
	public Message<?> request(Message<?> message) {
		if (this.requestChannel == null) {
			throw new MessageDeliveryException(message,
					"No request channel available. Cannot send request message.");
		}
		if (this.replyChannel != null) {
			return this.sendAndReceiveWithReplyMessageCorrelator(message);
		}
		else {
			return this.sendAndReceiveWithTemporaryChannel(message);
		}
	}

	private Message<?> sendAndReceiveWithReplyMessageCorrelator(Message<?> message) {
		if (this.replyMessageCorrelator == null) {
			this.registerReplyMessageCorrelator();
		}
		message = MessageBuilder.fromMessage(message)
				.setReturnAddress(this.replyChannel).build();
		this.send(message);
		return (this.replyTimeout >= 0) ? this.replyMessageCorrelator.getReply(message.getId(), this.replyTimeout) :
				this.replyMessageCorrelator.getReply(message.getId());
	}

	private Message<?> sendAndReceiveWithTemporaryChannel(Message<?> message) {
		RendezvousChannel temporaryChannel = new RendezvousChannel();
		Message<?> requestMessage = MessageBuilder.fromMessage(message)
				.setReturnAddress(temporaryChannel).build();
		this.send(requestMessage);
		return this.receiveResponse(temporaryChannel);
	}

	private Message<?> receiveResponse(MessageChannel channel) {
		return (this.replyTimeout >= 0) ? channel.receive(this.replyTimeout) : channel.receive();
	}

	private void registerReplyMessageCorrelator() {
		synchronized (this.replyMessageCorrelatorMonitor) {
			if (this.replyMessageCorrelator != null) {
				return;
			}
			if (this.endpointRegistry == null) {
				throw new ConfigurationException("No EndpointRegistry available. Cannot register ResponseCorrelator.");
			}
			ReplyMessageCorrelator correlator = new ReplyMessageCorrelator(10);
			HandlerEndpoint endpoint = new HandlerEndpoint(correlator);
			endpoint.setSource(this.replyChannel);
			endpoint.setName("internal.correlator." + this);
			this.endpointRegistry.registerEndpoint(endpoint);
			this.replyMessageCorrelator = correlator;
		}
	}


	private static class ReplyHandlingChannelAdapter implements MessageChannel {

		private final MessageTarget target;


		ReplyHandlingChannelAdapter(MessageTarget target) {
			this.target = target;
		}


        public List<Message<?>> clear() {
	        return null;
        }

        public String getName() {
	        return null;
        }

        public List<Message<?>> purge(MessageSelector selector) {
	        return null;
        }

        public void setName(String name) {
        }

        public Message<?> receive() {
	        return null;
        }

        public Message<?> receive(long timeout) {
	        return null;
        }

        public boolean send(Message<?> message) {
	        this.target.send(message);
	        return true;
        }

        public boolean send(Message<?> message, long timeout) {
	        return this.send(message);
        }

	}

}
