/*
 * Copyright 2026-present the original author or authors.
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

package org.springframework.integration.redis.dsl;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.redis.RedisContainerTest;
import org.springframework.integration.redis.inbound.RedisInboundChannelAdapter;
import org.springframework.integration.redis.support.RedisHeaders;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Jiandong Ma
 *
 * @since 7.1
 */
@SpringJUnitConfig
@DirtiesContext
class RedisTests implements RedisContainerTest {

	@Autowired
	private RedisConnectionFactory connectionFactory;

	@Autowired
	private RedisInboundChannelAdapter inboundChannelAdapter;

	@Autowired
	private QueueChannel inboundChannelAdapterQueueChannel;

	@Test
	void testInboundChannelAdapter() throws Exception {
		StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);
		redisTemplate.afterPropertiesSet();
		RedisContainerTest.awaitFullySubscribed(TestUtils.getPropertyValue(inboundChannelAdapter, "container"),
				redisTemplate, Config.inboundChannelAdapterTopic, inboundChannelAdapterQueueChannel, "foo");

		// Given
		int numToTest = 10;
		for (int i = 0; i < numToTest; i++) {
			String message = "test-" + i;
			redisTemplate.convertAndSend(Config.inboundChannelAdapterTopic, message);
		}
		// When
		int counter = 0;
		for (int i = 0; i < numToTest; i++) {
			Message<?> message = inboundChannelAdapterQueueChannel.receive(10000);
			// Then
			assertThat(message)
					.isNotNull()
					.satisfies(msg -> {
						assertThat(msg.getHeaders()).containsEntry(RedisHeaders.MESSAGE_SOURCE, Config.inboundChannelAdapterTopic);
						assertThat(msg.getPayload().toString()).startsWith("test-");
					});
			counter++;
		}
		assertThat(counter).isEqualTo(numToTest);
	}

	@Configuration
	@EnableIntegration
	public static class Config {

		static String inboundChannelAdapterTopic = "testDslInboundChannelAdapterTopic";

		@Bean
		public RedisConnectionFactory connectionFactory() {
			return RedisContainerTest.connectionFactory();
		}

		@Bean
		public IntegrationFlow inboundChannelAdapterFlow(RedisConnectionFactory redisConnectionFactory) {

			return IntegrationFlow.from(Redis
							.inboundChannelAdapter(redisConnectionFactory)
							.topics(inboundChannelAdapterTopic))
					.channel(c -> c.queue("inboundChannelAdapterQueueChannel"))
					.get();
		}

	}

}
