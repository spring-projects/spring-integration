/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.support.converter;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.MessageBuilder;

/**
 * The simple {@link MessageConverter} implementation which contract is to return
 * {@link Message} as is for both {@code from/to} operations.
 * <p>
 * It is useful in cases of some protocol implementations (e.g. STOMP),
 * which is based on the "Spring Messaging Foundation" and the further logic
 * operates only with {@link Message}s, e.g. Spring Integration Adapters.
 *
 * @author Artem Bilan
 *
 * @since 4.2
 */
public class PassThruMessageConverter implements MessageConverter {

	@Override
	public Object fromMessage(Message<?> message, Class<?> targetClass) {
		return message;
	}

	@Override
	public Message<?> toMessage(Object payload, @Nullable MessageHeaders headers) {
		if (payload instanceof byte[]) {
			return MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		}
		else {
			return null;
		}
	}

}
