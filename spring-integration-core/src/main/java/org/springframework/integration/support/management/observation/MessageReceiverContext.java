/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.support.management.observation;

import java.nio.charset.StandardCharsets;

import io.micrometer.observation.transport.ReceiverContext;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * The {@link ReceiverContext} extension for {@link Message} context.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 */
public class MessageReceiverContext extends ReceiverContext<Message<?>> {

	private final Message<?> message;

	private final String handlerName;

	public MessageReceiverContext(Message<?> message, @Nullable String handlerName) {
		super(MessageReceiverContext::getHeader);
		this.message = message;
		this.handlerName = handlerName != null ? handlerName : "unknown";
	}

	@Override
	public Message<?> getCarrier() {
		return this.message;
	}

	public String getHandlerName() {
		return this.handlerName;
	}

	@Nullable
	private static String getHeader(Message<?> message, String key) {
		Object value = message.getHeaders().get(key);
		return value instanceof byte[] bytes
				? new String(bytes, StandardCharsets.UTF_8)
				: (value != null ? value.toString() : null);
	}

}
