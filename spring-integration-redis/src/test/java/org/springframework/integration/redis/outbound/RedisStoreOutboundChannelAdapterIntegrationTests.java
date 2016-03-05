/*
 * Copyright 2007-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.redis.outbound;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

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
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;
import org.springframework.integration.redis.support.RedisHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.2
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class RedisStoreOutboundChannelAdapterIntegrationTests extends RedisAvailableTests {

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

	@Before
	@After
	public void setup() {
		RedisConnectionFactory jcf = getConnectionFactoryForTest();
		this.redisTemplate.setConnectionFactory(jcf);
		this.redisTemplate.afterPropertiesSet();

		this.redisTemplate.delete("pepboys");
		this.redisTemplate.delete("foo");
		this.redisTemplate.delete("bar");
		this.redisTemplate.delete("presidents");
	}

	@Test
	@RedisAvailable
	public void testListWithKeyAsHeader() {
		RedisList<String> redisList = new DefaultRedisList<String>("pepboys", this.redisTemplate);
		assertEquals(0, redisList.size());

		List<String> pepboys = new ArrayList<String>();
		pepboys.add("Manny");
		pepboys.add("Moe");
		pepboys.add("Jack");
		Message<List<String>> message = MessageBuilder.withPayload(pepboys).setHeader(RedisHeaders.KEY, "pepboys").build();
		this.listWithKeyAsHeaderChannel.send(message);

		assertEquals(3, redisList.size());
	}

	@Test
	@RedisAvailable
	public void testListWithKeyAsHeaderSimple() {
		redisTemplate.delete("foo");
		RedisList<String> redisList = new DefaultRedisList<String>("foo", this.redisTemplate);
		assertEquals(0, redisList.size());

		Message<String> message = MessageBuilder.withPayload("bar").setHeader("redis_key", "foo").build();
		this.listWithKeyAsHeaderChannel.send(message);

		assertEquals(1, redisList.size());
	}

	@Test
	@RedisAvailable
	public void testListWithProvidedKey() {
		RedisList<String> redisList = new DefaultRedisList<String>("pepboys", this.redisTemplate);
		assertEquals(0, redisList.size());

		List<String> pepboys = new ArrayList<String>();
		pepboys.add("Manny");
		pepboys.add("Moe");
		pepboys.add("Jack");
		Message<List<String>> message = MessageBuilder.withPayload(pepboys).build();
		this.listWithKeyProvidedChannel.send(message);

		assertEquals(3, redisList.size());
	}

	@Test
	@RedisAvailable
	public void testZsetSimplePayloadIncrement() {
		RedisZSet<String> redisZSet = new DefaultRedisZSet<String>("foo", this.redisTemplate);
		assertEquals(0, redisZSet.size());

		Message<String> message = MessageBuilder.withPayload("bar").setHeader(RedisHeaders.KEY, "foo").build();
		this.zsetChannel.send(message);

		assertEquals(1, redisZSet.size());
		assertEquals(Double.valueOf(1), redisZSet.score("bar"));

		this.zsetChannel.send(message);

		assertEquals(1, redisZSet.size());
		assertEquals(Double.valueOf(2), redisZSet.score("bar"));
	}

	@Test
	@RedisAvailable
	public void testZsetSimplePayloadOverwrite() {
		RedisZSet<String> redisZSet = new DefaultRedisZSet<String>("foo", this.redisTemplate);
		assertEquals(0, redisZSet.size());

		Message<String> message = MessageBuilder.withPayload("bar")
				.setHeader(RedisHeaders.KEY, "foo")
				.setHeader(RedisHeaders.ZSET_INCREMENT_SCORE, false)
				.build();
		this.zsetChannel.send(message);

		assertEquals(1, redisZSet.size());
		assertEquals(Double.valueOf(1), redisZSet.score("bar"));

		this.zsetChannel.send(message);

		assertEquals(1, redisZSet.size());
		assertEquals(Double.valueOf(1), redisZSet.score("bar"));
	}

	@Test
	@RedisAvailable
	public void testZsetSimplePayloadIncrementBy2() {
		RedisZSet<String> redisZSet = new DefaultRedisZSet<String>("foo", this.redisTemplate);
		assertEquals(0, redisZSet.size());

		Message<String> message = MessageBuilder.withPayload("bar")
				.setHeader(RedisHeaders.KEY, "foo")
				.setHeader(RedisHeaders.ZSET_SCORE, 2)
				.build();
		this.zsetChannel.send(message);

		assertEquals(1, redisZSet.size());
		assertEquals(Double.valueOf(2), redisZSet.score("bar"));

		this.zsetChannel.send(message);

		assertEquals(1, redisZSet.size());
		assertEquals(Double.valueOf(4), redisZSet.score("bar"));
	}

	@Test
	@RedisAvailable
	public void testZsetSimplePayloadOverwriteWithHeaderScore() {
		RedisZSet<String> redisZSet = new DefaultRedisZSet<String>("foo", this.redisTemplate);
		assertEquals(0, redisZSet.size());

		Message<String> message = MessageBuilder.withPayload("bar")
				.setHeader(RedisHeaders.KEY, "foo")
				.setHeader(RedisHeaders.ZSET_INCREMENT_SCORE, false)
				.setHeader(RedisHeaders.ZSET_SCORE, 2)
				.build();
		this.zsetChannel.send(message);

		assertEquals(1, redisZSet.size());
		assertEquals(Double.valueOf(2), redisZSet.score("bar"));

		this.zsetChannel.send(MessageBuilder.fromMessage(message).setHeader(RedisHeaders.ZSET_SCORE, 15).build());

		assertEquals(1, redisZSet.size());
		assertEquals(Double.valueOf(15), redisZSet.score("bar"));
	}

	@Test
	@RedisAvailable
	public void testMapToZsetWithProvidedKey() {
		RedisZSet<String> redisZset = new DefaultRedisZSet<String>("presidents", this.redisTemplate);
		assertEquals(0, redisZset.size());

		Map<String, Integer> presidents = new HashMap<String, Integer>();
		presidents.put("John Adams", 18);

		presidents.put("Barack Obama", 21);
		presidents.put("Thomas Jefferson", 19);
		presidents.put("John Quincy Adams", 19);
		presidents.put("Zachary Taylor", 19);

		Message<Map<String, Integer>> message = MessageBuilder.withPayload(presidents).build();

		this.mapToZsetChannel.send(message);

		assertEquals(5, redisZset.size());
		assertEquals(1, redisZset.rangeByScore(18, 18).size());
		assertEquals(4, redisZset.rangeByScore(18, 19).size());
		assertEquals(1, redisZset.rangeByScore(21, 21).size());

		RedisStoreWritingMessageHandler handler =
				this.beanFactory.getBean("mapToZset.handler", RedisStoreWritingMessageHandler.class);
		assertEquals("'presidents'", TestUtils.getPropertyValue(handler, "keyExpression.expression"));

		// test default (increment by score) behavior
		this.mapToZsetChannel.send(message);

		assertEquals(5, redisZset.size());
		assertEquals(1, redisZset.rangeByScore(36, 36).size());
		assertEquals(4, redisZset.rangeByScore(36, 38).size());
		assertEquals(1, redisZset.rangeByScore(42, 42).size());

		// test overwrite score behavior
		presidents.put("Barack Obama", 31);

		this.mapToZsetChannel.send(MessageBuilder.fromMessage(message)
				.setHeader(RedisHeaders.ZSET_INCREMENT_SCORE, false)
				.build());

		assertEquals(5, redisZset.size());
		assertEquals(1, redisZset.rangeByScore(18, 18).size());
		assertEquals(4, redisZset.rangeByScore(18, 19).size());
		assertEquals(1, redisZset.rangeByScore(31, 31).size());
	}

	@Test
	@RedisAvailable
	public void testMapToMapWithProvidedKey() {
		RedisMap<String, String> redisMap = new DefaultRedisMap<String, String>("pepboys", this.redisTemplate);

		assertEquals(0, redisMap.size());

		Map<String, String> pepboys = new HashMap<String, String>();
		pepboys.put("1", "Manny");
		pepboys.put("2", "Moe");
		pepboys.put("3", "Jack");

		Message<Map<String, String>> message = MessageBuilder.withPayload(pepboys).build();
		this.mapToMapAChannel.send(message);
		assertEquals("Manny", redisMap.get("1"));
		assertEquals("Moe", redisMap.get("2"));
		assertEquals("Jack", redisMap.get("3"));

		RedisStoreWritingMessageHandler handler = this.beanFactory.getBean("mapToMapA.handler",
				RedisStoreWritingMessageHandler.class);
		assertEquals("pepboys",
				TestUtils.getPropertyValue(handler, "keyExpression", LiteralExpression.class).getExpressionString());
		assertEquals("'foo'",
				TestUtils.getPropertyValue(handler, "mapKeyExpression", SpelExpression.class).getExpressionString());
	}

	@Test(expected = MessageHandlingException.class) // map key is not provided
	@RedisAvailable
	public void testMapToMapAsSingleEntryWithKeyAsHeaderFail() {
		RedisMap<String, Map<String, String>> redisMap =
				new DefaultRedisMap<String, Map<String, String>>("pepboys", this.redisTemplate);

		assertEquals(0, redisMap.size());

		Map<String, String> pepboys = new HashMap<String, String>();
		pepboys.put("1", "Manny");
		pepboys.put("2", "Moe");
		pepboys.put("3", "Jack");

		Message<Map<String, String>> message = MessageBuilder.withPayload(pepboys).
				setHeader(RedisHeaders.KEY, "pepboys").build();

		this.mapToMapBChannel.send(message);
	}

	@Test(expected = MessageHandlingException.class) // key is not provided
	@RedisAvailable
	public void testMapToMapNoKey() {
		RedisTemplate<String, Map<String, Map<String, String>>> redisTemplate = new RedisTemplate<String, Map<String, Map<String, String>>>();
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setHashKeySerializer(new StringRedisSerializer());
		redisTemplate.setConnectionFactory(getConnectionFactoryForTest());
		redisTemplate.afterPropertiesSet();

		RedisMap<String, Map<String, String>> redisMap =
				new DefaultRedisMap<String, Map<String, String>>("pepboys", redisTemplate);

		assertEquals(0, redisMap.size());

		Map<String, String> pepboys = new HashMap<String, String>();
		pepboys.put("1", "Manny");
		pepboys.put("2", "Moe");
		pepboys.put("3", "Jack");

		Message<Map<String, String>> message = MessageBuilder.withPayload(pepboys).build();
		this.mapToMapBChannel.send(message);
	}

	@Test
	@RedisAvailable
	public void testMapToMapAsSingleEntryWithKeyAsHeader() {
		RedisTemplate<String, Map<String, Map<String, String>>> redisTemplate = new RedisTemplate<String, Map<String, Map<String, String>>>();
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setHashKeySerializer(new StringRedisSerializer());
		redisTemplate.setConnectionFactory(getConnectionFactoryForTest());
		redisTemplate.afterPropertiesSet();

		RedisMap<String, Map<String, String>> redisMap =
				new DefaultRedisMap<String, Map<String, String>>("pepboys", redisTemplate);

		assertEquals(0, redisMap.size());

		Map<String, String> pepboys = new HashMap<String, String>();
		pepboys.put("1", "Manny");
		pepboys.put("2", "Moe");
		pepboys.put("3", "Jack");

		Message<Map<String, String>> message = MessageBuilder.withPayload(pepboys).
				setHeader(RedisHeaders.KEY, "pepboys").setHeader(RedisHeaders.MAP_KEY, "foo").build();
		this.mapToMapBChannel.send(message);
		Map<String, String> pepboyz = redisMap.get("foo");

		assertEquals("Manny", pepboyz.get("1"));
		assertEquals("Moe", pepboyz.get("2"));
		assertEquals("Jack", pepboyz.get("3"));
	}

	@Test
	@RedisAvailable
	public void testStoreSimpleStringInMap() {
		RedisMap<String, String> redisMap = new DefaultRedisMap<String, String>("bar", this.redisTemplate);

		assertEquals(0, redisMap.size());

		Message<String> message = MessageBuilder.withPayload("hello, world!").
				setHeader(RedisHeaders.KEY, "bar").setHeader(RedisHeaders.MAP_KEY, "foo").build();

		this.simpleMapChannel.send(message);
		String hello = redisMap.get("foo");

		assertEquals("hello, world!", hello);
	}

	@Test
	@RedisAvailable
	public void testSetWithKeyAsHeader() {
		RedisSet<String> redisSet = new DefaultRedisSet<String>("pepboys", this.redisTemplate);
		assertEquals(0, redisSet.size());

		Set<String> pepboys = new HashSet<String>();
		pepboys.add("Manny");
		pepboys.add("Moe");
		pepboys.add("Jack");
		Message<Set<String>> message = MessageBuilder.withPayload(pepboys).setHeader("redis_key", "pepboys").build();
		this.setChannel.send(message);

		assertEquals(3, redisSet.size());
	}

	@Test
	@RedisAvailable
	public void testSetWithKeyAsHeaderSimple() {
		RedisSet<String> redisSet = new DefaultRedisSet<String>("foo", this.redisTemplate);
		assertEquals(0, redisSet.size());

		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader(RedisHeaders.KEY, "foo").build();
		this.setChannel.send(message);

		assertEquals(1, redisSet.size());
	}

	@Test
	@RedisAvailable
	public void testSetWithKeyAsHeaderNotParsed() {
		RedisSet<String> redisSet = new DefaultRedisSet<String>("pepboys", this.redisTemplate);
		assertEquals(0, redisSet.size());

		Set<String> pepboys = new HashSet<String>();
		pepboys.add("Manny");
		pepboys.add("Moe");
		pepboys.add("Jack");
		Message<Set<String>> message = MessageBuilder.withPayload(pepboys).setHeader("redis_key", "pepboys").build();
		this.setNotParsedChannel.send(message);

		assertEquals(1, redisSet.size());
	}

	@Test
	@RedisAvailable
	public void testPojoIntoSet() {
		RedisSet<String> redisSet = new DefaultRedisSet<String>("pepboys", this.redisTemplate);
		assertEquals(0, redisSet.size());

		String pepboy = "Manny";
		Message<String> message = MessageBuilder.withPayload(pepboy).setHeader("redis_key", "pepboys").build();
		this.pojoIntoSetChannel.send(message);

		assertEquals(1, redisSet.size());
	}

	@Test
	@RedisAvailable
	public void testProperties() {
		RedisProperties redisProperties = new RedisProperties("pepboys", this.redisTemplate);

		assertEquals(0, redisProperties.size());

		Properties pepboys = new Properties();
		pepboys.put("1", "Manny");
		pepboys.put("2", "Moe");
		pepboys.put("3", "Jack");

		Message<Properties> message = MessageBuilder.withPayload(pepboys).build();
		this.propertyChannel.send(message);

		assertEquals("Manny", redisProperties.get("1"));
		assertEquals("Moe", redisProperties.get("2"));
		assertEquals("Jack", redisProperties.get("3"));
	}

	@Test
	@RedisAvailable
	public void testPropertiesSimple() {
		RedisProperties redisProperties = new RedisProperties("foo", this.redisTemplate);

		assertEquals(0, redisProperties.size());

		Message<String> message = MessageBuilder.withPayload("bar")
				.setHeader(RedisHeaders.KEY, "foo")
				.setHeader("baz", "qux")
				.build();
		this.simplePropertyChannel.send(message);

		assertEquals("bar", redisProperties.get("qux"));
	}

}
