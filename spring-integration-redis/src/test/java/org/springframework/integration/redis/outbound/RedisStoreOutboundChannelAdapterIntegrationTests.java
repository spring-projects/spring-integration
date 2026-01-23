/*
 * Copyright 2007-present the original author or authors.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.support.collections.DefaultRedisList;
import org.springframework.data.redis.support.collections.DefaultRedisMap;
import org.springframework.data.redis.support.collections.DefaultRedisSet;
import org.springframework.data.redis.support.collections.DefaultRedisZSet;
import org.springframework.data.redis.support.collections.RedisList;
import org.springframework.data.redis.support.collections.RedisMap;
import org.springframework.data.redis.support.collections.RedisProperties;
import org.springframework.data.redis.support.collections.RedisSet;
import org.springframework.data.redis.support.collections.RedisZSet;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.integration.redis.RedisContainerTest;
import org.springframework.integration.redis.support.RedisHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @author Artem Vozhdayenko
 * @author Glenn Renfro
 *
 * @since 2.2
 */
@SpringJUnitConfig
@DirtiesContext
class RedisStoreOutboundChannelAdapterIntegrationTests implements RedisContainerTest {

	private final StringRedisTemplate redisTemplate = new StringRedisTemplate();

	@Autowired
	private BeanFactory beanFactory;

	@Autowired
	@Qualifier("listWithKeyAsHeader")
	private MessageChannel listWithKeyAsHeaderChannel;

	@Autowired
	@Qualifier("listWithKeyProvided")
	private MessageChannel listWithKeyProvidedChannel;

	@Autowired
	@Qualifier("zset")
	private MessageChannel zsetChannel;

	@Autowired
	@Qualifier("mapToZset")
	private MessageChannel mapToZsetChannel;

	@Autowired
	@Qualifier("mapToMapA")
	private MessageChannel mapToMapAChannel;

	@Autowired
	@Qualifier("mapToMapB")
	private MessageChannel mapToMapBChannel;

	@Autowired
	@Qualifier("simpleMap")
	private MessageChannel simpleMapChannel;

	@Autowired
	@Qualifier("set")
	private MessageChannel setChannel;

	@Autowired
	@Qualifier("setNotParsed")
	private MessageChannel setNotParsedChannel;

	@Autowired
	@Qualifier("pojoIntoSet")
	private MessageChannel pojoIntoSetChannel;

	@Autowired
	@Qualifier("property")
	private MessageChannel propertyChannel;

	@Autowired
	@Qualifier("simpleProperty")
	private MessageChannel simplePropertyChannel;

	@Autowired
	private RedisConnectionFactory redisConnectionFactory;

	@BeforeEach
	@AfterEach
	public void setup() {
		this.redisTemplate.setConnectionFactory(redisConnectionFactory);
		this.redisTemplate.afterPropertiesSet();

		this.redisTemplate.delete("pepboys");
		this.redisTemplate.delete("foo");
		this.redisTemplate.delete("bar");
		this.redisTemplate.delete("presidents");
	}

	@Test
	void testListWithKeyAsHeader() {
		RedisList<String> redisList = new DefaultRedisList<String>("pepboys", this.redisTemplate);
		assertThat(redisList).isEmpty();

		List<String> pepboys = new ArrayList<String>();
		pepboys.add("Manny");
		pepboys.add("Moe");
		pepboys.add("Jack");
		Message<List<String>> message = MessageBuilder.withPayload(pepboys).setHeader(RedisHeaders.KEY, "pepboys").build();
		this.listWithKeyAsHeaderChannel.send(message);

		assertThat(redisList).hasSize(3);
	}

	@Test
	void testListWithKeyAsHeaderSimple() {
		redisTemplate.delete("foo");
		RedisList<String> redisList = new DefaultRedisList<String>("foo", this.redisTemplate);
		assertThat(redisList).isEmpty();

		Message<String> message = MessageBuilder.withPayload("bar").setHeader("redis_key", "foo").build();
		this.listWithKeyAsHeaderChannel.send(message);

		assertThat(redisList).hasSize(1);
	}

	@Test
	void testListWithProvidedKey() {
		RedisList<String> redisList = new DefaultRedisList<String>("pepboys", this.redisTemplate);
		assertThat(redisList).isEmpty();

		List<String> pepboys = new ArrayList<String>();
		pepboys.add("Manny");
		pepboys.add("Moe");
		pepboys.add("Jack");
		Message<List<String>> message = MessageBuilder.withPayload(pepboys).build();
		this.listWithKeyProvidedChannel.send(message);

		assertThat(redisList).hasSize(3);
	}

