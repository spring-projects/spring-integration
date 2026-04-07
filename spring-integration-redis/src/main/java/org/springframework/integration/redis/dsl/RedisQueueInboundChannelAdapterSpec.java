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
import org.springframework.integration.dsl.MessageProducerSpec;
import org.springframework.integration.redis.inbound.RedisQueueMessageDrivenEndpoint;

/**
 * A {@link MessageProducerSpec} for a {@link RedisQueueMessageDrivenEndpoint}.
 *
 * @author Jiandong Ma
 *
 * @since 7.1
 */
public class RedisQueueInboundChannelAdapterSpec extends MessageProducerSpec<RedisQueueInboundChannelAdapterSpec, RedisQueueMessageDrivenEndpoint> {

	protected RedisQueueInboundChannelAdapterSpec(String queueName, RedisConnectionFactory connectionFactory) {
		this.target = new RedisQueueMessageDrivenEndpoint(queueName, connectionFactory);
	}

	/**
	 * @param serializer the serializer
	 * @return the spec
	 * @see RedisQueueMessageDrivenEndpoint#setSerializer(RedisSerializer)
	 */
	public RedisQueueInboundChannelAdapterSpec serializer(RedisSerializer<?> serializer) {
		this.target.setSerializer(serializer);
		return this;
	}

	/**
	 * @param expectMessage the expectMessage
	 * @return the spec
	 * @see RedisQueueMessageDrivenEndpoint#setExpectMessage(boolean)
	 */
	public RedisQueueInboundChannelAdapterSpec expectMessage(boolean expectMessage) {
		this.target.setExpectMessage(expectMessage);
		return this;
	}

	/**
	 * @param receiveTimeout the receiveTimeout
	 * @return the spec
	 * @see RedisQueueMessageDrivenEndpoint#setReceiveDuration(Duration)
	 */
	public RedisQueueInboundChannelAdapterSpec receiveDuration(Duration receiveTimeout) {
		this.target.setReceiveDuration(receiveTimeout);
		return this;
	}

	/**
	 * @param receiveTimeout the receiveTimeout
	 * @return the spec
	 * @see RedisQueueMessageDrivenEndpoint#setReceiveTimeout(long)
	 */
	public RedisQueueInboundChannelAdapterSpec receiveTimeout(long receiveTimeout) {
		this.target.setReceiveTimeout(receiveTimeout);
		return this;
	}

	/**
	 * @param taskExecutor the taskExecutor
	 * @return the spec
	 * @see RedisQueueMessageDrivenEndpoint#setTaskExecutor(Executor)
	 */
	public RedisQueueInboundChannelAdapterSpec taskExecutor(Executor taskExecutor) {
		this.target.setTaskExecutor(taskExecutor);
		return this;
	}

	/**
	 * @param recoveryInterval the recoveryInterval
	 * @return the spec
	 * @see RedisQueueMessageDrivenEndpoint#setRecoveryInterval(long)
	 */
	public RedisQueueInboundChannelAdapterSpec recoveryInterval(long recoveryInterval) {
		this.target.setRecoveryInterval(recoveryInterval);
		return this;
	}

	/**
	 * @param rightPop the rightPop
	 * @return the spec
	 * @see RedisQueueMessageDrivenEndpoint#setRightPop(boolean)
	 */
	public RedisQueueInboundChannelAdapterSpec rightPop(boolean rightPop) {
		this.target.setRightPop(rightPop);
		return this;
	}

}
