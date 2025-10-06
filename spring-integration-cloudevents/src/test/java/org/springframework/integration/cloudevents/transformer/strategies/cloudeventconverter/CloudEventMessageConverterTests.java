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

package org.springframework.integration.cloudevents.transformer.strategies.cloudeventconverter;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchIllegalStateException;

public class CloudEventMessageConverterTests {

	private CloudEventMessageConverter converter;

	private CloudEventMessageConverter customPrefixConverter;

	@BeforeEach
	void setUp() {
		this.converter = new CloudEventMessageConverter(CloudEventMessageConverter.CE_PREFIX);
		this.customPrefixConverter = new CloudEventMessageConverter("CUSTOM_");
	}

	@Test
	void toMessageWithCloudEventAndDefaultPrefix() {
		CloudEvent cloudEvent = CloudEventBuilder.v1()
				.withId("test-id")
				.withSource(URI.create("https://example.com"))
				.withType("com.example.test")
				.withData("test data".getBytes())
				.build();

		Map<String, Object> headers = new HashMap<>();
		headers.put("existing-header", "existing-value");
		MessageHeaders messageHeaders = new MessageHeaders(headers);

		Message<?> result = this.converter.toMessage(cloudEvent, messageHeaders);

		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("test data".getBytes());

		MessageHeaders resultHeaders = result.getHeaders();
		assertThat(resultHeaders.get("existing-header")).isEqualTo("existing-value");
		assertThat(resultHeaders.get(CloudEventMessageConverter.CE_PREFIX + "id")).isEqualTo("test-id");
		assertThat(resultHeaders.get(CloudEventMessageConverter.CE_PREFIX + "source")).isEqualTo("https://example.com");
		assertThat(resultHeaders.get(CloudEventMessageConverter.CE_PREFIX + "type")).isEqualTo("com.example.test");
		assertThat(resultHeaders.get(CloudEventMessageConverter.CE_PREFIX + "specversion")).isEqualTo("1.0");
	}

	@Test
	void toMessageWithCloudEventAndCustomPrefix() {
		CloudEvent cloudEvent = CloudEventBuilder.v1()
				.withId("custom-id")
				.withSource(URI.create("https://custom.example.com"))
				.withType("com.example.custom")
				.withData("custom data".getBytes())
				.build();

		Map<String, Object> headers = new HashMap<>();
		headers.put("custom-header", "custom-value");
		MessageHeaders messageHeaders = new MessageHeaders(headers);

		Message<?> result = this.customPrefixConverter.toMessage(cloudEvent, messageHeaders);

		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("custom data".getBytes());

		MessageHeaders resultHeaders = result.getHeaders();
		assertThat(resultHeaders.get("custom-header")).isEqualTo("custom-value");
		assertThat(resultHeaders.get("CUSTOM_id")).isEqualTo("custom-id");
		assertThat(resultHeaders.get("CUSTOM_source")).isEqualTo("https://custom.example.com");
		assertThat(resultHeaders.get("CUSTOM_type")).isEqualTo("com.example.custom");
		assertThat(resultHeaders.get("CUSTOM_specversion")).isEqualTo("1.0");
	}

	@Test
	void toMessageWithCloudEventContainingExtensions() {
		CloudEvent cloudEvent = CloudEventBuilder.v1()
				.withId("ext-id")
				.withSource(URI.create("https://ext.example.com"))
				.withType("com.example.ext")
				.withExtension("spanid", "span-456")
				.withData("extension data".getBytes())
				.build();

		MessageHeaders messageHeaders = new MessageHeaders(new HashMap<>());

		Message<?> result = this.customPrefixConverter.toMessage(cloudEvent, messageHeaders);

		assertThat(result).isNotNull();
		MessageHeaders resultHeaders = result.getHeaders();

		assertThat(resultHeaders.get("CUSTOM_id")).isEqualTo("ext-id");
		assertThat(resultHeaders.get("CUSTOM_spanid")).isEqualTo("span-456");
	}

