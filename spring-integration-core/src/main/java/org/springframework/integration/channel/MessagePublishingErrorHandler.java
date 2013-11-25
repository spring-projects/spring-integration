/*
 * Copyright 2002-2009 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
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
 */
public class MessagePublishingErrorHandler implements ErrorHandler, BeanFactoryAware {

	private final Log logger = LogFactory.getLog(this.getClass());

	private volatile DestinationResolver<MessageChannel> channelResolver;

	private volatile MessageChannel defaultErrorChannel;

	private volatile long sendTimeout = 1000;


	public MessagePublishingErrorHandler() {
	}

	public MessagePublishingErrorHandler(DestinationResolver<MessageChannel> channelResolver) {
		Assert.notNull(channelResolver, "channelResolver must not be null");
		this.channelResolver = channelResolver;
	}


	public void setDefaultErrorChannel(MessageChannel defaultErrorChannel) {
		this.defaultErrorChannel = defaultErrorChannel;
	}

	public void setSendTimeout(long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	public void setBeanFactory(BeanFactory beanFactory) {
		Assert.notNull(beanFactory, "beanFactory must not be null");
		if (this.channelResolver == null) {
			this.channelResolver = new BeanFactoryChannelResolver(beanFactory);
		}
	}

	public final void handleError(Throwable t) {
		MessageChannel errorChannel = this.resolveErrorChannel(t);
		boolean sent = false;
		if (errorChannel != null) {
			try {
				if (this.sendTimeout >= 0) {
					sent = errorChannel.send(new ErrorMessage(t), this.sendTimeout);
				}
				else {
					sent = errorChannel.send(new ErrorMessage(t));
				}
			}
			catch (Throwable errorDeliveryError) { // message will be logged only
				if (logger.isWarnEnabled()) {
					logger.warn("Error message was not delivered.", errorDeliveryError);
				}
			}
		}
		if (!sent && logger.isErrorEnabled()) {
			Message<?> failedMessage = (t instanceof MessagingException) ?
					((MessagingException) t).getFailedMessage() : null;
			if (failedMessage != null) {
				logger.error("failure occurred in messaging task with message: " + failedMessage, t);
			}
			else {
				logger.error("failure occurred in messaging task", t);
			}
		}
	}

	private MessageChannel resolveErrorChannel(Throwable t) {
		Message<?> failedMessage = (t instanceof MessagingException) ?
				((MessagingException) t).getFailedMessage() : null;
		if (this.defaultErrorChannel == null && this.channelResolver != null) {
			this.defaultErrorChannel = this.channelResolver.resolveDestination(
					IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME);
		}

		if (failedMessage == null || failedMessage.getHeaders().getErrorChannel() == null) {
			return this.defaultErrorChannel;
		}
		Object errorChannelHeader = failedMessage.getHeaders().getErrorChannel();
		if (errorChannelHeader instanceof MessageChannel) {
			return (MessageChannel) errorChannelHeader;
		}
		Assert.isInstanceOf(String.class, errorChannelHeader,
				"Unsupported error channel header type. Expected MessageChannel or String, but actual type is [" +
				errorChannelHeader.getClass() + "]");
		return this.channelResolver.resolveDestination((String) errorChannelHeader);
	}

}
