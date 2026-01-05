/*
 * Copyright 2026-present the original author or authors.
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

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.cloudevents.CloudEvent;
import io.cloudevents.jackson.JsonCloudEventData;
import io.cloudevents.jackson.JsonFormat;
import io.cloudevents.xml.XMLFormat;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.common.LiteralExpression;
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

	private static final String TRACE_HEADER = "traceid";

	private static final String SPAN_HEADER = "spanid";

	private static final String USER_HEADER = "userid";

	private static final byte[] TEXT_PLAIN_PAYLOAD = "\"test message\"".getBytes(StandardCharsets.UTF_8);

	private static final byte[] XML_PAYLOAD = ("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><payload>" +
			"<message>testmessage</message></payload>").getBytes(StandardCharsets.UTF_8);

	private static final byte[] JSON_PAYLOAD = ("{\"message\":\"Hello, World!\"}").getBytes(StandardCharsets.UTF_8);

	@Autowired
	private ToCloudEventTransformer xmlTransformerWithNoExtensions;

	@Autowired
	private ToCloudEventTransformer jsonTransformerWithNoExtensions;

	@Autowired
	private ToCloudEventTransformer jsonTransformerWithExtensions;

	@Autowired
	private ToCloudEventTransformer transformerWithNoExtensionsNoFormat;

	@Autowired
	private ToCloudEventTransformer transformerWithNoExtensionsNoFormatEnabled;

	@Autowired
	private ToCloudEventTransformer transformerWithExtensionsNoFormat;

	@Autowired
	private ToCloudEventTransformer transformerWithExtensionsNoFormatWithPrefix;

	@Autowired
	private ToCloudEventTransformer xmlTransformerWithInvalidIDExpression;

	@Autowired
	private ToCloudEventTransformer transformerWithNoExtensionsNoFormatEnabledWithProviderExpression;

	private final JsonFormat jsonFormat = new JsonFormat();

	private final XMLFormat xmlFormat = new XMLFormat();

	@Test
	void transformWithPayloadBasedOnJsonFormatContentTypeWithProviderExpression() {
		Message<byte[]> originalMessage = createBaseMessage(JSON_PAYLOAD, "text/plain")
				.setHeader("customheader", "test-value")
				.setHeader("otherheader", "other-value")
				.build();
		ToCloudEventTransformer transformer = this.transformerWithNoExtensionsNoFormatEnabledWithProviderExpression;

		Message<byte[]> message = (Message<byte[]>) transformer.doTransform(originalMessage);

		CloudEvent cloudEvent = this.jsonFormat.deserialize(message.getPayload());
		verifyCloudEvent(cloudEvent, "transformerWithNoExtensionsNoFormatEnabledWithProviderExpression",
				"text/plain");
		assertThat(cloudEvent.getData().toBytes()).isEqualTo(JSON_PAYLOAD);
	}

	@Test
	void transformWithPayloadBasedOnJsonFormatContentType() {
		Message<byte[]> message =
				getTransformerNoExtensions(JSON_PAYLOAD, this.jsonTransformerWithNoExtensions, JsonFormat.CONTENT_TYPE);
		CloudEvent cloudEvent = this.jsonFormat.deserialize(message.getPayload());
		verifyCloudEvent(cloudEvent, "jsonTransformerWithNoExtensions", JsonFormat.CONTENT_TYPE);
		assertThat(((JsonCloudEventData) cloudEvent.getData()).getNode().toString().getBytes(StandardCharsets.UTF_8)).
				isEqualTo(JSON_PAYLOAD);
	}

	@Test
	void transformWithPayloadBasedOnApplicationJsonType() {
		Message<byte[]> message =
				getTransformerNoExtensions(JSON_PAYLOAD, this.jsonTransformerWithNoExtensions,
						"application/json");
		CloudEvent cloudEvent = this.jsonFormat.deserialize(message.getPayload());
		verifyCloudEvent(cloudEvent, "jsonTransformerWithNoExtensions", "application/json");
		assertThat(new String(cloudEvent.getData().toBytes())).contains("{\"message\":\"Hello, World!\"}");
	}

	@Test
	void transformWithPayloadBasedOnApplicationXMLType() throws IOException {
		Message<byte[]> message = getTransformerNoExtensions(XML_PAYLOAD,
				this.xmlTransformerWithNoExtensions, "application/xml");
		CloudEvent cloudEvent = xmlFormat.deserialize(message.getPayload());
		verifyCloudEvent(cloudEvent, "xmlTransformerWithNoExtensions", "application/xml");
		assertThat(new String(cloudEvent.getData().toBytes())).contains("<message>testmessage</message></payload>");
	}

	@Test
	void transformWithPayloadBasedOnContentXMLFormatType() {
		Message<byte[]> message = getTransformerNoExtensions(XML_PAYLOAD,
				this.xmlTransformerWithNoExtensions, XMLFormat.XML_CONTENT_TYPE);
		CloudEvent cloudEvent = this.xmlFormat.deserialize(message.getPayload());
		verifyCloudEvent(cloudEvent, "xmlTransformerWithNoExtensions", XMLFormat.XML_CONTENT_TYPE);
		assertThat(cloudEvent.getData().toBytes()).isEqualTo(XML_PAYLOAD);
	}

	@Test
	void convertMessageNoExtensions() {
		Message<byte[]> message = MessageBuilder.withPayload(TEXT_PLAIN_PAYLOAD)
				.setHeader(MessageHeaders.CONTENT_TYPE, "text/plain")
				.setHeader(TRACE_HEADER, "test-value")
				.setHeader(SPAN_HEADER, "other-value")
				.build();
		Message<byte[]> result =  (Message<byte[]>) this.transformerWithNoExtensionsNoFormat.doTransform(message);
		assertThat(result.getHeaders()).containsKeys(TRACE_HEADER, SPAN_HEADER);
		assertThat(result.getHeaders()).doesNotContainKeys("ce-" + TRACE_HEADER, "ce-" + SPAN_HEADER);
		assertThat(result.getHeaders()).containsEntry(MessageHeaders.CONTENT_TYPE, "text/plain");
		assertThat(result.getPayload()).isEqualTo(TEXT_PLAIN_PAYLOAD);
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

		Message<byte[]> resultMessage = (Message<byte[]>) this.jsonTransformerWithNoExtensions.doTransform(message);
		CloudEvent cloudEvent = this.jsonFormat.deserialize(resultMessage.getPayload());
		verifyCloudEvent(cloudEvent, "jsonTransformerWithNoExtensions", JsonFormat.CONTENT_TYPE);
		assertThat(new String(resultMessage.getPayload())).contains(new String(payload));
	}

	@Test
	void noContentType() {
		Message<byte[]> message = MessageBuilder.withPayload(TEXT_PLAIN_PAYLOAD).build();
		Message<?> result = this.transformerWithNoExtensionsNoFormat.transform(message);
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

		Message<byte[]> resultMessage = (Message<byte[]>) this.jsonTransformerWithExtensions.doTransform(message);

		CloudEvent cloudEvent = this.jsonFormat.deserialize(resultMessage.getPayload());

		assertThat(resultMessage.getHeaders()).containsEntry("correlation-id", "corr-999");
		verifyCloudEvent(cloudEvent, "jsonTransformerWithExtensions", JsonFormat.CONTENT_TYPE);
		assertThat(new String(resultMessage.getPayload())).contains("\"traceid\":\"trace-123\"");
	}

	@Test
	@SuppressWarnings({"unchecked"})
	void multipleExtensionMappingsWithApplicationJsonType() {
		Message<byte[]> message = createBaseMessage(JSON_PAYLOAD, "application/json")
				.setHeader("correlation-id", "corr-999")
				.setHeader(TRACE_HEADER, "trace-123")
				.build();

		Object result = this.jsonTransformerWithExtensions.doTransform(message);

		Message<byte[]> resultMessage = (Message<byte[]>) result;
		CloudEvent cloudEvent = this.jsonFormat.deserialize(resultMessage.getPayload());
		assertThat(resultMessage.getHeaders()).containsEntry("correlation-id", "corr-999");
		assertThat(cloudEvent.getExtensionNames()).containsExactly("traceid").
				doesNotContain("correlation-id");
		verifyCloudEvent(cloudEvent, "jsonTransformerWithExtensions", "application/json");
	}

	@Test
	void emptyStringPayloadHandling() {
		Message<byte[]> message = createBaseMessage("".getBytes(), "text/plain").build();
		Message<byte[]> resultMessage = (Message<byte[]>) this.jsonTransformerWithNoExtensions.doTransform(message);
		CloudEvent cloudEvent = this.jsonFormat.deserialize(resultMessage.getPayload());
		assertThat(cloudEvent.getData().toBytes()).isEmpty();
		verifyCloudEvent(cloudEvent, "jsonTransformerWithNoExtensions", "text/plain");
	}

	@Test
	void failWhenNoIdHeaderAndNoDefault() {
		Message<byte[]> message = createBaseMessage(TEXT_PLAIN_PAYLOAD, JsonFormat.CONTENT_TYPE).build();
		assertThatThrownBy(() -> this.xmlTransformerWithInvalidIDExpression.transform(message))
				.isInstanceOf(MessageTransformationException.class)
				.hasMessageContaining("failed to transform message");
	}

	private static void verifyCloudEvent(CloudEvent cloudEvent, String beanName, String type) {
		assertThat(cloudEvent.getDataContentType()).isEqualTo(type);
		assertThat(cloudEvent.getSource().toString()).isEqualTo("/spring/null." + beanName);
		assertThat(cloudEvent.getType()).isEqualTo("spring.message");
		assertThat(cloudEvent.getDataSchema()).isNull();
	}

	private static Message<byte[]> getTransformerNoExtensions(byte[] payload,
			ToCloudEventTransformer transformer, String contentType) {
		Message<byte[]> message = createBaseMessage(payload, contentType)
				.setHeader("customheader", "test-value")
				.setHeader("otherheader", "other-value")
				.build();
		return (Message<byte[]>) transformer.doTransform(message);
	}

	private static byte[] convertPayloadToBytes(TestRecord testRecord) throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		ObjectWriter writer = objectMapper.writer();
		return writer.writeValueAsBytes(testRecord);
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
		public ToCloudEventTransformer xmlTransformerWithNoExtensions() {
			return new ToCloudEventTransformer(new XMLFormat());
		}

		@Bean
		public ToCloudEventTransformer jsonTransformerWithNoExtensions() {
			return new ToCloudEventTransformer(new JsonFormat());
		}

		@Bean
		public ToCloudEventTransformer jsonTransformerWithExtensions() {
			return new ToCloudEventTransformer(new JsonFormat(), TEST_PATTERNS);
		}

		@Bean
		public ToCloudEventTransformer transformerWithNoExtensionsNoFormat() {
			return new ToCloudEventTransformer();
		}

		@Bean
		public ToCloudEventTransformer transformerWithExtensionsNoFormat() {
			return new ToCloudEventTransformer(null, TEST_PATTERNS);
		}

		@Bean
		public ToCloudEventTransformer transformerWithExtensionsNoFormatWithPrefix() {
			ToCloudEventTransformer toCloudEventsTransformer = new ToCloudEventTransformer(null, TEST_PATTERNS);
			toCloudEventsTransformer.setCloudEventPrefix("CLOUDEVENTS-");
			return toCloudEventsTransformer;
		}

		@Bean
		public ToCloudEventTransformer xmlTransformerWithInvalidIDExpression() {
			ToCloudEventTransformer transformer = new ToCloudEventTransformer(new XMLFormat());
			transformer.setEventIdExpression(parser.parseExpression("null"));
			return transformer;
		}

		@Bean
		public ToCloudEventTransformer transformerWithNoExtensionsNoFormatEnabled() {
			ToCloudEventTransformer toCloudEventsTransformer = new ToCloudEventTransformer();
			toCloudEventsTransformer.setFailOnNoFormat(true);
			return toCloudEventsTransformer;
		}

		@Bean
		public ToCloudEventTransformer transformerWithNoExtensionsNoFormatEnabledWithProviderExpression() {
			ToCloudEventTransformer toCloudEventsTransformer = new ToCloudEventTransformer();
			toCloudEventsTransformer.setEventFormatContentTypeExpression(new LiteralExpression(JsonFormat.CONTENT_TYPE));
			toCloudEventsTransformer.setFailOnNoFormat(true);
			return toCloudEventsTransformer;
		}
	}

	private record TestRecord(String sampleValue) implements Serializable { }

}
