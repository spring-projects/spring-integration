/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.support;

import org.jspecify.annotations.Nullable;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.Assert;

/**
 * The default message builder; creates immutable {@link GenericMessage}s.
 * Named MessageBuilder instead of DefaultMessageBuilder for backwards
 * compatibility.
 *
 * @param <T> the payload type.
 *
 * @author Arjen Poutsma
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Dave Syer
 * @author Gary Russell
 * @author Artem Bilan
 */
public final class MessageBuilder<T> extends BaseMessageBuilder<T, MessageBuilder<T>> {

	/**
	 * Private constructor to be invoked from the static factory methods only.
	 */
	private MessageBuilder(T payload, @Nullable Message<T> originalMessage) {
		super(payload, originalMessage);
	}

	/**
	 * Create a builder for a new {@link Message} instance pre-populated with all the headers copied from the
	 * provided message. The payload of the provided Message will also be used as the payload for the new message.
	 * @param message the Message from which the payload and all headers will be copied
	 * @param <T> The type of the payload.
	 * @return A MessageBuilder.
	 */
	public static <T> MessageBuilder<T> fromMessage(Message<T> message) {
		Assert.notNull(message, "message must not be null");
		return new MessageBuilder<>(message.getPayload(), message);
	}

	/**
	 * Create a builder for a new {@link Message} instance with the provided payload.
	 * @param payload the payload for the new message
	 * @param <T> The type of the payload.
	 * @return A MessageBuilder.
	 */
	public static <T> MessageBuilder<T> withPayload(T payload) {
		return new MessageBuilder<>(payload, null);
	}

}
