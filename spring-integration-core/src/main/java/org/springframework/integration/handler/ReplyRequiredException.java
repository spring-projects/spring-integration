/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.handler;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * Exception that indicates no reply message is produced by a handler
 * that does have a value of true for the 'requiresReply' property.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.1
 */
@SuppressWarnings("serial")
public class ReplyRequiredException extends MessagingException {

	/**
	 * @param failedMessage the failed message.
	 * @param description the description.
	 */
	public ReplyRequiredException(Message<?> failedMessage, String description) {
		super(failedMessage, description);
	}

	/**
	 * @param failedMessage the failed message.
	 * @param description the description.
	 * @param t the root cause.
	 * @since 4.3
	 */
	public ReplyRequiredException(Message<?> failedMessage, String description, Throwable t) {
		super(failedMessage, description, t);
	}

}
