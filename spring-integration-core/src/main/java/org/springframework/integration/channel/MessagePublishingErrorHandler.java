/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.channel;

import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.core.ErrorMessagePublisher;
import org.springframework.integration.support.ErrorMessageStrategy;
import org.springframework.integration.support.MessagingExceptionWrapper;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.util.Assert;
import org.springframework.util.ErrorHandler;
import org.springframework.util.StringUtils;

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

	private static final ErrorMessageStrategy DEFAULT_ERROR_MESSAGE_STRATEGY = (ex, attrs) -> {
		if (ex instanceof MessagingExceptionWrapper) {
			return new ErrorMessage(ex.getCause(), ((MessagingExceptionWrapper) ex).getFailedMessage());
		}
		else {
			return new ErrorMessage(ex);
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

	public void setDefaultErrorChannel(@Nullable MessageChannel defaultErrorChannel) {
		setChannel(defaultErrorChannel);
	}

	/**
	 * Return the default error channel for this error handler.
	 * @return the error channel.
	 * @since 4.3
	 */
	@Nullable
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
	public final void handleError(Throwable ex) {
		MessageChannel errorChannel = resolveErrorChannel(ex);
		boolean sent = false;
		if (errorChannel != null) {
			try {
				getMessagingTemplate().send(errorChannel, getErrorMessageStrategy().buildErrorMessage(ex, null));
				sent = true;
			}
			catch (Exception errorDeliveryError) {
				// message will be logged only
				if (this.logger.isWarnEnabled()) {
					this.logger.warn("Error message was not delivered.", errorDeliveryError);
				}
			}
		}
		if (!sent && this.logger.isErrorEnabled()) {
			Message<?> failedMessage =
					ex instanceof MessagingException
							? ((MessagingException) ex).getFailedMessage()
							: null;

			this.logger.error("failure occurred in messaging task" +
					(failedMessage != null ? " with message: " + failedMessage : ""), ex);
		}
	}

	@Nullable
	private MessageChannel resolveErrorChannel(Throwable t) {
		DestinationResolver<MessageChannel> channelResolver = getChannelResolver();
		Throwable actualThrowable = t;
		if (t instanceof MessagingExceptionWrapper) {
			actualThrowable = t.getCause();
		}
		Message<?> failedMessage =
				actualThrowable instanceof MessagingException
						? ((MessagingException) actualThrowable).getFailedMessage()
						: null;
		if (getDefaultErrorChannel() == null && channelResolver != null) {
			setChannel(channelResolver.resolveDestination(IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME));
		}

		if (failedMessage != null && failedMessage.getHeaders().getErrorChannel() != null) {
			Object errorChannelHeader = failedMessage.getHeaders().getErrorChannel();
			if (errorChannelHeader instanceof MessageChannel) {
				return (MessageChannel) errorChannelHeader;
			}
			Assert.isInstanceOf(String.class, errorChannelHeader, () ->
					"Unsupported error channel header type. Expected MessageChannel or String, but actual type is [" +
							errorChannelHeader.getClass() + "]");
			if (channelResolver != null && StringUtils.hasText((String) errorChannelHeader)) {
				return channelResolver.resolveDestination((String) errorChannelHeader);
			}
		}

		return getDefaultErrorChannel();
	}

}
