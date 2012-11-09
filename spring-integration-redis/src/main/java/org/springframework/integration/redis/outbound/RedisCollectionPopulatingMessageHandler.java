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
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.BoundZSetOperations;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
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
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.redis.support.RedisHeaders;
import org.springframework.util.Assert;
import org.springframework.util.NumberUtils;

/**
 * Implementation of {@link MessageHandler} which writes Message data into a Redis store
 * identified by a key {@link String}.
 * It supports the collection types identified by {@link CollectionType}.
 *
 * It also supports batch updates and single item entry.
 *
 * "Batch updates" means that the payload of the Message may be a Map or Collection.
 * With such a payload, individual items from it are added to the corresponding Redis store.
 * See {@link #handleMessage(Message)} for more details.
 *
 * You can also choose to persist such a payload as a single item if the {@link #extractPayloadElements}
 * property is set to false (default is true).
 *
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Mark Fisher
 * @since 2.2
 */
public class RedisCollectionPopulatingMessageHandler extends AbstractMessageHandler {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final Expression zsetIncrementScoreExpression =
			new SpelExpressionParser().parseExpression("headers." + RedisHeaders.ZSET_INCREMENT_SCORE);

	private volatile StandardEvaluationContext evaluationContext;

	private volatile Expression keyExpression =
			new SpelExpressionParser().parseExpression("headers." + RedisHeaders.KEY);

	private volatile Expression mapKeyExpression =
			new SpelExpressionParser().parseExpression("headers." + RedisHeaders.MAP_KEY);

	private volatile boolean mapKeyExpressionExplicitlySet;

	private volatile RedisTemplate<String, ?> redisTemplate = new StringRedisTemplate();

	private volatile boolean redisTemplateExplicitlySet;

	private volatile CollectionType collectionType = CollectionType.LIST;

	private volatile boolean extractPayloadElements = true;

	private volatile RedisConnectionFactory connectionFactory;

	private volatile boolean initialized;

	/**
	 * Constructs an instance using the
	 * provided {@link RedisTemplate}. The RedisTemplate must
	 * be fully initialized.
	 *
	 * The default expression 'headers.{@link RedisHeaders#KEY}'
	 * will be used.
	 * @param redisTemplate
	 */
	public RedisCollectionPopulatingMessageHandler(RedisTemplate<String, ?> redisTemplate) {
		this(redisTemplate, null);
	}

	/**
	 * Constructs an instance using the
	 * provided {@link RedisTemplate} and {@link #keyExpression}. The RedisTemplate must
	 * be fully initialized.
	 * If {@link #keyExpression} is null, the default expression 'headers.{@link RedisHeaders#KEY}'
	 * will be used.
	 *
	 * @param redisTemplate
	 * @param keyExpression
	 */
	public RedisCollectionPopulatingMessageHandler(RedisTemplate<String, ?> redisTemplate, Expression keyExpression) {
		Assert.notNull(redisTemplate, "'redisTemplate' must not be null");
		this.redisTemplate = redisTemplate;
		this.redisTemplateExplicitlySet = true;
		if (keyExpression != null) {
			this.keyExpression = keyExpression;
		}
	}

	/**
	 * Constructs an instance using the provided {@link RedisConnectionFactory}.
	 * It will use either a {@link StringRedisTemplate} if {@link #extractPayloadElements} is
	 * true (default) or a {@link RedisTemplate} with {@link StringRedisSerializer}s for
	 * keys and hash keys and {@link JdkSerializationRedisSerializer}s for values and
	 * hash values, when it is false.
	 *
	 * The default expression 'headers.{@link RedisHeaders#KEY}'
	 * will be used.
	 * @see #setExtractPayloadElements(boolean)
	 * @param connectionFactory
	 */
	public RedisCollectionPopulatingMessageHandler(RedisConnectionFactory connectionFactory) {
		this(connectionFactory, null);
	}

