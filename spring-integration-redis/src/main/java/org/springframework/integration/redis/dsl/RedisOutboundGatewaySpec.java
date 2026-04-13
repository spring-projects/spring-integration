/*
 * Copyright 2026-present the original author or authors.
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

package org.springframework.integration.redis.dsl;

import java.util.function.Function;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.expression.Expression;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.redis.outbound.ArgumentsStrategy;
import org.springframework.integration.redis.outbound.RedisOutboundGateway;
import org.springframework.messaging.Message;

/**
 * A {@link MessageHandlerSpec} for a {@link RedisOutboundGateway}.
 *
 * @author Jiandong Ma
 *
 * @since 7.1
 */
public class RedisOutboundGatewaySpec extends MessageHandlerSpec<RedisOutboundGatewaySpec, RedisOutboundGateway> {

	protected RedisOutboundGatewaySpec(RedisTemplate<?, ?> redisTemplate) {
		this.target = new RedisOutboundGateway(redisTemplate);
	}

	protected RedisOutboundGatewaySpec(RedisConnectionFactory connectionFactory) {
		this.target = new RedisOutboundGateway(connectionFactory);
	}

	/**
	 * Specify the argument serializer.
	 * @param serializer the serializer
	 * @return the spec
	 * @see RedisOutboundGateway#setArgumentsSerializer(RedisSerializer)
	 */
	public RedisOutboundGatewaySpec argumentsSerializer(RedisSerializer<?> serializer) {
		this.target.setArgumentsSerializer(serializer);
		return this;
	}

	/**
	 * Specify the command expression.
	 * @param commandExpression the commandExpression
	 * @return the spec
	 * @see RedisOutboundGateway#setCommandExpression(Expression)
	 */
	public RedisOutboundGatewaySpec commandExpression(Expression commandExpression) {
		this.target.setCommandExpression(commandExpression);
		return this;
	}

	/**
	 * Specify the command expression string.
	 * @param commandExpression the commandExpression
	 * @return the spec
	 * @see RedisOutboundGateway#setCommandExpressionString(String)
	 */
	public RedisOutboundGatewaySpec commandExpression(String commandExpression) {
		this.target.setCommandExpressionString(commandExpression);
		return this;
	}

	/**
	 * Specify the command function.
	 * @param commandFunction the commandFunction
	 * @return the spec
	 * @see RedisOutboundGateway#setCommandExpression(Expression)
	 */
	public RedisOutboundGatewaySpec commandFunction(Function<Message<?>, String> commandFunction) {
		this.target.setCommandExpression(new FunctionExpression<>(commandFunction));
		return this;
	}

	/**
	 * Specify arguments strategy.
	 * @param argumentsStrategy the argumentsStrategy
	 * @return the spec
	 * @see RedisOutboundGateway#setArgumentsStrategy(ArgumentsStrategy)
	 */
	public RedisOutboundGatewaySpec argumentsStrategy(ArgumentsStrategy argumentsStrategy) {
		this.target.setArgumentsStrategy(argumentsStrategy);
		return this;
	}

}
