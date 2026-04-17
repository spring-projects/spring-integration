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
import java.util.concurrent.Executor;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.integration.dsl.MessagingGatewaySpec;
import org.springframework.integration.redis.inbound.RedisQueueInboundGateway;

/**
 * A {@link MessagingGatewaySpec} for a {@link RedisQueueInboundGateway}.
 *
 * @author Jiandong Ma
 *
 * @since 7.1
 */
public class RedisQueueInboundGatewaySpec extends MessagingGatewaySpec<RedisQueueInboundGatewaySpec, RedisQueueInboundGateway> {

	protected RedisQueueInboundGatewaySpec(String queueName, RedisConnectionFactory connectionFactory) {
		super(new RedisQueueInboundGateway(queueName, connectionFactory));
	}

	/**
	 * Specify whether extract payload.
	 * @param extractPayload the extractPayload
	 * @return the spec
	 * @see RedisQueueInboundGateway#setExtractPayload(boolean)
	 */
	public RedisQueueInboundGatewaySpec extractPayload(boolean extractPayload) {
		this.target.setExtractPayload(extractPayload);
		return this;
	}

	/**
	 * Specify the redis serializer.
	 * @param serializer the serializer
	 * @return the spec
	 * @see RedisQueueInboundGateway#setSerializer(RedisSerializer)
	 */
	public RedisQueueInboundGatewaySpec serializer(RedisSerializer<?> serializer) {
		this.target.setSerializer(serializer);
		return this;
	}

	/**
	 * Specify the receiveTimeout.
	 * @param receiveTimeout the receiveTimeout
	 * @return the spec
	 * @see RedisQueueInboundGateway#setReceiveDuration(Duration)
	 */
	public RedisQueueInboundGatewaySpec receiveTimeout(Duration receiveTimeout) {
		this.target.setReceiveDuration(receiveTimeout);
		return this;
	}

	/**
	 * Specify the receiveTimeout.
	 * @param receiveTimeout the receiveTimeout
	 * @return the spec
	 * @see RedisQueueInboundGateway#setReceiveTimeout(long)
	 */
	public RedisQueueInboundGatewaySpec receiveTimeout(long receiveTimeout) {
		this.target.setReceiveTimeout(receiveTimeout);
		return this;
	}

	/**
	 * Specify an {@link Executor} for the underlying listening task.
	 * @param taskExecutor the taskExecutor
	 * @return the spec
	 * @see RedisQueueInboundGateway#setTaskExecutor(Executor)
	 */
	public RedisQueueInboundGatewaySpec taskExecutor(Executor taskExecutor) {
		this.target.setTaskExecutor(taskExecutor);
		return this;
	}

	/**
	 * Specify the time (in milliseconds) for the listener task should sleep after exceptions on
	 * the "right pop" operation before restarting the listener task.
	 * @param recoveryInterval the recoveryInterval
	 * @return the spec
	 * @see RedisQueueInboundGateway#setRecoveryInterval(long)
	 */
	public RedisQueueInboundGatewaySpec recoveryInterval(long recoveryInterval) {
		this.target.setRecoveryInterval(recoveryInterval);
		return this;
	}

}
