/*
 * Copyright 2026-present the original author or authors.
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
public class RedisStreamOutboundChannelAdapterSpec extends
		ReactiveMessageHandlerSpec<RedisStreamOutboundChannelAdapterSpec, ReactiveRedisStreamMessageHandler> {

	protected RedisStreamOutboundChannelAdapterSpec(ReactiveRedisConnectionFactory connectionFactory,
			String streamKey) {

		super(new ReactiveRedisStreamMessageHandler(connectionFactory, streamKey));
	}

	protected RedisStreamOutboundChannelAdapterSpec(ReactiveRedisConnectionFactory connectionFactory,
			Expression streamExpression) {

		super(new ReactiveRedisStreamMessageHandler(connectionFactory, streamExpression));
	}

	/**
	 * Specify the serialization context.
	 * @param serializationContext the serializationContext
	 * @return the spec
	 * @see ReactiveRedisStreamMessageHandler#setSerializationContext(RedisSerializationContext)
	 */
	public RedisStreamOutboundChannelAdapterSpec serializationContext(RedisSerializationContext<String, ?> serializationContext) {
		this.reactiveMessageHandler.setSerializationContext(serializationContext);
		return this;
	}

	/**
	 * Specify the hashMapper for {@link org.springframework.data.redis.core.ReactiveStreamOperations}.
	 * @param hashMapper the hashMapper
	 * @return the spec
	 * @see ReactiveRedisStreamMessageHandler#setHashMapper(HashMapper)
	 */
	public RedisStreamOutboundChannelAdapterSpec hashMapper(HashMapper<String, ?, ?> hashMapper) {
		this.reactiveMessageHandler.setHashMapper(hashMapper);
		return this;
	}

	/**
	 * Specify whether extract payload.
	 * @param extractPayload the extractPayload
	 * @return the spec
	 * @see ReactiveRedisStreamMessageHandler#setExtractPayload(boolean)
	 */
	public RedisStreamOutboundChannelAdapterSpec extractPayload(boolean extractPayload) {
		this.reactiveMessageHandler.setExtractPayload(extractPayload);
		return this;
	}

	/**
	 * Specify the function to create a {@link RedisStreamCommands.XAddOptions}.
	 * @param addOptionsFunction the addOptionsFunction
	 * @return the spec
	 * @see ReactiveRedisStreamMessageHandler#setAddOptionsFunction(Function)
	 */
	public RedisStreamOutboundChannelAdapterSpec addOptionsFunction(Function<Message<?>, RedisStreamCommands.XAddOptions> addOptionsFunction) {
		this.reactiveMessageHandler.setAddOptionsFunction(addOptionsFunction);
		return this;
	}

}
