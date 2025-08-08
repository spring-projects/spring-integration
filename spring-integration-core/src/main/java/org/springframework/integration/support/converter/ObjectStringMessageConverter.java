/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.support.converter;

import java.nio.charset.Charset;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.StringMessageConverter;

/**
 * A {@link StringMessageConverter} extension to convert any object to string.
 * <p>
 * Delegates to super when payload is {@code byte[]} or {@code String}.
 * Performs {@link Object#toString()} in other cases.
 * <p>
 * This class is intended to serve as a fallback converter for internal message deserialization purposes. Therefore, it
 * is recommended to exclusively use the
 * {@link org.springframework.messaging.converter.AbstractMessageConverter#fromMessage(Message, Class) fromMessage}
 * method with {@code String.class} as the {@code targetClass}.
 *
 * @author Marius Bogoevici
 * @author Artem Bilan
 * @author Falk Hanisch
 *
 * @since 5.0
 */
public class ObjectStringMessageConverter extends StringMessageConverter {

	public ObjectStringMessageConverter(Charset defaultCharset) {
		super(defaultCharset);
	}

	public ObjectStringMessageConverter() {
		super();
	}

	@Override
	protected Object convertFromInternal(Message<?> message, Class<?> targetClass, @Nullable Object conversionHint) {
		Object payload = message.getPayload();
		if (payload instanceof String || payload instanceof byte[]) {
			return super.convertFromInternal(message, targetClass, conversionHint);
		}
		else {
			return payload.toString();
		}
	}

}
