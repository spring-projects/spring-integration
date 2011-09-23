/*
 * Copyright 2002-2011 the original author or authors.
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

import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.channel.ChannelResolutionException;
import org.springframework.integration.support.channel.ChannelResolver;
import org.springframework.util.Assert;

/**
 * Base class for MessageHandlers that are capable of producing replies.
 *
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 */
public abstract class AbstractReplyProducingMessageHandler extends AbstractMessageHandler implements MessageProducer {

	private MessageChannel outputChannel;

	private volatile boolean requiresReply = false;

	private final MessagingTemplate messagingTemplate;


	public AbstractReplyProducingMessageHandler() {
		this.messagingTemplate = new MessagingTemplate();
	}


	public void setOutputChannel(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
	}

	/**
	 * Set the timeout for sending reply Messages.
	 */
	public void setSendTimeout(long sendTimeout) {
		this.messagingTemplate.setSendTimeout(sendTimeout);
	}

	/**
	 * Set the ChannelResolver to be used when there is no default output channel.
	 */
	public void setChannelResolver(ChannelResolver channelResolver) {
		Assert.notNull(channelResolver, "'channelResolver' must not be null");
		this.messagingTemplate.setChannelResolver(channelResolver);
	}

	/**
	 * Flag whether a reply is required. If true an incoming message MUST result in a reply message being sent.
	 * If false an incoming message MAY result in a reply message being sent. Default is false.
	 */
	public void setRequiresReply(boolean requiresReply) {
		this.requiresReply = requiresReply;
	}

	/**
	 * Provides access to the {@link MessagingTemplate} for subclasses.
	 */
	protected MessagingTemplate getMessagingTemplate() {
		return this.messagingTemplate;
	}

	@Override
	protected void onInit() {
		if (this.getBeanFactory() != null) {
			this.messagingTemplate.setBeanFactory(getBeanFactory());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void handleMessageInternal(Message<?> message) {
		Object result = this.handleRequestMessage(message);
		if (result != null) {
			MessageHeaders requestHeaders = message.getHeaders();
			this.handleResult(result, requestHeaders);
		}
		else if (this.requiresReply) {
			throw new ReplyRequiredException(message, "No reply produced by handler '" +
					this.getComponentName() + "', and its 'requiresReply' property is set to true.");
		}
		else if (logger.isDebugEnabled()) {
			logger.debug("handler '" + this + "' produced no reply for request Message: " + message);
		}
	}

	private void handleResult(Object result, MessageHeaders requestHeaders) {
		if (result instanceof Iterable<?> && this.shouldSplitReply((Iterable<?>) result)) {
			for (Object o : (Iterable<?>) result) {
				this.produceReply(o, requestHeaders);
			}
		}
		else if (result != null) {
			this.produceReply(result, requestHeaders);
		}
	}

	private void produceReply(Object reply, MessageHeaders requestHeaders) {
		Message<?> replyMessage = this.createReplyMessage(reply, requestHeaders);
		this.sendReplyMessage(replyMessage, requestHeaders.getReplyChannel());
	}

	private Message<?> createReplyMessage(Object reply, MessageHeaders requestHeaders) {
		MessageBuilder<?> builder = null;
		if (reply instanceof Message<?>) {
			if (!this.shouldCopyRequestHeaders()) {
				return (Message<?>) reply;
			}
			builder = MessageBuilder.fromMessage((Message<?>) reply);
		}
		else if (reply instanceof MessageBuilder<?>) {
			builder = (MessageBuilder<?>) reply;
		}
		else {
			builder = MessageBuilder.withPayload(reply);
		}
		if (this.shouldCopyRequestHeaders()) {
			builder.copyHeadersIfAbsent(requestHeaders);
		}
		return builder.build();
	}

	/**
	 * Send a reply Message. The 'replyChannelHeaderValue' will be considered only if this handler's
	 * 'outputChannel' is <code>null</code>. In that case, the header value must not also be
	 * <code>null</code>, and it must be an instance of either String or {@link MessageChannel}.
	 * @param replyMessage the reply Message to send
	 * @param replyChannelHeaderValue the 'replyChannel' header value from the original request 
	 */
	private final void sendReplyMessage(Message<?> replyMessage, final Object replyChannelHeaderValue) {
		if (logger.isDebugEnabled()) {
			logger.debug("handler '" + this + "' sending reply Message: " + replyMessage);
		}
		if (this.outputChannel != null) {
			this.sendMessage(replyMessage, this.outputChannel);
		}
		else if (replyChannelHeaderValue != null) {
			this.sendMessage(replyMessage, replyChannelHeaderValue);
		}
		else {
			throw new ChannelResolutionException("no output-channel or replyChannel header available");
		}
	}

	/**
	 * Send the message to the given channel. The channel must be a String or
	 * {@link MessageChannel} instance, never <code>null</code>.
	 */
	private void sendMessage(final Message<?> message, final Object channel) {
		if (channel instanceof MessageChannel) {
			this.messagingTemplate.send((MessageChannel) channel, message);
		}
		else if (channel instanceof String) {
			this.messagingTemplate.send((String) channel, message);
		}
		else {
			throw new MessageDeliveryException(message,
					"a non-null reply channel value of type MessageChannel or String is required");
		}
	}

	private boolean shouldSplitReply(Iterable<?> reply) {
		for (Object next : reply) {
			if (next instanceof Message<?> || next instanceof MessageBuilder<?>) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Subclasses may override this. True by default.
	 */
	protected boolean shouldCopyRequestHeaders() {
		return true;
	}

	/**
	 * Subclasses must implement this method to handle the request Message. The return
	 * value may be a Message, a MessageBuilder, or any plain Object. The base class
	 * will handle the final creation of a reply Message from any of those starting
	 * points. If the return value is null, the Message flow will end here.
	 */
	protected abstract Object handleRequestMessage(Message<?> requestMessage);

}
