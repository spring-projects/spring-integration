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
import org.springframework.integration.handler.ReplyHandler;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.MessageHeader;
import org.springframework.integration.message.Target;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Implementation of the {@link MessageEndpoint} interface for invoking
 * {@link MessageHandler MessageHandlers}.
 * 
 * @author Mark Fisher
 */
public class HandlerEndpoint extends TargetEndpoint {

	private volatile MessageHandler handler;

	private volatile ReplyHandler replyHandler = new EndpointReplyHandler();

	private volatile long replyTimeout = 1000;

	private volatile String outputChannelName;

	private volatile boolean returnAddressOverrides = false;


	public HandlerEndpoint(MessageHandler handler) {
		Assert.notNull(handler, "handler must not be null");
		this.handler = handler;
	}


	public MessageHandler getHandler() {
		return this.handler;
	}

	public void setReplyHandler(ReplyHandler replyHandler) {
		Assert.notNull(replyHandler, "'replyHandler' must not be null");
		this.replyHandler = replyHandler;
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

	/**
	 * Set the name of the channel to which this endpoint should send reply
	 * messages.
	 */
	public void setOutputChannelName(String outputChannelName) {
		this.outputChannelName = outputChannelName;
	}

	public String getOutputChannelName() {
		return this.outputChannelName;
	}

	public void setReturnAddressOverrides(boolean returnAddressOverrides) {
		this.returnAddressOverrides = returnAddressOverrides;
	}

	public void afterPropertiesSet() {
		Assert.notNull(this.handler, "handler must not be null");
		if (this.handler instanceof ChannelRegistryAware) {
			((ChannelRegistryAware) this.handler).setChannelRegistry(this.getChannelRegistry());
		}
		super.setTarget(new HandlerInvokingTarget(this.handler, this.replyHandler));
		super.afterPropertiesSet();
	}

	private MessageChannel resolveReplyChannel(MessageHeader originalMessageHeader) {
		if (this.returnAddressOverrides) {
			MessageChannel channel = this.getReturnAddress(originalMessageHeader);
			if (channel == null) {
				channel = this.getOutputChannel();
			}
			return channel;
		}
		else {
			MessageChannel channel = this.getOutputChannel();
			if (channel == null) {
				channel = this.getReturnAddress(originalMessageHeader);
			}
			return channel;
		}
	}

	private MessageChannel getReturnAddress(MessageHeader originalMessageHeader) {
		Object returnAddress = originalMessageHeader.getReturnAddress();
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

	private MessageChannel getOutputChannel() {
		ChannelRegistry registry = this.getChannelRegistry();
		if (this.outputChannelName != null && registry != null) {
			return registry.lookupChannel(this.outputChannelName);
		}
		return null;
	}


	private static class HandlerInvokingTarget implements Target {

		private final MessageHandler handler;

		private final ReplyHandler replyHandler;


		public HandlerInvokingTarget(MessageHandler handler, ReplyHandler replyHandler) {
			this.handler = handler;
			this.replyHandler = replyHandler;
		}


		public boolean send(Message<?> message) {
			Message<?> replyMessage = this.handler.handle(message);
			if (replyMessage != null) {
				if (replyMessage.getHeader().getCorrelationId() == null) {
					replyMessage.getHeader().setCorrelationId(message.getId());
				}
				this.replyHandler.handle(replyMessage, message.getHeader());
			}
			return true;
		}

	}


	private class EndpointReplyHandler implements ReplyHandler {

		public void handle(Message<?> replyMessage, MessageHeader originalMessageHeader) {
			if (replyMessage == null) {
				return;
			}
			MessageChannel replyChannel = resolveReplyChannel(originalMessageHeader);
			if (replyChannel == null) {
				throw new MessageHandlingException(replyMessage, "Unable to determine reply channel for message. " +
						"Provide an 'outputChannelName' on the message endpoint or a 'returnAddress' in the message header");
			}
			if (logger.isDebugEnabled()) {
				logger.debug("endpoint '" + HandlerEndpoint.this + "' replying to channel '" + replyChannel + "' with message: " + replyMessage);
			}
			if (!replyChannel.send(replyMessage, replyTimeout)) {
				throw new MessageDeliveryException(replyMessage,
						"unable to send reply message within alloted timeout of " + replyTimeout + " milliseconds");
			}
		}
	}

}
