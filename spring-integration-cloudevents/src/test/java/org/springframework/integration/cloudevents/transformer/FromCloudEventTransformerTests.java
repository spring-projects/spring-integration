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

import java.net.URI;
import java.time.OffsetDateTime;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.jackson.JsonFormat;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.cloudevents.CloudEventHeaders;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transformer.MessageTransformationException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test {@link FromCloudEventTransformer} transformer.
 *
 * @author Glenn Renfro
 * @author Artem Bilan
 *
 * @since 7.1
 */
@DirtiesContext
@SpringJUnitConfig
class FromCloudEventTransformerTests {

	static final byte[] EXPECTED_RESULT = "Hello, World!".getBytes();

	static final byte[] JSON_PAYLOAD = """
			{
				"specversion": "1.0",
				"id": "316b0cf3-0c4d-5858-6bd2-863a2042f442",
				"source": "/spring/null.jsonTransformerWithExtensions",
				"type": "spring.message",
				"subject": "test.subject",
				"datacontenttype": "text/plain",
				"time": "2026-01-30T08:53:06.099486-05:00",
				"traceid": "trace-123",
				"data": "Hello, World!"
			}
			""".getBytes();

	@Autowired
	FromCloudEventTransformer fromCloudEventTransformer;

	@Test
	void serializeTransformToMessage() {
		Message<byte[]> message =
				MessageBuilder.withPayload(JSON_PAYLOAD)
						.setHeader(MessageHeaders.CONTENT_TYPE, JsonFormat.CONTENT_TYPE)
						.setHeader("custom-header", "custom-value")
						.setHeader("another-header", 123)
						.build();

		Message<?> result = this.fromCloudEventTransformer.transform(message);

		assertThat(result.getHeaders())
				.containsEntry(CloudEventHeaders.EVENT_ID, "316b0cf3-0c4d-5858-6bd2-863a2042f442")
				.containsEntry(CloudEventHeaders.EVENT_TYPE, "spring.message")
				.containsEntry(CloudEventHeaders.EVENT_DATA_CONTENT_TYPE, "text/plain")
				.containsEntry(CloudEventHeaders.EVENT_SUBJECT, "test.subject")
				.containsEntry(CloudEventHeaders.PREFIX + "traceid", "trace-123")
				.containsEntry(MessageHeaders.CONTENT_TYPE, "text/plain")
				.containsEntry("custom-header", "custom-value")
				.containsEntry("another-header", 123)
				.containsEntry(CloudEventHeaders.EVENT_SOURCE, URI.create("/spring/null.jsonTransformerWithExtensions"))
				.containsKey(CloudEventHeaders.EVENT_TIME);

		assertThat(result.getPayload()).isEqualTo(EXPECTED_RESULT);
	}

	@Test
	void serializeTransformToMessageNoContentType() {
		Message<byte[]> message = new GenericMessage<>(JSON_PAYLOAD);
		this.fromCloudEventTransformer.setEventFormat(new JsonFormat());
		Message<?> result = this.fromCloudEventTransformer.transform(message);
		assertThat(result.getHeaders()).containsEntry(MessageHeaders.CONTENT_TYPE, "text/plain");
		assertThat(result.getPayload()).isEqualTo(EXPECTED_RESULT);
	}

	@Test
	void cloudEventTransformToMessage() {
		OffsetDateTime timestamp = OffsetDateTime.now();
		CloudEvent payload = CloudEventBuilder.v1().withId("123")
				.withSource(URI.create("/spring/null.jsonTransformerWithExtensions"))
				.withType("spring.event")
				.withTime(timestamp)
				.withDataContentType("text/plain")
				.withDataSchema(URI.create("dataschema"))
				.withSubject("test.subject")
				.withExtension("traceid", "trace-456")
				.withExtension("spanid", "span-789")
				.withExtension("userid", "user-123")
				.withData(EXPECTED_RESULT)
				.build();

		Message<?> message =
				MessageBuilder.withPayload(payload)
						.setHeader(MessageHeaders.CONTENT_TYPE, JsonFormat.CONTENT_TYPE)
						.build();

		Message<?> result = this.fromCloudEventTransformer.transform(message);

		assertThat(result.getHeaders())
				.containsEntry(CloudEventHeaders.EVENT_ID, "123")
				.containsEntry(CloudEventHeaders.EVENT_TYPE, "spring.event")
				.containsEntry(CloudEventHeaders.EVENT_SUBJECT, "test.subject")
				.containsEntry(CloudEventHeaders.EVENT_DATA_CONTENT_TYPE, "text/plain")
				.containsEntry(CloudEventHeaders.EVENT_SOURCE, URI.create("/spring/null.jsonTransformerWithExtensions"))
				.containsEntry(CloudEventHeaders.EVENT_DATA_SCHEMA, URI.create("dataschema"))
				.containsEntry(CloudEventHeaders.PREFIX + "traceid", "trace-456")
				.containsEntry(CloudEventHeaders.PREFIX + "spanid", "span-789")
				.containsEntry(CloudEventHeaders.PREFIX + "userid", "user-123")
				.containsEntry(MessageHeaders.CONTENT_TYPE, "text/plain")
				.containsEntry(CloudEventHeaders.EVENT_TIME, timestamp);

		assertThat(result.getPayload()).isEqualTo(EXPECTED_RESULT);
	}

	@Test
	void cloudEventWithNullDataTransformToMessage() {
		CloudEvent payload = CloudEventBuilder.v1().withId("empty-data")
				.withSource(URI.create("/spring/empty"))
				.withType("empty.event")
				.withDataContentType("text/plain")
				.build();
		Message<?> message = new GenericMessage<>(payload);
		message = this.fromCloudEventTransformer.transform(message);
		assertThat(message.getPayload()).isEqualTo(new byte[0]);
	}

	@Test
	void invalidPayloadTypeThrowsException() {
		Message<String> message = new GenericMessage<>("Invalid payload type");

		assertThatThrownBy(() -> this.fromCloudEventTransformer.transform(message))
				.isInstanceOf(MessageTransformationException.class)
				.cause()
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining(
						"Payload did not contain 'CloudEvent' nor could it be deserialized to a 'CloudEvent'");
	}

	@Test
	void byteArrayPayloadWithoutContentTypeThrowsException() {
		Message<byte[]> message = new GenericMessage<>("test".getBytes());

		assertThatThrownBy(() -> this.fromCloudEventTransformer.transform(message))
				.isInstanceOf(MessageTransformationException.class)
				.hasMessageContaining("No 'contentType' header found");
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	static class ContextConfiguration {

		@Bean
		FromCloudEventTransformer fromCloudEventTransformer() {
			return new FromCloudEventTransformer();
		}

	}

}
