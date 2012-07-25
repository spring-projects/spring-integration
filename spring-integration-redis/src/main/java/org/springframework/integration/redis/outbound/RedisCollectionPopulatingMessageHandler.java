/*
 * Copyright 2007-2012 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package org.springframework.integration.redis.outbound;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.support.collections.RedisCollectionFactoryBean;
import org.springframework.data.redis.support.collections.RedisCollectionFactoryBean.CollectionType;
import org.springframework.data.redis.support.collections.RedisList;
import org.springframework.data.redis.support.collections.RedisMap;
import org.springframework.data.redis.support.collections.RedisProperties;
import org.springframework.data.redis.support.collections.RedisSet;
import org.springframework.data.redis.support.collections.RedisStore;
import org.springframework.data.redis.support.collections.RedisZSet;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.redis.support.RedisHeaders;
import org.springframework.integration.util.ExpressionUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * @author Oleg Zhurakousky
 * @since 2.2
 */
public class RedisCollectionPopulatingMessageHandler extends AbstractMessageHandler {

	private final Log logger = LogFactory.getLog(this.getClass());

	protected volatile StandardEvaluationContext evaluationContext;

	protected volatile Expression keyExpression;

	protected final RedisTemplate<String, Object> redisTemplate;

	private volatile CollectionType collectionType = CollectionType.LIST;

	private volatile boolean parsePayload = true;


	public RedisCollectionPopulatingMessageHandler(RedisConnectionFactory connectionFactory) {
		this(connectionFactory, null);
	}

	public RedisCollectionPopulatingMessageHandler(RedisConnectionFactory connectionFactory, Expression keyExpression) {
		Assert.notNull(connectionFactory, "'connectionFactory' must not be null");

		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<String, Object>();
		redisTemplate.setConnectionFactory(connectionFactory);
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());

