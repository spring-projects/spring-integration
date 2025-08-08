/*
 * Copyright © 2007 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2007-present the original author or authors.
 */

package org.springframework.integration.redis.outbound;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.Topic;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.redis.RedisContainerTest;
import org.springframework.integration.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Artem Vozhdayenko
 * @since 2.1
 */
class RedisPublishingMessageHandlerTests implements RedisContainerTest {

	private static RedisConnectionFactory redisConnectionFactory;

	@BeforeAll
	static void setupConnection() {
		redisConnectionFactory = RedisContainerTest.connectionFactory();
	}

	@Test
	void testRedisPublishingMessageHandler() throws Exception {
		int numToTest = 10;
		String topic = "si.test.channel";
		final CountDownLatch latch = new CountDownLatch(numToTest * 2);

		MessageListenerAdapter listener = new MessageListenerAdapter();
		listener.setDelegate(new Listener(latch));
		listener.setSerializer(new StringRedisSerializer());
		listener.afterPropertiesSet();

		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(redisConnectionFactory);
		container.afterPropertiesSet();
		container.addMessageListener(listener, Collections.<Topic>singletonList(new ChannelTopic(topic)));
		container.start();

		RedisContainerTest.awaitContainerSubscribed(container);

		final RedisPublishingMessageHandler handler = new RedisPublishingMessageHandler(redisConnectionFactory);
		handler.setTopicExpression(new LiteralExpression(topic));

		for (int i = 0; i < numToTest; i++) {
			handler.handleMessage(MessageBuilder.withPayload("test-" + i).build());
		}

		for (int i = 0; i < numToTest; i++) {
			handler.handleMessage(MessageBuilder.withPayload(("test-" + i).getBytes()).build());
		}
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		container.stop();
	}

	private static class Listener {

		private final CountDownLatch latch;

		Listener(CountDownLatch latch) {
			this.latch = latch;
		}

		@SuppressWarnings("unused")
		public void handleMessage(String s) {
			this.latch.countDown();
		}

	}

}
