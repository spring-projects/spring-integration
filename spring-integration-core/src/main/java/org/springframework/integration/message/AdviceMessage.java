/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.message;

import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

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
 * @author Alexander Makarov
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
	public boolean equals(@Nullable Object o) {
		return o instanceof AdviceMessage<?> that
				&& super.equals(o)
				&& Objects.equals(this.inputMessage, that.inputMessage);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), this.inputMessage);
	}

}
