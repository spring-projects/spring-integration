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

package org.springframework.integration.cassandra.config;

import java.util.ArrayList;
import java.util.List;

import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.datastax.oss.driver.api.querybuilder.select.Select;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.cassandra.core.ReactiveCassandraTemplate;
import org.springframework.data.cassandra.core.WriteResult;
import org.springframework.integration.cassandra.CassandraContainerTest;
import org.springframework.integration.cassandra.IntegrationTestConfig;
import org.springframework.integration.cassandra.test.domain.Book;
import org.springframework.integration.cassandra.test.domain.BookSampler;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Filippo Balicchia
 * @author Artem Bilan
 *
 * @since 6.0
 */
@SpringJUnitConfig(CassandraOutboundAdapterIntegrationTests.Config.class)
@DirtiesContext
class CassandraOutboundAdapterIntegrationTests implements CassandraContainerTest {

	@Autowired
	private ReactiveCassandraTemplate cassandraTemplate;

	@Autowired
	private DirectChannel cassandraMessageHandler1;

	@Autowired
	private DirectChannel cassandraMessageHandler2;

	@Autowired
	private DirectChannel cassandraMessageHandler3;

	@Autowired
	private DirectChannel cassandraMessageHandler4;

	@Autowired
	private DirectChannel inputChannel;

	@Autowired
	private FluxMessageChannel resultChannel;

	@Test
	void testBasicCassandraInsert() {
		Book b1 = BookSampler.getBook();
		Message<Book> message = MessageBuilder.withPayload(b1).build();
		this.cassandraMessageHandler1.send(message);
		Select select = QueryBuilder.selectFrom("book").all();
		List<Book> books = this.cassandraTemplate.select(select.build(), Book.class).collectList().block();
		assertThat(books).hasSize(1);
		this.cassandraTemplate.delete(b1);
	}

	@Test
	void testCassandraBatchInsertAndSelectStatement() {
		List<Book> books = BookSampler.getBookList(5);
		this.cassandraMessageHandler2.send(new GenericMessage<>(books));
		Message<?> message = MessageBuilder.withPayload("Cassandra Puppy Guru").setHeader("limit", 2).build();
		this.inputChannel.send(message);

		Mono<Integer> testMono =
				Mono.from(this.resultChannel)
						.map(Message::getPayload)
						.cast(WriteResult.class)
						.map(r -> r.getRows().size());

		StepVerifier.create(testMono)
				.expectNext(2)
				.expectComplete()
				.verify();

		this.cassandraMessageHandler1.send(new GenericMessage<>(QueryBuilder.truncate("book").build()));

	}

	@Test
	void testCassandraBatchIngest() {
		List<Book> books = BookSampler.getBookList(5);
		List<List<?>> ingestBooks = new ArrayList<>();
		for (Book b : books) {

			List<Object> l = new ArrayList<>();
			l.add(b.isbn());
			l.add(b.title());
			l.add(b.author());
			l.add(b.pages());
			l.add(b.saleDate());
			l.add(b.isInStock());
			ingestBooks.add(l);
		}

		Message<List<List<?>>> message = MessageBuilder.withPayload(ingestBooks).build();
		this.cassandraMessageHandler3.send(message);
		Select select = QueryBuilder.selectFrom("book").all();
		books = this.cassandraTemplate.select(select.build(), Book.class).collectList().block();
		assertThat(books).hasSize(5);
		this.cassandraTemplate.batchOps().delete(books);
	}

	@Test
	void testExpressionTruncate() {
		Message<Book> message = MessageBuilder.withPayload(BookSampler.getBook()).build();
		this.cassandraMessageHandler1.send(message);
		Select select = QueryBuilder.selectFrom("book").all();
		List<Book> books = this.cassandraTemplate.select(select.build(), Book.class).collectList().block();
		assertThat(books).hasSize(1);
		this.cassandraMessageHandler4.send(MessageBuilder.withPayload("Empty").build());
		books = this.cassandraTemplate.select(select.build(), Book.class).collectList().block();
		assertThat(books).hasSize(0);
	}

	@Configuration
	@EnableIntegration
	@ImportResource("org/springframework/integration/cassandra/config/CassandraOutboundAdapterIntegrationTests-context.xml")
	public static class Config extends IntegrationTestConfig {

	}

}
