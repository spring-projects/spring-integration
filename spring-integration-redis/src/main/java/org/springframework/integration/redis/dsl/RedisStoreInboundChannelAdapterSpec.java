/*
 * Copyright 2026-present the original author or authors.
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
