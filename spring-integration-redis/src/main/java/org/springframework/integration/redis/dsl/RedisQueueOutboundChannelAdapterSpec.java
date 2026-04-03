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

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.expression.Expression;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.redis.outbound.RedisQueueOutboundChannelAdapter;

/**
 * A {@link MessageHandlerSpec} for a {@link RedisQueueOutboundChannelAdapterSpec}.
 *
 * @author Jiandong Ma
 *
 * @since 7.1
 */
public class RedisQueueOutboundChannelAdapterSpec extends MessageHandlerSpec<RedisQueueOutboundChannelAdapterSpec, RedisQueueOutboundChannelAdapter> {

	protected RedisQueueOutboundChannelAdapterSpec(String queueName, RedisConnectionFactory connectionFactory) {
		this.target = new RedisQueueOutboundChannelAdapter(queueName, connectionFactory);
	}

	protected RedisQueueOutboundChannelAdapterSpec(Expression queueExpression, RedisConnectionFactory connectionFactory) {
		this.target = new RedisQueueOutboundChannelAdapter(queueExpression, connectionFactory);
	}

	/**
	 * @param extractPayload the extractPayload
	 * @return the spec
	 * @see RedisQueueOutboundChannelAdapter#setExtractPayload(boolean)
	 */
	public RedisQueueOutboundChannelAdapterSpec extractPayload(boolean extractPayload) {
		this.target.setExtractPayload(extractPayload);
		return this;
	}

	/**
	 * @param serializer the serializer
	 * @return the spec
	 * @see RedisQueueOutboundChannelAdapter#setSerializer(RedisSerializer)
	 */
	public RedisQueueOutboundChannelAdapterSpec serializer(RedisSerializer<?> serializer) {
		this.target.setSerializer(serializer);
		return this;
	}

	/**
	 * @param leftPush the leftPush
	 * @return the spec
	 * @see RedisQueueOutboundChannelAdapter#setLeftPush(boolean)
	 */
	public RedisQueueOutboundChannelAdapterSpec leftPush(boolean leftPush) {
		this.target.setLeftPush(leftPush);
		return this;
	}

}