	@Test
	void toMessageWithCloudEventContainingOptionalAttributes() {
		OffsetDateTime time = OffsetDateTime.now();
		CloudEvent cloudEvent = CloudEventBuilder.v1()
				.withId("optional-id")
				.withSource(URI.create("https://optional.example.com"))
				.withType("com.example.optional")
				.withDataContentType("application/json")
				.withDataSchema(URI.create("https://schema.example.com"))
				.withSubject("test-subject")
				.withTime(time)
				.withData("{\"key\":\"value\"}".getBytes())
				.build();

		MessageHeaders messageHeaders = new MessageHeaders(new HashMap<>());

		Message<?> result = this.converter.toMessage(cloudEvent, messageHeaders);

		assertThat(result).isNotNull();
		MessageHeaders resultHeaders = result.getHeaders();

		assertThat(resultHeaders.get(CloudEventMessageConverter.CE_PREFIX + "datacontenttype")).isEqualTo("application/json");
		assertThat(resultHeaders.get(CloudEventMessageConverter.CE_PREFIX + "dataschema")).isEqualTo("https://schema.example.com");
		assertThat(resultHeaders.get(CloudEventMessageConverter.CE_PREFIX + "subject")).isEqualTo("test-subject");
		assertThat(resultHeaders.get(CloudEventMessageConverter.CE_PREFIX + "time")).isNotNull();
	}

	@Test
	void toMessageWithCloudEventWithoutData() {
		CloudEvent cloudEvent = CloudEventBuilder.v1()
				.withId("no-data-id")
				.withSource(URI.create("https://nodata.example.com"))
				.withType("com.example.nodata")
				.build();

		MessageHeaders messageHeaders = new MessageHeaders(new HashMap<>());

		Message<?> result = this.converter.toMessage(cloudEvent, messageHeaders);

		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo(new byte[0]);

		MessageHeaders resultHeaders = result.getHeaders();
		assertThat(resultHeaders.get(CloudEventMessageConverter.CE_PREFIX + "id")).isEqualTo("no-data-id");
		assertThat(resultHeaders.get(CloudEventMessageConverter.CE_PREFIX + "source")).isEqualTo("https://nodata.example.com");
		assertThat(resultHeaders.get(CloudEventMessageConverter.CE_PREFIX + "type")).isEqualTo("com.example.nodata");
	}

	@Test
	void toMessageWithNonCloudEventPayload() {
		String payload = "regular string payload";
		MessageHeaders messageHeaders = new MessageHeaders(new HashMap<>());

		catchIllegalStateException(() -> this.converter.toMessage(payload, messageHeaders));

	}

	@Test
	void toMessagePreservesExistingHeaders() {
		CloudEvent cloudEvent = CloudEventBuilder.v1()
				.withId("preserve-id")
				.withSource(URI.create("https://preserve.example.com"))
				.withType("com.example.preserve")
				.withData("preserve data".getBytes())
				.build();

		Map<String, Object> headers = new HashMap<>();
		headers.put("correlation-id", "corr-123");
		headers.put("message-timestamp", System.currentTimeMillis());
		headers.put("routing-key", "test.route");
		MessageHeaders messageHeaders = new MessageHeaders(headers);

		Message<?> result = this.converter.toMessage(cloudEvent, messageHeaders);

		assertThat(result).isNotNull();
		MessageHeaders resultHeaders = result.getHeaders();

		assertThat(resultHeaders.get("correlation-id")).isEqualTo("corr-123");
		assertThat(resultHeaders.get("message-timestamp")).isNotNull();
		assertThat(resultHeaders.get("routing-key")).isEqualTo("test.route");

		assertThat(resultHeaders.get(CloudEventMessageConverter.CE_PREFIX + "id")).isEqualTo("preserve-id");
	}

	@Test
	void toMessageWithEmptyHeaders() {
		CloudEvent cloudEvent = CloudEventBuilder.v1()
				.withId("empty-headers-id")
				.withSource(URI.create("https://empty.example.com"))
				.withType("com.example.empty")
				.build();

		MessageHeaders messageHeaders = new MessageHeaders(new HashMap<>());

		Message<?> result = this.converter.toMessage(cloudEvent, messageHeaders);

		assertThat(result).isNotNull();
		MessageHeaders resultHeaders = result.getHeaders();
		assertThat(resultHeaders.size()).isEqualTo(6);
		assertThat(resultHeaders.get(CloudEventMessageConverter.CE_PREFIX + "id")).isEqualTo("empty-headers-id");
	}

	@Test
	void invalidPayloadToMessage() {
		Message<?> message = MessageBuilder.withPayload(Integer.valueOf(1234)).build();
		assertThatIllegalStateException()
				.isThrownBy(() -> this.converter.toMessage(message, new MessageHeaders(new HashMap<>())))
				.withMessage("Payload must be a CloudEvent");

	}

	@Test
	void invalidPayloadFromMessage() {
		Message<?> message = MessageBuilder.withPayload(Integer.valueOf(1234)).build();
		assertThatThrownBy(() -> this.converter.fromMessage(message, Integer.class))
				.hasMessage("Could not parse. Unknown encoding. Invalid content type or spec version");
	}
}
