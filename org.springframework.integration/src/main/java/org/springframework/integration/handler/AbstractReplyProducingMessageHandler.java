/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.handler;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.integration.channel.BeanFactoryChannelResolver;
import org.springframework.integration.channel.ChannelResolver;
import org.springframework.integration.channel.MessageChannelTemplate;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.core.MessageHeaders;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.util.Assert;

/**
 * Base class for MessageHandlers that are capable of producing replies.
 *
 * @author Mark Fisher
 * @author Iwein Fuld
 */
public abstract class AbstractReplyProducingMessageHandler extends AbstractMessageHandler implements BeanFactoryAware {

	public static final long DEFAULT_SEND_TIMEOUT = 1000;


	private MessageChannel outputChannel;

	private volatile ChannelResolver channelResolver;

	private volatile boolean requiresReply = false;

	private final MessageChannelTemplate channelTemplate;


	public AbstractReplyProducingMessageHandler() {
		this.channelTemplate = new MessageChannelTemplate();
		this.channelTemplate.setSendTimeout(DEFAULT_SEND_TIMEOUT);
	}


	public void setOutputChannel(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
	}

	protected MessageChannel getOutputChannel() {
		return this.outputChannel;
	}

	/**
	 * Set the timeout for sending reply Messages.
	 */
	public void setSendTimeout(long sendTimeout) {
		this.channelTemplate.setSendTimeout(sendTimeout);
	}

	/**
	 * Set the ChannelResolver to be used when there is no default output channel.
	 */
	public void setChannelResolver(ChannelResolver channelResolver) {
		Assert.notNull(channelResolver, "'channelResolver' must not be null");
		this.channelResolver = channelResolver;
	}

	/**
	 * Flag wether reply is required. If true an incoming message MUST result in a reply message being sent.
	 * If false an incoming message MAY result in a reply message being sent
	 */
	public void setRequiresReply(boolean requiresReply) {
		this.requiresReply = requiresReply;
	}

	public void setBeanFactory(BeanFactory beanFactory) {
		if (this.channelResolver == null) {
			this.channelResolver = new BeanFactoryChannelResolver(beanFactory);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected final void handleMessageInternal(Message<?> message) {
		Object result = this.handleRequestMessage(message);
		if (result == null) {
			if (this.requiresReply) {
				throw new MessageHandlingException(message, "handler '" + this
						+ "' requires a reply, but no reply was received");
			}
			if (logger.isDebugEnabled()) {
				logger.debug("handler '" + this + "' produced no reply for request Message: " + message);
			}
			return;
		}
		MessageChannel replyChannel = resolveReplyChannel(message, this.outputChannel, this.channelResolver);
		MessageHeaders requestHeaders = message.getHeaders();
		if (result instanceof Iterable) {
			this.sendReplyMessages((Iterable) result, requestHeaders, replyChannel);
		}
		else {
			Message<?> replyMessage = this.createReplyMessage(result, requestHeaders);
			if (!this.sendReplyMessage(replyMessage, replyChannel)) {
				throw new MessageDeliveryException(replyMessage,
						"failed to send reply Message to channel '" + replyChannel + "'");
			}
		}
	}

	private void sendReplyMessages(Iterable<?> replies, MessageHeaders requestHeaders, MessageChannel replyChannel) {
		for (Object reply : replies) {
			Message<?> replyMessage = this.createReplyMessage(reply, requestHeaders);
			if (!this.sendReplyMessage(replyMessage, replyChannel)) {
				throw new MessageDeliveryException(replyMessage,
						"failed to send reply Message to channel '" + replyChannel + "'");
			}
		}
	}

	@SuppressWarnings("unchecked")
	private Message<?> createReplyMessage(Object reply, MessageHeaders requestHeaders) {
		MessageBuilder<?> builder = (reply instanceof MessageBuilder) ? (MessageBuilder) reply
				: (reply instanceof Message) ? MessageBuilder.fromMessage((Message) reply)
						: MessageBuilder.withPayload(reply);
		builder.copyHeadersIfAbsent(requestHeaders);
		return builder.build();
	}

	protected boolean sendReplyMessage(Message<?> replyMessage, MessageChannel replyChannel) {
		if (logger.isDebugEnabled()) {
			logger.debug("handler '" + this + "' sending reply Message: " + replyMessage);
		}
		return this.channelTemplate.send(replyMessage, replyChannel);
	}

	/**
	 * Subclasses must implement this method to handle the request Message. The return
	 * value may be a Message, a MessageBuilder, or any plain Object. The base class
	 * will handle the final creation of a reply Message from any of those starting
	 * points. If the return value is null, the Message flow will end here.
	 */
	protected abstract Object handleRequestMessage(Message<?> requestMessage);

}
