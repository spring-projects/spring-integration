package org.springframework.integration.redis.config;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.support.collections.DefaultRedisList;
import org.springframework.data.redis.support.collections.RedisList;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;
import org.springframework.integration.support.MessageBuilder;

public class RedisCollectionsOutboundChannelAdapterParserTests extends RedisAvailableTests {

	@SuppressWarnings("unchecked")
	@Test
	@RedisAvailable
	public void testListWithKeyAsHeader(){
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisList<String> redisList =
				new DefaultRedisList<String>("pepboys",
						(RedisOperations<String, String>) this.initTemplate(jcf, new RedisTemplate<String, String>()));
		assertEquals(0, redisList.size());

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("list-outbound-adapter.xml", this.getClass());
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

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("list-outbound-adapter.xml", this.getClass());
		MessageChannel redisChannel = context.getBean("listWithKeyProvided", MessageChannel.class);
		List<String> pepboys = new ArrayList<String>();
		pepboys.add("Manny");
		pepboys.add("Moe");
		pepboys.add("Jack");
		Message<List<String>> message = MessageBuilder.withPayload(pepboys).build();
		redisChannel.send(message);

		assertEquals(3, redisList.size());
	}

	private RedisTemplate<?,?> initTemplate(RedisConnectionFactory rcf, RedisTemplate<?, ?> redisTemplate){
		redisTemplate.setConnectionFactory(rcf);
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
		return redisTemplate;
	}
}
