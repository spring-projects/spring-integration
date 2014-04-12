/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.redis.outbound;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.expression.IntegrationEvaluationContextAware;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.redis.support.RedisHeaders;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * The Gateway component implementation to perform Redis commands with provided arguments and to return command result.
 *
 * @author Artem Bilan
 * @since 4.0
 */
public class RedisOutboundGateway extends AbstractReplyProducingMessageHandler
		implements IntegrationEvaluationContextAware {

	private static final SpelExpressionParser PARSER = new SpelExpressionParser();

	private final RedisTemplate<?, ?> redisTemplate;

	private EvaluationContext evaluationContext;

	private volatile RedisSerializer<Object> argumentsSerializer = new GenericToStringSerializer<Object>(Object.class);

	private volatile Expression commandExpression = PARSER.parseExpression("headers[" + RedisHeaders.COMMAND + "]");

	private volatile ArgumentsStrategy argumentsStrategy = new PayloadArgumentsStrategy();

	public RedisOutboundGateway(RedisTemplate<?, ?> redisTemplate) {
		Assert.notNull(redisTemplate, "'redisTemplate' must not be null");
		this.redisTemplate = redisTemplate;
	}

	public RedisOutboundGateway(RedisConnectionFactory connectionFactory) {
		Assert.notNull(connectionFactory, "'connectionFactory' must not be null");
		this.redisTemplate = new RedisTemplate<Object, Object>();
		this.redisTemplate.setConnectionFactory(connectionFactory);
		this.redisTemplate.afterPropertiesSet();
	}

	@Override
	public void setIntegrationEvaluationContext(EvaluationContext evaluationContext) {
		Assert.notNull(evaluationContext, "'evaluationContext' must not be null");
		this.evaluationContext = evaluationContext;
	}

	@SuppressWarnings("unchecked")
	public void setArgumentsSerializer(RedisSerializer<?> serializer) {
		Assert.notNull(serializer, "'serializer' must not be null");
		this.argumentsSerializer = (RedisSerializer<Object>) serializer;
	}

	public void setCommandExpression(String commandExpression) {
		Assert.hasText(commandExpression, "'commandExpression' must not be an empty string");
		this.commandExpression = PARSER.parseExpression(commandExpression);
	}

	public void setArgumentsStrategy(ArgumentsStrategy argumentsStrategy) {
		this.argumentsStrategy = argumentsStrategy;
	}

	@Override
	public String getComponentType() {
		return "redis:outbound-gateway";
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		final String command = this.commandExpression.getValue(this.evaluationContext, requestMessage, String.class);
		Assert.notNull(command, "The 'command' must not evaluate to 'null'.");
		byte[][] args = null;
		if (this.argumentsStrategy != null) {
			Object[] arguments = this.argumentsStrategy.resolve(command, requestMessage);
			if (!ObjectUtils.isEmpty(arguments)) {
				args = new byte[arguments.length][];

				for (int i = 0; i < arguments.length; i++) {
					Object argument = arguments[i];
					byte[] arg = null;
					if (argument instanceof byte[]) {
						arg = (byte[]) argument;
					}
					else {
						arg = this.argumentsSerializer.serialize(argument);
					}
					args[i] = arg;
				}
			}
		}

		final byte[][] actualArgs = args;

		return this.redisTemplate.execute(new RedisCallback<Object>() {

			@Override
			public Object doInRedis(RedisConnection connection) throws DataAccessException {
				return connection.execute(command, actualArgs);
			}

		});
	}

	private class PayloadArgumentsStrategy implements ArgumentsStrategy {

		@Override
		public Object[] resolve(String command, Message<?> message) {
			Object payload = message.getPayload();
			if (payload instanceof Object[]) {
				return (Object[]) payload;
			}
			else {
				return new Object[]{payload};
			}
		}

	}

}
