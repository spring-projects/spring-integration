/*
 * Copyright 2016-2024 the original author or authors.
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

package org.springframework.integration.amqp.dsl;

import java.util.function.Function;

import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.expression.Expression;
import org.springframework.integration.amqp.outbound.AbstractAmqpOutboundEndpoint;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.support.ErrorMessageStrategy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

/**
 * The base {@link MessageHandlerSpec} for {@link AbstractAmqpOutboundEndpoint}s.
 *
 * @param <S> the target {@link AmqpBaseOutboundEndpointSpec} implementation type.
 * @param <E> the target {@link AbstractAmqpOutboundEndpoint} implementation type.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public abstract class
AmqpBaseOutboundEndpointSpec<S extends AmqpBaseOutboundEndpointSpec<S, E>, E extends AbstractAmqpOutboundEndpoint>
		extends MessageHandlerSpec<S, E> {

	protected final DefaultAmqpHeaderMapper headerMapper = DefaultAmqpHeaderMapper.outboundMapper(); // NOSONAR final

	/**
	 * Set a custom {@link AmqpHeaderMapper} for mapping request and reply headers.
	 * @param headerMapper the {@link AmqpHeaderMapper} to use.
	 * @return the spec
	 */
	public S headerMapper(AmqpHeaderMapper headerMapper) {
		this.target.setHeaderMapper(headerMapper);
		return _this();
	}

	/**
	 * Set the default delivery mode.
	 * @param defaultDeliveryMode the delivery mode.
	 * @return the spec
	 */
	public S defaultDeliveryMode(MessageDeliveryMode defaultDeliveryMode) {
		this.target.setDefaultDeliveryMode(defaultDeliveryMode);
		return _this();
	}

	/**
	 * Configure an AMQP routing key for sending messages.
	 * @param routingKey the routing key to use
	 * @return the spec
	 */
	public S routingKey(String routingKey) {
		this.target.setRoutingKey(routingKey);
		return _this();
	}

	/**
	 * A SpEL expression to evaluate routing key at runtime.
	 * @param routingKeyExpression the expression to use.
	 * @return the spec
	 */
	public S routingKeyExpression(String routingKeyExpression) {
		return routingKeyExpression(PARSER.parseExpression(routingKeyExpression));
	}

	/**
	 * A function to evaluate routing key at runtime.
	 * @param routingKeyFunction the {@link Function} to use.
	 * @return the spec
	 */
	public S routingKeyFunction(Function<Message<?>, String> routingKeyFunction) {
		return routingKeyExpression(new FunctionExpression<>(routingKeyFunction));
	}

	/**
	 * A SpEL expression to evaluate routing key at runtime.
	 * @param routingKeyExpression the expression to use.
	 * @return the spec
	 */
	public S routingKeyExpression(Expression routingKeyExpression) {
		this.target.setRoutingKeyExpression(routingKeyExpression);
		return _this();
	}

	/**
	 * Set the channel to which returned messages are sent.
	 * @param returnChannel the channel.
	 * @return the spec
	 */
	public S returnChannel(MessageChannel returnChannel) {
		this.target.setReturnChannel(returnChannel);
		return _this();
	}

	/**
	 * Set the channel to which acks are send (publisher confirms).
	 * @param ackChannel the channel.
	 * @return the spec
	 */
	public S confirmAckChannel(MessageChannel ackChannel) {
		this.target.setConfirmAckChannel(ackChannel);
		return _this();
	}

	/**
	 * Configure an AMQP exchange name for sending messages.
	 * @param exchangeName the exchange name for sending messages.
	 * @return the spec
	 */
	public S exchangeName(String exchangeName) {
		this.target.setExchangeName(exchangeName);
		return _this();
	}

	/**
	 * Configure a SpEL expression to evaluate an exchange name at runtime.
	 * @param exchangeNameExpression the expression to use.
	 * @return the spec
	 */
	public S exchangeNameExpression(String exchangeNameExpression) {
		return exchangeNameExpression(PARSER.parseExpression(exchangeNameExpression));
	}

	/**
	 * Configure a {@link Function} to evaluate an exchange name at runtime.
	 * @param exchangeNameFunction the function to use.
	 * @return the spec
	 */
	public S exchangeNameFunction(Function<Message<?>, String> exchangeNameFunction) {
		return exchangeNameExpression(new FunctionExpression<>(exchangeNameFunction));
	}

	/**
	 * Configure a SpEL expression to evaluate an exchange name at runtime.
	 * @param exchangeNameExpression the expression to use.
	 * @return the spec
	 */
	public S exchangeNameExpression(Expression exchangeNameExpression) {
		this.target.setExchangeNameExpression(exchangeNameExpression);
		return _this();
	}

	/**
	 * Set the channel to which nacks are send (publisher confirms).
	 * @param nackChannel the channel.
	 * @return the spec
	 */
	public S confirmNackChannel(MessageChannel nackChannel) {
		this.target.setConfirmNackChannel(nackChannel);
		return _this();
	}

	/**
	 * Set a SpEL expression to evaluate confirm correlation at runtime.
	 * @param confirmCorrelationExpression the expression to use.
	 * @return the spec
	 */
	public S confirmCorrelationExpression(String confirmCorrelationExpression) {
		return confirmCorrelationExpression(PARSER.parseExpression(confirmCorrelationExpression));
	}

	/**
	 * Set a {@link Function} to evaluate confirm correlation at runtime.
	 * @param confirmCorrelationFunction the function to use.
	 * @return the spec
	 */
	public S confirmCorrelationFunction(Function<Message<?>, Object> confirmCorrelationFunction) {
		return confirmCorrelationExpression(new FunctionExpression<>(confirmCorrelationFunction));
	}

	/**
	 * Set a SpEL expression to evaluate confirm correlation at runtime.
	 * @param confirmCorrelationExpression the expression to use.
	 * @return the spec
	 */
	public S confirmCorrelationExpression(Expression confirmCorrelationExpression) {
		this.target.setConfirmCorrelationExpression(confirmCorrelationExpression);
		return _this();
	}

	/**
	 * Provide the header names that should be mapped from a request to a
	 * {@link org.springframework.messaging.MessageHeaders}.
	 * @param headers The request header names.
	 * @return the spec
	 */
	public S mappedRequestHeaders(String... headers) {
		this.headerMapper.setRequestHeaderNames(headers);
		return _this();
	}

	/**
	 * Provide the header names that should be mapped to a response
	 * from a {@link org.springframework.messaging.MessageHeaders}.
	 * @param headers The reply header names.
	 *  @return the spec
	 */
	public S mappedReplyHeaders(String... headers) {
		this.headerMapper.setReplyHeaderNames(headers);
		return _this();
	}

	/**
	 * Determine whether the headers are
	 * mapped before the message is converted, or afterwards.
	 * @param headersLast true to map headers last.
	 * @return the spec.
	 * @see AbstractAmqpOutboundEndpoint#setHeadersMappedLast(boolean)
	 */
	public S headersMappedLast(boolean headersLast) {
		this.target.setHeadersMappedLast(headersLast);
		return _this();
	}

	/**
	 * Set to {@code false} to attempt to connect during endpoint start.
	 * @param lazyConnect the lazyConnect to set.
	 * @return the spec.
	 * @since 5.0.2
	 * @see AbstractAmqpOutboundEndpoint#setLazyConnect(boolean)
	 */
	public S lazyConnect(boolean lazyConnect) {
		this.target.setLazyConnect(lazyConnect);
		return _this();
	}

	/**
	 * Set the value to set in the {@code x-delay} header when using the
	 * RabbitMQ delayed message exchange plugin.
	 * @param delay the delay.
	 * @return the spec.
	 * @since 5.0.2
	 * @see AbstractAmqpOutboundEndpoint#setDelay(int)
	 */
	public S delay(int delay) {
		this.target.setDelay(delay);
		return _this();
	}

	/**
	 * Set the function to calculate the {@code x-delay} header when using the
	 * RabbitMQ delayed message exchange plugin.
	 * @param delayFunction the function to evaluate the value for the {@code x-delay} header.
	 * @return the spec.
	 * @since 5.0.2
	 * @see #delayExpression(Expression)
	 */
	public S delayFunction(Function<Message<?>, Integer> delayFunction) {
		return delayExpression(new FunctionExpression<>(delayFunction));
	}

	/**
	 * Set the SpEL expression to calculate the {@code x-delay} header when using the
	 * RabbitMQ delayed message exchange plugin.
	 * @param delayExpression the expression.
	 * @return the spec.
	 * @since 5.0.2
	 * @see AbstractAmqpOutboundEndpoint#setDelayExpression(Expression)
	 */
	public S delayExpression(Expression delayExpression) {
		this.target.setDelayExpression(delayExpression);
		return _this();
	}

	/**
	 * Set the SpEL expression to calculate the {@code x-delay} header when using the
	 * RabbitMQ delayed message exchange plugin.
	 * @param delayExpression the expression.
	 * @return the spec.
	 * @since 5.0.2
	 * @see AbstractAmqpOutboundEndpoint#setDelayExpressionString(String)
	 */
	public S delayExpression(String delayExpression) {
		this.target.setDelayExpressionString(delayExpression);
		return _this();
	}

	/**
	 * Set the error message strategy to use for returned (or negatively confirmed)
	 * messages.
	 * @param errorMessageStrategy the strategy.
	 * @return the spec.
	 * @since 5.0.2
	 * @see AbstractAmqpOutboundEndpoint#setErrorMessageStrategy(ErrorMessageStrategy)
	 */
	public S errorMessageStrategy(ErrorMessageStrategy errorMessageStrategy) {
		this.target.setErrorMessageStrategy(errorMessageStrategy);
		return _this();
	}

	/**
	 * Set a timeout after which a nack will be synthesized if no publisher confirm has
	 * been received within that time. Missing confirms will be checked every 50% of this
	 * value so the synthesized nack will be sent between 1x and 1.5x this timeout.
	 * @param timeout the approximate timeout.
	 * @return the spec.
	 * @since 5.3
	 */
	public S confirmTimeout(long timeout) {
		this.target.setConfirmTimeout(timeout);
		return _this();
	}

}
