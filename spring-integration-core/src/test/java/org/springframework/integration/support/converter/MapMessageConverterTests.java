/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.integration.support.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Map;

import org.junit.Test;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @since 3.0
 *
 */
public class MapMessageConverterTests {

	@Test
	public void testFromMessageToMessage() throws Exception {
		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader("bar", "baz")
				.setHeader("baz", "qux")
				.build();
		MapMessageConverter converter = new MapMessageConverter();
		converter.setHeaderNames("bar");
		@SuppressWarnings("unchecked")
		Map<String, Object> map =  (Map<String, Object>) converter.fromMessage(message, Map.class);
		@SuppressWarnings("unchecked")
		Map<String, Object> headers = (Map<String, Object>) map.get("headers");

		assertNotNull(headers);
		assertNotNull(map.get("payload"));
		assertEquals("foo", map.get("payload"));
		assertNotNull(headers.get("bar"));
		assertEquals("baz", headers.get("bar"));
		assertNull(headers.get("baz"));

		headers.put("baz", "qux");
		Message<?> converted = converter.toMessage(map, null);
		assertEquals("foo", converted.getPayload());
		assertEquals("baz", converted.getHeaders().get("bar"));
		assertEquals("qux", converted.getHeaders().get("baz"));

		converter.setFilterHeadersInToMessage(true);

		converted = converter.toMessage(map, null);
		assertEquals("foo", converted.getPayload());
		assertEquals("baz", converted.getHeaders().get("bar"));
		assertNull(converted.getHeaders().get("baz"));
	}

	@Test
	public void testInvalid() throws Exception {
		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader("bar", "baz")
				.setHeader("baz", "qux")
				.build();
		MapMessageConverter converter = new MapMessageConverter();
		converter.setHeaderNames("bar");
		@SuppressWarnings("unchecked")
		Map<String, Object> map =  (Map<String, Object>) converter.fromMessage(message, Map.class);

		map.remove("payload");

		try {
			converter.toMessage(map, null);
			fail("Expected exception");
		}
		catch (IllegalArgumentException e) {
			assertEquals("'payload' entry cannot be null", e.getMessage());
		}
	}

	@Test
	public void testNoHeaders() throws Exception {
		Message<String> message = MessageBuilder.withPayload("foo")
				.build();
		MapMessageConverter converter = new MapMessageConverter();
		converter.setHeaderNames("bar");
		@SuppressWarnings("unchecked")
		Map<String, Object> map =  (Map<String, Object>) converter.fromMessage(message, Map.class);
		@SuppressWarnings("unchecked")
		Map<String, Object> headers = (Map<String, Object>) map.get("headers");

		assertNotNull(headers);
		assertEquals(0, headers.size());
		map.remove("headers");
		Message<?> converted = converter.toMessage(map, null);
		assertEquals("foo", converted.getPayload());
	}

	@Test
	public void testNotIncludedIfNull() throws Exception {
		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader("bar", null)
				.build();
		MapMessageConverter converter = new MapMessageConverter();
		converter.setHeaderNames("bar");
		@SuppressWarnings("unchecked")
		Map<String, Object> map =  (Map<String, Object>) converter.fromMessage(message, Map.class);
		@SuppressWarnings("unchecked")
		Map<String, Object> headers = (Map<String, Object>) map.get("headers");

		assertNotNull(headers);
		assertNotNull(map.get("payload"));
		assertEquals("foo", map.get("payload"));
		assertFalse(headers.keySet().contains("bar"));
		assertEquals(0, headers.size());
	}
}
