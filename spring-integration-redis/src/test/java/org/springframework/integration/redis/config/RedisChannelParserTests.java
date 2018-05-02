/*
 * Copyright 2002-2018 the original author or authors.
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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.integration.redis.channel.SubscribableRedisChannel;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class RedisChannelParserTests extends RedisAvailableTests {

	@Autowired
	private SubscribableRedisChannel redisChannel;

	@Autowired
	private SubscribableRedisChannel redisChannelWithSubLimit;

	@Autowired
	private ApplicationContext context;

	@Before
	public void setup() {
		this.redisChannel.start();
		this.redisChannelWithSubLimit.start();
	}

	@After
	public void tearDown() {
		this.redisChannel.stop();
		this.redisChannelWithSubLimit.stop();
	}

	@Test
	public void testPubSubChannelConfig() {
		RedisConnectionFactory connectionFactory =
				TestUtils.getPropertyValue(this.redisChannel, "connectionFactory", RedisConnectionFactory.class);
		RedisSerializer<?> redisSerializer = TestUtils.getPropertyValue(redisChannel, "serializer", RedisSerializer.class);
		assertEquals(connectionFactory, this.context.getBean("redisConnectionFactory"));
		assertEquals(redisSerializer, this.context.getBean("redisSerializer"));
		assertEquals("si.test.topic.parser", TestUtils.getPropertyValue(redisChannel, "topicName"));
		assertEquals(Integer.MAX_VALUE, TestUtils.getPropertyValue(
				TestUtils.getPropertyValue(this.redisChannel, "dispatcher"), "maxSubscribers", Integer.class).intValue());

		assertEquals(1,
				TestUtils.getPropertyValue(this.redisChannelWithSubLimit, "dispatcher.maxSubscribers", Integer.class).intValue());
		Object mbf = this.context.getBean(IntegrationUtils.INTEGRATION_MESSAGE_BUILDER_FACTORY_BEAN_NAME);
		assertSame(mbf, TestUtils.getPropertyValue(this.redisChannelWithSubLimit, "messageBuilderFactory"));
	}

	@Test
	@RedisAvailable
	public void testPubSubChannelUsage() throws Exception {
		this.awaitContainerSubscribed(TestUtils.getPropertyValue(this.redisChannel, "container",
				RedisMessageListenerContainer.class));

		final Message<?> m = new GenericMessage<>("Hello Redis");

		final CountDownLatch latch = new CountDownLatch(1);
		this.redisChannel.subscribe(message -> {
			assertEquals(m.getPayload(), message.getPayload());
			latch.countDown();
		});

		this.redisChannel.send(m);

		assertTrue(latch.await(10, TimeUnit.SECONDS));
	}

}
