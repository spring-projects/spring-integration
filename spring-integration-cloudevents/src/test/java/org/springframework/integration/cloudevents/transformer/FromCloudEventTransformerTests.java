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
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.jackson.JsonFormat;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transformer.MessageTransformationException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test {@link FromCloudEventTransformer} transformer.
 *
 * @author Glenn Renfro
 *
 * @since 7.1
 */
@DirtiesContext
@SpringJUnitConfig
public class FromCloudEventTransformerTests {
	private static final byte[] EXPECTED_RESULT = "Hello, World!".getBytes(StandardCharsets.UTF_8);

	@Autowired
	FromCloudEventTransformer fromCloudEventTransformer;

	@Test
	void serializeTransformToMessage() {
		String payload = """
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
				""";
		Message<byte[]> message =
				MessageBuilder.withPayload(payload.getBytes(StandardCharsets.UTF_8))
						.setHeader(MessageHeaders.CONTENT_TYPE, JsonFormat.CONTENT_TYPE)
						.setHeader("custom-header", "custom-value")
						.setHeader("another-header", 123)
						.build();
		Message<?> result = this.fromCloudEventTransformer.transform(message);
		assertThat(result.getHeaders())
				.containsEntry("ce-id", "316b0cf3-0c4d-5858-6bd2-863a2042f442")
				.containsEntry("ce-type", "spring.message")
				.containsEntry("ce-datacontenttype", "text/plain")
				.containsEntry("ce-subject", "test.subject")
				.containsEntry("ce-traceid", "trace-123")
				.containsEntry(MessageHeaders.CONTENT_TYPE, "text/plain")
				.containsEntry("custom-header", "custom-value")
				.containsEntry("another-header", 123)
				.containsEntry("ce-source", URI.create("/spring/null.jsonTransformerWithExtensions"));

		assertThat(result.getHeaders().get("ce-time")).isNotNull();
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
				.containsEntry("ce-id", "123")
				.containsEntry("ce-type", "spring.event")
				.containsEntry("ce-subject", "test.subject")
				.containsEntry("ce-datacontenttype", "text/plain")
				.containsEntry("ce-source", URI.create("/spring/null.jsonTransformerWithExtensions"))
				.containsEntry("ce-dataschema", URI.create("dataschema"))
				.containsEntry("ce-traceid", "trace-456")
				.containsEntry("ce-spanid", "span-789")
				.containsEntry("ce-userid", "user-123")
				.containsEntry(MessageHeaders.CONTENT_TYPE, "text/plain")
				.containsEntry("ce-time", timestamp);

		assertThat(result.getPayload()).isEqualTo(EXPECTED_RESULT);
	}

	@Test
	void cloudEventWithEmptyDataTransformToMessage() {
		CloudEvent payload = CloudEventBuilder.v1().withId("empty-data")
				.withSource(URI.create("/spring/empty"))
				.withType("empty.event")
				.withDataContentType("text/plain")
				.withData(new byte[0])
				.build();
		Message<?> message = MessageBuilder.withPayload(payload).build();
		Message<?> result = this.fromCloudEventTransformer.transform(message);

		assertThat(result.getHeaders()).containsEntry("ce-id", "empty-data");
		assertThat(result.getPayload()).isEqualTo(new byte[0]);
	}

	@Test
	void cloudEventWithNullDataTransformToMessage() {
		CloudEvent payload = CloudEventBuilder.v1().withId("empty-data")
				.withSource(URI.create("/spring/empty"))
				.withType("empty.event")
				.withDataContentType("text/plain")
				.build();
		Message<?> message = MessageBuilder.withPayload(payload).build();
		message = this.fromCloudEventTransformer.transform(message);
		assertThat(message.getPayload()).isEqualTo(new byte[0]);
	}

	@Test
	void invalidPayloadTypeThrowsException() {
		Message<String> message = MessageBuilder.withPayload("Invalid payload type").build();

		assertThatThrownBy(() -> this.fromCloudEventTransformer.transform(message))
				.isInstanceOf(MessageTransformationException.class)
				.cause()
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Payload did not contain CloudEvent nor could it be deserialized to a CloudEvent");
	}

	@Test
	void byteArrayPayloadWithoutContentTypeThrowsException() {
		Message<byte[]> message = MessageBuilder.withPayload("test".getBytes(StandardCharsets.UTF_8)).build();

		assertThatThrownBy(() -> this.fromCloudEventTransformer.transform(message))
				.isInstanceOf(MessageTransformationException.class)
				.hasMessageContaining("No Content-Type header found");
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	public static class ContextConfiguration {

		@Bean
		public FromCloudEventTransformer fromCloudEventTransformer() {
			return new FromCloudEventTransformer();
		}

	}

}
