/*
 * Copyright 2002-present the original author or authors.
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.integration.redis.RedisContainerTest;
import org.springframework.integration.redis.channel.SubscribableRedisChannel;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Artem Vozhdayenko
 * @author Glenn Renfro
 */
@SpringJUnitConfig
@DirtiesContext
class RedisChannelParserTests implements RedisContainerTest {

	@Autowired
	private SubscribableRedisChannel redisChannel;

	@Autowired
	private SubscribableRedisChannel redisChannelWithSubLimit;

	@Autowired
	private ApplicationContext context;

	@BeforeEach
	public void setup() {
		this.redisChannel.start();
		this.redisChannelWithSubLimit.start();
	}

	@AfterEach
	public void tearDown() {
		this.redisChannel.stop();
		this.redisChannelWithSubLimit.stop();
	}

	@Test
	void testPubSubChannelConfig() {
		RedisConnectionFactory connectionFactory =
				TestUtils.<RedisConnectionFactory>getPropertyValue(this.redisChannel, "connectionFactory");
		RedisSerializer<?> redisSerializer = TestUtils.getPropertyValue(redisChannel, "serializer");
		assertThat(this.context.getBean("redisConnectionFactory")).isEqualTo(connectionFactory);
		assertThat(this.context.getBean("redisSerializer")).isEqualTo(redisSerializer);
		assertThat(TestUtils.<String>getPropertyValue(redisChannel, "topicName")).isEqualTo("si.test.topic.parser");
		assertThat(TestUtils.<Integer>getPropertyValue(
						TestUtils.getPropertyValue(this.redisChannel, "dispatcher"), "maxSubscribers")
				.intValue()).isEqualTo(Integer.MAX_VALUE);

		assertThat(TestUtils.<Integer>getPropertyValue(this.redisChannelWithSubLimit, "dispatcher.maxSubscribers")
				.intValue()).isEqualTo(1);
		Object mbf = this.context.getBean(IntegrationUtils.INTEGRATION_MESSAGE_BUILDER_FACTORY_BEAN_NAME);
		assertThat(TestUtils.<Object>getPropertyValue(this.redisChannelWithSubLimit, "messageBuilderFactory"))
				.isSameAs(mbf);
	}

	@Test
	void testPubSubChannelUsage() throws Exception {
		RedisContainerTest.awaitContainerSubscribed(TestUtils.<RedisMessageListenerContainer>getPropertyValue(this.redisChannel, "container"));

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
