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

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import io.cloudevents.CloudEvent;
import io.cloudevents.jackson.JsonCloudEventData;
import io.cloudevents.jackson.JsonFormat;
import io.cloudevents.xml.XMLFormat;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.serializer.DefaultSerializer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.transformer.MessageTransformationException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/**
 * Test {@link ToCloudEventTransformer} transformer.
 *
 * @author Glenn Renfro
 *
 * @since 7.1
 */
@DirtiesContext
@SpringJUnitConfig
class ToCloudEventTransformerTests {

	private static final String TRACE_HEADER = "traceId";

	private static final String SPAN_HEADER = "spanId";

	private static final String USER_HEADER = "userId";

	private static final byte[] TEXT_PLAIN_PAYLOAD = "\"test message\"".getBytes(StandardCharsets.UTF_8);

	private static final byte[] XML_PAYLOAD = ("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><payload>" +
			"<message>testmessage</message></payload>").getBytes(StandardCharsets.UTF_8);

	private static final byte[] JSON_PAYLOAD = ("{\"message\":\"Hello, World!\"}").getBytes(StandardCharsets.UTF_8);

	@Autowired
	private ToCloudEventTransformer transformerWithNoExtensions;

	@Autowired
	private ToCloudEventTransformer transformerWithExtensions;

	@Autowired
	private ToCloudEventTransformer transformerWithNoExtensionsNoFormat;

	@Autowired
	private ToCloudEventTransformer transformerWithNoExtensionsNoFormatEnabled;

	@Autowired
	private ToCloudEventTransformer transformerWithExtensionsNoFormat;

	@Autowired
	private ToCloudEventTransformer transformerWithExtensionsNoFormatWithPrefix;

	@Autowired
	private ToCloudEventTransformer transformerWithInvalidIDExpression;

	private final JsonFormat jsonFormat = new JsonFormat();

	private final XMLFormat xmlFormat = new XMLFormat();

	@Test
	void transformWithPayloadBasedOnJsonFormatContentType() {
		Message<byte[]> message =
				getTransformerNoExtensions(JSON_PAYLOAD, this.transformerWithNoExtensions, JsonFormat.CONTENT_TYPE);
		CloudEvent cloudEvent = this.jsonFormat.deserialize(message.getPayload());
		assertThat(((JsonCloudEventData) cloudEvent.getData()).getNode().toString().getBytes(StandardCharsets.UTF_8)).
				isEqualTo(JSON_PAYLOAD);
		assertThat(cloudEvent.getSource().toString()).isEqualTo("/spring/null.transformerWithNoExtensions");
		assertThat(cloudEvent.getDataSchema()).isNull();
		assertThat(cloudEvent.getDataContentType()).isEqualTo(JsonFormat.CONTENT_TYPE);
	}

	@Test
	void transformWithPayloadBasedOnApplicationJsonType() {
		Message<byte[]> message =
				getTransformerNoExtensions(JSON_PAYLOAD, this.transformerWithNoExtensions, "application/json");
		assertThat(message.getPayload()).isEqualTo(JSON_PAYLOAD);
		verifyApplicationTypes(message);
	}

	@Test
	void transformWithPayloadBasedOnApplicationXMLType() {
		Message<byte[]> message = getTransformerNoExtensions(XML_PAYLOAD,
				this.transformerWithNoExtensions, "application/xml");
		assertThat(message.getPayload()).isEqualTo(XML_PAYLOAD);
		verifyApplicationTypes(message);
	}

	@Test
	void transformWithPayloadBasedOnContentXMLFormatType() {
		Message<byte[]> message = getTransformerNoExtensions(XML_PAYLOAD,
				this.transformerWithNoExtensions, XMLFormat.XML_CONTENT_TYPE);
		CloudEvent cloudEvent = this.xmlFormat.deserialize(message.getPayload());
		assertThat(cloudEvent.getData().toBytes()).isEqualTo(XML_PAYLOAD);
		assertThat(cloudEvent.getSource().toString()).isEqualTo("/spring/null.transformerWithNoExtensions");
		assertThat(cloudEvent.getDataSchema()).isNull();
		assertThat(cloudEvent.getDataContentType()).isEqualTo(XMLFormat.XML_CONTENT_TYPE);
	}

