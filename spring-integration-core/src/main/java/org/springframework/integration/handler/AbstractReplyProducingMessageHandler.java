/*
 * Copyright 2002-2014 the original author or authors.
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

import org.aopalliance.aop.Advice;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

/**
 * Base class for MessageHandlers that are capable of producing replies.
 *
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
public abstract class AbstractReplyProducingMessageHandler extends AbstractMessageHandler
		implements MessageProducer, BeanClassLoaderAware {

	private MessageChannel outputChannel;

	private String outputChannelName;

	private volatile boolean requiresReply = false;

	private final MessagingTemplate messagingTemplate;

	private volatile RequestHandler advisedRequestHandler;

	private volatile List<Advice> adviceChain;

	private volatile ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();



	public AbstractReplyProducingMessageHandler() {
		this.messagingTemplate = new MessagingTemplate();
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
	 * Set the timeout for sending reply Messages.
	 * @param sendTimeout The send timeout.
	 */
	public void setSendTimeout(long sendTimeout) {
		this.messagingTemplate.setSendTimeout(sendTimeout);
	}

	/**
	 * Set the DestinationResolver&lt;MessageChannel&gt; to be used when there is no default output channel.
	 * @param channelResolver The channel resolver.
	 */
	public void setChannelResolver(DestinationResolver<MessageChannel> channelResolver) {
		Assert.notNull(channelResolver, "'channelResolver' must not be null");
		this.messagingTemplate.setDestinationResolver(channelResolver);
	}

	/**
	 * Flag whether a reply is required. If true an incoming message MUST result in a reply message being sent.
	 * If false an incoming message MAY result in a reply message being sent. Default is false.
	 * @param requiresReply true if a reply is required.
	 */
	public void setRequiresReply(boolean requiresReply) {
		this.requiresReply = requiresReply;
	}

	/**
	 * Provides access to the {@link MessagingTemplate} for subclasses.
	 * @return The messaging template.
	 */
	protected MessagingTemplate getMessagingTemplate() {
		return this.messagingTemplate;
	}


	public void setAdviceChain(List<Advice> adviceChain) {
		Assert.notNull(adviceChain, "adviceChain cannot be null");
		this.adviceChain = adviceChain;
	}

	protected boolean hasAdviceChain() {
		return this.adviceChain != null && this.adviceChain.size() > 0;
	}

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}


	@Override
	protected final void onInit() {
		Assert.state(!(this.outputChannelName != null && this.outputChannel != null),
				"'outputChannelName' and 'outputChannel' are mutually exclusive.");
		if (this.getBeanFactory() != null) {
			this.messagingTemplate.setBeanFactory(getBeanFactory());
		}
		if (!CollectionUtils.isEmpty(this.adviceChain)) {
			ProxyFactory proxyFactory = new ProxyFactory(new AdvisedRequestHandler());
			for (Advice advice : this.adviceChain) {
				proxyFactory.addAdvice(advice);
			}
			this.advisedRequestHandler = (RequestHandler) proxyFactory.getProxy(this.beanClassLoader);
		}
		this.doInit();
	}

	protected void doInit() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void handleMessageInternal(Message<?> message) {
		Object result;
		if (this.advisedRequestHandler == null) {
			result = this.handleRequestMessage(message);
		}
		else {
			result = doInvokeAdvisedRequestHandler(message);
		}
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

	protected Object doInvokeAdvisedRequestHandler(Message<?> message) {
		return this.advisedRequestHandler.handleRequestMessage(message);
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

	protected void produceReply(Object reply, MessageHeaders requestHeaders) {
		Message<?> replyMessage = this.createReplyMessage(reply, requestHeaders);
		this.sendReplyMessage(replyMessage, requestHeaders.getReplyChannel());
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
	 * Send a reply Message. The 'replyChannelHeaderValue' will be considered only if this handler's
	 * 'outputChannel' is <code>null</code>. In that case, the header value must not also be
	 * <code>null</code>, and it must be an instance of either String or {@link MessageChannel}.
	 * @param replyMessage the reply Message to send
	 * @param replyChannelHeaderValue the 'replyChannel' header value from the original request
	 */
	private void sendReplyMessage(Message<?> replyMessage, final Object replyChannelHeaderValue) {
		if (logger.isDebugEnabled()) {
			logger.debug("handler '" + this + "' sending reply Message: " + replyMessage);
		}

		if (this.outputChannelName != null) {
			synchronized (this) {
				if (this.outputChannelName != null) {
					try {
						this.outputChannel = this.getBeanFactory().getBean(this.outputChannelName, MessageChannel.class);
						this.outputChannelName = null;
					}
					catch (BeansException e) {
						throw new DestinationResolutionException("Failed to look up MessageChannel with name '"
								+ this.outputChannelName + "' in the BeanFactory.");
					}
				}
			}
		}

		if (this.outputChannel != null) {
			this.sendMessage(replyMessage, this.outputChannel);
		}
		else if (replyChannelHeaderValue != null) {
			this.sendMessage(replyMessage, replyChannelHeaderValue);
		}
		else {
			throw new DestinationResolutionException("no output-channel or replyChannel header available");
		}
	}

	/**
	 * Send the message to the given channel. The channel must be a String or
	 * {@link MessageChannel} instance, never <code>null</code>.
	 * @param message The message.
	 * @param channel The channel to which to send the message.
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
			if (next instanceof Message<?> || next instanceof AbstractIntegrationMessageBuilder<?>) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Subclasses may override this. True by default.
	 * @return true if the request headers should be copied.
	 */
	protected boolean shouldCopyRequestHeaders() {
		return true;
	}

	/**
	 * Subclasses must implement this method to handle the request Message. The return
	 * value may be a Message, a MessageBuilder, or any plain Object. The base class
	 * will handle the final creation of a reply Message from any of those starting
	 * points. If the return value is null, the Message flow will end here.
	 * @param requestMessage The request message.
	 * @return The result of handling the message, or {@code null}.
	 */
	protected abstract Object handleRequestMessage(Message<?> requestMessage);


	public interface RequestHandler {

		Object handleRequestMessage(Message<?> requestMessage);

		@Override
		String toString();
	}

	private class AdvisedRequestHandler implements RequestHandler {

		@Override
		public Object handleRequestMessage(Message<?> requestMessage) {
			return AbstractReplyProducingMessageHandler.this.handleRequestMessage(requestMessage);
		}

		@Override
		public String toString() {
			return AbstractReplyProducingMessageHandler.this.toString();
		}

	}

}
