/*
 * Copyright 2002-2024 the original author or authors.
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.support.collections.DefaultRedisList;
import org.springframework.data.redis.support.collections.DefaultRedisZSet;
import org.springframework.data.redis.support.collections.RedisCollectionFactoryBean.CollectionType;
import org.springframework.data.redis.support.collections.RedisList;
import org.springframework.data.redis.support.collections.RedisZSet;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.redis.RedisContainerTest;
import org.springframework.integration.redis.support.RedisHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @author Artem Vozhdayenko
 */
class RedisStoreWritingMessageHandlerTests implements RedisContainerTest {

	private static RedisConnectionFactory redisConnectionFactory;

	@BeforeAll
	static void setupConnection() {
		redisConnectionFactory = RedisContainerTest.connectionFactory();
	}

	@Test
	void testListWithListPayloadParsedAndProvidedKey() {
		RedisContainerTest.deleteKey(redisConnectionFactory, "foo");
		String key = "foo";
		RedisList<String> redisList =
				new DefaultRedisList<>(key, this.initTemplate(redisConnectionFactory, new StringRedisTemplate()));

		assertThat(redisList).isEmpty();

		RedisStoreWritingMessageHandler handler =
				new RedisStoreWritingMessageHandler(redisConnectionFactory);
		handler.setKey(key);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		List<String> list = new ArrayList<>();
		list.add("Manny");
		list.add("Moe");
		list.add("Jack");
		Message<List<String>> message = new GenericMessage<>(list);
		handler.handleMessage(message);

		assertThat(redisList).hasSize(3);
		assertThat(redisList.get(0)).isEqualTo("Manny");
		assertThat(redisList.get(1)).isEqualTo("Moe");
		assertThat(redisList.get(2)).isEqualTo("Jack");
		RedisContainerTest.deleteKey(redisConnectionFactory, "foo");
	}

	@Test
	void testListWithListPayloadParsedAndProvidedKeyAsHeader() {
		RedisContainerTest.deleteKey(redisConnectionFactory, "foo");
		String key = "foo";
		RedisList<String> redisList =
				new DefaultRedisList<>(key, this.initTemplate(redisConnectionFactory, new StringRedisTemplate()));

		assertThat(redisList).isEmpty();

		RedisStoreWritingMessageHandler handler =
				new RedisStoreWritingMessageHandler(redisConnectionFactory);

		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		List<String> list = new ArrayList<>();
		list.add("Manny");
		list.add("Moe");
		list.add("Jack");
		Message<List<String>> message = MessageBuilder.withPayload(list).setHeader("redis_key", key).build();
		handler.handleMessage(message);

		assertThat(redisList).hasSize(3);
		assertThat(redisList.get(0)).isEqualTo("Manny");
		assertThat(redisList.get(1)).isEqualTo("Moe");
		assertThat(redisList.get(2)).isEqualTo("Jack");
		RedisContainerTest.deleteKey(redisConnectionFactory, "foo");
	}

	@Test
	void testListWithListPayloadParsedAndNoKey() {
		RedisContainerTest.deleteKey(redisConnectionFactory, "foo");
		String key = "foo";
		RedisList<String> redisList =
				new DefaultRedisList<>(key, this.initTemplate(redisConnectionFactory, new RedisTemplate<>()));

		assertThat(redisList).isEmpty();

		RedisStoreWritingMessageHandler handler =
				new RedisStoreWritingMessageHandler(redisConnectionFactory);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		List<String> list = new ArrayList<>();
		list.add("Manny");
		list.add("Moe");
		list.add("Jack");
		Message<List<String>> message = MessageBuilder.withPayload(list).build();
		assertThatThrownBy(() -> handler.handleMessage(message)).isInstanceOf(MessageHandlingException.class);
		RedisContainerTest.deleteKey(redisConnectionFactory, "foo");
	}

