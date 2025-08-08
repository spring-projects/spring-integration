/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.support.converter;

import java.util.Map;

import org.junit.Test;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

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
		Map<String, Object> map = (Map<String, Object>) converter.fromMessage(message, Map.class);
		@SuppressWarnings("unchecked")
		Map<String, Object> headers = (Map<String, Object>) map.get("headers");

		assertThat(headers).isNotNull();
		assertThat(map.get("payload")).isNotNull();
		assertThat(map.get("payload")).isEqualTo("foo");
		assertThat(headers.get("bar")).isNotNull();
		assertThat(headers.get("bar")).isEqualTo("baz");
		assertThat(headers.get("baz")).isNull();

		headers.put("baz", "qux");
		Message<?> converted = converter.toMessage(map, null);
		assertThat(converted.getPayload()).isEqualTo("foo");
		assertThat(converted.getHeaders().get("bar")).isEqualTo("baz");
		assertThat(converted.getHeaders().get("baz")).isEqualTo("qux");

		converter.setFilterHeadersInToMessage(true);

		converted = converter.toMessage(map, null);
		assertThat(converted.getPayload()).isEqualTo("foo");
		assertThat(converted.getHeaders().get("bar")).isEqualTo("baz");
		assertThat(converted.getHeaders().get("baz")).isNull();
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
		Map<String, Object> map = (Map<String, Object>) converter.fromMessage(message, Map.class);

		map.remove("payload");

		try {
			converter.toMessage(map, null);
			fail("Expected exception");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("'payload' entry cannot be null");
		}
	}

	@Test
	public void testNoHeaders() throws Exception {
		Message<String> message = MessageBuilder.withPayload("foo")
				.build();
		MapMessageConverter converter = new MapMessageConverter();
		converter.setHeaderNames("bar");
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) converter.fromMessage(message, Map.class);
		@SuppressWarnings("unchecked")
		Map<String, Object> headers = (Map<String, Object>) map.get("headers");

		assertThat(headers).isNotNull();
		assertThat(headers.size()).isEqualTo(0);
		map.remove("headers");
		Message<?> converted = converter.toMessage(map, null);
		assertThat(converted.getPayload()).isEqualTo("foo");
	}

	@Test
	public void testNotIncludedIfNull() throws Exception {
		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader("bar", null)
				.build();
		MapMessageConverter converter = new MapMessageConverter();
		converter.setHeaderNames("bar");
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) converter.fromMessage(message, Map.class);
		@SuppressWarnings("unchecked")
		Map<String, Object> headers = (Map<String, Object>) map.get("headers");

		assertThat(headers).isNotNull();
		assertThat(map.get("payload")).isNotNull();
		assertThat(map.get("payload")).isEqualTo("foo");
		assertThat(headers.keySet().contains("bar")).isFalse();
		assertThat(headers.size()).isEqualTo(0);
	}

}
