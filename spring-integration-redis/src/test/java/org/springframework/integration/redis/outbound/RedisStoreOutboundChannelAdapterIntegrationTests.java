/*
 * Copyright 2007-2014 the original author or authors
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

package org.springframework.integration.redis.outbound;

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

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.2
 */
public class RedisStoreOutboundChannelAdapterIntegrationTests extends RedisAvailableTests {

	@Test
	@RedisAvailable
	public void testListWithKeyAsHeader(){
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();

		this.deleteKey(jcf, "pepboys");
		RedisList<String> redisList =
				new DefaultRedisList<String>("pepboys", this.initTemplate(jcf, new StringRedisTemplate()));
		assertEquals(0, redisList.size());

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("store-outbound-adapter.xml", this.getClass());
		MessageChannel redisChannel = context.getBean("listWithKeyAsHeader", MessageChannel.class);
		List<String> pepboys = new ArrayList<String>();
		pepboys.add("Manny");
		pepboys.add("Moe");
		pepboys.add("Jack");
		Message<List<String>> message = MessageBuilder.withPayload(pepboys).setHeader(RedisHeaders.KEY, "pepboys").build();
		redisChannel.send(message);

		assertEquals(3, redisList.size());
		this.deleteKey(jcf, "pepboys");
		context.close();
	}

	@Test
	@RedisAvailable
	public void testListWithKeyAsHeaderSimple(){
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();

		StringRedisTemplate redisTemplate = new StringRedisTemplate();
		RedisList<String> redisList =
				new DefaultRedisList<String>("foo", this.initTemplate(jcf, redisTemplate));
		assertEquals(0, redisList.size());

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("store-outbound-adapter.xml", this.getClass());
		MessageChannel redisChannel = context.getBean("listWithKeyAsHeader", MessageChannel.class);
		Message<String> message = MessageBuilder.withPayload("bar").setHeader("redis_key", "foo").build();
		redisChannel.send(message);

		assertEquals(1, redisList.size());
		redisTemplate.delete("foo");
		context.close();
	}

	@Test
	@RedisAvailable
	public void testListWithProvidedKey(){
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		this.deleteKey(jcf, "pepboys");
		RedisList<String> redisList =
				new DefaultRedisList<String>("pepboys", this.initTemplate(jcf, new StringRedisTemplate()));
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
		this.deleteKey(jcf, "pepboys");
		context.close();
	}

	@Test
	@RedisAvailable
	public void testZsetSimplePayloadIncrement(){
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();

		StringRedisTemplate redisTemplate = new StringRedisTemplate();
		RedisZSet<String> redisZSet =
				new DefaultRedisZSet<String>("foo", this.initTemplate(jcf, redisTemplate));
		assertEquals(0, redisZSet.size());

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("store-outbound-adapter.xml", this.getClass());
		MessageChannel redisChannel = context.getBean("zset", MessageChannel.class);
		Message<String> message = MessageBuilder.withPayload("bar").setHeader(RedisHeaders.KEY, "foo").build();
		redisChannel.send(message);

		assertEquals(1, redisZSet.size());
		assertEquals(Double.valueOf(1), redisZSet.score("bar"));

		redisChannel.send(message);

		assertEquals(1, redisZSet.size());
		assertEquals(Double.valueOf(2), redisZSet.score("bar"));
		redisTemplate.delete("foo");
		context.close();
	}

	@Test
	@RedisAvailable
	public void testZsetSimplePayloadOverwrite(){
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();

		StringRedisTemplate redisTemplate = new StringRedisTemplate();
		RedisZSet<String> redisZSet =
				new DefaultRedisZSet<String>("foo", this.initTemplate(jcf, redisTemplate));
		assertEquals(0, redisZSet.size());

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("store-outbound-adapter.xml", this.getClass());
		MessageChannel redisChannel = context.getBean("zset", MessageChannel.class);
		Message<String> message = MessageBuilder.withPayload("bar")
				.setHeader(RedisHeaders.KEY, "foo")
				.setHeader(RedisHeaders.ZSET_INCREMENT_SCORE, false)
				.build();
		redisChannel.send(message);

		assertEquals(1, redisZSet.size());
		assertEquals(Double.valueOf(1), redisZSet.score("bar"));

		redisChannel.send(message);

		assertEquals(1, redisZSet.size());
		assertEquals(Double.valueOf(1), redisZSet.score("bar"));
		redisTemplate.delete("foo");
		context.close();
	}