		this.redisTemplate = redisTemplate;
		this.keyExpression = keyExpression;
	}

	/**
	 * Sets the collection type for this handler as per {@link CollectionType}
	 *
	 * @param collectionType
	 */
	public void setCollectionType(CollectionType collectionType) {
		this.collectionType = collectionType;
	}

	/**
	 * Sets the flag signifying that the payload should be parsed
	 * and its entries (e.g., Collection/Map) are entered as individual entries
	 * into the underlying Redis collection.
	 *
	 * @param parsePayload
	 */
	public void setParsePayload(boolean parsePayload) {
		this.parsePayload = parsePayload;
	}

	@Override
	public String getComponentType() {
		return "redis:store-outbound-channel-adapter";
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		RedisConnectionUtils.bindConnection(redisTemplate.getConnectionFactory());
		String key = null;
		if (this.keyExpression == null){
			key = (String) message.getHeaders().get(RedisHeaders.KEY);
		}
		else {
			key = this.keyExpression.getValue(this.evaluationContext, String.class);
		}
		Assert.hasText(key, "Can not determine a 'key' for a Redis store. The key can be provide via " +
				"'key' or 'key-expression' atribute or the '" + RedisHeaders.KEY + "' Message header");

		RedisStore store = this.createStoreView(key);

		try {
			if (collectionType == CollectionType.ZSET){
				this.handleZset((RedisZSet<Object>) store, message);
			}
			else if (collectionType == CollectionType.SET){
				this.handleSet((RedisSet<Object>) store, message);
			}
			else if (collectionType == CollectionType.LIST){
				this.handleList((RedisList<Object>) store, message);
			}
			else if (collectionType == CollectionType.LIST){
				this.handleMap((RedisMap<Object, Object>) store, message);
			}
			else if (collectionType == CollectionType.PROPERTIES){
				this.handleProperties((RedisProperties) store, message);
			}
		}
		catch (Exception e) {
			throw new MessageHandlingException(message, "Failed to store Message data in Redis collection", e);
		}
		finally {
			RedisConnectionUtils.unbindConnection(redisTemplate.getConnectionFactory());
		}
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

	@SuppressWarnings("unchecked")
	private void handleZset(final RedisZSet<Object> zset, Message<?> message) throws Exception{
		Object payload = message.getPayload();

		if (this.parsePayload){
			if ((payload instanceof Map<?, ?> && this.isMapValuesOfTypeDouble((Map<?, ?>) payload))) {
				final Map<Object, Double> pyloadAsMap = (Map<Object, Double>) payload;
				this.processInPipelineOrTransaction(new Runnable() {
					public void run() {
						for (final Object key : pyloadAsMap.keySet()) {
							addToZset(zset, key, pyloadAsMap.get(key));
						}
					}
				});
			}
			else if (payload instanceof Collection<?>){
				zset.addAll((Collection<? extends Object>) payload);
			}
			else {
				Number score = (Number) message.getHeaders().get(RedisHeaders.ZSET_SCORE);
				if (score == null){
					score = 1;
				}
				this.addToZset(zset, payload, this.determineScore(message));
			}
		}
		else {
			this.addToZset(zset, message, this.determineScore(message));
		}
	}

	private void processInPipelineOrTransaction(Runnable callback) throws Exception{
		boolean inTransaction = TransactionSynchronizationManager.isActualTransactionActive();

		if (inTransaction){
			redisTemplate.multi();
		}
		else {
			redisTemplate.getConnectionFactory().getConnection().openPipeline();
		}

		try {
			callback.run();

			if (inTransaction){
				redisTemplate.exec();
			}
			else {
				redisTemplate.getConnectionFactory().getConnection().closePipeline();
			}
		} catch (Exception e) {
			redisTemplate.discard();
			throw e;
		}
	}

	@SuppressWarnings("unchecked")
	private void handleList(RedisList<Object> list, Message<?> message){
		Object payload = message.getPayload();
		if (this.parsePayload){
			if (payload instanceof Collection<?>){
				list.addAll((Collection<? extends Object>) payload);
			}
			else {
				list.add(payload);
			}
		}
		else {
			list.add(payload);
		}
	}

	@SuppressWarnings("unchecked")
	private void handleSet(RedisSet<Object> set, Message<?> message){
		Object payload = message.getPayload();
		if (this.parsePayload){
			if (payload instanceof Collection<?>){
				set.addAll((Collection<? extends Object>) payload);
			}
			else {
				set.add(payload);
			}
		}
		else {
			set.add(payload);
		}
	}

	@SuppressWarnings("unchecked")
	private void handleMap(RedisMap<Object, Object> map, Message<?> message){
		Object payload = message.getPayload();
		if (this.parsePayload){
			if (payload instanceof Map<?, ?>){
				map.putAll((Map<? extends Object, ? extends Object>) payload);
			}
			else {
				this.assertMapEntry(message, false);
				map.put(message.getHeaders().get(RedisHeaders.MAP_KEY), payload);
			}
		}
		else {
			this.assertMapEntry(message, false);
			map.put(message.getHeaders().get(RedisHeaders.MAP_KEY), payload);
		}
	}

	@SuppressWarnings("unchecked")
	private void handleProperties(RedisProperties properties, Message<?> message){
		Object payload = message.getPayload();
		if (this.parsePayload){
			if (payload instanceof Properties){
				properties.putAll((Map<? extends Object, ? extends Object>) payload);
			}
			else {
				this.assertMapEntry(message, true);
				properties.put(message.getHeaders().get(RedisHeaders.MAP_KEY), payload);
			}
		}
		else {
			this.assertMapEntry(message, true);
			properties.put(message.getHeaders().get(RedisHeaders.MAP_KEY), payload);
		}
	}

	private void assertMapEntry(Message<?> message, boolean property){
		Object mapKey = message.getHeaders().get(RedisHeaders.MAP_KEY);
		Object payload = message.getPayload();
		if (property){
			Assert.isInstanceOf(String.class, mapKey);
		}
		if (property){
			Assert.isInstanceOf(String.class, payload);
		}
		Assert.isTrue(mapKey != null, "Failed to determine the key for the " +
				"Redis Map entry. Payload is not a Map and '" + RedisHeaders.MAP_KEY +
				"' header is not provided");
	}

	private void addToZset(RedisZSet<Object> zset, Object objectToAdd, Double score) {
		if (score != null){
			zset.add(objectToAdd, score);
		}
		else {
			logger.debug("Zset Score could not be determined. Using default score of 1");
			zset.add(objectToAdd);
		}
	}

	public boolean isMapValuesOfTypeDouble(Map<?,?> map){
		for (Object value : map.values()) {
			if (!(value instanceof Double)){
				logger.warn("Failed to parse payload since one of its values '" + value + "' is not of type Double");
				return false;
			}
		}
		return true;
	}

	private RedisStore createStoreView(String key){
		RedisCollectionFactoryBean fb = new RedisCollectionFactoryBean();
		fb.setKey(key);
		fb.setTemplate(this.redisTemplate);
		fb.setType(this.collectionType);
		fb.afterPropertiesSet();
		return fb.getObject();
	}

	private double determineScore(Message<?> message){
		Number score = (Number) message.getHeaders().get("redis_zset_score");
		if (score == null){
			score = 1;
		}
		return Double.valueOf(score.toString());
	}
}
