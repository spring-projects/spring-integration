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

package org.springframework.integration.bus;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.channel.RendezvousChannel;
import org.springframework.integration.channel.interceptor.ChannelInterceptorAdapter;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;

/**
 * The default error channel implementation used by the {@link MessageBus}.
 * 
 * @author Mark Fisher
 */
public class DefaultErrorChannel extends RendezvousChannel {

	private final Log logger = LogFactory.getLog(this.getClass());


	public DefaultErrorChannel() {
		this.addInterceptor(new ErrorLoggingInterceptor());
		this.setBeanName(ApplicationContextMessageBus.ERROR_CHANNEL_BEAN_NAME);
	}


	private class ErrorLoggingInterceptor extends ChannelInterceptorAdapter {

		@Override
		public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
			if (!sent) {
				if (logger.isWarnEnabled()) {
					logger.warn("Error channel rejected Message. Are any handlers subscribed? " + message);
				}
			}
		}

		/**
		 * Even if the error channel has no subscribers, errors are at least visible at debug level.
		 */
		@Override
		public Message<?> preSend(Message<?> message, MessageChannel channel) {
			if (logger.isDebugEnabled()) {
				String errorMessage = "Error Received. Message: " + message.toString();
				Object payload = message.getPayload();
				if (payload instanceof Throwable) {
					logger.debug(errorMessage, (Throwable) payload);
				}
				else {
					logger.debug(errorMessage);
				}
			}
			return message;
		}
	}

}
