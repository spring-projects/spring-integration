/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.integration.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.AttributeAccessor;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.message.EnhancedErrorMessage;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.util.Assert;

/**
 * A {@link RecoveryCallback} that sends the {@link ErrorMessage} after retry exhaustion.
 * It can also be used as a general error message publisher. See
 * {@link #recover(MessagingException)} and {@link #recover(Message, Throwable)}.
 * <p>
 * An {@link ErrorMessageStrategy} can be used to provide customization for the target
 * {@link ErrorMessage} based on the {@link RetryContext} (or the message and/or
 * throwable when using the other {@code recover()} methods).
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.3.9
 */
public abstract class ErrorMessagePublisher implements BeanFactoryAware {

	protected final Log logger = LogFactory.getLog(getClass());

	protected final MessagingTemplate messagingTemplate = new MessagingTemplate();

	private DestinationResolver<MessageChannel> channelResolver;

	private MessageChannel recoveryChannel;

	private String recoveryChannelName;

	private ErrorMessageStrategy errorMessageStrategy = new DefaultErrorMessageStrategy();

	public final void setErrorMessageStrategy(ErrorMessageStrategy errorMessageStrategy) {
		Assert.notNull(errorMessageStrategy, "'errorMessageStrategy' must not be null");
		this.errorMessageStrategy = errorMessageStrategy;
	}

	public final void setRecoveryChannel(MessageChannel recoveryChannel) {
		this.recoveryChannel = recoveryChannel;
	}

	public ErrorMessageStrategy getErrorMessageStrategy() {
		return this.errorMessageStrategy;
	}

	public MessageChannel getRecoveryChannel() {
		populateRecoveryChannel();
		return this.recoveryChannel;
	}

	public void setRecoveryChannelName(String recoveryChannelName) {
		this.recoveryChannelName = recoveryChannelName;
	}

	public void setSendTimeout(long sendTimeout) {
		this.messagingTemplate.setSendTimeout(sendTimeout);
	}

	public void setChannelResolver(DestinationResolver<MessageChannel> channelResolver) {
		this.channelResolver = channelResolver;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		Assert.notNull(beanFactory, "beanFactory must not be null");
		if (this.channelResolver == null) {
			this.channelResolver = new BeanFactoryChannelResolver(beanFactory);
		}
	}

	/**
	 * Publish an error message for the supplied exception.
	 * @param exception the exception.
	 * @throws Exception if the recovery fails.
	 */
	public void recover(MessagingException exception) throws Exception {
		recover(null, exception.getFailedMessage(), exception);
	}

	/**
	 * Publish an error message for the supplied message and throwable. If the throwable
	 * is already a {@link MessagingException} containing the message in its
	 * {@code failedMessage} property, use {@link #recover(MessagingException)} instead.
	 * @param failedMessage the message.
	 * @param throwable the throwable.
	 * @throws Exception if the recovery fails.
	 */
	public void recover(Message<?> failedMessage, Throwable throwable) throws Exception {
		recover(null, failedMessage, throwable);
	}

	/**
	 * Publish an error message for the supplied exception.
	 * @param inputMessage the message that started the subflow.
	 * @param exception the exception.
	 * @throws Exception if the recovery fails.
	 */
	public void recover(Message<?> inputMessage, MessagingException exception) throws Exception {
		recover(inputMessage, exception.getFailedMessage(), exception);
	}

	/**
	 * Publish an error message for the supplied message and throwable. If the throwable
	 * is already a {@link MessagingException} containing the message in its
	 * {@code failedMessage} property, use {@link #recover(MessagingException)} instead.
	 * @param inputMessage the message that started the subflow.
	 * @param failedMessage the message.
	 * @param throwable the throwable.
	 * @throws Exception if the recovery fails.
	 */
	public void recover(Message<?> inputMessage, Message<?> failedMessage, Throwable throwable) throws Exception {
		recover(throwable, ErrorMessageUtils.getAttributeAccessor(inputMessage, failedMessage));
	}

	public Object recover(Throwable throwable, AttributeAccessor context) throws Exception {
		populateRecoveryChannel();
		Throwable payload = determinePayload(throwable, context);
		ErrorMessage errorMessage = this.errorMessageStrategy.buildErrorMessage(payload, context);
		if (this.logger.isDebugEnabled() && payload instanceof MessagingException) {
			MessagingException exception = (MessagingException) errorMessage.getPayload();
			this.logger.debug("Sending ErrorMessage: failedMessage: " + exception.getFailedMessage(), exception);
		}
		this.messagingTemplate.send(errorMessage);
		return null;
	}

	protected Throwable determinePayload(Throwable throwable, AttributeAccessor context) {
		Throwable lastThrowable = throwable;
		if (lastThrowable == null) {
			lastThrowable = payloadWhenNull(context);
		}
		else if (!(lastThrowable instanceof MessagingException)) {
			lastThrowable = new MessagingException(
					(Message<?>) context.getAttribute(ErrorMessageUtils.FAILED_MESSAGE_CONTEXT_KEY),
					lastThrowable.getMessage(), lastThrowable);
		}
		return lastThrowable;
	}

	protected Throwable payloadWhenNull(AttributeAccessor context) {
		return new MessagingException((Message<?>) context.getAttribute(ErrorMessageUtils.FAILED_MESSAGE_CONTEXT_KEY),
				"No root cause exception available");
	}

	private void populateRecoveryChannel() {
		if (this.messagingTemplate.getDefaultDestination() == null) {
			if (this.recoveryChannel == null) {
				String recoveryChannelName = this.recoveryChannelName;
				if (recoveryChannelName == null) {
					recoveryChannelName = IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME;
				}
				if (this.channelResolver != null) {
					this.recoveryChannel = this.channelResolver.resolveDestination(recoveryChannelName);
				}
			}

			this.messagingTemplate.setDefaultChannel(this.recoveryChannel);
		}
	}

	public static class DefaultErrorMessageStrategy implements ErrorMessageStrategy {

		@Override
		public ErrorMessage buildErrorMessage(Throwable throwable, AttributeAccessor context) {
			Object inputMessage = context.getAttribute(ErrorMessageUtils.INPUT_MESSAGE_CONTEXT_KEY);
			return inputMessage instanceof Message
					? new EnhancedErrorMessage((Message<?>) inputMessage, throwable)
					: new ErrorMessage(throwable);
		}

	}

}
