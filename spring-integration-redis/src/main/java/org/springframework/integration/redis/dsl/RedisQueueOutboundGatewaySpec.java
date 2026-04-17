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
