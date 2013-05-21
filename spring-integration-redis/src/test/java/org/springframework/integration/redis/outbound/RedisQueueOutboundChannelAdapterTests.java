/*
 * Copyright 2007-2013 the original author or authors
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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableRule;
import org.springframework.integration.redis.rules.RedisAvailableTests;
import org.springframework.integration.support.MessageBuilder;

/**
 * @author Gunnar Hillert
 * @since 3.0
 */
public class RedisQueueOutboundChannelAdapterTests extends RedisAvailableTests{

	@Test
	@RedisAvailable
	public void testRedisQueueOutboundChannelAdapter() throws Exception {

		final String queueName = "si.test.testRedisQueueOutboundChannelAdapter";

		JedisConnectionFactory connectionFactory = new JedisConnectionFactory();
		connectionFactory.setPort(RedisAvailableRule.REDIS_PORT);
		connectionFactory.afterPropertiesSet();

		final RedisQueueOutboundChannelAdapter handler = new RedisQueueOutboundChannelAdapter(queueName, connectionFactory);

		handler.handleMessage(MessageBuilder.withPayload("testing").build());

		StringRedisTemplate redisTemplate = new StringRedisTemplate();
		redisTemplate.setConnectionFactory(connectionFactory);
		redisTemplate.afterPropertiesSet();

		String result = redisTemplate.boundListOps(queueName).rightPop(5000, TimeUnit.MILLISECONDS);

		assertEquals("testing", result);

	}

	@Test
	@RedisAvailable
	public void testRedisQueueOutboundChannelAdapter2() throws Exception {

		final String queueName = "si.test.testRedisQueueOutboundChannelAdapter2";

		JedisConnectionFactory connectionFactory = new JedisConnectionFactory();
		connectionFactory.setPort(RedisAvailableRule.REDIS_PORT);
		connectionFactory.afterPropertiesSet();

		final RedisQueueOutboundChannelAdapter handler = new RedisQueueOutboundChannelAdapter(queueName, connectionFactory);
		handler.setExtractPayload(false);
		handler.handleMessage(MessageBuilder.withPayload("testing").build());

		StringRedisTemplate redisTemplate = new StringRedisTemplate();
		redisTemplate.setConnectionFactory(connectionFactory);
		redisTemplate.afterPropertiesSet();

		String result = redisTemplate.boundListOps(queueName).rightPop(5000, TimeUnit.MILLISECONDS);

		assertNotEquals("testing", result);
		assertTrue(result.startsWith("{\""));

	}

}
