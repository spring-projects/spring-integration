/*
 * Copyright 2020-present the original author or authors.
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

package org.springframework.integration.redis.outbound;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.handler.ReactiveMessageHandlerAdapter;
import org.springframework.integration.redis.RedisContainerTest;
import org.springframework.integration.redis.util.Address;
import org.springframework.integration.redis.util.Person;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Attoumane Ahamadi
 * @author Artem Bilan
 * @author Artem Vozhdayenko
 *
 * @since 5.4
 */
@SpringJUnitConfig
@DirtiesContext
class ReactiveRedisStreamMessageHandlerTests implements RedisContainerTest {

	private static final String STREAM_KEY = ReactiveRedisStreamMessageHandlerTests.class.getSimpleName() + ".stream";

	@Autowired
	@Qualifier("streamChannel")
	private MessageChannel messageChannel;

	@Autowired
	private ReactiveMessageHandlerAdapter handlerAdapter;

	@Autowired
	private ReactiveRedisConnectionFactory redisConnectionFactory;

	@BeforeEach
	void deleteStreamKey() {
		ReactiveRedisTemplate<String, String> template =
				new ReactiveRedisTemplate<>(redisConnectionFactory, RedisSerializationContext.string());
		template.delete(STREAM_KEY).block();
	}

	@Test
	void testIntegrationStreamOutbound() {
		String messagePayload = "Hello stream message";

		this.messageChannel.send(new GenericMessage<>(messagePayload));

		ReactiveRedisTemplate<String, ?> template =
				new ReactiveRedisTemplate<>(redisConnectionFactory, RedisSerializationContext.string());

		ObjectRecord<String, String> record =
				template.opsForStream()
						.read(String.class, StreamOffset.fromStart(STREAM_KEY))
						.blockFirst();

		assertThat(record.getStream()).isEqualTo(STREAM_KEY);

		assertThat(record.getValue()).isEqualTo(messagePayload);
	}

	@Test
	void testMessageWithListPayload() {
		List<String> messagePayload = Arrays.asList("Hello", "stream", "message");
		this.handlerAdapter.handleMessage(new GenericMessage<>(messagePayload));

		ReactiveRedisTemplate<String, ?> template = new ReactiveRedisTemplate<>(redisConnectionFactory,
				RedisSerializationContext.string());
		ObjectRecord<String, ?> record = template.opsForStream().read(List.class, StreamOffset.fromStart(STREAM_KEY))
				.blockFirst();

		assertThat(record.getStream()).isEqualTo(STREAM_KEY);
		assertThat(record.getValue()).isEqualTo(messagePayload);
	}

	@Test
	void testExplicitSerializationContextWithModel() {
		Address address = new Address("Rennes, France");
		Person person = new Person(address, "Attoumane");

		Message<?> message = new GenericMessage<>(person);

		this.handlerAdapter.handleMessage(message);

		ReactiveRedisTemplate<String, ?> template =
				new ReactiveRedisTemplate<>(redisConnectionFactory, RedisSerializationContext.string());

		ObjectRecord<String, Person> record =
				template.opsForStream()
						.read(Person.class, StreamOffset.fromStart(STREAM_KEY))
						.blockFirst();

		assertThat(record.getStream()).isEqualTo(STREAM_KEY);
		assertThat(record.getValue().getName()).isEqualTo("Attoumane");
		assertThat(record.getValue().getAddress().getAddress()).isEqualTo("Rennes, France");
	}

	@Configuration
	public static class ReactiveRedisStreamMessageHandlerTestsContext {

		@Bean
		ReactiveRedisConnectionFactory redisConnectionFactory() {
			return RedisContainerTest.connectionFactory();
		}

		@Bean
		public MessageChannel streamChannel(ReactiveMessageHandlerAdapter messageHandlerAdapter) {
			DirectChannel directChannel = new DirectChannel();
			directChannel.subscribe(messageHandlerAdapter);
			directChannel.setMaxSubscribers(1);
			return directChannel;
		}

		@Bean
		public ReactiveRedisStreamMessageHandler streamMessageHandler(ReactiveRedisConnectionFactory redisConnectionFactory) {

			return new ReactiveRedisStreamMessageHandler(redisConnectionFactory, STREAM_KEY);
		}

		@Bean
		public ReactiveMessageHandlerAdapter reactiveMessageHandlerAdapter(
				ReactiveRedisStreamMessageHandler streamMessageHandler) {

			return new ReactiveMessageHandlerAdapter(streamMessageHandler);
		}

	}

}
