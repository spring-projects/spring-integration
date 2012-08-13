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

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.BoundZSetOperations;
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
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.redis.support.RedisHeaders;
import org.springframework.integration.util.ExpressionUtils;
import org.springframework.util.Assert;
import org.springframework.util.NumberUtils;

/**
 * Implementation of the {@link MessageHandler} which writes Message data into a Redis store
 * identified by key {@link String}.
 * It supports the 5 collection types identified by {@link CollectionType}
 *
 * It also supports batch updates and single item entry.
 *
 * Batch updates means that the payload of the Message may be a Map or Collection.
 * Such payload is parsed and individual items are added to the corresponding Redis store
 * see {@link #handleMessage(Message)} for more details.
 *
 * You can also chose to persist such payload as a single item if you {@link #parsePayload}
 * property is set to false
 *
 * @author Oleg Zhurakousky
 * @since 2.2
 */
public class RedisCollectionPopulatingMessageHandler extends AbstractMessageHandler {

	private final Log logger = LogFactory.getLog(this.getClass());

	protected volatile StandardEvaluationContext evaluationContext;

	protected volatile Expression keyExpression =
			new SpelExpressionParser().parseExpression("headers." + RedisHeaders.KEY);

	protected final RedisTemplate<String, ?> redisTemplate;

	private volatile CollectionType collectionType = CollectionType.LIST;

	private volatile boolean storePayloadAsSingleValue = false;

	/**
	 * Will construct this instance using fully created and initialized instance of
	 * provided {@link RedisTemplate}
	 *
	 * @param redisTemplate
	 */
	public RedisCollectionPopulatingMessageHandler(RedisTemplate<String, ?> redisTemplate) {
		this(redisTemplate, null);
	}

	/**
	 * Will construct this instance using fully created and initialized instance of
	 * provided {@link RedisTemplate} and {@link #keyExpression}.
	 * If {@link #keyExpression} is null default expression 'headers.{@link RedisHeaders#KEY}'
	 * will be used.
	 *
	 * @param redisTemplate
	 * @param keyExpression
	 */
	public RedisCollectionPopulatingMessageHandler(RedisTemplate<String, ?> redisTemplate, Expression keyExpression) {
		Assert.notNull(redisTemplate, "'redisTemplate' must not be null");
		this.redisTemplate = redisTemplate;
		if (keyExpression != null){
			this.keyExpression = keyExpression;
		}
	}

	/**
	 * Will construct this instance using provided {@link RedisConnectionFactory}.
	 * It will create an instance of {@link RedisTemplate} initializing it with
	 * {@link StringRedisSerializer} as keySerializer and {@link JdkSerializationRedisSerializer}
	 * as valueSerializer, hasKeySerializer and hashValueSerializer.
	 *
	 * @param redisTemplate
	 */
	public RedisCollectionPopulatingMessageHandler(RedisConnectionFactory connectionFactory) {
		this(connectionFactory, null);
	}