	@Test
	void testZsetSimplePayloadIncrement() {
		RedisZSet<String> redisZSet = new DefaultRedisZSet<String>("foo", this.redisTemplate);
		assertThat(redisZSet).isEmpty();

		Message<String> message = MessageBuilder.withPayload("bar")
				.setHeader(RedisHeaders.KEY, "foo")
				.setHeader(RedisHeaders.ZSET_INCREMENT_SCORE, true)
				.build();
		this.zsetChannel.send(message);

		assertThat(redisZSet).hasSize(1);
		assertThat(redisZSet.score("bar")).isEqualTo(Double.valueOf(1));

		this.zsetChannel.send(message);

		assertThat(redisZSet).hasSize(1);
		assertThat(redisZSet.score("bar")).isEqualTo(Double.valueOf(2));
	}

	@Test
	void testZsetSimplePayloadOverwrite() {
		RedisZSet<String> redisZSet = new DefaultRedisZSet<String>("foo", this.redisTemplate);
		assertThat(redisZSet).isEmpty();

		Message<String> message = MessageBuilder.withPayload("bar")
				.setHeader(RedisHeaders.KEY, "foo")
				.build();
		this.zsetChannel.send(message);

		assertThat(redisZSet).hasSize(1);
		assertThat(redisZSet.score("bar")).isEqualTo(Double.valueOf(1));

		this.zsetChannel.send(message);

		assertThat(redisZSet).hasSize(1);
		assertThat(redisZSet.score("bar")).isEqualTo(Double.valueOf(1));
	}

	@Test
	void testZsetSimplePayloadIncrementBy2() {
		RedisZSet<String> redisZSet = new DefaultRedisZSet<String>("foo", this.redisTemplate);
		assertThat(redisZSet).isEmpty();

		Message<String> message = MessageBuilder.withPayload("bar")
				.setHeader(RedisHeaders.KEY, "foo")
				.setHeader(RedisHeaders.ZSET_SCORE, 2)
				.setHeader(RedisHeaders.ZSET_INCREMENT_SCORE, true)
				.build();
		this.zsetChannel.send(message);

		assertThat(redisZSet).hasSize(1);
		assertThat(redisZSet.score("bar")).isEqualTo(Double.valueOf(2));

		this.zsetChannel.send(message);

		assertThat(redisZSet).hasSize(1);
		assertThat(redisZSet.score("bar")).isEqualTo(Double.valueOf(4));
	}

	@Test
	void testZsetSimplePayloadOverwriteWithHeaderScore() {
		RedisZSet<String> redisZSet = new DefaultRedisZSet<String>("foo", this.redisTemplate);
		assertThat(redisZSet).isEmpty();

		Message<String> message = MessageBuilder.withPayload("bar")
				.setHeader(RedisHeaders.KEY, "foo")
				.setHeader(RedisHeaders.ZSET_INCREMENT_SCORE, false)
				.setHeader(RedisHeaders.ZSET_SCORE, 2)
				.build();
		this.zsetChannel.send(message);

		assertThat(redisZSet).hasSize(1);
		assertThat(redisZSet.score("bar")).isEqualTo(Double.valueOf(2));

		this.zsetChannel.send(MessageBuilder.fromMessage(message).setHeader(RedisHeaders.ZSET_SCORE, 15).build());

		assertThat(redisZSet).hasSize(1);
		assertThat(redisZSet.score("bar")).isEqualTo(Double.valueOf(15));
	}

