/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.support.converter;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Falk Hanisch
 *
 * @since 6.2
 */
class ObjectStringMessageConverterTests {

	private static final ArbitraryPayload PAYLOAD = new ArbitraryPayload(1234, "ÄÖÜ");

	@Test
	void testFromMessage() {
		Message<ArbitraryPayload> message = MessageBuilder.withPayload(PAYLOAD).build();
		ObjectStringMessageConverter converter = new ObjectStringMessageConverter();
		Object result = converter.fromMessage(message, String.class);

		assertThat(message.getPayload()).isEqualTo(PAYLOAD);
		assertThat(result).isEqualTo(PAYLOAD.toString());

		result = converter.fromMessage(message, Object.class);
		assertThat(result).isNull();
	}

	@Test
	void testToMessage() {
		String text = PAYLOAD.text();
		ObjectStringMessageConverter converter = new ObjectStringMessageConverter(StandardCharsets.ISO_8859_1);
		Message<?> converted = converter.toMessage(text, null);

		assertThat(converted).isNotNull();
		assertThat(converted.getPayload()).isEqualTo(text.getBytes(StandardCharsets.ISO_8859_1));

		converter.setSerializedPayloadClass(String.class);
		converted = converter.toMessage(text, null);
		assertThat(converted).isNotNull();
		assertThat(converted.getPayload()).isEqualTo(text);

		converted = converter.toMessage(PAYLOAD, null);
		assertThat(converted).isNull();
	}

	private record ArbitraryPayload(Integer id, String text) {

	}

}
