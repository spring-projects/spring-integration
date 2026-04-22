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

import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStreamCommands;
import org.springframework.data.redis.hash.HashMapper;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.dsl.ReactiveMessageHandlerSpec;
import org.springframework.integration.redis.outbound.ReactiveRedisStreamMessageHandler;
import org.springframework.messaging.Message;

/**
 * A {@link ReactiveMessageHandlerSpec} for a {@link ReactiveRedisStreamMessageHandler}.
 *
 * @author Jiandong Ma
 *
 * @since 7.1
 */
public class ReactiveRedisOutboundChannelAdapterSpec extends
		ReactiveMessageHandlerSpec<ReactiveRedisOutboundChannelAdapterSpec, ReactiveRedisStreamMessageHandler> {

	protected ReactiveRedisOutboundChannelAdapterSpec(ReactiveRedisConnectionFactory connectionFactory,
			String streamKey) {

		super(new ReactiveRedisStreamMessageHandler(connectionFactory, streamKey));
	}

	protected ReactiveRedisOutboundChannelAdapterSpec(ReactiveRedisConnectionFactory connectionFactory,
			Expression streamExpression) {

		super(new ReactiveRedisStreamMessageHandler(connectionFactory, streamExpression));
	}

	/**
	 * Specify the serialization context.
	 * @param serializationContext the serializationContext
	 * @return the spec
	 * @see ReactiveRedisStreamMessageHandler#setSerializationContext(RedisSerializationContext)
	 */
	public ReactiveRedisOutboundChannelAdapterSpec serializationContext(RedisSerializationContext<String, ?> serializationContext) {
		this.reactiveMessageHandler.setSerializationContext(serializationContext);
		return this;
	}

	/**
	 * Specify the hashMapper for {@link org.springframework.data.redis.core.ReactiveStreamOperations}.
	 * @param hashMapper the hashMapper
	 * @return the spec
	 * @see ReactiveRedisStreamMessageHandler#setHashMapper(HashMapper)
	 */
	public ReactiveRedisOutboundChannelAdapterSpec hashMapper(HashMapper<String, ?, ?> hashMapper) {
		this.reactiveMessageHandler.setHashMapper(hashMapper);
		return this;
	}

	/**
	 * Specify whether extract payload.
	 * @param extractPayload the extractPayload
	 * @return the spec
	 * @see ReactiveRedisStreamMessageHandler#setExtractPayload(boolean)
	 */
	public ReactiveRedisOutboundChannelAdapterSpec extractPayload(boolean extractPayload) {
		this.reactiveMessageHandler.setExtractPayload(extractPayload);
		return this;
	}

	/**
	 * Specify the function to create a {@link RedisStreamCommands.XAddOptions}.
	 * @param addOptionsFunction the addOptionsFunction
	 * @return the spec
	 * @see ReactiveRedisStreamMessageHandler#setAddOptionsFunction(Function)
	 */
	public ReactiveRedisOutboundChannelAdapterSpec addOptionsFunction(Function<Message<?>, RedisStreamCommands.XAddOptions> addOptionsFunction) {
		this.reactiveMessageHandler.setAddOptionsFunction(addOptionsFunction);
		return this;
	}

}
