package org.springframework.integration.redis.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.support.collections.DefaultRedisList;
import org.springframework.data.redis.support.collections.DefaultRedisZSet;
import org.springframework.data.redis.support.collections.RedisCollectionFactoryBean.CollectionType;
import org.springframework.data.redis.support.collections.RedisList;
import org.springframework.data.redis.support.collections.RedisZSet;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;
import org.springframework.integration.support.MessageBuilder;

public class RedisCollectionPopulatingMessageHandlerTests extends RedisAvailableTests{

	@SuppressWarnings("unchecked")
	@Test
	@RedisAvailable
	public void testListWithListPayloadParsedAndProvidedKey(){
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		String key = "foo";
		RedisList<String> redisList =
				new DefaultRedisList<String>(key,
						(RedisOperations<String, String>) this.initTemplate(jcf, new RedisTemplate<String, String>()));

		assertEquals(0, redisList.size());

		RedisCollectionPopulatingMessageHandler handler =
				new RedisCollectionPopulatingMessageHandler(jcf, new LiteralExpression(key));

		List<String> list = new ArrayList<String>();
		list.add("Manny");
		list.add("Moe");
		list.add("Jack");
		Message<List<String>> message = new GenericMessage<List<String>>(list);
		handler.handleMessage(message);

		assertEquals(3, redisList.size());
		assertEquals("Manny", redisList.get(0));
		assertEquals("Moe", redisList.get(1));
		assertEquals("Jack", redisList.get(2));
	}

	@SuppressWarnings("unchecked")
	@Test
	@RedisAvailable
	public void testListWithListPayloadParsedAndProvidedKeyAsHeader(){
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		String key = "foo";
		RedisList<String> redisList =
				new DefaultRedisList<String>(key,
						(RedisOperations<String, String>) this.initTemplate(jcf, new RedisTemplate<String, String>()));

		assertEquals(0, redisList.size());

		RedisCollectionPopulatingMessageHandler handler =
				new RedisCollectionPopulatingMessageHandler(jcf, null);

		List<String> list = new ArrayList<String>();
		list.add("Manny");
		list.add("Moe");
		list.add("Jack");
		Message<List<String>> message = MessageBuilder.withPayload(list).setHeader("redis_key", key).build();
		handler.handleMessage(message);

		assertEquals(3, redisList.size());
		assertEquals("Manny", redisList.get(0));
		assertEquals("Moe", redisList.get(1));
		assertEquals("Jack", redisList.get(2));
	}

	@SuppressWarnings("unchecked")
	@RedisAvailable
	@Test(expected=MessageHandlingException.class)
	public void testListWithListPayloadParsedAndNoKey(){
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		String key = "foo";
		RedisList<String> redisList =
				new DefaultRedisList<String>(key,
						(RedisOperations<String, String>) this.initTemplate(jcf, new RedisTemplate<String, String>()));

		assertEquals(0, redisList.size());

		RedisCollectionPopulatingMessageHandler handler =
				new RedisCollectionPopulatingMessageHandler(jcf, null);

		List<String> list = new ArrayList<String>();
		list.add("Manny");
		list.add("Moe");
		list.add("Jack");
		Message<List<String>> message = MessageBuilder.withPayload(list).build();
		handler.handleMessage(message);
	}

	@SuppressWarnings("unchecked")
	@Test
	@RedisAvailable
	public void testListWithListPayloadNotParsed(){
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		String key = "foo";
		RedisList<List<String>> redisList =
				new DefaultRedisList<List<String>>(key,
						(RedisOperations<String, List<String>>) this.initTemplate(jcf, new RedisTemplate<String, List<String>>()));

		assertEquals(0, redisList.size());

		RedisCollectionPopulatingMessageHandler handler =
				new RedisCollectionPopulatingMessageHandler(jcf, new LiteralExpression(key));
		handler.setParsePayload(false);

		List<String> list = new ArrayList<String>();
		list.add("Manny");
		list.add("Moe");
		list.add("Jack");
		Message<List<String>> message = new GenericMessage<List<String>>(list);
		handler.handleMessage(message);

		assertEquals(1, redisList.size());
		List<String> resultList = redisList.get(0);
		assertEquals("Manny", resultList.get(0));
		assertEquals("Moe", resultList.get(1));
		assertEquals("Jack", resultList.get(2));
	}

