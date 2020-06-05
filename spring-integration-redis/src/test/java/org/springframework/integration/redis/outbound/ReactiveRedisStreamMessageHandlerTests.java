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

package org.springframework.integration.redis.outbound;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.integration.handler.ReactiveMessageHandlerAdapter;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;
import org.springframework.integration.redis.util.Address;
import org.springframework.integration.redis.util.Person;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Attoumane Ahamadi
 *
 * @since 5.4
 */
@ContextConfiguration(classes = ReactiveRedisStreamMessageHandlerTestsContext.class)
@RunWith(SpringRunner.class)
@DirtiesContext
@SuppressWarnings({"unchecked", "rawtypes"})
public class ReactiveRedisStreamMessageHandlerTests extends RedisAvailableTests {

	@Autowired
	@Qualifier("streamChannel")
	private MessageChannel messageChannel;

	@Autowired
	private ReactiveRedisConnectionFactory redisConnectionFactory;

	@Autowired
	private ReactiveMessageHandlerAdapter handlerAdapter;

	@Autowired
	private ReactiveRedisStreamMessageHandler streamMessageHandler;

	@Test
	@RedisAvailable
	public void integrationStreamOutboundTest() {
		String messagePayload = "Hello stream message";

		messageChannel.send(new GenericMessage<>(messagePayload));

		RedisSerializationContext<String, String> serializationContext = redisStringOrJsonSerializationContext(null);

		ReactiveRedisTemplate<String, String> template = new ReactiveRedisTemplate(redisConnectionFactory, serializationContext);

		ObjectRecord<String, String> record = template.opsForStream().read(String.class, StreamOffset.fromStart(ReactiveRedisStreamMessageHandlerTestsContext.STREAM_KEY)).blockFirst();

		assertThat(record.getStream()).isEqualTo(ReactiveRedisStreamMessageHandlerTestsContext.STREAM_KEY);
		assertThat(record.getValue()).isEqualTo(messagePayload);

		template.delete(ReactiveRedisStreamMessageHandlerTestsContext.STREAM_KEY).block();
	}

	//TODO Find why the deserialization fail does not work
	/*@Test
	@RedisAvailable*/
	public void explicitJsonSerializationContextTest() {
		List<String> messagePayload = Arrays.asList("Hello", "stream", "message");

		RedisSerializationContext<String, Object> jsonSerializationContext = redisStringOrJsonSerializationContext(List.class);

		streamMessageHandler.setSerializationContext(jsonSerializationContext);

		handlerAdapter.handleMessage(new GenericMessage<>(messagePayload));

		ReactiveRedisTemplate<String, List> template = new ReactiveRedisTemplate(redisConnectionFactory,
				jsonSerializationContext);

		ObjectRecord<String, List> record = template.opsForStream().read(List.class, StreamOffset.fromStart(ReactiveRedisStreamMessageHandlerTestsContext.STREAM_KEY))
				.blockFirst();

		assertThat(record.getStream()).isEqualTo(ReactiveRedisStreamMessageHandlerTestsContext.STREAM_KEY);
		assertThat(record.getValue()).isEqualTo("[\"Hello\", \"stream\", \"message\"]");

		template.delete(ReactiveRedisStreamMessageHandlerTestsContext.STREAM_KEY).block();
	}

	//TODO Find why the deserialization does not work
	/*@Test
	@RedisAvailable*/
	public void explicitJsonSerializationContextWithModelTest() {
		Address address = new Address().withAddress("Rennes, France");
		Person person = new Person(address, "Attoumane");

		Message message = new GenericMessage(person);

		RedisSerializationContext<String, Object> jsonSerializationContext = redisStringOrJsonSerializationContext(Person.class);

		streamMessageHandler.setSerializationContext(jsonSerializationContext);

		handlerAdapter.handleMessage(message);

		ReactiveRedisTemplate<String, Person> template = new ReactiveRedisTemplate(redisConnectionFactory, jsonSerializationContext);

		ObjectRecord<String, Person> record = template.opsForStream().read(Person.class, StreamOffset.fromStart(ReactiveRedisStreamMessageHandlerTestsContext.STREAM_KEY)).blockFirst();

		assertThat(record.getStream()).isEqualTo(ReactiveRedisStreamMessageHandlerTestsContext.STREAM_KEY);
		assertThat(record.getValue().getName()).isEqualTo("Attoumane");
		assertThat(record.getValue().getAddress().getAddress()).isEqualTo("Rennes, France");

		template.delete(ReactiveRedisStreamMessageHandlerTestsContext.STREAM_KEY).block();
	}


	private RedisSerializationContext redisStringOrJsonSerializationContext(Class<?> jsonTargetType) {

		RedisSerializationContext redisSerializationContext;
		RedisSerializer jsonSerializer = null;

		if (jsonTargetType != null) {
			jsonSerializer = new Jackson2JsonRedisSerializer(jsonTargetType);
		}
		RedisSerializer stringSerializer = StringRedisSerializer.UTF_8;
		RedisSerializationContext.SerializationPair stringSerializerPair = RedisSerializationContext.SerializationPair
				.fromSerializer(stringSerializer);

		if (jsonTargetType == null) {
			redisSerializationContext = RedisSerializationContext
					.newSerializationContext()
					.key(stringSerializerPair)
					.value(stringSerializer)
					.hashKey(stringSerializer)
					.hashValue(stringSerializer)
					.build();
		}
		else {
			redisSerializationContext = RedisSerializationContext
					.newSerializationContext()
					.key(jsonSerializer)
					.value(jsonSerializer)
					.hashKey(jsonSerializer)
					.hashValue(jsonSerializer)
					.build();
		}

		return redisSerializationContext;
	}

}
