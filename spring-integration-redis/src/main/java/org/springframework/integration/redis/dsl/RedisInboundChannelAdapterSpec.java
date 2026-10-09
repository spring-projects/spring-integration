/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.redis.dsl;

import java.util.concurrent.Executor;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.integration.dsl.MessageProducerSpec;
import org.springframework.integration.redis.inbound.RedisInboundChannelAdapter;
import org.springframework.messaging.converter.MessageConverter;

/**
 * A {@link MessageProducerSpec} for a {@link RedisInboundChannelAdapter}.
 *
 * @author Jiandong Ma
 *
 * @since 7.1
 */
public class RedisInboundChannelAdapterSpec extends
		MessageProducerSpec<RedisInboundChannelAdapterSpec, RedisInboundChannelAdapter> {

	protected RedisInboundChannelAdapterSpec(RedisConnectionFactory connectionFactory) {
		this.target = new RedisInboundChannelAdapter(connectionFactory);
	}

	/**
	 * Specify the RedisSerializer to deserialize the body of Redis messages.
	 * @param serializer the serializer
	 * @return the spec
	 * @see RedisInboundChannelAdapter#setSerializer(RedisSerializer)
	 */
	public RedisInboundChannelAdapterSpec serializer(RedisSerializer<?> serializer) {
		this.target.setSerializer(serializer);
		return this;
	}

	/**
	 * Specify the topics to subscribe.
	 * @param topics the topics
	 * @return the spec
	 * @see RedisInboundChannelAdapter#setTopics(String...)
	 */
	public RedisInboundChannelAdapterSpec topics(String... topics) {
		this.target.setTopics(topics);
		return this;
	}

	/**
	 * Specify the topicPatterns to subscribe.
	 * @param topicPatterns the topicPatterns
	 * @return the spec
	 * @see RedisInboundChannelAdapter#setTopicPatterns(String...)
	 */
	public RedisInboundChannelAdapterSpec topicPatterns(String... topicPatterns) {
		this.target.setTopicPatterns(topicPatterns);
		return this;
	}

	/**
	 * Specify the messageConverter to convert between Redis messages and Spring message payloads.
	 * @param messageConverter the messageConverter
	 * @return the spec
	 * @see RedisInboundChannelAdapter#setMessageConverter(MessageConverter)
	 */
	public RedisInboundChannelAdapterSpec messageConverter(MessageConverter messageConverter) {
		this.target.setMessageConverter(messageConverter);
		return this;
	}

	/**
	 * Specify an {@link Executor} for running the message listeners when messages are received.
	 * @param taskExecutor the taskExecutor
	 * @return the spec
	 * @see RedisInboundChannelAdapter#setTaskExecutor(Executor)
	 */
	public RedisInboundChannelAdapterSpec taskExecutor(Executor taskExecutor) {
		this.target.setTaskExecutor(taskExecutor);
		return this;
	}

}