	@Test
	@RedisAvailable
	public void testZsetSimplePayloadIncrementBy2(){
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();

		StringRedisTemplate redisTemplate = new StringRedisTemplate();
		RedisZSet<String> redisZSet =
				new DefaultRedisZSet<String>("foo", this.initTemplate(jcf, redisTemplate));
		assertEquals(0, redisZSet.size());

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("store-outbound-adapter.xml", this.getClass());
		MessageChannel redisChannel = context.getBean("zset", MessageChannel.class);
		Message<String> message = MessageBuilder.withPayload("bar")
				.setHeader(RedisHeaders.KEY, "foo")
				.setHeader(RedisHeaders.ZSET_SCORE, 2)
				.build();
		redisChannel.send(message);

		assertEquals(1, redisZSet.size());
		assertEquals(Double.valueOf(2), redisZSet.score("bar"));

		redisChannel.send(message);

		assertEquals(1, redisZSet.size());
		assertEquals(Double.valueOf(4), redisZSet.score("bar"));
		redisTemplate.delete("foo");
		context.close();
	}

	@Test
	@RedisAvailable
	public void testZsetSimplePayloadOverwriteWithHeaderScore(){
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();

		StringRedisTemplate redisTemplate = new StringRedisTemplate();
		RedisZSet<String> redisZSet =
				new DefaultRedisZSet<String>("foo", this.initTemplate(jcf, redisTemplate));
		assertEquals(0, redisZSet.size());

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("store-outbound-adapter.xml", this.getClass());
		MessageChannel redisChannel = context.getBean("zset", MessageChannel.class);
		Message<String> message = MessageBuilder.withPayload("bar")
				.setHeader(RedisHeaders.KEY, "foo")
				.setHeader(RedisHeaders.ZSET_INCREMENT_SCORE, false)
				.setHeader(RedisHeaders.ZSET_SCORE, 2)
				.build();
		redisChannel.send(message);

		assertEquals(1, redisZSet.size());
		assertEquals(Double.valueOf(2), redisZSet.score("bar"));

		redisChannel.send(MessageBuilder.fromMessage(message).setHeader(RedisHeaders.ZSET_SCORE, 15).build());

		assertEquals(1, redisZSet.size());
		assertEquals(Double.valueOf(15), redisZSet.score("bar"));
		redisTemplate.delete("foo");
		context.close();
	}

	@Test
	@RedisAvailable
	public void testMapToZsetWithProvidedKey(){
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		this.deletePresidents(jcf);
		RedisZSet<String> redisZset =
				new DefaultRedisZSet<String>("presidents", this.initTemplate(jcf, new StringRedisTemplate()));
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
		assertEquals(1, redisZset.rangeByScore(21, 21).size());

		RedisStoreWritingMessageHandler handler = context.getBean("mapToZset.handler",
				RedisStoreWritingMessageHandler.class);
		assertEquals("'presidents'", TestUtils.getPropertyValue(handler, "keyExpression", SpelExpression.class).getExpressionString());

		// test default (increment by score) behavior
		redisChannel.send(message);
		assertEquals(5, redisZset.size());
		assertEquals(1, redisZset.rangeByScore(36, 36).size());
		assertEquals(4, redisZset.rangeByScore(36, 38).size());
		assertEquals(1, redisZset.rangeByScore(42, 42).size());

		// test overwrite score behavior
		presidents.put("Barack Obama", 31);
		redisChannel.send(MessageBuilder.fromMessage(message).setHeader(RedisHeaders.ZSET_INCREMENT_SCORE, false).build());
		assertEquals(5, redisZset.size());
		assertEquals(1, redisZset.rangeByScore(18, 18).size());
		assertEquals(4, redisZset.rangeByScore(18, 19).size());
		assertEquals(1, redisZset.rangeByScore(31, 31).size());
		this.deletePresidents(jcf);
		context.close();
	}

