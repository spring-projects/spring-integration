/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.codec.kryo;

import java.util.HashMap;
import java.util.Map;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import org.springframework.messaging.MessageHeaders;

/**
 * Kryo Serializer for {@link MessageHeaders}.
 *
 * @author David Turanski
 * @author Artem Bilan
 *
 * @since 4.2
 */
class MessageHeadersSerializer extends Serializer<MessageHeaders> {

	@Override
	public void write(Kryo kryo, Output output, MessageHeaders headers) {
		HashMap<String, Object> map = new HashMap<>();
		for (Map.Entry<String, Object> entry : headers.entrySet()) {
			if (entry.getValue() != null) {
				map.put(entry.getKey(), entry.getValue());
			}
		}
		kryo.writeObject(output, map);
	}

	@Override
	public MessageHeaders read(Kryo kryo, Input input, Class<? extends MessageHeaders> type) {
		@SuppressWarnings("unchecked")
		Map<String, Object> headers = kryo.readObject(input, HashMap.class);
		return new MessageHeaders(headers);
	}

}
