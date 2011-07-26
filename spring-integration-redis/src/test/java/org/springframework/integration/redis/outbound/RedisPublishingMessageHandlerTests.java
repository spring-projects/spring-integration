/*
 * Copyright 2007-2011 the original author or authors
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

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.Topic;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.integration.support.MessageBuilder;

/**
 * @author Mark Fisher
 * @since 2.1
 */
public class RedisPublishingMessageHandlerTests {

	@Test @Ignore
	public void testRedisPublishingMessageHandler() throws Exception {
		int numToTest = 100;
		String topic = "si.test.channel";
		final CountDownLatch latch = new CountDownLatch(numToTest);

		JedisConnectionFactory connectionFactory = new JedisConnectionFactory();
		connectionFactory.afterPropertiesSet();

		MessageListenerAdapter listener = new MessageListenerAdapter();
		listener.setDelegate(new Listener(latch));
		listener.setSerializer(new StringRedisSerializer());

		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.afterPropertiesSet();
		container.addMessageListener(listener, Collections.<Topic>singletonList(new ChannelTopic(topic)));
		container.start();
		Thread.sleep(500);

		final RedisPublishingMessageHandler handler = new RedisPublishingMessageHandler(connectionFactory);
		handler.setDefaultTopic(topic);
		for (int i = 0; i < numToTest; i++) {
			handler.handleMessage(MessageBuilder.withPayload("test-" + i).build());
		}
		latch.await(3, TimeUnit.SECONDS);
		assertEquals(0, latch.getCount());
	}


	private static class Listener {

		private final CountDownLatch latch;

		private Listener(CountDownLatch latch) {
			this.latch = latch;
		}

		@SuppressWarnings("unused")
		public void handleMessage(String s) {
			this.latch.countDown();
		}
	}

}