	@Test
	void convertMessageNoExtensions() {
		Message<byte[]> message = MessageBuilder.withPayload(TEXT_PLAIN_PAYLOAD)
				.setHeader(MessageHeaders.CONTENT_TYPE, "text/plain")
				.setHeader(TRACE_HEADER, "test-value")
				.setHeader(SPAN_HEADER, "other-value")
				.build();
		Message<byte[]> result =  (Message<byte[]>) this.transformerWithNoExtensionsNoFormat.doTransform(message);
		assertThat(result.getPayload()).isEqualTo(TEXT_PLAIN_PAYLOAD);
		assertThat(result.getHeaders()).containsKeys(TRACE_HEADER, SPAN_HEADER);
		assertThat(result.getHeaders()).doesNotContainKeys("ce-" + TRACE_HEADER, "ce-" + SPAN_HEADER);
		assertThat(result.getHeaders()).containsEntry(MessageHeaders.CONTENT_TYPE, "application/cloudevents");

	}

	@Test
	void convertMessageWithExtensions() {
		Message<byte[]> message = MessageBuilder.withPayload(TEXT_PLAIN_PAYLOAD)
				.setHeader(MessageHeaders.CONTENT_TYPE, "text/plain")
				.setHeader(TRACE_HEADER, "test-value")
				.setHeader(SPAN_HEADER, "other-value")
				.build();
		Message<byte[]> result = (Message<byte[]>) transformerWithExtensionsNoFormat.doTransform(message);
		assertThat(result.getHeaders()).containsKeys(TRACE_HEADER, SPAN_HEADER).
				containsKeys("ce-" + TRACE_HEADER, "ce-" + SPAN_HEADER);
		assertThat(result.getPayload()).isEqualTo(TEXT_PLAIN_PAYLOAD);
	}

	@Test
	void convertMessageWithExtensionsNewPrefix() {
		Message<byte[]> message = MessageBuilder.withPayload(TEXT_PLAIN_PAYLOAD)
				.setHeader(MessageHeaders.CONTENT_TYPE, "text/plain")
				.setHeader(TRACE_HEADER, "test-value")
				.setHeader(SPAN_HEADER, "other-value")
				.build();
		Message<byte[]> result = (Message<byte[]>) this.transformerWithExtensionsNoFormatWithPrefix.doTransform(message);
		assertThat(result.getHeaders()).containsKeys(TRACE_HEADER, SPAN_HEADER, "CLOUDEVENTS-" + TRACE_HEADER,
				"CLOUDEVENTS-" + SPAN_HEADER, "CLOUDEVENTS-id", "CLOUDEVENTS-specversion",
				"CLOUDEVENTS-datacontenttype");
		assertThat(result.getPayload()).isEqualTo(TEXT_PLAIN_PAYLOAD);
	}

	@Test
	@SuppressWarnings("unchecked")
	void doTransformWithObjectPayload() throws Exception {
		TestRecord testRecord = new TestRecord("sample data");
		byte[] payload = convertPayloadToBytes(testRecord);
		Message<byte[]> message = MessageBuilder.withPayload(payload).setHeader("test_id", "test-id")
				.setHeader(MessageHeaders.CONTENT_TYPE, JsonFormat.CONTENT_TYPE)
				.build();
		Object result = this.transformerWithNoExtensions.doTransform(message);

		assertThat(result).isInstanceOf(Message.class);

		Message<byte[]> resultMessage = (Message<byte[]>) result;
		assertThat(new String(resultMessage.getPayload())).endsWith(new String(payload) + "}");
	}

	@Test
	void noContentType() {
		Message<byte[]> message = MessageBuilder.withPayload(TEXT_PLAIN_PAYLOAD).build();
		Message<?> result = this.transformerWithNoExtensions.transform(message);
		assertThat(result.getHeaders()).containsEntry("ce-datacontenttype", "application/octet-stream");
		assertThat(message.getPayload()).isEqualTo(TEXT_PLAIN_PAYLOAD);

	}

	@Test
	void noContentTypeNoFormatEnabled() {
		Message<byte[]> message = MessageBuilder.withPayload(TEXT_PLAIN_PAYLOAD).build();
		assertThatThrownBy(() -> this.transformerWithNoExtensionsNoFormatEnabled.transform(message))
				.isInstanceOf(MessageTransformationException.class)
				.hasMessageContaining("No EventFormat found for 'application/octet-stream'");
	}

	@Test
	@SuppressWarnings({"unchecked"})
	void multipleExtensionMappingsWithJsonFormatType() {
		Message<byte[]> message = createBaseMessage(JSON_PAYLOAD, JsonFormat.CONTENT_TYPE)
				.setHeader("correlation-id", "corr-999")
				.setHeader(TRACE_HEADER, "trace-123")
				.build();

		Object result = this.transformerWithExtensions.doTransform(message);

		Message<byte[]> resultMessage = (Message<byte[]>) result;

		assertThat(resultMessage.getHeaders()).containsEntry("correlation-id", "corr-999");
		assertThat(new String(resultMessage.getPayload())).contains("\"traceId\":\"trace-123\"");
	}

