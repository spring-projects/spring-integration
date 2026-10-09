/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.redis.dsl;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.expression.Expression;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.redis.outbound.RedisQueueOutboundChannelAdapter;

/**
 * A {@link MessageHandlerSpec} for a {@link RedisQueueOutboundChannelAdapter}.
 *
 * @author Jiandong Ma
 *
 * @since 7.1
 */
public class RedisQueueOutboundChannelAdapterSpec extends
		MessageHandlerSpec<RedisQueueOutboundChannelAdapterSpec, RedisQueueOutboundChannelAdapter> {

	protected RedisQueueOutboundChannelAdapterSpec(String queueName, RedisConnectionFactory connectionFactory) {
		this.target = new RedisQueueOutboundChannelAdapter(queueName, connectionFactory);
	}

	protected RedisQueueOutboundChannelAdapterSpec(Expression queueExpression, RedisConnectionFactory connectionFactory) {
		this.target = new RedisQueueOutboundChannelAdapter(queueExpression, connectionFactory);
	}

	/**
	 * Specify send only the payload or the entire Message to the Redis queue.
	 * @param extractPayload the extractPayload
	 * @return the spec
	 * @see RedisQueueOutboundChannelAdapter#setExtractPayload(boolean)
	 */
	public RedisQueueOutboundChannelAdapterSpec extractPayload(boolean extractPayload) {
		this.target.setExtractPayload(extractPayload);
		return this;
	}

	/**
	 * Specify the RedisSerializer to serialize data before sending to the Redis Queue.
	 * @param serializer the serializer
	 * @return the spec
	 * @see RedisQueueOutboundChannelAdapter#setSerializer(RedisSerializer)
	 */
	public RedisQueueOutboundChannelAdapterSpec serializer(RedisSerializer<?> serializer) {
		this.target.setSerializer(serializer);
		return this;
	}

	/**
	 * Specify use "left push" or "right push" to write messages to the Redis Queue.
	 * @param leftPush the leftPush
	 * @return the spec
	 * @see RedisQueueOutboundChannelAdapter#setLeftPush(boolean)
	 */
	public RedisQueueOutboundChannelAdapterSpec leftPush(boolean leftPush) {
		this.target.setLeftPush(leftPush);
		return this;
	}

}
