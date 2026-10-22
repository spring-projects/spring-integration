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
	void indexesAMapOrJsonDocumentWithTheValueOfItsIdFieldAsItsId() {
		ElasticsearchMessageHandler handler = new ElasticsearchMessageHandler(operations);
		handler.setIndexName("settlements");
		initialized(handler);

		handler.handleMessage(new GenericMessage<>(Map.of("id", "s-1", "status", "settled"))).block(TIMEOUT);
		handler.handleMessage(new GenericMessage<>("{\"id\": \"s-1\", \"status\": \"settled\"}")).block(TIMEOUT);

		assertThat(countOf("settlements", "status", "settled")).isEqualTo(1);
		assertThat(operations.exists("s-1", IndexCoordinates.of("settlements")).block(TIMEOUT)).isTrue();
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
		ElasticsearchMessageHandler handler = initialized(deletingHandler());

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
	void signalsAnErrorForAMapPayloadWithKeysThatAreNotStrings() {
		ElasticsearchMessageHandler handler = new ElasticsearchMessageHandler(operations);
		handler.setIndexName("payments");
		initialized(handler);

		StepVerifier.create(handler.handleMessage(new GenericMessage<>(Map.of(1, "paid"))))
				.expectErrorSatisfies(error -> assertThat(error).hasMessageContaining("must be String field names"))
				.verify(TIMEOUT);
	}

	@Test
	void signalsAnErrorWhenTheIdExpressionEvaluatesToNull() {
		ElasticsearchMessageHandler handler = deletingHandler();
		handler.setIndexName("books");
		handler.setIdExpression(PARSER.parseExpression("headers['bookId']"));
		initialized(handler);

		StepVerifier.create(handler.handleMessage(new GenericMessage<>("goos")))
				.expectErrorSatisfies(error -> assertThat(error).hasMessageContaining("'idExpression' must not evaluate to null"))
				.verify(TIMEOUT);
	}

	@Test
	void deletesTheDocumentWithTheIdOfTheIdExpression() {
		operations.save(new Book("dp", "Design Patterns")).block(TIMEOUT);
		ElasticsearchMessageHandler handler = deletingHandler();
		handler.setIndexName("books");
		handler.setIdExpression(PARSER.parseExpression("headers.bookId"));
		initialized(handler);

		handler.handleMessage(MessageBuilder.withPayload("ignored").setHeader("bookId", "dp").build()).block(TIMEOUT);

		assertThat(operations.exists("dp", Book.class).block(TIMEOUT)).isFalse();
	}

	@Test
	void deletesTheEntityOfThePayload() {
		operations.save(new Book("tdd", "Test-Driven Development")).block(TIMEOUT);
		ElasticsearchMessageHandler handler = initialized(deletingHandler());

		handler.handleMessage(new GenericMessage<>(new Book("tdd", "Test-Driven Development"))).block(TIMEOUT);

		assertThat(operations.exists("tdd", Book.class).block(TIMEOUT)).isFalse();
	}

	@Test
	void deletesTheDocumentWithTheIdOfAStringPayload() {
		operations.save(new Book("goos", "Growing Object-Oriented Software")).block(TIMEOUT);
		ElasticsearchMessageHandler handler = deletingHandler();
		handler.setIndexName("books");
		handler.setIdExpression(PARSER.parseExpression("payload"));
		initialized(handler);

		handler.handleMessage(new GenericMessage<>("goos")).block(TIMEOUT);

		assertThat(operations.exists("goos", Book.class).block(TIMEOUT)).isFalse();
	}

	@Test
	void signalsAnErrorForAStringPayloadToDeleteWithoutAnIdExpression() {
		ElasticsearchMessageHandler handler = deletingHandler();
		handler.setIndexName("books");
		initialized(handler);

		StepVerifier.create(handler.handleMessage(new GenericMessage<>("goos")))
				.expectErrorSatisfies(error -> assertThat(error).hasMessageContaining("Set an 'idExpression'"))
				.verify(TIMEOUT);
	}

	@Test
	void indexesOrDeletesWithTheTypeOfTheTypeExpression() {
		ElasticsearchMessageHandler handler = new ElasticsearchMessageHandler(operations);
		handler.setTypeExpression(PARSER.parseExpression("headers['deleted'] == true ? 'DELETE' : 'INDEX'"));
		handler.setIndexName("books");
		handler.setIdExpression(PARSER.parseExpression("headers['bookId']"));
		initialized(handler);

		handler.handleMessage(new GenericMessage<>(new Book("pragprog", "The Pragmatic Programmer"))).block(TIMEOUT);
		assertThat(operations.exists("pragprog", Book.class).block(TIMEOUT)).isTrue();

		handler.handleMessage(MessageBuilder.withPayload("{}")
				.setHeader("deleted", true)
				.setHeader("bookId", "pragprog")
				.build()).block(TIMEOUT);
		assertThat(operations.exists("pragprog", Book.class).block(TIMEOUT)).isFalse();
	}

	private static long countOf(String index, String field, String value) {
		IndexCoordinates coordinates = IndexCoordinates.of(index);
		operations.indexOps(coordinates).refresh().block(TIMEOUT);
		return operations.count(new CriteriaQuery(Criteria.where(field).is(value)), Map.class, coordinates)
				.block(TIMEOUT);
	}

	private static ElasticsearchMessageHandler deletingHandler() {
		ElasticsearchMessageHandler handler = new ElasticsearchMessageHandler(operations);
		handler.setType(ElasticsearchMessageHandler.Type.DELETE);
		return handler;
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