	@Test
	void testListWithListPayloadAsSingleEntry() {
		RedisContainerTest.deleteKey(redisConnectionFactory, "foo");
		String key = "foo";
		RedisList<List<String>> redisList =
				new DefaultRedisList<>(key, this.initTemplate(redisConnectionFactory, new RedisTemplate<>()));

		assertThat(redisList).isEmpty();

		RedisTemplate<String, List<String>> template = this.initTemplate(redisConnectionFactory, new RedisTemplate<>());
		RedisStoreWritingMessageHandler handler =
				new RedisStoreWritingMessageHandler(template);
		handler.setKey(key);
		handler.setExtractPayloadElements(false);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		List<String> list = new ArrayList<>();
		list.add("Manny");
		list.add("Moe");
		list.add("Jack");
		Message<List<String>> message = new GenericMessage<>(list);
		handler.handleMessage(message);

		assertThat(redisList).hasSize(1);
		List<String> resultList = redisList.get(0);
		assertThat(resultList.get(0)).isEqualTo("Manny");
		assertThat(resultList.get(1)).isEqualTo("Moe");
		assertThat(resultList.get(2)).isEqualTo("Jack");
		RedisContainerTest.deleteKey(redisConnectionFactory, "foo");
	}

	@Test
	void testZsetWithListPayloadParsedAndProvidedKeyDefault() {
		RedisContainerTest.deleteKey(redisConnectionFactory, "foo");
		String key = "foo";
		RedisZSet<String> redisZset =
				new DefaultRedisZSet<>(key, this.initTemplate(redisConnectionFactory, new StringRedisTemplate()));

		assertThat(redisZset).isEmpty();

		RedisStoreWritingMessageHandler handler =
				new RedisStoreWritingMessageHandler(redisConnectionFactory);
		handler.setKey(key);
		handler.setCollectionType(CollectionType.ZSET);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		List<String> list = new ArrayList<>();
		list.add("Manny");
		list.add("Moe");
		list.add("Jack");
		Message<List<String>> message = MessageBuilder.withPayload(list)
				.setHeader(RedisHeaders.ZSET_INCREMENT_SCORE, true)
				.build();
		handler.handleMessage(message);

		assertThat(redisZset).hasSize(3);
		Set<TypedTuple<String>> pepboys = redisZset.rangeByScoreWithScores(1, 1);
		for (TypedTuple<String> pepboy : pepboys) {
			assertThat(pepboy.getScore()).isEqualTo(1);
		}

		handler.handleMessage(message);
		assertThat(redisZset).hasSize(3);
		pepboys = redisZset.rangeByScoreWithScores(1, 2);
		// should have incremented by 1
		for (TypedTuple<String> pepboy : pepboys) {
			assertThat(pepboy.getScore()).isEqualTo(Double.valueOf(2));
		}
		RedisContainerTest.deleteKey(redisConnectionFactory, "foo");
	}

	@Test
	void testZsetWithListPayloadParsedAndProvidedKeyScoreIncrement() {
		RedisContainerTest.deleteKey(redisConnectionFactory, "foo");
		String key = "foo";
		RedisZSet<String> redisZset =
				new DefaultRedisZSet<>(key, this.initTemplate(redisConnectionFactory, new StringRedisTemplate()));

		assertThat(redisZset).isEmpty();

		RedisStoreWritingMessageHandler handler =
				new RedisStoreWritingMessageHandler(redisConnectionFactory);
		handler.setKey(key);
		handler.setCollectionType(CollectionType.ZSET);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		List<String> list = new ArrayList<>();
		list.add("Manny");
		list.add("Moe");
		list.add("Jack");
		Message<List<String>> message = MessageBuilder.withPayload(list)
				.setHeader(RedisHeaders.ZSET_INCREMENT_SCORE, Boolean.TRUE)
				.build();

		handler.handleMessage(message);

		assertThat(redisZset).hasSize(3);
		Set<TypedTuple<String>> pepboys = redisZset.rangeByScoreWithScores(1, 1);
		for (TypedTuple<String> pepboy : pepboys) {
			assertThat(pepboy.getScore()).isEqualTo(1);
		}

		handler.handleMessage(message);
		assertThat(redisZset).hasSize(3);
		pepboys = redisZset.rangeByScoreWithScores(1, 2);
		// should have incremented
		for (TypedTuple<String> pepboy : pepboys) {
			assertThat(pepboy.getScore()).isEqualTo(2);
		}
		RedisContainerTest.deleteKey(redisConnectionFactory, "foo");
	}

