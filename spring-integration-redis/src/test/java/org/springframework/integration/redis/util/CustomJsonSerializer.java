/*
 * Copyright 2013 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package org.springframework.integration.redis.util;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.integration.mapping.InboundMessageMapper;
import org.springframework.integration.support.json.Jackson2JsonMessageParser;
import org.springframework.integration.support.json.JsonInboundMessageMapper;
import org.springframework.messaging.Message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
