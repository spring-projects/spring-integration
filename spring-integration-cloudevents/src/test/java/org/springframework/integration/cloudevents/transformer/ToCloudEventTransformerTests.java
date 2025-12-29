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
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.jackson.JsonFormat;
import io.cloudevents.xml.XMLFormat;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
 * @since 7.1
 */
@DirtiesContext
@SpringJUnitConfig
class ToCloudEventTransformerTests {

	private static final String TRACE_HEADER = "traceId";

	private static final String SPAN_HEADER = "spanId";

	private static final String USER_HEADER = "userId";

	private static final String[] TEST_PATTERNS = {"trace*", SPAN_HEADER, USER_HEADER};

	private static final byte[] PAYLOAD = "\"test message\"".getBytes(StandardCharsets.UTF_8);

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
	@SuppressWarnings("NullAway")
	void doJsonTransformWithPayloadBasedOnContentType() {
		CloudEvent cloudEvent = getTransformerNoExtensions(PAYLOAD, this.jsonFormat);
		assertThat(cloudEvent.getData().toBytes()).isEqualTo(PAYLOAD);
		assertThat(cloudEvent.getSource().toString()).isEqualTo("/spring/null.transformerWithNoExtensions");
		assertThat(cloudEvent.getDataSchema()).isNull();
		assertThat(cloudEvent.getDataContentType()).isEqualTo(JsonFormat.CONTENT_TYPE);
	}

	@Test
	@SuppressWarnings("NullAway")
	void doXMLTransformWithPayloadBasedOnContentType() {
		String xmlPayload = ("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><payload>" +
				"<message>testmessage</message></payload>");
		CloudEvent cloudEvent = getTransformerNoExtensions(xmlPayload.getBytes(), this.xmlFormat);
		assertThat(cloudEvent.getData().toBytes()).isEqualTo(xmlPayload.getBytes());
		assertThat(cloudEvent.getSource().toString()).isEqualTo("/spring/null.transformerWithNoExtensions");
		assertThat(cloudEvent.getDataSchema()).isNull();
		assertThat(cloudEvent.getDataContentType()).isEqualTo(XMLFormat.XML_CONTENT_TYPE);
	}

	@Test
	void convertMessageNoExtensions() {
		Message<byte[]> message = MessageBuilder.withPayload(PAYLOAD)
				.setHeader(MessageHeaders.CONTENT_TYPE, "text/plain")
				.setHeader(TRACE_HEADER, "test-value")
				.setHeader(SPAN_HEADER, "other-value")
				.build();
		Message<byte[]> result = transformMessage(message, this.transformerWithNoExtensionsNoFormat);
		assertThat(result.getPayload()).isEqualTo(PAYLOAD);
		assertThat(result.getHeaders()).containsKeys(TRACE_HEADER, SPAN_HEADER);
		assertThat(result.getHeaders()).doesNotContainKeys("ce-" + TRACE_HEADER, "ce-" + SPAN_HEADER);
	}

	@Test
	void convertMessageWithExtensions() {
		Message<byte[]> message = MessageBuilder.withPayload(PAYLOAD)
				.setHeader(MessageHeaders.CONTENT_TYPE, "text/plain")
				.setHeader(TRACE_HEADER, "test-value")
				.setHeader(SPAN_HEADER, "other-value")
				.build();
		Message<byte[]> result = transformMessage(message, this.transformerWithExtensionsNoFormat);
		assertThat(result.getHeaders()).containsKeys(TRACE_HEADER, SPAN_HEADER);
		assertThat(result.getHeaders()).containsKeys("ce-" + TRACE_HEADER, "ce-" + SPAN_HEADER);
		assertThat(result.getPayload()).isEqualTo(PAYLOAD);
	}

	@Test
	void convertMessageWithExtensionsNewPrefix() {
		Message<byte[]> message = MessageBuilder.withPayload(PAYLOAD)
				.setHeader(MessageHeaders.CONTENT_TYPE, "text/plain")
				.setHeader(TRACE_HEADER, "test-value")
				.setHeader(SPAN_HEADER, "other-value")
				.build();
		Message<byte[]> result = transformMessage(message, this.transformerWithExtensionsNoFormatWithPrefix);
		assertThat(result.getHeaders()).containsKeys(TRACE_HEADER, SPAN_HEADER);
		assertThat(result.getHeaders()).containsKeys("CLOUDEVENTS-" + TRACE_HEADER, "CLOUDEVENTS-" + SPAN_HEADER,
				"CLOUDEVENTS-id", "CLOUDEVENTS-specversion", "CLOUDEVENTS-datacontenttype");
		assertThat(result.getPayload()).isEqualTo(PAYLOAD);
	}

	@Test
	@SuppressWarnings("unchecked")
	void doTransformWithObjectPayload() throws Exception {
		TestRecord testRecord = new TestRecord("sample data");
		byte[] payload = convertPayloadToBytes(testRecord);
		Message<byte[]> message = MessageBuilder.withPayload(payload).setHeader("test_id", "test-id")
				.setHeader("contentType", JsonFormat.CONTENT_TYPE)
				.build();
		Object result = this.transformerWithNoExtensions.doTransform(message);

		assertThat(result).isNotNull();
		assertThat(result).isInstanceOf(Message.class);

		Message<byte[]> resultMessage = (Message<byte[]>) result;
		assertThat(resultMessage.getPayload()).isNotNull();
		assertThat(new String(resultMessage.getPayload())).endsWith(new String(payload) + "}");
	}