	@SuppressWarnings("unchecked")
	@Test
	@RedisAvailable
	public void testZsetWithListPayloadParsedAndProvidedKeyDefaultScore(){
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		String key = "foo";
		RedisZSet<String> redisZset =
				new DefaultRedisZSet<String>(key,
						(RedisOperations<String, String>) this.initTemplate(jcf, new RedisTemplate<String, String>()));

		assertEquals(0, redisZset.size());

		RedisCollectionPopulatingMessageHandler handler =
				new RedisCollectionPopulatingMessageHandler(jcf, new LiteralExpression(key));

		handler.setCollectionType(CollectionType.ZSET);

		List<String> list = new ArrayList<String>();
		list.add("Manny");
		list.add("Moe");
		list.add("Jack");
		Message<List<String>> message = new GenericMessage<List<String>>(list);
		handler.handleMessage(message);

		assertEquals(3, redisZset.size());
		Set<TypedTuple<String>> pepboys = redisZset.rangeByScoreWithScores(1, 1);
		for (TypedTuple<String> pepboy : pepboys) {
			assertTrue(pepboy.getScore() == 1);
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	@RedisAvailable
	public void testZsetWithListPayloadNotParsedAndHeaderKeyHeaderScore(){
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		String key = "foo";
		RedisZSet<List<String>> redisZset =
				new DefaultRedisZSet<List<String>>(key,
						(RedisOperations<String, List<String>>) this.initTemplate(jcf, new RedisTemplate<String, List<String>>()));

		assertEquals(0, redisZset.size());

		RedisCollectionPopulatingMessageHandler handler =
				new RedisCollectionPopulatingMessageHandler(jcf, null);

		handler.setCollectionType(CollectionType.ZSET);
		handler.setParsePayload(false);

		List<String> list = new ArrayList<String>();
		list.add("Manny");
		list.add("Moe");
		list.add("Jack");
		Message<List<String>> message = MessageBuilder.withPayload(list).setHeader("redis_key", key).
						setHeader("redis_zset_score", 4).build();
		handler.handleMessage(message);

		assertEquals(1, redisZset.size());
		Set<TypedTuple<List<String>>> entries = redisZset.rangeByScoreWithScores(1, 4);
		for (TypedTuple<List<String>> pepboys : entries) {
			assertTrue(pepboys.getScore() == 4);
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	@RedisAvailable
	public void testZsetWithMapPayloadParsedHeaderKey(){
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		String key = "presidents";
		RedisZSet<String> redisZset =
				new DefaultRedisZSet<String>(key,
						(RedisOperations<String, String>) this.initTemplate(jcf, new RedisTemplate<String, String>()));

		assertEquals(0, redisZset.size());

		RedisCollectionPopulatingMessageHandler handler =
				new RedisCollectionPopulatingMessageHandler(jcf, new LiteralExpression(key));

		handler.setCollectionType(CollectionType.ZSET);

		Map<String, Double> presidents = new HashMap<String, Double>();
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

		assertEquals(13, redisZset.size());

		Set<TypedTuple<String>> entries = redisZset.rangeByScoreWithScores(18, 19);
		assertEquals(6, entries.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	@RedisAvailable
	public void testZsetWithMapPayloadPojoParsedHeaderKey(){
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		String key = "presidents";
		RedisZSet<President> redisZset =
				new DefaultRedisZSet<President>(key,
						(RedisOperations<String, President>) this.initTemplate(jcf, new RedisTemplate<String, President>()));

		assertEquals(0, redisZset.size());

		RedisCollectionPopulatingMessageHandler handler =
				new RedisCollectionPopulatingMessageHandler(jcf, new LiteralExpression(key));

		handler.setCollectionType(CollectionType.ZSET);

		Map<President, Double> presidents = new HashMap<President, Double>();
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

		assertEquals(13, redisZset.size());

		Set<TypedTuple<President>> entries = redisZset.rangeByScoreWithScores(18, 19);
		assertEquals(6, entries.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	@RedisAvailable
	public void testZsetWithMapPayloadPojoNotParsedHeaderKey(){
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		String key = "presidents";
		RedisZSet<Map<President, Double>> redisZset =
				new DefaultRedisZSet<Map<President, Double>>(key,
						(RedisOperations<String, Map<President, Double>>) this.initTemplate(jcf, new RedisTemplate<String, Map<President, Double>>()));

		assertEquals(0, redisZset.size());

		RedisCollectionPopulatingMessageHandler handler =
				new RedisCollectionPopulatingMessageHandler(jcf, new LiteralExpression(key));

		handler.setCollectionType(CollectionType.ZSET);
		handler.setParsePayload(false);

		Map<President, Double> presidents = new HashMap<President, Double>();
		presidents.put(new President("John Adams"), 18D);

		presidents.put(new President("Barack Obama"), 21D);
		presidents.put(new President("Thomas Jefferson"), 19D);

		Message<Map<President, Double>> message = MessageBuilder.withPayload(presidents).setHeader("redis_key", key).build();
		handler.handleMessage(message);

		assertEquals(1, redisZset.size());
	}

	private RedisTemplate<?,?> initTemplate(RedisConnectionFactory rcf, RedisTemplate<?, ?> redisTemplate){
		redisTemplate.setConnectionFactory(rcf);
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
		return redisTemplate;
	}

	private static class President implements Serializable{
		private static final long serialVersionUID = 1L;
		private String name;

		public President(String name) {
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
