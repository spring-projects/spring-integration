/*
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.cassandra.outbound;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.datastax.oss.driver.api.querybuilder.select.Select;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.InsertOptions;
import org.springframework.data.cassandra.core.ReactiveCassandraOperations;
import org.springframework.data.cassandra.core.WriteResult;
import org.springframework.data.cassandra.core.cql.WriteOptions;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.cassandra.CassandraContainerTest;
import org.springframework.integration.cassandra.IntegrationTestConfig;
import org.springframework.integration.cassandra.test.domain.Book;
import org.springframework.integration.cassandra.test.domain.BookSampler;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Soby Chacko
 * @author Artem Bilan
 *
 * @since 6.0
 */
@SpringJUnitConfig
@DirtiesContext
class CassandraMessageHandlerTests implements CassandraContainerTest {

	static final SpelExpressionParser PARSER = new SpelExpressionParser();

	@Autowired
	MessageHandler cassandraMessageHandler1;

	@Autowired
	MessageHandler cassandraMessageHandler2;

	@Autowired
	MessageHandler cassandraMessageHandler3;

	@Autowired
	MessageHandler cassandraMessageHandler4;

	@Autowired
	CassandraOperations template;

	@Autowired
	FluxMessageChannel resultChannel;

	@Test
	void testBasicCassandraInsert() {
		Book b1 = BookSampler.getBook();

		Message<Book> message = MessageBuilder.withPayload(b1).build();
		this.cassandraMessageHandler1.handleMessage(message);

		Select select = QueryBuilder.selectFrom("book").all();
		List<Book> books = this.template.select(select.build(), Book.class);
		assertThat(books).hasSize(1);

		this.template.delete(b1);
	}

	@Test
	void testCassandraBatchInsertAndSelectStatement() {
		List<Book> books = BookSampler.getBookList(5);

		this.cassandraMessageHandler2.handleMessage(new GenericMessage<>(books));

		Message<?> message = MessageBuilder.withPayload("Cassandra Guru").setHeader("limit", 2).build();
		this.cassandraMessageHandler4.handleMessage(message);

		Mono<Integer> testMono =
				Mono.from(this.resultChannel)
						.map(Message::getPayload)
						.cast(WriteResult.class)
						.map(r -> r.getRows().size());

		StepVerifier.create(testMono)
				.expectNext(1)
				.expectComplete()
				.verify();

		this.cassandraMessageHandler1.handleMessage(new GenericMessage<>(QueryBuilder.truncate("book").build()));
	}

	@Test
	void testCassandraBatchIngest() {
		List<Book> books = BookSampler.getBookList(5);
		List<List<Object>> ingestBooks =
				books.stream()
						.map(book ->
								List.<Object>of(
										book.isbn(),
										book.title(),
										book.author(),
										book.pages(),
										book.saleDate(),
										book.isInStock()))
						.toList();

		this.cassandraMessageHandler3.handleMessage(MessageBuilder.withPayload(ingestBooks).build());

		Select select = QueryBuilder.selectFrom("book").all();
		books = this.template.select(select.build(), Book.class);
		assertThat(books).hasSize(5);

		this.template.batchOps().delete(books);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	static class Config extends IntegrationTestConfig {

		@Autowired
		@Lazy
		ReactiveCassandraOperations template;

		@Bean
		MessageHandler cassandraMessageHandler1() {
			CassandraMessageHandler cassandraMessageHandler = new CassandraMessageHandler(this.template);
			cassandraMessageHandler.setAsync(false);
			return cassandraMessageHandler;
		}

		@Bean
		PollableChannel messageChannel() {
			return new NullChannel();
		}

		@Bean
		MessageHandler cassandraMessageHandler2(PollableChannel messageChannel) {
			CassandraMessageHandler cassandraMessageHandler = new CassandraMessageHandler(this.template);

			WriteOptions options =
					InsertOptions.builder()
							.ttl(60)
							.consistencyLevel(ConsistencyLevel.ONE)
							.build();

			cassandraMessageHandler.setWriteOptions(options);
			cassandraMessageHandler.setOutputChannel(messageChannel);
			cassandraMessageHandler.setAsync(false);
			return cassandraMessageHandler;
		}

		@Bean
		MessageHandler cassandraMessageHandler3() {
			CassandraMessageHandler cassandraMessageHandler = new CassandraMessageHandler(this.template);
			String cqlIngest =
					"insert into book (isbn, title, author, pages, saleDate, isInStock) values (?, ?, ?, ?, ?, ?)";
			cassandraMessageHandler.setIngestQuery(cqlIngest);
			cassandraMessageHandler.setAsync(false);
			return cassandraMessageHandler;
		}

		@Bean
		FluxMessageChannel resultChannel() {
			return new FluxMessageChannel();
		}

		@Bean
		MessageHandler cassandraMessageHandler4(FluxMessageChannel resultChannel) {
			CassandraMessageHandler cassandraMessageHandler = new CassandraMessageHandler(this.template);
			cassandraMessageHandler.setQuery("SELECT * FROM book WHERE author = :author limit :size");

			Map<String, Expression> params = new HashMap<>();
			params.put("author", PARSER.parseExpression("payload"));
			params.put("size", PARSER.parseExpression("headers.limit"));

			cassandraMessageHandler.setParameterExpressions(params);

			cassandraMessageHandler.setOutputChannel(resultChannel);
			cassandraMessageHandler.setProducesReply(true);
			return cassandraMessageHandler;
		}

	}

}
