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

import java.io.Serializable;

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
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.cloudevents.CloudEventHeaders;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.transformer.MessageTransformationException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

/**
 * Test {@link ToCloudEventTransformer} transformer.
 *
 * @author Glenn Renfro
 * @author Artem Bilan
 *
 * @since 7.1
 */
@DirtiesContext
@SpringJUnitConfig
class ToCloudEventTransformerTests {

	static final String TRACE_HEADER = "traceid";

	static final String SPAN_HEADER = "spanid";

	static final String USER_HEADER = "userid";

	static final byte[] TEXT_PLAIN_PAYLOAD = "\"test message\"".getBytes();

	static final byte[] XML_PAYLOAD =
			("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" +
					"<payload>" +
					"<message>testmessage</message>" +
					"</payload>")
					.getBytes();

	static final byte[] JSON_PAYLOAD = "{\"message\":\"Hello, World!\"}".getBytes();

	final JsonFormat jsonFormat = new JsonFormat();

	final XMLFormat xmlFormat = new XMLFormat();

	@Autowired
	ToCloudEventTransformer xmlTransformerWithNoExtensions;

	@Autowired
	ToCloudEventTransformer jsonTransformerWithNoExtensions;

	@Autowired
	ToCloudEventTransformer jsonTransformerWithExtensions;

	@Autowired
	ToCloudEventTransformer transformerWithNoExtensionsNoFormat;

	@Autowired
	ToCloudEventTransformer invalidEventFormatContentTypeExpression;

	@Autowired
	ToCloudEventTransformer transformerWithExtensionsNoFormat;

	@Autowired
	ToCloudEventTransformer transformerWithExtensionsNoFormatWithPrefix;

	@Autowired
	ToCloudEventTransformer xmlTransformerWithInvalidIDExpression;

	@Autowired
	ToCloudEventTransformer transformerWithNoExtensionsNoFormatEnabledWithProviderExpression;

	@SuppressWarnings({"unchecked"})
	@Test
	void transformWithPayloadBasedOnJsonFormatContentTypeWithProviderExpression() {
		Message<byte[]> originalMessage =
				createBaseMessage(JSON_PAYLOAD, "text/plain")
						.setHeader("customheader", "test-value")
						.setHeader("otherheader", "other-value")
						.build();
		ToCloudEventTransformer transformer = this.transformerWithNoExtensionsNoFormatEnabledWithProviderExpression;

		Message<byte[]> message = (Message<byte[]>) transformer.transform(originalMessage);

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
		assertThat(cloudEvent.getData())
				.asInstanceOf(type(JsonCloudEventData.class))
				.extracting(JsonCloudEventData::toBytes)
				.isEqualTo(JSON_PAYLOAD);
	}

	@Test
	void transformWithPayloadBasedOnApplicationJsonType() {
		Message<byte[]> message =
				getTransformerNoExtensions(JSON_PAYLOAD, this.jsonTransformerWithNoExtensions, "application/json");
		CloudEvent cloudEvent = this.jsonFormat.deserialize(message.getPayload());
		verifyCloudEvent(cloudEvent, "jsonTransformerWithNoExtensions", "application/json");
		assertThat(new String(cloudEvent.getData().toBytes())).contains("{\"message\":\"Hello, World!\"}");
	}

	@Test
	void transformWithPayloadBasedOnApplicationXMLType() {
		Message<byte[]> message =
				getTransformerNoExtensions(XML_PAYLOAD, this.xmlTransformerWithNoExtensions, "application/xml");
		CloudEvent cloudEvent = xmlFormat.deserialize(message.getPayload());
		verifyCloudEvent(cloudEvent, "xmlTransformerWithNoExtensions", "application/xml");
		assertThat(new String(cloudEvent.getData().toBytes())).contains("<message>testmessage</message></payload>");
	}

	@Test
	void transformWithPayloadBasedOnContentXMLFormatType() {
		Message<byte[]> message =
				getTransformerNoExtensions(XML_PAYLOAD, this.xmlTransformerWithNoExtensions,
						XMLFormat.XML_CONTENT_TYPE);
		CloudEvent cloudEvent = this.xmlFormat.deserialize(message.getPayload());
		verifyCloudEvent(cloudEvent, "xmlTransformerWithNoExtensions", XMLFormat.XML_CONTENT_TYPE);
		assertThat(cloudEvent.getData().toBytes()).isEqualTo(XML_PAYLOAD);
	}