	@Test
	@RedisAvailable
	public void testMapToMapWithProvidedKey(){
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		this.deleteKey(jcf, "pepboys");
		RedisMap<String, String> redisMap =
				new DefaultRedisMap<String, String>("pepboys",
						this.initTemplate(jcf, new StringRedisTemplate()));

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

		RedisStoreWritingMessageHandler handler = context.getBean("mapToMapA.handler",
				RedisStoreWritingMessageHandler.class);
		assertEquals("pepboys", TestUtils.getPropertyValue(handler, "keyExpression", LiteralExpression.class).getExpressionString());
		assertEquals("'foo'", TestUtils.getPropertyValue(handler, "mapKeyExpression", SpelExpression.class).getExpressionString());
		this.deleteKey(jcf, "pepboys");
		context.close();
	}

	@Test(expected=MessageHandlingException.class) // map key is not provided
	@RedisAvailable
	public void testMapToMapAsSingleEntryWithKeyAsHeaderFail(){
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		this.deleteKey(jcf, "pepboys");
		RedisMap<String, Map<String, String>> redisMap =
				new DefaultRedisMap<String, Map<String, String>>("pepboys",
						this.initTemplate(jcf, new RedisTemplate<String, Map<String, Map<String, String>>>()));

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
		this.deleteKey(jcf, "pepboys");
		context.close();
	}

	@Test(expected=MessageHandlingException.class) // key is not provided
	@RedisAvailable
	public void testMapToMapNoKey(){
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		this.deleteKey(jcf, "pepboys");
		RedisTemplate<String, Map<String, Map<String, String>>> redisTemplate = new RedisTemplate<String, Map<String, Map<String, String>>>();
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setHashKeySerializer(new StringRedisSerializer());
		RedisMap<String, Map<String, String>> redisMap =
				new DefaultRedisMap<String, Map<String, String>>("pepboys",
						this.initTemplate(jcf, redisTemplate));

		assertEquals(0, redisMap.size());

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("store-outbound-adapter.xml", this.getClass());
		MessageChannel redisChannel = context.getBean("mapToMapB", MessageChannel.class);
		Map<String, String> pepboys = new HashMap<String, String>();
		pepboys.put("1", "Manny");
		pepboys.put("2", "Moe");
		pepboys.put("3", "Jack");

		Message<Map<String, String>> message = MessageBuilder.withPayload(pepboys).build();
		redisChannel.send(message);
		this.deleteKey(jcf, "pepboys");
		context.close();
	}

	@Test
	@RedisAvailable
	public void testMapToMapAsSingleEntryWithKeyAsHeader(){
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		this.deleteKey(jcf, "pepboys");
		RedisTemplate<String, Map<String, Map<String, String>>> redisTemplate = new RedisTemplate<String, Map<String, Map<String, String>>>();
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setHashKeySerializer(new StringRedisSerializer());
		RedisMap<String, Map<String, String>> redisMap =
				new DefaultRedisMap<String, Map<String, String>>("pepboys",
						this.initTemplate(jcf, redisTemplate));

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
		this.deleteKey(jcf, "pepboys");
		context.close();
	}

	@Test
	@RedisAvailable
	public void testStoreSimpleStringInMap(){
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		this.deleteKey(jcf, "bar");
		StringRedisTemplate redisTemplate = new StringRedisTemplate();
		RedisMap<String, String> redisMap =
				new DefaultRedisMap<String, String>("bar",
						this.initTemplate(jcf, redisTemplate));

		assertEquals(0, redisMap.size());

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("store-outbound-adapter.xml", this.getClass());
		MessageChannel redisChannel = context.getBean("simpleMap", MessageChannel.class);

		Message<String> message = MessageBuilder.withPayload("hello, world!").
				setHeader(RedisHeaders.KEY, "bar").setHeader(RedisHeaders.MAP_KEY, "foo").build();
		redisChannel.send(message);
		String hello = redisMap.get("foo");

		assertEquals("hello, world!", hello);
		this.deleteKey(jcf, "bar");
		context.close();
	}

	@Test
	@RedisAvailable
	public void testSetWithKeyAsHeader(){
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		this.deleteKey(jcf, "pepboys");
		RedisSet<String> redisSet =
				new DefaultRedisSet<String>("pepboys", this.initTemplate(jcf, new StringRedisTemplate()));
		assertEquals(0, redisSet.size());

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("store-outbound-adapter.xml", this.getClass());
		MessageChannel redisChannel = context.getBean("set", MessageChannel.class);
		Set<String> pepboys = new HashSet<String>();
		pepboys.add("Manny");
		pepboys.add("Moe");
		pepboys.add("Jack");
		Message<Set<String>> message = MessageBuilder.withPayload(pepboys).setHeader("redis_key", "pepboys").build();
		redisChannel.send(message);

		assertEquals(3, redisSet.size());
		this.deleteKey(jcf, "pepboys");
		context.close();
	}

