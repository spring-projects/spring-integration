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

package org.springframework.integration.redis.inbound;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.integration.Message;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;
import org.springframework.integration.test.util.TestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Mark Fisher
 * @since 2.1
 */
public class RedisInboundChannelAdapterTests extends RedisAvailableTests{

	private final Log logger = LogFactory.getLog(this.getClass());

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

		JedisConnectionFactory connectionFactory = new JedisConnectionFactory();
		connectionFactory.setPort(7379);
		connectionFactory.afterPropertiesSet();

		RedisInboundChannelAdapter adapter = new RedisInboundChannelAdapter(connectionFactory);
		adapter.setTopics("testRedisInboundChannelAdapterChannel");
		adapter.setOutputChannel(channel);
		adapter.afterPropertiesSet();
		adapter.start();

		RedisMessageListenerContainer container = waitUntilSubscribed(adapter);

		StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);
		redisTemplate.afterPropertiesSet();
		for (int i = 0; i < numToTest; i++) {
			String message = "test-" + i + " iteration " + iteration;
			redisTemplate.convertAndSend(redisChannelName, message);
			logger.debug("Sent " + message);
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
		container.stop();
		connectionFactory.destroy();
	}

	/**
	 * Wait until the container has subscribed to the queue and return a
	 * reference to it, so we can stop it at the end of the test.
	 */
	protected RedisMessageListenerContainer waitUntilSubscribed(
			RedisInboundChannelAdapter adapter) throws Exception {
		RedisMessageListenerContainer container = (RedisMessageListenerContainer) TestUtils
				.getPropertyValue(adapter, "container");
		Object subscriptionTask = TestUtils.getPropertyValue(container, "subscriptionTask");
		RedisConnection connection = (RedisConnection) TestUtils
				.getPropertyValue(subscriptionTask, "connection");
		int n = 0;
		while (true) {
			if (n++ > 50) {
				fail("RMLC Failed to Subscribe");
			}
			if (connection.isSubscribed()) {
				logger.debug("Subscribed OK");
				break;
			}
			logger.debug("Waiting...");
			Thread.sleep(100);
		}
		Thread.sleep(100); // Wait a little longer due to race condition in connection.isSubscribed()
		return container;
	}

}
