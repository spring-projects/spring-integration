/*
 * Copyright 2025-present the original author or authors.
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

import org.springframework.amqp.core.AsyncAmqpTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.core.ResolvableType;
import org.springframework.expression.Expression;
import org.springframework.integration.amqp.outbound.AmqpClientMessageHandler;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.messaging.Message;

/**
 * Spec for an {@link AmqpClientMessageHandler}.
 *
 * @author Artem Bilan
 *
 * @since 7.0
 */
public class AmqpClientMessageHandlerSpec
		extends MessageHandlerSpec<AmqpClientMessageHandlerSpec, AmqpClientMessageHandler> {

	public AmqpClientMessageHandlerSpec(AsyncAmqpTemplate amqpTemplate) {
		this.target = new AmqpClientMessageHandler(amqpTemplate);
	}

	public AmqpClientMessageHandlerSpec headerMapper(AmqpHeaderMapper headerMapper) {
		this.target.setHeaderMapper(headerMapper);
		return this;
	}

	public AmqpClientMessageHandlerSpec messageConverter(MessageConverter messageConverter) {
		this.target.setMessageConverter(messageConverter);
		return this;
	}

	public AmqpClientMessageHandlerSpec exchange(String exchange) {
		this.target.setExchange(exchange);
		return this;
	}

	public <P> AmqpClientMessageHandlerSpec exchangeFunction(Function<Message<P>, String> exchangeFunction) {
		return exchangeExpression(new FunctionExpression<>(exchangeFunction));
	}

	public AmqpClientMessageHandlerSpec exchangeExpression(Expression exchangeExpression) {
		this.target.setExchangeExpression(exchangeExpression);
		return this;
	}

	public AmqpClientMessageHandlerSpec exchangeExpression(String exchangeExpression) {
		this.target.setExchangeExpressionString(exchangeExpression);
		return this;
	}

	public AmqpClientMessageHandlerSpec routingKey(String routingKey) {
		this.target.setRoutingKey(routingKey);
		return this;
	}

	public <P> AmqpClientMessageHandlerSpec routingKeyFunction(Function<Message<P>, String> routingKeyFunction) {
		return routingKeyExpression(new FunctionExpression<>(routingKeyFunction));
	}

	public AmqpClientMessageHandlerSpec routingKeyExpression(String routingKeyExpression) {
		this.target.setRoutingKeyExpressionString(routingKeyExpression);
		return this;
	}

	public AmqpClientMessageHandlerSpec routingKeyExpression(Expression routingKeyExpression) {
		this.target.setRoutingKeyExpression(routingKeyExpression);
		return this;
	}

	public AmqpClientMessageHandlerSpec queue(String queue) {
		this.target.setQueue(queue);
		return this;
	}

	public AmqpClientMessageHandlerSpec queueExpression(String queueExpression) {
		this.target.setQueueExpressionString(queueExpression);
		return this;
	}

	public <P> AmqpClientMessageHandlerSpec queueFunction(Function<Message<P>, String> queueFunction) {
		return queueExpression(new FunctionExpression<>(queueFunction));
	}

	public AmqpClientMessageHandlerSpec queueExpression(Expression queueExpression) {
		this.target.setQueueExpression(queueExpression);
		return this;
	}

	/**
	 * Set a reply payload type.
	 * Used only in the gateway mode.
	 * @param replyPayloadType the reply payload type.
	 * @return the spec
	 */
	public AmqpClientMessageHandlerSpec replyPayloadType(Class<?> replyPayloadType) {
		this.target.setReplyPayloadType(replyPayloadType);
		return this;
	}

	/**
	 * Set a reply payload type.
	 * Used only in the gateway mode.
	 * @param replyPayloadType the reply payload type.
	 * @return the spec
	 */
	public AmqpClientMessageHandlerSpec replyPayloadType(ResolvableType replyPayloadType) {
		this.target.setReplyPayloadType(replyPayloadType);
		return this;
	}

	/**
	 * Set a SpEL expression for the reply payload type.
	 * Used only in the gateway mode.
	 * Must be evaluated to a {@link Class} or {@link ResolvableType}.
	 * @param replyPayloadTypeExpression the expression for a reply payload type.
	 * @return the spec
	 */
	public AmqpClientMessageHandlerSpec replyPayloadTypeExpression(String replyPayloadTypeExpression) {
		this.target.setReplyPayloadTypeExpressionString(replyPayloadTypeExpression);
		return this;
	}

	/**
	 * Set a function for the reply payload type.
	 * Used only in the gateway mode.
	 * Must be evaluated to a {@link Class} or {@link ResolvableType}.
	 * @param replyPayloadTypeFunction the expression for a reply payload type.
	 * @return the spec
	 */
	public <P> AmqpClientMessageHandlerSpec replyPayloadTypeFunction(Function<Message<P>, ?> replyPayloadTypeFunction) {
		return replyPayloadTypeExpression(new FunctionExpression<>(replyPayloadTypeFunction));
	}

	/**
	 * Set a SpEL expression for the reply payload type.
	 * Used only in the gateway mode.
	 * Must be evaluated to a {@link Class} or {@link ResolvableType}.
	 * @param replyPayloadTypeExpression the expression for a reply payload type.
	 * @return the spec
	 */
	public AmqpClientMessageHandlerSpec replyPayloadTypeExpression(Expression replyPayloadTypeExpression) {
		this.target.setReplyPayloadTypeExpression(replyPayloadTypeExpression);
		return this;
	}

	/**
	 * Set to true to return the reply as a whole AMQP message.
	 * Used only in the gateway mode.
	 * @param returnMessage true to return the reply as a whole AMQP message.
	 * @return the spec
	 */
	public AmqpClientMessageHandlerSpec returnMessage(boolean returnMessage) {
		this.target.setReturnMessage(returnMessage);
		return this;
	}

}
