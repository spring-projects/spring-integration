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

package org.springframework.integration.mongodb.inbound;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.ChangeStreamEvent;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.mongodb.Person;
import org.springframework.integration.mongodb.dsl.MongoDb;
import org.springframework.integration.mongodb.support.MongoHeaders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.mongodb.client.model.changestream.OperationType;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * @author Artem Bilan
 *
 * @since 5.3
 */
@Disabled("Requires MongoDb server of version 4.x started with 'replSet' option.")
@SpringJUnitConfig
@DirtiesContext
public class MongoDbChangeStreamMessageProducerTests {

	private static final String CONNECTION_STRING =
			"mongodb://127.0.0.1:27017/?replicaSet=rs0&w=majority&uuidrepresentation=javaLegacy";

	private static MongoClient mongoClient;

	@Autowired
	ReactiveMongoOperations mongoTemplate;

	@Autowired
	FluxMessageChannel fluxMessageChannel;

	@Autowired
	MessageProducerSupport changeStreamMessageProducer;

	@BeforeAll
	static void setup() {
		mongoClient = MongoClients.create(CONNECTION_STRING);
	}

	@AfterAll
	static void tearDown() {
		mongoClient.close();
	}

	@BeforeEach
	void prepare() {
		this.mongoTemplate.remove(Person.class).all()
				.then()
				.onErrorResume(ex -> this.mongoTemplate.createCollection(Person.class).then())
				.block(Duration.ofSeconds(10));
	}

	@AfterEach
	void cleanUp() {
		this.mongoTemplate.remove(Person.class).all()
				.block(Duration.ofSeconds(10));
	}

	@Test
	void testChangeStreamMessageProducer() {
		Person person1 = new Person("John", 38);
		Person person2 = new Person("Josh", 39);
		Person person3 = new Person("Jack", 37);

		this.changeStreamMessageProducer.start();

		StepVerifier stepVerifier =
				Flux.from(this.fluxMessageChannel)
						.as(StepVerifier::create)
						.assertNext((message) -> {
							assertThat(message.getPayload())
									.isInstanceOf(ChangeStreamEvent.class)
									.extracting("body")
									.asInstanceOf(InstanceOfAssertFactories.type(Person.class))
									.isEqualTo(person1);
							assertThat(message.getHeaders())
									.containsEntry(MongoHeaders.COLLECTION_NAME, "person")
									.containsEntry(MongoHeaders.CHANGE_STREAM_OPERATION_TYPE, OperationType.INSERT)
									.containsKeys(MongoHeaders.CHANGE_STREAM_TIMESTAMP,
											MongoHeaders.CHANGE_STREAM_RESUME_TOKEN);
						})
						.assertNext((message) ->
								assertThat(message.getPayload())
										.extracting("body")
										.isEqualTo(person2))
						.assertNext((message) ->
								assertThat(message.getPayload())
										.extracting("body")
										.isEqualTo(person3))
						.thenCancel()
						.verifyLater();

		Flux.concat(this.mongoTemplate.insert(person1),
				this.mongoTemplate.insert(person2),
				this.mongoTemplate.insert(person3))
				.as(StepVerifier::create)
				.expectNextCount(3)
				.verifyComplete();

		stepVerifier.verify(Duration.ofSeconds(10));

		this.changeStreamMessageProducer.stop();
	}

	@Configuration
	@EnableIntegration
	static class Config {

		@Bean
		ReactiveMongoOperations mongoTemplate() {
			return new ReactiveMongoTemplate(mongoClient, "test");
		}

		@Bean
		IntegrationFlow changeStreamFlow() {
			return IntegrationFlows.from(
					MongoDb.changeStreamInboundChannelAdapter(mongoTemplate())
							.domainType(Person.class)
							.collection("person")
							.extractBody(false)
							.autoStartup(false)
							.shouldTrack(true))
					.channel(MessageChannels.flux())
					.get();
		}

	}

}
