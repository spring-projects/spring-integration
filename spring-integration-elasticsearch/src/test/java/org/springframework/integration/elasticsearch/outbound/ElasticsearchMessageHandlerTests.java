/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.elasticsearch.outbound;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.elasticsearch.ElasticsearchContainerTest;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Sharang Gupta
 *
 * @since 7.3
 */
class ElasticsearchMessageHandlerTests implements ElasticsearchContainerTest, TestApplicationContextAware {

	private static final Duration TIMEOUT = Duration.ofSeconds(30);

	private static final SpelExpressionParser PARSER = new SpelExpressionParser();

	private static ReactiveElasticsearchOperations operations;

	@BeforeAll
	static void setUpOperations() {
		operations = ElasticsearchContainerTest.reactiveElasticsearchOperations();
	}

	@Test
	void indexesTheEntityOfThePayload() {
		ElasticsearchMessageHandler handler = initialized(new ElasticsearchMessageHandler(operations));

		handler.handleMessage(new GenericMessage<>(new Book("eip", "Enterprise Integration Patterns"))).block(TIMEOUT);

		assertThat(operations.get("eip", Book.class).block(TIMEOUT))
				.isEqualTo(new Book("eip", "Enterprise Integration Patterns"));
	}

	@Test
	void indexesEveryEntityOfACollectionPayload() {
		ElasticsearchMessageHandler handler = initialized(new ElasticsearchMessageHandler(operations));

		handler.handleMessage(new GenericMessage<>(List.of(new Book("ddd", "Domain-Driven Design"),
				new Book("refactoring", "Refactoring")))).block(TIMEOUT);

		assertThat(operations.exists("ddd", Book.class).block(TIMEOUT)).isTrue();
		assertThat(operations.exists("refactoring", Book.class).block(TIMEOUT)).isTrue();
	}

	@Test
	void indexesIntoTheIndexOfTheIndexNameExpression() {
		ElasticsearchMessageHandler handler = new ElasticsearchMessageHandler(operations);
		handler.setIndexNameExpression(PARSER.parseExpression("headers.shelf"));
		initialized(handler);

		handler.handleMessage(MessageBuilder.withPayload(new Book("sicp", "Structure and Interpretation"))
				.setHeader("shelf", "archive")
				.build()).block(TIMEOUT);

		assertThat(operations.exists("sicp", IndexCoordinates.of("archive")).block(TIMEOUT)).isTrue();
		assertThat(operations.exists("sicp", Book.class).block(TIMEOUT)).isFalse();
	}

	@Test
	void indexesAJsonDocumentIntoTheConfiguredIndex() {
		ElasticsearchMessageHandler handler = new ElasticsearchMessageHandler(operations);
		handler.setIndexName("shipments");
		initialized(handler);

		handler.handleMessage(new GenericMessage<>("{\"status\":\"shipped\"}")).block(TIMEOUT);

		assertThat(countOf("shipments", "status", "shipped")).isEqualTo(1);
	}

	@Test
	void indexesAMapDocumentIntoTheConfiguredIndex() {
		ElasticsearchMessageHandler handler = new ElasticsearchMessageHandler(operations);
		handler.setIndexName("payments");
		initialized(handler);

		handler.handleMessage(new GenericMessage<>(Map.of("status", "paid"))).block(TIMEOUT);

		assertThat(countOf("payments", "status", "paid")).isEqualTo(1);
	}

	@Test
	void indexesTheMapsOfACollectionPayloadIntoTheConfiguredIndex() {
		ElasticsearchMessageHandler handler = new ElasticsearchMessageHandler(operations);
		handler.setIndexName("refunds");
		initialized(handler);

		handler.handleMessage(new GenericMessage<>(List.of(Map.of("status", "requested"),
				Map.of("status", "requested")))).block(TIMEOUT);

		assertThat(countOf("refunds", "status", "requested")).isEqualTo(2);
	}

	@Test
	void signalsAnErrorForEntitiesOfDifferentClassesWithoutAnIndexName() {
		ElasticsearchMessageHandler handler = initialized(new ElasticsearchMessageHandler(operations));

		StepVerifier.create(handler.handleMessage(new GenericMessage<>(
						List.of(new Book("dune", "Dune"), new Magazine("wired", "Wired")))))
				.expectErrorSatisfies(error -> assertThat(error).hasMessageContaining("entities of the same class"))
				.verify(TIMEOUT);
	}

	@Test
	void signalsAnErrorForAnEntityOfASubclassWithoutAnIndexName() {
		ElasticsearchMessageHandler handler = initialized(new ElasticsearchMessageHandler(operations));

		StepVerifier.create(handler.handleMessage(new GenericMessage<>(
						List.of(new Novel("emma", "Emma"), new SignedNovel("persuasion", "Persuasion")))))
				.expectErrorSatisfies(error -> assertThat(error).hasMessageContaining("entities of the same class"))
				.verify(TIMEOUT);
	}

	@Test
	void signalsAnErrorForANullElementOfACollectionPayload() {
		ElasticsearchMessageHandler handler = initialized(new ElasticsearchMessageHandler(operations));

		StepVerifier.create(handler.handleMessage(new GenericMessage<>(Arrays.asList(new Book("dune", "Dune"), null))))
				.expectErrorSatisfies(error -> assertThat(error).hasMessageContaining("must not contain null elements"))
				.verify(TIMEOUT);
	}

