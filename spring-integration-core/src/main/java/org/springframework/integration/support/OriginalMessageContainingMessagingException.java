/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.integration.support;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * A {@link MessagingException} used to convey the cause and original message to
 * a {@link org.springframework.integration.channel.MessagePublishingErrorHandler}.
 *
 * @author Gary Russell
 * @since 5.0
 *
 */
public class OriginalMessageContainingMessagingException extends MessagingException {

	private static final long serialVersionUID = 1L;

	private final Message<?> originalMessage;

	public OriginalMessageContainingMessagingException(Message<?> originalMessage, MessagingException cause) {
		super((String) null, cause);
		this.originalMessage = originalMessage;
	}

	public Message<?> getOriginalMessage() {
		return this.originalMessage;
	}

}