	@Test
	void testMapToZsetWithProvidedKey() {
		RedisZSet<String> redisZset = new DefaultRedisZSet<String>("presidents", this.redisTemplate);
		assertThat(redisZset).isEmpty();

		Map<String, Integer> presidents = new HashMap<String, Integer>();
		presidents.put("John Adams", 18);

		presidents.put("Barack Obama", 21);
		presidents.put("Thomas Jefferson", 19);
		presidents.put("John Quincy Adams", 19);
		presidents.put("Zachary Taylor", 19);

		Message<Map<String, Integer>> message = MessageBuilder.withPayload(presidents)
				.setHeader(RedisHeaders.ZSET_INCREMENT_SCORE, true)
				.build();

		this.mapToZsetChannel.send(message);

		assertThat(redisZset).hasSize(5);
		assertThat(redisZset.rangeByScore(18, 18)).hasSize(1);
		assertThat(redisZset.rangeByScore(18, 19)).hasSize(4);
		assertThat(redisZset.rangeByScore(21, 21)).hasSize(1);

		RedisStoreWritingMessageHandler handler =
				this.beanFactory.getBean("mapToZset.handler", RedisStoreWritingMessageHandler.class);
		assertThat(TestUtils.<String>getPropertyValue(handler, "keyExpression.expression"))
				.isEqualTo("'presidents'");

		this.mapToZsetChannel.send(message);

		assertThat(redisZset).hasSize(5);
		assertThat(redisZset.rangeByScore(36, 36)).hasSize(1);
		assertThat(redisZset.rangeByScore(36, 38)).hasSize(4);
		assertThat(redisZset.rangeByScore(42, 42)).hasSize(1);

		// test overwrite score behavior
		presidents.put("Barack Obama", 31);

		this.mapToZsetChannel.send(MessageBuilder.fromMessage(message)
				.setHeader(RedisHeaders.ZSET_INCREMENT_SCORE, false)
				.build());

		assertThat(redisZset).hasSize(5);
		assertThat(redisZset.rangeByScore(18, 18)).hasSize(1);
		assertThat(redisZset.rangeByScore(18, 19)).hasSize(4);
		assertThat(redisZset.rangeByScore(31, 31)).hasSize(1);
	}

	@Test
	void testMapToMapWithProvidedKey() {
		RedisMap<String, String> redisMap = new DefaultRedisMap<String, String>("pepboys", this.redisTemplate);

		assertThat(redisMap).isEmpty();

		Map<String, String> pepboys = new HashMap<String, String>();
		pepboys.put("1", "Manny");
		pepboys.put("2", "Moe");
		pepboys.put("3", "Jack");

		Message<Map<String, String>> message = MessageBuilder.withPayload(pepboys).build();
		this.mapToMapAChannel.send(message);
		assertThat(redisMap)
				.containsEntry("1", "Manny")
				.containsEntry("2", "Moe")
				.containsEntry("3", "Jack");

		RedisStoreWritingMessageHandler handler = this.beanFactory.getBean("mapToMapA.handler",
				RedisStoreWritingMessageHandler.class);
		assertThat(TestUtils.<LiteralExpression>getPropertyValue(handler, "keyExpression").getExpressionString())
				.isEqualTo("pepboys");
		assertThat(TestUtils.<SpelExpression>getPropertyValue(handler, "mapKeyExpression").getExpressionString())
				.isEqualTo("'foo'");
	}

	@Test
		// map key is not provided
	void testMapToMapAsSingleEntryWithKeyAsHeaderFail() {
		RedisMap<String, Map<String, String>> redisMap =
				new DefaultRedisMap<String, Map<String, String>>("pepboys", this.redisTemplate);

		assertThat(redisMap).isEmpty();

		Map<String, String> pepboys = new HashMap<String, String>();
		pepboys.put("1", "Manny");
		pepboys.put("2", "Moe");
		pepboys.put("3", "Jack");

		Message<Map<String, String>> message = MessageBuilder.withPayload(pepboys).
				setHeader(RedisHeaders.KEY, "pepboys").build();

		assertThatThrownBy(() -> this.mapToMapBChannel.send(message))
				.isInstanceOf(MessageHandlingException.class);
	}

	@Test
		// key is not provided
	void testMapToMapNoKey() {
		RedisTemplate<String, Map<String, Map<String, String>>> redisTemplate = new RedisTemplate<String, Map<String, Map<String, String>>>();
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setHashKeySerializer(new StringRedisSerializer());
		redisTemplate.setConnectionFactory(redisConnectionFactory);
		redisTemplate.afterPropertiesSet();

		RedisMap<String, Map<String, String>> redisMap =
				new DefaultRedisMap<String, Map<String, String>>("pepboys", redisTemplate);

		assertThat(redisMap).isEmpty();

		Map<String, String> pepboys = new HashMap<String, String>();
		pepboys.put("1", "Manny");
		pepboys.put("2", "Moe");
		pepboys.put("3", "Jack");

		Message<Map<String, String>> message = MessageBuilder.withPayload(pepboys).build();
		assertThatThrownBy(() -> this.mapToMapBChannel.send(message))
				.isInstanceOf(MessageHandlingException.class);
	}

