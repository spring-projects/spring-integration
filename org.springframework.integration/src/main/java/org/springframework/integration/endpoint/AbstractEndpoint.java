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

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.integration.message.CompositeMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.util.ErrorHandler;

/**
 * The base class for Message Endpoint implementations.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractEndpoint implements MessageEndpoint, BeanNameAware {

	protected final Log logger = LogFactory.getLog(this.getClass());

	private volatile String name;

	private volatile ErrorHandler errorHandler;

	private volatile boolean requiresReply = false;


	public void setBeanName(String name) {
		this.name = name;
	}

	/**
	 * Return the name of this endpoint.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Specify whether this endpoint should throw an Exception when
	 * it returns an invalid reply Message after handling the request.
	 */
	public void setRequiresReply(boolean requiresReply) {
		this.requiresReply = requiresReply;
	}

	/**
	 * Provide an error handler for any Exceptions that occur
	 * upon invocation of this endpoint. If none is provided,
	 * the Exception messages will be logged (at warn level),
	 * and the Exception rethrown.
	 */
	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	public final boolean send(Message<?> requestMessage) {
		if (requestMessage == null || requestMessage.getPayload() == null) {
			throw new IllegalArgumentException("Message and its payload must not be null");
		}
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("endpoint '" + this + "' handling message: " + requestMessage);
		}
		try {
			Message<?> replyMessage = this.handleRequestMessage(requestMessage);
			if (!this.isValidReplyMessage(replyMessage)) {
				if (this.requiresReply) {
					throw new MessageHandlingException(requestMessage,
							"endpoint requires reply but none was received");
				}
			}
			else if (replyMessage instanceof CompositeMessage) {
				for (Message<?> nextReply : (CompositeMessage) replyMessage) {
					this.sendReplyMessage(nextReply, requestMessage);
				}
			}
			else {
				this.sendReplyMessage(replyMessage, requestMessage);
			}
			return true;
		}
		catch (Exception e) {
			if (e instanceof MessagingException) {
				this.handleException((MessagingException) e);
			}
			else {
				this.handleException(new MessageHandlingException(requestMessage,
						"failure occurred in endpoint's send operation", e));
			}
			return false;
		}
	}

	protected abstract Message<?> handleRequestMessage(Message<?> requestMessage);

	protected abstract boolean isValidReplyMessage(Message<?> replyMessage);

	protected abstract void sendReplyMessage(Message<?> replyMessage, Message<?> requestMessage);


	private void handleException(MessagingException exception) {
		if (this.errorHandler == null) {
			if (this.logger.isWarnEnabled()) {
				this.logger.warn("exception occurred in endpoint '" + this.name + "'", exception);
			}
			throw exception;
		}
		this.errorHandler.handle(exception);
	}


	public String toString() {
		return (this.name != null) ? this.name : super.toString();
	}

}
