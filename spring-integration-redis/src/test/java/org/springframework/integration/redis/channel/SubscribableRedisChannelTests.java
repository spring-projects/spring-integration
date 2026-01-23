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

package org.springframework.integration.redis.channel;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.integration.redis.RedisContainerTest;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Artem Vozhdayenko
 * @author Glenn Renfro
 *
 * @since 2.0
 */
class SubscribableRedisChannelTests implements RedisContainerTest {

	private static RedisConnectionFactory redisConnectionFactory;

	@BeforeAll
	static void setupConnection() {
		redisConnectionFactory = RedisContainerTest.connectionFactory();
	}

	@Test
	void pubSubChannelTest() throws Exception {

		SubscribableRedisChannel channel = new SubscribableRedisChannel(redisConnectionFactory, "si.test.channel");
		channel.setBeanFactory(mock(BeanFactory.class));
		channel.afterPropertiesSet();
		channel.start();

		RedisContainerTest.awaitContainerSubscribed(TestUtils.getPropertyValue(channel, "container"));

		final CountDownLatch latch = new CountDownLatch(3);
		MessageHandler handler = message -> latch.countDown();
		channel.subscribe(handler);

		channel.send(new GenericMessage<>("1"));
		channel.send(new GenericMessage<>("2"));
		channel.send(new GenericMessage<>("3"));
		assertThat(latch.await(20, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void dispatcherHasNoSubscribersTest() throws Exception {

		SubscribableRedisChannel channel = new SubscribableRedisChannel(redisConnectionFactory, "si.test.channel.no.subs");
		channel.setBeanName("dhnsChannel");
		channel.setBeanFactory(mock(BeanFactory.class));
		channel.afterPropertiesSet();

		RedisMessageListenerContainer container = TestUtils.getPropertyValue(channel, "container");
		@SuppressWarnings("unchecked")
		Map<?, Set<MessageListenerAdapter>> channelMapping = (Map<?, Set<MessageListenerAdapter>>) TestUtils
				.getPropertyValue(container, "channelMapping");
		MessageListenerAdapter listener = channelMapping.entrySet().iterator().next().getValue().iterator().next();
		Object delegate = TestUtils.getPropertyValue(listener, "delegate");
		assertThatExceptionOfType(InvocationTargetException.class)
				.isThrownBy(() -> ReflectionUtils.findMethod(delegate.getClass(), "handleMessage", Object.class).invoke(delegate, "Hello, world!"))
				.havingCause()
				.withMessageContaining("Dispatcher has no subscribers for redis-channel 'si.test.channel.no.subs' (dhnsChannel).");
	}

}
