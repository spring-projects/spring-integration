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

import io.cloudevents.jackson.JsonFormat;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test {@link FromCloudEventTransformer} transformer.
 *
 * @author Glenn Renfro
 *
 * @since 7.1
 */
@SpringJUnitConfig
@DirtiesContext
public class CloudEventBackToBackTests {

	private static final byte[] JSON_PAYLOAD = "{\"message\":\"Hello, World!\"}".getBytes(StandardCharsets.UTF_8);

	@Autowired
	private PollableChannel outputChannel;

	@Test
	public void convertHeaderBasedCEToMessage(
			@Autowired @Qualifier("defaultCaseInputChannel") MessageChannel inputChannel) {
		Message<byte[]> message = MessageBuilder.withPayload("hello world".getBytes(StandardCharsets.UTF_8))
				.setHeader(MessageHeaders.CONTENT_TYPE, "text/plain")
				.setHeader("ce-extension-one", "test-val-one")
				.setHeader("ce-extension-two", "test-val-two")
				.build();

		inputChannel.send(message);

		Message<?> result = outputChannel.receive(1000);

		assertThat(result).isNotNull();

		MessageHeaders headers = result.getHeaders();
		assertThat(headers.get(MessageHeaders.CONTENT_TYPE)).isEqualTo("application/cloudevents");
		assertThat(headers.get("ce-datacontenttype")).isEqualTo("text/plain");
		assertThat(headers.get("ce-id")).isEqualTo(message.getHeaders().getId().toString());
		assertThat(headers.get("ce-time")).isNotNull();
		assertThat(headers.get("ce-subject")).isNull();
		assertThat(headers.get("ce-source").toString())
				.isEqualTo("/spring/null.defaultCaseFlow.ce:to-cloudevent-transformer#0");
		assertThat(headers.get("ce-extension-one")).isEqualTo("test-val-one");
		assertThat(headers.get("ce-extension-two")).isEqualTo("test-val-two");
		assertThat(result.getPayload()).isEqualTo("hello world".getBytes(StandardCharsets.UTF_8));
	}

	@Test
	public void convertSerializedCEToMessage(
			@Autowired @Qualifier("jsonCaseInputChannel") MessageChannel inputChannel) {

		Message<?> message = MessageBuilder.withPayload(JSON_PAYLOAD)
				.setHeader(MessageHeaders.CONTENT_TYPE, "text/plain")
				.setHeader("customheader", "test-value")
				.setHeader("otherheader", "other-value")
				.setHeader("ce-extension-one", "test-val-one")
				.setHeader("ce-extension-two", "test-val-two")
				.build();
		inputChannel.send(message);

		Message<?> result = outputChannel.receive(1000);

		assertThat(result).isNotNull();

		MessageHeaders headers = result.getHeaders();
		assertThat(headers.get(MessageHeaders.CONTENT_TYPE)).isEqualTo(JsonFormat.CONTENT_TYPE);
		assertThat(headers.get("ce-datacontenttype")).isEqualTo("text/plain");
		assertThat(headers.get("ce-id")).isEqualTo(message.getHeaders().get(MessageHeaders.ID).toString());
		assertThat(headers.get("ce-time")).isNotNull();
		assertThat(headers.get("ce-subject")).isNull();
		assertThat(headers.get("ce-source").toString())
				.isEqualTo("/spring/null.jsonCaseFlow.ce:to-cloudevent-transformer#0");
		assertThat(headers.get("customheader")).isEqualTo("test-value");
		assertThat(headers.get("otherheader")).isEqualTo("other-value");
		assertThat(headers.get("ce-extension-one")).isEqualTo("test-val-one");
		assertThat(headers.get("ce-extension-two")).isEqualTo("test-val-two");
		assertThat(result.getPayload()).isEqualTo(JSON_PAYLOAD);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	static class ContextConfiguration {

		@Bean
		public IntegrationFlow defaultCaseFlow() {
			return IntegrationFlow.from("defaultCaseInputChannel")
					.transform(new ToCloudEventTransformer())
					.transform(new FromCloudEventTransformer())
					.channel("outputChannel")
					.get();
		}

		@Bean
		public IntegrationFlow jsonCaseFlow() {
			ToCloudEventTransformer toCloudEventsTransformer = new ToCloudEventTransformer();
			toCloudEventsTransformer
					.setEventFormatContentTypeExpression(new LiteralExpression(JsonFormat.CONTENT_TYPE));
			return IntegrationFlow.from("jsonCaseInputChannel")
					.transform(toCloudEventsTransformer)
					.transform(new FromCloudEventTransformer())
					.channel("outputChannel")
					.get();
		}

		@Bean
		public PollableChannel outputChannel() {
			return new QueueChannel();
		}

	}

}
