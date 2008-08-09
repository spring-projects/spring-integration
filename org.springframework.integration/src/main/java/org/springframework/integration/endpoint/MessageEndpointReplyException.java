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

import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageDeliveryException;

/**
 * An Exception that indicates a failure occurred while a Message Endpoint
 * was attempting to send a reply message. The Exception provides access to
 * both the reply message and the original request message.
 * 
 * @author Mark Fisher
 */
public class MessageEndpointReplyException extends MessageDeliveryException {

	private final Message<?> requestMessage;


	public MessageEndpointReplyException(Message<?> undeliveredMessage, Message<?> requestMessage) {
		super(undeliveredMessage);
		this.requestMessage = requestMessage;
	}

	public MessageEndpointReplyException(Message<?> undeliveredMessage,  Message<?> requestMessage, String description) {
		super(undeliveredMessage, description);
		this.requestMessage = requestMessage;
	}

	public MessageEndpointReplyException(Message<?> undeliveredMessage, Message<?> requestMessage, String description, Throwable cause) {
		super(undeliveredMessage, description, cause);
		this.requestMessage = requestMessage;
	}


	public Message<?> getOriginalRequestMessage() {
		return this.requestMessage;
	}

	public Message<?> getUndeliveredReplyMessage() {
		return this.getFailedMessage();
	}

}