	@Test
	void signalsAnErrorForACollectionPayloadToDelete() {
		ElasticsearchMessageHandler handler = initialized(
				new ElasticsearchMessageHandler(operations, ElasticsearchMessageHandler.Type.DELETE));

		StepVerifier.create(handler.handleMessage(new GenericMessage<>(List.of(new Book("dune", "Dune")))))
				.expectErrorSatisfies(error -> assertThat(error).hasMessageContaining("Collection payload is not supported"))
				.verify(TIMEOUT);
	}

	@Test
	void signalsAnErrorForAMapPayloadWithoutAnIndexName() {
		ElasticsearchMessageHandler handler = initialized(new ElasticsearchMessageHandler(operations));

		StepVerifier.create(handler.handleMessage(new GenericMessage<>(Map.of("status", "paid"))))
				.expectErrorSatisfies(error -> assertThat(error).hasMessageContaining("An index name is required"))
				.verify(TIMEOUT);
	}

	@Test
	void signalsAnErrorWhenTheIdExpressionEvaluatesToNull() {
		ElasticsearchMessageHandler handler =
				new ElasticsearchMessageHandler(operations, ElasticsearchMessageHandler.Type.DELETE);
		handler.setIndexName("books");
		handler.setIdExpression(PARSER.parseExpression("headers['bookId']"));
		initialized(handler);

		StepVerifier.create(handler.handleMessage(new GenericMessage<>("goos")))
				.expectErrorSatisfies(error -> assertThat(error).hasMessageContaining("'idExpression' must not evaluate to null"))
				.verify(TIMEOUT);
	}

	@Test
	void rejectsAnIdExpressionForTheIndexType() {
		ElasticsearchMessageHandler handler = new ElasticsearchMessageHandler(operations);
		handler.setIdExpression(PARSER.parseExpression("headers.id"));
		handler.setBeanFactory(TEST_INTEGRATION_CONTEXT);

		assertThatIllegalStateException().isThrownBy(handler::afterPropertiesSet)
				.withMessageContaining("'idExpression' only applies to the DELETE type");
	}

	@Test
	void deletesTheDocumentWithTheIdOfTheIdExpression() {
		operations.save(new Book("dp", "Design Patterns")).block(TIMEOUT);
		ElasticsearchMessageHandler handler =
				new ElasticsearchMessageHandler(operations, ElasticsearchMessageHandler.Type.DELETE);
		handler.setIndexName("books");
		handler.setIdExpression(PARSER.parseExpression("headers.bookId"));
		initialized(handler);

		handler.handleMessage(MessageBuilder.withPayload("ignored").setHeader("bookId", "dp").build()).block(TIMEOUT);

		assertThat(operations.exists("dp", Book.class).block(TIMEOUT)).isFalse();
	}

	@Test
	void deletesTheEntityOfThePayload() {
		operations.save(new Book("tdd", "Test-Driven Development")).block(TIMEOUT);
		ElasticsearchMessageHandler handler = initialized(
				new ElasticsearchMessageHandler(operations, ElasticsearchMessageHandler.Type.DELETE));

		handler.handleMessage(new GenericMessage<>(new Book("tdd", "Test-Driven Development"))).block(TIMEOUT);

		assertThat(operations.exists("tdd", Book.class).block(TIMEOUT)).isFalse();
	}

	@Test
	void deletesTheDocumentWithTheIdOfThePayload() {
		operations.save(new Book("goos", "Growing Object-Oriented Software")).block(TIMEOUT);
		ElasticsearchMessageHandler handler =
				new ElasticsearchMessageHandler(operations, ElasticsearchMessageHandler.Type.DELETE);
		handler.setIndexName("books");
		initialized(handler);

		handler.handleMessage(new GenericMessage<>("goos")).block(TIMEOUT);

		assertThat(operations.exists("goos", Book.class).block(TIMEOUT)).isFalse();
	}

	private static long countOf(String index, String field, String value) {
		IndexCoordinates coordinates = IndexCoordinates.of(index);
		operations.indexOps(coordinates).refresh().block(TIMEOUT);
		return operations.count(new CriteriaQuery(Criteria.where(field).is(value)), Map.class, coordinates)
				.block(TIMEOUT);
	}

	private static ElasticsearchMessageHandler initialized(ElasticsearchMessageHandler handler) {
		handler.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		handler.afterPropertiesSet();
		return handler;
	}

	@Document(indexName = "books")
	record Book(@Id String id, String title) {

	}

	@Document(indexName = "magazines")
	record Magazine(@Id String id, String title) {

	}

	@Document(indexName = "novels")
	static class Novel {

		@Id
		private final String id;

		private final String title;

		Novel(String id, String title) {
			this.id = id;
			this.title = title;
		}

		public String getId() {
			return this.id;
		}

		public String getTitle() {
			return this.title;
		}

	}

	@Document(indexName = "signed-novels")
	static class SignedNovel extends Novel {

		SignedNovel(String id, String title) {
			super(id, title);
		}

	}

}
