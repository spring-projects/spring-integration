/*
 * Copyright 2022 the original author or authors.
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

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import graphql.execution.reactive.SubscriptionPublisher;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.graphql.ExecutionGraphQlRequest;
import org.springframework.graphql.ExecutionGraphQlResponse;
import org.springframework.graphql.ExecutionGraphQlService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.graphql.data.method.annotation.support.AnnotatedControllerConfigurer;
import org.springframework.graphql.execution.DefaultExecutionGraphQlService;
import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.graphql.support.DefaultExecutionGraphQlRequest;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.graphql.dsl.GraphQl;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Daniel Frey
 * @author Artem Bilan
 *
 * @since 6.0
 */
@SpringJUnitConfig
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class GraphQlMessageHandlerTests {

	@Autowired
	private FluxMessageChannel inputChannel;

	@Autowired
	private FluxMessageChannel resultChannel;

	@Autowired
	private PollableChannel errorChannel;

	@Autowired
	private GraphQlMessageHandler graphQlMessageHandler;

	@Autowired
	private UpdateRepository updateRepository;

	@Test
	@SuppressWarnings("unchecked")
	void testHandleMessageForQueryWithRequestInputProvided() {
		StepVerifier verifier =
				StepVerifier.create(
								Flux.from(this.resultChannel)
										.map(Message::getPayload)
										.cast(ExecutionGraphQlResponse.class)
						)
						.consumeNextWith(result -> {
							assertThat(result).isInstanceOf(ExecutionGraphQlResponse.class);
							Map<String, Object> data = result.getData();
							Map<String, Object> testQuery = (Map<String, Object>) data.get("testQuery");
							assertThat(testQuery.get("id")).isEqualTo("test-data");
						})
						.thenCancel()
						.verifyLater();

		ExecutionGraphQlRequest payload = new DefaultExecutionGraphQlRequest("{ testQuery { id } }", null, null, null,
				UUID.randomUUID().toString(), null);
		this.inputChannel.send(MessageBuilder.withPayload(payload).build());

		verifier.verify(Duration.ofSeconds(10));
	}

	@Test
	@SuppressWarnings("unchecked")
	void testHandleMessageForQueryWithQueryProvided() {
		String fakeQuery = "{ testQuery { id } }";
		this.graphQlMessageHandler.setOperation(fakeQuery);

		Locale locale = Locale.getDefault();
		this.graphQlMessageHandler.setLocale(locale);

		var resultMono = (Mono<ExecutionGraphQlResponse>) this.graphQlMessageHandler.handleRequestMessage(
				new GenericMessage<>(fakeQuery));
		StepVerifier.create(resultMono)
				.consumeNextWith(result -> {
					assertThat(result).isInstanceOf(ExecutionGraphQlResponse.class);
					Map<String, Object> data = result.getData();
					Map<String, Object> testQuery = (Map<String, Object>) data.get("testQuery");
					assertThat(testQuery.get("id")).isEqualTo("test-data");
				})
				.expectComplete()
				.verify();
	}

	@Test
	@SuppressWarnings("unchecked")
	void testHandleMessageForMutationWithRequestInputProvided() {
		String fakeId = UUID.randomUUID().toString();
		Update expected = new Update(fakeId);

		StepVerifier verifier = StepVerifier.create(
						Flux.from(this.resultChannel)
								.map(Message::getPayload)
								.cast(ExecutionGraphQlResponse.class)
				)
				.consumeNextWith(result -> {
							assertThat(result).isInstanceOf(ExecutionGraphQlResponse.class);
							Map<String, Object> data = result.getData();
							Map<String, Object> update = (Map<String, Object>) data.get("update");
							assertThat(update.get("id")).isEqualTo(fakeId);

							assertThat(this.updateRepository.current().block()).isEqualTo(expected);
						}
				)
				.thenCancel()
				.verifyLater();

		ExecutionGraphQlRequest payload =
				new DefaultExecutionGraphQlRequest("mutation { update(id: \"" + fakeId + "\") { id } }", null, null,
						null, UUID.randomUUID().toString(), null);
		this.inputChannel.send(MessageBuilder.withPayload(payload).build());

		verifier.verify(Duration.ofSeconds(10));

		StepVerifier.create(this.updateRepository.current())
				.expectNext(expected)
				.expectComplete()
				.verify();
	}

	@Test
	@SuppressWarnings("unchecked")
	void testHandleMessageForSubscriptionWithRequestInputProvided() {
		StepVerifier verifier = StepVerifier.create(
						Flux.from(this.resultChannel)
								.map(Message::getPayload)
								.cast(ExecutionGraphQlResponse.class)
								.mapNotNull(ExecutionGraphQlResponse::getData)
								.cast(SubscriptionPublisher.class)
								.map(Flux::from)
								.flatMap(data -> data)
				)
				.consumeNextWith(requestOutput -> {
					Map<String, Object> results = requestOutput.getData();
					assertThat(results).containsKey("results");

					Map<String, Object> operationResult = (Map<String, Object>) results.get("results");
					assertThat(operationResult)
							.containsKey("id")
							.containsValue("test-data-01");

				})
				.expectNextCount(9)
				.thenCancel()
				.verifyLater();

		ExecutionGraphQlRequest payload =
				new DefaultExecutionGraphQlRequest("subscription { results { id } }", null, null, null,
						UUID.randomUUID().toString(), null);
		this.inputChannel.send(MessageBuilder.withPayload(payload).build());

		verifier.verify(Duration.ofSeconds(10));
	}

	@Test
	void testHandleMessageWithInvalidPayload() {
		this.inputChannel.send(MessageBuilder.withPayload(new Object()).build());

		Message<?> errorMessage = errorChannel.receive(10_000);
		assertThat(errorMessage).isNotNull()
				.isInstanceOf(ErrorMessage.class)
				.extracting(Message::getPayload)
				.isInstanceOf(MessageHandlingException.class)
				.satisfies((ex) -> assertThat((Exception) ex)
						.hasStackTraceContaining("'operationExpression' must not be null"));
	}

	@Controller
	static class GraphQlController {

		final UpdateRepository updateRepository;

		GraphQlController(UpdateRepository updateRepository) {
			this.updateRepository = updateRepository;
		}

		@QueryMapping
		public Mono<QueryResult> testQuery() {
			return Mono.just(new QueryResult("test-data"));
		}

		@QueryMapping
		public Mono<QueryResult> testQueryById(@Argument String id) {
			return Mono.just(new QueryResult("test-data"));
		}

		@MutationMapping
		public Mono<Update> update(@Argument String id) {
			return this.updateRepository.save(new Update(id));
		}

		@SubscriptionMapping
		public Flux<QueryResult> results() {
			return Flux.range(1, 10)
					.map(d -> String.format("test-data-%02d", d))
					.map(QueryResult::new);
		}

	}

	@Repository
	static class UpdateRepository {

		private Update current;

		Mono<Update> save(Update update) {
			this.current = update;
			return Mono.justOrEmpty(this.current);
		}

		Mono<Update> current() {
			return Mono.just(this.current);
		}

	}

	@Configuration
	@EnableIntegration
	static class TestConfig {

		@Bean
		IntegrationFlow graphqlQueryMessageHandlerFlow(ExecutionGraphQlService graphQlService) {
			return IntegrationFlow.from(MessageChannels.flux("inputChannel"))
					.handle(GraphQl.gateway(graphQlService))
					.channel(c -> c.flux("resultChannel"))
					.get();
		}

		@Bean
		PollableChannel errorChannel() {
			return new QueueChannel();
		}

		@Bean
		UpdateRepository updateRepository() {
			return new UpdateRepository();
		}

		@Bean
		GraphQlController graphqlQueryController(UpdateRepository updateRepository) {
			return new GraphQlController(updateRepository);
		}

		@Bean
		ExecutionGraphQlService graphQlService(GraphQlSource graphQlSource) {
			return new DefaultExecutionGraphQlService(graphQlSource);
		}

		@Bean
		GraphQlSource graphQlSource(AnnotatedControllerConfigurer annotatedDataFetcherConfigurer) {
			return GraphQlSource.schemaResourceBuilder()
					.schemaResources(new ClassPathResource("graphql/test-schema.graphqls"))
					.configureRuntimeWiring(annotatedDataFetcherConfigurer)
					.build();
		}

		@Bean
		AnnotatedControllerConfigurer annotatedDataFetcherConfigurer() {
			return new AnnotatedControllerConfigurer();
		}

	}

	record QueryResult(String id) {

	}

	record Update(String id) {

	}

}
