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

import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.bus.MessageBusAware;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.MessageChannelTemplate;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.endpoint.MessagingGateway;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.util.Assert;

/**
 * A convenient base class for connecting application code to
 * {@link MessageChannel}s for sending, receiving, or request-reply operations.
 * Exposes setters for configuring request and reply {@link MessageChannel}s as
 * well as the timeout values for sending and receiving Messages.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractMessagingGateway implements MessagingGateway, MessageBusAware {

	private volatile MessageChannel requestChannel;

	private volatile MessageChannel replyChannel;

	private final MessageChannelTemplate channelTemplate = new MessageChannelTemplate();

	private volatile ReplyMessageCorrelator replyMessageCorrelator;

	private volatile MessageBus messageBus;

	private final Object replyMessageCorrelatorMonitor = new Object();



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
	 * Set the timeout value for sending request messages. If not
	 * explicitly configured, the default is an indefinite timeout.
	 * 
	 * @param requestTimeout the timeout value in milliseconds
	 */
	public void setRequestTimeout(long requestTimeout) {
		this.channelTemplate.setSendTimeout(requestTimeout);
	}

	/**
	 * Set the timeout value for receiving reply messages. If not
	 * explicitly configured, the default is an indefinite timeout.
	 * 
	 * @param replyTimeout the timeout value in milliseconds
	 */
	public void setReplyTimeout(long replyTimeout) {
		this.channelTemplate.setReceiveTimeout(replyTimeout);
	}

	public void setMessageBus(MessageBus messageBus) {
		this.messageBus = messageBus;
	}

	public void send(Object object) {
		Assert.state(this.requestChannel != null,
				"send is not supported, because no request channel has been configured");
		Message<?> message = this.toMessage(object);
		Assert.notNull(message, "message must not be null");
		if (!this.channelTemplate.send(message, this.requestChannel)) {
			throw new MessageDeliveryException(message, "failed to send Message to channel");
		}
	}

	public Object receive() {
		Assert.state(this.replyChannel != null && (this.replyChannel instanceof PollableChannel),
				"no-arg receive is not supported, because no pollable reply channel has been configured");
		Message<?> message = this.channelTemplate.receive((PollableChannel) this.replyChannel);
		return this.fromMessage(message);
	}

	public Object sendAndReceive(Object object) {
		return this.sendAndReceive(object, true);
	}

	public Message<?> sendAndReceiveMessage(Object object) {
		return (Message<?>) this.sendAndReceive(object, false);
	}

	private Object sendAndReceive(Object object, boolean shouldMapMessage) {
		Message<?> request = this.toMessage(object);
		Message<?> reply = this.sendAndReceiveMessage(request);
		if (!shouldMapMessage) {
			return reply;
		}
		return this.fromMessage(reply);
	}

	private Message<?> sendAndReceiveMessage(Message<?> message) {
		Assert.notNull(message, "request message must not be null");
		if (this.requestChannel == null) {
			throw new MessageDeliveryException(message,
					"No request channel available. Cannot send request message.");
		}
		if (this.replyChannel != null && this.replyMessageCorrelator == null) {
			this.registerReplyMessageCorrelator();
		}
		return this.channelTemplate.sendAndReceive(message, this.requestChannel);
	}

	private void registerReplyMessageCorrelator() {
		synchronized (this.replyMessageCorrelatorMonitor) {
			if (this.replyMessageCorrelator != null) {
				return;
			}
			Assert.state(this.messageBus != null, "No MessageBus available. Cannot register ReplyMessageCorrelator.");
			ReplyMessageCorrelator correlator = new ReplyMessageCorrelator();
			correlator.setBeanName("internal.correlator." + this);
			correlator.setInputChannel(this.replyChannel);
			correlator.afterPropertiesSet();
			this.messageBus.registerEndpoint(correlator);
			this.replyMessageCorrelator = correlator;
		}
	}

	/**
	 * Subclasses must implement this to map from an Object to a Message.
	 */
	protected abstract Message<?> toMessage(Object object);

	/**
	 * Subclasses must implement this to map from a Message to an Object.
	 */
	protected abstract Object fromMessage(Message<?> message);

}
