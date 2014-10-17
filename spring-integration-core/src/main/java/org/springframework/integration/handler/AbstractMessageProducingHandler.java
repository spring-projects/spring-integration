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

import org.springframework.beans.BeansException;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.routingslip.RoutingSlip;
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
						Assert.state(getBeanFactory() != null,
								"A bean factory is required to resolve the outputChannel at runtime.");
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

	protected void sendOutputs(Object result, Message<?> requestMessage) {
		if (result instanceof Iterable<?> && shouldSplitOutput((Iterable<?>) result)) {
			for (Object o : (Iterable<?>) result) {
				this.produceOutput(o, requestMessage);
			}
		}
		else if (result != null) {
			this.produceOutput(result, requestMessage);
		}
	}

	protected boolean shouldSplitOutput(Iterable<?> reply) {
		for (Object next : reply) {
			if (next instanceof Message<?> || next instanceof AbstractIntegrationMessageBuilder<?>) {
				return true;
			}
		}
		return false;
	}

	protected void produceOutput(Object reply, Message<?> requestMessage) {
		MessageHeaders requestHeaders = requestMessage.getHeaders();

		Object replyChannel = null;
		if (getOutputChannel() == null) {
			RoutingSlip routingSlip = requestHeaders.get(IntegrationMessageHeaderAccessor.ROUTING_SLIP, RoutingSlip.class);

			if (routingSlip != null) {
				replyChannel = getReplyChannelFromRoutingSlip(reply, requestMessage, routingSlip);
			}

			if (replyChannel == null) {
				replyChannel = requestHeaders.getReplyChannel();
			}
		}

		Message<?> replyMessage = createOutputMessage(reply, requestHeaders);
		sendOutput(replyMessage, replyChannel);
	}

	private String getReplyChannelFromRoutingSlip(Object reply, Message<?> requestMessage, RoutingSlip routingSlip) {
		if (routingSlip.end()) {
			return null;
		}

		String pathValue = routingSlip.get();
		if (pathValue.startsWith("@")) {
			RoutingSlipRouteStrategy routingSlipRouteStrategy =
					getBeanFactory().getBean(pathValue.substring(1), RoutingSlipRouteStrategy.class);
			String nextPath = routingSlipRouteStrategy.getNextPath(requestMessage, reply);
			if (StringUtils.hasText(nextPath)) {
				return nextPath;
			}
			else {
				routingSlip.move();
				return getReplyChannelFromRoutingSlip(reply, requestMessage, routingSlip);
			}
		}
		else {
			routingSlip.move();
			return pathValue;
		}
	}

	private Message<?> createOutputMessage(Object output, MessageHeaders requestHeaders) {
		AbstractIntegrationMessageBuilder<?> builder = null;
		if (output instanceof Message<?>) {
			if (!this.shouldCopyRequestHeaders()) {
				return (Message<?>) output;
			}
			builder = this.getMessageBuilderFactory().fromMessage((Message<?>) output);
		}
		else if (output instanceof AbstractIntegrationMessageBuilder) {
			builder = (AbstractIntegrationMessageBuilder<?>) output;
		}
		else {
			builder = this.getMessageBuilderFactory().withPayload(output);
		}
		if (this.shouldCopyRequestHeaders()) {
			builder.copyHeadersIfAbsent(requestHeaders);
		}
		return builder.build();
	}

	/**
	 * Send a output Message. The 'replyChannel' will be considered only if this handler's
	 * 'outputChannel' is <code>null</code>. In that case, the 'replyChannel' value must not also be
	 * <code>null</code>, and it must be an instance of either String or {@link MessageChannel}.
	 * @param output the output object to send
	 * @param replyChannel the 'replyChannel' value from the original request
	 */
	private void sendOutput(Object output, Object replyChannel) {
		MessageChannel outputChannel = getOutputChannel();
		if (outputChannel != null) {
			replyChannel = outputChannel;
		}
		if (replyChannel == null) {
			throw new DestinationResolutionException("no output-channel or replyChannel header available");
		}

		if (replyChannel instanceof MessageChannel) {
			if (output instanceof Message<?>) {
				this.messagingTemplate.send((MessageChannel) replyChannel, (Message<?>) output);
			}
			else {
				this.messagingTemplate.convertAndSend((MessageChannel) replyChannel, output);
			}
		}
		else if (replyChannel instanceof String) {
			if (output instanceof Message<?>) {
				this.messagingTemplate.send((String) replyChannel, (Message<?>) output);
			}
			else {
				this.messagingTemplate.convertAndSend((String) replyChannel, output);
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
