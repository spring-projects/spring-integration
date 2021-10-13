package org.springframework.integration.graphql.outbound;

import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.graphql.GraphQlService;
import org.springframework.graphql.RequestInput;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.support.AnnotatedControllerConfigurer;
import org.springframework.graphql.execution.*;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

public class GraphqlQueryMessageHandlerTests {

	private GraphqlQueryMessageHandler handler;

	@BeforeEach
	void setup() {

		AnnotationConfigApplicationContext testContext = new AnnotationConfigApplicationContext();
		testContext.register(TestConfig.class);
		testContext.refresh();

		this.handler = testContext.getBean(GraphqlQueryMessageHandler.class);

	}

	@Test
	void testHandleMessageForQuery() {

		QueueChannel output = new QueueChannel();
		this.handler.setOutputChannel(output);

		Message<?> testMessage = new GenericMessage<>(new RequestInput("{ testQuery { id } }", null, emptyMap()));
		this.handler.handleMessage(testMessage);

		Message<?> message = output.receive(0);
		ExecutionResult result = (ExecutionResult) message.getPayload();
		assertThat(result).isInstanceOf(ExecutionResultImpl.class);

		Map<String, Object> data = result.getData();
		Map<String, Object> testQuery = (Map<String, Object>) data.get("testQuery");
		assertThat(testQuery.get("id")).isEqualTo("test-data");
	}

	@Controller
	static class GraphqlQueryController {

		@QueryMapping
		public Mono<QueryResult> testQuery() {
			return Mono.just(new QueryResult("test-data"));
		}

	}

	@Configuration
	static class TestConfig {

		@Bean
		public GraphqlQueryMessageHandler handler(GraphQlService graphQlService) {

			return new GraphqlQueryMessageHandler(graphQlService);
		}

		@Bean
		public GraphqlQueryController graphqlQueryController() {
			return new GraphqlQueryController();
		}

		@Bean
		public GraphQlService graphQlService(GraphQlSource graphQlSource, BatchLoaderRegistry batchLoaderRegistry) {
			ExecutionGraphQlService service = new ExecutionGraphQlService(graphQlSource);
			service.addDataLoaderRegistrar(batchLoaderRegistry);
			return service;
		}

		@Bean
		public GraphQlSource graphQlSource(AnnotatedControllerConfigurer annotatedDataFetcherConfigurer) {
			return GraphQlSource.builder()
					.schemaResources(new ClassPathResource("graphql/test-query-schema.graphqls"))
					.configureRuntimeWiring(annotatedDataFetcherConfigurer)
					.build();
		}

		@Bean
		public AnnotatedControllerConfigurer annotatedDataFetcherConfigurer() {
			return new AnnotatedControllerConfigurer();
		}

		@Bean
		public BatchLoaderRegistry batchLoaderRegistry() {
			return new DefaultBatchLoaderRegistry();
		}

	}

	static class QueryResult {

		private final String id;

		public QueryResult(final String id) {
			this.id = id;
		}

		public String getId() {
			return this.id;
		}

	}

}
