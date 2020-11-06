/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.gateway;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.AttributeAccessor;
import org.springframework.integration.IntegrationPattern;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.integration.MessageTimeoutException;
import org.springframework.integration.channel.ReactiveStreamsSubscribableChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.endpoint.ReactiveStreamsConsumer;
import org.springframework.integration.handler.BridgeHandler;
import org.springframework.integration.history.HistoryWritingMessagePostProcessor;
import org.springframework.integration.mapping.InboundMessageMapper;
import org.springframework.integration.mapping.MessageMappingException;
import org.springframework.integration.mapping.OutboundMessageMapper;
import org.springframework.integration.support.DefaultErrorMessageStrategy;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.ErrorMessageStrategy;
import org.springframework.integration.support.ErrorMessageUtils;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.support.MutableMessageBuilder;
import org.springframework.integration.support.converter.SimpleMessageConverter;
import org.springframework.integration.support.management.IntegrationInboundManagement;
import org.springframework.integration.support.management.IntegrationManagedResource;
import org.springframework.integration.support.management.metrics.MeterFacade;
import org.springframework.integration.support.management.metrics.MetricsCaptor;
import org.springframework.integration.support.management.metrics.SampleFacade;
import org.springframework.integration.support.management.metrics.TimerFacade;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * A convenient base class for connecting application code to
 * {@link MessageChannel}s for sending, receiving, or request-reply operations.
 * Exposes setters for configuring request and reply {@link MessageChannel}s as
 * well as the timeout values for sending and receiving Messages.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
