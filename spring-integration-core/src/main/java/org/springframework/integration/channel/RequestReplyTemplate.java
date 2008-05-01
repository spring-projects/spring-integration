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

package org.springframework.integration.channel;

import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.config.MessageBusParser;
import org.springframework.integration.endpoint.EndpointRegistry;
import org.springframework.integration.endpoint.HandlerEndpoint;
import org.springframework.integration.handler.ReplyHandler;
import org.springframework.integration.handler.ResponseCorrelator;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.message.selector.MessageSelector;
import org.springframework.integration.scheduling.Subscription;

/**
 * A template that facilitates the implementation of request-reply usage
 * scenarios above one-way {@link MessageChannel MessageChannels}.
 * 
 * @author Mark Fisher
 */
public class RequestReplyTemplate implements ApplicationContextAware {

	private MessageChannel requestChannel;

	private MessageChannel replyChannel;

	private volatile long requestTimeout = -1;

	private volatile long replyTimeout = -1;

	private ResponseCorrelator responseCorrelator;

	private EndpointRegistry endpointRegistry;

	private final Object responseCorrelatorMonitor = new Object();


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

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		if (applicationContext.containsBean(MessageBusParser.MESSAGE_BUS_BEAN_NAME)) {
			this.setEndpointRegistry((EndpointRegistry) applicationContext.getBean(MessageBusParser.MESSAGE_BUS_BEAN_NAME));
		}
	}

	public boolean send(Message<?> message) {
		if (message == null) {
			throw new MessagingException("Message must not be null.");
		}
		if (this.requestChannel == null) {
			throw new MessagingException("No request channel has been configured. Cannot send message.");
		}
		boolean sent = (this.requestTimeout >= 0) ?
				this.requestChannel.send(message, this.requestTimeout) : this.requestChannel.send(message);
		if (!sent) {
			throw new MessagingException("Failed to send request message.");
		}
		return true;
	}

	public Message<?> receive() {
		if (this.replyChannel == null) {
			throw new MessagingException("No reply channel has been configured. Cannot perform receive only operation.");
		}
		return this.receiveResponse(this.replyChannel);
	}

	/**
	 * Send a request message whose reply should be handled be the provided callback.
	 */
	public boolean request(Message<?> message, ReplyHandler replyHandler) {
		MessageChannel replyChannelAdapter = new ReplyHandlingChannelAdapter(message, replyHandler);
		message.getHeader().setReturnAddress(replyChannelAdapter);
		return this.send(message);
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
			throw new MessagingException("No request channel available. Cannot send request message.");
		}
		if (this.replyChannel != null) {
			return this.sendAndReceiveWithResponseCorrelator(message);
		}
		else {
			return this.sendAndReceiveWithTemporaryChannel(message);
		}
	}

	private Message<?> sendAndReceiveWithResponseCorrelator(Message<?> message) {
		if (this.responseCorrelator == null) {
			this.registerResponseCorrelator();
		}
		message.getHeader().setReturnAddress(this.replyChannel);
		this.send(message);
		return (this.replyTimeout >= 0) ? this.responseCorrelator.getResponse(message.getId(), this.replyTimeout) :
				this.responseCorrelator.getResponse(message.getId());
	}

	private Message<?> sendAndReceiveWithTemporaryChannel(Message<?> message) {
		RendezvousChannel temporaryChannel = new RendezvousChannel();
		message.getHeader().setReturnAddress(temporaryChannel);
		this.send(message);
		return this.receiveResponse(temporaryChannel);
	}

	private Message<?> receiveResponse(MessageChannel channel) {
		return (this.replyTimeout >= 0) ? channel.receive(this.replyTimeout) : channel.receive();
	}

	private void registerResponseCorrelator() {
		synchronized (this.responseCorrelatorMonitor) {
			if (this.responseCorrelator != null) {
				return;
			}
			if (this.endpointRegistry == null) {
				throw new ConfigurationException("No EndpointRegistry available. Cannot register ResponseCorrelator.");
			}
			ResponseCorrelator correlator = new ResponseCorrelator(10);
			HandlerEndpoint endpoint = new HandlerEndpoint(correlator);
			endpoint.setSubscription(new Subscription(this.replyChannel));
			this.endpointRegistry.registerEndpoint("internal.correlator." + this, endpoint);
			this.responseCorrelator = correlator;
		}
	}


	private static class ReplyHandlingChannelAdapter implements MessageChannel {

		private final Message<?> originalMessage;

		private final ReplyHandler replyHandler;


		ReplyHandlingChannelAdapter(Message<?> originalMessage, ReplyHandler replyHandler) {
			this.originalMessage = originalMessage;
			this.replyHandler = replyHandler;
		}


        public List<Message<?>> clear() {
	        return null;
        }

        public DispatcherPolicy getDispatcherPolicy() {
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

        public Message receive() {
	        return null;
        }

        public Message receive(long timeout) {
	        return null;
        }

        public boolean send(Message<?> message) {
	        this.replyHandler.handle(message, originalMessage.getHeader());
	        return true;
        }

        public boolean send(Message<?> message, long timeout) {
	        return this.send(message);
        }

	}

}
