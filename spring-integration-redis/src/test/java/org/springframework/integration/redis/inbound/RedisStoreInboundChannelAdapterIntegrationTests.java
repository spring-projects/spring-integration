/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.redis.inbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.support.collections.RedisList;
import org.springframework.data.redis.support.collections.RedisZSet;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.SubscribableChannel;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gary Russell
 * @since 2.2
 */
public class RedisStoreInboundChannelAdapterIntegrationTests extends RedisAvailableTests {

	@Test
	@RedisAvailable
	@SuppressWarnings("unchecked")
	public void testListInboundConfiguration() throws Exception{
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();
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
		this.deletePresidents(jcf);
		context.close();
	}

	@Test
	@RedisAvailable
	@SuppressWarnings("unchecked")
	// synchronization commit renames the list
	public void testListInboundConfigurationWithSynchronization() throws Exception{
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		StringRedisTemplate template = this.createStringRedisTemplate(jcf);
		template.delete("bar");
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
		assertEquals(Long.valueOf(13), template.boundListOps("bar").size());
		template.delete("bar");

		spca.stop();
		context.close();
	}

	@Test
	@RedisAvailable
	// synchronization rollback renames the list
	public void testListInboundConfigurationWithSynchronizationAndRollback() throws Exception{
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		StringRedisTemplate template = this.createStringRedisTemplate(jcf);
		template.delete("baz");
		this.prepareList(jcf);
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("list-inbound-adapter.xml",
				this.getClass());
		SubscribableChannel fail = context.getBean("redisFailChannel", SubscribableChannel.class);
		final CountDownLatch latch = new CountDownLatch(1);
		fail.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				latch.countDown();
				throw new RuntimeException("Test Rollback");
			}
		});
		SourcePollingChannelAdapter spca = context.getBean("listAdapterWithSynchronizationAndRollback",
				SourcePollingChannelAdapter.class);
		spca.start();
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		int n = 0;
		while (n++ < 100 && template.keys("baz").size() == 0) {
			Thread.sleep(100);
		}
		assertTrue("Rename didn't occcur", n < 100);
		assertEquals(Long.valueOf(13), template.boundListOps("baz").size());
		template.delete("baz");

		spca.stop();
		context.close();
	}

	@Test
	@RedisAvailable
	@SuppressWarnings("unchecked")
	// synchronization commit renames the list
	public void testListInboundConfigurationWithSynchronizationAndTemplate() throws Exception{
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		StringRedisTemplate template = this.createStringRedisTemplate(jcf);
		template.delete("bar");
		this.prepareList(jcf);
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("list-inbound-adapter.xml", this.getClass());
		SourcePollingChannelAdapter spca = context.getBean("listAdapterWithSynchronizationAndRedisTemplate", SourcePollingChannelAdapter.class);
		spca.start();
		QueueChannel redisChannel = context.getBean("redisChannel", QueueChannel.class);

		Message<RedisList<Object>> message = (Message<RedisList<Object>>) redisChannel.receive(1000);
		assertNotNull(message);
		assertEquals(13, message.getPayload().size());

		//poll again, should get nothing since the collection was removed during synchronization
		message = (Message<RedisList<Object>>) redisChannel.receive(1000);
		assertNull(message);
		assertEquals(Long.valueOf(13), template.boundListOps("bar").size());
		template.delete("bar");

		spca.stop();
		context.close();
	}

	@Test
	@RedisAvailable
	@SuppressWarnings("unchecked")
	public void testZsetInboundAdapter() throws InterruptedException {
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		this.prepareZset(jcf);
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("zset-inbound-adapter.xml", this.getClass());

		//No Score test
		SourcePollingChannelAdapter zsetAdapterNoScore =
				context.getBean("zsetAdapterNoScore", SourcePollingChannelAdapter.class);
		zsetAdapterNoScore.start();

		QueueChannel redisChannel = context.getBean("redisChannel", QueueChannel.class);

		Message<RedisZSet<Object>> message = (Message<RedisZSet<Object>>) redisChannel.receive(2000);
		assertNotNull(message);
		assertEquals(13, message.getPayload().size());

		//poll again, should get the same stuff
		message = (Message<RedisZSet<Object>>) redisChannel.receive(2000);
		assertNotNull(message);
		assertEquals(13, message.getPayload().size());

		zsetAdapterNoScore.stop();


		//ScoreRange test
		SourcePollingChannelAdapter zsetAdapterWithScoreRange =
				context.getBean("zsetAdapterWithScoreRange", SourcePollingChannelAdapter.class);
		zsetAdapterWithScoreRange.start();

		message = (Message<RedisZSet<Object>>) redisChannel.receive(2000);
		assertNotNull(message);
		assertEquals(11, message.getPayload().rangeByScore(18, 20).size());

		//poll again, should get the same stuff
		message = (Message<RedisZSet<Object>>) redisChannel.receive(2000);
		assertNotNull(message);
		assertEquals(11, message.getPayload().rangeByScore(18, 20).size());

		zsetAdapterWithScoreRange.stop();


		//SingleScore test
		SourcePollingChannelAdapter zsetAdapterWithSingleScore =
				context.getBean("zsetAdapterWithSingleScore", SourcePollingChannelAdapter.class);
		zsetAdapterWithSingleScore.start();

		message = (Message<RedisZSet<Object>>) redisChannel.receive(2000);
		assertNotNull(message);
		assertEquals(2, message.getPayload().rangeByScore(18, 18).size());

		//poll again, should get the same stuff
		message = (Message<RedisZSet<Object>>) redisChannel.receive(2000);
		assertNotNull(message);
		assertEquals(2, message.getPayload().rangeByScore(18, 18).size());

		zsetAdapterWithSingleScore.stop();


		//SingleScoreAndSynchronization test
		SourcePollingChannelAdapter zsetAdapterWithSingleScoreAndSynchronization =
				context.getBean("zsetAdapterWithSingleScoreAndSynchronization", SourcePollingChannelAdapter.class);

		QueueChannel otherRedisChannel = context.getBean("otherRedisChannel", QueueChannel.class);

		// get all 13 presidents
		zsetAdapterNoScore.start();
		message = (Message<RedisZSet<Object>>) redisChannel.receive(2000);
		assertNotNull(message);
		assertEquals(13, message.getPayload().size());
		zsetAdapterNoScore.stop();

		// get only presidents for 18th century
		zsetAdapterWithSingleScoreAndSynchronization.start();
		message = (Message<RedisZSet<Object>>) otherRedisChannel.receive(2000);
		assertNotNull(message);
		assertEquals(2, message.getPayload().rangeByScore(18, 18).size());

		// ... however other elements are still available 13-2=11
		zsetAdapterNoScore.start();
		message = (Message<RedisZSet<Object>>) redisChannel.receive(2000);
		assertNotNull(message);

		int n = 0;
		while(n++ < 100 && message.getPayload().size() != 11) {
			Thread.sleep(100);
		}
		assertTrue(n < 100);

		zsetAdapterNoScore.stop();
		zsetAdapterWithSingleScoreAndSynchronization.stop();
		this.deletePresidents(jcf);

		context.close();
	}

}
