/*
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.mapping;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.messaging.Message;

/**
 * An {@link OutboundMessageMapper} and {@link InboundMessageMapper} that
 * maps to/from {@code byte[]}.
 *
 * @author Gary Russell
 * @since 5.0
 *
 */
public interface BytesMessageMapper extends InboundMessageMapper<byte[]>, OutboundMessageMapper<byte[]> {

	@Override
	default Message<?> toMessage(byte[] object) {
		return toMessage(object, null);
	}

	@Override
	Message<?> toMessage(byte[] bytes, @Nullable Map<String, Object> headers);

	@Override
	byte[] fromMessage(Message<?> message);

}
