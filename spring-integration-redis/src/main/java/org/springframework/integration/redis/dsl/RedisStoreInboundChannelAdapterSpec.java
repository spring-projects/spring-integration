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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.support.collections.RedisCollectionFactoryBean.CollectionType;
import org.springframework.expression.Expression;
import org.springframework.integration.dsl.MessageSourceSpec;
import org.springframework.integration.redis.inbound.RedisStoreMessageSource;

/**
 * A {@link MessageSourceSpec} for a {@link RedisStoreMessageSource}.
 *
 * @author Jiandong Ma
 *
 * @since 7.1
 */
public class RedisStoreInboundChannelAdapterSpec extends
		MessageSourceSpec<RedisStoreInboundChannelAdapterSpec, RedisStoreMessageSource> {

	protected RedisStoreInboundChannelAdapterSpec(RedisTemplate<String, ?> redisTemplate, Expression keyExpression) {
		this.target = new RedisStoreMessageSource(redisTemplate, keyExpression);
	}

	protected RedisStoreInboundChannelAdapterSpec(RedisConnectionFactory connectionFactory, Expression keyExpression) {
		this.target = new RedisStoreMessageSource(connectionFactory, keyExpression);
	}

	/**
	 * Specify the collection type. supported collections are LIST, SET, ZSET, PROPERTIES, and MAP.
	 * @param collectionType the collectionType
	 * @return the spec
	 * @see RedisStoreMessageSource#setCollectionType(CollectionType)
	 */
	public RedisStoreInboundChannelAdapterSpec collectionType(CollectionType collectionType) {
		this.target.setCollectionType(collectionType);
		return this;
	}

}
