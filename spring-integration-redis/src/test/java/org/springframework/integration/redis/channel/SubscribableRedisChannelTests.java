/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.integration.redis.channel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.ReflectionUtils;
/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.0
 */
public class SubscribableRedisChannelTests extends RedisAvailableTests {


	@Test
	@RedisAvailable
	public void pubSubChannelTest() throws Exception {
		RedisConnectionFactory connectionFactory = this.getConnectionFactoryForTest();

		SubscribableRedisChannel channel = new SubscribableRedisChannel(connectionFactory, "si.test.channel");
		channel.setBeanFactory(mock(BeanFactory.class));
		channel.afterPropertiesSet();
		channel.start();

		this.awaitContainerSubscribed(TestUtils.getPropertyValue(channel, "container",
				RedisMessageListenerContainer.class));

		final CountDownLatch latch = new CountDownLatch(3);
		MessageHandler handler = new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				latch.countDown();
			}
		};
		channel.subscribe(handler);

		channel.send(new GenericMessage<String>("1"));
		channel.send(new GenericMessage<String>("2"));
		channel.send(new GenericMessage<String>("3"));
		assertTrue(latch.await(20, TimeUnit.SECONDS));
	}

	@Test
	@RedisAvailable
	public void dispatcherHasNoSubscribersTest() throws Exception{
		RedisConnectionFactory connectionFactory = this.getConnectionFactoryForTest();

		SubscribableRedisChannel channel = new SubscribableRedisChannel(connectionFactory, "si.test.channel.no.subs");
		channel.setBeanName("dhnsChannel");
		channel.setBeanFactory(mock(BeanFactory.class));
		channel.afterPropertiesSet();

		RedisMessageListenerContainer container = TestUtils.getPropertyValue(
				channel, "container", RedisMessageListenerContainer.class);
		@SuppressWarnings("unchecked")
		Map<?, Set<MessageListenerAdapter>> channelMapping = (Map<?, Set<MessageListenerAdapter>>) TestUtils
				.getPropertyValue(container, "channelMapping");
		MessageListenerAdapter listener = channelMapping.entrySet().iterator().next().getValue().iterator().next();
		Object delegate = TestUtils.getPropertyValue(listener, "delegate");
		try {
			ReflectionUtils.findMethod(delegate.getClass(), "handleMessage", String.class).invoke(delegate,
					"Hello, world!");
			fail("Exception expected");
		}
		catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			assertNotNull(cause);
			assertEquals("Dispatcher has no subscribers for redis-channel 'si.test.channel.no.subs' (dhnsChannel).",
					cause.getMessage());
		}

	}
}
