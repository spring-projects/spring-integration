/*
 * Copyright 2002-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.amqp.support;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.JsonMessageConverter;
import org.springframework.integration.MessageHeaders;

/**
 * @author Mark Fisher
 * @since 2.1
 */
public class DefaultAmqpHeaderMapperTests {

	@Test
	public void replyChannelNotMappedToAmqpProperties() {
		DefaultAmqpHeaderMapper headerMapper = new DefaultAmqpHeaderMapper();
		Map<String, Object> headerMap = new HashMap<String, Object>();
		headerMap.put(MessageHeaders.REPLY_CHANNEL, "foo");
		MessageHeaders integrationHeaders = new MessageHeaders(headerMap);
		MessageProperties amqpProperties = new MessageProperties();
		headerMapper.fromHeaders(integrationHeaders, amqpProperties);
		assertEquals(null, amqpProperties.getHeaders().get(MessageHeaders.REPLY_CHANNEL));
	}

	@Test
	public void errorChannelNotMappedToAmqpProperties() {
		DefaultAmqpHeaderMapper headerMapper = new DefaultAmqpHeaderMapper();
		Map<String, Object> headerMap = new HashMap<String, Object>();
		headerMap.put(MessageHeaders.ERROR_CHANNEL, "foo");
		MessageHeaders integrationHeaders = new MessageHeaders(headerMap);
		MessageProperties amqpProperties = new MessageProperties();
		headerMapper.fromHeaders(integrationHeaders, amqpProperties);
		assertEquals(null, amqpProperties.getHeaders().get(MessageHeaders.ERROR_CHANNEL));
	}

	@Test // INT-2090
	public void jsonTypeIdNotOverwritten() {
		DefaultAmqpHeaderMapper headerMapper = new DefaultAmqpHeaderMapper();
		JsonMessageConverter converter = new JsonMessageConverter();
		MessageProperties amqpProperties = new MessageProperties();
		converter.toMessage("123", amqpProperties);
		Map<String, Object> headerMap = new HashMap<String, Object>();
		headerMap.put("__TypeId__", "java.lang.Integer");
		MessageHeaders integrationHeaders = new MessageHeaders(headerMap);
		headerMapper.fromHeaders(integrationHeaders, amqpProperties);
		assertEquals("java.lang.String", amqpProperties.getHeaders().get("__TypeId__"));
		Object result = converter.fromMessage(new Message("123".getBytes(), amqpProperties));
		assertEquals(String.class, result.getClass());
	}
}
