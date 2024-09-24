/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.message;

import java.util.Map;
import java.util.Objects;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;

/**
 * A message implementation that is produced by an advice after
 * successful message handling.
 * Contains the result of the expression evaluation in the payload
 * and the original message that the advice passed to the
 * handler.
 *
 * @param <T> the payload type.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.2
 */
public class AdviceMessage<T> extends GenericMessage<T> {

	private static final long serialVersionUID = 1L;

	@SuppressWarnings("serial")
	private final Message<?> inputMessage;

	public AdviceMessage(T payload, Message<?> inputMessage) {
		super(payload);
		this.inputMessage = inputMessage;
	}

	public AdviceMessage(T payload, Map<String, Object> headers, Message<?> inputMessage) {
		super(payload, headers);
		this.inputMessage = inputMessage;
	}

	/**
	 * A constructor with the {@link MessageHeaders} instance to use.
	 * <p><strong>Note:</strong> the given {@link MessageHeaders} instance is used
	 * directly in the new message, i.e. it is not copied.
	 * @param payload the message payload (never {@code null})
	 * @param headers message headers
	 * @param inputMessage the input message for advice.
	 * @since 4.3.10
	 */
	public AdviceMessage(T payload, MessageHeaders headers, Message<?> inputMessage) {
		super(payload, headers);
		this.inputMessage = inputMessage;
	}

	public Message<?> getInputMessage() {
		return this.inputMessage;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(super.toString());
		builder.setLength(builder.length() - 1);
		builder.append(", inputMessage=").append(this.inputMessage.toString()).append("]");
		return builder.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof AdviceMessage)) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		AdviceMessage<?> that = (AdviceMessage<?>) o;
		return Objects.equals(this.inputMessage, that.inputMessage);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), this.inputMessage);
	}

}
