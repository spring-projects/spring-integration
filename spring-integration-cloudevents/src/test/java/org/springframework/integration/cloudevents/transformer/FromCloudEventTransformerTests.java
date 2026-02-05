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
import io.cloudevents.core.format.EventDeserializationException;
import io.cloudevents.jackson.JsonFormat;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transformer.MessageTransformationException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
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

	@Autowired
	FromCloudEventTransformer fromCloudEventTransformer;

	@Test
	void serializeTransformToMessage() {
		String payload = "{\"specversion\":\"1.0\"," +
				"\"id\":\"316b0cf3-0c4d-5858-6bd2-863a2042f442\",\"source\":\"/spring/null" +
				".jsonTransformerWithExtensions\",\"type\":\"spring.message\"," + "\"subject\":\"test.subject\"," +
				"\"datacontenttype\":\"text/plain\",\"time\":\"2026-01-30T08:53:06" +
				".099486-05:00\",\"traceid\":\"trace-123\",\"data\":\"Hello, World!\"}";
		Message<byte[]> message =
				MessageBuilder.withPayload(payload.getBytes(StandardCharsets.UTF_8))
						.setHeader(MessageHeaders.CONTENT_TYPE, JsonFormat.CONTENT_TYPE)
						.build();
		Message<?> result = this.fromCloudEventTransformer.transform(message);
		assertThat(result.getHeaders().get("ce-id")).isEqualTo("316b0cf3-0c4d-5858-6bd2-863a2042f442");
		assertThat(result.getHeaders().get("ce-time")).isNotNull();
		assertThat(result.getHeaders().get("ce-type")).isEqualTo("spring.message");
		assertThat(result.getHeaders().get("ce-datacontenttype")).isEqualTo("text/plain");
		assertThat(result.getPayload()).isEqualTo("Hello, World!".getBytes(StandardCharsets.UTF_8));
		assertThat(result.getHeaders().get("ce-source").toString())
				.isEqualTo("/spring/null.jsonTransformerWithExtensions");
		assertThat(result.getHeaders().get("ce-subject")).isEqualTo("test.subject");
		assertThat(result.getHeaders().get("ce-traceid")).isEqualTo("trace-123");
	}

	@Test
	void cloudEventTransformToMessage() {
		CloudEvent payload = CloudEventBuilder.v1().withId("123")
				.withSource(URI.create("/spring/null.jsonTransformerWithExtensions"))
				.withType("spring.event")
				.withTime(OffsetDateTime.now())
				.withDataContentType("text/plain")
				.withDataSchema(URI.create("dataschema"))
				.withSubject("test.subject")
				.withData("Hello, World!".getBytes(StandardCharsets.UTF_8))
				.build();
		Message<?> message =
				MessageBuilder.withPayload(payload)
						.setHeader(MessageHeaders.CONTENT_TYPE, JsonFormat.CONTENT_TYPE)
						.build();
		Message<?> result = this.fromCloudEventTransformer.transform(message);
		assertThat(result.getHeaders().get("ce-id")).isEqualTo("123");
		assertThat(result.getHeaders().get("ce-time")).isNotNull();
		assertThat(result.getHeaders().get("ce-type")).isEqualTo("spring.event");
		assertThat(result.getHeaders().get("ce-subject")).isEqualTo("test.subject");
		assertThat(result.getHeaders().get("ce-datacontenttype")).isEqualTo("text/plain");
		assertThat(result.getPayload()).isEqualTo("Hello, World!".getBytes(StandardCharsets.UTF_8));
		assertThat(result.getHeaders().get("ce-source").toString())
				.isEqualTo("/spring/null.jsonTransformerWithExtensions");
		assertThat(result.getHeaders().get("ce-dataschema").toString())
				.isEqualTo("dataschema");
	}

	@Test
	void cloudEventWithMultipleExtensionsTransformToMessage() {
		CloudEvent payload = CloudEventBuilder.v1().withId("789")
				.withSource(URI.create("/spring/extensions"))
				.withType("extension.event")
				.withDataContentType("text/plain")
				.withExtension("traceid", "trace-456")
				.withExtension("spanid", "span-789")
				.withExtension("userid", "user-123")
				.withData("Extension test".getBytes(StandardCharsets.UTF_8))
				.build();
		Message<?> message = MessageBuilder.withPayload(payload).build();
		Message<?> result = this.fromCloudEventTransformer.transform(message);

		assertThat(result.getHeaders().get("ce-id")).isEqualTo("789");
		assertThat(result.getHeaders().get("ce-traceid")).isEqualTo("trace-456");
		assertThat(result.getHeaders().get("ce-spanid")).isEqualTo("span-789");
		assertThat(result.getHeaders().get("ce-userid")).isEqualTo("user-123");
		assertThat(result.getPayload()).isEqualTo("Extension test".getBytes(StandardCharsets.UTF_8));
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

		assertThat(result.getHeaders().get("ce-id")).isEqualTo("empty-data");
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
		assertThatThrownBy(() ->  this.fromCloudEventTransformer.transform(message))
				.isInstanceOf(MessageTransformationException.class)
				.cause()
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("CloudEvent data can not be null");
	}

	@Test
	void cloudEventWithoutOptionalAttributesTransformToMessage() {
		CloudEvent payload = CloudEventBuilder.v1().withId("no-time")
				.withSource(URI.create("/spring/notime"))
				.withType("notime.event")
				.withDataContentType("text/plain")
				.withData("No time".getBytes(StandardCharsets.UTF_8))
				.build();
		Message<?> message = MessageBuilder.withPayload(payload).build();
		Message<?> result = this.fromCloudEventTransformer.transform(message);

		assertThat(result.getHeaders().get("ce-id")).isEqualTo("no-time");
		assertThat(result.getHeaders().get("ce-time")).isNull();
		assertThat(result.getHeaders().get("ce-subject")).isNull();
		assertThat(result.getHeaders().get("ce-dataschema")).isNull();
		assertThat(result.getPayload()).isEqualTo("No time".getBytes(StandardCharsets.UTF_8));
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
				.cause()
				.isInstanceOf(MessageHandlingException.class)
				.hasMessageContaining("No Content-Type header found");
	}

	@Test
	void byteArrayPayloadWithInvalidJsonThrowsException() {
		Message<byte[]> message =
				MessageBuilder.withPayload("not valid json".getBytes(StandardCharsets.UTF_8))
						.setHeader(MessageHeaders.CONTENT_TYPE, JsonFormat.CONTENT_TYPE)
						.build();

		assertThatThrownBy(() -> this.fromCloudEventTransformer.transform(message))
				.isInstanceOf(MessageTransformationException.class)
				.hasCauseInstanceOf(EventDeserializationException.class);
	}

	@Test
	void cloudEventPreservesOriginalMessageHeaders() {
		CloudEvent payload = CloudEventBuilder.v1().withId("preserve-headers")
				.withSource(URI.create("/spring/headers"))
				.withType("headers.event")
				.withDataContentType("text/plain")
				.withData("Test".getBytes(StandardCharsets.UTF_8))
				.build();
		Message<?> message = MessageBuilder.withPayload(payload)
				.setHeader("custom-header", "custom-value")
				.setHeader("another-header", 123)
				.build();
		Message<?> result = this.fromCloudEventTransformer.transform(message);

		assertThat(result.getHeaders().get("custom-header")).isEqualTo("custom-value");
		assertThat(result.getHeaders().get("another-header")).isEqualTo(123);
		assertThat(result.getHeaders().get("ce-id")).isEqualTo("preserve-headers");
	}

	@Test
	void componentTypeReturnsCorrectValue() {
		assertThat(this.fromCloudEventTransformer.getComponentType()).isEqualTo("from-cloudevent-transformer");
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