	@Test
	void testMapToMapAsSingleEntryWithKeyAsHeader() {
		RedisTemplate<String, Map<String, Map<String, String>>> redisTemplate = new RedisTemplate<String, Map<String, Map<String, String>>>();
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setHashKeySerializer(new StringRedisSerializer());
		redisTemplate.setConnectionFactory(redisConnectionFactory);
		redisTemplate.afterPropertiesSet();

		RedisMap<String, Map<String, String>> redisMap =
				new DefaultRedisMap<String, Map<String, String>>("pepboys", redisTemplate);

		assertThat(redisMap).isEmpty();

		Map<String, String> pepboys = new HashMap<String, String>();
		pepboys.put("1", "Manny");
		pepboys.put("2", "Moe");
		pepboys.put("3", "Jack");

		Message<Map<String, String>> message = MessageBuilder.withPayload(pepboys).
				setHeader(RedisHeaders.KEY, "pepboys").setHeader(RedisHeaders.MAP_KEY, "foo").build();
		this.mapToMapBChannel.send(message);
		Map<String, String> pepboyz = redisMap.get("foo");

		assertThat(pepboyz)
				.containsEntry("1", "Manny")
				.containsEntry("2", "Moe")
				.containsEntry("3", "Jack");
	}

	@Test
	void testStoreSimpleStringInMap() {
		RedisMap<String, String> redisMap = new DefaultRedisMap<String, String>("bar", this.redisTemplate);

		assertThat(redisMap).isEmpty();

		Message<String> message = MessageBuilder.withPayload("hello, world!").
				setHeader(RedisHeaders.KEY, "bar").setHeader(RedisHeaders.MAP_KEY, "foo").build();

		this.simpleMapChannel.send(message);
		String hello = redisMap.get("foo");

		assertThat(hello).isEqualTo("hello, world!");
	}

	@Test
	void testSetWithKeyAsHeader() {
		RedisSet<String> redisSet = new DefaultRedisSet<String>("pepboys", this.redisTemplate);
		assertThat(redisSet).isEmpty();

		Set<String> pepboys = new HashSet<String>();
		pepboys.add("Manny");
		pepboys.add("Moe");
		pepboys.add("Jack");
		Message<Set<String>> message = MessageBuilder.withPayload(pepboys).setHeader("redis_key", "pepboys").build();
		this.setChannel.send(message);

		assertThat(redisSet).hasSize(3);
	}

	@Test
	void testSetWithKeyAsHeaderSimple() {
		RedisSet<String> redisSet = new DefaultRedisSet<String>("foo", this.redisTemplate);
		assertThat(redisSet).isEmpty();

		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader(RedisHeaders.KEY, "foo").build();
		this.setChannel.send(message);

		assertThat(redisSet).hasSize(1);
	}

	@Test
	void testSetWithKeyAsHeaderNotParsed() {
		RedisSet<String> redisSet = new DefaultRedisSet<String>("pepboys", this.redisTemplate);
		assertThat(redisSet).isEmpty();

		Set<String> pepboys = new HashSet<String>();
		pepboys.add("Manny");
		pepboys.add("Moe");
		pepboys.add("Jack");
		Message<Set<String>> message = MessageBuilder.withPayload(pepboys).setHeader("redis_key", "pepboys").build();
		this.setNotParsedChannel.send(message);

		assertThat(redisSet).hasSize(1);
	}

	@Test
	void testPojoIntoSet() {
		RedisSet<String> redisSet = new DefaultRedisSet<String>("pepboys", this.redisTemplate);
		assertThat(redisSet).isEmpty();

		String pepboy = "Manny";
		Message<String> message = MessageBuilder.withPayload(pepboy).setHeader("redis_key", "pepboys").build();
		this.pojoIntoSetChannel.send(message);

		assertThat(redisSet).hasSize(1);
	}

	@Test
	void testProperties() {
		RedisProperties redisProperties = new RedisProperties("pepboys", this.redisTemplate);

		assertThat(redisProperties).isEmpty();

		Properties pepboys = new Properties();
		pepboys.put("1", "Manny");
		pepboys.put("2", "Moe");
		pepboys.put("3", "Jack");

		Message<Properties> message = MessageBuilder.withPayload(pepboys).build();
		this.propertyChannel.send(message);

		assertThat(redisProperties)
				.containsEntry("1", "Manny")
				.containsEntry("2", "Moe")
				.containsEntry("3", "Jack");
	}

	@Test
	void testPropertiesSimple() {
		RedisProperties redisProperties = new RedisProperties("foo", this.redisTemplate);

		assertThat(redisProperties).isEmpty();

		Message<String> message = MessageBuilder.withPayload("bar")
				.setHeader(RedisHeaders.KEY, "foo")
				.setHeader("baz", "qux")
				.build();
		this.simplePropertyChannel.send(message);

		assertThat(redisProperties).containsEntry("qux", "bar");
	}

}
