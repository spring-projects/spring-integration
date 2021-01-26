/*
 * Copyright 2014-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.ReactiveStreamsSubscribableChannel;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.routingslip.RoutingSlipRouteStrategy;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.lang.Nullable;
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

	protected final MessagingTemplate messagingTemplate = new MessagingTemplate(); // NOSONAR final

	private boolean async;

	@Nullable
	private String outputChannelName;

	@Nullable
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

	@Override
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

		if (headerPatterns.contains("*")) {
			this.notPropagatedHeaders = new String[]{ "*" };
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
	protected void onInit() {
		super.onInit();
		Assert.state(!(this.outputChannelName != null && this.outputChannel != null), //NOSONAR (inconsistent sync)
				"'outputChannelName' and 'outputChannel' are mutually exclusive.");
		BeanFactory beanFactory = getBeanFactory();
		if (beanFactory != null) {
			this.messagingTemplate.setBeanFactory(beanFactory);
		}
		this.messagingTemplate.setDestinationResolver(getChannelResolver());
	}

	@Override
	@Nullable
	public MessageChannel getOutputChannel() {
		String channelName = this.outputChannelName;
		if (channelName != null) {
			this.outputChannel = getChannelResolver().resolveDestination(channelName);
			this.outputChannelName = null;
		}
		return this.outputChannel;
	}

	protected void sendOutputs(Object result, Message<?> requestMessage) {
		if (result instanceof Iterable<?> && shouldSplitOutput((Iterable<?>) result)) {
			for (Object o : (Iterable<?>) result) {
				produceOutput(o, requestMessage);
			}
		}
		else if (result != null) {
			produceOutput(result, requestMessage);
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

	protected void produceOutput(Object replyArg, final Message<?> requestMessage) {
		MessageHeaders requestHeaders = requestMessage.getHeaders();
		Object reply = replyArg;
		Object replyChannel = null;
		if (getOutputChannel() == null) {
			Map<?, ?> routingSlipHeader = obtainRoutingSlipHeader(requestHeaders, reply);
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
					reply = addRoutingSlipHeader(reply, routingSlip, routingSlipIndex);
				}
			}
			if (replyChannel == null) {
				replyChannel = obtainReplyChannel(requestHeaders, reply);
			}
		}
		doProduceOutput(requestMessage, requestHeaders, reply, replyChannel);
	}

	@Nullable
	private Map<?, ?> obtainRoutingSlipHeader(MessageHeaders requestHeaders, Object reply) {
		Map<?, ?> routingSlipHeader = requestHeaders.get(IntegrationMessageHeaderAccessor.ROUTING_SLIP, Map.class);
		if (routingSlipHeader == null) {
			if (reply instanceof Message) {
				routingSlipHeader = ((Message<?>) reply).getHeaders()
						.get(IntegrationMessageHeaderAccessor.ROUTING_SLIP, Map.class);
			}
			else if (reply instanceof AbstractIntegrationMessageBuilder<?>) {
				routingSlipHeader = ((AbstractIntegrationMessageBuilder<?>) reply)
						.getHeader(IntegrationMessageHeaderAccessor.ROUTING_SLIP, Map.class);
			}
		}
		return routingSlipHeader;
	}

	@Nullable
	private Object obtainReplyChannel(MessageHeaders requestHeaders, Object reply) {
		Object replyChannel = requestHeaders.getReplyChannel();
		if (replyChannel == null) {
			if (reply instanceof Message) {
				replyChannel = ((Message<?>) reply).getHeaders().getReplyChannel();
			}
			else if (reply instanceof AbstractIntegrationMessageBuilder<?>) {
				replyChannel = ((AbstractIntegrationMessageBuilder<?>) reply)
						.getHeader(MessageHeaders.REPLY_CHANNEL, Object.class);
			}
		}
		return replyChannel;
	}

	private void doProduceOutput(Message<?> requestMessage, MessageHeaders requestHeaders, Object reply,
			@Nullable Object replyChannelArg) {

		Object replyChannel = replyChannelArg;
		if (replyChannel == null) {
			replyChannel = getOutputChannel();
		}

		if (this.async && (reply instanceof ListenableFuture<?> || reply instanceof Publisher<?>)) {
			if (reply instanceof Publisher<?> &&
					replyChannel instanceof ReactiveStreamsSubscribableChannel) {

				((ReactiveStreamsSubscribableChannel) replyChannel)
						.subscribeTo(
								Flux.from((Publisher<?>) reply)
										.doOnError((ex) -> sendErrorMessage(requestMessage, ex))
										.map(result -> createOutputMessage(result, requestHeaders)));
			}
			else {
				asyncNonReactiveReply(requestMessage, reply, replyChannel);
			}
		}
		else {
			sendOutput(createOutputMessage(reply, requestHeaders), replyChannel, false);
		}
	}

	private AbstractIntegrationMessageBuilder<?> addRoutingSlipHeader(Object reply, List<?> routingSlip,
			AtomicInteger routingSlipIndex) {

		return messageBuilderForReply(reply)
				.setHeader(IntegrationMessageHeaderAccessor.ROUTING_SLIP,
						Collections.singletonMap(routingSlip, routingSlipIndex.get()));
	}

	protected AbstractIntegrationMessageBuilder<?> messageBuilderForReply(Object reply) {
		AbstractIntegrationMessageBuilder<?> builder;
		if (reply instanceof Message) {
			builder = getMessageBuilderFactory().fromMessage((Message<?>) reply);
		}
		else if (reply instanceof AbstractIntegrationMessageBuilder) {
			builder = (AbstractIntegrationMessageBuilder<?>) reply;
		}
		else {
			builder = getMessageBuilderFactory().withPayload(reply);
		}
		return builder;
	}

	private void asyncNonReactiveReply(Message<?> requestMessage, Object reply, @Nullable Object replyChannel) {
		ListenableFuture<?> future;
		if (reply instanceof ListenableFuture<?>) {
			future = (ListenableFuture<?>) reply;
		}
		else {
			SettableListenableFuture<Object> settableListenableFuture = new SettableListenableFuture<>();
			Mono<?> reactiveReply;
			ReactiveAdapter adapter = ReactiveAdapterRegistry.getSharedInstance().getAdapter(null, reply);
			if (adapter != null && adapter.isMultiValue()) {
				reactiveReply = Mono.just(reply);
			}
			else {
				reactiveReply = Mono.from((Publisher<?>) reply);
			}
			reactiveReply.subscribe(settableListenableFuture::set, settableListenableFuture::setException);
			future = settableListenableFuture;
		}
		future.addCallback(new ReplyFutureCallback(requestMessage, replyChannel));
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
		AbstractIntegrationMessageBuilder<?> builder;
		if (output instanceof Message<?>) {
			if (this.noHeadersPropagation || !shouldCopyRequestHeaders()) {
				return (Message<?>) output;
			}
			builder = getMessageBuilderFactory().fromMessage((Message<?>) output);
		}
		else if (output instanceof AbstractIntegrationMessageBuilder) {
			builder = (AbstractIntegrationMessageBuilder<?>) output;
		}
		else {
			builder = getMessageBuilderFactory().withPayload(output);
		}
		if (!this.noHeadersPropagation &&
				(shouldCopyRequestHeaders() ||
						(!(output instanceof Message<?>) &&
								!(output instanceof AbstractIntegrationMessageBuilder<?>)))) {

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
	 * @param replyChannelArg the 'replyChannel' value from the original request
	 * @param useArgChannel - use the replyChannel argument (must not be null), not
	 * the configured output channel.
	 */
	protected void sendOutput(Object output, @Nullable Object replyChannelArg, boolean useArgChannel) {
		Object replyChannel = replyChannelArg;
		MessageChannel outChannel = getOutputChannel();
		if (!useArgChannel && outChannel != null) {
			replyChannel = outChannel;
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

	protected void sendErrorMessage(Message<?> requestMessage, Throwable ex) {
		Object errorChannel = resolveErrorChannel(requestMessage.getHeaders());
		Throwable result = ex;
		if (!(ex instanceof MessagingException)) {
			result = new MessageHandlingException(requestMessage, ex);
		}
		if (errorChannel == null) {
			logger.error(result,
					"Async exception received and no 'errorChannel' header exists and no default 'errorChannel' found");
		}
		else {
			try {
				sendOutput(new ErrorMessage(result), errorChannel, true);
			}
			catch (Exception e) {
				Exception exceptionToLog =
						IntegrationUtils.wrapInHandlingExceptionIfNecessary(requestMessage,
								() -> "failed to send error message in the [" + this + ']', e);
				logger.error(exceptionToLog, "Failed to send async reply");
			}
		}
	}

	protected Object resolveErrorChannel(final MessageHeaders requestHeaders) {
		Object errorChannel = requestHeaders.getErrorChannel();
		if (errorChannel == null) {
			try {
				errorChannel = getChannelResolver().resolveDestination(IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME);
			}
			catch (DestinationResolutionException e) {
				// ignore
			}
		}
		return errorChannel;
	}

	private final class ReplyFutureCallback implements ListenableFutureCallback<Object> {

		private final Message<?> requestMessage;

		@Nullable
		private final Object replyChannel;

		ReplyFutureCallback(Message<?> requestMessage, @Nullable Object replyChannel) {
			this.requestMessage = requestMessage;
			this.replyChannel = replyChannel;
		}


		@Override
		public void onSuccess(Object result) {
			Message<?> replyMessage = null;
			try {
				replyMessage = createOutputMessage(result, this.requestMessage.getHeaders());
				sendOutput(replyMessage, this.replyChannel, false);
			}
			catch (Exception ex) {
				Exception exceptionToLogAndSend = ex;
				if (!(ex instanceof MessagingException)) { // NOSONAR
					exceptionToLogAndSend = new MessageHandlingException(this.requestMessage, ex);
					if (replyMessage != null) {
						exceptionToLogAndSend = new MessagingException(replyMessage, exceptionToLogAndSend);
					}
				}
				logger.error(exceptionToLogAndSend, () -> "Failed to send async reply: " + result.toString());
				onFailure(exceptionToLogAndSend);
			}
		}

		@Override
		public void onFailure(Throwable ex) {
			sendErrorMessage(this.requestMessage, ex);
		}

	}

}
