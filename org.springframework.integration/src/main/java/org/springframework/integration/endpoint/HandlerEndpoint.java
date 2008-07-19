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

import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.ChannelRegistryAware;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Implementation of the {@link MessageEndpoint} interface for invoking
 * {@link MessageHandler MessageHandlers}.
 * 
 * @author Mark Fisher
 */
public class HandlerEndpoint extends AbstractEndpoint {

	private volatile MessageHandler handler;

	private volatile long replyTimeout = 1000;

	private volatile boolean returnAddressOverrides = false;


	public HandlerEndpoint(MessageHandler handler) {
		Assert.notNull(handler, "handler must not be null");
		this.handler = handler;
	}


	public MessageHandler getHandler() {
		return this.handler;
	}

	/**
	 * Set the timeout in milliseconds to be enforced when this endpoint sends a
	 * reply message. If the message is not sent successfully within the
	 * allotted time, then a MessageDeliveryException will be thrown.
	 * The default <code>replyTimeout</code> value is 1000 milliseconds.
	 */
	public void setReplyTimeout(long replyTimeout) {
		this.replyTimeout = replyTimeout;
	}

	public void setReturnAddressOverrides(boolean returnAddressOverrides) {
		this.returnAddressOverrides = returnAddressOverrides;
	}

	public void initialize() {
		Assert.notNull(this.handler, "handler must not be null");
		if (this.handler instanceof ChannelRegistryAware) {
			((ChannelRegistryAware) this.handler).setChannelRegistry(this.getChannelRegistry());
		}
		super.initialize();
	}

	private MessageChannel resolveReplyChannel(Message<?> originalMessage) {
		if (this.returnAddressOverrides) {
			MessageChannel channel = this.getReturnAddress(originalMessage);
			if (channel == null) {
				channel = this.getOutputChannel();
			}
			return channel;
		}
		else {
			MessageChannel channel = this.getOutputChannel();
			if (channel == null) {
				channel = this.getReturnAddress(originalMessage);
			}
			return channel;
		}
	}

	private MessageChannel getReturnAddress(Message<?> originalMessage) {
		Object returnAddress = originalMessage.getHeaders().getReturnAddress();
		if (returnAddress != null) {
			if (returnAddress instanceof MessageChannel) {
				return (MessageChannel) returnAddress;
			}
			ChannelRegistry registry = this.getChannelRegistry();
			if (returnAddress instanceof String && registry != null) {
				String channelName = (String) returnAddress;
				if (StringUtils.hasText(channelName)) {
					return registry.lookupChannel(channelName);
				}
			}
		}
		return null;
	}

	@Override
	protected Message<?> handleMessage(Message<?> message) {
		Message<?> replyMessage = this.handler.handle(message);
		if (replyMessage != null) {
			Object correlationId = replyMessage.getHeaders().getCorrelationId();
			if (correlationId == null) {
				replyMessage = MessageBuilder.fromMessage(replyMessage)
						.setHeader(MessageHeaders.CORRELATION_ID, message.getId()).build();
			}
			if (replyMessage != null) {
				MessageChannel replyChannel = resolveReplyChannel(replyMessage);
				if (replyChannel == null) {
					replyChannel = resolveReplyChannel(message);
					if (replyChannel == null) {
						throw new MessageHandlingException(replyMessage, "Unable to determine reply channel for message. " +
								"Provide an 'outputChannelName' on the message endpoint or a 'returnAddress' in the message header");
					}
				}
				this.sendReplyMessage(replyMessage, replyChannel);
			}
		}
		return null;
	}

	private void sendReplyMessage(Message<?> replyMessage, MessageChannel replyChannel) {
		if (logger.isDebugEnabled()) {
			logger.debug("endpoint '" + HandlerEndpoint.this + "' replying to channel '" + replyChannel + "' with message: " + replyMessage);
		}
		if (!replyChannel.send(replyMessage, replyTimeout)) {
			throw new MessageDeliveryException(replyMessage,
					"unable to send reply message within alloted timeout of " + replyTimeout + " milliseconds");
		}
	}

}