@IntegrationManagedResource
public abstract class MessagingGatewaySupport extends AbstractEndpoint
		implements org.springframework.integration.support.management.TrackableComponent,
		IntegrationInboundManagement, IntegrationPattern {

	private static final long DEFAULT_TIMEOUT = 1000L;

	protected final MessagingTemplate messagingTemplate; // NOSONAR

	private final SimpleMessageConverter messageConverter = new SimpleMessageConverter();

	private final HistoryWritingMessagePostProcessor historyWritingPostProcessor =
			new HistoryWritingMessagePostProcessor();

	private final Object replyMessageCorrelatorMonitor = new Object();

	private final ManagementOverrides managementOverrides = new ManagementOverrides();

	private final Set<TimerFacade> timers = ConcurrentHashMap.newKeySet();

	private boolean errorOnTimeout;

	private ErrorMessageStrategy errorMessageStrategy = new DefaultErrorMessageStrategy();

	private MessageChannel requestChannel;

	private String requestChannelName;

	private MessageChannel replyChannel;

	private String replyChannelName;

	private MessageChannel errorChannel;

	private String errorChannelName;

	private long replyTimeout = DEFAULT_TIMEOUT;

	private InboundMessageMapper<Object> requestMapper = new DefaultRequestMapper();

	private boolean loggingEnabled = true;

	private String managedType;

	private String managedName;

	private MetricsCaptor metricsCaptor;

	private TimerFacade successTimer;

	private volatile AbstractEndpoint replyMessageCorrelator;

	private volatile boolean initialized;


	/**
	 * Construct an instance that will return null if no reply is received.
	 */
	public MessagingGatewaySupport() {
		this(false);
	}

	/**
	 * If errorOnTimeout is true, construct an instance that will send an
	 * {@link ErrorMessage} with a {@link MessageTimeoutException} payload to the error
	 * channel if a reply is expected but none is received. If no error channel is
	 * configured, the {@link MessageTimeoutException} will be thrown.
	 * @param errorOnTimeout true to create the error message.
	 * @since 4.2
	 * @see #setErrorOnTimeout
	 */
	public MessagingGatewaySupport(boolean errorOnTimeout) {
		MessagingTemplate template = new MessagingTemplate();
		template.setMessageConverter(this.messageConverter);
		template.setSendTimeout(DEFAULT_TIMEOUT);
		template.setReceiveTimeout(this.replyTimeout);
		this.messagingTemplate = template;
		this.errorOnTimeout = errorOnTimeout;
	}

	/**
	 * If errorOnTimeout is true, construct an instance that will send an
	 * {@link ErrorMessage} with a {@link MessageTimeoutException} payload to the error
	 * channel if a reply is expected but none is received. If no error channel is
	 * configured, the {@link MessageTimeoutException} will be thrown.
	 * @param errorOnTimeout true to create the error message on reply timeout.
	 * @since 5.2.2
	 */
	public void setErrorOnTimeout(boolean errorOnTimeout) {
		this.errorOnTimeout = errorOnTimeout;
	}

	/**
	 * Set the request channel.
	 * @param requestChannel the channel to which request messages will be sent
	 */
	public void setRequestChannel(MessageChannel requestChannel) {
		this.requestChannel = requestChannel;
	}

	/**
	 * Set the request channel name.
	 * @param requestChannelName the channel bean name to which request messages will be sent
	 * @since 4.1
	 */
	public void setRequestChannelName(String requestChannelName) {
		Assert.hasText(requestChannelName, "'requestChannelName' must not be empty");
		this.requestChannelName = requestChannelName;
	}

	/**
	 * Set the reply channel. If no reply channel is provided, this gateway will
	 * always use an anonymous, temporary channel for handling replies.
	 * @param replyChannel the channel from which reply messages will be received
	 */
	public void setReplyChannel(MessageChannel replyChannel) {
		this.replyChannel = replyChannel;
	}

	/**
	 * Set the reply channel name. If no reply channel is provided, this gateway will
	 * always use an anonymous, temporary channel for handling replies.
	 * @param replyChannelName the channel bean name from which reply messages will be received
	 * @since 4.1
	 */
	public void setReplyChannelName(String replyChannelName) {
		Assert.hasText(replyChannelName, "'replyChannelName' must not be empty");
		this.replyChannelName = replyChannelName;
	}

	/**
	 * Set the error channel. If no error channel is provided, this gateway will
	 * propagate Exceptions to the caller. To completely suppress Exceptions, provide
	 * a reference to the "nullChannel" here.
	 * @param errorChannel The error channel.
	 */
	public void setErrorChannel(MessageChannel errorChannel) {
		this.errorChannel = errorChannel;
	}

	/**
	 * Set the error channel name. If no error channel is provided, this gateway will
	 * propagate Exceptions to the caller. To completely suppress Exceptions, provide
	 * a reference to the "nullChannel" here.
	 * @param errorChannelName The error channel bean name.
	 * @since 4.1
	 */
	public void setErrorChannelName(String errorChannelName) {
		Assert.hasText(errorChannelName, "'errorChannelName' must not be empty");
		this.errorChannelName = errorChannelName;
	}

	/**
	 * Set the timeout value for sending request messages. If not
	 * explicitly configured, the default is one second.
	 * @param requestTimeout the timeout value in milliseconds
	 */
	public void setRequestTimeout(long requestTimeout) {
		this.messagingTemplate.setSendTimeout(requestTimeout);
	}

	/**
	 * Set the timeout value for receiving reply messages. If not
	 * explicitly configured, the default is one second.
	 * @param replyTimeout the timeout value in milliseconds
	 */
	public void setReplyTimeout(long replyTimeout) {
		this.replyTimeout = replyTimeout;
		this.messagingTemplate.setReceiveTimeout(replyTimeout);
	}

	/**
	 * Provide an {@link InboundMessageMapper} for creating request Messages
	 * from any object passed in a send or sendAndReceive operation.
	 * @param requestMapper The request mapper.
	 */
	@SuppressWarnings("unchecked")
	public void setRequestMapper(@Nullable InboundMessageMapper<?> requestMapper) {
		if (requestMapper != null) {
			this.requestMapper = (InboundMessageMapper<Object>) requestMapper;
		}
		this.messageConverter.setInboundMessageMapper(this.requestMapper);
	}

	/**
	 * Provide an {@link OutboundMessageMapper} for mapping to objects from
	 * any reply Messages received in receive or sendAndReceive operations.
	 * @param replyMapper The reply mapper.
	 */
	public void setReplyMapper(OutboundMessageMapper<?> replyMapper) {
		this.messageConverter.setOutboundMessageMapper(replyMapper);
	}

	/**
	 * Specify whether this gateway should be tracked in the Message History
	 * of Messages that originate from its send or sendAndReceive operations.
	 */
	@Override
	public void setShouldTrack(boolean shouldTrack) {
		this.historyWritingPostProcessor.setShouldTrack(shouldTrack);
	}

	@Override
	public String getComponentType() {
		return "gateway";
	}

	@Override
	public void setLoggingEnabled(boolean enabled) {
		this.loggingEnabled = enabled;
		this.managementOverrides.loggingConfigured = true;
	}

	@Override
	public boolean isLoggingEnabled() {
		return this.loggingEnabled;
	}

	/**
	 * Set an {@link ErrorMessageStrategy} to use to build an error message when a exception occurs.
	 * Default is the {@link DefaultErrorMessageStrategy}.
	 * @param errorMessageStrategy the {@link ErrorMessageStrategy}.
	 * @since 4.3.10
	 */
	public final void setErrorMessageStrategy(ErrorMessageStrategy errorMessageStrategy) {
		Assert.notNull(errorMessageStrategy, "'errorMessageStrategy' cannot be null");
		this.errorMessageStrategy = errorMessageStrategy;
	}

	@Override
	public ManagementOverrides getOverrides() {
		return this.managementOverrides;
	}

	@Override
	public void setManagedType(String managedType) {
		this.managedType = managedType;
	}

	@Override
	public String getManagedType() {
		return this.managedType;
	}

	@Override
	public void setManagedName(String managedName) {
		this.managedName = managedName;
	}

	@Override
	public String getManagedName() {
		return this.managedName;
	}

	@Override
	public IntegrationPatternType getIntegrationPatternType() {
		return IntegrationPatternType.inbound_gateway;
	}

	@Override
	public void registerMetricsCaptor(MetricsCaptor metricsCaptorToRegister) {
		this.metricsCaptor = metricsCaptorToRegister;
	}

	@Override
	protected void onInit() {
		Assert.state(!(this.requestChannelName != null && this.requestChannel != null),
				"'requestChannelName' and 'requestChannel' are mutually exclusive.");
		Assert.state(!(this.replyChannelName != null && this.replyChannel != null),
				"'replyChannelName' and 'replyChannel' are mutually exclusive.");
		Assert.state(!(this.errorChannelName != null && this.errorChannel != null),
				"'errorChannelName' and 'errorChannel' are mutually exclusive.");
		this.historyWritingPostProcessor.setTrackableComponent(this);
		MessageBuilderFactory messageBuilderFactory = getMessageBuilderFactory();
		this.historyWritingPostProcessor.setMessageBuilderFactory(messageBuilderFactory);
		BeanFactory beanFactory = getBeanFactory();
		if (beanFactory != null) {
			this.messagingTemplate.setBeanFactory(beanFactory);
			if (this.requestMapper instanceof DefaultRequestMapper) {
				((DefaultRequestMapper) this.requestMapper).setMessageBuilderFactory(messageBuilderFactory);
			}
			if (this.requestMapper instanceof BeanFactoryAware) {
				((BeanFactoryAware) this.requestMapper).setBeanFactory(beanFactory);
			}
			this.messageConverter.setBeanFactory(beanFactory);
		}
		this.initialized = true;
	}

	private void initializeIfNecessary() {
		if (!this.initialized) {
			afterPropertiesSet();
		}
	}

	/**
	 * Return this gateway's request channel.
	 * @return the channel.
	 * @since 4.2
	 */
	@Nullable
	public MessageChannel getRequestChannel() {
		if (this.requestChannel == null && this.requestChannelName != null) {
			this.requestChannel = getChannelResolver().resolveDestination(this.requestChannelName);
		}
		return this.requestChannel;
	}

	/**
	 * Return this gateway's reply channel if any.
	 * @return the reply channel instance
	 * @since 5.1
	 */
	@Nullable
	public MessageChannel getReplyChannel() {
		if (this.replyChannel == null && this.replyChannelName != null) {
			this.replyChannel = getChannelResolver().resolveDestination(this.replyChannelName);
		}
		return this.replyChannel;
	}

	/**
	 * Return the error channel (if provided) to which error messages will
	 * be routed.
	 * @return the channel or null.
	 * @since 4.3
	 */
	@Nullable
	public MessageChannel getErrorChannel() {
		if (this.errorChannel == null && this.errorChannelName != null) {
			this.errorChannel = getChannelResolver().resolveDestination(this.errorChannelName);
		}
		return this.errorChannel;
	}

	protected void send(Object object) {
		initializeIfNecessary();
		Assert.notNull(object, "request must not be null");
		MessageChannel channel = getRequestChannel();
		Assert.state(channel != null,
				"send is not supported, because no request channel has been configured");
		SampleFacade sample = null;
		if (this.metricsCaptor != null) {
			sample = this.metricsCaptor.start();
		}
		try {
			this.messagingTemplate.convertAndSend(channel, object, this.historyWritingPostProcessor);
			if (sample != null) {
				sample.stop(sendTimer());
			}
		}
		catch (Exception e) {
			if (sample != null) {
				sample.stop(buildSendTimer(false, e.getClass().getSimpleName()));
			}
			MessageChannel errorChan = getErrorChannel();
			if (errorChan != null) {
				this.messagingTemplate.send(errorChan, new ErrorMessage(e));
			}
			else {
				rethrow(e, "failed to send message");
			}
		}
	}

	@Nullable
	protected Object receive() {
		initializeIfNecessary();
		MessageChannel channel = getReplyChannel();
		assertPollableChannel(channel);
		return this.messagingTemplate.receiveAndConvert(channel, Object.class);
	}

	@Nullable
	protected Message<?> receiveMessage() {
		initializeIfNecessary();
		MessageChannel channel = getReplyChannel();
		assertPollableChannel(channel);
		return this.messagingTemplate.receive(channel);
	}

	@Nullable
	protected Object receive(long timeout) {
		initializeIfNecessary();
		MessageChannel channel = getReplyChannel();
		assertPollableChannel(channel);
		return this.messagingTemplate.receiveAndConvert(channel, timeout);
	}

	@Nullable
	protected Message<?> receiveMessage(long timeout) {
		initializeIfNecessary();
		MessageChannel channel = getReplyChannel();
		assertPollableChannel(channel);
		return this.messagingTemplate.receive(channel, timeout);
	}

	private void assertPollableChannel(@Nullable MessageChannel channel) {
		Assert.state(channel instanceof PollableChannel,
				"receive is not supported, because no pollable reply channel has been configured");
	}

	@Nullable
	protected Object sendAndReceive(Object object) {
		return doSendAndReceive(object, true);
	}

	@Nullable
	protected Message<?> sendAndReceiveMessage(Object object) {
		return (Message<?>) doSendAndReceive(object, false);
	}

	@Nullable // NOSONAR
	private Object doSendAndReceive(Object object, boolean shouldConvert) {
		initializeIfNecessary();
		Assert.notNull(object, "request must not be null");
		MessageChannel channel = getRequestChannel();
		if (channel == null) {
			throw new MessagingException("No request channel available. Cannot send request message.");
		}

		registerReplyMessageCorrelatorIfNecessary();

		Object reply;
		Message<?> requestMessage = null;
		SampleFacade sample = null;
		try {
			if (this.metricsCaptor != null) {
				sample = this.metricsCaptor.start();
			}
			if (shouldConvert) {
				reply = this.messagingTemplate.convertSendAndReceive(channel, object, Object.class,
						this.historyWritingPostProcessor);
			}
			else {
				requestMessage = (object instanceof Message<?>)
						? (Message<?>) object : this.requestMapper.toMessage(object);
				Assert.state(requestMessage != null, () -> "request mapper resulted in no message for " + object);
				requestMessage = this.historyWritingPostProcessor.postProcessMessage(requestMessage);
				reply = this.messagingTemplate.sendAndReceive(channel, requestMessage);
			}

			if (reply == null && this.errorOnTimeout) {
				throwMessageTimeoutException(object, "No reply received within timeout");
			}
			if (sample != null) {
				sample.stop(sendTimer());
			}
		}
		catch (Throwable ex) { // NOSONAR (catch throwable)
			if (logger.isDebugEnabled()) {
				logger.debug("failure occurred in gateway sendAndReceive: " + ex.getMessage());
			}
			reply = ex;
			if (sample != null) {
				sample.stop(buildSendTimer(false, ex.getClass().getSimpleName()));
			}
		}

		if (reply instanceof Throwable || reply instanceof ErrorMessage) {
			Throwable error =
					reply instanceof ErrorMessage
							? ((ErrorMessage) reply).getPayload()
							: (Throwable) reply;
			return handleSendAndReceiveError(object, requestMessage, error, shouldConvert);
		}
		return reply;
	}

	@Nullable
	private Object handleSendAndReceiveError(Object object, @Nullable Message<?> requestMessage, Throwable error,
			boolean shouldConvert) {

		MessageChannel errorChan = getErrorChannel();
		if (errorChan != null) {
			ErrorMessage errorMessage = buildErrorMessage(requestMessage, error);
			Message<?> errorFlowReply = sendErrorMessageAndReceive(errorChan, errorMessage);
			if (errorFlowReply == null && this.errorOnTimeout) {
				throwMessageTimeoutException(object, "No reply received from error channel within timeout");
				return null; // unreachable
			}
			else {
				return shouldConvert && errorFlowReply != null
						? errorFlowReply.getPayload()
						: errorFlowReply;
			}
		}
		else if (error instanceof Error) {
			throw (Error) error;
		}
		else {
			Throwable errorToReThrow = error;
			if (error instanceof MessagingException &&
					requestMessage != null && requestMessage.getHeaders().getErrorChannel() != null) {
				// We are in nested flow where upstream expects errors in its own errorChannel header.
				errorToReThrow = new MessageHandlingException(requestMessage, error);
			}
			rethrow(errorToReThrow, "gateway received checked Exception");
			return null; // unreachable
		}
	}

	@Nullable
	private Message<?> sendErrorMessageAndReceive(MessageChannel errorChan, ErrorMessage errorMessage) {
		Message<?> errorFlowReply;
		try {
			errorFlowReply = this.messagingTemplate.sendAndReceive(errorChan, errorMessage);
		}
		catch (Exception errorFlowFailure) {
			throw new MessagingException(errorMessage, "failure occurred in error-handling flow",
					errorFlowFailure);
		}
		if (errorFlowReply != null && errorFlowReply.getPayload() instanceof Throwable) {
			rethrow((Throwable) errorFlowReply.getPayload(), "error flow returned an Error Message");
		}
		return errorFlowReply;
	}

	private void throwMessageTimeoutException(Object object, String exceptionMessage) {
		if (object instanceof Message) {
			throw new MessageTimeoutException((Message<?>) object, exceptionMessage);
		}
		else {
			throw new MessageTimeoutException(exceptionMessage);
		}
	}

	protected Mono<Message<?>> sendAndReceiveMessageReactive(Object object) {
		initializeIfNecessary();
		Assert.notNull(object, "request must not be null");
		MessageChannel channel = getRequestChannel();
		if (channel == null) {
			throw new MessagingException("No request channel available. Cannot send request message.");
		}

		registerReplyMessageCorrelatorIfNecessary();

		return doSendAndReceiveMessageReactive(channel, object, false);
	}

	private Mono<Message<?>> doSendAndReceiveMessageReactive(MessageChannel requestChannel, Object object,
			boolean error) {

		Message<?> requestMessage;
		try {
			Message<?> message =
					object instanceof Message<?>
							? (Message<?>) object
							: this.requestMapper.toMessage(object);
			Assert.state(message != null, () -> "request mapper resulted in no message for " + object);
			message = this.historyWritingPostProcessor.postProcessMessage(message);
			requestMessage = message;
		}
		catch (Exception e) {
			throw new MessageMappingException("Cannot map to message: " + object, e);
		}

		return Mono.defer(() -> {
			Object originalReplyChannelHeader = requestMessage.getHeaders().getReplyChannel();
			Object originalErrorChannelHeader = requestMessage.getHeaders().getErrorChannel();

			MonoReplyChannel replyChan = new MonoReplyChannel();

			Message<?> messageToSend = MutableMessageBuilder.fromMessage(requestMessage)
					.setReplyChannel(replyChan)
					.setHeader(this.messagingTemplate.getSendTimeoutHeader(), null)
					.setHeader(this.messagingTemplate.getReceiveTimeoutHeader(), null)
					.setErrorChannel(replyChan)
					.build();

			sendMessageForReactiveFlow(requestChannel, messageToSend);

			return buildReplyMono(requestMessage, replyChan.replyMono.asMono(), error, originalReplyChannelHeader,
					originalErrorChannelHeader);
		})
				.onErrorResume(t -> error ? Mono.error(t) : handleSendError(requestMessage, t));
	}

	private void sendMessageForReactiveFlow(MessageChannel requestChannel, Message<?> requestMessage) {
		if (requestChannel instanceof ReactiveStreamsSubscribableChannel) {
			((ReactiveStreamsSubscribableChannel) requestChannel)
					.subscribeTo(Mono.just(requestMessage));
		}
		else {
			long sendTimeout = sendTimeout(requestMessage);

			boolean sent =
					sendTimeout >= 0
							? requestChannel.send(requestMessage, sendTimeout)
							: requestChannel.send(requestMessage);

			if (!sent) {
				throw new MessageDeliveryException(requestMessage,
						"Failed to send message to channel '" + requestChannel +
								"' within timeout: " + sendTimeout);
			}
		}
	}

	private Mono<Message<?>> buildReplyMono(Message<?> requestMessage, Mono<Message<?>> reply, boolean error,
			@Nullable Object originalReplyChannelHeader, @Nullable Object originalErrorChannelHeader) {

		MetricsCaptor captor = this.metricsCaptor;
		return reply
				.doOnSubscribe(s -> {
					if (!error && captor != null) {
						captor.start().stop(sendTimer());
					}
				})
				.<Message<?>>map(replyMessage -> {
					if (!error && replyMessage instanceof ErrorMessage) {
						ErrorMessage em = (ErrorMessage) replyMessage;
						if (em.getPayload() instanceof MessagingException) {
							throw (MessagingException) em.getPayload();
						}
						else {
							throw new MessagingException(requestMessage, em.getPayload());
						}
					}
					else {
						return MessageBuilder.fromMessage(replyMessage)
								.setHeader(MessageHeaders.REPLY_CHANNEL, originalReplyChannelHeader)
								.setHeader(MessageHeaders.ERROR_CHANNEL, originalErrorChannelHeader)
								.build();
					}
				});
	}

	private Mono<Message<?>> handleSendError(Message<?> requestMessage, Throwable exception) {
		if (logger.isDebugEnabled()) {
			logger.debug("failure occurred in gateway sendAndReceiveReactive: " + exception.getMessage());
		}
		MessageChannel channel = getErrorChannel();
		if (channel != null) {
			ErrorMessage errorMessage = buildErrorMessage(requestMessage, exception);
			try {
				return doSendAndReceiveMessageReactive(channel, errorMessage, true);
			}
			catch (Exception errorFlowFailure) {
				throw new MessagingException(errorMessage, "failure occurred in error-handling flow",
						errorFlowFailure);
			}
		}
		else {
			// no errorChannel so we'll propagate
			throw wrapExceptionIfNecessary(exception, "gateway received checked Exception");
		}
	}

	protected TimerFacade sendTimer() {
		if (this.successTimer == null) {
			this.successTimer = buildSendTimer(true, "none");
		}
		return this.successTimer;
	}

	protected TimerFacade buildSendTimer(boolean success, String exception) {
		TimerFacade timer = this.metricsCaptor.timerBuilder(SEND_TIMER_NAME)
				.tag("type", "source")
				.tag("name", getComponentName() == null ? "unknown" : getComponentName())
				.tag("result", success ? "success" : "failure")
				.tag("exception", exception)
				.description("Send processing time")
				.build();
		this.timers.add(timer);
		return timer;
	}

	private long sendTimeout(Message<?> requestMessage) {
		Long sendTimeout = headerToLong(requestMessage.getHeaders()
				.get(this.messagingTemplate.getSendTimeoutHeader()));
		return (sendTimeout != null ? sendTimeout : this.messagingTemplate.getSendTimeout());
	}

	@Nullable
	private Long headerToLong(@Nullable Object headerValue) {
		if (headerValue instanceof Number) {
			return ((Number) headerValue).longValue();
		}
		else if (headerValue instanceof String) {
			return Long.parseLong((String) headerValue);
		}
		else {
			return null;
		}
	}

	/**
	 * Build an error message for the message and throwable using the configured
	 * {@link ErrorMessageStrategy}.
	 * @param requestMessage the requestMessage.
	 * @param throwable the throwable.
	 * @return the error message.
	 * @since 4.3.10
	 */
	protected final ErrorMessage buildErrorMessage(@Nullable Message<?> requestMessage, Throwable throwable) {
		return this.errorMessageStrategy.buildErrorMessage(throwable, getErrorMessageAttributes(requestMessage));
	}

	/**
	 * Populate an {@link AttributeAccessor} to be used when building an error message
	 * with the {@link #setErrorMessageStrategy(ErrorMessageStrategy)
	 * errorMessageStrategy}.
	 * @param message the message.
	 * @return the attributes.
	 * @since 4.3.10
	 */
	protected AttributeAccessor getErrorMessageAttributes(@Nullable Message<?> message) {
		return ErrorMessageUtils.getAttributeAccessor(message, null);
	}

	private void rethrow(Throwable t, String description) {
		throw wrapExceptionIfNecessary(t, description);
	}

	private RuntimeException wrapExceptionIfNecessary(Throwable t, String description) {
		if (t instanceof RuntimeException) {
			return (RuntimeException) t;
		}
		else {
			return new MessagingException(description, t);
		}
	}

	protected void registerReplyMessageCorrelatorIfNecessary() {
		MessageChannel replyChan = getReplyChannel();
		if (replyChan != null && this.replyMessageCorrelator == null) {
			synchronized (this.replyMessageCorrelatorMonitor) {
				if (this.replyMessageCorrelator != null) {
					return;
				}
				AbstractEndpoint correlator;
				BridgeHandler handler = new BridgeHandler();
				BeanFactory beanFactory = getBeanFactory();
				if (beanFactory != null) {
					handler.setBeanFactory(beanFactory);
				}
				handler.afterPropertiesSet();
				if (replyChan instanceof SubscribableChannel) {
					correlator = new EventDrivenConsumer((SubscribableChannel) replyChan, handler);
				}
				else if (replyChan instanceof PollableChannel) {
					PollingConsumer endpoint = new PollingConsumer((PollableChannel) replyChan, handler);
					endpoint.setReceiveTimeout(this.replyTimeout);
					correlator = endpoint;
				}
				else if (replyChan instanceof ReactiveStreamsSubscribableChannel) {
					correlator = new ReactiveStreamsConsumer(replyChan, (Subscriber<Message<?>>) handler);
				}
				else {
					throw new MessagingException("Unsupported 'replyChannel' type [" + replyChan.getClass() + "]."
							+ "SubscribableChannel or PollableChannel type are supported.");
				}

				if (beanFactory != null) {
					correlator.setBeanFactory(beanFactory);
				}
				correlator.afterPropertiesSet();
				this.replyMessageCorrelator = correlator;
			}
			if (isRunning()) {
				this.replyMessageCorrelator.start();
			}
		}
	}

	@Override // guarded by super#lifecycleLock
	protected void doStart() {
		if (this.replyMessageCorrelator != null) {
			this.replyMessageCorrelator.start();
		}
	}

	@Override // guarded by super#lifecycleLock
	protected void doStop() {
		if (this.replyMessageCorrelator != null) {
			this.replyMessageCorrelator.stop();
		}
	}

	@Override
	public void destroy() {
		super.destroy();
		this.timers.forEach(MeterFacade::remove);
		this.timers.clear();
	}

	private static class DefaultRequestMapper implements InboundMessageMapper<Object> {

		private volatile MessageBuilderFactory messageBuilderFactory = new DefaultMessageBuilderFactory();

		DefaultRequestMapper() {
		}

		void setMessageBuilderFactory(MessageBuilderFactory messageBuilderFactory) {
			this.messageBuilderFactory = messageBuilderFactory;
		}

		@Override
		public Message<?> toMessage(Object object, @Nullable Map<String, Object> headers) {
			if (object instanceof Message<?>) {
				return (Message<?>) object;
			}
			return this.messageBuilderFactory.withPayload(object).copyHeadersIfAbsent(headers).build();
		}

	}

	private static class MonoReplyChannel implements MessageChannel, ReactiveStreamsSubscribableChannel {

		private final Sinks.One<Message<?>> replyMono = Sinks.one();

		MonoReplyChannel() {
		}

		@Override
		public boolean send(Message<?> message, long timeout) {
			return this.replyMono.tryEmitValue(message).isSuccess();
		}

		@Override
		public void subscribeTo(Publisher<? extends Message<?>> publisher) {
			Mono.from(publisher)
					.subscribe(
							(value) -> this.replyMono.emitValue(value, Sinks.EmitFailureHandler.FAIL_FAST),
							this.replyMono::tryEmitError, this.replyMono::tryEmitEmpty);
		}

	}

}
