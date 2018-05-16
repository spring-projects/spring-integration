/*
 * Copyright 2014-2018 the original author or authors.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.reactivestreams.Publisher;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.ReactiveStreamsSubscribableChannel;
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
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.SettableListenableFuture;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * The base {@link AbstractMessageHandler} implementation for the {@link MessageProducer}.
 *
 * @author David Liu
 * @author Artem Bilan
 * @author Gary Russell
 * @author Marius Bogoevici
 *
 * since 4.1
 */
public abstract class AbstractMessageProducingHandler extends AbstractMessageHandler
		implements MessageProducer, HeaderPropagationAware {

	protected final MessagingTemplate messagingTemplate = new MessagingTemplate();

	private boolean async;

	private String outputChannelName;

	private MessageChannel outputChannel;

	private String[] notPropagatedHeaders;

	private boolean selectiveHeaderPropagation;

	private boolean noHeadersPropagation;

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
	 * Allow async replies. If the handler reply is a {@link ListenableFuture}, send
	 * the output when it is satisfied rather than sending the future as the result.
	 * Ignored for return types other than {@link ListenableFuture}.
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

	/**
	 * Set header patterns ("xxx*", "*xxx", "*xxx*" or "xxx*yyy")
	 * that will NOT be copied from the inbound message if
	 * {@link #shouldCopyRequestHeaders() shouldCopyRequestHeaaders} is true.
	 * At least one pattern as "*" means do not copy headers at all.
	 * @param headers the headers to not propagate from the inbound message.
	 * @since 4.3.10
	 * @see org.springframework.util.PatternMatchUtils
	 */
	@Override
	public void setNotPropagatedHeaders(String... headers) {
		updateNotPropagatedHeaders(headers, false);
	}

	/**
	 * Set or replace not propagated headers. Exposed so that subclasses can set specific
	 * headers in a constructor, since {@link #setNotPropagatedHeaders(String...)} is not
	 * final.
	 * @param headers Header patterns to not propagate.
	 * @param merge true to merge with existing patterns; false to replace.
	 * @since 5.0.2
	 */
	protected final void updateNotPropagatedHeaders(String[] headers, boolean merge) {
		Set<String> headerPatterns = new HashSet<>();

		if (merge && this.notPropagatedHeaders != null) {
			headerPatterns.addAll(Arrays.asList(this.notPropagatedHeaders));
		}

		if (!ObjectUtils.isEmpty(headers)) {
			Assert.noNullElements(headers, "null elements are not allowed in 'headers'");

			headerPatterns.addAll(Arrays.asList(headers));

			this.notPropagatedHeaders = headerPatterns.toArray(new String[0]);
		}

		boolean hasAsterisk = headerPatterns.contains("*");

		if (hasAsterisk) {
			this.notPropagatedHeaders = new String[] { "*" };
			this.noHeadersPropagation = true;
		}

		this.selectiveHeaderPropagation = !ObjectUtils.isEmpty(this.notPropagatedHeaders);
	}

	/**
	 * Get the header patterns this handler doesn't propagate.
	 * @return an immutable {@link java.util.Collection} of headers that will not be
	 * copied from the inbound message if {@link #shouldCopyRequestHeaders()} is true.
	 * @since 4.3.10
	 * @see #setNotPropagatedHeaders(String...)
	 * @see org.springframework.util.PatternMatchUtils
	 */
	@Override
	public Collection<String> getNotPropagatedHeaders() {
		return this.notPropagatedHeaders != null
				? Collections.unmodifiableSet(new HashSet<>(Arrays.asList(this.notPropagatedHeaders)))
				: Collections.emptyList();
	}

	/**
	 * Add header patterns ("xxx*", "*xxx", "*xxx*" or "xxx*yyy")
	 * that will NOT be copied from the inbound message if
	 * {@link #shouldCopyRequestHeaders()} is true, instead of overwriting the existing
	 * set.
	 * @param headers the headers to not propagate from the inbound message.
	 * @since 4.3.10
	 * @see #setNotPropagatedHeaders(String...)
	 */
	@Override
	public void addNotPropagatedHeaders(String... headers) {
		updateNotPropagatedHeaders(headers, true);
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
				Assert.isTrue(routingSlipHeader.size() == 1,
						"The RoutingSlip header value must be a SingletonMap");
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
				if (replyChannel == null && reply instanceof Message) {
					replyChannel = ((Message<?>) reply).getHeaders().getReplyChannel();
				}
			}
		}

		if (this.async && (reply instanceof ListenableFuture<?> || reply instanceof Publisher<?>)) {
			if (reply instanceof ListenableFuture<?> ||
					!(getOutputChannel() instanceof ReactiveStreamsSubscribableChannel)) {
				ListenableFuture<?> future;
				if (reply instanceof ListenableFuture<?>) {
					future = (ListenableFuture<?>) reply;
				}
				else {
					SettableListenableFuture<Object> settableListenableFuture = new SettableListenableFuture<>();

					Mono.from((Publisher<?>) reply)
							.subscribe(settableListenableFuture::set, settableListenableFuture::setException);

					future = settableListenableFuture;
				}

				Object theReplyChannel = replyChannel;
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
				((ReactiveStreamsSubscribableChannel) getOutputChannel())
						.subscribeTo(
								Flux.from((Publisher<?>) reply)
										.map(result -> createOutputMessage(result, requestHeaders)));
			}
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
			if (this.noHeadersPropagation || !shouldCopyRequestHeaders()) {
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
		if (!this.noHeadersPropagation && shouldCopyRequestHeaders()) {
			builder.filterAndCopyHeadersIfAbsent(requestHeaders,
					this.selectiveHeaderPropagation ? this.notPropagatedHeaders : null);
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
