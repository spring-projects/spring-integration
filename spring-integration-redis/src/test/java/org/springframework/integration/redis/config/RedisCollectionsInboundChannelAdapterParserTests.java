/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.redis.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.support.collections.RedisList;
import org.springframework.data.redis.support.collections.RedisZSet;
import org.springframework.integration.Message;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;

/**
 * @author Oleg Zhurakousky
 * @since 2.2
 */
public class RedisCollectionsInboundChannelAdapterParserTests extends RedisAvailableTests{

	@Test
	@RedisAvailable
	@SuppressWarnings("unchecked")
	public void testListInboundConfiguration() throws Exception{
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		this.prepareList(jcf);
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("list-inbound-adapter.xml", this.getClass());
		SourcePollingChannelAdapter spca = context.getBean("listAdapter", SourcePollingChannelAdapter.class);
		spca.start();
		QueueChannel redisChannel = context.getBean("redisChannel", QueueChannel.class);


		Message<RedisList<Object>> message = (Message<RedisList<Object>>) redisChannel.receive(1000);
		assertNotNull(message);
		assertEquals(13, message.getPayload().size());

		//poll again, should get the same stuff
		message = (Message<RedisList<Object>>) redisChannel.receive(1000);
		assertNotNull(message);
		assertEquals(13, message.getPayload().size());
		context.close();
	}

	@Test
	@RedisAvailable
	@SuppressWarnings("unchecked")
	public void testListInboundConfigurationWithSynchronization() throws Exception{
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		this.prepareList(jcf);
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("list-inbound-adapter.xml", this.getClass());
		SourcePollingChannelAdapter spca = context.getBean("listAdapterWithSynchronization", SourcePollingChannelAdapter.class);
		spca.start();
		QueueChannel redisChannel = context.getBean("redisChannel", QueueChannel.class);


		Message<RedisList<Object>> message = (Message<RedisList<Object>>) redisChannel.receive(1000);
		assertNotNull(message);
		assertEquals(13, message.getPayload().size());

		//poll again, should get nothing since the collection was removed during synchronization
		message = (Message<RedisList<Object>>) redisChannel.receive(1000);
		assertNull(message);

		spca.stop();
		context.close();
	}

	@Test
	@RedisAvailable
	@SuppressWarnings("unchecked")
	public void testZsetInboundConfiguration(){
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		this.prepareZset(jcf);
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("zset-inbound-adapter.xml", this.getClass());
		SourcePollingChannelAdapter zsetAdapterNoScore =
				context.getBean("zsetAdapterNoScore", SourcePollingChannelAdapter.class);
		zsetAdapterNoScore.start();

		QueueChannel redisChannel = context.getBean("redisChannel", QueueChannel.class);

		Message<RedisZSet<Object>> message = (Message<RedisZSet<Object>>) redisChannel.receive(1000);
		assertNotNull(message);
		assertEquals(13, message.getPayload().size());

		//poll again, should get the same stuff
		message = (Message<RedisZSet<Object>>) redisChannel.receive(1000);
		assertNotNull(message);
		assertEquals(13, message.getPayload().size());

		zsetAdapterNoScore.stop();
		context.close();
	}

	@Test
	@RedisAvailable
	@SuppressWarnings("unchecked")
	public void testZsetInboundConfigurationWithScoreRange(){
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		this.prepareZset(jcf);
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("zset-inbound-adapter.xml", this.getClass());
		SourcePollingChannelAdapter zsetAdapterWithScoreRange =
				context.getBean("zsetAdapterWithScoreRange", SourcePollingChannelAdapter.class);
		zsetAdapterWithScoreRange.start();

		QueueChannel redisChannel = context.getBean("redisChannel", QueueChannel.class);

		Message<RedisZSet<Object>> message = (Message<RedisZSet<Object>>) redisChannel.receive(1000);
		assertNotNull(message);
		assertEquals(11, message.getPayload().rangeByScore(18, 20).size());

		//poll again, should get the same stuff
		message = (Message<RedisZSet<Object>>) redisChannel.receive(1000);
		assertNotNull(message);
		assertEquals(11, message.getPayload().rangeByScore(18, 20).size());

		zsetAdapterWithScoreRange.stop();
		context.close();
	}

	@Test
	@RedisAvailable
	@SuppressWarnings("unchecked")
	public void testZsetInboundConfigurationWithSingleScore(){
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		this.prepareZset(jcf);
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("zset-inbound-adapter.xml", this.getClass());
		SourcePollingChannelAdapter zsetAdapterWithSingleScore =
				context.getBean("zsetAdapterWithSingleScore", SourcePollingChannelAdapter.class);
		zsetAdapterWithSingleScore.start();

		QueueChannel redisChannel = context.getBean("redisChannel", QueueChannel.class);

		Message<RedisZSet<Object>> message = (Message<RedisZSet<Object>>) redisChannel.receive(1000);
		assertNotNull(message);
		assertEquals(2, message.getPayload().rangeByScore(18, 18).size());

		//poll again, should get the same stuff
		message = (Message<RedisZSet<Object>>) redisChannel.receive(1000);
		assertNotNull(message);
		assertEquals(2, message.getPayload().rangeByScore(18, 18).size());

		zsetAdapterWithSingleScore.stop();
		context.close();
	}

	@Test
	@RedisAvailable
	@SuppressWarnings("unchecked")
	public void testZsetInboundConfigurationWithSingleScoreAndSynchronization() throws Exception{
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		this.prepareZset(jcf);
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("zset-inbound-adapter.xml", this.getClass());
		SourcePollingChannelAdapter zsetAdapterWithSingleScoreAndSynchronization =
				context.getBean("zsetAdapterWithSingleScoreAndSynchronization", SourcePollingChannelAdapter.class);

		SourcePollingChannelAdapter zsetAdapterNoScore =
				context.getBean("zsetAdapterNoScore", SourcePollingChannelAdapter.class);

		QueueChannel redisChannel = context.getBean("redisChannel", QueueChannel.class);
		QueueChannel otherRedisChannel = context.getBean("otherRedisChannel", QueueChannel.class);
		// get all 13 presidents
		zsetAdapterNoScore.start();

		Message<RedisZSet<Object>> message = (Message<RedisZSet<Object>>) redisChannel.receive(1000);
		assertNotNull(message);
		assertEquals(13, message.getPayload().size());

		zsetAdapterNoScore.stop();
		Thread.sleep(1000);
		// get only presidents for 18th century
		zsetAdapterWithSingleScoreAndSynchronization.start();

		message = (Message<RedisZSet<Object>>) otherRedisChannel.receive(1000);
		assertNotNull(message);
		assertEquals(2, message.getPayload().rangeByScore(18, 18).size());
		//message.getPayload().remo
		zsetAdapterWithSingleScoreAndSynchronization.stop();
		Thread.sleep(1000);

		// ... however other elements are still available 13-2=11
		zsetAdapterNoScore.start();
		message = (Message<RedisZSet<Object>>) redisChannel.receive(1000);
		assertNotNull(message);
		assertEquals(11, message.getPayload().size());

		zsetAdapterNoScore.stop();
		context.close();
	}
}
