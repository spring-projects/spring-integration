/*
 * Copyright 2013 the original author or authors
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
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @since 3.0
 */
public class RedisQueueOutboundChannelAdapterTests extends RedisAvailableTests {

	@Test
	@RedisAvailable
	public void testInt3015Default() throws Exception {

		final String queueName = "si.test.testRedisQueueOutboundChannelAdapter";

		RedisConnectionFactory connectionFactory = this.getConnectionFactoryForTest();

		final RedisQueueOutboundChannelAdapter handler = new RedisQueueOutboundChannelAdapter(queueName, connectionFactory);

		String payload = "testing";
		handler.handleMessage(MessageBuilder.withPayload(payload).build());

		RedisTemplate<String, ?> redisTemplate = new StringRedisTemplate();
		redisTemplate.setConnectionFactory(connectionFactory);
		redisTemplate.afterPropertiesSet();

		Object result = redisTemplate.boundListOps(queueName).rightPop(5000, TimeUnit.MILLISECONDS);
		assertNotNull(result);

		assertEquals(payload, result);

		Date payload2 = new Date();
		handler.handleMessage(MessageBuilder.withPayload(payload2).build());

		RedisTemplate<String, ?> redisTemplate2 = new RedisTemplate<String, Object>();
		redisTemplate2.setConnectionFactory(connectionFactory);
		redisTemplate2.setEnableDefaultSerializer(false);
		redisTemplate2.setKeySerializer(new StringRedisSerializer());
		redisTemplate2.setValueSerializer(new JdkSerializationRedisSerializer());
		redisTemplate2.afterPropertiesSet();

		Object result2 = redisTemplate2.boundListOps(queueName).rightPop(5000, TimeUnit.MILLISECONDS);
		assertNotNull(result2);

		assertEquals(payload2, result2);
	}

	@Test
	@RedisAvailable
	public void testInt3015ExtractPayloadFalse() throws Exception {

		final String queueName = "si.test.testRedisQueueOutboundChannelAdapter2";

		RedisConnectionFactory connectionFactory = this.getConnectionFactoryForTest();

		final RedisQueueOutboundChannelAdapter handler = new RedisQueueOutboundChannelAdapter(queueName, connectionFactory);
		handler.setExtractPayload(false);

		Message<String> message = MessageBuilder.withPayload("testing").build();
		handler.handleMessage(message);

		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<String, Object>();
		redisTemplate.setConnectionFactory(connectionFactory);
		redisTemplate.setEnableDefaultSerializer(false);
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
		redisTemplate.afterPropertiesSet();

		Object result = redisTemplate.boundListOps(queueName).rightPop(5000, TimeUnit.MILLISECONDS);
		assertNotNull(result);

		assertEquals(message, result);

	}

	@Test
	@RedisAvailable
	public void testInt3015ExplicitSerializer() throws Exception {

		final String queueName = "si.test.testRedisQueueOutboundChannelAdapter2";

		RedisConnectionFactory connectionFactory = this.getConnectionFactoryForTest();

		final RedisQueueOutboundChannelAdapter handler = new RedisQueueOutboundChannelAdapter(queueName, connectionFactory);
		handler.setSerializer(new JacksonJsonRedisSerializer<Object>(Object.class));

		RedisTemplate<String, ?> redisTemplate = new StringRedisTemplate();
		redisTemplate.setConnectionFactory(connectionFactory);
		redisTemplate.afterPropertiesSet();

		handler.handleMessage(new GenericMessage<Object>(Arrays.asList("foo", "bar", "baz")));

		Object result = redisTemplate.boundListOps(queueName).rightPop(5000, TimeUnit.MILLISECONDS);
		assertNotNull(result);

		assertEquals("[\"foo\",\"bar\",\"baz\"]", result);

		handler.handleMessage(new GenericMessage<Object>("test"));

		result = redisTemplate.boundListOps(queueName).rightPop(5000, TimeUnit.MILLISECONDS);
		assertNotNull(result);

		assertEquals("\"test\"", result);
	}

}
