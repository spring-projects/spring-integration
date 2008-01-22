/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.scheduling;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.ErrorMessage;
import org.springframework.integration.util.ErrorHandler;

/**
 * {@link ErrorHandler} implementation that sends an {@link ErrorMessage} to a
 * {@link MessageChannel}.
 * 
 * @author Mark Fisher
 */
public class MessagePublishingErrorHandler implements ErrorHandler {

	private Log logger = LogFactory.getLog(this.getClass());

	private MessageChannel errorChannel;

	private long sendTimeout = 1000;


	public MessagePublishingErrorHandler() {
	}

	public MessagePublishingErrorHandler(MessageChannel errorChannel) {
		this.errorChannel = errorChannel;
	}


	public void setErrorChannel(MessageChannel errorChannel) {
		this.errorChannel = errorChannel;
	}

	public void handle(Throwable t) {
		if (this.errorChannel != null) {
			try {
				this.errorChannel.send(new ErrorMessage(t), this.sendTimeout);
			}
			catch (Throwable ignore) { // message will be logged only
			}
		}
		if (logger.isWarnEnabled()) {
			logger.warn("failure occurred in messaging task", t);
		}
	}

}