	/**
	 * Will construct this instance using provided {@link RedisConnectionFactory} and {@link #keyExpression}
	 * It will create an instance of {@link RedisTemplate} initializing it with
	 * {@link StringRedisSerializer} as keySerializer and {@link JdkSerializationRedisSerializer}
	 * as valueSerializer, hasKeySerializer and hashValueSerializer.
	 *
	 * If {@link #keyExpression} is null default expression 'headers.{@link RedisHeaders#KEY}'
	 * will be used.
	 *
	 * @param redisTemplate
	 * @param keyExpression
	 */
	public RedisCollectionPopulatingMessageHandler(RedisConnectionFactory connectionFactory, Expression keyExpression) {
		Assert.notNull(connectionFactory, "'connectionFactory' must not be null");

		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<String, Object>();
		redisTemplate.setConnectionFactory(connectionFactory);
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
		redisTemplate.setHashKeySerializer(new JdkSerializationRedisSerializer());
		redisTemplate.setHashValueSerializer(new JdkSerializationRedisSerializer());

		this.redisTemplate = redisTemplate;
		if (keyExpression != null){
			this.keyExpression = keyExpression;
		}
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
	 * Sets the flag signifying that the payload should be saved as single value
	 * instead of using the semantics of Collection.addAll/putAll in the cases where
	 * payload is Colection or Map. If teh payload is not and instance of
	 * Collection or Map this attribute is meaningless as the payload will always be
	 * stored as a single value.
	 *
	 * @param storePayloadAsSingleValue
	 */
	public void setStorePayloadAsSingleValue(boolean storePayloadAsSingleValue) {
		this.storePayloadAsSingleValue = storePayloadAsSingleValue;
	}

	@Override
	public String getComponentType() {
		return "redis:store-outbound-channel-adapter";
	}

	/**
	 * Will extract payload from the Message storing it in the collection identified by the
	 * {@link #collectionType}. Default is LIST.
	 * The rules for storing payload are:
	 * LIST/SET
	 * If payload is of type Collection and {@link #parsePayload} is 'true' (default)
	 * the payload will be added as addAll() method. If {@link #parsePayload} is set to 'false' then
	 * regardless of the payload type the payload will be added using add();
	 *
	 * ZSET
	 * Aside from following the same rules as described in LIST/SET, ZSET allows the 'score' information
	 * to be provided. The score can be provided using {@link RedisHeaders#ZSET_SCORE} message header or
	 * by by sending a Map as a payload where the 'key' is the value you want to save and the 'value' is
	 * the score assigned to this value.
	 * If {@link #parsePayload} is set to 'false' the map will be stored as a whole.
	 * If 'score' can not be determined the default value 1 will be used.
	 *
	 * MAP/PROPERTIES
	 * You can also store payload of type Map or Properties following the same rules as above.
	 * If payload itself needs to be stored as a value of the map/property then the map key must be
	 * specified via {@link RedisHeaders#MAP_KEY} Message header
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		String key = this.keyExpression.getValue(this.evaluationContext, message, String.class);

		Assert.hasText(key, "Can not determine a 'key' for a Redis store. The key can be provided via " +
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
			else if (collectionType == CollectionType.MAP){
				this.handleMap((RedisMap<Object, Object>) store, message);
			}
			else if (collectionType == CollectionType.PROPERTIES){
				this.handleProperties((RedisProperties) store, message);
			}
		}
		catch (Exception e) {
			throw new MessageHandlingException(message, "Failed to store Message data in Redis collection", e);
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
	private void handleZset(RedisZSet<Object> zset, final Message<?> message) throws Exception{
		final Object payload = message.getPayload();

		if (!this.storePayloadAsSingleValue){
			final BoundZSetOperations<String, Object> ops =
					(BoundZSetOperations<String, Object>) this.redisTemplate.boundZSetOps(zset.getKey());

			if ((payload instanceof Map<?, ?> && this.isMapValuesOfTypeNumber((Map<?, ?>) payload))) {
				final Map<Object, Number> pyloadAsMap = (Map<Object, Number>) payload;
				this.processInPipeline(new PipelineCallback() {
					public void process() {
						for (Object key : pyloadAsMap.keySet()) {
							Number d = pyloadAsMap.get(key);
							ops.add(key, d == null ?
									determineScore(message) :
									NumberUtils.convertNumberToTargetClass(d, Double.class));
						}
					}
				});
			}
			else if (payload instanceof Collection<?>){
				this.processInPipeline(new PipelineCallback() {
					public void process() {
						for (Object object : ((Collection<?>)payload)) {
							ops.add(object, determineScore(message));
						}
					}
				});
			}
			else {
				this.addToZset(zset, payload, this.determineScore(message));
			}
		}
		else {
			this.addToZset(zset, payload, this.determineScore(message));
		}
	}

	@SuppressWarnings("unchecked")
	private void handleList(RedisList<Object> list, Message<?> message){
		Object payload = message.getPayload();
		if (!this.storePayloadAsSingleValue){
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
	private void handleSet(final RedisSet<Object> set, Message<?> message){
		final Object payload = message.getPayload();
		if ((!(this.storePayloadAsSingleValue)) && payload instanceof Collection<?>){
			final BoundSetOperations<String, Object> ops =
					(BoundSetOperations<String, Object>) this.redisTemplate.boundSetOps(set.getKey());

			this.processInPipeline(new PipelineCallback() {
				public void process() {
					for (Object object : ((Collection<?>)payload)) {
						ops.add(object);
					}
				}
			});
		}
		else {
			set.add(payload);
		}
	}

	@SuppressWarnings("unchecked")
	private void handleMap(final RedisMap<Object, Object> map, Message<?> message){
		final Object payload = message.getPayload();
		if ((!(this.storePayloadAsSingleValue)) && payload instanceof Map<?, ?>){
			this.processInPipeline(new PipelineCallback() {
				public void process() {
					map.putAll((Map<? extends Object, ? extends Object>) payload);
				}
			});
		}
		else {
			this.assertMapEntry(message, false);
			map.put(message.getHeaders().get(RedisHeaders.MAP_KEY), payload);
		}
	}

	private void handleProperties(final RedisProperties properties, Message<?> message){
		final Object payload = message.getPayload();
		if ((!(this.storePayloadAsSingleValue)) && payload instanceof Properties){
			this.processInPipeline(new PipelineCallback() {
				public void process() {
					properties.putAll((Properties) payload);
				}
			});
		}
		else {
			this.assertMapEntry(message, true);
			properties.put(message.getHeaders().get(RedisHeaders.MAP_KEY), payload);
		}
	}

	private void processInPipeline(PipelineCallback callback){
		RedisConnection connection =
				RedisConnectionUtils.bindConnection(redisTemplate.getConnectionFactory());
		try {
			connection.openPipeline();
			callback.process();
		}
		finally {
			connection.closePipeline();
			RedisConnectionUtils.unbindConnection(redisTemplate.getConnectionFactory());
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

	private boolean isMapValuesOfTypeNumber(Map<?,?> map){
		for (Object value : map.values()) {
			if (!(value instanceof Number)){
				logger.warn("Failed to parse payload since one of its values '" + value + "' is not of type Number");
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

	private interface PipelineCallback {
		public void process();
	}
}