	@Test
	@SuppressWarnings("NullAway")
	void emptyExtensionNames() {
		Message<byte[]> message = createBaseMessage(PAYLOAD, JsonFormat.CONTENT_TYPE).build();

		Object result = this.transformerWithNoExtensions.doTransform(message);
		assertThat(result).isNotNull();
		Message<?> resultMessage = (Message<?>) result;
		assertThat(resultMessage.getPayload()).isNotNull();
		assertThat(message.getPayload()).isEqualTo(PAYLOAD);

	}

	@Test
	void noContentType() {
		Message<byte[]> message = MessageBuilder.withPayload(PAYLOAD).build();
		Message<?> result = this.transformerWithNoExtensions.transform(message);
		assertThat(result.getHeaders().get("ce-datacontenttype")).isEqualTo("application/octet-stream");
		assertThat(message.getPayload()).isEqualTo(PAYLOAD);

	}

	@Test
	void noContentTypeNoFormatEnabled() {
		Message<byte[]> message = MessageBuilder.withPayload(PAYLOAD).build();
		assertThatThrownBy(() -> this.transformerWithNoExtensionsNoFormatEnabled.transform(message))
				.isInstanceOf(MessageTransformationException.class)
				.hasMessageContaining("No EventFormat found for 'application/octet-stream'");
	}

	@Test
	@SuppressWarnings({"unchecked", "NullAway"})
	void multipleExtensionMappings() {
		String payload = "test message";
		Message<byte[]> message = createBaseMessage(payload.getBytes(), "application/cloudevents+json")
			.setHeader("correlation-id", "corr-999")
				.setHeader(TRACE_HEADER, "trace-123")
				.build();

		Object result = this.transformerWithExtensions.doTransform(message);

		assertThat(result).isNotNull();
		Message<byte[]> resultMessage = (Message<byte[]>) result;

		assertThat(resultMessage.getHeaders()).containsKeys("correlation-id");
		assertThat(resultMessage.getHeaders().get("correlation-id")).isEqualTo("corr-999");
		assertThat(new String(resultMessage.getPayload())).contains("\"traceId\":\"trace-123\"");
		assertThat(new String(resultMessage.getPayload())).doesNotContain("\"spanId\":\"span-456\"",
				"\"userId\":\"user-789\"");
	}

	@Test
	void emptyStringPayloadHandling() {
		Message<byte[]> message = createBaseMessage("".getBytes(), "application/cloudevents+json").build();
		Object result = this.transformerWithNoExtensions.doTransform(message);

		assertThat(result).isNotNull();
		assertThat(result).isInstanceOf(Message.class);
	}

	@Test
	void failWhenNoIdHeaderAndNoDefault() {
		Message<byte[]> message = MessageBuilder.withPayload(PAYLOAD)
				.setHeader("contentType", JsonFormat.CONTENT_TYPE)
				.build();

		assertThatThrownBy(() -> this.transformerWithInvalidIDExpression.transform(message))
				.isInstanceOf(MessageTransformationException.class)
				.hasMessageContaining("failed to transform message");
	}

	private CloudEvent getTransformerNoExtensions(byte[] payload, EventFormat eventFormat) {
		Message<byte[]> message = createBaseMessage(payload, eventFormat.serializedContentType())
				.setHeader("customheader", "test-value")
				.setHeader("otherheader", "other-value")
				.build();
		Message<byte[]> result = transformMessage(message, this.transformerWithNoExtensions);
		return eventFormat.deserialize(result.getPayload());
	}

	@SuppressWarnings("unchecked")
	private Message<byte[]> transformMessage(Message<byte[]> message, ToCloudEventTransformer transformer) {
		Object result = transformer.doTransform(message);

		assertThat(result).isNotNull();
		assertThat(result).isInstanceOf(Message.class);
		return (Message<byte[]>) result;
	}

	private byte[] convertPayloadToBytes(TestRecord testRecord) throws Exception {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(byteArrayOutputStream);
		out.writeObject(testRecord);
		out.flush();
		return  byteArrayOutputStream.toByteArray();
	}

	private static MessageBuilder<byte[]> createBaseMessage(byte[] payload, String contentType) {
		return MessageBuilder.withPayload(payload)
				.setHeader(MessageHeaders.CONTENT_TYPE, contentType);
	}

	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {

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
			ToCloudEventTransformer toCloudEventsTransformer =  new ToCloudEventTransformer();
			toCloudEventsTransformer.setFailOnNoFormat(false);
			return toCloudEventsTransformer;
		}

		@Bean
		public ToCloudEventTransformer transformerWithExtensionsNoFormat() {
			ToCloudEventTransformer toCloudEventsTransformer =  new ToCloudEventTransformer(TEST_PATTERNS);
			toCloudEventsTransformer.setFailOnNoFormat(false);
			return toCloudEventsTransformer;
		}

		@Bean
		public ToCloudEventTransformer transformerWithExtensionsNoFormatWithPrefix() {
			ToCloudEventTransformer toCloudEventsTransformer =  new ToCloudEventTransformer(TEST_PATTERNS);
			toCloudEventsTransformer.setFailOnNoFormat(false);
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
			ToCloudEventTransformer toCloudEventsTransformer =  new ToCloudEventTransformer();
			toCloudEventsTransformer.setFailOnNoFormat(true);
			return toCloudEventsTransformer;
		}

	}

	private record TestRecord(String sampleValue) implements Serializable { }

}
