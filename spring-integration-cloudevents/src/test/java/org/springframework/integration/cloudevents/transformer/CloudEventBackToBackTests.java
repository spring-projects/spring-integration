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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.cloudevents.CloudEventHeaders;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link ToCloudEventTransformer} and {@link FromCloudEventTransformer}.
 *
 * @author Glenn Renfro
 * @author Artem Bilan
 *
 * @since 7.1
 */
@SpringJUnitConfig
@DirtiesContext
@TestPropertySource(properties = "spring.application.name=test-app")
class CloudEventBackToBackTests {

	static final String[] TEST_PATTERNS = {"extensionone", "extensiontwo"};

	static final byte[] JSON_PAYLOAD = """
			{
				"message": "Hello, World!"
			}
			""".getBytes();

	@Autowired
	PollableChannel outputChannel;

	@Test
	void convertCloudEventToMessageToCloudEvent(
			@Autowired @Qualifier("messageInputChannel") MessageChannel inputChannel) {

		JsonFormat jsonFormat = new JsonFormat();
		OffsetDateTime currentTime = OffsetDateTime.now();
		CloudEvent payload = CloudEventBuilder.v1()
				.withData(JSON_PAYLOAD)
				.withType("text/plain")
				.withDataContentType("text/plain")
				.withId("123")
				.withTime(currentTime)
				.withSource(URI.create("/spring/null.jsonCaseFlow.ce:to-cloudevent-transformer#0"))
				.withExtension("extensionone", "test-val-one")
				.build();

		Message<?> message =
				MessageBuilder.withPayload(payload)
						.setHeader(MessageHeaders.CONTENT_TYPE, "cloudevent")
						.build();

		inputChannel.send(message);

		Message<?> result = outputChannel.receive(1000);

		assertThat(result).isNotNull();

		assertThat(result.getHeaders())
				.containsEntry(MessageHeaders.CONTENT_TYPE, "application/cloudevents+json")
				.containsEntry(CloudEventHeaders.EVENT_DATA_CONTENT_TYPE, "text/plain")
				.containsEntry(CloudEventHeaders.EVENT_ID, "123")
				.containsEntry(CloudEventHeaders.EVENT_TIME, currentTime)
				.containsEntry(CloudEventHeaders.PREFIX + "extensionone", "test-val-one")
				.containsEntry(CloudEventHeaders.EVENT_SOURCE,
						URI.create("/spring/null.jsonCaseFlow.ce:to-cloudevent-transformer#0"));

		CloudEvent cloudEvent = jsonFormat.deserialize((byte[]) result.getPayload());
		assertThat(cloudEvent.getDataContentType()).isEqualTo("text/plain");
		assertThat(cloudEvent.getId()).isNotNull(); //new ID created when new CloudEvent is created
		assertThat(cloudEvent.getTime()).isNotNull(); //new time created when new CloudEvent is created
		assertThat(cloudEvent.getSource().toString())
				.isEqualTo("/spring/test-app.messageToCloudEvent.ce:to-cloudevent-transformer#0");
		assertThat(cloudEvent.getData().toBytes()).isEqualTo(JSON_PAYLOAD);
	}

	@Test
	void convertMessageToSerializedCloudEventToMessage(
			@Autowired @Qualifier("jsonCaseInputChannel") MessageChannel inputChannel) {

		Message<?> message =
				MessageBuilder.withPayload(JSON_PAYLOAD)
						.setHeader(MessageHeaders.CONTENT_TYPE, "text/plain")
						.setHeader("customheader", "test-value")
						.setHeader("otherheader", "other-value")
						.setHeader("extensionone", "test-val-one")
						.setHeader("extensiontwo", "test-val-two")
						.build();

		inputChannel.send(message);

		Message<?> result = outputChannel.receive(1000);

		assertThat(result).isNotNull();

		MessageHeaders headers = result.getHeaders();
		assertThat(headers)
				.containsEntry(MessageHeaders.CONTENT_TYPE, "text/plain")
				.containsEntry(CloudEventHeaders.EVENT_DATA_CONTENT_TYPE, "text/plain")
				.containsEntry(CloudEventHeaders.EVENT_ID, message.getHeaders().get(MessageHeaders.ID).toString())
				.containsEntry(CloudEventHeaders.EVENT_SOURCE,
						URI.create("/spring/test-app.jsonCaseFlow" + ".ce:to-cloudevent-transformer#0"))
				.containsEntry("customheader", "test-value")
				.containsEntry("otherheader", "other-value")
				.containsEntry(CloudEventHeaders.PREFIX + "extensionone", "test-val-one")
				.containsEntry(CloudEventHeaders.PREFIX + "extensiontwo", "test-val-two")
				.containsKey(CloudEventHeaders.EVENT_TIME);

		assertThat(result.getPayload()).isEqualTo(JSON_PAYLOAD);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	static class ContextConfiguration {

		@Bean
		IntegrationFlow messageToCloudEvent() {
			ToCloudEventTransformer toCloudEventsTransformer = new ToCloudEventTransformer();
			toCloudEventsTransformer
					.setEventFormatContentTypeExpression(new LiteralExpression(JsonFormat.CONTENT_TYPE));

			return IntegrationFlow.from("messageInputChannel")
					.transform(new FromCloudEventTransformer())
					.transform(toCloudEventsTransformer)
					.channel("outputChannel")
					.get();
		}

		@Bean
		IntegrationFlow jsonCaseFlow() {
			ToCloudEventTransformer toCloudEventsTransformer = new ToCloudEventTransformer(TEST_PATTERNS);
			toCloudEventsTransformer
					.setEventFormatContentTypeExpression(new LiteralExpression(JsonFormat.CONTENT_TYPE));

			return IntegrationFlow.from("jsonCaseInputChannel")
					.transform(toCloudEventsTransformer)
					.transform(new FromCloudEventTransformer())
					.channel("outputChannel")
					.get();
		}

		@Bean
		PollableChannel outputChannel() {
			return new QueueChannel();
		}

	}

}
