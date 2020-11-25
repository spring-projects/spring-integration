/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.redis.config;

import static org.assertj.core.api.Assertions.assertThat;

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
	@RedisAvailable
	public void testPubSubChannelConfig() {
		RedisConnectionFactory connectionFactory =
				TestUtils.getPropertyValue(this.redisChannel, "connectionFactory", RedisConnectionFactory.class);
		RedisSerializer<?> redisSerializer = TestUtils.getPropertyValue(redisChannel, "serializer",
				RedisSerializer.class);
		assertThat(this.context.getBean("redisConnectionFactory")).isEqualTo(connectionFactory);
		assertThat(this.context.getBean("redisSerializer")).isEqualTo(redisSerializer);
		assertThat(TestUtils.getPropertyValue(redisChannel, "topicName")).isEqualTo("si.test.topic.parser");
		assertThat(TestUtils.getPropertyValue(
				TestUtils.getPropertyValue(this.redisChannel, "dispatcher"), "maxSubscribers", Integer.class)
				.intValue()).isEqualTo(Integer.MAX_VALUE);

		assertThat(TestUtils.getPropertyValue(this.redisChannelWithSubLimit, "dispatcher.maxSubscribers",
				Integer.class)
				.intValue()).isEqualTo(1);
		Object mbf = this.context.getBean(IntegrationUtils.INTEGRATION_MESSAGE_BUILDER_FACTORY_BEAN_NAME);
		assertThat(TestUtils.getPropertyValue(this.redisChannelWithSubLimit, "messageBuilderFactory")).isSameAs(mbf);
	}

	@Test
	@RedisAvailable
	public void testPubSubChannelUsage() throws Exception {
		this.awaitContainerSubscribed(TestUtils.getPropertyValue(this.redisChannel, "container",
				RedisMessageListenerContainer.class));

		final Message<?> m = new GenericMessage<>("Hello Redis");

		final CountDownLatch latch = new CountDownLatch(1);
		this.redisChannel.subscribe(message -> {
			assertThat(message.getPayload()).isEqualTo(m.getPayload());
			latch.countDown();
		});

		this.redisChannel.send(m);

		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
	}

}