	@Test
	void testZsetWithListPayloadParsedAndProvidedKeyScoreIncrementAsStringHeader() { // see INT-2775
		RedisContainerTest.deleteKey(redisConnectionFactory, "foo");
		String key = "foo";
		RedisZSet<String> redisZset =
				new DefaultRedisZSet<>(key, this.initTemplate(redisConnectionFactory, new StringRedisTemplate()));

		assertThat(redisZset).isEmpty();

		RedisStoreWritingMessageHandler handler =
				new RedisStoreWritingMessageHandler(redisConnectionFactory);
		handler.setKey(key);
		handler.setCollectionType(CollectionType.ZSET);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		List<String> list = new ArrayList<>();
		list.add("Manny");
		list.add("Moe");
		list.add("Jack");
		Message<List<String>> message = MessageBuilder.withPayload(list)
				.setHeader(RedisHeaders.ZSET_INCREMENT_SCORE, "true")
				.build();

		handler.handleMessage(message);

		assertThat(redisZset).hasSize(3);
		Set<TypedTuple<String>> pepboys = redisZset.rangeByScoreWithScores(1, 1);
		for (TypedTuple<String> pepboy : pepboys) {
			assertThat(pepboy.getScore()).isEqualTo(1);
		}

		handler.handleMessage(message);
		assertThat(redisZset).hasSize(3);
		pepboys = redisZset.rangeByScoreWithScores(1, 2);
		// should have incremented
		for (TypedTuple<String> pepboy : pepboys) {
			assertThat(pepboy.getScore()).isEqualTo(2);
		}
		RedisContainerTest.deleteKey(redisConnectionFactory, "foo");
	}

	@Test
	void testZsetWithListPayloadAsSingleEntryAndHeaderKeyHeaderScore() {
		RedisContainerTest.deleteKey(redisConnectionFactory, "foo");
		String key = "foo";
		RedisZSet<List<String>> redisZset =
				new DefaultRedisZSet<>(key, this.initTemplate(redisConnectionFactory, new RedisTemplate<>()));

		assertThat(redisZset).isEmpty();

		RedisTemplate<String, List<String>> template = this.initTemplate(redisConnectionFactory, new RedisTemplate<>());
		RedisStoreWritingMessageHandler handler =
				new RedisStoreWritingMessageHandler(template);

		handler.setCollectionType(CollectionType.ZSET);
		handler.setExtractPayloadElements(false);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		List<String> list = new ArrayList<>();
		list.add("Manny");
		list.add("Moe");
		list.add("Jack");
		Message<List<String>> message = MessageBuilder.withPayload(list).setHeader("redis_key", key).
				setHeader("redis_zsetScore", 4).build();
		handler.handleMessage(message);

		assertThat(redisZset).hasSize(1);
		Set<TypedTuple<List<String>>> entries = redisZset.rangeByScoreWithScores(1, 4);
		for (TypedTuple<List<String>> pepboys : entries) {
			assertThat(pepboys.getScore()).isEqualTo(4);
		}
		RedisContainerTest.deleteKey(redisConnectionFactory, "foo");
	}

