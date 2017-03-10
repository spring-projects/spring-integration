/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.integration.handler.advice;

import org.springframework.integration.support.ErrorMessagePublishingRecoveryCallback;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.retry.RetryContext;

/**
 * RecoveryCallback that sends the final throwable as an ErrorMessage after
 * retry exhaustion.
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.2
 *
 */
public class ErrorMessageSendingRecoverer extends ErrorMessagePublishingRecoveryCallback {

	public ErrorMessageSendingRecoverer() {
		this(null);
	}

	public ErrorMessageSendingRecoverer(MessageChannel channel) {
		setRecoveryChannel(channel);
		setErrorMessageStrategy(new RecovererErrorMessageStrategy());
	}

	private static final class RecovererErrorMessageStrategy implements ErrorMessageStrategy {

		@Override
		public ErrorMessage buildErrorMessage(RetryContext context) {
			Throwable lastThrowable = context.getLastThrowable();
			if (lastThrowable == null) {
				lastThrowable = new RetryExceptionNotAvailableException(
						(Message<?>) context.getAttribute("message"),
						"No retry exception available; " +
								"this can occur, for example, if the RetryPolicy allowed zero attempts " +
								"to execute the handler; " +
								"RetryContext: " + context.toString());
			}
			else if (!(lastThrowable instanceof MessagingException)) {
				lastThrowable = new MessagingException((Message<?>) context.getAttribute("message"),
						lastThrowable.getMessage(), lastThrowable);
			}
			return new ErrorMessage(lastThrowable);
		}

	}

	public static class RetryExceptionNotAvailableException extends MessagingException {

		private static final long serialVersionUID = 1L;

		public RetryExceptionNotAvailableException(Message<?> message, String description) {
			super(message, description);
		}

	}

}
