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
import io.cloudevents.CloudEventData;
import io.cloudevents.avro.compact.AvroCompactFormat;
import io.cloudevents.core.format.EventDeserializationException;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.core.format.EventSerializationException;
import io.cloudevents.jackson.JsonFormat;
import io.cloudevents.rw.CloudEventDataMapper;
import io.cloudevents.xml.XMLFormat;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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

@DirtiesContext
@SpringJUnitConfig
class ToCloudEventsTransformerTests {

	private static final String TRACE_HEADER = "traceId";

	private static final String SPAN_HEADER = "spanId";

	private static final String USER_HEADER = "userId";

	private static final String[] TEST_PATTERNS = {"trace*", SPAN_HEADER, USER_HEADER};

	private static final byte[] PAYLOAD = "\"test message\"".getBytes(StandardCharsets.UTF_8);

	@Autowired
	private ToCloudEventsTransformer transformerWithNoExtensions;

	@Autowired
	private ToCloudEventsTransformer transformerWithExtensions;

	@Autowired
	private ToCloudEventsTransformer transformerWithNoExtensionsNoFormat;

	@Autowired
	private ToCloudEventsTransformer transformerWithExtensionsNoFormat;

	@Autowired
	private ToCloudEventsTransformer transformerWithExtensionsNoFormatWithPrefix;

	@Autowired
	private ToCloudEventsTransformer transformerWithInvalidIDExpression;

	private final  JsonFormat jsonFormat = new JsonFormat();

	private final  AvroCompactFormat avroFormat = new AvroCompactFormat();

	private final  XMLFormat xmlFormat = new XMLFormat();

	@Test
	@SuppressWarnings("NullAway")
	void doJsonTransformWithPayloadBasedOnContentType() {
		CloudEvent cloudEvent = getTransformerNoExtensions(PAYLOAD, jsonFormat);
		assertThat(cloudEvent.getData().toBytes()).isEqualTo(PAYLOAD);
		assertThat(cloudEvent.getSource().toString()).isEqualTo("/spring/unknown.transformerWithNoExtensions");
		assertThat(cloudEvent.getDataSchema()).isNull();
		assertThat(cloudEvent.getDataContentType()).isEqualTo(JsonFormat.CONTENT_TYPE);
	}

	@Test
	@SuppressWarnings("NullAway")
	void doXMLTransformWithPayloadBasedOnContentType() {
		String xmlPayload = ("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><payload>" +
				"<message>testmessage</message></payload>");
		CloudEvent cloudEvent = getTransformerNoExtensions(xmlPayload.getBytes(), xmlFormat);
		assertThat(cloudEvent.getData().toBytes()).isEqualTo(xmlPayload.getBytes());
		assertThat(cloudEvent.getSource().toString()).isEqualTo("/spring/unknown.transformerWithNoExtensions");
		assertThat(cloudEvent.getDataSchema()).isNull();
		assertThat(cloudEvent.getDataContentType()).isEqualTo(XMLFormat.XML_CONTENT_TYPE);
	}

	@Test
	@SuppressWarnings("NullAway")
	void doAvroTransformWithPayloadBasedOnContentType() {
		CloudEvent cloudEvent = getTransformerNoExtensions(PAYLOAD, avroFormat);
		assertThat(cloudEvent.getData().toBytes()).isEqualTo(PAYLOAD);
		assertThat(cloudEvent.getSource().toString()).isEqualTo("/spring/unknown.transformerWithNoExtensions");
		assertThat(cloudEvent.getDataSchema()).isNull();
		assertThat(cloudEvent.getDataContentType()).isEqualTo(AvroCompactFormat.AVRO_COMPACT_CONTENT_TYPE);
	}

	@Test
	void unregisteredFormatType() {
		EventFormat testFormat = new EventFormat() {

			@Override
			public byte[] serialize(CloudEvent event) throws EventSerializationException {
				return new byte[0];
			}

			@Override
			public CloudEvent deserialize(byte[] bytes, CloudEventDataMapper<? extends CloudEventData> mapper) throws EventDeserializationException {
				return Mockito.mock(CloudEvent.class);
			}

			@Override
			public String serializedContentType() {
				return "application/cloudevents+invalid";
			}
		};
		assertThatThrownBy(() -> getTransformerNoExtensions(PAYLOAD, testFormat))
				.hasMessage("No EventFormat found for 'application/cloudevents+invalid'");
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
		Message<byte[]> message = createBaseMessage(PAYLOAD, "application/cloudevents+json").build();

		Object result = this.transformerWithNoExtensions.doTransform(message);
		assertThat(result).isNotNull();
		Message<?> resultMessage = (Message<?>) result;
		assertThat(resultMessage.getPayload()).isNotNull();
	}

	@Test
	void noContentType() {
		Message<byte[]> message = MessageBuilder.withPayload(PAYLOAD).build();
		assertThatThrownBy(() -> this.transformerWithNoExtensions.transform(message))
				.isInstanceOf(MessageTransformationException.class)
				.hasMessageContaining("Missing 'Content-Type' header");
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

		assertThatThrownBy(() -> this.transformerWithInvalidIDExpression.transform(message)).isInstanceOf(MessageTransformationException.class)
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
	private Message<byte[]> transformMessage(Message<byte[]> message, ToCloudEventsTransformer transformer) {
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
		public ToCloudEventsTransformer transformerWithNoExtensions() {
			return new ToCloudEventsTransformer();
		}

		@Bean
		public ToCloudEventsTransformer transformerWithExtensions() {
			return new ToCloudEventsTransformer(TEST_PATTERNS);
		}

		@Bean
		public ToCloudEventsTransformer transformerWithNoExtensionsNoFormat() {
			ToCloudEventsTransformer toCloudEventsTransformer =  new ToCloudEventsTransformer();
			toCloudEventsTransformer.setNoFormat(true);
			return toCloudEventsTransformer;
		}

		@Bean
		public ToCloudEventsTransformer transformerWithExtensionsNoFormat() {
			ToCloudEventsTransformer toCloudEventsTransformer =  new ToCloudEventsTransformer(TEST_PATTERNS);
			toCloudEventsTransformer.setNoFormat(true);
			return toCloudEventsTransformer;
		}

		@Bean
		public ToCloudEventsTransformer transformerWithExtensionsNoFormatWithPrefix() {
			ToCloudEventsTransformer toCloudEventsTransformer =  new ToCloudEventsTransformer(TEST_PATTERNS);
			toCloudEventsTransformer.setNoFormat(true);
			toCloudEventsTransformer.setCloudEventPrefix("CLOUDEVENTS-");
			return toCloudEventsTransformer;
		}

		@Bean
		public ToCloudEventsTransformer transformerWithInvalidIDExpression() {
			ToCloudEventsTransformer transformer = new ToCloudEventsTransformer();
			transformer.setEventIdExpression(parser.parseExpression("null"));
			return transformer;
		}
	}

	private record TestRecord(String sampleValue) implements Serializable { }
}
