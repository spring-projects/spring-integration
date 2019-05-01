/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.integration.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.AttributeAccessor;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.support.DefaultErrorMessageStrategy;
import org.springframework.integration.support.ErrorMessageStrategy;
import org.springframework.integration.support.ErrorMessageUtils;
import org.springframework.integration.support.channel.ChannelResolverUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.util.Assert;

/**
 * The component which can be used as general purpose of errors publishing.
 * Can be called or extended in any error handling or retry scenarios.
 * <p>
 * An {@link ErrorMessageStrategy} can be used to provide customization for the target
 * {@link ErrorMessage} based on the {@link AttributeAccessor} (or the message and/or
 * throwable when using the other {@code publish()} methods).
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.3.10
 */
public class ErrorMessagePublisher implements BeanFactoryAware {

	protected final Log logger = LogFactory.getLog(getClass()); // NOSONAR final

	protected final MessagingTemplate messagingTemplate = new MessagingTemplate(); // NOSONAR final

	private DestinationResolver<MessageChannel> channelResolver;

	private MessageChannel channel;

	private String channelName;

	private ErrorMessageStrategy errorMessageStrategy = new DefaultErrorMessageStrategy();

	public final void setErrorMessageStrategy(ErrorMessageStrategy errorMessageStrategy) {
		Assert.notNull(errorMessageStrategy, "'errorMessageStrategy' must not be null");
		this.errorMessageStrategy = errorMessageStrategy;
	}

	public final void setChannel(MessageChannel channel) {
		this.channel = channel;
	}

	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}

	public ErrorMessageStrategy getErrorMessageStrategy() {
		return this.errorMessageStrategy;
	}

	public MessageChannel getChannel() {
		populateChannel();
		return this.channel;
	}

	public final void setSendTimeout(long sendTimeout) {
		this.messagingTemplate.setSendTimeout(sendTimeout);
	}

	public final void setChannelResolver(DestinationResolver<MessageChannel> channelResolver) {
		Assert.notNull(channelResolver, "channelResolver must not be null");
		this.channelResolver = channelResolver;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		Assert.notNull(beanFactory, "beanFactory must not be null");
		if (this.channelResolver == null) {
			this.channelResolver = ChannelResolverUtils.getChannelResolver(beanFactory);
		}
	}

	protected MessagingTemplate getMessagingTemplate() {
		return this.messagingTemplate;
	}

	@Nullable
	protected DestinationResolver<MessageChannel> getChannelResolver() {
		return this.channelResolver;
	}

	/**
	 * Publish an error message for the supplied exception.
	 * @param exception the exception.
	 */
	public void publish(MessagingException exception) {
		publish(null, exception.getFailedMessage(), exception);
	}

	/**
	 * Publish an error message for the supplied message and throwable. If the throwable
	 * is already a {@link MessagingException} containing the message in its
	 * {@code failedMessage} property, use {@link #publish(MessagingException)} instead.
	 * @param failedMessage the message.
	 * @param throwable the throwable.
	 */
	public void publish(Message<?> failedMessage, Throwable throwable) {
		publish(null, failedMessage, throwable);
	}

	/**
	 * Publish an error message for the supplied exception.
	 * @param inputMessage the message that started the subflow.
	 * @param exception the exception.
	 */
	public void publish(Message<?> inputMessage, MessagingException exception) {
		publish(inputMessage, exception.getFailedMessage(), exception);
	}

	/**
	 * Publish an error message for the supplied message and throwable. If the throwable
	 * is already a {@link MessagingException} containing the message in its
	 * {@code failedMessage} property, use {@link #publish(MessagingException)} instead.
	 * @param inputMessage the message that started the subflow.
	 * @param failedMessage the message.
	 * @param throwable the throwable.
	 */
	public void publish(@Nullable Message<?> inputMessage, Message<?> failedMessage, Throwable throwable) {
		publish(throwable, ErrorMessageUtils.getAttributeAccessor(inputMessage, failedMessage));
	}

	/**
	 * Publish an error message for the supplied throwable and context.
	 * The {@link #errorMessageStrategy} is used to build a {@link ErrorMessage}
	 * to publish.
	 * @param throwable the throwable. May be null.
	 * @param context the context for {@link ErrorMessage} properties.
	 */
	public void publish(Throwable throwable, AttributeAccessor context) {
		populateChannel();
		Throwable payload = determinePayload(throwable, context);
		ErrorMessage errorMessage = this.errorMessageStrategy.buildErrorMessage(payload, context);
		if (this.logger.isDebugEnabled() && payload instanceof MessagingException) {
			MessagingException exception = (MessagingException) errorMessage.getPayload();
			this.logger.debug("Sending ErrorMessage: failedMessage: " + exception.getFailedMessage(), exception);
		}
		this.messagingTemplate.send(errorMessage);
	}

	/**
	 * Build a {@code Throwable payload} for future {@link ErrorMessage}.
	 * @param throwable the error to determine an {@link ErrorMessage} payload. Can be null.
	 * @param context the context for error.
	 * @return the throwable for the {@link ErrorMessage} payload
	 * @see ErrorMessageUtils
	 */
	protected Throwable determinePayload(Throwable throwable, AttributeAccessor context) {
		Throwable lastThrowable = throwable;
		if (lastThrowable == null) {
			lastThrowable = payloadWhenNull(context);
		}
		else if (!(lastThrowable instanceof MessagingException)) {
			Message<?> message = (Message<?>) context.getAttribute(ErrorMessageUtils.FAILED_MESSAGE_CONTEXT_KEY);
			lastThrowable = message == null
					? new MessagingException(lastThrowable.getMessage(), lastThrowable)
					: new MessagingException(message, lastThrowable.getMessage(), lastThrowable);
		}
		return lastThrowable;
	}

	/**
	 * Build a {@code Throwable payload} based on the provided context
	 * for future {@link ErrorMessage} when there is original {@code Throwable}.
	 * @param context the {@link AttributeAccessor} to use for exception properties.
	 * @return the {@code Throwable} for an {@link ErrorMessage} payload.
	 * @see ErrorMessageUtils
	 */
	protected Throwable payloadWhenNull(AttributeAccessor context) {
		Message<?> message = (Message<?>) context.getAttribute(ErrorMessageUtils.FAILED_MESSAGE_CONTEXT_KEY);
		return message == null
				? new MessagingException("No root cause exception available")
				: new MessagingException(message, "No root cause exception available");
	}

	private void populateChannel() {
		if (this.messagingTemplate.getDefaultDestination() == null) {
			if (this.channel == null) {
				String recoveryChannelName = this.channelName;
				if (recoveryChannelName == null) {
					recoveryChannelName = IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME;
				}
				if (this.channelResolver != null) {
					this.channel = this.channelResolver.resolveDestination(recoveryChannelName);
				}
			}

			this.messagingTemplate.setDefaultChannel(this.channel);
		}
	}

}