	@Test
	@SuppressWarnings({"unchecked"})
	void multipleExtensionMappingsWithApplicationJsonType() {
		Message<byte[]> message = createBaseMessage(JSON_PAYLOAD, "application/json")
				.setHeader("correlation-id", "corr-999")
				.setHeader(TRACE_HEADER, "trace-123")
				.build();

		Object result = this.transformerWithExtensions.doTransform(message);

		Message<byte[]> resultMessage = (Message<byte[]>) result;

		assertThat(resultMessage.getHeaders()).containsEntry("correlation-id", "corr-999").
				containsEntry("ce-traceId", "trace-123");
	}

	@Test
	void emptyStringPayloadHandling() {
		Message<byte[]> message = createBaseMessage("".getBytes(), "text/plain").build();
		Object result = this.transformerWithNoExtensions.doTransform(message);

		assertThat(result).isInstanceOf(Message.class);
	}

	@Test
	void failWhenNoIdHeaderAndNoDefault() {
		Message<byte[]> message = createBaseMessage(TEXT_PLAIN_PAYLOAD, JsonFormat.CONTENT_TYPE).build();
		assertThatThrownBy(() -> this.transformerWithInvalidIDExpression.transform(message))
				.isInstanceOf(MessageTransformationException.class)
				.hasMessageContaining("failed to transform message");
	}

	private static void verifyApplicationTypes(Message<byte[]> message) {
		assertThat(message.getHeaders())
				.containsEntry("ce-source", "/spring/null.transformerWithNoExtensions")
				.containsEntry("ce-type", "spring.message")
				.containsEntry(MessageHeaders.CONTENT_TYPE, "application/cloudevents");
	}

	private static Message<byte[]> getTransformerNoExtensions(byte[] payload,
			ToCloudEventTransformer transformer, String contentType) {
		Message<byte[]> message = createBaseMessage(payload, contentType)
				.setHeader("customheader", "test-value")
				.setHeader("otherheader", "other-value")
				.build();
		Message<byte[]> result = (Message<byte[]>) transformer.doTransform(message);
		return result;
	}

	private static byte[] convertPayloadToBytes(TestRecord testRecord) throws Exception {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(byteArrayOutputStream);
		DefaultSerializer defaultSerializer = new DefaultSerializer();
		defaultSerializer.serialize(testRecord, out);
		return  byteArrayOutputStream.toByteArray();
	}

	private static MessageBuilder<byte[]> createBaseMessage(byte[] payload, String contentType) {
		return MessageBuilder.withPayload(payload)
				.setHeader(MessageHeaders.CONTENT_TYPE, contentType);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	public static class ContextConfiguration {

		private static final String[] TEST_PATTERNS = {"trace*", SPAN_HEADER, USER_HEADER};

		private static final ExpressionParser parser = new SpelExpressionParser();

		@Bean
		public ToCloudEventTransformer transformerWithNoExtensions() {
			return new ToCloudEventTransformer();
		}

		@Bean
		public ToCloudEventTransformer transformerWithExtensions() {
			return new ToCloudEventTransformer(TEST_PATTERNS);
		}

		@Bean
		public ToCloudEventTransformer transformerWithNoExtensionsNoFormat() {
			ToCloudEventTransformer toCloudEventsTransformer = new ToCloudEventTransformer();
			return toCloudEventsTransformer;
		}

		@Bean
		public ToCloudEventTransformer transformerWithExtensionsNoFormat() {
			ToCloudEventTransformer toCloudEventsTransformer =  new ToCloudEventTransformer(TEST_PATTERNS);
			return toCloudEventsTransformer;
		}

		@Bean
		public ToCloudEventTransformer transformerWithExtensionsNoFormatWithPrefix() {
			ToCloudEventTransformer toCloudEventsTransformer =  new ToCloudEventTransformer(TEST_PATTERNS);
			toCloudEventsTransformer.setCloudEventPrefix("CLOUDEVENTS-");
			return toCloudEventsTransformer;
		}

		@Bean
		public ToCloudEventTransformer transformerWithInvalidIDExpression() {
			ToCloudEventTransformer transformer = new ToCloudEventTransformer();
			transformer.setEventIdExpression(parser.parseExpression("null"));
			return transformer;
		}

		@Bean
		public ToCloudEventTransformer transformerWithNoExtensionsNoFormatEnabled() {
			ToCloudEventTransformer toCloudEventsTransformer = new ToCloudEventTransformer();
			toCloudEventsTransformer.setFailOnNoFormat(true);
			return toCloudEventsTransformer;
		}

	}

	private record TestRecord(String sampleValue) implements Serializable { }

}