	@Test
	void testZsetWithMapPayloadParsedHeaderKey() {
		RedisContainerTest.deletePresidents(redisConnectionFactory);
		String key = "presidents";
		RedisZSet<String> redisZset =
				new DefaultRedisZSet<>(key, this.initTemplate(redisConnectionFactory, new StringRedisTemplate()));

		assertThat(redisZset).isEmpty();

		RedisStoreWritingMessageHandler handler =
				new RedisStoreWritingMessageHandler(redisConnectionFactory);
		handler.setKey(key);
		handler.setCollectionType(CollectionType.ZSET);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		Map<String, Double> presidents = new HashMap<>();
		presidents.put("John Adams", 18D);

		presidents.put("Barack Obama", 21D);
		presidents.put("Thomas Jefferson", 19D);
		presidents.put("John Quincy Adams", 19D);
		presidents.put("Zachary Taylor", 19D);

		presidents.put("Theodore Roosevelt", 20D);
		presidents.put("Woodrow Wilson", 20D);
		presidents.put("George W. Bush", 21D);
		presidents.put("Franklin D. Roosevelt", 20D);
		presidents.put("Ronald Reagan", 20D);
		presidents.put("William J. Clinton", 20D);
		presidents.put("Abraham Lincoln", 19D);
		presidents.put("George Washington", 18D);

		Message<Map<String, Double>> message = MessageBuilder.withPayload(presidents).setHeader("redis_key", key).build();
		handler.handleMessage(message);

		assertThat(redisZset).hasSize(13);

		Set<TypedTuple<String>> entries = redisZset.rangeByScoreWithScores(18, 19);
		assertThat(entries).hasSize(6);
		RedisContainerTest.deletePresidents(redisConnectionFactory);
	}

	@Test
	void testZsetWithMapPayloadPojoParsedHeaderKey() {
		RedisContainerTest.deletePresidents(redisConnectionFactory);
		String key = "presidents";
		RedisZSet<President> redisZset =
				new DefaultRedisZSet<>(key, this.initTemplate(redisConnectionFactory, new RedisTemplate<>()));

		assertThat(redisZset).isEmpty();

		RedisTemplate<String, President> template = this.initTemplate(redisConnectionFactory, new RedisTemplate<>());
		RedisStoreWritingMessageHandler handler =
				new RedisStoreWritingMessageHandler(template);
		handler.setKey(key);
		handler.setCollectionType(CollectionType.ZSET);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		Map<President, Double> presidents = new HashMap<>();
		presidents.put(new President("John Adams"), 18D);

		presidents.put(new President("Barack Obama"), 21D);
		presidents.put(new President("Thomas Jefferson"), 19D);
		presidents.put(new President("John Quincy Adams"), 19D);
		presidents.put(new President("Zachary Taylor"), 19D);

		presidents.put(new President("Theodore Roosevelt"), 20D);
		presidents.put(new President("Woodrow Wilson"), 20D);
		presidents.put(new President("George W. Bush"), 21D);
		presidents.put(new President("Franklin D. Roosevelt"), 20D);
		presidents.put(new President("Ronald Reagan"), 20D);
		presidents.put(new President("William J. Clinton"), 20D);
		presidents.put(new President("Abraham Lincoln"), 19D);
		presidents.put(new President("George Washington"), 18D);

		Message<Map<President, Double>> message = MessageBuilder.withPayload(presidents).setHeader("redis_key", key).build();
		handler.handleMessage(message);

		assertThat(redisZset).hasSize(13);

		Set<TypedTuple<President>> entries = redisZset.rangeByScoreWithScores(18, 19);
		assertThat(entries).hasSize(6);
		RedisContainerTest.deletePresidents(redisConnectionFactory);
	}

	@Test
	void testZsetWithMapPayloadPojoAsSingleEntryHeaderKey() {
		RedisContainerTest.deletePresidents(redisConnectionFactory);
		String key = "presidents";
		RedisZSet<Map<President, Double>> redisZset =
				new DefaultRedisZSet<>(key, this.initTemplate(redisConnectionFactory, new RedisTemplate<>()));

		assertThat(redisZset).isEmpty();

		RedisTemplate<String, Map<President, Double>> template = this.initTemplate(redisConnectionFactory, new RedisTemplate<>());
		RedisStoreWritingMessageHandler handler =
				new RedisStoreWritingMessageHandler(template);
		handler.setKey(key);
		handler.setCollectionType(CollectionType.ZSET);
		handler.setExtractPayloadElements(false);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		Map<President, Double> presidents = new HashMap<>();
		presidents.put(new President("John Adams"), 18D);

		presidents.put(new President("Barack Obama"), 21D);
		presidents.put(new President("Thomas Jefferson"), 19D);

		Message<Map<President, Double>> message = MessageBuilder.withPayload(presidents).setHeader("redis_key", key).build();
		handler.handleMessage(message);

		assertThat(redisZset).hasSize(1);
		RedisContainerTest.deletePresidents(redisConnectionFactory);
	}

