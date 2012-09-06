/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.support.collections.RedisCollectionFactoryBean;
import org.springframework.data.redis.support.collections.RedisCollectionFactoryBean.CollectionType;
import org.springframework.data.redis.support.collections.RedisStore;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.Message;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transaction.MessageSourceResourceHolder;
import org.springframework.integration.util.ExpressionUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
/**
 * Inbound channel adapter which returns a Message representing a view into
 * a Redis store. The type of store depends on the {@link #collectionType} attribute.
 * Default is LIST. This adapter supports 5 types of collections identified by
 * {@link CollectionType}
 *
 * @author Oleg Zhurakousky
 * @since 2.2
 */
public class RedisStoreMessageSource extends IntegrationObjectSupport
		implements MessageSource<RedisStore> {

	private final ThreadLocal<RedisStore> resourceHolder = new ThreadLocal<RedisStore>();

	private volatile StandardEvaluationContext evaluationContext;

	private volatile Expression keyExpression;

	private volatile CollectionType collectionType = CollectionType.LIST;

	private final RedisTemplate<String, ?> redisTemplate;

	private volatile RedisSerializer<?> keySerializer;

	private volatile RedisSerializer<?> valueSerializer;

	private volatile RedisSerializer<?> hashKeySerializer;

	private volatile RedisSerializer<?> hashValueSerializer;

	private volatile boolean redisTemplateNotSet = true;

	/**
	 * Creates this instance with provided {@link RedisTemplate} and SpEL expression
	 * which should resolve to a 'key' name of the collection to be used.
	 * It assumes that {@link RedisTemplate} is fully initialized and ready to be used.
	 * The 'keyExpression' will be evaluated on every call to the {@link #receive()} method.
	 *
	 * @param redisTemplate
	 * @param keyExpression
	 */
	public RedisStoreMessageSource(RedisTemplate<String, ?> redisTemplate,
		       Expression keyExpression) {

		Assert.notNull(keyExpression, "'keyExpression' must not be null");
		Assert.notNull(redisTemplate, "'redisTemplate' must not be null");

		this.redisTemplate = redisTemplate;
		this.redisTemplateNotSet = false;
		this.keyExpression = keyExpression;
	}

	/**
	 * Creates this instance with provided {@link RedisConnectionFactory} and SpEL expression
	 * which should resolve to a 'key' name of the collection to be used.
	 * It will create and initialize an instance of the {@link RedisTemplate} using
	 * {@link StringRedisSerializer} as key serializer and {@link JdkSerializationRedisSerializer} for
	 * value, hashKey and hashValue serializer.
	 *
	 * The 'keyExpression' will be evaluated on every call to the {@link #receive()} method.
	 *
	 * @param connectionFactory
	 * @param keyExpression
	 */
	public RedisStoreMessageSource(RedisConnectionFactory connectionFactory,
									       Expression keyExpression) {

		Assert.notNull(keyExpression, "'keyExpression' must not be null");
		Assert.notNull(connectionFactory, "'connectionFactory' must not be null");

		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<String, Object>();
		redisTemplate.setConnectionFactory(connectionFactory);

		this.redisTemplate = redisTemplate;
		this.keyExpression = keyExpression;
	}

	public void setKeySerializer(RedisSerializer<?> keySerializer) {
		Assert.state(this.redisTemplateNotSet, "'keySerializer' can not be set if RedisTemplate provided");
		Assert.notNull(keySerializer, "'keySerializer' must not be null");
		this.keySerializer = keySerializer;
	}

	public void setValueSerializer(RedisSerializer<?> valueSerializer) {
		Assert.state(this.redisTemplateNotSet, "'valueSerializer' can not be set if RedisTemplate provided");
		Assert.notNull(valueSerializer, "'valueSerializer' must not be null");
		this.valueSerializer = valueSerializer;
	}

	public void setHashKeySerializer(RedisSerializer<?> hashKeySerializer) {
		Assert.state(this.redisTemplateNotSet, "'hashKeySerializer' can not be set if RedisTemplate provided");
		Assert.notNull(hashKeySerializer, "'hashKeySerializer' must not be null");
		this.hashKeySerializer = hashKeySerializer;
	}

	public void setHashValueSerializer(RedisSerializer<?> hashValueSerializer) {
		Assert.state(this.redisTemplateNotSet, "'hashValueSerializer' can not be set if RedisTemplate provided");
		Assert.notNull(hashValueSerializer, "'hashValueSerializer' must not be null");
		this.hashValueSerializer = hashValueSerializer;
	}

	public void setCollectionType(CollectionType collectionType) {
		this.collectionType = collectionType;
	}

	/**
	 * Returns a Message with the view into a {@link RedisStore} identified
	 * by {@link #keyExpression}
	 */
	@SuppressWarnings("unchecked")
	public Message<RedisStore> receive() {
		String key = this.keyExpression.getValue(this.evaluationContext, String.class);
		Assert.hasText(key, "Failed to determine the key for the collection");

		RedisStore store = this.createStoreView(key);

		Object holder = TransactionSynchronizationManager.getResource(this);
		if (holder != null) {
			Assert.isInstanceOf(MessageSourceResourceHolder.class, holder);
			((MessageSourceResourceHolder) holder).addAttribute("store", store);
		}

		if (store instanceof Collection<?> && ((Collection<Object>)store).size() < 1){
			return null;
		}
		else {
			return MessageBuilder.withPayload(store).build();
		}
	}

	private RedisStore createStoreView(String key){
		RedisCollectionFactoryBean fb = new RedisCollectionFactoryBean();
		fb.setKey(key);
		fb.setTemplate(this.redisTemplate);
		fb.setType(this.collectionType);
		fb.afterPropertiesSet();
		return fb.getObject();
	}

	@Override
	protected void onInit() throws Exception {
		if (this.getBeanFactory() != null){
			this.evaluationContext =
					ExpressionUtils.createStandardEvaluationContext(this.getBeanFactory());
		}
		else {
			this.evaluationContext = ExpressionUtils.createStandardEvaluationContext();
		}

		if (this.redisTemplateNotSet){
			if (this.keySerializer != null){
				redisTemplate.setKeySerializer(this.keySerializer);
			}
			if (this.valueSerializer != null){
				redisTemplate.setValueSerializer(this.valueSerializer);
			}
			if (this.hashKeySerializer != null){
				redisTemplate.setHashKeySerializer(this.hashKeySerializer);
			}
			if (this.hashValueSerializer != null){
				redisTemplate.setHashValueSerializer(this.hashValueSerializer);
			}
			this.redisTemplate.afterPropertiesSet();
		}
	}

	public RedisStore getResource() {
		return resourceHolder.get();
	}

	public void afterCommit(Object object) {
		this.resourceHolder.remove();
	}

	public void afterRollback(Object object) {
		this.resourceHolder.remove();
	}
}
