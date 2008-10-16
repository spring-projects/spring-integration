/*
 * Copyright 2002-2008 the original author or authors.
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

import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.core.MessagingException;
import org.springframework.integration.message.ErrorMessage;
import org.springframework.integration.util.ErrorHandler;
import org.springframework.util.Assert;

/**
 * {@link ErrorHandler} implementation that sends an {@link ErrorMessage} to a
 * {@link MessageChannel}.
 * 
 * @author Mark Fisher
 */
public class MessagePublishingErrorHandler implements ErrorHandler {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final ChannelResolver channelResolver;

	private volatile MessageChannel defaultErrorChannel;

	private volatile long sendTimeout = 1000;


	public MessagePublishingErrorHandler(ChannelResolver channelResolver) {
		Assert.notNull(channelResolver, "channelResolver must not be null");
		this.channelResolver = channelResolver;
	}


	public void setDefaultErrorChannel(MessageChannel defaultErrorChannel) {
		this.defaultErrorChannel = defaultErrorChannel;
	}

	public void setSendTimeout(long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	public final void handle(Throwable t) {
		Message<?> failedMessage = null;
		if (logger.isWarnEnabled()) {
			if (t instanceof MessagingException) {
				failedMessage = ((MessagingException) t).getFailedMessage();
				logger.warn("failure occurred in messaging task with message: " + failedMessage, t);
			}
			else {
				logger.warn("failure occurred in messaging task", t);
			}
		}
		MessageChannel errorChannel = null;
		if (failedMessage != null) {
			Object errorChannelHeader = failedMessage.getHeaders().getErrorChannel();
			if (errorChannelHeader != null) {
				if (errorChannelHeader instanceof MessageChannel) {
					errorChannel = (MessageChannel) errorChannelHeader;
				}
				else if (errorChannelHeader instanceof String) {
					errorChannel = this.channelResolver.resolveChannelName((String) errorChannelHeader); 
				}
			}
		}
		if (errorChannel == null) {
			errorChannel = this.defaultErrorChannel;
		}
		if (errorChannel != null) {
			try {
				if (this.sendTimeout >= 0) {
					errorChannel.send(new ErrorMessage(t), this.sendTimeout);
				}
				else {
					errorChannel.send(new ErrorMessage(t));
				}
			}
			catch (Throwable ignore) { // message will be logged only
			}
		}
	}

}
