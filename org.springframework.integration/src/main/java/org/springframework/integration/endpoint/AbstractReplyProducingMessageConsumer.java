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

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.integration.channel.BeanFactoryChannelResolver;
import org.springframework.integration.channel.ChannelResolver;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.MessageChannelTemplate;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.MessageHeaders;
import org.springframework.integration.message.MessageProducer;
import org.springframework.integration.message.MessageRejectedException;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.selector.MessageSelector;
import org.springframework.util.Assert;

/**
 * Base class for MessageConsumers that are capable of producing replies.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractReplyProducingMessageConsumer extends AbstractMessageConsumer
		implements MessageProducer, BeanFactoryAware {

	public static final long DEFAULT_SEND_TIMEOUT = 1000;


	private MessageChannel outputChannel;

	private volatile ChannelResolver channelResolver;

	private volatile MessageSelector selector;

	private volatile boolean requiresReply = false;

	private final MessageChannelTemplate channelTemplate;


	public AbstractReplyProducingMessageConsumer() {
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

	public void setChannelResolver(ChannelResolver channelResolver) {
		Assert.notNull(channelResolver, "channelResolver must not be null");
		this.channelResolver = channelResolver;
	}

	public void setSelector(MessageSelector selector) {
		this.selector = selector;
	}

	public void setRequiresReply(boolean requiresReply) {
		this.requiresReply = requiresReply;
	}

	public void setBeanFactory(BeanFactory beanFactory) {
		if (this.channelResolver == null) {
			this.channelResolver = new BeanFactoryChannelResolver(beanFactory);
		}
	}

	@Override
	protected final void onMessageInternal(Message<?> message) {
		if (!this.supports(message)) {
			throw new MessageRejectedException(message, "unsupported message");
		}
		ReplyMessageHolder replyMessageHolder = new ReplyMessageHolder();
		this.onMessage(message, replyMessageHolder);
		if (replyMessageHolder.isEmpty()) {
			if (this.requiresReply) {
				throw new MessageHandlingException(message, "consumer '" + this
						+ "' requires a reply, but no reply was received");
			}
			return;
		}
		Object targetChannelValue = replyMessageHolder.getTargetChannel();
		MessageChannel replyChannel = null;
		if (targetChannelValue == null) {
			replyChannel = this.resolveReplyChannel(message);
		}
		else if (targetChannelValue instanceof String) {
			replyChannel = this.channelResolver.resolveChannelName((String) targetChannelValue);
		}
		MessageHeaders requestHeaders = message.getHeaders();
		for (MessageBuilder<?> builder : replyMessageHolder.builders()) {
			builder.copyHeadersIfAbsent(requestHeaders);
			this.sendReplyMessage(builder.build(), replyChannel);
		}
	}

	protected abstract void onMessage(Message<?> requestMessage, ReplyMessageHolder replyMessageHolder);

	protected boolean supports(Message<?> message) {
		if (this.selector != null && !this.selector.accept(message)) {
			if (logger.isDebugEnabled()) {
				logger.debug("selector for consumer '" + this + "' rejected message: " + message);
			}
			return false;
		}
		return true;
	}

	protected boolean sendReplyMessage(Message<?> replyMessage, MessageChannel replyChannel) {
		return this.channelTemplate.send(replyMessage, replyChannel);
	}

	private MessageChannel resolveReplyChannel(Message<?> requestMessage) {
		MessageChannel replyChannel = this.getOutputChannel();
		if (replyChannel == null) {
			Object returnAddress = requestMessage.getHeaders().getReturnAddress();
			if (returnAddress != null) {
				if (returnAddress instanceof MessageChannel) {
					replyChannel = (MessageChannel) returnAddress;
				}
				else if (returnAddress instanceof String) {
					Assert.state(this.channelResolver != null,
							"ChannelResolver is required for resolving a reply channel by name");
					replyChannel = this.channelResolver.resolveChannelName((String) returnAddress);
				}
				else {
					throw new MessagingException("expected a MessageChannel or String for 'returnAddress', but type is ["
							+ returnAddress.getClass() + "]");
				}
			}
		}
		if (replyChannel == null) {
			throw new MessagingException("unable to resolve reply channel");
		}
		return replyChannel;
	}

}
