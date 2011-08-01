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

import org.junit.Test;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.integration.Message;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Mark Fisher
 * @since 2.1
 */
public class RedisInboundChannelAdapterTests extends RedisAvailableTests{

	@Test 
	@RedisAvailable
	public void testRedisInboundChannelAdapter() throws Exception {
		int numToTest = 100;
		String redisChannelName = "si.test.channel";
		QueueChannel channel = new QueueChannel();

		JedisConnectionFactory connectionFactory = new JedisConnectionFactory();
		connectionFactory.setPort(7379);
		connectionFactory.afterPropertiesSet();

		RedisInboundChannelAdapter adapter = new RedisInboundChannelAdapter(connectionFactory);
		adapter.setTopics("si.test.channel");
		adapter.setOutputChannel(channel);
		adapter.afterPropertiesSet();
		adapter.start();

		StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);
		redisTemplate.afterPropertiesSet();
		for (int i = 0; i < numToTest; i++) {
			redisTemplate.convertAndSend(redisChannelName, "test-" + i);
		}
		int counter = 0;
		for (int i = 0; i < numToTest; i++) {
			Message<?> message = channel.receive(5000);
			assertNotNull(message);
			assertTrue(message.getPayload().toString().startsWith("test-"));
			counter++;
		}
		assertEquals(numToTest, counter);
	}

}
