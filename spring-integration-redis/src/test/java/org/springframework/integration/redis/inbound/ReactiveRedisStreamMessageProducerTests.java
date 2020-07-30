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

package org.springframework.integration.redis.inbound;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.stream.StreamInfo;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.handler.ReactiveMessageHandlerAdapter;
import org.springframework.integration.redis.outbound.ReactiveRedisStreamMessageHandler;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableRule;
import org.springframework.integration.redis.rules.RedisAvailableTests;
import org.springframework.integration.redis.util.Address;
import org.springframework.integration.redis.util.Person;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * @author Attoumane Ahamadi
 *
 * @since 5.4
 */
@DirtiesContext
@RunWith(SpringRunner.class)
public class ReactiveRedisStreamMessageProducerTests extends RedisAvailableTests {

	private static final String STREAM_KEY = "myStream";

	private static final String CONSUMER = "consumer";

	@Autowired
	FluxMessageChannel fluxMessageChannel;

	@Autowired
	ReactiveRedisStreamMessageProducer redisStreamMessageProducer;

	@Autowired
	ReactiveRedisTemplate<String, ?> template;

	@Autowired
	ReactiveMessageHandlerAdapter messageHandler;

	@Before
	public void delKey() {
		this.template.delete(STREAM_KEY).subscribe();
	}

	@AfterEach
	public void tearDown() {
		this.redisStreamMessageProducer.stop();
	}

	@Test
	@RedisAvailable
	public void consumerGroupCreationTest() {
		this.redisStreamMessageProducer.setCreateConsumerGroup(true);
		this.redisStreamMessageProducer.setConsumerName(CONSUMER);
		this.redisStreamMessageProducer.afterPropertiesSet();
		this.redisStreamMessageProducer.doStart();

		StreamInfo.XInfoGroup infoGroup = this.template.opsForStream().groups(STREAM_KEY).blockFirst();
		assertThat(infoGroup).isNotNull();
		assertThat(infoGroup.groupName()).isEqualTo(this.redisStreamMessageProducer.getBeanName());
	}

	//TODO find why the execution of this test turns into an infinite loop
	/*@Test
	@RedisAvailable*/
	public void readingMessageAsStandaloneClientTest() {
		Address address = new Address("Rennes, France");
		Person person = new Person(address, "Attoumane");
		this.messageHandler.handleMessage(new GenericMessage<>(person));

		this.redisStreamMessageProducer.start();

		Flux.from(this.fluxMessageChannel)
				.as(StepVerifier::create)
				.assertNext(message -> {
					assertThat(message.getPayload()).isInstanceOf(Person.class)
							.extracting("body")
							.isEqualTo(person);
				}).thenCancel()
				.verify();
	}

	@Test
	@RedisAvailable
	public void readingMessageAsConsumerInConsumerGroupTest() {
		//TODO find why the test above does not execute before implementing this one
	}

	@Configuration
	@EnableIntegration
	static class ContextConfig {

		@Bean
		ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
			return RedisAvailableRule.connectionFactory;
		}

		@Bean
		ReactiveRedisStreamMessageHandler redisStreamMessageHandler(ReactiveRedisConnectionFactory connectionFactory) {
			return new ReactiveRedisStreamMessageHandler(connectionFactory, STREAM_KEY);
		}

		@Bean
		public ReactiveMessageHandlerAdapter reactiveMessageHandlerAdapter(ReactiveRedisStreamMessageHandler
				streamMessageHandler) {
			return new ReactiveMessageHandlerAdapter(streamMessageHandler);
		}

		@Bean
		ReactiveRedisTemplate<String, ?> reactiveStreamOperations(ReactiveRedisConnectionFactory
				connectionFactory) {
			return new ReactiveRedisTemplate<>(connectionFactory, RedisSerializationContext.string());
		}

		@Bean
		ReactiveRedisStreamMessageProducer reactiveRedisStreamProducer(ReactiveRedisConnectionFactory connectionFactory) {
			return new ReactiveRedisStreamMessageProducer(connectionFactory, STREAM_KEY);
		}

		@Bean
		IntegrationFlow redisStreamFlow(MessageProducerSupport messageProducer) {
			return IntegrationFlows.
					from(messageProducer)
					.channel(MessageChannels.flux())
					.get();
		}
	}
}
