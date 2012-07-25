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
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.support.collections.RedisCollectionFactoryBean;
import org.springframework.data.redis.support.collections.RedisCollectionFactoryBean.CollectionType;
import org.springframework.data.redis.support.collections.RedisStore;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.Message;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.core.PseudoTransactionalMessageSource;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.util.ExpressionUtils;
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
public class RedisStoreInboundChannelAdapter extends IntegrationObjectSupport
		implements PseudoTransactionalMessageSource<RedisStore, RedisStore> {

	private final ThreadLocal<RedisStore> resourceHolder = new ThreadLocal<RedisStore>();

	private volatile StandardEvaluationContext evaluationContext;

	private volatile Expression keyExpression;

	private volatile CollectionType collectionType = CollectionType.LIST;

	private final RedisTemplate<String, Object> redisTemplate;

	public RedisStoreInboundChannelAdapter(RedisConnectionFactory connectionFactory,
									       Expression keyExpression) {

		Assert.notNull(keyExpression, "'keyExpression' must not be null");
		Assert.notNull(connectionFactory, "'connectionFactory' must not be null");

		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<String, Object>();
		redisTemplate.setConnectionFactory(connectionFactory);
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
		this.redisTemplate = redisTemplate;

		this.keyExpression = keyExpression;
		this.collectionType = collectionType;
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
		this.resourceHolder.set(store);

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

	public void afterReceiveNoTx(RedisStore resource) {
		this.resourceHolder.remove();
	}

	public void afterSendNoTx(RedisStore resource) {
		this.resourceHolder.remove();
	}
}
