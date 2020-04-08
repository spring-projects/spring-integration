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

package org.springframework.integration.endpoint;

import org.reactivestreams.Publisher;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.AttributeAccessor;
import org.springframework.integration.IntegrationPattern;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.integration.channel.ReactiveStreamsSubscribableChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.support.DefaultErrorMessageStrategy;
import org.springframework.integration.support.ErrorMessageStrategy;
import org.springframework.integration.support.ErrorMessageUtils;
import org.springframework.integration.support.management.TrackableComponent;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import reactor.core.publisher.Flux;

/**
 * A support class for producer endpoints that provides a setter for the
 * output channel and a convenience method for sending Messages.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 */
public abstract class MessageProducerSupport extends AbstractEndpoint implements MessageProducer, TrackableComponent,
		SmartInitializingSingleton, IntegrationPattern {

	private final MessagingTemplate messagingTemplate = new MessagingTemplate();

	private ErrorMessageStrategy errorMessageStrategy = new DefaultErrorMessageStrategy();

	private MessageChannel outputChannel;

	private String outputChannelName;

	private MessageChannel errorChannel;

	private String errorChannelName;

	private boolean shouldTrack = false;

	protected MessageProducerSupport() {
		this.setPhase(Integer.MAX_VALUE / 2);
	}

	@Override
	public void setOutputChannel(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
	}

	/**
	 * Set the output channel name; overrides
	 * {@link #setOutputChannel(MessageChannel) outputChannel} if provided.
	 * @param outputChannelName the channel name.
	 * @since 4.3
	 */
	@Override
	public void setOutputChannelName(String outputChannelName) {
		Assert.hasText(outputChannelName, "'outputChannelName' must not be null or empty");
		this.outputChannelName = outputChannelName;
	}

	@Override
	public MessageChannel getOutputChannel() {
		String channelName = this.outputChannelName;
		if (channelName != null) {
			this.outputChannel = getChannelResolver().resolveDestination(channelName);
			this.outputChannelName = null;
		}
		return this.outputChannel;
	}

	public void setErrorChannel(MessageChannel errorChannel) {
		this.errorChannel = errorChannel;
	}

	/**
	 * Set the error channel name. If no error channel is provided, this endpoint will
	 * propagate Exceptions to the message-driven source. To completely suppress
	 * Exceptions, provide a reference to the "nullChannel" here.
	 * @param errorChannelName The error channel bean name.
	 * @since 4.3
	 */
	public void setErrorChannelName(String errorChannelName) {
		Assert.hasText(errorChannelName, "'errorChannelName' must not be empty");
		this.errorChannelName = errorChannelName;
	}

	/**
	 * Return the error channel (if provided) to which error messages will
	 * be routed.
	 * @return the channel or null.
	 * @since 4.3
	 */
	@Nullable
	public MessageChannel getErrorChannel() {
		String channelName = this.errorChannelName;
		if (channelName != null) {
			this.errorChannel = getChannelResolver().resolveDestination(channelName);
			this.errorChannelName = null;
		}
		return this.errorChannel;
	}

	/**
	 * Configure the default timeout value to use for send operations.
	 * May be overridden for individual messages.
	 * @param sendTimeout the send timeout in milliseconds
	 * @see MessagingTemplate#setSendTimeout
	 */
	public void setSendTimeout(long sendTimeout) {
		this.messagingTemplate.setSendTimeout(sendTimeout);
	}

	@Override
	public void setShouldTrack(boolean shouldTrack) {
		this.shouldTrack = shouldTrack;
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

	protected MessagingTemplate getMessagingTemplate() {
		return this.messagingTemplate;
	}

	@Override
	public IntegrationPatternType getIntegrationPatternType() {
		return IntegrationPatternType.inbound_channel_adapter;
	}

	@Override
	public void afterSingletonsInstantiated() {
		Assert.state(this.outputChannel != null || StringUtils.hasText(this.outputChannelName),
				"'outputChannel' or 'outputChannelName' is required");
	}

	@Override
	protected void onInit() {
		super.onInit();
		BeanFactory beanFactory = getBeanFactory();
		if (beanFactory != null) {
			this.messagingTemplate.setBeanFactory(beanFactory);
		}

	}

	/**
	 * Take no action by default.
	 * Subclasses may override this if they
	 * need lifecycle-managed behavior. Protected by 'lifecycleLock'.
	 */
	@Override
	protected void doStart() {
	}

	/**
	 * Take no action by default.
	 * Subclasses may override this if they
	 * need lifecycle-managed behavior.
	 */
	@Override
	protected void doStop() {
	}

	protected void sendMessage(Message<?> messageArg) {
		Message<?> message = messageArg;
		if (message == null) {
			throw new MessagingException("cannot send a null message");
		}
		message = trackMessageIfAny(message);
		try {
			this.messagingTemplate.send(getRequiredOutputChannel(), message);
		}
		catch (RuntimeException ex) {
			if (!sendErrorMessageIfNecessary(message, ex)) {
				throw ex;
			}
		}
	}

	protected void subscribeToPublisher(Publisher<? extends Message<?>> publisher) {
		MessageChannel channelForSubscription = getRequiredOutputChannel();

		Flux<? extends Message<?>> messageFlux =
				Flux.from(publisher)
						.map(this::trackMessageIfAny)
						.doOnComplete(this::stop)
						.doOnCancel(this::stop)
						.takeWhile((message) -> isRunning());

		if (channelForSubscription instanceof ReactiveStreamsSubscribableChannel) {
			((ReactiveStreamsSubscribableChannel) channelForSubscription).subscribeTo(messageFlux);
		}
		else {
			messageFlux
					.doOnNext((message) -> {
						try {
							sendMessage(message);
						}
						catch (Exception ex) {
							logger.error("Error sending a message: " + message, ex);
						}
					})
					.subscribe();
		}
	}

	/**
	 * Send an error message based on the exception and message.
	 * @param message the message.
	 * @param exception the exception.
	 * @return true if the error channel is available and message sent.
	 * @since 4.3.10
	 */
	protected final boolean sendErrorMessageIfNecessary(Message<?> message, Exception exception) {
		MessageChannel channel = getErrorChannel();
		if (channel != null) {
			this.messagingTemplate.send(channel, buildErrorMessage(message, exception));
			return true;
		}
		return false;
	}

	/**
	 * Build an error message for the exception and message using the configured
	 * {@link ErrorMessageStrategy}.
	 * @param message the message.
	 * @param exception the exception.
	 * @return the error message.
	 * @since 4.3.10
	 */
	protected final ErrorMessage buildErrorMessage(Message<?> message, Exception exception) {
		return this.errorMessageStrategy.buildErrorMessage(exception, getErrorMessageAttributes(message));
	}

	/**
	 * Populate an {@link AttributeAccessor} to be used when building an error message
	 * with the {@link #setErrorMessageStrategy(ErrorMessageStrategy)
	 * errorMessageStrategy}.
	 * @param message the message.
	 * @return the attributes.
	 * @since 4.3.10
	 */
	protected AttributeAccessor getErrorMessageAttributes(Message<?> message) {
		return ErrorMessageUtils.getAttributeAccessor(message, null);
	}

	private MessageChannel getRequiredOutputChannel() {
		MessageChannel messageChannel = getOutputChannel();
		Assert.state(messageChannel != null, "The 'outputChannel' or `outputChannelName` must be configured");
		return messageChannel;
	}

	private Message<?> trackMessageIfAny(Message<?> message) {
		if (this.shouldTrack) {
			return MessageHistory.write(message, this, getMessageBuilderFactory());
		}
		else {
			return message;
		}
	}

}
