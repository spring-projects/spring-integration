/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.cassandra.dsl;

import java.time.Duration;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.core.InsertOptions;
import org.springframework.data.cassandra.core.ReactiveCassandraOperations;
import org.springframework.data.cassandra.core.WriteResult;
import org.springframework.integration.cassandra.CassandraContainerTest;
import org.springframework.integration.cassandra.IntegrationTestConfig;
import org.springframework.integration.cassandra.test.domain.BookSampler;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Artem Bilan
 *
 * @since 6.0
 */
@SpringJUnitConfig
@DirtiesContext
public class CassandraDslTests implements CassandraContainerTest {

	@Autowired
	@Qualifier("cassandraTruncateFlow.input")
	MessageChannel cassandraTruncateFlowInput;

	@Autowired
	@Qualifier("cassandraInsertFlow.input")
	MessageChannel cassandraInsertFlowInput;

	@Autowired
	@Qualifier("cassandraSelectFlow.input")
	MessageChannel cassandraSelectFlowInput;

	@Autowired
	FluxMessageChannel resultChannel;

	@Test
	void testCassandraDslConfiguration() {
		this.cassandraInsertFlowInput.send(new GenericMessage<>(BookSampler.getBookList(5)));

		Mono<Integer> testMono =
				Mono.from(this.resultChannel)
						.map(Message::getPayload)
						.cast(WriteResult.class)
						.map(r -> r.getRows().size());

		StepVerifier stepVerifier = StepVerifier.create(testMono)
				.expectNext(1)
				.expectComplete()
				.verifyLater();

		this.cassandraSelectFlowInput.send(MessageBuilder.withPayload("Cassandra Guru").setHeader("limit", 2).build());

		stepVerifier.verify(Duration.ofSeconds(10));

		this.cassandraTruncateFlowInput.send(new GenericMessage<>(""));
	}

	@Configuration
	@EnableIntegration
	public static class Config extends IntegrationTestConfig {

		@Bean
		IntegrationFlow cassandraTruncateFlow(ReactiveCassandraOperations cassandraOperations) {
			return flow -> flow
					.handle(Cassandra.outboundChannelAdapter(cassandraOperations)
									.statementExpression("T(QueryBuilder).truncate('book').build()"),
							e -> e.async(false));
		}

		@Bean
		IntegrationFlow cassandraInsertFlow(ReactiveCassandraOperations cassandraOperations) {
			return flow -> flow
					.handle(Cassandra.outboundChannelAdapter(cassandraOperations)
									.writeOptions(InsertOptions.builder()
											.ttl(60)
											.consistencyLevel(ConsistencyLevel.ONE)
											.build()),
							e -> e.async(false));
		}

		@Bean
		IntegrationFlow cassandraSelectFlow(ReactiveCassandraOperations cassandraOperations) {
			return flow -> flow
					.handle(Cassandra.outboundGateway(cassandraOperations)
							.query("SELECT * FROM book WHERE author = :author limit :size")
							.parameter("author", "payload")
							.parameter("size", m -> m.getHeaders().get("limit")))
					.channel(c -> c.flux("resultChannel"));
		}

	}

}
