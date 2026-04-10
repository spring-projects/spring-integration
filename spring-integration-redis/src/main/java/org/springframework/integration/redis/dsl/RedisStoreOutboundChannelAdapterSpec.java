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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.support.collections.RedisCollectionFactoryBean.CollectionType;
import org.springframework.expression.Expression;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.redis.outbound.RedisStoreWritingMessageHandler;
import org.springframework.messaging.Message;

/**
 * A {@link MessageHandlerSpec} for a {@link RedisStoreWritingMessageHandler}.
 *
 * @author Jiandong Ma
 *
 * @since 7.1
 */
public class RedisStoreOutboundChannelAdapterSpec extends
		MessageHandlerSpec<RedisStoreOutboundChannelAdapterSpec, RedisStoreWritingMessageHandler> {

	protected RedisStoreOutboundChannelAdapterSpec(RedisTemplate<String, ?> redisTemplate) {
		this.target = new RedisStoreWritingMessageHandler(redisTemplate);
	}

	protected RedisStoreOutboundChannelAdapterSpec(RedisConnectionFactory connectionFactory) {
		this.target = new RedisStoreWritingMessageHandler(connectionFactory);
	}

	/**
	 * Specify the key for the Redis store.
	 * @param key the key
	 * @return the spec
	 * @see RedisStoreWritingMessageHandler#setKey(String)
	 */
	public RedisStoreOutboundChannelAdapterSpec key(String key) {
		this.target.setKey(key);
		return this;
	}

	/**
	 * Specify a SpEL Expression to be used to determine the key for the Redis store.
	 * @param keyExpression the keyExpression
	 * @return the spec
	 * @see RedisStoreWritingMessageHandler#setKeyExpressionString(String)
	 */
	public RedisStoreOutboundChannelAdapterSpec keyExpression(String keyExpression) {
		this.target.setKeyExpressionString(keyExpression);
		return this;
	}

	/**
	 * Specify an Expression to be used to determine the key for the Redis store.
	 * @param keyExpression the keyExpression
	 * @return the spec
	 * @see RedisStoreWritingMessageHandler#setKeyExpression(Expression)
	 */
	public RedisStoreOutboundChannelAdapterSpec keyExpression(Expression keyExpression) {
		this.target.setKeyExpression(keyExpression);
		return this;
	}

	/**
	 * Specify a KeyFunction to be used to determine the key for the Redis store.
	 * @param keyFunction the keyFunction
	 * @return the spec
	 * @see RedisStoreWritingMessageHandler#setKeyExpression(Expression)
	 */
	public RedisStoreOutboundChannelAdapterSpec keyFunction(Function<Message<?>, String> keyFunction) {
		this.target.setKeyExpression(new FunctionExpression<>(keyFunction));
		return this;
	}

	/**
	 * Specify the collection type. supported collections are LIST, SET, ZSET, PROPERTIES, and MAP.
	 * @param collectionType the collectionType
	 * @return the spec
	 * @see RedisStoreWritingMessageHandler#setCollectionType(CollectionType)
	 */
	public RedisStoreOutboundChannelAdapterSpec collectionType(CollectionType collectionType) {
		this.target.setCollectionType(collectionType);
		return this;
	}

	/**
	 * Specify whether payload elements should be extracted.
	 * @param extractPayloadElements the extractPayloadElements
	 * @return the spec
	 * @see RedisStoreWritingMessageHandler#setExtractPayloadElements(boolean)
	 */
	public RedisStoreOutboundChannelAdapterSpec extractPayloadElements(boolean extractPayloadElements) {
		this.target.setExtractPayloadElements(extractPayloadElements);
		return this;
	}

	/**
	 * Specify the SPEL expression used as the key for Map and Properties entries.
	 * @param mapKeyExpression the mapKeyExpression
	 * @return the spec
	 * @see RedisStoreWritingMessageHandler#setMapKeyExpressionString(String)
	 */
	public RedisStoreOutboundChannelAdapterSpec mapKeyExpression(String mapKeyExpression) {
		this.target.setMapKeyExpressionString(mapKeyExpression);
		return this;
	}

	/**
	 * Specify the expression used as the key for Map and Properties entries.
	 * @param mapKeyExpression the mapKeyExpression
	 * @return the spec
	 * @see RedisStoreWritingMessageHandler#setMapKeyExpression(Expression)
	 */
	public RedisStoreOutboundChannelAdapterSpec mapKeyExpression(Expression mapKeyExpression) {
		this.target.setMapKeyExpression(mapKeyExpression);
		return this;
	}

	/**
	 * Specify the function used as the key for Map and Properties entries.
	 * @param mapKeyFunction the mapKeyFunction
	 * @return the spec
	 * @see RedisStoreWritingMessageHandler#setMapKeyExpression(Expression)
	 */
	public RedisStoreOutboundChannelAdapterSpec mapKeyFunction(Function<Message<?>, String> mapKeyFunction) {
		this.target.setMapKeyExpression(new FunctionExpression<>(mapKeyFunction));
		return this;
	}

	/**
	 * Specify the SPEL expression used as the INCR flag for the ZADD command in case of ZSet collection.
	 * @param zsetIncrementScoreExpression the zsetIncrementScoreExpression
	 * @return the spec
	 * @see RedisStoreWritingMessageHandler#setZsetIncrementExpressionString(String)
	 */
	public RedisStoreOutboundChannelAdapterSpec zsetIncrementScoreExpression(String zsetIncrementScoreExpression) {
		this.target.setZsetIncrementExpressionString(zsetIncrementScoreExpression);
		return this;
	}

	/**
	 * Specify the expression used as the INCR flag for the ZADD command in case of ZSet collection.
	 * @param zsetIncrementScoreExpression the zsetIncrementScoreExpression
	 * @return the spec
	 * @see RedisStoreWritingMessageHandler#setZsetIncrementExpression(Expression)
	 */
	public RedisStoreOutboundChannelAdapterSpec zsetIncrementScoreExpression(Expression zsetIncrementScoreExpression) {
		this.target.setZsetIncrementExpression(zsetIncrementScoreExpression);
		return this;
	}

	/**
	 * Specify the function used as the INCR flag for the ZADD command in case of ZSet collection.
	 * @param zsetIncrementScoreFunction the zsetIncrementScoreFunction
	 * @return the spec
	 * @see RedisStoreWritingMessageHandler#setZsetIncrementExpression(Expression)
	 */
	public RedisStoreOutboundChannelAdapterSpec zsetIncrementScoreFunction(Function<Message<?>, Boolean> zsetIncrementScoreFunction) {
		this.target.setZsetIncrementExpression(new FunctionExpression<>(zsetIncrementScoreFunction));
		return this;
	}

}
