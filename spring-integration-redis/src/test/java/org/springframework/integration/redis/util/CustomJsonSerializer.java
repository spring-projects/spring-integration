/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.redis.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.integration.mapping.InboundMessageMapper;
import org.springframework.integration.support.json.Jackson2JsonMessageParser;
import org.springframework.integration.support.json.JsonInboundMessageMapper;
import org.springframework.messaging.Message;

/**
 * @author Artem Bilan
 * @since 3.0
 */
public class CustomJsonSerializer implements RedisSerializer<Message<?>> {

	private final ObjectMapper objectMapper = new ObjectMapper();

	private final InboundMessageMapper<String> mapper =
			new JsonInboundMessageMapper(String.class, new Jackson2JsonMessageParser());

	@Override
	public byte[] serialize(Message<?> message) throws SerializationException {
		try {
			return this.objectMapper.writeValueAsBytes(message);
		}
		catch (JsonProcessingException e) {
			throw new SerializationException("Fail to serialize 'message' to json.", e);
		}
	}

	@Override
	public Message<?> deserialize(byte[] bytes) throws SerializationException {
		try {
			return mapper.toMessage(new String(bytes));
		}
		catch (Exception e) {
			throw new SerializationException("Fail to deserialize 'message' from json.", e);
		}
	}

}
