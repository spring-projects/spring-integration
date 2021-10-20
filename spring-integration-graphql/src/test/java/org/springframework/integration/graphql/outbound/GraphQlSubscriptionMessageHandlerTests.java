/*
 * Copyright 2021 the original author or authors.
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

package org.springframework.integration.graphql.outbound;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.graphql.GraphQlService;
import org.springframework.graphql.RequestInput;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.graphql.data.method.annotation.support.AnnotatedControllerConfigurer;
import org.springframework.graphql.execution.BatchLoaderRegistry;
import org.springframework.graphql.execution.DefaultBatchLoaderRegistry;
import org.springframework.graphql.execution.ExecutionGraphQlService;
import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import graphql.ExecutionResult;
import graphql.execution.reactive.SubscriptionPublisher;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 *
 * @author Daniel Frey
 *
 */
@SpringJUnitConfig(GraphQlSubscriptionMessageHandlerTests.TestConfig.class)
@DirtiesContext
public class GraphQlSubscriptionMessageHandlerTests {

	@Autowired
	private FluxMessageChannel inputChannel;

	@Autowired
	private FluxMessageChannel resultChannel;

	@Autowired
	private PollableChannel errorChannel;

	@Test
	@SuppressWarnings("unchecked")
	void testHandleMessageForSubscription() {

		StepVerifier verifier = StepVerifier.create(
				Flux.from(this.resultChannel)
						.map(Message::getPayload)
						.cast(ExecutionResult.class)
						.map(ExecutionResult::getData)
						.cast(SubscriptionPublisher.class)
						.map(Flux::from)
						.flatMap(data -> data)
				)
				.consumeNextWith(executionResult -> {
					Map<String, Object> results = (Map<String, Object>) executionResult.getData();
					assertThat(results).containsKey("results");

					Map<String, Object> queryResult = (Map<String, Object>) results.get("results");
					assertThat(queryResult)
							.containsKey("id")
							.containsValue("test-data-01");

				})
				.expectNextCount(9)
				.thenCancel()
				.verifyLater();

		this.inputChannel.send(
				MessageBuilder
						.withPayload(new RequestInput("subscription { results { id } }", null, Collections.emptyMap()))
						.build()
		);

		verifier.verify(Duration.ofSeconds(10));
	}

	@Test
	void testHandleMessageForSubscriptionWithInvalidPayload() {

		this.inputChannel.send(
				MessageBuilder
						.withPayload("subscription { results { id } }")
						.build()
		);

		Message<?> errorMessage = errorChannel.receive(10_000);
		assertThat(errorMessage).isNotNull()
				.isInstanceOf(ErrorMessage.class)
				.extracting(Message::getPayload)
				.isInstanceOf(MessageHandlingException.class)
				.satisfies((ex) -> assertThat((Exception) ex)
						.hasMessageContaining(
								"Message payload needs to be 'org.springframework.graphql.RequestInput'"));

	}

	@Controller
	static class GraphqlSubscriptionController {

		@SubscriptionMapping
		public Flux<QueryResult> results() {
			return Flux.just(
					new QueryResult("test-data-01"),
					new QueryResult("test-data-02"),
					new QueryResult("test-data-03"),
					new QueryResult("test-data-04"),
					new QueryResult("test-data-05"),
					new QueryResult("test-data-06"),
					new QueryResult("test-data-07"),
					new QueryResult("test-data-08"),
					new QueryResult("test-data-09"),
					new QueryResult("test-data-10")
			);
		}

	}

	@Configuration
	@EnableIntegration
	static class TestConfig {

		@Bean
		GraphQlMessageHandler handler(GraphQlService graphQlService) {

			return new GraphQlMessageHandler(graphQlService);
		}

		@Bean
		IntegrationFlow graphqlQueryMessageHandlerFlow(GraphQlMessageHandler handler) {

			return IntegrationFlows.from(MessageChannels.flux("inputChannel"))
					.handle(handler)
					.channel(c -> c.flux("resultChannel"))
					.get();
		}

		@Bean
		PollableChannel errorChannel() {

			return new QueueChannel();
		}

		@Bean
		GraphqlSubscriptionController graphqlSubscriptionController() {

			return new GraphqlSubscriptionController();
		}

		@Bean
		GraphQlService graphQlService(GraphQlSource graphQlSource, BatchLoaderRegistry batchLoaderRegistry) {

			ExecutionGraphQlService service = new ExecutionGraphQlService(graphQlSource);
			service.addDataLoaderRegistrar(batchLoaderRegistry);

			return service;
		}

		@Bean
		GraphQlSource graphQlSource(AnnotatedControllerConfigurer annotatedDataFetcherConfigurer) {

			return GraphQlSource.builder()
					.schemaResources(new ClassPathResource("graphql/test-schema.graphqls"))
					.configureRuntimeWiring(annotatedDataFetcherConfigurer)
					.build();
		}

		@Bean
		AnnotatedControllerConfigurer annotatedDataFetcherConfigurer() {

			return new AnnotatedControllerConfigurer();
		}

		@Bean
		BatchLoaderRegistry batchLoaderRegistry() {

			return new DefaultBatchLoaderRegistry();
		}

	}

	static class QueryResult {

		private final String id;

		QueryResult(final String id) {
			this.id = id;
		}

		String getId() {
			return this.id;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof QueryResult)) {
				return false;
			}
			QueryResult that = (QueryResult) o;
			return getId().equals(that.getId());
		}

		@Override
		public int hashCode() {
			return Objects.hash(getId());
		}

		@Override
		public String toString() {
			return "QueryResult{" +
					"id='" + id + '\'' +
					'}';
		}
	}

}