	@SuppressWarnings({"unchecked"})
	@Test
	void convertMessageNoExtensions() {
		Message<byte[]> message =
				MessageBuilder.withPayload(TEXT_PLAIN_PAYLOAD)
						.setHeader(MessageHeaders.CONTENT_TYPE, "text/plain")
						.setHeader(TRACE_HEADER, "test-value")
						.setHeader(SPAN_HEADER, "other-value")
						.build();
		Message<byte[]> result = (Message<byte[]>) this.transformerWithNoExtensionsNoFormat.doTransform(message);
		assertThat(result.getHeaders())
				.containsKeys(TRACE_HEADER, SPAN_HEADER)
				.doesNotContainKeys(CloudEventHeaders.PREFIX + TRACE_HEADER, CloudEventHeaders.PREFIX + SPAN_HEADER)
				.containsEntry(MessageHeaders.CONTENT_TYPE, "application/cloudevents");
		assertThat(result.getPayload()).isEqualTo(TEXT_PLAIN_PAYLOAD);
	}

	@SuppressWarnings({"unchecked"})
	@Test
	void convertMessageWithExtensions() {
		Message<byte[]> message =
				MessageBuilder.withPayload(TEXT_PLAIN_PAYLOAD)
						.setHeader(MessageHeaders.CONTENT_TYPE, "text/plain")
						.setHeader(TRACE_HEADER, "test-value")
						.setHeader(SPAN_HEADER, "other-value")
						.build();
		Message<byte[]> result = (Message<byte[]>) transformerWithExtensionsNoFormat.doTransform(message);
		assertThat(result.getHeaders())
				.containsKeys(TRACE_HEADER, SPAN_HEADER)
				.containsKeys(CloudEventHeaders.PREFIX + TRACE_HEADER, CloudEventHeaders.PREFIX + SPAN_HEADER);
		assertThat(result.getPayload()).isEqualTo(TEXT_PLAIN_PAYLOAD);
	}

	@SuppressWarnings({"unchecked"})
	@Test
	void convertMessageWithExtensionsNewPrefix() {
		Message<byte[]> message =
				MessageBuilder.withPayload(TEXT_PLAIN_PAYLOAD)
						.setHeader(MessageHeaders.CONTENT_TYPE, "text/plain")
						.setHeader(TRACE_HEADER, "test-value")
						.setHeader(SPAN_HEADER, "other-value")
						.build();
		Message<byte[]> result =
				(Message<byte[]>) this.transformerWithExtensionsNoFormatWithPrefix.doTransform(message);
		assertThat(result.getHeaders())
				.containsKeys(TRACE_HEADER, SPAN_HEADER, "CLOUDEVENTS-" + TRACE_HEADER,
						"CLOUDEVENTS-" + SPAN_HEADER, "CLOUDEVENTS-id", "CLOUDEVENTS-specversion",
						"CLOUDEVENTS-datacontenttype");
		assertThat(result.getPayload()).isEqualTo(TEXT_PLAIN_PAYLOAD);
	}

	@Test
	@SuppressWarnings("unchecked")
	void doTransformWithObjectPayload() throws Exception {
		TestRecord testRecord = new TestRecord("sample data");
		byte[] payload = new DefaultSerializer().serializeToByteArray(testRecord);
		Message<byte[]> message =
				MessageBuilder.withPayload(payload).setHeader("test_id", "test-id")
						.setHeader(MessageHeaders.CONTENT_TYPE, "application/x-java-serialized-object")
						.build();

		Message<byte[]> resultMessage = (Message<byte[]>) this.jsonTransformerWithNoExtensions.doTransform(message);
		CloudEvent cloudEvent = this.jsonFormat.deserialize(resultMessage.getPayload());
		verifyCloudEvent(cloudEvent, "jsonTransformerWithNoExtensions", "application/x-java-serialized-object");
		assertThat(cloudEvent.getData().toBytes()).contains(payload);
	}

	@Test
	void noContentType() {
		Message<byte[]> message = MessageBuilder.withPayload(TEXT_PLAIN_PAYLOAD).build();
		Message<?> result = this.transformerWithNoExtensionsNoFormat.transform(message);
		assertThat(result.getHeaders()).containsEntry(MessageHeaders.CONTENT_TYPE, "application/cloudevents");
		assertThat(message.getPayload()).isEqualTo(TEXT_PLAIN_PAYLOAD);
	}

	@Test
	void invalidEventFormatContentTypeExpression() {
		Message<byte[]> message = new GenericMessage<>(TEXT_PLAIN_PAYLOAD);
		assertThatThrownBy(() -> this.invalidEventFormatContentTypeExpression.transform(message))
				.isInstanceOf(MessageTransformationException.class)
				.hasMessageContaining("No EventFormat found for content type of 'invalid/type' provided by " +
						"the expression 'invalid/type'");
	}

