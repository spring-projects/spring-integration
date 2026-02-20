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

package org.springframework.integration.cloudevents.dsl;

import java.net.URI;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.cloudevents.CloudEventHeaders;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Glenn Renfro
 *
 * @since 7.1
 */
@SpringJUnitConfig
@DirtiesContext
class CloudEventHeadersBuilderIntegrationTests {

	@Autowired
	@Qualifier("outputChannel")
	private PollableChannel outputChannel;

	@Test
	void testEnrichHeadersWithDirectValues(@Autowired @Qualifier("directFlow.input") MessageChannel inputChannel) {
		inputChannel.send(new GenericMessage<>("test-payload"));

		Message<?> result = this.outputChannel.receive(1000);
		assertThat(result.getHeaders())
				.containsEntry(CloudEventHeaders.EVENT_ID, "test-id-123")
				.containsEntry(CloudEventHeaders.EVENT_TYPE, "test.event")
				.containsEntry(CloudEventHeaders.EVENT_SOURCE, URI.create("https://example.com"))
				.containsEntry(CloudEventHeaders.EVENT_SUBJECT, "test-subject")
				.doesNotContainKey(CloudEventHeaders.EVENT_DATA_SCHEMA);
	}

	@Test
	void testEnrichHeadersWithDirectValuesNewPrefix(
			@Autowired @Qualifier("directFlowNewPrefixFlow.input") MessageChannel inputChannel) {

		inputChannel.send(new GenericMessage<>("test-payload"));

		Message<?> result = this.outputChannel.receive(1000);
		assertThat(result.getHeaders())
				.containsEntry("test-id", "test-id-123")
				.containsEntry("test-type", "test.event")
				.containsEntry("test-source", URI.create("https://example.com"))
				.containsEntry("test-subject", "test-subject")
				.doesNotContainKey("test-dataschema");
	}

	@Test
	void testEnrichHeadersWithFunctions(@Autowired @Qualifier("functionFlow.input") MessageChannel inputChannel) {
		Message<String> message = MessageBuilder.withPayload("order-456")
				.setHeader("action", "created")
				.build();
		inputChannel.send(message);

		Message<?> result = this.outputChannel.receive(1000);
		assertThat(result.getHeaders())
				.containsEntry(CloudEventHeaders.EVENT_ID, "id-order-456")
				.containsEntry(CloudEventHeaders.EVENT_TYPE, "order.created")
				.containsEntry(CloudEventHeaders.EVENT_SOURCE, URI.create("https://example.com/created"))
				.containsEntry(CloudEventHeaders.EVENT_DATA_SCHEMA, URI.create("https://example.com/schema/created"));
	}

	@Test
	void testEnrichHeadersWithExpressions(@Autowired @Qualifier("expressionFlow.input") MessageChannel inputChannel) {
		Message<String> message = MessageBuilder.withPayload("test-payload")
				.setHeader("orderId", "order-789")
				.setHeader("type", "updated")
				.build();
		inputChannel.send(message);

		Message<?> result = this.outputChannel.receive(1000);
		assertThat(result.getHeaders())
				.containsEntry(CloudEventHeaders.EVENT_ID, "order-789")
				.containsEntry(CloudEventHeaders.EVENT_TYPE, "order.updated")
				.containsEntry(CloudEventHeaders.EVENT_SOURCE, "https://example.com/updated");
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	static class TestConfiguration {

		@Bean
		IntegrationFlow directFlow() {
			return flow -> flow
					.enrichHeaders(CloudEvents.headers()
							.id("test-id-123")
							.type("test.event")
							.dataSchema(null)
							.source(URI.create("https://example.com"))
							.subject("test-subject"))
					.channel("outputChannel");
		}

		@Bean
		IntegrationFlow directFlowNewPrefixFlow() {
			return flow -> flow
					.enrichHeaders(CloudEvents.headers("test-")
							.id("test-id-123")
							.type("test.event")
							.dataSchema(null)
							.source(URI.create("https://example.com"))
							.subject("test-subject"))
					.channel("outputChannel");
		}

		@Bean
		IntegrationFlow functionFlow() {
			return flow -> flow
					.enrichHeaders(CloudEvents.headers()
							.dataSchemaFunction(
									msg -> URI.create("https://example.com/schema/" + msg.getHeaders().get("action")))
							.idFunction(msg -> "id-" + msg.getPayload())
							.typeFunction(msg -> "order." + msg.getHeaders().get("action"))
							.sourceFunction(msg -> URI.create("https://example.com/" + msg.getHeaders().get("action"))))
					.channel("outputChannel");
		}

		@Bean
		IntegrationFlow expressionFlow() {
			return flow -> flow
					.enrichHeaders(CloudEvents.headers()
							.idExpression("headers.orderId")
							.typeExpression("'order.' + headers.type")
							.sourceExpression("'https://example.com/' + headers.type"))
					.channel("outputChannel");
		}

		@Bean
		PollableChannel outputChannel() {
			return new QueueChannel();
		}

	}

}
