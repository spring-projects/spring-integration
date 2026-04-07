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

import java.util.function.Function;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.expression.Expression;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.redis.outbound.RedisPublishingMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConverter;

/**
 * A {@link MessageHandlerSpec} for a {@link RedisPublishingMessageHandler}.
 *
 * @author Jiandong Ma
 *
 * @since 7.1
 */
public class RedisOutboundChannelAdapterSpec extends MessageHandlerSpec<RedisOutboundChannelAdapterSpec, RedisPublishingMessageHandler> {

	protected RedisOutboundChannelAdapterSpec(RedisConnectionFactory connectionFactory) {
		this.target = new RedisPublishingMessageHandler(connectionFactory);
	}

	/**
	 * @param serializer the serializer
	 * @return the spec
	 * @see RedisPublishingMessageHandler#setSerializer(RedisSerializer)
	 */
	public RedisOutboundChannelAdapterSpec serializer(RedisSerializer<?> serializer) {
		this.target.setSerializer(serializer);
		return this;
	}

	/**
	 * @param messageConverter the messageConverter
	 * @return the spec
	 * @see RedisPublishingMessageHandler#setMessageConverter(MessageConverter)
	 */
	public RedisOutboundChannelAdapterSpec messageConverter(MessageConverter messageConverter) {
		this.target.setMessageConverter(messageConverter);
		return this;
	}

	/**
	 * @param topic the topic
	 * @return the spec
	 * @see RedisPublishingMessageHandler#setTopic(String)
	 */
	public RedisOutboundChannelAdapterSpec topic(String topic) {
		this.target.setTopic(topic);
		return this;
	}

	/**
	 * @param topicExpression the topicExpression
	 * @return the spec
	 * @see RedisPublishingMessageHandler#setTopicExpression(Expression)
	 */
	public RedisOutboundChannelAdapterSpec topicExpression(Expression topicExpression) {
		this.target.setTopicExpression(topicExpression);
		return this;
	}

	/**
	 * Configure a SpEL expression to determine the topic.
	 * @param topicExpression the topicExpression
	 * @return the spec
	 * @see RedisPublishingMessageHandler#setTopicExpression(Expression)
	 */
	public RedisOutboundChannelAdapterSpec topicExpression(String topicExpression) {
		this.target.setTopicExpression(PARSER.parseExpression(topicExpression));
		return this;
	}

	/**
	 * Configure a {@link Function} to determine the topic.
	 * @param topicFunction the topicFunction
	 * @param <P> the payload type.
	 * @return the spec
	 */
	public <P> RedisOutboundChannelAdapterSpec topicFunction(Function<Message<P>, String> topicFunction) {
		this.target.setTopicExpression(new FunctionExpression<>(topicFunction));
		return this;
	}

}
