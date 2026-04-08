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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
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
import org.springframework.data.redis.support.collections.RedisCollectionFactoryBean.CollectionType;
import org.springframework.data.redis.support.collections.RedisZSet;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.GenericTransformer;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.redis.RedisContainerTest;
import org.springframework.integration.redis.inbound.RedisInboundChannelAdapter;
import org.springframework.integration.redis.support.RedisHeaders;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.transaction.DefaultTransactionSynchronizationFactory;
import org.springframework.integration.transaction.ExpressionEvaluatingTransactionSynchronizationProcessor;
import org.springframework.integration.transaction.PseudoTransactionManager;
import org.springframework.integration.transaction.TransactionSynchronizationFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
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

	static final String QUEUE_NAME_FOR_QUEUE_INBOUND_CHANNEL_ADAPTER = "dslQueueInboundChannelAdapter";

	static final String QUEUE_NAME_FOR_QUEUE_OUTBOUND_CHANNEL_ADAPTER = "dslQueueOutboundChannelAdapter";

	static final String STORE_FOR_INBOUND_CHANNEL_ADAPTER = "dslStoreInboundChannelAdapter";

	static final String STORE_FOR_OUTBOUND_CHANNEL_ADAPTER = "dslStoreOutboundChannelAdapter";

	@Autowired
	RedisConnectionFactory connectionFactory;

	@Autowired
	RedisInboundChannelAdapter inboundChannelAdapter;

	@Autowired
	QueueChannel inboundChannelAdapterQueueChannel;

	@Autowired
	@Qualifier("outboundChannelAdapterFlow.input")
	MessageChannel outboundChannelAdapterInputChannel;

	@Autowired
	QueueChannel queueInboundChannelAdapterOutputChannel;

	@Autowired
	@Qualifier("queueOutboundChannelAdapterFlow.input")
	MessageChannel queueOutboundChannelAdapterInputChannel;

	@Autowired
	SourcePollingChannelAdapter storeSourcePollingChannelAdapter;

	@Autowired
	QueueChannel storeInboundChannelAdapterOutputChannel;

	@Autowired
	@Qualifier("storeOutboundChannelAdapterFlow.input")
	MessageChannel storeOutboundChannelAdapterInputChannel;

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

	@Test
	void testQueueInboundChannelAdapterFlow() {
		// Given
		int numToTest = 10;
		StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);
		var listOps = redisTemplate.boundListOps(QUEUE_NAME_FOR_QUEUE_INBOUND_CHANNEL_ADAPTER);
		for (int i = 0; i < numToTest; i++) {
			listOps.leftPush("queue-inbound-message-" + i);
		}

		for (int i = 0; i < numToTest; i++) {
			// When
			Message<?> message = queueInboundChannelAdapterOutputChannel.receive(10000);
			// Then
			assertThat(message).isNotNull()
					.extracting(Message::getPayload)
					.isEqualTo("queue-inbound-message-" + i);
		}
	}

	@Test
	void testQueueOutboundChannelAdapterFlow() {
		// Given
		int numToTest = 10;
		for (int i = 0; i < numToTest; i++) {
			queueOutboundChannelAdapterInputChannel.send(MessageBuilder.withPayload("queue-outbound-message-" + i).build());
		}

		StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);
		var listOps = redisTemplate.boundListOps(QUEUE_NAME_FOR_QUEUE_OUTBOUND_CHANNEL_ADAPTER);
		for (int i = 0; i < numToTest; i++) {
			// When
			String msg = listOps.rightPop();
			// Then
			assertThat(msg)
					.isNotNull()
					.isEqualTo("queue-outbound-message-" + i);
		}
	}

	@Test
	void testStoreInboundChannelAdapterFlow() {
		// Given
		StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);
		var zSetOps = redisTemplate.boundZSetOps(STORE_FOR_INBOUND_CHANNEL_ADAPTER);
		zSetOps.add("task:1", 1);
		zSetOps.add("task:2", 2);
		zSetOps.add("task:3", 3);
		zSetOps.add("task:4", 4);
		zSetOps.add("task:5", 5);

		// When
		storeSourcePollingChannelAdapter.start();
		Message<?> receive = storeInboundChannelAdapterOutputChannel.receive(10000);

		// Then
		assertThat(receive)
				.isNotNull()
				.extracting(Message::getPayload)
				.asInstanceOf(InstanceOfAssertFactories.set(String.class))
				.hasSize(3)
				.containsExactly("task:1", "task:2", "task:3");

		Set<String> remainingItems = zSetOps.rangeByScore(1, 5);
		assertThat(remainingItems)
				.isNotNull()
				.hasSize(2)
				.containsExactly("task:4", "task:5");

		storeSourcePollingChannelAdapter.stop();
	}

	@Test
	void testStoreOutboundChannelAdapterFlow() {
		// Given
		var entry1 = new GenericMessage<>("red", Map.of(RedisHeaders.MAP_KEY, "apple"));
		var entry2 = MessageBuilder.withPayload(Map.of("banana", "yellow")).build();
		// When
		storeOutboundChannelAdapterInputChannel.send(entry1);
		storeOutboundChannelAdapterInputChannel.send(entry2);
		// Then
		StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);
		var hashOps = redisTemplate.boundHashOps(STORE_FOR_OUTBOUND_CHANNEL_ADAPTER);
		var entries = hashOps.entries();
		assertThat(entries)
				.isNotNull()
				.hasSize(2)
				.satisfies(entry -> {
							assertThat(entry.get("apple")).isEqualTo("red");
							assertThat(entry.get("banana")).isEqualTo("yellow");
						}
				);

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

		@Bean
		IntegrationFlow queueInboundChannelAdapterFlow(RedisConnectionFactory redisConnectionFactory) {
			return IntegrationFlow.from(Redis
							.queueInboundChannelAdapter(QUEUE_NAME_FOR_QUEUE_INBOUND_CHANNEL_ADAPTER, redisConnectionFactory)
							.serializer(RedisSerializer.string())
					)
					.channel(c -> c.queue("queueInboundChannelAdapterOutputChannel"))
					.get();
		}

		@Bean
		IntegrationFlow queueOutboundChannelAdapterFlow(RedisConnectionFactory redisConnectionFactory) {
			return flow -> flow
					.handle(Redis.queueOutboundChannelAdapter(QUEUE_NAME_FOR_QUEUE_OUTBOUND_CHANNEL_ADAPTER, redisConnectionFactory)
							.serializer(RedisSerializer.string())
					);
		}

		@Bean
		IntegrationFlow storeInboundChannelAdapterFlow(RedisConnectionFactory redisConnectionFactory,
				TransactionSynchronizationFactory syncFactory) {
			return IntegrationFlow.from(Redis
									.storeInboundChannelAdapterSpec(redisConnectionFactory, STORE_FOR_INBOUND_CHANNEL_ADAPTER)
									.collectionType(CollectionType.ZSET),
							endpointConfigure -> endpointConfigure
									.poller(Pollers
											.fixedDelay(1000)
											.transactional(new PseudoTransactionManager())
											.transactionSynchronizationFactory(syncFactory)
									)
									.autoStartup(false)
									.id("storeSourcePollingChannelAdapter"))
					.transform((GenericTransformer<RedisZSet<?>, Collection<?>>) source -> source.rangeByScore(1, 3))
					.channel(c -> c.queue("storeInboundChannelAdapterOutputChannel"))
					.get();
		}

		@Bean
		TransactionSynchronizationFactory syncFactory(BeanFactory beanFactory) {
			var processor = new ExpressionEvaluatingTransactionSynchronizationProcessor();
			processor.setAfterCommitExpression(new SpelExpressionParser()
					.parseExpression("payload.removeByScore(1, 3)"));
			processor.setAfterCommitChannel(MessageChannels.queue("storeInboundAdapterCommitChannel").getObject());

			processor.setBeanFactory(beanFactory);
			processor.afterPropertiesSet();
			return new DefaultTransactionSynchronizationFactory(processor);
		}

		@Bean
		IntegrationFlow storeOutboundChannelAdapterFlow(RedisConnectionFactory redisConnectionFactory) {
			return flow -> flow
					.handle(Redis.storeOutboundChannelAdapterSpec(redisConnectionFactory)
							.key(STORE_FOR_OUTBOUND_CHANNEL_ADAPTER)
							.collectionType(CollectionType.MAP));
		}

	}

}
