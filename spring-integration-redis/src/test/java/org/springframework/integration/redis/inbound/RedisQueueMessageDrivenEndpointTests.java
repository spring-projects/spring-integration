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

package org.springframework.integration.redis.inbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.Date;

import org.hamcrest.Matchers;
import org.junit.Test;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ErrorMessage;

/**
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @since 3.0
 */
public class RedisQueueMessageDrivenEndpointTests extends RedisAvailableTests {

	@Test
	@RedisAvailable
	@SuppressWarnings("unchecked")
	public void testInt3014Default() throws Exception {

		String queueName = "si.test.redisQueueInboundChannelAdapterTests";

		RedisConnectionFactory connectionFactory = this.getConnectionFactoryForTest();

		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<String, Object>();
		redisTemplate.setConnectionFactory(connectionFactory);
		redisTemplate.setEnableDefaultSerializer(false);
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
		redisTemplate.afterPropertiesSet();

		String payload = "testing";

		redisTemplate.boundListOps(queueName).leftPush(payload);

		Date payload2 = new Date();

		redisTemplate.boundListOps(queueName).leftPush(payload2);

		PollableChannel channel = new QueueChannel();

		RedisQueueMessageDrivenEndpoint endpoint = new RedisQueueMessageDrivenEndpoint(queueName, connectionFactory);
		endpoint.setOutputChannel(channel);
		endpoint.setReceiveTimeout(1000);
		endpoint.afterPropertiesSet();
		endpoint.start();

		Message<Object> receive = (Message<Object>) channel.receive(2000);
		assertNotNull(receive);
		assertEquals(payload, receive.getPayload());

		receive = (Message<Object>) channel.receive(2000);
		assertNotNull(receive);
		assertEquals(payload2, receive.getPayload());

		endpoint.stop();
		this.waitUntilListening(endpoint);
	}

	@Test
	@RedisAvailable
	@SuppressWarnings("unchecked")
	public void testInt3014ExpectMessageTrue() throws Exception {

		final String queueName = "si.test.redisQueueInboundChannelAdapterTests2";

		RedisConnectionFactory connectionFactory = this.getConnectionFactoryForTest();

		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<String, Object>();
		redisTemplate.setConnectionFactory(connectionFactory);
		redisTemplate.setEnableDefaultSerializer(false);
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
		redisTemplate.afterPropertiesSet();

		Message<?> message = MessageBuilder.withPayload("testing").build();

		redisTemplate.boundListOps(queueName).leftPush(message);

		redisTemplate.boundListOps(queueName).leftPush("test");

		PollableChannel channel = new QueueChannel();

		PollableChannel errorChannel = new QueueChannel();

		RedisQueueMessageDrivenEndpoint endpoint = new RedisQueueMessageDrivenEndpoint(queueName, connectionFactory);
		endpoint.setExpectMessage(true);
		endpoint.setOutputChannel(channel);
		endpoint.setErrorChannel(errorChannel);
		endpoint.setReceiveTimeout(1000);
		endpoint.afterPropertiesSet();
		endpoint.start();

		Message<Object> receive = (Message<Object>) channel.receive(2000);
		assertNotNull(receive);

		assertEquals(message, receive);

		receive = (Message<Object>) errorChannel.receive(2000);
		assertNotNull(receive);
		assertThat(receive, Matchers.instanceOf(ErrorMessage.class));
		assertThat(receive.getPayload(), Matchers.instanceOf(MessagingException.class));
		assertThat(((Exception) receive.getPayload()).getMessage(), Matchers.containsString("Deserialization of Message failed."));
		assertThat(((Exception) receive.getPayload()).getCause(), Matchers.instanceOf(ClassCastException.class));
		assertThat(((Exception) receive.getPayload()).getCause().getMessage(),
				Matchers.containsString("java.lang.String cannot be cast to org.springframework.messaging.Message"));


		endpoint.stop();
		this.waitUntilListening(endpoint);
	}


	public void waitUntilListening(RedisQueueMessageDrivenEndpoint endpoint) throws Exception {
		int n = 0;
		while (endpoint.isListening()) {
			Thread.sleep(100);
			if (n++ > 100) {
				throw new Exception("RedisQueueMessageDrivenEndpoint failed to stop.");
			}
		}

	}

}