	@Test
	@SuppressWarnings({"unchecked"})
	void multipleExtensionMappingsWithJsonFormatType() {
		Message<byte[]> message =
				createBaseMessage(JSON_PAYLOAD, JsonFormat.CONTENT_TYPE)
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
		Message<byte[]> message =
				createBaseMessage(JSON_PAYLOAD, "application/json")
						.setHeader("correlation-id", "corr-999")
						.setHeader(TRACE_HEADER, "trace-123")
						.build();

		Message<byte[]> resultMessage = (Message<byte[]>) this.jsonTransformerWithExtensions.doTransform(message);

		CloudEvent cloudEvent = this.jsonFormat.deserialize(resultMessage.getPayload());
		assertThat(resultMessage.getHeaders()).containsEntry("correlation-id", "corr-999");
		assertThat(cloudEvent.getExtensionNames())
				.containsExactly("traceid")
				.doesNotContain("correlation-id");
		verifyCloudEvent(cloudEvent, "jsonTransformerWithExtensions", "application/json");
	}

	@SuppressWarnings({"unchecked"})
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

	static void verifyCloudEvent(CloudEvent cloudEvent, String beanName, String type) {
		assertThat(cloudEvent.getDataContentType()).isEqualTo(type);
		assertThat(cloudEvent.getSource().toString()).isEqualTo("/spring/null." + beanName);
		assertThat(cloudEvent.getType()).isEqualTo("spring.message");
		assertThat(cloudEvent.getDataSchema()).isNull();
	}

	@SuppressWarnings({"unchecked"})
	static Message<byte[]> getTransformerNoExtensions(byte[] payload,
			ToCloudEventTransformer transformer, String contentType) {

		Message<byte[]> message = createBaseMessage(payload, contentType)
				.setHeader("customheader", "test-value")
				.setHeader("otherheader", "other-value")
				.build();
		return (Message<byte[]>) transformer.doTransform(message);
	}

	static MessageBuilder<byte[]> createBaseMessage(byte[] payload, String contentType) {
		return MessageBuilder.withPayload(payload)
				.setHeader(MessageHeaders.CONTENT_TYPE, contentType);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	static class ContextConfiguration {

		static final String[] TEST_PATTERNS = {"trace*", SPAN_HEADER, USER_HEADER};

		static final ExpressionParser parser = new SpelExpressionParser();

		@Bean
		ToCloudEventTransformer xmlTransformerWithNoExtensions() {
			ToCloudEventTransformer transformer = new ToCloudEventTransformer();
			transformer.setEventFormat(new XMLFormat());
			return transformer;
		}

		@Bean
		ToCloudEventTransformer jsonTransformerWithNoExtensions() {
			ToCloudEventTransformer transformer = new ToCloudEventTransformer();
			transformer.setEventFormat(new JsonFormat());
			return transformer;
		}

		@Bean
		ToCloudEventTransformer jsonTransformerWithExtensions() {
			ToCloudEventTransformer transformer = new ToCloudEventTransformer(TEST_PATTERNS);
			transformer.setEventFormat(new JsonFormat());
			return transformer;
		}

		@Bean
		ToCloudEventTransformer transformerWithNoExtensionsNoFormat() {
			return new ToCloudEventTransformer();
		}

		@Bean
		ToCloudEventTransformer transformerWithExtensionsNoFormat() {
			return new ToCloudEventTransformer(TEST_PATTERNS);
		}

		@Bean
		ToCloudEventTransformer transformerWithExtensionsNoFormatWithPrefix() {
			ToCloudEventTransformer toCloudEventsTransformer = new ToCloudEventTransformer(TEST_PATTERNS);
			toCloudEventsTransformer.setCloudEventPrefix("CLOUDEVENTS-");
			return toCloudEventsTransformer;
		}

		@Bean
		ToCloudEventTransformer xmlTransformerWithInvalidIDExpression() {
			ToCloudEventTransformer transformer = new ToCloudEventTransformer();
			transformer.setEventFormat(new XMLFormat());
			transformer.setEventIdExpression(parser.parseExpression("null"));
			return transformer;
		}

		@Bean
		ToCloudEventTransformer invalidEventFormatContentTypeExpression() {
			ToCloudEventTransformer toCloudEventsTransformer = new ToCloudEventTransformer();
			toCloudEventsTransformer.setEventFormatContentTypeExpression(new LiteralExpression("invalid/type"));
			return toCloudEventsTransformer;
		}

		@Bean
		ToCloudEventTransformer transformerWithNoExtensionsNoFormatEnabledWithProviderExpression() {
			ToCloudEventTransformer toCloudEventsTransformer = new ToCloudEventTransformer();
			toCloudEventsTransformer.setEventFormatContentTypeExpression(new LiteralExpression(JsonFormat.CONTENT_TYPE));
			return toCloudEventsTransformer;
		}

	}

	private record TestRecord(String sampleValue) implements Serializable {

	}

}
