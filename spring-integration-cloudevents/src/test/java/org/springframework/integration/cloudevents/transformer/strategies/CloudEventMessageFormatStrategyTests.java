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

package org.springframework.integration.cloudevents.transformer.strategies;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import org.junit.jupiter.api.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import static org.assertj.core.api.Assertions.assertThat;

public class CloudEventMessageFormatStrategyTests {

	@Test
	void toIntegrationMessageCloudEventToMessage() {
		CloudEventMessageFormatStrategy strategy = new CloudEventMessageFormatStrategy("ce_");

		CloudEvent cloudEvent = CloudEventBuilder.v1()
				.withId("test-id")
				.withSource(URI.create("test-source"))
				.withType("test-type")
				.withData("Some data".getBytes())
				.build();

		MessageHeaders headers = new MessageHeaders(null);

		Object result = strategy.toIntegrationMessage(cloudEvent, headers);

		assertThat(result).isInstanceOf(Message.class);
		Message<?> message = (Message<?>) result;
		assertThat(message.getPayload()).isNotNull();
		assertThat(message.getHeaders().containsKey("ce_id")).isTrue();
		assertThat(message.getHeaders().get("ce_id")).isEqualTo("test-id");
	}

	@Test
	void toIntegrationMessageWithAdditionalHeaders() {
		CloudEventMessageFormatStrategy strategy = new CloudEventMessageFormatStrategy("ce_");

		CloudEvent cloudEvent = CloudEventBuilder.v1()
				.withId("test-id")
				.withSource(URI.create("test-source"))
				.withType("test-type")
				.withData("application/json", "{}".getBytes())
				.build();

		Map<String, Object> additionalHeaders = new HashMap<>();
		additionalHeaders.put("custom-header", "custom-value");
		MessageHeaders headers = new MessageHeaders(additionalHeaders);

		Object result = strategy.toIntegrationMessage(cloudEvent, headers);

		assertThat(result).isInstanceOf(Message.class);
		Message<?> message = (Message<?>) result;
		assertThat(message.getHeaders().containsKey("custom-header")).isTrue();
		assertThat(message.getHeaders().get("custom-header")).isEqualTo("custom-value");
	}

	@Test
	void toIntegrationMessageWithDifferentPrefix() {
		CloudEventMessageFormatStrategy strategy = new CloudEventMessageFormatStrategy("cloudevent-");

		CloudEvent cloudEvent = CloudEventBuilder.v1()
				.withId("test-id")
				.withSource(URI.create("test-source"))
				.withType("test-type")
				.build();

		MessageHeaders headers = new MessageHeaders(null);

		Object result = strategy.toIntegrationMessage(cloudEvent, headers);

		assertThat(result).isInstanceOf(Message.class);
		Message<?> message = (Message<?>) result;
		assertThat(message.getHeaders().containsKey("cloudevent-id")).isTrue();
	}

	@Test
	void toIntegrationMessageWithEmptyHeaders() {
		CloudEventMessageFormatStrategy strategy = new CloudEventMessageFormatStrategy("ce_");

		CloudEvent cloudEvent = CloudEventBuilder.v1()
				.withId("test-id")
				.withSource(URI.create("test-source"))
				.withType("test-type")
				.build();

		MessageHeaders headers = new MessageHeaders(new HashMap<>());

		Object result = strategy.toIntegrationMessage(cloudEvent, headers);

		assertThat(result).isInstanceOf(Message.class);
	}
}
