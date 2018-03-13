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

package org.springframework.integration.channel;

import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.core.ErrorMessagePublisher;
import org.springframework.integration.support.ErrorMessageStrategy;
import org.springframework.integration.support.MessagingExceptionWrapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.util.Assert;
import org.springframework.util.ErrorHandler;

/**
 * {@link ErrorHandler} implementation that sends an {@link ErrorMessage} to a
 * {@link MessageChannel}.
 *
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
public class MessagePublishingErrorHandler extends ErrorMessagePublisher implements ErrorHandler {

	private static final int DEFAULT_SEND_TIMEOUT = 1000;

	private static final ErrorMessageStrategy DEFAULT_ERROR_MESSAGE_STRATEGY = (t, a) -> {
		if (t instanceof MessagingExceptionWrapper) {
			return new ErrorMessage(t.getCause(), ((MessagingExceptionWrapper) t).getFailedMessage());
		}
		else {
			return new ErrorMessage(t);
		}
	};

	public MessagePublishingErrorHandler() {
		setErrorMessageStrategy(DEFAULT_ERROR_MESSAGE_STRATEGY);
		setSendTimeout(DEFAULT_SEND_TIMEOUT);
	}

	public MessagePublishingErrorHandler(DestinationResolver<MessageChannel> channelResolver) {
		this();
		setChannelResolver(channelResolver);
	}


	public void setDefaultErrorChannel(MessageChannel defaultErrorChannel) {
		setChannel(defaultErrorChannel);
	}

	/**
	 * Return the default error channel for this error handler.
	 * @return the error channel.
	 * @since 4.3
	 */
	public MessageChannel getDefaultErrorChannel() {
		return getChannel();
	}

	/**
	 * Specify the bean name of default error channel for this error handler.
	 * @param defaultErrorChannelName the bean name of the error channel
	 * @since 4.3.3
	 */
	public void setDefaultErrorChannelName(String defaultErrorChannelName) {
		setChannelName(defaultErrorChannelName);
	}

	@Override
	public final void handleError(Throwable t) {
		MessageChannel errorChannel = resolveErrorChannel(t);
		boolean sent = false;
		if (errorChannel != null) {
			try {
				getMessagingTemplate().send(errorChannel, getErrorMessageStrategy().buildErrorMessage(t, null));
				sent = true;
			}
			catch (Throwable errorDeliveryError) { //NOSONAR
				// message will be logged only
				if (this.logger.isWarnEnabled()) {
					this.logger.warn("Error message was not delivered.", errorDeliveryError);
				}
				if (errorDeliveryError instanceof Error) {
					throw ((Error) errorDeliveryError);
				}
			}
		}
		if (!sent && this.logger.isErrorEnabled()) {
			Message<?> failedMessage = (t instanceof MessagingException) ?
					((MessagingException) t).getFailedMessage() : null;
			if (failedMessage != null) {
				this.logger.error("failure occurred in messaging task with message: " + failedMessage, t);
			}
			else {
				this.logger.error("failure occurred in messaging task", t);
			}
		}
	}

	private MessageChannel resolveErrorChannel(Throwable t) {
		Throwable actualThrowable = t;
		if (t instanceof MessagingExceptionWrapper) {
			actualThrowable = t.getCause();
		}
		Message<?> failedMessage = (actualThrowable instanceof MessagingException) ?
				((MessagingException) actualThrowable).getFailedMessage() : null;
		if (getDefaultErrorChannel() == null && getChannelResolver() != null) {
			setChannel(getChannelResolver().resolveDestination(
					IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME));
		}

		if (failedMessage == null || failedMessage.getHeaders().getErrorChannel() == null) {
			return getDefaultErrorChannel();
		}
		Object errorChannelHeader = failedMessage.getHeaders().getErrorChannel();
		if (errorChannelHeader instanceof MessageChannel) {
			return (MessageChannel) errorChannelHeader;
		}
		Assert.isInstanceOf(String.class, errorChannelHeader,
				"Unsupported error channel header type. Expected MessageChannel or String, but actual type is [" +
						errorChannelHeader.getClass() + "]");
		return getChannelResolver().resolveDestination((String) errorChannelHeader);
	}

}
