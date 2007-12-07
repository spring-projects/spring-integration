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

package org.springframework.integration.endpoint;

import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.MessageSource;
import org.springframework.integration.MessageTarget;
import org.springframework.integration.channel.ChannelResolver;
import org.springframework.integration.channel.consumer.ConsumerType;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.Message;

/**
 * A generic endpoint implementation designed to accommodate a variety of
 * strategies including:
 * <ul>
 *   <li><i>source channel-adapter:</i> source adapter + target channel</li>
 *   <li><i>target channel-adapter:</i> source channel + target adapter</li>
 *   <li><i>one-way:</i> source + handler that returns null and no target</li>
 *   <li><i>request-reply:</i> source + handler and either a reply channel
 *   specified on the request message or a default target on the endpoint</i>
 * </ul>
 * 
 * @author Mark Fisher
 */
public class GenericMessageEndpoint implements MessageEndpoint {

	private MessageSource source;

	private MessageTarget target;

	private MessageHandler handler;

	private ChannelResolver channelResolver;

	private ConsumerType consumerType = ConsumerType.EVENT_DRIVEN;


	/**
	 * Set the source from which this endpoint receives messages.
	 */
	public void setSource(MessageSource source) {
		this.source = source;
	}

	/**
	 * Return the source from which this endpoint receives messages.
	 */
	public MessageSource getSource() {
		return this.source;
	}

	/**
	 * Set the target to which this endpoint can send messages.
	 */
	public void setTarget(MessageTarget target) {
		this.target = target;
	}

	/**
	 * Set a handler to be invoked for each consumed message.
	 */
	public void setHandler(MessageHandler handler) {
		this.handler = handler;
	}

	/**
	 * Set the type of consumer to use for this endpoint.
	 */
	public void setConsumerType(ConsumerType consumerType) {
		this.consumerType = consumerType;
	}

	/**
	 * Return the type of consumer to use for this endpoint.
	 */
	public ConsumerType getConsumerType() {
		return this.consumerType;
	}

	/**
	 * Set the channel resolver strategy to use when a message
	 * provides a '<i>replyChannelName</i>'.
	 */
	public void setChannelResolver(final ChannelResolver channelResolver) {
		this.channelResolver = channelResolver;
	}


	public void messageReceived(Message message) {
		if (this.handler == null) {
			target.send(message);
		}
		Message replyMessage = handler.handle(message);
		if (replyMessage != null) {
			MessageTarget replyTarget = resolveReplyTarget(message);
			if (replyTarget == null) {
				throw new MessageHandlingException("Unable to determine reply target for message. "
						+ "Provide a 'replyChannelName' in the message header or a 'target' "
						+ "on the message endpoint.");
			}
			replyTarget.send(replyMessage);
		}
	}

	private MessageTarget resolveReplyTarget(Message message) {
		MessageTarget replyTo = null;
		if (this.channelResolver != null) {
			String replyChannelName = message.getHeader().getReplyChannelName();
			if (replyChannelName != null && replyChannelName.trim().length() > 0) {
				replyTo = this.channelResolver.resolve(replyChannelName);
			}
		}
		return (replyTo != null ? replyTo : target);
	}

}
