/*
 * Copyright 2014-2016 the original author or authors.
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.routingslip.RoutingSlipRouteStrategy;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

/**
 * The base {@link AbstractMessageHandler} implementation for the {@link MessageProducer}.
 *
 * @author David Liu
 * @author Artem Bilan
 * @author Gary Russell
 * since 4.1
 */
public abstract class AbstractMessageProducingHandler extends AbstractMessageHandler
		implements MessageProducer {

	protected final MessagingTemplate messagingTemplate = new MessagingTemplate();

	private volatile MessageChannel outputChannel;

	private volatile String outputChannelName;

	private volatile boolean async;

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
		this.outputChannelName = outputChannelName; //NOSONAR (inconsistent sync)
	}

	/**
	 * Allow async replies. If the handler reply is a {@link ListenableFuture} send
	 * the output when it is satisfied rather than sending the future as the result.
	 * Only subclasses that support this feature should set it.
	 * @param async true to allow.
	 * @since 4.3
	 */
	public final void setAsync(boolean async) {
		this.async = async;
	}

	/**
	 * @see #setAsync(boolean)
	 * @return true if this handler supports async replies.
	 * @since 4.3
	 */
	protected boolean isAsync() {
		return this.async;
	}

	@Override
	protected void onInit() throws Exception {
		super.onInit();
		Assert.state(!(this.outputChannelName != null && this.outputChannel != null), //NOSONAR (inconsistent sync)
				"'outputChannelName' and 'outputChannel' are mutually exclusive.");
		if (getBeanFactory() != null) {
			this.messagingTemplate.setBeanFactory(getBeanFactory());
		}
		this.messagingTemplate.setDestinationResolver(getChannelResolver());
	}

	@Override
	public MessageChannel getOutputChannel() {
		if (this.outputChannelName != null) {
			synchronized (this) {
				if (this.outputChannelName != null) {
					this.outputChannel = getChannelResolver().resolveDestination(this.outputChannelName);
					this.outputChannelName = null;
				}
			}
		}
		return this.outputChannel;
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

	protected void produceOutput(Object reply, final Message<?> requestMessage) {
		final MessageHeaders requestHeaders = requestMessage.getHeaders();

		Object replyChannel = null;
		if (getOutputChannel() == null) {
			Map<?, ?> routingSlipHeader = requestHeaders.get(IntegrationMessageHeaderAccessor.ROUTING_SLIP, Map.class);
			if (routingSlipHeader != null) {
				Assert.isTrue(routingSlipHeader.size() == 1, "The RoutingSlip header value must be a SingletonMap");
				Object key = routingSlipHeader.keySet().iterator().next();
				Object value = routingSlipHeader.values().iterator().next();
				Assert.isInstanceOf(List.class, key, "The RoutingSlip key must be List");
				Assert.isInstanceOf(Integer.class, value, "The RoutingSlip value must be Integer");
				List<?> routingSlip = (List<?>) key;
				AtomicInteger routingSlipIndex = new AtomicInteger((Integer) value);
				replyChannel = getOutputChannelFromRoutingSlip(reply, requestMessage, routingSlip, routingSlipIndex);
				if (replyChannel != null) {
					//TODO Migrate to the SF MessageBuilder
					AbstractIntegrationMessageBuilder<?> builder = null;
					if (reply instanceof Message) {
						builder = this.getMessageBuilderFactory().fromMessage((Message<?>) reply);
					}
					else if (reply instanceof AbstractIntegrationMessageBuilder) {
						builder = (AbstractIntegrationMessageBuilder<?>) reply;
					}
					else {
						builder = this.getMessageBuilderFactory().withPayload(reply);
					}
					builder.setHeader(IntegrationMessageHeaderAccessor.ROUTING_SLIP,
							Collections.singletonMap(routingSlip, routingSlipIndex.get()));
					reply = builder;
				}
			}

			if (replyChannel == null) {
				replyChannel = requestHeaders.getReplyChannel();
			}
		}

		if (this.async && reply instanceof ListenableFuture<?>) {
			ListenableFuture<?> future = (ListenableFuture<?>) reply;
			final Object theReplyChannel = replyChannel;
			future.addCallback(new ListenableFutureCallback<Object>() {

				@Override
				public void onSuccess(Object result) {
					Message<?> replyMessage = null;
					try {
						replyMessage = createOutputMessage(result, requestHeaders);
						sendOutput(replyMessage, theReplyChannel, false);
					}
					catch (Exception e) {
						Exception exceptionToLogAndSend = e;
						if (!(e instanceof MessagingException)) {
							exceptionToLogAndSend = new MessageHandlingException(requestMessage, e);
							if (replyMessage != null) {
								exceptionToLogAndSend = new MessagingException(replyMessage, exceptionToLogAndSend);
							}
						}
						logger.error("Failed to send async reply: " + result.toString(), exceptionToLogAndSend);
						onFailure(exceptionToLogAndSend);
					}
				}

				@Override
				public void onFailure(Throwable ex) {
					sendErrorMessage(requestMessage, ex);
				}
			});
		}
		else {
			sendOutput(createOutputMessage(reply, requestHeaders), replyChannel, false);
		}
	}

	private Object getOutputChannelFromRoutingSlip(Object reply, Message<?> requestMessage, List<?> routingSlip,
			AtomicInteger routingSlipIndex) {
		if (routingSlipIndex.get() >= routingSlip.size()) {
			return null;
		}

		Object path = routingSlip.get(routingSlipIndex.get());
		Object routingSlipPathValue = null;

		if (path instanceof String) {
			routingSlipPathValue = getBeanFactory().getBean((String) path);
		}
		else if (path instanceof RoutingSlipRouteStrategy) {
			routingSlipPathValue = path;
		}
		else {
			throw new IllegalArgumentException("The RoutingSlip 'path' can be of " +
					"String or RoutingSlipRouteStrategy type, but got: " + path.getClass());
		}

		if (routingSlipPathValue instanceof MessageChannel) {
			routingSlipIndex.incrementAndGet();
			return routingSlipPathValue;
		}
		else {
			Object nextPath = ((RoutingSlipRouteStrategy) routingSlipPathValue).getNextPath(requestMessage, reply);
			if (nextPath != null && (!(nextPath instanceof String) || StringUtils.hasText((String) nextPath))) {
				return nextPath;
			}
			else {
				routingSlipIndex.incrementAndGet();
				return getOutputChannelFromRoutingSlip(reply, requestMessage, routingSlip, routingSlipIndex);
			}
		}
	}

	protected Message<?> createOutputMessage(Object output, MessageHeaders requestHeaders) {
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
	 * Send an output Message. The 'replyChannel' will be considered only if this handler's
	 * 'outputChannel' is <code>null</code>. In that case, the 'replyChannel' value must not also be
	 * <code>null</code>, and it must be an instance of either String or {@link MessageChannel}.
	 * @param output the output object to send
	 * @param replyChannel the 'replyChannel' value from the original request
	 * @param useArgChannel - use the replyChannel argument (must not be null), not
	 * the configured output channel.
	 */
	protected void sendOutput(Object output, Object replyChannel, boolean useArgChannel) {
		MessageChannel outputChannel = getOutputChannel();
		if (!useArgChannel && outputChannel != null) {
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

	protected void sendErrorMessage(final Message<?> requestMessage, Throwable ex) {
		Object errorChannel = resolveErrorChannel(requestMessage.getHeaders());
		Throwable result = ex;
		if (!(ex instanceof MessagingException)) {
			result = new MessageHandlingException(requestMessage, ex);
		}
		if (errorChannel == null) {
			logger.error("Async exception received and no 'errorChannel' header exists and no default "
					+ "'errorChannel' found", result);
		}
		else {
			try {
				sendOutput(new ErrorMessage(result), errorChannel, true);
			}
			catch (Exception e) {
				Exception exceptionToLog = e;
				if (!(e instanceof MessagingException)) {
					exceptionToLog = new MessageHandlingException(requestMessage, e);
				}
				logger.error("Failed to send async reply", exceptionToLog);
			}
		}
	}

	protected Object resolveErrorChannel(final MessageHeaders requestHeaders) {
		Object errorChannel = requestHeaders.getErrorChannel();
		if (errorChannel == null) {
			try {
				errorChannel = getChannelResolver().resolveDestination("errorChannel");
			}
			catch (DestinationResolutionException e) {
				// ignore
			}
		}
		return errorChannel;
	}

}
