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

package org.springframework.integration.cloudevents.v1.transformer;

import java.net.URI;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;

class ToCloudEventTransformerTest {

	private ToCloudEventTransformer transformer;

	@BeforeEach
	void setUp() {
		String extensionPatterns = "customer-header";
		this.transformer = new ToCloudEventTransformer(extensionPatterns, ToCloudEventTransformer.ConversionType.DEFAULT,
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

		ToCloudEventTransformer extendedTransformer = new ToCloudEventTransformer(extensionPatterns, ToCloudEventTransformer.ConversionType.DEFAULT, new CloudEventProperties());

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
	void avroConversion() {
		ToCloudEventTransformer avroTransformer = new ToCloudEventTransformer(null, ToCloudEventTransformer.ConversionType.AVRO, new CloudEventProperties());

		String payload = "test avro message";
		Message<String> message = MessageBuilder.withPayload(payload)
			.setHeader("source-header", "test-value")
			.build();

		Object result = avroTransformer.doTransform(message);

		assertThat(result).isNotNull();
		assertThat(result).isInstanceOf(Message.class);

		Message<?> resultMessage = (Message<?>) result;
		assertThat(resultMessage.getPayload()).isInstanceOf(byte[].class);

		MessageHeaders headers = resultMessage.getHeaders();
		assertThat(headers.get("content-type")).isEqualTo("application/avro");
		assertThat(headers.containsKey("source-header")).isTrue();
	}

	@Test
	void avroConversionWithExtensions() {
		String extensionPatterns = "trace-id";

		ToCloudEventTransformer avroTransformer = new ToCloudEventTransformer(extensionPatterns, ToCloudEventTransformer.ConversionType.AVRO, new CloudEventProperties());

		String payload = "test avro with extensions";
		Message<String> message = MessageBuilder.withPayload(payload)
			.setHeader("trace-id", "trace-123")
			.setHeader("regular-header", "regular-value")
			.build();

		Object result = avroTransformer.doTransform(message);

		assertThat(result).isNotNull();
		Message<?> resultMessage = (Message<?>) result;
		assertThat(resultMessage.getPayload()).isInstanceOf(byte[].class);

		MessageHeaders headers = resultMessage.getHeaders();
		assertThat(headers.get("content-type")).isEqualTo("application/avro");
		assertThat(headers.containsKey("trace-id")).isFalse();
		assertThat(headers.containsKey("regular-header")).isTrue();
	}

	@Test
	void xmlConversion() {
		ToCloudEventTransformer xmlTransformer = new ToCloudEventTransformer(null, ToCloudEventTransformer.ConversionType.XML, new CloudEventProperties());

		String payload = "test xml message";
		Message<String> message = MessageBuilder.withPayload(payload)
			.setHeader("source-header", "test-value")
			.build();

		Object result = xmlTransformer.doTransform(message);

		assertThat(result).isNotNull();
		assertThat(result).isInstanceOf(Message.class);

		Message<?> resultMessage = (Message<?>) result;
		assertThat(resultMessage.getPayload()).isInstanceOf(String.class);

		String xmlPayload = (String) resultMessage.getPayload();
		assertThat(xmlPayload).contains("<?xml");
		assertThat(xmlPayload).contains("cloudevents");

		MessageHeaders headers = resultMessage.getHeaders();
		assertThat(headers.get("content-type")).isEqualTo("application/xml");
		assertThat(headers.containsKey("source-header")).isTrue();
	}

	@Test
	void xmlConversionWithExtensions() {
		String extensionPatterns = "span-id";

		ToCloudEventTransformer xmlTransformer = new ToCloudEventTransformer(extensionPatterns, ToCloudEventTransformer.ConversionType.XML, new CloudEventProperties());

		String payload = "test xml with extensions";
		Message<String> message = MessageBuilder.withPayload(payload)
			.setHeader("span-id", "span-456")
			.setHeader("regular-header", "regular-value")
			.build();

		Object result = xmlTransformer.doTransform(message);

		assertThat(result).isNotNull();
		Message<?> resultMessage = (Message<?>) result;
		assertThat(resultMessage.getPayload()).isInstanceOf(String.class);

		String xmlPayload = (String) resultMessage.getPayload();
		assertThat(xmlPayload).contains("<?xml");

		MessageHeaders headers = resultMessage.getHeaders();
		assertThat(headers.get("content-type")).isEqualTo("application/xml");
		assertThat(headers.containsKey("span-id")).isFalse();
		assertThat(headers.containsKey("regular-header")).isTrue();
		assertThat(xmlPayload).contains("<span-id xsi:type=\"ce:string\">span-456</span-id>");
	}

	@Test
	void jsonConversion() {
		ToCloudEventTransformer jsonTransformer = new ToCloudEventTransformer(null, ToCloudEventTransformer.ConversionType.JSON, new CloudEventProperties());

		String payload = "test json message";
		Message<String> message = MessageBuilder.withPayload(payload)
			.setHeader("source-header", "test-value")
			.build();

		Object result = jsonTransformer.doTransform(message);

		assertThat(result).isNotNull();
		assertThat(result).isInstanceOf(Message.class);

		Message<?> resultMessage = (Message<?>) result;
		assertThat(resultMessage.getPayload()).isInstanceOf(String.class);

		String jsonPayload = (String) resultMessage.getPayload();
		assertThat(jsonPayload).contains("\"specversion\"");
		assertThat(jsonPayload).contains("\"type\"");
		assertThat(jsonPayload).contains("\"source\"");
		assertThat(jsonPayload).contains("\"id\"");

		MessageHeaders headers = resultMessage.getHeaders();
		assertThat(headers.get("content-type")).isEqualTo("application/json");
		assertThat(headers.containsKey("source-header")).isTrue();
	}

	@Test
	void jsonConversionWithExtensions() {
		String extensionPatterns = "user-id";

		ToCloudEventTransformer jsonTransformer = new ToCloudEventTransformer(extensionPatterns, ToCloudEventTransformer.ConversionType.JSON, new CloudEventProperties());

		String payload = "test json with extensions";
		Message<String> message = MessageBuilder.withPayload(payload)
			.setHeader("user-id", "user-789")
			.setHeader("regular-header", "regular-value")
			.build();

		Object result = jsonTransformer.doTransform(message);

		assertThat(result).isNotNull();
		Message<?> resultMessage = (Message<?>) result;
		assertThat(resultMessage.getPayload()).isInstanceOf(String.class);

		String jsonPayload = (String) resultMessage.getPayload();
		assertThat(jsonPayload).contains("\"specversion\"");
		assertThat(jsonPayload).contains("\"user-id\"");

		MessageHeaders headers = resultMessage.getHeaders();
		assertThat(headers.get("content-type")).isEqualTo("application/json");
		assertThat(headers.containsKey("user-id")).isFalse();
		assertThat(jsonPayload).contains("\"user-id\":\"user-789\"");
	}

	@Test
	void avroConversionWithByteArrayPayload() {
		ToCloudEventTransformer avroTransformer = new ToCloudEventTransformer(null, ToCloudEventTransformer.ConversionType.AVRO, new CloudEventProperties());

		byte[] payload = "test avro bytes".getBytes();
		Message<byte[]> message = MessageBuilder.withPayload(payload).build();

		Object result = avroTransformer.doTransform(message);

		assertThat(result).isNotNull();
		Message<?> resultMessage = (Message<?>) result;
		assertThat(resultMessage.getPayload()).isInstanceOf(byte[].class);
		assertThat(resultMessage.getHeaders().get("content-type")).isEqualTo("application/avro");
	}

	@Test
	void xmlConversionWithObjectPayload() {
		ToCloudEventTransformer xmlTransformer = new ToCloudEventTransformer(null, ToCloudEventTransformer.ConversionType.XML, new CloudEventProperties());

		Object payload = new Object() {
			@Override
			public String toString() {
				return "custom xml object";
			}
		};
		Message<Object> message = MessageBuilder.withPayload(payload).build();

		Object result = xmlTransformer.doTransform(message);

		assertThat(result).isNotNull();
		Message<?> resultMessage = (Message<?>) result;
		assertThat(resultMessage.getPayload()).isInstanceOf(String.class);

		String xmlPayload = (String) resultMessage.getPayload();
		assertThat(xmlPayload).contains("<?xml");
		assertThat(resultMessage.getHeaders().get("content-type")).isEqualTo("application/xml");
	}

	@Test
	void jsonConversionWithEmptyPayload() {
		ToCloudEventTransformer jsonTransformer = new ToCloudEventTransformer(null, ToCloudEventTransformer.ConversionType.JSON, new CloudEventProperties());

		Message<String> message = MessageBuilder.withPayload("").build();

		Object result = jsonTransformer.doTransform(message);

		assertThat(result).isNotNull();
		Message<?> resultMessage = (Message<?>) result;
		assertThat(resultMessage.getPayload()).isInstanceOf(String.class);

		String jsonPayload = (String) resultMessage.getPayload();
		assertThat(jsonPayload).contains("\"specversion\"");
		assertThat(resultMessage.getHeaders().get("content-type")).isEqualTo("application/json");
	}

	@Test
	void cloudEventPropertiesWithCustomValues() {
		CloudEventProperties properties = new CloudEventProperties();
		properties.setId("custom-event-id");
		properties.setSource(URI.create("https://example.com/source"));
		properties.setType("com.example.custom.event");
		properties.setDataContentType("application/json");
		properties.setDataSchema(URI.create("https://example.com/schema"));
		properties.setSubject("custom-subject");
		properties.setTime(OffsetDateTime.now());

		ToCloudEventTransformer customTransformer = new ToCloudEventTransformer(null, ToCloudEventTransformer.ConversionType.JSON, properties);

		String payload = "test custom properties";
		Message<String> message = MessageBuilder.withPayload(payload).build();

		Object result = customTransformer.doTransform(message);

		assertThat(result).isNotNull();
		Message<?> resultMessage = (Message<?>) result;
		assertThat(resultMessage.getPayload()).isInstanceOf(String.class);

		String jsonPayload = (String) resultMessage.getPayload();
		assertThat(jsonPayload).contains("\"id\":\"custom-event-id\"");
		assertThat(jsonPayload).contains("\"source\":\"https://example.com/source\"");
		assertThat(jsonPayload).contains("\"type\":\"com.example.custom.event\"");
		assertThat(jsonPayload).contains("\"datacontenttype\":\"application/json\"");
		assertThat(jsonPayload).contains("\"dataschema\":\"https://example.com/schema\"");
		assertThat(jsonPayload).contains("\"subject\":\"custom-subject\"");
		assertThat(jsonPayload).contains("\"time\":");
	}

	@Test
	void cloudEventPropertiesWithNullValues() {
		CloudEventProperties properties = new CloudEventProperties();
		properties.setId("test-id");
		properties.setSource(URI.create("https://example.com"));
		properties.setType("test.type");

		ToCloudEventTransformer customTransformer = new ToCloudEventTransformer(null, ToCloudEventTransformer.ConversionType.JSON, properties);

		String payload = "test null properties";
		Message<String> message = MessageBuilder.withPayload(payload).build();

		Object result = customTransformer.doTransform(message);

		assertThat(result).isNotNull();
		Message<?> resultMessage = (Message<?>) result;
		assertThat(resultMessage.getPayload()).isInstanceOf(String.class);

		String jsonPayload = (String) resultMessage.getPayload();
		assertThat(jsonPayload).contains("\"id\":\"test-id\"");
		assertThat(jsonPayload).contains("\"source\":\"https://example.com\"");
		assertThat(jsonPayload).contains("\"type\":\"test.type\"");
		assertThat(jsonPayload).doesNotContain("\"datacontenttype\":");
		assertThat(jsonPayload).doesNotContain("\"dataschema\":");
		assertThat(jsonPayload).doesNotContain("\"subject\":");
		assertThat(jsonPayload).doesNotContain("\"time\":");
	}

	@Test
	void cloudEventPropertiesInXmlFormat() {
		CloudEventProperties properties = new CloudEventProperties();
		properties.setId("xml-event-123");
		properties.setSource(URI.create("https://xml.example.com"));
		properties.setType("xml.event.type");
		properties.setSubject("xml-subject");

		ToCloudEventTransformer xmlTransformer = new ToCloudEventTransformer(null, ToCloudEventTransformer.ConversionType.XML, properties);

		String payload = "test xml properties";
		Message<String> message = MessageBuilder.withPayload(payload).build();

		Object result = xmlTransformer.doTransform(message);

		assertThat(result).isNotNull();
		Message<?> resultMessage = (Message<?>) result;
		assertThat(resultMessage.getPayload()).isInstanceOf(String.class);

		String xmlPayload = (String) resultMessage.getPayload();
		assertThat(xmlPayload).contains("<?xml");
		assertThat(xmlPayload).contains("<id>xml-event-123</id>");
		assertThat(xmlPayload).contains("<source>https://xml.example.com</source>");
		assertThat(xmlPayload).contains("<type>xml.event.type</type>");
		assertThat(xmlPayload).contains("<subject>xml-subject</subject>");
	}

	@Test
	void cloudEventPropertiesInAvroFormat() {
		CloudEventProperties properties = new CloudEventProperties();
		properties.setId("avro-event-456");
		properties.setSource(URI.create("https://avro.example.com"));
		properties.setType("avro.event.type");
		properties.setDataContentType("application/avro");

		ToCloudEventTransformer avroTransformer = new ToCloudEventTransformer(null, ToCloudEventTransformer.ConversionType.AVRO, properties);

		String payload = "test avro properties";
		Message<String> message = MessageBuilder.withPayload(payload).build();

		Object result = avroTransformer.doTransform(message);

		assertThat(result).isNotNull();
		Message<?> resultMessage = (Message<?>) result;
		assertThat(resultMessage.getPayload()).isInstanceOf(byte[].class);
		assertThat(resultMessage.getHeaders().get("content-type")).isEqualTo("application/avro");
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
	void testCloudEventPropertiesWithExtensions() {
		CloudEventProperties properties = new CloudEventProperties();
		properties.setId("extension-event");
		properties.setSource(URI.create("https://extension.example.com"));
		properties.setType("type.event");

		String extensionPatterns = "x-trace-id,!x-span-id";
		ToCloudEventTransformer extTransformer = new ToCloudEventTransformer(extensionPatterns, ToCloudEventTransformer.ConversionType.JSON, properties);

		String payload = "test extensions with properties";
		Message<String> message = MessageBuilder.withPayload(payload)
			.setHeader("x-trace-id", "trace-999")
			.setHeader("x-span-id", "span-888")
			.setHeader("regular-header", "regular-value")
			.build();

		Object result = extTransformer.doTransform(message);

		assertThat(result).isNotNull();
		Message<?> resultMessage = (Message<?>) result;
		assertThat(resultMessage.getPayload()).isInstanceOf(String.class);

		String jsonPayload = (String) resultMessage.getPayload();
		assertThat(jsonPayload).contains("\"id\":\"extension-event\"");
		assertThat(jsonPayload).contains("\"source\":\"https://extension.example.com\"");
		assertThat(jsonPayload).contains("\"type\":\"type.event\"");
		assertThat(jsonPayload).contains("\"x-trace-id\":\"trace-999\"");
		assertThat(jsonPayload).doesNotContain("\"x-span-id\":\"span-888\"");

		MessageHeaders headers = resultMessage.getHeaders();
		assertThat(headers.containsKey("x-trace-id")).isFalse();
		assertThat(headers.containsKey("x-span-id")).isFalse();
		assertThat(headers.containsKey("regular-header")).isTrue();
	}

	@Test
	void testCustomCePrefixInHeaders() {
		CloudEventProperties properties = new CloudEventProperties();
		properties.setCePrefix("CUSTOM_");

		ToCloudEventTransformer customPrefixTransformer = new ToCloudEventTransformer(null, ToCloudEventTransformer.ConversionType.DEFAULT, properties);

		String payload = "test custom prefix";
		Message<String> message = MessageBuilder.withPayload(payload)
			.setHeader("test-header", "test-value")
			.build();

		Object result = customPrefixTransformer.doTransform(message);

		assertThat(result).isNotNull();
		assertThat(result).isInstanceOf(Message.class);

		Message<?> resultMessage = (Message<?>) result;
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
		ToCloudEventTransformer customExtTransformer = new ToCloudEventTransformer(extensionPatterns, ToCloudEventTransformer.ConversionType.DEFAULT, properties);

		String payload = "test custom prefix with extensions";
		Message<String> message = MessageBuilder.withPayload(payload)
			.setHeader("trace-id", "trace-456")
			.setHeader("span-id", "span-789")
			.setHeader("regular-header", "regular-value")
			.build();

		Object result = customExtTransformer.doTransform(message);

		assertThat(result).isNotNull();
		Message<?> resultMessage = (Message<?>) result;
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
	void testCustomPrefixWithJsonConversion() {
		CloudEventProperties properties = new CloudEventProperties();
		properties.setId("json-prefix-id");
		properties.setSource(URI.create("https://json-prefix.example.com"));
		properties.setType("com.example.json.prefix");
		properties.setCePrefix("JSON_CE_");

		ToCloudEventTransformer jsonPrefixTransformer = new ToCloudEventTransformer(null, ToCloudEventTransformer.ConversionType.JSON, properties);

		String payload = "test json with custom prefix";
		Message<String> message = MessageBuilder.withPayload(payload)
			.setHeader("correlation-id", "json-corr-123")
			.build();

		Object result = jsonPrefixTransformer.doTransform(message);

		assertThat(result).isNotNull();
		Message<?> resultMessage = (Message<?>) result;
		MessageHeaders headers = resultMessage.getHeaders();


		assertThat(headers.get("content-type")).isEqualTo("application/json");
		assertThat(headers.get("correlation-id")).isEqualTo("json-corr-123");

		String jsonPayload = (String) resultMessage.getPayload();
		assertThat(jsonPayload).contains("\"id\":\"json-prefix-id\"");
		assertThat(jsonPayload).contains("\"source\":\"https://json-prefix.example.com\"");
		assertThat(jsonPayload).contains("\"type\":\"com.example.json.prefix\"");
	}

	@Test
	void testCustomPrefixWithAvroConversion() {
		CloudEventProperties properties = new CloudEventProperties();
		properties.setId("avro-prefix-id");
		properties.setSource(URI.create("https://avro-prefix.example.com"));
		properties.setType("com.example.avro.prefix");
		properties.setCePrefix("AVRO_CE_");

		ToCloudEventTransformer avroPrefixTransformer = new ToCloudEventTransformer(null, ToCloudEventTransformer.ConversionType.AVRO, properties);

		String payload = "test avro with custom prefix";
		Message<String> message = MessageBuilder.withPayload(payload)
			.setHeader("message-id", "avro-msg-123")
			.build();

		Object result = avroPrefixTransformer.doTransform(message);

		assertThat(result).isNotNull();
		Message<?> resultMessage = (Message<?>) result;
		MessageHeaders headers = resultMessage.getHeaders();

		assertThat(headers.get("content-type")).isEqualTo("application/avro");
		assertThat(headers.get("message-id")).isEqualTo("avro-msg-123");
		assertThat(resultMessage.getPayload()).isInstanceOf(byte[].class);
	}

	@Test
	void testCustomPrefixWithXmlConversion() {
		CloudEventProperties properties = new CloudEventProperties();
		properties.setId("xml-prefix-id");
		properties.setSource(URI.create("https://xml-prefix.example.com"));
		properties.setType("com.example.xml.prefix");
		properties.setCePrefix("XML_CE_");

		ToCloudEventTransformer xmlPrefixTransformer = new ToCloudEventTransformer(null, ToCloudEventTransformer.ConversionType.XML, properties);

		String payload = "test xml with custom prefix";
		Message<String> message = MessageBuilder.withPayload(payload)
			.setHeader("request-id", "xml-req-123")
			.build();

		Object result = xmlPrefixTransformer.doTransform(message);

		assertThat(result).isNotNull();
		Message<?> resultMessage = (Message<?>) result;
		MessageHeaders headers = resultMessage.getHeaders();


		assertThat(headers.get("content-type")).isEqualTo("application/xml");
		assertThat(headers.get("request-id")).isEqualTo("xml-req-123");

		String xmlPayload = (String) resultMessage.getPayload();
		assertThat(xmlPayload).contains("<?xml");
		assertThat(xmlPayload).contains("<id>xml-prefix-id</id>");
		assertThat(xmlPayload).contains("<source>https://xml-prefix.example.com</source>");
		assertThat(xmlPayload).contains("<type>com.example.xml.prefix</type>");
	}

	@Test
	void testCustomPrefixWithXmlConversionWithExtensions() {
		CloudEventProperties properties = new CloudEventProperties();
		properties.setId("xml-prefix-id");
		properties.setSource(URI.create("https://xml-prefix.example.com"));
		properties.setType("com.example.xml.prefix");
		properties.setCePrefix("XML_CE_");

		ToCloudEventTransformer xmlPrefixTransformer = new ToCloudEventTransformer("request-id", ToCloudEventTransformer.ConversionType.XML, properties);

		String payload = "test xml with custom prefix";
		Message<String> message = MessageBuilder.withPayload(payload)
				.setHeader("request-id", "xml-req-123")
				.build();

		Object result = xmlPrefixTransformer.doTransform(message);

		assertThat(result).isNotNull();
		Message<?> resultMessage = (Message<?>) result;
		MessageHeaders headers = resultMessage.getHeaders();


		assertThat(headers.get("content-type")).isEqualTo("application/xml");
		assertThat(headers.get("request-id")).isNull();

		String xmlPayload = (String) resultMessage.getPayload();
		assertThat(xmlPayload).contains("<?xml");
		assertThat(xmlPayload).contains("<id>xml-prefix-id</id>");
		assertThat(xmlPayload).contains("<source>https://xml-prefix.example.com</source>");
		assertThat(xmlPayload).contains("<type>com.example.xml.prefix</type>");
		assertThat(xmlPayload).contains("<request-id xsi:type=\"ce:string\">xml-req-123</request-id>");
	}

	@Test
	void testEmptyStringCePrefixBehavior() {
		CloudEventProperties properties = new CloudEventProperties();
		properties.setCePrefix("");

		ToCloudEventTransformer emptyPrefixTransformer = new ToCloudEventTransformer(null, ToCloudEventTransformer.ConversionType.DEFAULT, properties);

		String payload = "test empty prefix";
		Message<String> message = MessageBuilder.withPayload(payload).build();

		Object result = emptyPrefixTransformer.doTransform(message);

		assertThat(result).isNotNull();
		Message<?> resultMessage = (Message<?>) result;
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
}
