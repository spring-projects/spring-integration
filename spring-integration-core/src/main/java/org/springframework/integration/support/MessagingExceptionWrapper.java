/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.integration.support;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * A wrapper exception for a {@link MessagingException} used to convey the cause and
 * original message to a
 * {@link org.springframework.integration.channel.MessagePublishingErrorHandler}.
 * The original message is in this exception's {@link #getFailedMessage() failedMessage}
 * property.
 * <p>Intended for internal framework use only. Error handlers will typically unwrap
 * the cause while creating an error message.
 *
 * @author Gary Russell
 * @since 5.0
 *
 */
public class MessagingExceptionWrapper extends MessagingException {

	private static final long serialVersionUID = 1L;

	public MessagingExceptionWrapper(Message<?> originalMessage, MessagingException cause) {
		super(originalMessage, cause);
	}

}
