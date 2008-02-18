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

package org.springframework.integration.message;

import org.springframework.integration.MessagingException;

/**
 * Exception that indicates an error during message delivery.
 * 
 * @author Mark Fisher
 */
@SuppressWarnings("serial")
public class MessageDeliveryException extends MessagingException {

	private Message<?> undeliveredMessage;


	public MessageDeliveryException() {
		super();
	}

	public MessageDeliveryException(Message<?> undeliveredMessage) {
		this.undeliveredMessage = undeliveredMessage;
	}

	public MessageDeliveryException(String description) {
		super(description);
	}

	public MessageDeliveryException(Message<?> undeliveredMessage, String description) {
		super(description);
		this.undeliveredMessage = undeliveredMessage;
	}

	public MessageDeliveryException(String description, Throwable cause) {
		super(description, cause);
	}


	/**
	 * Return the undelivered {@link Message} if available, may be null.
	 */
	public Message<?> getUndeliveredMessage() {
		return this.undeliveredMessage;
	}

}
