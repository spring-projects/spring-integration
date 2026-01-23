/*
 * Copyright 2007-present the original author or authors.
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

package org.springframework.integration.redis.inbound;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.redis.RedisContainerTest;
import org.springframework.integration.redis.support.RedisHeaders;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 * @author Artem Vozhdayenko
 * @author Glenn Renfro
 *
 * @since 2.1
 */
class RedisInboundChannelAdapterTests implements RedisContainerTest {

	private static RedisConnectionFactory redisConnectionFactory;

	@BeforeAll
	static void setupConnection() {
		redisConnectionFactory = RedisContainerTest.connectionFactory();
	}

	@Test
	void testRedisInboundChannelAdapter() throws Exception {
		for (int iteration = 0; iteration < 10; iteration++) {
			testRedisInboundChannelAdapterGuts(iteration);
		}
	}

	private void testRedisInboundChannelAdapterGuts(int iteration) throws Exception {
		int numToTest = 10;
		String redisChannelName = "testRedisInboundChannelAdapterChannel";
		QueueChannel channel = new QueueChannel();

		RedisConnectionFactory connectionFactory = redisConnectionFactory;

		RedisInboundChannelAdapter adapter = new RedisInboundChannelAdapter(connectionFactory);
		adapter.setTopics(redisChannelName);
		adapter.setOutputChannel(channel);
		adapter.setBeanFactory(mock(BeanFactory.class));
		adapter.afterPropertiesSet();
		adapter.start();

		StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);
		redisTemplate.afterPropertiesSet();

		RedisContainerTest.awaitFullySubscribed(TestUtils.<RedisMessageListenerContainer>getPropertyValue(adapter, "container"),
				redisTemplate, redisChannelName, channel, "foo");

		for (int i = 0; i < numToTest; i++) {
			String message = "test-" + i + " iteration " + iteration;
			redisTemplate.convertAndSend(redisChannelName, message);
		}
		int counter = 0;
		for (int i = 0; i < numToTest; i++) {
			Message<?> message = channel.receive(10000);
			if (message == null) {
				throw new RuntimeException("Failed to receive message # " + i + " iteration " + iteration);
			}
			assertThat(message).isNotNull();
			assertThat(message.getPayload().toString()).startsWith("test-");
			assertThat(message.getHeaders())
					.containsEntry(RedisHeaders.MESSAGE_SOURCE, "testRedisInboundChannelAdapterChannel");
			counter++;
		}
		assertThat(counter).isEqualTo(numToTest);
		adapter.stop();

		redisChannelName = "testRedisBytesInboundChannelAdapterChannel";

		adapter.setTopics(redisChannelName);
		adapter.setSerializer(null);
		adapter.afterPropertiesSet();
		adapter.start();

		RedisTemplate<?, ?> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);
		template.setEnableDefaultSerializer(false);
		template.afterPropertiesSet();

		RedisContainerTest.awaitFullySubscribed(TestUtils.<RedisMessageListenerContainer>getPropertyValue(adapter, "container"),
				template, redisChannelName, channel, "foo".getBytes());

		for (int i = 0; i < numToTest; i++) {
			String message = "test-" + i + " iteration " + iteration;
			template.convertAndSend(redisChannelName, message.getBytes());
		}

		counter = 0;
		for (int i = 0; i < numToTest; i++) {
			Message<?> message = channel.receive(10000);
			if (message == null) {
				throw new RuntimeException("Failed to receive message # " + i + " iteration " + iteration);
			}
			assertThat(message).isNotNull();
			Object payload = message.getPayload();
			assertThat(payload).isInstanceOf(byte[].class);

			assertThat(new String((byte[]) payload)).startsWith("test-");
			counter++;
		}

		assertThat(counter).isEqualTo(numToTest);
		adapter.stop();
	}

}
