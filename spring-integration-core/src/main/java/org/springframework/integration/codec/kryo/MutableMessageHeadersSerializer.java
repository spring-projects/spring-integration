/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.codec.kryo;

import java.util.HashMap;
import java.util.Map;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;

import org.springframework.integration.support.MutableMessageHeaders;
import org.springframework.messaging.MessageHeaders;

/**
 * Kryo Serializer for {@link MutableMessageHeaders}.
 *
 * @author David Turanski
 * @author Artem Bilan
 *
 * @since 4.2
 */
class MutableMessageHeadersSerializer extends MessageHeadersSerializer {

	@Override
	public MessageHeaders read(Kryo kryo, Input input, Class<? extends MessageHeaders> type) {
		@SuppressWarnings("unchecked")
		Map<String, Object> headers = kryo.readObject(input, HashMap.class);
		return new MutableMessageHeaders(headers);
	}

}
