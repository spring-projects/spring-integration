/*
 * Copyright 2025-present the original author or authors.
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

package org.springframework.integration.cloudevents.transformer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.integration.cloudevents.transformer.strategies.CloudEventMessageFormatStrategy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;

class ToCloudEventTransformerTest {

	private ToCloudEventTransformer transformer;

	@BeforeEach
	void setUp() {
		String extensionPatterns = "customer-header";
		this.transformer = new ToCloudEventTransformer(extensionPatterns, new CloudEventMessageFormatStrategy("ce-"),
				new CloudEventProperties());
	}

	@Test
	void doTransformWithStringPayload() {
		String payload = "test message";
		Message<String> message = MessageBuilder.withPayload(payload)
				.setHeader("custom-header", "test-value")
				.setHeader("other-header", "other-value")
				.build();

		Object result = this.transformer.doTransform(message);

		assertThat(result).isNotNull();
		assertThat(result).isInstanceOf(Message.class);

		Message<?> resultMessage = (Message<?>) result;
		assertThat(resultMessage.getPayload()).isEqualTo(payload.getBytes());

		// Verify that CloudEvent headers are present in the message
		MessageHeaders headers = resultMessage.getHeaders();
		assertThat(headers).isNotNull();

		// Check that the original other-header is preserved (not mapped to extension)
		assertThat(headers.containsKey("other-header")).isTrue();
		assertThat(headers.get("other-header")).isEqualTo("other-value");

	}

	@Test
	void doTransformWithByteArrayPayload() {
		byte[] payload = "test message".getBytes();
		Message<byte[]> message = MessageBuilder.withPayload(payload).build();

		Object result = transformer.doTransform(message);

		assertThat(result).isNotNull();
		assertThat(result).isInstanceOf(Message.class);

		Message<?> resultMessage = (Message<?>) result;
		assertThat(resultMessage.getPayload()).isNotNull();
		assertThat(resultMessage.getPayload()).isEqualTo(payload);

	}

	@Test
	void doTransformWithObjectPayload() {
		Object payload = new Object() {
			@Override
			public String toString() {
				return "custom object";
			}
		};
		Message<Object> message = MessageBuilder.withPayload(payload).build();

		Object result = transformer.doTransform(message);

		assertThat(result).isNotNull();
		assertThat(result).isInstanceOf(Message.class);

		Message<?> resultMessage = (Message<?>) result;
		assertThat(resultMessage.getPayload()).isNotNull();
		assertThat(resultMessage.getPayload()).isEqualTo(payload.toString().getBytes());
	}

	@Test
	void headerFiltering() {
		String payload = "test message";
		Message<String> message = MessageBuilder.withPayload(payload)
			.setHeader("customer-header", "extension-value")
			.setHeader("regular-header", "regular-value")
			.setHeader("another-regular", "another-value")
			.build();

		Object result = transformer.doTransform(message);

		assertThat(result).isNotNull();
		Message<?> resultMessage = (Message<?>) result;

		// Check that regular headers are preserved
		assertThat(resultMessage.getHeaders().containsKey("regular-header")).isTrue();
		assertThat(resultMessage.getHeaders().containsKey("another-regular")).isTrue();
		assertThat(resultMessage.getHeaders().containsKey("ce-customer-header")).isTrue();
		assertThat(resultMessage.getHeaders().get("regular-header")).isEqualTo("regular-value");
		assertThat(resultMessage.getHeaders().get("another-regular")).isEqualTo("another-value");



	}

	@Test
	void emptyExtensionNames() {
		ToCloudEventTransformer emptyExtensionTransformer = new ToCloudEventTransformer();

		String payload = "test message";
		Message<String> message = MessageBuilder.withPayload(payload)
			.setHeader("some-header", "some-value")
			.build();

		Object result = emptyExtensionTransformer.doTransform(message);

		assertThat(result).isNotNull();
		Message<?> resultMessage = (Message<?>) result;

		// All headers should be preserved when no extension mapping exists
		assertThat(resultMessage.getHeaders().containsKey("some-header")).isTrue();
		assertThat(resultMessage.getHeaders().get("some-header")).isEqualTo("some-value");
	}

	@Test
	void multipleExtensionMappings() {
		String extensionPatterns = "trace-id,span-id,user-id";

		ToCloudEventTransformer extendedTransformer = new ToCloudEventTransformer(extensionPatterns, new CloudEventMessageFormatStrategy("ce-"), new CloudEventProperties());

		String payload = "test message";
		Message<String> message = MessageBuilder.withPayload(payload)
			.setHeader("trace-id", "trace-123")
			.setHeader("span-id", "span-456")
			.setHeader("user-id", "user-789")
			.setHeader("correlation-id", "corr-999")
			.build();

		Object result = extendedTransformer.doTransform(message);

		assertThat(result).isNotNull();
		Message<?> resultMessage = (Message<?>) result;

		// Extension-mapped headers should be converted to cloud event extensions
		assertThat(resultMessage.getHeaders().containsKey("trace-id")).isFalse();
		assertThat(resultMessage.getHeaders().containsKey("span-id")).isFalse();
		assertThat(resultMessage.getHeaders().containsKey("user-id")).isFalse();

		assertThat(resultMessage.getHeaders().containsKey("ce-trace-id")).isTrue();
		assertThat(resultMessage.getHeaders().containsKey("ce-span-id")).isTrue();
		assertThat(resultMessage.getHeaders().containsKey("ce-user-id")).isTrue();

		// Non-mapped header should be preserved
		assertThat(resultMessage.getHeaders().containsKey("correlation-id")).isTrue();
		assertThat(resultMessage.getHeaders().get("correlation-id")).isEqualTo("corr-999");
	}

	@Test
	void emptyStringPayloadHandling() {
		Message<String> message = MessageBuilder.withPayload("").build();

		Object result = transformer.doTransform(message);

		assertThat(result).isNotNull();
		assertThat(result).isInstanceOf(Message.class);
	}

	@Test
	void defaultConstructorUsesDefaultCloudEventProperties() {
		ToCloudEventTransformer defaultTransformer = new ToCloudEventTransformer();

		String payload = "test default properties";
		Message<String> message = MessageBuilder.withPayload(payload).build();

		Object result = defaultTransformer.doTransform(message);

		assertThat(result).isNotNull();
		assertThat(result).isInstanceOf(Message.class);
	}

	@Test
	void testCustomCePrefixInHeaders() {
		CloudEventProperties properties = new CloudEventProperties();
		properties.setCePrefix("CUSTOM_");

		ToCloudEventTransformer customPrefixTransformer = new ToCloudEventTransformer(null, new CloudEventMessageFormatStrategy(properties.getCePrefix()), properties);

		String payload = "test custom prefix";
		Message<String> message = MessageBuilder.withPayload(payload)
			.setHeader("test-header", "test-value")
			.build();

		Object result = customPrefixTransformer.doTransform(message);

		Message<?> resultMessage = getTransformedMessage(result);

		MessageHeaders headers = resultMessage.getHeaders();

		assertThat(headers.get("CUSTOM_id")).isNotNull();
		assertThat(headers.get("CUSTOM_source")).isNotNull();
		assertThat(headers.get("CUSTOM_type")).isNotNull();
		assertThat(headers.get("CUSTOM_specversion")).isEqualTo("1.0");

		assertThat(headers.containsKey("ce-id")).isFalse();
		assertThat(headers.containsKey("ce-source")).isFalse();
		assertThat(headers.containsKey("ce-type")).isFalse();
		assertThat(headers.containsKey("ce-specversion")).isFalse();

		assertThat(headers.get("test-header")).isEqualTo("test-value");
	}

	@Test
	void testCustomPrefixWithExtensions() {
		CloudEventProperties properties = new CloudEventProperties();
		properties.setCePrefix("APP_CE_");

		String extensionPatterns = "trace-id,span-id";
		ToCloudEventTransformer customExtTransformer = new ToCloudEventTransformer(extensionPatterns, new CloudEventMessageFormatStrategy(properties.getCePrefix()), properties);

		String payload = "test custom prefix with extensions";
		Message<String> message = MessageBuilder.withPayload(payload)
			.setHeader("trace-id", "trace-456")
			.setHeader("span-id", "span-789")
			.setHeader("regular-header", "regular-value")
			.build();

		Object result = customExtTransformer.doTransform(message);

		Message<?> resultMessage = getTransformedMessage(result);

		MessageHeaders headers = resultMessage.getHeaders();

		assertThat(headers.get("APP_CE_id")).isNotNull();
		assertThat(headers.get("APP_CE_source")).isNotNull();
		assertThat(headers.get("APP_CE_type")).isNotNull();
		assertThat(headers.get("APP_CE_specversion")).isEqualTo("1.0");
		assertThat(headers.get("APP_CE_trace-id")).isEqualTo("trace-456");
		assertThat(headers.get("APP_CE_span-id")).isEqualTo("span-789");

		assertThat(headers.containsKey("trace-id")).isFalse();
		assertThat(headers.containsKey("span-id")).isFalse();
		assertThat(headers.get("regular-header")).isEqualTo("regular-value");
	}

	@Test
	void testEmptyStringCePrefixBehavior() {
		CloudEventProperties properties = new CloudEventProperties();
		properties.setCePrefix("");

		ToCloudEventTransformer emptyPrefixTransformer = new ToCloudEventTransformer(null, new CloudEventMessageFormatStrategy(properties.getCePrefix()), properties);

		String payload = "test empty prefix";
		Message<String> message = MessageBuilder.withPayload(payload).build();

		Object result = emptyPrefixTransformer.doTransform(message);

		Message<?> resultMessage = getTransformedMessage(result);

		MessageHeaders headers = resultMessage.getHeaders();

		assertThat(headers.get("id")).isNotNull();
		assertThat(headers.get("source")).isNotNull();
		assertThat(headers.get("type")).isNotNull();
		assertThat(headers.get("specversion")).isEqualTo("1.0");

		assertThat(headers.containsKey("ce-id")).isFalse();
		assertThat(headers.containsKey("ce-source")).isFalse();
		assertThat(headers.containsKey("ce-type")).isFalse();
		assertThat(headers.containsKey("ce-specversion")).isFalse();
	}

	private Message<?> getTransformedMessage(Object object) {
		assertThat(object).isNotNull();
		assertThat(object).isInstanceOf(Message.class);

		return (Message<?>) object;
	}

}
