/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
