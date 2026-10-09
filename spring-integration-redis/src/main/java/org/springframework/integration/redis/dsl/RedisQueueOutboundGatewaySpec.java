/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.redis.dsl;

import java.time.Duration;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.redis.outbound.RedisQueueOutboundGateway;

/**
 * A {@link MessageHandlerSpec} for a {@link RedisQueueOutboundGateway}.
 *
 * @author Jiandong Ma
 *
 * @since 7.1
 */
public class RedisQueueOutboundGatewaySpec extends
		MessageHandlerSpec<RedisQueueOutboundGatewaySpec, RedisQueueOutboundGateway> {

	protected RedisQueueOutboundGatewaySpec(String queueName, RedisConnectionFactory connectionFactory) {
		this.target = new RedisQueueOutboundGateway(queueName, connectionFactory);
	}

	/**
	 * Specify the receiveTimeout.
	 * @param receiveTimeout the receiveTimeout
	 * @return the spec
	 * @see RedisQueueOutboundGateway#setReceiveDuration(Duration)
	 */
	public RedisQueueOutboundGatewaySpec receiveTimeout(Duration receiveTimeout) {
		this.target.setReceiveDuration(receiveTimeout);
		return this;
	}

	/**
	 * Specify the receiveTimeout.
	 * @param receiveTimeout the receiveTimeout
	 * @return the spec
	 * @see RedisQueueOutboundGateway#setReceiveTimeout(long)
	 */
	public RedisQueueOutboundGatewaySpec receiveTimeout(long receiveTimeout) {
		this.target.setReceiveTimeout(receiveTimeout);
		return this;
	}

	/**
	 * Specify whether extract payload.
	 * @param extractPayload the extractPayload
	 * @return the spec
	 * @see RedisQueueOutboundGateway#setExtractPayload(boolean)
	 */
	public RedisQueueOutboundGatewaySpec extractPayload(boolean extractPayload) {
		this.target.setExtractPayload(extractPayload);
		return this;
	}

	/**
	 * Specify the redis serializer.
	 * @param serializer the serializer
	 * @return the spec
	 * @see RedisQueueOutboundGateway#setSerializer(RedisSerializer)
	 */
	public RedisQueueOutboundGatewaySpec serializer(RedisSerializer<?> serializer) {
		this.target.setSerializer(serializer);
		return this;
	}

}
