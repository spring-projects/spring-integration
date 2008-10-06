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

import java.util.ArrayList;
import java.util.List;

import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.ChannelRegistryAware;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.MessageChannelTemplate;
import org.springframework.integration.message.CompositeMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.MessageHeaders;
import org.springframework.integration.message.MessageRejectedException;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.message.selector.MessageSelector;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 */
public abstract class AbstractMessageHandlingEndpoint extends AbstractMessageConsumer implements ChannelRegistryAware {

	private MessageChannel outputChannel;

	private volatile ChannelRegistry channelRegistry;

	private volatile MessageSelector selector;

	private volatile boolean requiresReply = false;

	private final MessageChannelTemplate channelTemplate = new MessageChannelTemplate();


	public void setOutputChannel(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
	}

	protected MessageChannel getOutputChannel() {
		return this.outputChannel;
	}

	public void setChannelRegistry(ChannelRegistry channelRegistry) {
		this.channelRegistry = channelRegistry;
	}

	public void setSelector(MessageSelector selector) {
		this.selector = selector;
	}

	public void setRequiresReply(boolean requiresReply) {
		this.requiresReply = requiresReply;
	}


	@Override
	protected void onMessageInternal(Message<?> message) {
		if (!this.supports(message)) {
			throw new MessageRejectedException(message, "unsupported message");
		}
		Object result = this.handle(message);
		if (result == null) {
			if (this.requiresReply) {
				throw new MessageHandlingException(message, "consumer '" + this
						+ "' requires a reply, but no reply was received");
			}
			return;
		}
		Message<?> reply = null;
		if (result instanceof Message && result.equals(message)) {
			// we simply pass along an unaltered request Message
			reply = (Message<?>) result;
		}
		else {
			reply = buildReplyMessage(result, message.getHeaders());
		}
		MessageChannel replyChannel = this.resolveReplyChannel(message);
		if (reply instanceof CompositeMessage && this.shouldSplitComposite()) {
			boolean sentAtLeastOne = false;
			for (Message<?> nextReply : (CompositeMessage) reply) {
				boolean sent = this.sendReplyMessage(nextReply, replyChannel);
				sentAtLeastOne = (sentAtLeastOne || sent);
			}
		}
		else {
			this.sendReplyMessage(reply, replyChannel);
		}
	}

	protected abstract Object handle(Message<?> message);

	protected boolean supports(Message<?> message) {
		if (this.selector != null && !this.selector.accept(message)) {
			if (logger.isDebugEnabled()) {
				logger.debug("selector for consumer '" + this + "' rejected message: " + message);
			}
			return false;
		}
		return true;
	}

	protected boolean shouldSplitComposite() {
		return false;
	}

	private boolean sendReplyMessage(Message<?> replyMessage, MessageChannel replyChannel) {
		return this.channelTemplate.send(replyMessage, replyChannel);
	}

	private Message<?> buildReplyMessage(Object result, MessageHeaders requestHeaders) {
		MessageBuilder<?> builder = null;
		if (result instanceof MessageBuilder) {
			builder = (MessageBuilder<?>) result;
		}
		else if (result instanceof CompositeMessage) {
			List<Message<?>> messages = ((CompositeMessage) result).getPayload();
			List<Message<?>> replies = new ArrayList<Message<?>>();
			for (Message<?> message : messages) {
				replies.add(this.buildReplyMessage(message, requestHeaders));
			}
			return new CompositeMessage(replies);
		}
		else if (result instanceof Message<?>) {
			builder = MessageBuilder.fromMessage((Message<?>) result);
		}
		else {
			builder = MessageBuilder.withPayload(result);
		}
		return builder.copyHeadersIfAbsent(requestHeaders)
			.setHeaderIfAbsent(MessageHeaders.CORRELATION_ID, requestHeaders.getId())
			.build();
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
					Assert.state(this.channelRegistry != null,
							"ChannelRegistry is required for resolving a reply channel by name");
					replyChannel = this.channelRegistry.lookupChannel((String) returnAddress);
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
