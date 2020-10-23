/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.integration.r2dbc.dsl;

import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.Lifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.r2dbc.config.R2dbcDatabaseConfiguration;
import org.springframework.integration.r2dbc.entity.Person;
import org.springframework.integration.r2dbc.outbound.R2dbcMessageHandler;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * @author Artem Bilan
 *
 * @since 5.4
 */
@SpringJUnitConfig
@DirtiesContext
public class R2dbcDslTests {

	@Autowired
	R2dbcEntityTemplate r2dbcEntityTemplate;

	@Autowired
	Lifecycle r2dbcInboundChannelAdapter;

	@BeforeEach
	public void setup() {
		List<String> statements =
				Arrays.asList(
						"DROP TABLE IF EXISTS person;",
						"CREATE table person (id INT AUTO_INCREMENT NOT NULL, name VARCHAR2, age INT NOT NULL);");

		DatabaseClient databaseClient = this.r2dbcEntityTemplate.getDatabaseClient();
		statements.forEach(it ->
				databaseClient.sql(it)
						.fetch()
						.rowsUpdated()
						.as(StepVerifier::create)
						.expectNextCount(1)
						.verifyComplete());
	}

	@Test
	void testR2DbcDsl() {
		this.r2dbcInboundChannelAdapter.start();

		this.r2dbcEntityTemplate.insert(new Person("Bob", 35))
				.then()
				.as(StepVerifier::create)
				.verifyComplete();

		await().until(() ->
				this.r2dbcEntityTemplate.select(Person.class)
						.matching(Query.query(Criteria.where("age").is(36)))
						.one()
						.block(Duration.ofMillis(100)) != null);
	}

	@Configuration
	@EnableIntegration
	@Import(R2dbcDatabaseConfiguration.class)
	static class R2dbcMessageSourceConfiguration {

		@Bean
		IntegrationFlow r2dbcDslFlow(R2dbcEntityTemplate r2dbcEntityTemplate) {
			return IntegrationFlows
					.from(R2dbc.inboundChannelAdapter(r2dbcEntityTemplate,
							(selectCreator) ->
									selectCreator.createSelect("person")
											.withProjection("*")
											.withCriteria(Criteria.where("id").is(1)))
									.expectSingleResult(true)
									.payloadType(Person.class)
									.updateSql("UPDATE Person SET id='2' where id = :id")
									.bindFunction((DatabaseClient.GenericExecuteSpec bindSpec, Person o) ->
											bindSpec.bind("id", o.getId())),
							e -> e.poller(p -> p.fixedDelay(100)).autoStartup(false).id("r2dbcInboundChannelAdapter"))
					.<Mono<?>>handle((p, h) -> p, e -> e.async(true))
					.channel(MessageChannels.flux())
					.handle(R2dbc.outboundChannelAdapter(r2dbcEntityTemplate)
							.queryType(R2dbcMessageHandler.Type.UPDATE)
							.tableNameExpression("payload.class.simpleName")
							.criteria((message) -> Criteria.where("id").is(2))
							.values("{age:36}"))
					.get();
		}

	}

}
