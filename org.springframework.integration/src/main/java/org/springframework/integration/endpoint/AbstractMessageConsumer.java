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

package org.springframework.integration.endpoint;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageConsumer;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.util.ErrorHandler;
import org.springframework.util.Assert;

/**
 * Base class for MessageConsumer implementations that provides basic
 * validation and error handling capabilities. Asserts that the incoming
 * Message is not null and that it does not contain a null payload. For custom
 * error handling, provide a reference to an {@link ErrorHandler}.  
 * 
 * @author Mark Fisher
 */
public abstract class AbstractMessageConsumer implements MessageConsumer {

	protected final Log logger = LogFactory.getLog(this.getClass());

	private volatile ErrorHandler errorHandler;


	/**
	 * Provide an error handler for any Exceptions that occur
	 * upon invocation of this consumer. If none is provided,
	 * the Exception messages will be logged (at warn level),
	 * and the Exception rethrown.
	 */
	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	public final void onMessage(Message<?> message) {
		Assert.notNull(message == null, "Message must not be null");
		Assert.notNull(message.getPayload(), "Message payload must not be null");
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("consumer '" + this + "' processing message: " + message);
		}
		try {
			this.onMessageInternal(message);
		}
		catch (Exception e) {
			if (e instanceof MessagingException) {
				this.handleException((MessagingException) e);
			}
			else {
				this.handleException(new MessageHandlingException(message,
						"failure occurred in consumer '" + this.toString() + "'", e));
			}
		}
	}

	protected void handleException(MessagingException exception) {
		if (this.errorHandler == null) {
			if (this.logger.isWarnEnabled()) {
				this.logger.warn("exception occurred in consumer '" + this + "'", exception);
			}
			throw exception;
		}
		this.errorHandler.handle(exception);
	}

	protected abstract void onMessageInternal(Message<?> message);

}
