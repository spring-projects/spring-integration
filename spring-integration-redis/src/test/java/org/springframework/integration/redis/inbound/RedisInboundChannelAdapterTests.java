/*
 * Copyright 2007-2014 the original author or authors
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

package org.springframework.integration.redis.inbound;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.hamcrest.Matchers;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @since 2.1
 */
public class RedisInboundChannelAdapterTests extends RedisAvailableTests {

	@Test
	@RedisAvailable
	public void testRedisInboundChannelAdapter() throws Exception {
		for (int iteration = 0; iteration < 10; iteration ++) {
			testRedisInboundChannelAdapterGuts(iteration);
		}
	}

	private void testRedisInboundChannelAdapterGuts(int iteration) throws Exception {
		int numToTest = 10;
		String redisChannelName = "testRedisInboundChannelAdapterChannel";
		QueueChannel channel = new QueueChannel();

		RedisConnectionFactory connectionFactory = this.getConnectionFactoryForTest();

		RedisInboundChannelAdapter adapter = new RedisInboundChannelAdapter(connectionFactory);
		adapter.setTopics(redisChannelName);
		adapter.setOutputChannel(channel);
		adapter.setBeanFactory(mock(BeanFactory.class));
		adapter.afterPropertiesSet();
		adapter.start();

		this.awaitContainerSubscribed(TestUtils.getPropertyValue(adapter, "container",
				RedisMessageListenerContainer.class));

		StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);
		redisTemplate.afterPropertiesSet();
		for (int i = 0; i < numToTest; i++) {
			String message = "test-" + i + " iteration " + iteration;
			redisTemplate.convertAndSend(redisChannelName, message);
		}
		int counter = 0;
		for (int i = 0; i < numToTest; i++) {
			Message<?> message = channel.receive(5000);
			if (message == null){
				throw new RuntimeException("Failed to receive message # " + i + " iteration " + iteration);
			}
			assertNotNull(message);
			assertTrue(message.getPayload().toString().startsWith("test-"));
			counter++;
		}
		assertEquals(numToTest, counter);
		adapter.stop();

		redisChannelName = "testRedisBytesInboundChannelAdapterChannel";

		adapter.setTopics(redisChannelName);
		adapter.setSerializer(null);
		adapter.afterPropertiesSet();
		adapter.start();

		this.awaitContainerSubscribed(TestUtils.getPropertyValue(adapter, "container",
				RedisMessageListenerContainer.class));

		RedisTemplate<?, ?> template = new RedisTemplate<Object, Object>();
		template.setConnectionFactory(connectionFactory);
		template.setEnableDefaultSerializer(false);
		template.afterPropertiesSet();

		for (int i = 0; i < numToTest; i++) {
			String message = "test-" + i + " iteration " + iteration;
			template.convertAndSend(redisChannelName, message.getBytes());
		}

		counter = 0;
		for (int i = 0; i < numToTest; i++) {
			Message<?> message = channel.receive(5000);
			if (message == null){
				throw new RuntimeException("Failed to receive message # " + i + " iteration " + iteration);
			}
			assertNotNull(message);
			Object payload = message.getPayload();
			assertThat(payload, Matchers.instanceOf(byte[].class));

			assertTrue(new String((byte[]) payload).startsWith("test-"));
			counter++;
		}

		assertEquals(numToTest, counter);
		adapter.stop();
	}

}
