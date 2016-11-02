/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.amqp.dsl;

import java.util.function.Function;

import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.expression.Expression;
import org.springframework.integration.amqp.outbound.AbstractAmqpOutboundEndpoint;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

/**
 * The base {@link MessageHandlerSpec} for {@link AbstractAmqpOutboundEndpoint}s.
 *
 * @param <S> the target {@link AmqpBaseOutboundEndpointSpec} implementation type.
 * @param <E> the target {@link AbstractAmqpOutboundEndpoint} implementation type.
 *
 * @author Artem Bilan
 * @since 5.0
 */
public abstract class
	AmqpBaseOutboundEndpointSpec<S extends AmqpBaseOutboundEndpointSpec<S, E>, E extends AbstractAmqpOutboundEndpoint>
		extends MessageHandlerSpec<S, E> {

	protected final DefaultAmqpHeaderMapper headerMapper = DefaultAmqpHeaderMapper.outboundMapper();

	public S headerMapper(AmqpHeaderMapper headerMapper) {
		this.target.setHeaderMapper(headerMapper);
		return _this();
	}

	public S defaultDeliveryMode(MessageDeliveryMode defaultDeliveryMode) {
		this.target.setDefaultDeliveryMode(defaultDeliveryMode);
		return _this();
	}

	public S routingKey(String routingKey) {
		this.target.setRoutingKey(routingKey);
		return _this();
	}

	public S routingKeyExpression(String routingKeyExpression) {
		return routingKeyExpression(PARSER.parseExpression(routingKeyExpression));
	}

	public S routingKeyFunction(Function<Message<?>, String> routingKeyFunction) {
		return routingKeyExpression(new FunctionExpression<Message<?>>(routingKeyFunction));
	}

	public S routingKeyExpression(Expression routingKeyExpression) {
		this.target.setRoutingKeyExpression(routingKeyExpression);
		return _this();
	}

	public S returnChannel(MessageChannel returnChannel) {
		this.target.setReturnChannel(returnChannel);
		return _this();
	}

	public S confirmAckChannel(MessageChannel ackChannel) {
		this.target.setConfirmAckChannel(ackChannel);
		return _this();
	}

	public S exchangeName(String exchangeName) {
		this.target.setExchangeName(exchangeName);
		return _this();
	}

	public S exchangeNameExpression(String exchangeNameExpression) {
		return exchangeNameExpression(PARSER.parseExpression(exchangeNameExpression));
	}

	public S exchangeNameFunction(Function<Message<?>, String> exchangeNameFunction) {
		return exchangeNameExpression(new FunctionExpression<Message<?>>(exchangeNameFunction));
	}

	public S exchangeNameExpression(Expression exchangeNameExpression) {
		this.target.setExchangeNameExpression(exchangeNameExpression);
		return _this();
	}

	public S confirmNackChannel(MessageChannel nackChannel) {
		this.target.setConfirmNackChannel(nackChannel);
		return _this();
	}

	public S confirmCorrelationExpression(String confirmCorrelationExpression) {
		return confirmCorrelationExpression(PARSER.parseExpression(confirmCorrelationExpression));
	}

	public S confirmCorrelationFunction(Function<Message<?>, Object> confirmCorrelationFunction) {
		return confirmCorrelationExpression(new FunctionExpression<Message<?>>(confirmCorrelationFunction));
	}


	public S confirmCorrelationExpression(Expression confirmCorrelationExpression) {
		this.target.setConfirmCorrelationExpression(confirmCorrelationExpression);
		return _this();
	}

	public S mappedRequestHeaders(String... headers) {
		this.headerMapper.setRequestHeaderNames(headers);
		return _this();
	}

	public S mappedReplyHeaders(String... headers) {
		this.headerMapper.setReplyHeaderNames(headers);
		return _this();
	}

}
