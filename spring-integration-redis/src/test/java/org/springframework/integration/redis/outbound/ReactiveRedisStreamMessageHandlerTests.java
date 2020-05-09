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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.lang.reflect.Method;
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
import org.springframework.integration.redis.store.RedisMessageStoreTests;
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
@ContextConfiguration
@RunWith(SpringRunner.class)
@DirtiesContext
@SuppressWarnings({"unchecked", "rawtypes"})
public class ReactiveRedisStreamMessageHandlerTests extends RedisAvailableTests {

	@Autowired
	@Qualifier("forRedisStreamChannel")
	private MessageChannel messageChannel;

	@Autowired
	private ReactiveRedisConnectionFactory redisConnectionFactory;

	@Autowired
	private ReactiveMessageHandlerAdapter handlerAdapter;

	@Test
	@RedisAvailable
	public void emptyStreamKeyTest() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ReactiveRedisStreamMessageHandler("", null));
	}

	@Test
	@RedisAvailable
	public void nullConnectionFactoryTest() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ReactiveRedisStreamMessageHandler("stream", null));
	}


	@Test
	@RedisAvailable
	public void simpleStringInsertionTest() {
		String streamKey = "myStream";
		String messagePayload = "Bonjour à tous les confinés";

		handlerAdapter.handleMessage(new GenericMessage<>(messagePayload));

		RedisSerializationContext<String, String> serializationContext = redisStringOrJsonSerializationContext(true, null);

		ReactiveRedisTemplate<String, String> template = new ReactiveRedisTemplate(redisConnectionFactory, serializationContext);

		ObjectRecord<String, String> record = template.opsForStream().read(String.class, StreamOffset.fromStart(streamKey)).blockFirst();
		assertThat(record.getStream()).isEqualTo(streamKey);
		assertThat(record.getValue()).isEqualTo(messagePayload);
		template.delete(streamKey).block();
	}

	@Test
	@RedisAvailable
	public void integrationStreamOutboundTest() {
		String streamKey = "myStream";
		String messagePayload = "Bonjour à tous les confinés";

		messageChannel.send(new GenericMessage<>(messagePayload));

		RedisSerializationContext<String, String> serializationContext = redisStringOrJsonSerializationContext(true, null);

		ReactiveRedisTemplate<String, String> template = new ReactiveRedisTemplate(redisConnectionFactory, serializationContext);

		ObjectRecord<String, String> record = template.opsForStream().read(String.class, StreamOffset.fromStart(streamKey)).blockFirst();
		assertThat(record.getStream()).isEqualTo(streamKey);
		assertThat(record.getValue()).isEqualTo(messagePayload);
		template.delete(streamKey).block();
	}

	//TODO Find why the deserialization fail does not work
	/*@Test
	@RedisAvailable*/
	public void explicitJsonSerializationContextTest() {
		String streamKey = "myStream";
		List<String> messagePayload = Arrays.asList("Bonjour", "à", "tous", "les", "confinés");

		RedisSerializationContext<String, Object> jsonSerializationContext = redisStringOrJsonSerializationContext(false, List.class);

		ReactiveRedisStreamMessageHandler streamMessageHandler = new ReactiveRedisStreamMessageHandler(streamKey, redisConnectionFactory);
		streamMessageHandler.setSerializationContext(jsonSerializationContext);
		//initializes reactiveRedisStreamOperations
		invokeOnInitMethod(streamMessageHandler);

		ReactiveMessageHandlerAdapter handlerAdapter = new ReactiveMessageHandlerAdapter(streamMessageHandler);
		handlerAdapter.handleMessage(new GenericMessage<>(messagePayload));

		ReactiveRedisTemplate<String, String> template = new ReactiveRedisTemplate(redisConnectionFactory,
				jsonSerializationContext);
		ObjectRecord<String, List> record = template.opsForStream().read(List.class, StreamOffset.fromStart(streamKey))
				.blockFirst();

		assertThat(record.getStream()).isEqualTo(streamKey);
		assertThat(record.getValue()).isEqualTo("[\"Bonjour\", \"à\", \"tous\", \"les\", \"confinés\"]");
		template.delete(streamKey).block();
	}

	//TODO Find why the deserialization does not work
	/*@Test
	@RedisAvailable*/
	public void explicitJsonSerializationContextWithModelTest() {
		String streamKey = "myStream";

		RedisMessageStoreTests.Address address = new RedisMessageStoreTests.Address().withAddress("Rennes, France");
		RedisMessageStoreTests.Person person = new RedisMessageStoreTests.Person(address, "Attoumane");

		Message message = new GenericMessage(person);

		RedisSerializationContext<String, Object> jsonSerializationContext = redisStringOrJsonSerializationContext(false, RedisMessageStoreTests.Person.class);

		ReactiveRedisStreamMessageHandler streamMessageHandler = new ReactiveRedisStreamMessageHandler(streamKey, redisConnectionFactory);
		streamMessageHandler.setSerializationContext(jsonSerializationContext);
		//initializes reactiveRedisStreamOperations
		invokeOnInitMethod(streamMessageHandler);

		ReactiveMessageHandlerAdapter handlerAdapter = new ReactiveMessageHandlerAdapter(streamMessageHandler);
		handlerAdapter.handleMessage(message);

		ReactiveRedisTemplate<String, String> template = new ReactiveRedisTemplate(redisConnectionFactory, jsonSerializationContext);
		ObjectRecord<String, RedisMessageStoreTests.Person> record = template.opsForStream().read(RedisMessageStoreTests.Person.class, StreamOffset.fromStart(streamKey)).blockFirst();
		assertThat(record.getStream()).isEqualTo(streamKey);
		assertThat(record.getValue().getName()).isEqualTo("Attoumane");
		assertThat(record.getValue().getAddress().getAddress()).isEqualTo("Rennes, France");
		template.delete(streamKey).block();
	}


	private RedisSerializationContext redisStringOrJsonSerializationContext(boolean string, Class jsonTargetType) {

		RedisSerializationContext redisSerializationContext;
		RedisSerializer jsonSerializer = null;

		if (jsonTargetType != null) {
			jsonSerializer = new Jackson2JsonRedisSerializer(jsonTargetType);
		}
		RedisSerializer stringSerializer = StringRedisSerializer.UTF_8;
		RedisSerializationContext.SerializationPair stringSerializerPair = RedisSerializationContext.SerializationPair
				.fromSerializer(stringSerializer);

		if (string) {
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
					.key(stringSerializerPair)
					.value(jsonSerializer)
					.hashKey(jsonSerializer)
					.hashValue(jsonSerializer)
					.build();
		}

		return redisSerializationContext;
	}

	private void invokeOnInitMethod(ReactiveRedisStreamMessageHandler streamMessageHandler) {
		try {
			Method onInit = ReactiveRedisStreamMessageHandler.class.getDeclaredMethod("onInit");
			onInit.setAccessible(true);
			onInit.invoke(streamMessageHandler);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
