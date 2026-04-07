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
public class RedisInboundChannelAdapterSpec extends MessageProducerSpec<RedisInboundChannelAdapterSpec, RedisInboundChannelAdapter> {

	protected RedisInboundChannelAdapterSpec(RedisConnectionFactory connectionFactory) {
		this.target = new RedisInboundChannelAdapter(connectionFactory);
	}

	/**
	 * @param serializer the serializer
	 * @return the spec
	 * @see RedisInboundChannelAdapter#setSerializer(RedisSerializer)
	 */
	public RedisInboundChannelAdapterSpec serializer(RedisSerializer<?> serializer) {
		this.target.setSerializer(serializer);
		return this;
	}

	/**
	 * @param topics the topics
	 * @return the spec
	 * @see RedisInboundChannelAdapter#setTopics(String...)
	 */
	public RedisInboundChannelAdapterSpec topics(String... topics) {
		this.target.setTopics(topics);
		return this;
	}

	/**
	 * @param topicPatterns the topicPatterns
	 * @return the spec
	 * @see RedisInboundChannelAdapter#setTopicPatterns(String...)
	 */
	public RedisInboundChannelAdapterSpec topicPatterns(String... topicPatterns) {
		this.target.setTopicPatterns(topicPatterns);
		return this;
	}

	/**
	 * @param messageConverter the messageConverter
	 * @return the spec
	 * @see RedisInboundChannelAdapter#setMessageConverter(MessageConverter)
	 */
	public RedisInboundChannelAdapterSpec messageConverter(MessageConverter messageConverter) {
		this.target.setMessageConverter(messageConverter);
		return this;
	}

	/**
	 * @param taskExecutor the taskExecutor
	 * @return the spec
	 * @see RedisInboundChannelAdapter#setTaskExecutor(Executor)
	 */
	public RedisInboundChannelAdapterSpec taskExecutor(Executor taskExecutor) {
		this.target.setTaskExecutor(taskExecutor);
		return this;
	}

}