	@Test
	void testListWithMapKeyExpression() {
		String key = "foo";
		RedisStoreWritingMessageHandler handler =
				new RedisStoreWritingMessageHandler(redisConnectionFactory);
		handler.setKey(key);
		handler.setMapKeyExpression(new LiteralExpression(key));
		handler.setBeanFactory(mock(BeanFactory.class));
		assertThatThrownBy(handler::afterPropertiesSet).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void testSetWithMapKeyExpression() {
		String key = "foo";
		RedisStoreWritingMessageHandler handler =
				new RedisStoreWritingMessageHandler(redisConnectionFactory);
		handler.setKey(key);
		handler.setCollectionType(CollectionType.SET);
		handler.setMapKeyExpression(new LiteralExpression(key));
		handler.setBeanFactory(mock(BeanFactory.class));
		assertThatThrownBy(handler::afterPropertiesSet).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void testZsetWithMapKeyExpression() {
		String key = "foo";
		RedisStoreWritingMessageHandler handler =
				new RedisStoreWritingMessageHandler(redisConnectionFactory);
		handler.setKey(key);
		handler.setCollectionType(CollectionType.ZSET);
		handler.setMapKeyExpression(new LiteralExpression(key));
		handler.setBeanFactory(mock(BeanFactory.class));
		assertThatThrownBy(handler::afterPropertiesSet).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void testMapWithMapKeyExpression() {
		RedisContainerTest.deleteKey(redisConnectionFactory, "foo");
		String key = "foo";
		RedisStoreWritingMessageHandler handler =
				new RedisStoreWritingMessageHandler(redisConnectionFactory);
		handler.setKey(key);
		handler.setCollectionType(CollectionType.MAP);
		handler.setMapKeyExpression(new LiteralExpression(key));
		handler.setBeanFactory(mock(BeanFactory.class));
		try {
			handler.afterPropertiesSet();
		}
		catch (Exception e) {
			fail("No exception expected:" + e.getMessage());
		}
		RedisContainerTest.deleteKey(redisConnectionFactory, "foo");
	}

	@Test
	void testPropertiesWithMapKeyExpression() {
		RedisContainerTest.deleteKey(redisConnectionFactory, "foo");
		String key = "foo";
		RedisStoreWritingMessageHandler handler =
				new RedisStoreWritingMessageHandler(redisConnectionFactory);
		handler.setKey(key);
		handler.setCollectionType(CollectionType.PROPERTIES);
		handler.setMapKeyExpression(new LiteralExpression(key));
		handler.setBeanFactory(mock(BeanFactory.class));
		try {
			handler.afterPropertiesSet();
		}
		catch (Exception e) {
			fail("No exception expected:" + e.getMessage());
		}
		RedisContainerTest.deleteKey(redisConnectionFactory, "foo");
	}

	private <K, V> RedisTemplate<K, V> initTemplate(RedisConnectionFactory rcf, RedisTemplate<K, V> redisTemplate) {
		redisTemplate.setConnectionFactory(rcf);
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.afterPropertiesSet();
		return redisTemplate;
	}

	private static class President implements Serializable {

		private static final long serialVersionUID = 1L;

		private String name;

		President(String name) {
			this.name = name;
		}

		@SuppressWarnings("unused")
		public String getName() {
			return name;
		}

		@SuppressWarnings("unused")
		public void setName(String name) {
			this.name = name;
		}

	}

}
