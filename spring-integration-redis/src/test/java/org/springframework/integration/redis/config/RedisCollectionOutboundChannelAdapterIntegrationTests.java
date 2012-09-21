/*
 * Copyright 2007-2012 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package org.springframework.integration.redis.config;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
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
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.redis.outbound.RedisCollectionPopulatingMessageHandler;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;
import org.springframework.integration.redis.support.RedisHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author Oleg Zhurakousky
 * @since 2.2
 */
public class RedisCollectionOutboundChannelAdapterIntegrationTests extends RedisAvailableTests {

	@SuppressWarnings("unchecked")
	@Test
	@RedisAvailable
	public void testListWithKeyAsHeader(){
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();

		RedisList<String> redisList =
				new DefaultRedisList<String>("pepboys",
						(RedisOperations<String, String>) this.initTemplate(jcf, new RedisTemplate<String, String>()));
		assertEquals(0, redisList.size());

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("store-outbound-adapter.xml", this.getClass());
		MessageChannel redisChannel = context.getBean("listWithKeyAsHeader", MessageChannel.class);
		List<String> pepboys = new ArrayList<String>();
		pepboys.add("Manny");
		pepboys.add("Moe");
		pepboys.add("Jack");
		Message<List<String>> message = MessageBuilder.withPayload(pepboys).setHeader("redis_key", "pepboys").build();
		redisChannel.send(message);

		assertEquals(3, redisList.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	@RedisAvailable
	public void testListWithProvidedKey(){
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisList<String> redisList =
				new DefaultRedisList<String>("pepboys",
						(RedisOperations<String, String>) this.initTemplate(jcf, new RedisTemplate<String, String>()));
		assertEquals(0, redisList.size());

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("store-outbound-adapter.xml", this.getClass());
		MessageChannel redisChannel = context.getBean("listWithKeyProvided", MessageChannel.class);
		List<String> pepboys = new ArrayList<String>();
		pepboys.add("Manny");
		pepboys.add("Moe");
		pepboys.add("Jack");
		Message<List<String>> message = MessageBuilder.withPayload(pepboys).build();
		redisChannel.send(message);

		assertEquals(3, redisList.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	@RedisAvailable
	public void testMapToZsetWithProvidedKey(){
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisZSet<String> redisZset =
				new DefaultRedisZSet<String>("presidents",
						(RedisOperations<String, String>) this.initTemplate(jcf, new RedisTemplate<String, String>()));
		assertEquals(0, redisZset.size());

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("store-outbound-adapter.xml", this.getClass());
		MessageChannel redisChannel = context.getBean("mapToZset", MessageChannel.class);
		Map<String, Integer> presidents = new HashMap<String, Integer>();
		presidents.put("John Adams", 18);

		presidents.put("Barack Obama", 21);
		presidents.put("Thomas Jefferson", 19);
		presidents.put("John Quincy Adams", 19);
		presidents.put("Zachary Taylor", 19);

		Message<Map<String, Integer>> message = MessageBuilder.withPayload(presidents).build();
		redisChannel.send(message);

		assertEquals(5, redisZset.size());
		assertEquals(1, redisZset.rangeByScore(18, 18).size());
		assertEquals(4, redisZset.rangeByScore(18, 19).size());

		RedisCollectionPopulatingMessageHandler handler = context.getBean("mapToZset.handler",
				RedisCollectionPopulatingMessageHandler.class);
		assertEquals("'presidents'", TestUtils.getPropertyValue(handler, "keyExpression", SpelExpression.class).getExpressionString());
	}

	@SuppressWarnings("unchecked")
	@Test
	@RedisAvailable
	public void testMapToMapWithProvidedKey(){
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMap<String, String> redisMap =
				new DefaultRedisMap<String, String>("pepboys",
						(RedisOperations<String, ?>) this.initTemplate(jcf, new RedisTemplate<String, Map<String, String>>()));

		assertEquals(0, redisMap.size());

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("store-outbound-adapter.xml", this.getClass());
		MessageChannel redisChannel = context.getBean("mapToMapA", MessageChannel.class);
		Map<String, String> pepboys = new HashMap<String, String>();
		pepboys.put("1", "Manny");
		pepboys.put("2", "Moe");
		pepboys.put("3", "Jack");


		Message<Map<String, String>> message = MessageBuilder.withPayload(pepboys).build();
		redisChannel.send(message);
		assertEquals("Manny", redisMap.get("1"));
		assertEquals("Moe", redisMap.get("2"));
		assertEquals("Jack", redisMap.get("3"));

		RedisCollectionPopulatingMessageHandler handler = context.getBean("mapToMapA.handler",
				RedisCollectionPopulatingMessageHandler.class);
		assertEquals("pepboys", TestUtils.getPropertyValue(handler, "keyExpression", LiteralExpression.class).getExpressionString());
		assertEquals("'foo'", TestUtils.getPropertyValue(handler, "mapKeyExpression", SpelExpression.class).getExpressionString());
	}

	@SuppressWarnings("unchecked")
	@Test(expected=MessageHandlingException.class)// map key is not proivided
	@RedisAvailable
	public void testMapToMapAsSingleEntryWithKeyAsHeaderFail(){
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMap<String, Map<String, String>> redisMap =
				new DefaultRedisMap<String, Map<String, String>>("pepboys",
						(RedisOperations<String, ?>) this.initTemplate(jcf, new RedisTemplate<String, Map<String, Map<String, String>>>()));

		assertEquals(0, redisMap.size());

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("store-outbound-adapter.xml", this.getClass());
		MessageChannel redisChannel = context.getBean("mapToMapB", MessageChannel.class);
		Map<String, String> pepboys = new HashMap<String, String>();
		pepboys.put("1", "Manny");
		pepboys.put("2", "Moe");
		pepboys.put("3", "Jack");


		Message<Map<String, String>> message = MessageBuilder.withPayload(pepboys).
				setHeader(RedisHeaders.KEY, "pepboys").build();
		redisChannel.send(message);
	}

	@SuppressWarnings("unchecked")
	@Test(expected=MessageHandlingException.class)//key is not provided
	@RedisAvailable
	public void testMapToMapNoKey(){
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMap<String, Map<String, String>> redisMap =
				new DefaultRedisMap<String, Map<String, String>>("pepboys",
						(RedisOperations<String, ?>) this.initTemplate(jcf, new RedisTemplate<String, Map<String, Map<String, String>>>()));

		assertEquals(0, redisMap.size());

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("store-outbound-adapter.xml", this.getClass());
		MessageChannel redisChannel = context.getBean("mapToMapB", MessageChannel.class);
		Map<String, String> pepboys = new HashMap<String, String>();
		pepboys.put("1", "Manny");
		pepboys.put("2", "Moe");
		pepboys.put("3", "Jack");


		Message<Map<String, String>> message = MessageBuilder.withPayload(pepboys).build();
		redisChannel.send(message);
	}

	@SuppressWarnings("unchecked")
	@Test
	@RedisAvailable
	public void testMapToMapAsSingleEntryWithKeyAsHeader(){
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMap<String, Map<String, String>> redisMap =
				new DefaultRedisMap<String, Map<String, String>>("pepboys",
						(RedisOperations<String, ?>) this.initTemplate(jcf, new RedisTemplate<String, Map<String, Map<String, String>>>()));

		assertEquals(0, redisMap.size());

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("store-outbound-adapter.xml", this.getClass());
		MessageChannel redisChannel = context.getBean("mapToMapB", MessageChannel.class);
		Map<String, String> pepboys = new HashMap<String, String>();
		pepboys.put("1", "Manny");
		pepboys.put("2", "Moe");
		pepboys.put("3", "Jack");


		Message<Map<String, String>> message = MessageBuilder.withPayload(pepboys).
				setHeader(RedisHeaders.KEY, "pepboys").setHeader(RedisHeaders.MAP_KEY, "foo").build();
		redisChannel.send(message);
		Map<String, String> pepboyz = redisMap.get("foo");

		assertEquals("Manny", pepboyz.get("1"));
		assertEquals("Moe", pepboyz.get("2"));
		assertEquals("Jack", pepboyz.get("3"));
	}

	@SuppressWarnings("unchecked")
	@Test
	@RedisAvailable
	public void testSetWithKeyAsHeader(){
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisSet<String> redisList =
				new DefaultRedisSet<String>("pepboys",
						(RedisOperations<String, String>) this.initTemplate(jcf, new RedisTemplate<String, String>()));
		assertEquals(0, redisList.size());

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("store-outbound-adapter.xml", this.getClass());
		MessageChannel redisChannel = context.getBean("set", MessageChannel.class);
		Set<String> pepboys = new HashSet<String>();
		pepboys.add("Manny");
		pepboys.add("Moe");
		pepboys.add("Jack");
		Message<Set<String>> message = MessageBuilder.withPayload(pepboys).setHeader("redis_key", "pepboys").build();
		redisChannel.send(message);

		assertEquals(3, redisList.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	@RedisAvailable
	public void testSetWithKeyAsHeaderNotParsed(){
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisSet<String> redisList =
				new DefaultRedisSet<String>("pepboys",
						(RedisOperations<String, String>) this.initTemplate(jcf, new RedisTemplate<String, String>()));
		assertEquals(0, redisList.size());

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("store-outbound-adapter.xml", this.getClass());
		MessageChannel redisChannel = context.getBean("setNotParsed", MessageChannel.class);
		Set<String> pepboys = new HashSet<String>();
		pepboys.add("Manny");
		pepboys.add("Moe");
		pepboys.add("Jack");
		Message<Set<String>> message = MessageBuilder.withPayload(pepboys).setHeader("redis_key", "pepboys").build();
		redisChannel.send(message);

		assertEquals(1, redisList.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	@RedisAvailable
	public void testPojoIntoSet(){
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisSet<String> redisList =
				new DefaultRedisSet<String>("pepboys",
						(RedisOperations<String, String>) this.initTemplate(jcf, new RedisTemplate<String, String>()));
		assertEquals(0, redisList.size());

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("store-outbound-adapter.xml", this.getClass());
		MessageChannel redisChannel = context.getBean("pojoIntoSet", MessageChannel.class);
		String pepboy = "Manny";
		Message<String> message = MessageBuilder.withPayload(pepboy).setHeader("redis_key", "pepboys").build();
		redisChannel.send(message);

		assertEquals(1, redisList.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	@RedisAvailable
	public void testProperty(){
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisProperties redisProperties =
				new RedisProperties("pepboys",
						(RedisOperations<String, ?>) this.initTemplate(jcf, new RedisTemplate<String, Properties>()));

		assertEquals(0, redisProperties.size());

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("store-outbound-adapter.xml", this.getClass());
		MessageChannel redisChannel = context.getBean("property", MessageChannel.class);
		Properties pepboys = new Properties();
		pepboys.put("1", "Manny");
		pepboys.put("2", "Moe");
		pepboys.put("3", "Jack");

		Message<Properties> message = MessageBuilder.withPayload(pepboys).build();
		redisChannel.send(message);
		assertEquals("Manny", redisProperties.get("1"));
		assertEquals("Moe", redisProperties.get("2"));
		assertEquals("Jack", redisProperties.get("3"));
	}

	private RedisTemplate<?,?> initTemplate(RedisConnectionFactory rcf, RedisTemplate<?, ?> redisTemplate){
		redisTemplate.setConnectionFactory(rcf);
		redisTemplate.afterPropertiesSet();
		return redisTemplate;
	}
}
