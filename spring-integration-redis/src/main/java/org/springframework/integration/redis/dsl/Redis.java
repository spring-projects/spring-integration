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
import java.util.function.Supplier;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.expression.SupplierExpression;
import org.springframework.messaging.Message;

/**
 * Factory class for Redis components.
 *
 * @author Jiandong Ma
 *
 * @since 7.1
 */
public final class Redis {

	/**
	 * The factory to produce a {@link RedisInboundChannelAdapterSpec}.
	 * @param connectionFactory the {@link RedisConnectionFactory} to build on
	 * @return the {@link RedisInboundChannelAdapterSpec} instance
	 */
	public static RedisInboundChannelAdapterSpec inboundChannelAdapter(RedisConnectionFactory connectionFactory) {
		return new RedisInboundChannelAdapterSpec(connectionFactory);
	}

	/**
	 * The factory to produce a {@link RedisOutboundChannelAdapterSpec}.
	 * @param connectionFactory the {@link RedisConnectionFactory} to build on
	 * @return the {@link RedisOutboundChannelAdapterSpec} instance
	 */
	public static RedisOutboundChannelAdapterSpec outboundChannelAdapter(RedisConnectionFactory connectionFactory) {
		return new RedisOutboundChannelAdapterSpec(connectionFactory);
	}

	/**
	 * The factory to produce a {@link RedisQueueInboundChannelAdapterSpec}.
	 * @param queueName The queueName of the Redis list to build on
	 * @param connectionFactory the {@link RedisConnectionFactory} to build on
	 * @return the {@link RedisQueueInboundChannelAdapterSpec} instance
	 */
	public static RedisQueueInboundChannelAdapterSpec queueInboundChannelAdapter(String queueName,
			RedisConnectionFactory connectionFactory) {

		return new RedisQueueInboundChannelAdapterSpec(queueName, connectionFactory);
	}

	/**
	 * The factory to produce a {@link RedisQueueOutboundChannelAdapterSpec}.
	 * @param queueName The queueName of the Redis list to build on
	 * @param connectionFactory the {@link RedisConnectionFactory} to build on
	 * @return the {@link RedisQueueOutboundChannelAdapterSpec} instance
	 */
	public static RedisQueueOutboundChannelAdapterSpec queueOutboundChannelAdapter(String queueName,
			RedisConnectionFactory connectionFactory) {

		return new RedisQueueOutboundChannelAdapterSpec(queueName, connectionFactory);
	}

	/**
	 * The factory to produce a {@link RedisQueueOutboundChannelAdapterSpec}.
	 * @param queueExpression The queueExpression of the Redis list to build on
	 * @param connectionFactory the {@link RedisConnectionFactory} to build on
	 * @return the {@link RedisQueueOutboundChannelAdapterSpec} instance
	 */
	public static RedisQueueOutboundChannelAdapterSpec queueOutboundChannelAdapter(Expression queueExpression,
			RedisConnectionFactory connectionFactory) {

		return new RedisQueueOutboundChannelAdapterSpec(queueExpression, connectionFactory);
	}

	/**
	 * The factory to produce a {@link RedisQueueOutboundChannelAdapterSpec}.
	 * @param queueFunction The queueExpression of the Redis list to build on
	 * @param connectionFactory the {@link RedisConnectionFactory} to build on
	 * @return the {@link RedisQueueOutboundChannelAdapterSpec} instance
	 */
	public static RedisQueueOutboundChannelAdapterSpec queueOutboundChannelAdapter(
			Function<Message<?>, String> queueFunction, RedisConnectionFactory connectionFactory) {

		return new RedisQueueOutboundChannelAdapterSpec(new FunctionExpression<>(queueFunction), connectionFactory);
	}

	/**
	 * The factory to produce a {@link RedisStoreInboundChannelAdapterSpec}.
	 * @param connectionFactory the {@link RedisConnectionFactory} to build on
	 * @param key The key of the Redis collection to build on
	 * @return the {@link RedisStoreInboundChannelAdapterSpec} instance
	 */
	public static RedisStoreInboundChannelAdapterSpec storeInboundChannelAdapterSpec(
			RedisConnectionFactory connectionFactory, String key) {

		return storeInboundChannelAdapterSpec(connectionFactory, new LiteralExpression(key));
	}

