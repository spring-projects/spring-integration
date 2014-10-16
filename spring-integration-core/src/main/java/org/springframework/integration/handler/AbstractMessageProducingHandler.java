/*
 * Copyright 2014 the original author or authors.
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

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.BeansException;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.routingslip.RoutingSlipRouteStrategy;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * The base {@link AbstractMessageHandler} implementation for the {@link MessageProducer}.
 *
 * @author David Liu
 * @author Artem Bilan
 * since 4.1
 */
public abstract class AbstractMessageProducingHandler extends AbstractMessageHandler
		implements MessageProducer {

	protected final MessagingTemplate messagingTemplate = new MessagingTemplate();

	private MessageChannel outputChannel;

	private String outputChannelName;

	/**
	 * Set the timeout for sending reply Messages.
	 * @param sendTimeout The send timeout.
	 */
	public void setSendTimeout(long sendTimeout) {
		this.messagingTemplate.setSendTimeout(sendTimeout);
	}

	@Override
	public void setOutputChannel(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
	}

	public void setOutputChannelName(String outputChannelName) {
		Assert.hasText(outputChannelName, "'outputChannelName' must not be empty");
		this.outputChannelName = outputChannelName;
	}

	/**
	 * Set the DestinationResolver&lt;MessageChannel&gt; to be used when there is no default output channel.
	 * @param channelResolver The channel resolver.
	 */
	public void setChannelResolver(DestinationResolver<MessageChannel> channelResolver) {
		Assert.notNull(channelResolver, "'channelResolver' must not be null");
		this.messagingTemplate.setDestinationResolver(channelResolver);
	}

	@Override
	protected void onInit() throws Exception {
		super.onInit();
		Assert.state(!(this.outputChannelName != null && this.outputChannel != null),
				"'outputChannelName' and 'outputChannel' are mutually exclusive.");
		if (getBeanFactory() != null) {
			this.messagingTemplate.setBeanFactory(getBeanFactory());
		}
	}

	public MessageChannel getOutputChannel() {
		if (this.outputChannelName != null) {
			synchronized (this) {
				if (this.outputChannelName != null) {
					try {
						Assert.state(getBeanFactory() != null, "A bean factory is required to resolve the outputChannel at runtime.");
						this.outputChannel = getBeanFactory().getBean(this.outputChannelName, MessageChannel.class);
						this.outputChannelName = null;
					}
					catch (BeansException e) {
						throw new DestinationResolutionException("Failed to look up MessageChannel with name '"
								+ this.outputChannelName + "' in the BeanFactory.");
					}
				}
			}
		}
		return outputChannel;
	}

	protected void sendReplies(Object result, Message<?> requestMessage) {
		if (result instanceof Iterable<?> && shouldSplitReply((Iterable<?>) result)) {
			for (Object o : (Iterable<?>) result) {
				this.produceReply(o, requestMessage);
			}
		}
		else if (result != null) {
			this.produceReply(result, requestMessage);
		}
	}

	protected boolean shouldSplitReply(Iterable<?> reply) {
		for (Object next : reply) {
			if (next instanceof Message<?> || next instanceof AbstractIntegrationMessageBuilder<?>) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	protected void produceReply(Object reply, Message<?> requestMessage) {
		MessageHeaders requestHeaders = requestMessage.getHeaders();

		Object replyChannel = null;
		if (getOutputChannel() == null) {
			List<String> routingSlip = requestHeaders.get(IntegrationMessageHeaderAccessor.ROUTING_SLIP, List.class);

			if (routingSlip != null) {
				Integer routingSlipIndexValue =
						requestHeaders.get(IntegrationMessageHeaderAccessor.ROUTING_SLIP_INDEX, Integer.class);
				if (routingSlipIndexValue == null) {
					routingSlipIndexValue = 0;
				}
				AtomicInteger routingSlipIndex = new AtomicInteger(routingSlipIndexValue);
				replyChannel = getReplyChannelFromRoutingSlip(reply, requestMessage, routingSlip, routingSlipIndex);
				if (replyChannel != null) {
					//TODO Migrate to the SF MessageBuilder
					AbstractIntegrationMessageBuilder<?> builder = null;
					if (reply instanceof Message) {
						builder = this.getMessageBuilderFactory().fromMessage((Message<?>) reply);
					}
					else if (reply instanceof AbstractIntegrationMessageBuilder) {
						builder = (AbstractIntegrationMessageBuilder) reply;
					}
					else {
						builder = this.getMessageBuilderFactory().withPayload(reply);
					}
					builder.setHeader(IntegrationMessageHeaderAccessor.ROUTING_SLIP_INDEX, routingSlipIndex.get());
					reply = builder;
				}
			}

			if (replyChannel == null) {
				replyChannel = requestHeaders.getReplyChannel();
			}
		}

		Message<?> replyMessage = createReplyMessage(reply, requestHeaders);
		sendReplyMessage(replyMessage, replyChannel);
	}

	private String getReplyChannelFromRoutingSlip(Object reply, Message<?> requestMessage, List<String> routingSlipList,
			AtomicInteger routingSlipIndex) {
		if (routingSlipList.size() == routingSlipIndex.get()) {
			return null;
		}

		String routingSlipValue = routingSlipList.get(routingSlipIndex.get());
		if (routingSlipValue.startsWith("@")) {
			RoutingSlipRouteStrategy routingSlip =
					getBeanFactory().getBean(routingSlipValue.substring(1), RoutingSlipRouteStrategy.class);
			String nextPath = routingSlip.getNextPath(requestMessage, reply);
			if (StringUtils.hasText(nextPath)) {
				return nextPath;
			}
			else {
				routingSlipIndex.incrementAndGet();
				return getReplyChannelFromRoutingSlip(reply, requestMessage, routingSlipList, routingSlipIndex);
			}
		}
		else {
			routingSlipIndex.incrementAndGet();
			return routingSlipValue;
		}
	}

	private Message<?> createReplyMessage(Object reply, MessageHeaders requestHeaders) {
		AbstractIntegrationMessageBuilder<?> builder = null;
		if (reply instanceof Message<?>) {
			if (!this.shouldCopyRequestHeaders()) {
				return (Message<?>) reply;
			}
			builder = this.getMessageBuilderFactory().fromMessage((Message<?>) reply);
		}
		else if (reply instanceof AbstractIntegrationMessageBuilder) {
			builder = (AbstractIntegrationMessageBuilder<?>) reply;
		}
		else {
			builder = this.getMessageBuilderFactory().withPayload(reply);
		}
		if (this.shouldCopyRequestHeaders()) {
			builder.copyHeadersIfAbsent(requestHeaders);
		}
		return builder.build();
	}

	/**
	 * Send a reply Message. The 'replyChannel' will be considered only if this handler's
	 * 'outputChannel' is <code>null</code>. In that case, the 'replyChannel' value must not also be
	 * <code>null</code>, and it must be an instance of either String or {@link MessageChannel}.
	 * @param reply the reply Message to send
	 * @param replyChannel the 'replyChannel' value from the original request
	 */
	private void sendReplyMessage(Object reply, Object replyChannel) {
		MessageChannel outputChannel = getOutputChannel();
		if (outputChannel != null) {
			replyChannel = outputChannel;
		}
		if (replyChannel == null) {
			throw new DestinationResolutionException("no output-channel or replyChannel header available");
		}

		if (replyChannel instanceof MessageChannel) {
			if (reply instanceof Message<?>) {
				this.messagingTemplate.send((MessageChannel) replyChannel, (Message<?>) reply);
			}
			else {
				this.messagingTemplate.convertAndSend((MessageChannel) replyChannel, reply);
			}
		}
		else if (replyChannel instanceof String) {
			if (reply instanceof Message<?>) {
				this.messagingTemplate.send((String) replyChannel, (Message<?>) reply);
			}
			else {
				this.messagingTemplate.convertAndSend((String) replyChannel, reply);
			}
		}
		else {
			throw new MessagingException("replyChannel must be a MessageChannel or String");
		}
	}

	/**
	 * Subclasses may override this. True by default.
	 * @return true if the request headers should be copied.
	 */
	protected boolean shouldCopyRequestHeaders() {
		return true;
	}

}