	/**
	 * Constructs an instance using the provided {@link RedisConnectionFactory} and {@link #keyExpression}
	 * It will use either a {@link StringRedisTemplate} if {@link #extractPayloadElements} is
	 * true (default) or a {@link RedisTemplate} with {@link StringRedisSerializer}s for
	 * keys and hash keys and {@link JdkSerializationRedisSerializer}s for values and
	 * hash values, when it is false.
	 *
	 * If {@link #keyExpression} is null, the default expression 'headers.{@link RedisHeaders#KEY}'
	 * will be used.
	 *
	 * @see #setExtractPayloadElements(boolean)
	 * @param connectionFactory
	 * @param keyExpression
	 */
	public RedisCollectionPopulatingMessageHandler(RedisConnectionFactory connectionFactory, Expression keyExpression) {
		Assert.notNull(connectionFactory, "'connectionFactory' must not be null");
		this.connectionFactory = connectionFactory;
		if (keyExpression != null) {
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
	 * Sets the flag signifying that if the payload is a "multivalue" (i.e., Collection or Map),
	 * it should be saved using addAll/putAll semantics. Default is 'true'.
	 * If set to 'false' the payload will be saved as a single entry regardless of its type.
	 * If the payload is not an instance of "multivalue" (i.e., Collection or Map)
	 * the value of this attribute is meaningless as the payload will always be
	 * stored as a single entry.
	 *
	 * @param extractPayloadElements
	 */
	public void setExtractPayloadElements(boolean extractPayloadElements) {
		this.extractPayloadElements = extractPayloadElements;
	}

	/**
	 * Sets the expression used as the key for Map and Properties entries.
	 * Default is 'headers.{@link RedisHeaders#MAP_KEY}'
	 * @param mapKeyExpression
	 */
	public void setMapKeyExpression(Expression mapKeyExpression) {
		Assert.notNull(mapKeyExpression, "'mapKeyExpression' must not be null");
		this.mapKeyExpression = mapKeyExpression;
		this.mapKeyExpressionExplicitlySet = true;
	}

	@Override
	public String getComponentType() {
		return "redis:store-outbound-channel-adapter";
	}

	@Override
	protected void onInit() throws Exception {
		if (this.getBeanFactory() != null) {
			this.evaluationContext =
					ExpressionUtils.createStandardEvaluationContext(this.getBeanFactory());
		}
		else {
			this.evaluationContext = ExpressionUtils.createStandardEvaluationContext();
		}
		Assert.state(!this.mapKeyExpressionExplicitlySet ||
				(this.collectionType == CollectionType.MAP || this.collectionType == CollectionType.PROPERTIES),
				"'mapKeyExpression' can only be set for CollectionType.MAP or CollectionType.PROPERTIES");
		if (!this.redisTemplateExplicitlySet) {
			if (!this.extractPayloadElements) {
				RedisTemplate<String, Object> template = new RedisTemplate<String, Object>();
				StringRedisSerializer serializer = new StringRedisSerializer();
				template.setKeySerializer(serializer);
				template.setHashKeySerializer(serializer);
				this.redisTemplate = template;
			}
			this.redisTemplate.setConnectionFactory(this.connectionFactory);
			this.redisTemplate.afterPropertiesSet();
		}
		this.initialized = true;
	}

	/**
	 * Will extract payload from the Message storing it in the collection identified by the
	 * {@link #collectionType}. The default CollectinType is LIST.
	 * <p/>
	 * The rules for storing payload are:
	 * <p/>
	 * <b>LIST/SET</b>
	 * If payload is of type Collection and {@link #extractPayloadElements} is 'true' (default),
	 * the payload will be added using the addAll() method. If {@link #extractPayloadElements} is set to 'false' then,
	 * regardless of the payload type, the payload will be added using add();
	 * <p/>
	 * <b>ZSET</b>
	 * In addition to rules described for LIST/SET, ZSET allows 'score' information
	 * to be provided. The score can be provided using the {@link RedisHeaders#ZSET_SCORE} message header,
	 * when the payload is a Collection, or
	 * by sending a Map as the payload, where the Map 'key' is the value to be saved and the 'value' is
	 * the score assigned to this value.
	 * If {@link #extractPayloadElements} is set to 'false' the map will be stored as a single entry.
	 * If the 'score' can not be determined, the default value (1) will be used.
	 * <p/>
	 * <b>MAP/PROPERTIES</b>
	 * You can also store a payload of type Map or Properties following the same rules as above.
	 * If payload itself needs to be stored as a value of the map/property then the map key must be
	 * specified via the mapKeyExpression (default {@link RedisHeaders#MAP_KEY} Message header).
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		String key = this.keyExpression.getValue(this.evaluationContext, message, String.class);

		Assert.hasText(key, "Cannot determine a 'key' for a Redis store. The key can be provided via the " +
				"'key' or 'key-expression' attributes.");

		RedisStore store = this.createStoreView(key);

		Assert.state(this.initialized, "handler not initialized");
		try {
			if (collectionType == CollectionType.ZSET) {
				this.handleZset((RedisZSet<Object>) store, message);
			}
			else if (collectionType == CollectionType.SET) {
				this.handleSet((RedisSet<Object>) store, message);
			}
			else if (collectionType == CollectionType.LIST) {
				this.handleList((RedisList<Object>) store, message);
			}
			else if (collectionType == CollectionType.MAP) {
				this.handleMap((RedisMap<Object, Object>) store, message);
			}
			else if (collectionType == CollectionType.PROPERTIES) {
				this.handleProperties((RedisProperties) store, message);
			}
		}
		catch (Exception e) {
			throw new MessageHandlingException(message, "Failed to store Message data in Redis collection", e);
		}
	}

	@SuppressWarnings("unchecked")
	private void handleZset(RedisZSet<Object> zset, final Message<?> message) throws Exception{
		final Object payload = message.getPayload();
		final BoundZSetOperations<String, Object> ops =
				(BoundZSetOperations<String, Object>) this.redisTemplate.boundZSetOps(zset.getKey());
		final boolean zsetIncrementHeader = this.extractZsetIncrementHeader(message);

		if (this.extractPayloadElements) {

			if ((payload instanceof Map<?, ?> && this.isMapValuesOfTypeNumber((Map<?, ?>) payload))) {
				final Map<Object, Number> payloadAsMap = (Map<Object, Number>) payload;
				this.processInPipeline(new PipelineCallback() {
					public void process() {
						for (Entry<Object, Number> entry : payloadAsMap.entrySet()) {
							Number d = entry.getValue();
							incrementOrOverwrite(ops, entry.getKey(), d == null ?
									determineScore(message) :
									NumberUtils.convertNumberToTargetClass(d, Double.class),
									zsetIncrementHeader);
						}
					}
				});
			}
			else if (payload instanceof Collection<?>) {
				this.processInPipeline(new PipelineCallback() {
					public void process() {
						for (Object object : ((Collection<?>)payload)) {
							incrementOrOverwrite(ops, object, determineScore(message), zsetIncrementHeader);
						}
					}
				});
			}
			else {
				this.incrementOrOverwrite(ops, payload, this.determineScore(message), zsetIncrementHeader);
			}
		}
		else {
			this.incrementOrOverwrite(ops, payload, this.determineScore(message), zsetIncrementHeader);
		}
	}

	private boolean extractZsetIncrementHeader(Message<?> message){
		if (message.getHeaders().containsKey(RedisHeaders.ZSET_INCREMENT_SCORE)){
			return this.zsetIncrementScoreExpression.getValue(this.evaluationContext, message, Boolean.class);
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	private void handleList(RedisList<Object> list, Message<?> message) {
		Object payload = message.getPayload();
		if (this.extractPayloadElements) {
			if (payload instanceof Collection<?>) {
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
	private void handleSet(final RedisSet<Object> set, Message<?> message) {
		final Object payload = message.getPayload();
		if (this.extractPayloadElements && payload instanceof Collection<?>) {
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
	private void handleMap(final RedisMap<Object, Object> map, Message<?> message) {
		final Object payload = message.getPayload();
		if (this.extractPayloadElements && payload instanceof Map<?, ?>) {
			this.processInPipeline(new PipelineCallback() {
				public void process() {
					map.putAll((Map<? extends Object, ? extends Object>) payload);
				}
			});
		}
		else {
			Object key = this.assertMapEntry(message, false);
			map.put(key, payload);
		}
	}

	private void handleProperties(final RedisProperties properties, Message<?> message) {
		final Object payload = message.getPayload();
		if (this.extractPayloadElements && payload instanceof Properties) {
			this.processInPipeline(new PipelineCallback() {
				public void process() {
					properties.putAll((Properties) payload);
				}
			});
		}
		else {
			Object key = this.assertMapEntry(message, true);
			properties.put(key, payload);
		}
	}

	private void processInPipeline(PipelineCallback callback) {
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

	private Object assertMapEntry(Message<?> message, boolean property) {
		Object mapKey = this.mapKeyExpression.getValue(this.evaluationContext, message);
		Assert.notNull(mapKey, "Cannot determine a map key for the entry. The key is determined by evaluating " +
				"the 'mapKeyExpression' property.");
		Object payload = message.getPayload();
		if (property) {
			Assert.isInstanceOf(String.class, mapKey, "For property, key must be a String");
			Assert.isInstanceOf(String.class, payload, "For property, payload must be a String");
		}
		Assert.isTrue(mapKey != null, "Failed to determine the key for the " +
				"Redis Map entry. Payload is not a Map and '" + RedisHeaders.MAP_KEY +
				"' header is not provided");
		return mapKey;
	}

	private void incrementOrOverwrite(final BoundZSetOperations<String, Object> ops, Object object, Double score,
			boolean zsetIncrementScore) {

		if (score != null) {
			this.doIncrementOrOverwrite(ops, object, score, zsetIncrementScore);
		}
		else {
			logger.debug("Zset Score could not be determined. Using default score of 1");
			this.doIncrementOrOverwrite(ops, object, Double.valueOf(1), zsetIncrementScore);
		}
	}

	private void doIncrementOrOverwrite(final BoundZSetOperations<String, Object> ops, Object object, Double score,
			boolean increment) {
		if (increment) {
			ops.incrementScore(object, score);
		}
		else {
			ops.add(object, score);
		}
	}

	private boolean isMapValuesOfTypeNumber(Map<?,?> map) {
		for (Object value : map.values()) {
			if (!(value instanceof Number)) {
				logger.warn("Failed to extract payload elements because one of its values '" + value + "' is not of type Number");
				return false;
			}
		}
		return true;
	}

	private RedisStore createStoreView(String key) {
		RedisCollectionFactoryBean fb = new RedisCollectionFactoryBean();
		fb.setKey(key);
		fb.setTemplate(this.redisTemplate);
		fb.setType(this.collectionType);
		fb.afterPropertiesSet();
		return fb.getObject();
	}

	private double determineScore(Message<?> message) {
		Object scoreHeader = message.getHeaders().get(RedisHeaders.ZSET_SCORE);
		if (scoreHeader == null) {
			return Double.valueOf(1);
		}
		else {
			Assert.isInstanceOf(Number.class, scoreHeader, "Header " + RedisHeaders.ZSET_SCORE + " must be a Number");
			Number score = (Number) scoreHeader;
			return Double.valueOf(score.toString());
		}
	}

	private interface PipelineCallback {
		public void process();
	}
}
