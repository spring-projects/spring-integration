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

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

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

	@Autowired
	FromCloudEventTransformer fromCloudEventTransformer;

	@Autowired
	FromCloudEventTransformer fromCloudEventTransformerWithExtensionPattern;

	@Test
	void ceMessageContainingNullSubject() {
		Message<byte[]> message = getBaseMessageBuilder()
				.build();
		Message<?> result = this.fromCloudEventTransformer.transform(message);
		verifyBaseHeadersAndPayload(result);
		assertThat(result.getHeaders().get("ce-subject")).isNull();
	}

	private void verifyBaseHeadersAndPayload(Message<?> result) {
		assertThat(result.getHeaders().get("ce-id")).isNotNull();
		assertThat(result.getHeaders().get("ce-time")).isNotNull();
		assertThat(result.getHeaders().get("ce-datacontenttype")).isEqualTo("text/plain");
		assertThat(result.getHeaders().get("ce-type")).isEqualTo("test.type");
		assertThat(result.getPayload()).isEqualTo("hello world".getBytes(StandardCharsets.UTF_8));
		assertThat(result.getHeaders().get("ce-source").toString())
				.isEqualTo("/spring/null.toCloudEventTransformer.ce:to-cloudevent-transformer#0");
		assertThat(result.getHeaders().get("ce-dataschema").toString()).isEqualTo("test-schema");
	}

	private MessageBuilder<byte[]> getBaseMessageBuilder() {
		return MessageBuilder.withPayload("hello world".getBytes())
				.setHeader(MessageHeaders.CONTENT_TYPE, "text/plain")
				.setHeader("ce-id", UUID.randomUUID())
				.setHeader("ce-datacontenttype", "text/plain")
				.setHeader("ce-type", "test.type")
				.setHeader("ce-time", OffsetDateTime.parse("2026-02-05T15:30:00+01:00"))
				.setHeader("ce-source", "/spring/null.toCloudEventTransformer.ce:to-cloudevent-transformer#0")
				.setHeader("ce-dataschema", "test-schema");
	}

	@Test
	void ceMessageContainingExtensionPattern() {
		Message<byte[]> message = getBaseMessageBuilder()
				.setHeader("cvextensionone", "test-val-one")
				.setHeader("cvextensiontwo", "test-val-two")
				.setHeader("ce-subject", "test-subject")
				.build();
		Message<?> result = this.fromCloudEventTransformerWithExtensionPattern.transform(message);

		verifyBaseHeadersAndPayload(result);
		assertThat(result.getHeaders().get("ce-subject")).isEqualTo("test-subject");
		assertThat(result.getHeaders().get("cvextensionone")).isEqualTo("test-val-one");
		assertThat(result.getHeaders().get("cvextensiontwo")).isEqualTo("test-val-two");
	}

	@Test
	void ceMessageSerializeTransformToMessage() {
		String payload = "{\"specversion\":\"1.0\"," +
				"\"id\":\"316b0cf3-0c4d-5858-6bd2-863a2042f442\",\"source\":\"/spring/null" +
				".jsonTransformerWithExtensions\",\"type\":\"spring.message\"," +
				"\"datacontenttype\":\"application/cloudevents+json\",\"time\":\"2026-01-30T08:53:06" +
				".099486-05:00\",\"traceid\":\"trace-123\",\"data\":{\"message\":\"Hello, World!\"}}";
		Message<byte[]> message =
				MessageBuilder.withPayload(payload.getBytes(StandardCharsets.UTF_8))
						.setHeader(MessageHeaders.CONTENT_TYPE, JsonFormat.CONTENT_TYPE)
						.build();
		Message<?> result = this.fromCloudEventTransformer.transform(message);
		assertThat(result.getHeaders().get("ce-id")).isNotNull();
		assertThat(result.getHeaders().get("ce-time")).isNotNull();
		assertThat(result.getHeaders().get("ce-type")).isEqualTo("spring.message");
		assertThat(result.getHeaders().get("ce-datacontenttype")).isEqualTo("application/cloudevents+json");
		assertThat(result.getPayload()).isEqualTo("{\"message\":\"Hello, World!\"}".getBytes(StandardCharsets.UTF_8));
		assertThat(result.getHeaders().get("ce-source").toString())
				.isEqualTo("/spring/null.jsonTransformerWithExtensions");
		assertThat(result.getHeaders().get("ce-subject")).isNull();
	}

	@Test
	void ceMessageContainingNullSource() {
		Message<byte[]> message = MessageBuilder.withPayload("hello world".getBytes())
				.setHeader(MessageHeaders.CONTENT_TYPE, "text/plain")
				.setHeader("ce-id", UUID.randomUUID())
				.setHeader("ce-datacontenttype", "text/plain")
				.setHeader("ce-time", Instant.now().toEpochMilli())
				.setHeader("ce-extension-one", "test-val-one")
				.setHeader("ce-extension-two", "test-val-two")
				.build();
		assertThatThrownBy(() -> {
			this.fromCloudEventTransformer.transform(message);
		})
				.isInstanceOf(MessageTransformationException.class)
				.hasCauseInstanceOf(IllegalStateException.class);
	}

	@Test
	void ceMessageContainingNullType() {
		Message<byte[]> message = MessageBuilder.withPayload("hello world".getBytes())
				.setHeader(MessageHeaders.CONTENT_TYPE, "text/plain")
				.setHeader("ce-id", UUID.randomUUID())
				.setHeader("ce-datacontenttype", "text/plain")
				.setHeader("ce-time", Instant.now().toEpochMilli())
				.setHeader("ce-source", "/spring/null.toCloudEventTransformer.ce:to-cloudevent-transformer#0")
				.setHeader("ce-extension-one", "test-val-one")
				.setHeader("ce-extension-two", "test-val-two")
				.build();
		assertThatThrownBy(() -> {
			this.fromCloudEventTransformer.transform(message);
		})
				.isInstanceOf(MessageTransformationException.class)
				.hasCauseInstanceOf(IllegalStateException.class);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	public static class ContextConfiguration {

		@Bean
		public FromCloudEventTransformer fromCloudEventTransformer() {
			return new FromCloudEventTransformer();
		}

		@Bean
		public FromCloudEventTransformer fromCloudEventTransformerWithExtensionPattern() {
			FromCloudEventTransformer fromCloudEventTransformer = new FromCloudEventTransformer("cv*");
			return fromCloudEventTransformer;
		}

	}

}
