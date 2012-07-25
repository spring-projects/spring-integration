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
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.Message;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.util.ExpressionUtils;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
/**
 * Base adapter class that returns messages from Redis collections such as
 * List and Zset (see {@link RedisListInboundChannelAdapter} and {@link RedisZsetInboundChannelAdapter}
 *
 * @author Oleg Zhurakousky
 * @since 2.2
 */
public abstract class AbstractRedisCollectionInboundChannelAdapter extends IntegrationObjectSupport {

	protected volatile StandardEvaluationContext evaluationContext;

	protected volatile Expression keyExpression;

	protected final RedisOperations<String, Object> redisTemplate;

	public AbstractRedisCollectionInboundChannelAdapter(RedisConnectionFactory connectionFactory, Expression keyExpression) {
		Assert.notNull(keyExpression, "'keyExpression' must not be null");
		Assert.notNull(connectionFactory, "'connectionFactory' must not be null");
		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<String, Object>();
		redisTemplate.setConnectionFactory(connectionFactory);
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
		this.redisTemplate = redisTemplate;
		this.keyExpression = keyExpression;
	}

	/**
	 * Returns a Message containing the value for Redis collection identified via {@link #keyExpression}
	 */
	public Message<Collection<Object>> receive() {
		Message<Collection<Object>> message = null;
		String key = this.keyExpression.getValue(this.evaluationContext, String.class);
		Collection<Object> values = doPoll(key);
		if (!CollectionUtils.isEmpty(values)){
			message = MessageBuilder.withPayload(values).build();
		}
		return message;
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

	protected abstract Collection<Object> doPoll(String key);

}