	/**
	 * The factory to produce a {@link RedisStoreInboundChannelAdapterSpec}.
	 * @param connectionFactory the {@link RedisConnectionFactory} to build on
	 * @param keyExpression The keyExpression of the Redis collection to build on
	 * @return the {@link RedisStoreInboundChannelAdapterSpec} instance
	 */
	public static RedisStoreInboundChannelAdapterSpec storeInboundChannelAdapterSpec(
			RedisConnectionFactory connectionFactory, Expression keyExpression) {

		return new RedisStoreInboundChannelAdapterSpec(connectionFactory, keyExpression);
	}

	/**
	 * The factory to produce a {@link RedisStoreInboundChannelAdapterSpec}.
	 * @param connectionFactory the {@link RedisConnectionFactory} to build on
	 * @param keySupplier The keySupplier of the Redis collection to build on
	 * @return the {@link RedisStoreInboundChannelAdapterSpec} instance
	 */
	public static RedisStoreInboundChannelAdapterSpec storeInboundChannelAdapterSpec(
			RedisConnectionFactory connectionFactory, Supplier<Message<?>> keySupplier) {

		return storeInboundChannelAdapterSpec(connectionFactory, new SupplierExpression<>(keySupplier));
	}

	/**
	 * The factory to produce a {@link RedisStoreInboundChannelAdapterSpec}.
	 * @param redisTemplate the {@link RedisTemplate} to build on
	 * @param key The key of the Redis collection to build on
	 * @return the {@link RedisStoreInboundChannelAdapterSpec} instance
	 */
	public static RedisStoreInboundChannelAdapterSpec storeInboundChannelAdapterSpec(
			RedisTemplate<String, ?> redisTemplate, String key) {

		return storeInboundChannelAdapterSpec(redisTemplate, new LiteralExpression(key));
	}

	/**
	 * The factory to produce a {@link RedisStoreInboundChannelAdapterSpec}.
	 * @param redisTemplate the {@link RedisTemplate} to build on
	 * @param keyExpression The keyExpression of the Redis collection to build on
	 * @return the {@link RedisStoreInboundChannelAdapterSpec} instance
	 */
	public static RedisStoreInboundChannelAdapterSpec storeInboundChannelAdapterSpec(
			RedisTemplate<String, ?> redisTemplate, Expression keyExpression) {

		return new RedisStoreInboundChannelAdapterSpec(redisTemplate, keyExpression);
	}

	/**
	 * The factory to produce a {@link RedisStoreInboundChannelAdapterSpec}.
	 * @param redisTemplate the {@link RedisTemplate} to build on
	 * @param keySupplier The keySupplier of the Redis collection to build on
	 * @return the {@link RedisStoreInboundChannelAdapterSpec} instance
	 */
	public static RedisStoreInboundChannelAdapterSpec storeInboundChannelAdapterSpec(
			RedisTemplate<String, ?> redisTemplate, Supplier<Message<?>> keySupplier) {

		return storeInboundChannelAdapterSpec(redisTemplate, new SupplierExpression<>(keySupplier));
	}

	/**
	 * The factory to produce a {@link RedisStoreOutboundChannelAdapterSpec}.
	 * @param connectionFactory the {@link RedisConnectionFactory} to build on
	 * @return the {@link RedisStoreOutboundChannelAdapterSpec} instance
	 */
	public static RedisStoreOutboundChannelAdapterSpec storeOutboundChannelAdapterSpec(
			RedisConnectionFactory connectionFactory) {

		return new RedisStoreOutboundChannelAdapterSpec(connectionFactory);
	}

	/**
	 * The factory to produce a {@link RedisStoreOutboundChannelAdapterSpec}.
	 * @param redisTemplate the {@link RedisTemplate} to build on
	 * @return the {@link RedisStoreOutboundChannelAdapterSpec} instance
	 */
	public static RedisStoreOutboundChannelAdapterSpec storeOutboundChannelAdapterSpec(
			RedisTemplate<String, ?> redisTemplate) {

		return new RedisStoreOutboundChannelAdapterSpec(redisTemplate);
	}

	private Redis() {
	}

}
