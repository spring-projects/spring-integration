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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.Topic;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.redis.RedisContainerTest;
import org.springframework.integration.redis.inbound.RedisInboundChannelAdapter;
import org.springframework.integration.redis.support.RedisHeaders;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
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

	static final String TOPIC_FOR_INBOUND_CHANNEL_ADAPTER = "dslInboundChannelAdapterTopic";

	static final String TOPIC_FOR_OUTBOUND_CHANNEL_ADAPTER = "dslOutboundChannelAdapterTopic";

	@Autowired
	RedisConnectionFactory connectionFactory;

	@Autowired
	RedisInboundChannelAdapter inboundChannelAdapter;

	@Autowired
	QueueChannel inboundChannelAdapterQueueChannel;

	@Autowired
	@Qualifier("outboundChannelAdapterFlow.input")
	MessageChannel outboundChannelAdapterInputChannel;

	@Test
	void testInboundChannelAdapterFlow() throws Exception {
		StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);
		redisTemplate.afterPropertiesSet();
		RedisContainerTest.awaitFullySubscribed(TestUtils.getPropertyValue(inboundChannelAdapter, "container"),
				redisTemplate, TOPIC_FOR_INBOUND_CHANNEL_ADAPTER, inboundChannelAdapterQueueChannel, "subscribeTestMessage");

		// Given
		int numToTest = 10;
		for (int i = 0; i < numToTest; i++) {
			String message = "test-" + i;
			redisTemplate.convertAndSend(TOPIC_FOR_INBOUND_CHANNEL_ADAPTER, message);
		}
		// When & Then
		for (int i = 0; i < numToTest; i++) {
			Message<?> message = inboundChannelAdapterQueueChannel.receive(10000);
			assertThat(message)
					.isNotNull()
					.satisfies(msg -> {
						assertThat(msg.getHeaders()).containsEntry(RedisHeaders.MESSAGE_SOURCE, TOPIC_FOR_INBOUND_CHANNEL_ADAPTER);
						assertThat(msg.getPayload().toString()).startsWith("test-");
					});
		}
	}

	@Test
	void testOutboundChannelAdapterFlow() throws Exception {
		// Given
		int numToTest = 10;
		final CountDownLatch latch = new CountDownLatch(numToTest);
		List<org.springframework.data.redis.connection.Message> receivedMessages = new ArrayList<>();
		MessageListenerAdapter listener = new MessageListenerAdapter() {

			@Override
			public void onMessage(org.springframework.data.redis.connection.Message message, byte @Nullable [] pattern) {
				receivedMessages.add(message);
				latch.countDown();
			}
		};

		listener.afterPropertiesSet();

		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.afterPropertiesSet();
		container.addMessageListener(listener, Collections.<Topic>singletonList(new ChannelTopic(TOPIC_FOR_OUTBOUND_CHANNEL_ADAPTER)));
		container.start();

		RedisContainerTest.awaitContainerSubscribed(container);

		// When
		for (int i = 0; i < numToTest; i++) {
			outboundChannelAdapterInputChannel.send(MessageBuilder.withPayload("outbound-test-" + i).build());
		}

		// Then
		RedisSerializer<String> stringRedisSerializer = RedisSerializer.string();
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(receivedMessages)
				.hasSize(numToTest)
				.satisfies(msgList -> {
					msgList.forEach((msg) -> {
						assertThat(stringRedisSerializer.deserialize(msg.getChannel())).isEqualTo(TOPIC_FOR_OUTBOUND_CHANNEL_ADAPTER);
						assertThat(stringRedisSerializer.deserialize(msg.getBody())).startsWith("outbound-test-");
					});
				});
		container.stop();
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	static class Config {

		@Bean
		RedisConnectionFactory connectionFactory() {
			return RedisContainerTest.connectionFactory();
		}

		@Bean
		IntegrationFlow inboundChannelAdapterFlow(RedisConnectionFactory redisConnectionFactory) {

			return IntegrationFlow.from(Redis
							.inboundChannelAdapter(redisConnectionFactory)
							.topics(TOPIC_FOR_INBOUND_CHANNEL_ADAPTER))
					.channel(c -> c.queue("inboundChannelAdapterQueueChannel"))
					.get();
		}

		@Bean
		IntegrationFlow outboundChannelAdapterFlow(RedisConnectionFactory redisConnectionFactory) {
			return flow -> flow
					.handle(Redis.outboundChannelAdapter(redisConnectionFactory)
							.topicExpression(new LiteralExpression(TOPIC_FOR_OUTBOUND_CHANNEL_ADAPTER)));
		}

	}

}
