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

package org.springframework.integration.message;

import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.ErrorMessage;

/**
 * An error message that is enhanced with a message that is available at the
 * stack frame where the error message is generated. Typically this will be a
 * message that begins a subflow, whereas if the {@link Throwable} payload is
 * a {@link org.springframework.messaging.MessagingException}, its failedMessage
 * property will contain the message at the point where the exception occurred.
 *
 * @author Gary Russell
 *
 * @since 4.3.10
 *
 * @deprecated since 4.3.10 in favor of direct {@link ErrorMessage} usage since 5.0.
 */
@Deprecated
public class EnhancedErrorMessage extends ErrorMessage {

	private static final long serialVersionUID = 5857673472822628678L;

	private final Message<?> originalMessage;

	public EnhancedErrorMessage(Throwable payload, Message<?> originalMessage) {
		super(payload);
		this.originalMessage = originalMessage;
	}

	public EnhancedErrorMessage(Throwable payload, MessageHeaders headers, Message<?> originalMessage) {
		super(payload, headers);
		this.originalMessage = originalMessage;
	}

	public EnhancedErrorMessage(Throwable payload, Map<String, Object> headers, Message<?> originalMessage) {
		super(payload, headers);
		this.originalMessage = originalMessage;
	}

	public Message<?> getOriginalMessage() {
		return this.originalMessage;
	}

	@Override
	public String toString() {
		if (this.originalMessage == null) {
			return super.toString();
		}

		StringBuilder sb = new StringBuilder(super.toString());
		sb.append(" for original ").append(this.originalMessage);
		return sb.toString();
	}

}
