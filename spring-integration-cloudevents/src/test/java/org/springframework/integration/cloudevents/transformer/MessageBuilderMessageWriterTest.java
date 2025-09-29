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

import java.util.HashMap;
import java.util.Map;

import io.cloudevents.CloudEventData;
import io.cloudevents.SpecVersion;
import io.cloudevents.core.format.EventFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.integration.cloudevents.CloudEventsHeaders;
import org.springframework.integration.cloudevents.MessageBuilderMessageWriter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MessageBuilderMessageWriterTest {

	private MessageBuilderMessageWriter writer;

	private MessageBuilderMessageWriter customPrefixWriter;

	@BeforeEach
	void setUp() {
		Map<String, Object> headers = new HashMap<>();
		headers.put("existing-header", "existing-value");
		headers.put("correlation-id", "corr-123");

		this.writer = new MessageBuilderMessageWriter(headers, CloudEventsHeaders.CE_PREFIX);
		this.customPrefixWriter = new MessageBuilderMessageWriter(headers, "CUSTOM_");
	}

	@Test
	void createWithSpecVersionAndDefaultPrefix() {
		MessageBuilderMessageWriter result = this.writer.create(SpecVersion.V1);

		assertThat(result).isNotNull();
		assertThat(result).isSameAs(this.writer);

		Message<byte[]> message = result.end();
		MessageHeaders messageHeaders = message.getHeaders();

		assertThat(messageHeaders.get("existing-header")).isEqualTo("existing-value");
		assertThat(messageHeaders.get("correlation-id")).isEqualTo("corr-123");
		assertThat(messageHeaders.get(CloudEventsHeaders.CE_PREFIX + "specversion")).isEqualTo("1.0");
	}

	@Test
	void createWithSpecVersionAndCustomPrefix() {
		MessageBuilderMessageWriter result = this.customPrefixWriter.create(SpecVersion.V1);

		assertThat(result).isNotNull();
		assertThat(result).isSameAs(this.customPrefixWriter);

		Message<byte[]> message = result.end();
		MessageHeaders messageHeaders = message.getHeaders();

		assertThat(messageHeaders.get("existing-header")).isEqualTo("existing-value");
		assertThat(messageHeaders.get("CUSTOM_specversion")).isEqualTo("1.0");
	}

	@Test
	void withContextAttributeDefaultPrefix() {
		this.writer.create(SpecVersion.V1)
				.withContextAttribute("id", "test-id")
				.withContextAttribute("source", "https://example.com")
				.withContextAttribute("type", "com.example.test");

		Message<byte[]> message = this.writer.end();
		MessageHeaders messageHeaders = message.getHeaders();

		assertThat(messageHeaders.get(CloudEventsHeaders.CE_PREFIX + "id")).isEqualTo("test-id");
		assertThat(messageHeaders.get(CloudEventsHeaders.CE_PREFIX + "source")).isEqualTo("https://example.com");
		assertThat(messageHeaders.get(CloudEventsHeaders.CE_PREFIX + "type")).isEqualTo("com.example.test");
		assertThat(messageHeaders.get(CloudEventsHeaders.CE_PREFIX + "specversion")).isEqualTo("1.0");
	}

	@Test
	void withContextAttributeCustomPrefix() {
		this.customPrefixWriter.create(SpecVersion.V1)
				.withContextAttribute("id", "custom-id")
				.withContextAttribute("source", "https://custom.example.com")
				.withContextAttribute("type", "com.example.custom");

		Message<byte[]> message = this.customPrefixWriter.end();
		MessageHeaders messageHeaders = message.getHeaders();

		assertThat(messageHeaders.get("CUSTOM_id")).isEqualTo("custom-id");
		assertThat(messageHeaders.get("CUSTOM_source")).isEqualTo("https://custom.example.com");
		assertThat(messageHeaders.get("CUSTOM_type")).isEqualTo("com.example.custom");
		assertThat(messageHeaders.get("CUSTOM_specversion")).isEqualTo("1.0");
	}

	@Test
	void withContextAttributeExtensions() {
		this.writer.create(SpecVersion.V1)
				.withContextAttribute("id", "ext-id")
				.withContextAttribute("source", "https://ext.example.com")
				.withContextAttribute("type", "com.example.ext")
				.withContextAttribute("trace-id", "trace-123")
				.withContextAttribute("span-id", "span-456");

		Message<byte[]> message = this.writer.end();
		MessageHeaders messageHeaders = message.getHeaders();

		assertThat(messageHeaders.get(CloudEventsHeaders.CE_PREFIX + "trace-id")).isEqualTo("trace-123");
		assertThat(messageHeaders.get(CloudEventsHeaders.CE_PREFIX + "span-id")).isEqualTo("span-456");
	}

	@Test
	void withContextAttributeOptionalAttributes() {
		this.writer.create(SpecVersion.V1)
				.withContextAttribute("id", "optional-id")
				.withContextAttribute("source", "https://optional.example.com")
				.withContextAttribute("type", "com.example.optional")
				.withContextAttribute("datacontenttype", "application/json")
				.withContextAttribute("dataschema", "https://schema.example.com")
				.withContextAttribute("subject", "test-subject")
				.withContextAttribute("time", "2023-01-01T10:00:00Z");

		Message<byte[]> message = this.writer.end();
		MessageHeaders messageHeaders = message.getHeaders();

		assertThat(messageHeaders.get(CloudEventsHeaders.CE_PREFIX + "datacontenttype")).isEqualTo("application/json");
		assertThat(messageHeaders.get(CloudEventsHeaders.CE_PREFIX + "dataschema")).isEqualTo("https://schema.example.com");
		assertThat(messageHeaders.get(CloudEventsHeaders.CE_PREFIX + "subject")).isEqualTo("test-subject");
		assertThat(messageHeaders.get(CloudEventsHeaders.CE_PREFIX + "time")).isEqualTo("2023-01-01T10:00:00Z");
	}

	@Test
	void testEndWithEmptyPayload() {
		this.writer.create(SpecVersion.V1)
				.withContextAttribute("id", "empty-id")
				.withContextAttribute("source", "https://empty.example.com")
				.withContextAttribute("type", "com.example.empty");

		Message<byte[]> message = this.writer.end();

		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo(new byte[0]);
		assertThat(message.getHeaders().get("existing-header")).isEqualTo("existing-value");
		assertThat(message.getHeaders().get(CloudEventsHeaders.CE_PREFIX + "id")).isEqualTo("empty-id");
	}

	@Test
	void endWithCloudEventData() {
		CloudEventData mockData = mock(CloudEventData.class);
		byte[] testData = "test data content".getBytes();
		when(mockData.toBytes()).thenReturn(testData);

		this.writer.create(SpecVersion.V1)
				.withContextAttribute("id", "data-id")
				.withContextAttribute("source", "https://data.example.com")
				.withContextAttribute("type", "com.example.data");

		Message<byte[]> message = this.writer.end(mockData);

		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo(testData);
		assertThat(message.getHeaders().get(CloudEventsHeaders.CE_PREFIX + "id")).isEqualTo("data-id");
	}

	@Test
	void endWithNullCloudEventData() {
		this.writer.create(SpecVersion.V1)
				.withContextAttribute("id", "null-data-id")
				.withContextAttribute("source", "https://nulldata.example.com")
				.withContextAttribute("type", "com.example.nulldata");

		Message<byte[]> message = this.writer.end(null);

		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo(new byte[0]);
		assertThat(message.getHeaders().get(CloudEventsHeaders.CE_PREFIX + "id")).isEqualTo("null-data-id");
	}

	@Test
	void setEventWithTextPayload() {
		EventFormat mockFormat = mock(EventFormat.class);
		when(mockFormat.serializedContentType()).thenReturn("application/cloudevents+json");

		byte[] eventData = "serialized event data".getBytes();

		this.writer.create(SpecVersion.V1)
				.withContextAttribute("id", "format-id")
				.withContextAttribute("source", "https://format.example.com")
				.withContextAttribute("type", "com.example.format");

		Message<byte[]> message = this.writer.setEvent(mockFormat, eventData);

		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo(eventData);
		assertThat(message.getHeaders().get(CloudEventsHeaders.CONTENT_TYPE)).isEqualTo("application/cloudevents+json");
		assertThat(message.getHeaders().get("existing-header")).isEqualTo("existing-value");
	}

	@Test
	void headersCorrectlyAssignedToMessageHeader() {
		this.writer.create(SpecVersion.V1);
		this.writer.withContextAttribute("id", "preserve-id");
		this.writer.withContextAttribute("source", "https://preserve.example.com");

		Message<byte[]> message = this.writer.end();
		MessageHeaders messageHeaders = message.getHeaders();

		assertThat(messageHeaders.get("existing-header")).isEqualTo("existing-value");
		assertThat(messageHeaders.get("correlation-id")).isEqualTo("corr-123");
		assertThat(messageHeaders.get(CloudEventsHeaders.CE_PREFIX + "id")).isEqualTo("preserve-id");
		assertThat(messageHeaders.get(CloudEventsHeaders.CE_PREFIX + "source")).isEqualTo("https://preserve.example.com");
	}

}
