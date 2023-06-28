/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.integration.redis.inbound;

import java.util.Collection;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.support.collections.RedisCollectionFactoryBean;
import org.springframework.data.redis.support.collections.RedisCollectionFactoryBean.CollectionType;
import org.springframework.data.redis.support.collections.RedisStore;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.transaction.IntegrationResourceHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * Inbound channel adapter which returns a Message representing a view into
 * a Redis store. The type of store depends on the {@link #collectionType} attribute.
 * Default is LIST. This adapter supports 5 types of collections identified by
 * {@link CollectionType}
 *
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.2
 */
public class RedisStoreMessageSource extends AbstractMessageSource<RedisStore> {

	private final RedisTemplate<String, ?> redisTemplate;

	private final Expression keyExpression;

	private StandardEvaluationContext evaluationContext;

	private CollectionType collectionType = CollectionType.LIST;

	/**
	 * Create an instance with the provided {@link RedisTemplate} and SpEL expression
	 * which should resolve to a 'key' name of the collection to be used.
	 * It assumes that {@link RedisTemplate} is fully initialized and ready to be used.
	 * The 'keyExpression' will be evaluated on every call to the {@link #receive()} method.
	 * @param redisTemplate The Redis template.
	 * @param keyExpression The key expression.
	 */
	public RedisStoreMessageSource(RedisTemplate<String, ?> redisTemplate, Expression keyExpression) {
		Assert.notNull(keyExpression, "'keyExpression' must not be null");
		Assert.notNull(redisTemplate, "'redisTemplate' must not be null");

		this.redisTemplate = redisTemplate;
		this.keyExpression = keyExpression;
	}

	/**
	 * Create an instance with the provided {@link RedisConnectionFactory} and SpEL expression
	 * which should resolve to a 'key' name of the collection to be used.
	 * It will create and initialize an instance of {@link StringRedisTemplate} that uses
	 * {@link org.springframework.data.redis.serializer.StringRedisSerializer} for all
	 * serialization.
	 * The 'keyExpression' will be evaluated on every call to the {@link #receive()} method.
	 * @param connectionFactory The connection factory.
	 * @param keyExpression The key expression.
	 */
	public RedisStoreMessageSource(RedisConnectionFactory connectionFactory, Expression keyExpression) {
		Assert.notNull(keyExpression, "'keyExpression' must not be null");
		Assert.notNull(connectionFactory, "'connectionFactory' must not be null");

		this.redisTemplate = new StringRedisTemplate();
		this.redisTemplate.setConnectionFactory(connectionFactory);
		this.redisTemplate.afterPropertiesSet();

		this.keyExpression = keyExpression;
	}

	public void setCollectionType(CollectionType collectionType) {
		this.collectionType = collectionType;
	}

	@Override
	protected void onInit() {
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
	}

	/**
	 * Return a Message with the view into a {@link RedisStore} identified
	 * by {@link #keyExpression}
	 */
	@Override
	protected RedisStore doReceive() {
		String key = this.keyExpression.getValue(this.evaluationContext, String.class);
		Assert.hasText(key, "Failed to determine the key for the collection");

		RedisStore store = createStoreView(key);

		Object holder = TransactionSynchronizationManager.getResource(this);
		if (holder != null) {
			Assert.isInstanceOf(IntegrationResourceHolder.class, holder);
			((IntegrationResourceHolder) holder).addAttribute("store", store);
		}

		if (store instanceof Collection<?> && ((Collection<?>) store).size() < 1) {
			return null;
		}
		else {
			return store;
		}
	}

	private RedisStore createStoreView(String key) {
		RedisCollectionFactoryBean fb = new RedisCollectionFactoryBean();
		fb.setKey(key);
		fb.setTemplate(this.redisTemplate);
		fb.setType(this.collectionType);
		fb.afterPropertiesSet();
		return fb.getObject();
	}

	@Override
	public String getComponentType() {
		return "redis:store-inbound-channel-adapter";
	}

}
