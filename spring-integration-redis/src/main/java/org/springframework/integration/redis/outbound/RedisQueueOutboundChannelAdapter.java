/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.integration.redis.outbound;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Rainer Frey
 * @since 3.0
 */
public class RedisQueueOutboundChannelAdapter extends AbstractMessageHandler {

	private final RedisSerializer<String> stringSerializer = new StringRedisSerializer();

	private final RedisTemplate<String, Object> template;

	private final Expression queueNameExpression;

	private volatile EvaluationContext evaluationContext;

	private volatile boolean extractPayload = true;

	private volatile RedisSerializer<?> serializer = new JdkSerializationRedisSerializer();

	private volatile boolean serializerExplicitlySet;

	private volatile boolean leftPush = true;

	public RedisQueueOutboundChannelAdapter(String queueName, RedisConnectionFactory connectionFactory) {
		this(new LiteralExpression(queueName), connectionFactory);
	}

	public RedisQueueOutboundChannelAdapter(Expression queueNameExpression, RedisConnectionFactory connectionFactory) {
		Assert.notNull(queueNameExpression, "'queueNameExpression' is required");
		Assert.hasText(queueNameExpression.getExpressionString(), "'queueNameExpression.getExpressionString()' is required");
		Assert.notNull(connectionFactory, "'connectionFactory' must not be null");
		this.queueNameExpression = queueNameExpression;
		this.template = new RedisTemplate<String, Object>();
		this.template.setConnectionFactory(connectionFactory);
		this.template.setEnableDefaultSerializer(false);
		this.template.setKeySerializer(new StringRedisSerializer());
		this.template.afterPropertiesSet();
	}

	public void setExtractPayload(boolean extractPayload) {
		this.extractPayload = extractPayload;
	}

	public void setSerializer(RedisSerializer<?> serializer) {
		Assert.notNull(serializer, "'serializer' must not be null");
		this.serializer = serializer;
		this.serializerExplicitlySet = true;
	}

	/**
	 * Specify if {@code PUSH} operation to Redis List should be {@code LPUSH} or {@code RPUSH}.
	 * @param leftPush the {@code LPUSH} flag. Defaults to {@code true}.
	 * @since 4.3
	 */
	public void setLeftPush(boolean leftPush) {
		this.leftPush = leftPush;
	}

	public void setIntegrationEvaluationContext(EvaluationContext evaluationContext) {
		this.evaluationContext = evaluationContext;
	}

	@Override
	public String getComponentType() {
		return "redis:queue-outbound-channel-adapter";
	}

	@Override
	protected void onInit() {
		super.onInit();
		if (this.evaluationContext == null) {
			this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void handleMessageInternal(Message<?> message) {
		Object value = message;

		if (this.extractPayload) {
			value = message.getPayload();
		}

		if (!(value instanceof byte[])) {
			if (value instanceof String && !this.serializerExplicitlySet) {
				value = this.stringSerializer.serialize((String) value);
			}
			else {
				value = ((RedisSerializer<Object>) this.serializer).serialize(value);
			}
		}

		String queueName = this.queueNameExpression.getValue(this.evaluationContext, message, String.class);
		// TODO: 5.2 assert both not null
		if (this.leftPush) {
			this.template.boundListOps(queueName).leftPush(value); // NOSONAR
		}
		else {
			this.template.boundListOps(queueName).rightPush(value); // NOSONAR
		}
	}

}
