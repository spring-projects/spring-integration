/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.handler.advice;

import org.springframework.core.AttributeAccessor;
import org.springframework.integration.core.ErrorMessagePublisher;
import org.springframework.integration.support.DefaultErrorMessageStrategy;
import org.springframework.integration.support.ErrorMessageStrategy;
import org.springframework.integration.support.ErrorMessageUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryContext;

/**
 * A {@link RecoveryCallback} that sends the final throwable as an
 * {@link org.springframework.messaging.support.ErrorMessage} after
 * retry exhaustion.
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @author Oleg Zhurakousky
 *
 * @since 2.2
 *
 */
public class ErrorMessageSendingRecoverer extends ErrorMessagePublisher implements RecoveryCallback<Object> {

	/**
	 * Construct instance with the default {@code errorChannel}
	 * to publish recovery error message.
	 * The {@link DefaultErrorMessageStrategy} is used for building error message to publish.
	 * @since 4.3.10
	 */
	public ErrorMessageSendingRecoverer() {
		this(null);
	}

	/**
	 * Construct instance based on the provided message channel.
	 * The {@link DefaultErrorMessageStrategy} is used for building error message to publish.
	 * @param channel the message channel to publish error messages on recovery action.
	 */
	public ErrorMessageSendingRecoverer(MessageChannel channel) {
		this(channel, null);
	}

	/**
	 * Construct instance based on the provided message channel and {@link ErrorMessageStrategy}.
	 * In the event provided {@link ErrorMessageStrategy} is null, the {@link DefaultErrorMessageStrategy}
	 * will be used.
	 * @param channel the message channel to publish error messages on recovery action.
	 * @param errorMessageStrategy the {@link ErrorMessageStrategy}
	 * to build error message for publishing. Can be null at which point the
	 * {@link DefaultErrorMessageStrategy} is used.
	 * @since 4.3.10
	 */
	public ErrorMessageSendingRecoverer(MessageChannel channel, ErrorMessageStrategy errorMessageStrategy) {
		setChannel(channel);
		setErrorMessageStrategy(
				errorMessageStrategy == null
						? new DefaultErrorMessageStrategy()
						: errorMessageStrategy);
	}

	@Override
	public Object recover(RetryContext context) {
		publish(context.getLastThrowable(), context);
		return null;
	}

	@Override
	protected Throwable payloadWhenNull(AttributeAccessor context) {
		Message<?> message = (Message<?>) context.getAttribute(ErrorMessageUtils.FAILED_MESSAGE_CONTEXT_KEY);
		String description = "No retry exception available; " +
				"this can occur, for example, if the RetryPolicy allowed zero attempts " +
				"to execute the handler; " +
				"RetryContext: " + context.toString();
		return message == null
				? new RetryExceptionNotAvailableException(description)
				: new RetryExceptionNotAvailableException(message, description);
	}

	public static class RetryExceptionNotAvailableException extends MessagingException {

		private static final long serialVersionUID = 1L;

		RetryExceptionNotAvailableException(String description) {
			super(description);
		}

		public RetryExceptionNotAvailableException(Message<?> message, String description) {
			super(message, description);
		}

	}

}