	@Test
	@RedisAvailable
	public void testSetWithKeyAsHeaderSimple(){
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		StringRedisTemplate redisTemplate = new StringRedisTemplate();
		RedisSet<String> redisSet =
				new DefaultRedisSet<String>("foo", this.initTemplate(jcf, redisTemplate));
		assertEquals(0, redisSet.size());

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("store-outbound-adapter.xml", this.getClass());
		MessageChannel redisChannel = context.getBean("set", MessageChannel.class);
		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader(RedisHeaders.KEY, "foo").build();
		redisChannel.send(message);

		assertEquals(1, redisSet.size());
		redisTemplate.delete("foo");
		context.close();
	}

	@Test
	@RedisAvailable
	public void testSetWithKeyAsHeaderNotParsed(){
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		this.deleteKey(jcf, "pepboys");
		RedisTemplate<String, String> redisTemplate = new RedisTemplate<String, String>();
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setHashKeySerializer(new StringRedisSerializer());
		RedisSet<String> redisSet =
				new DefaultRedisSet<String>("pepboys", this.initTemplate(jcf, redisTemplate));
		assertEquals(0, redisSet.size());

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("store-outbound-adapter.xml", this.getClass());
		MessageChannel redisChannel = context.getBean("setNotParsed", MessageChannel.class);
		Set<String> pepboys = new HashSet<String>();
		pepboys.add("Manny");
		pepboys.add("Moe");
		pepboys.add("Jack");
		Message<Set<String>> message = MessageBuilder.withPayload(pepboys).setHeader("redis_key", "pepboys").build();
		redisChannel.send(message);

		assertEquals(1, redisSet.size());
		this.deleteKey(jcf, "pepboys");
		context.close();
	}

	@Test
	@RedisAvailable
	public void testPojoIntoSet(){
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		this.deleteKey(jcf, "pepboys");
		RedisSet<String> redisSet =
				new DefaultRedisSet<String>("pepboys", this.initTemplate(jcf, new StringRedisTemplate()));
		assertEquals(0, redisSet.size());

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("store-outbound-adapter.xml", this.getClass());
		MessageChannel redisChannel = context.getBean("pojoIntoSet", MessageChannel.class);
		String pepboy = "Manny";
		Message<String> message = MessageBuilder.withPayload(pepboy).setHeader("redis_key", "pepboys").build();
		redisChannel.send(message);

		assertEquals(1, redisSet.size());
		this.deleteKey(jcf, "pepboys");
		context.close();
	}

	@Test
	@RedisAvailable
	public void testProperties(){
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		this.deleteKey(jcf, "pepboys");
		RedisProperties redisProperties =
				new RedisProperties("pepboys", this.initTemplate(jcf, new StringRedisTemplate()));

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
		this.deleteKey(jcf, "pepboys");
		context.close();
	}

	@Test
	@RedisAvailable
	public void testPropertiesSimple(){
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		StringRedisTemplate redisTemplate = new StringRedisTemplate();
		RedisProperties redisProperties =
				new RedisProperties("foo", this.initTemplate(jcf, redisTemplate));

		assertEquals(0, redisProperties.size());

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("store-outbound-adapter.xml", this.getClass());
		MessageChannel redisChannel = context.getBean("simpleProperty", MessageChannel.class);
		Message<String> message = MessageBuilder.withPayload("bar")
				.setHeader(RedisHeaders.KEY, "foo")
				.setHeader("baz", "qux")
				.build();
		redisChannel.send(message);

		assertEquals("bar", redisProperties.get("qux"));
		redisTemplate.delete("foo");
		context.close();
	}

	private <K,V> RedisTemplate<K,V> initTemplate(RedisConnectionFactory rcf, RedisTemplate<K,V> redisTemplate){
		redisTemplate.setConnectionFactory(rcf);
		redisTemplate.afterPropertiesSet();
		return redisTemplate;
	}
}
