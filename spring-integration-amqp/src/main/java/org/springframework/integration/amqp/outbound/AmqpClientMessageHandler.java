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

package org.springframework.integration.amqp.outbound;

import java.util.concurrent.CompletableFuture;

import org.jspecify.annotations.Nullable;

import org.springframework.amqp.core.AsyncAmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.amqp.support.converter.SmartMessageConverter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.mapping.AbstractHeaderMapper;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * An {@link AbstractReplyProducingMessageHandler} implementation for AMQP 1.0 client.
 * <p>
 * With the {@link #setRequiresReply(boolean)} configured as {@code true}, this message handler
 * behaves as a gateway - the RPC over AMQP.
 * In this case, when {@link #replyPayloadTypeExpression} is provided,
 * the {@link #messageConverter} must be an instance of {@link SmartMessageConverter}.
 * <p>
 * This handler is {@code async} by default.
 * <p>
 * In async mode, the error is sent to the error channel even if not in a gateway mode.
 * <p>
 * The {@link #exchangeExpression}, {@link #routingKeyExpression} and {@link #queueExpression}
 * are optional.
 * In this case they have to be supplied by the {@link AsyncAmqpTemplate}.
 *
 * @author Artem Bilan
 *
 * @since 7.0
 */
public class AmqpClientMessageHandler extends AbstractReplyProducingMessageHandler {

	private final AsyncAmqpTemplate amqpTemplate;

	private AmqpHeaderMapper headerMapper = DefaultAmqpHeaderMapper.outboundMapper();

	private MessageConverter messageConverter = new SimpleMessageConverter();

	private @Nullable Expression exchangeExpression;

	private @Nullable Expression routingKeyExpression;

	private @Nullable Expression queueExpression;

	private @Nullable Expression replyPayloadTypeExpression;

	private boolean returnMessage;

	@SuppressWarnings("NullAway.Init")
	private StandardEvaluationContext evaluationContext;

	/**
	 * Construct an instance with the provided {@link AsyncAmqpTemplate}.
	 * The {@link AsyncAmqpTemplate} must be an implementation for AMQP 1.0 protocol,
	 * e.g. {@link org.springframework.amqp.rabbitmq.client.RabbitAmqpTemplate}.
	 * @param amqpTemplate the {@link AsyncAmqpTemplate} to use.
	 */
	@SuppressWarnings("this-escape")
	public AmqpClientMessageHandler(AsyncAmqpTemplate amqpTemplate) {
		this.amqpTemplate = amqpTemplate;
		setAsync(true);
	}

	public void setHeaderMapper(AmqpHeaderMapper headerMapper) {
		this.headerMapper = headerMapper;
	}

	public void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	public void setExchange(String exchange) {
		setExchangeExpression(new ValueExpression<>(exchange));
	}

	public void setExchangeExpressionString(String exchangeExpression) {
		setExchangeExpression(EXPRESSION_PARSER.parseExpression(exchangeExpression));
	}

	public void setExchangeExpression(Expression exchangeExpression) {
		this.exchangeExpression = exchangeExpression;
	}

	public void setRoutingKey(String routingKey) {
		setRoutingKeyExpression(new ValueExpression<>(routingKey));
	}

	public void setRoutingKeyExpressionString(String routingKeyExpression) {
		setRoutingKeyExpression(EXPRESSION_PARSER.parseExpression(routingKeyExpression));
	}

	public void setRoutingKeyExpression(Expression routingKeyExpression) {
		this.routingKeyExpression = routingKeyExpression;
	}

	public void setQueue(String queue) {
		setQueueExpression(new ValueExpression<>(queue));
	}

	public void setQueueExpressionString(String queueExpression) {
		setQueueExpression(EXPRESSION_PARSER.parseExpression(queueExpression));
	}

	public void setQueueExpression(Expression queueExpression) {
		this.queueExpression = queueExpression;
	}

	/**
	 * Set the reply payload type.
	 * Used only if {@link #setRequiresReply(boolean)} is {@code true}.
	 * @param replyPayloadType the reply payload type.
	 */
	public void setReplyPayloadType(Class<?> replyPayloadType) {
		setReplyPayloadType(ResolvableType.forClass(replyPayloadType));
	}

	/**
	 * Set the reply payload type.
	 * Used only if {@link #setRequiresReply(boolean)} is {@code true}.
	 * @param replyPayloadType the reply payload type.
	 */
	public void setReplyPayloadType(ResolvableType replyPayloadType) {
		setReplyPayloadTypeExpression(new ValueExpression<>(replyPayloadType));
	}

	/**
	 * Set a SpEL expression for the reply payload type.
	 * Used only if {@link #setRequiresReply(boolean)} is {@code true}.
	 * Must be evaluated to a {@link Class} or {@link ResolvableType}.
	 * @param replyPayloadTypeExpression the expression for a reply payload type.
	 */
	public void setReplyPayloadTypeExpressionString(String replyPayloadTypeExpression) {
		setReplyPayloadTypeExpression(EXPRESSION_PARSER.parseExpression(replyPayloadTypeExpression));
	}

	/**
	 * Set a SpEL expression for the reply payload type.
	 * Used only if {@link #setRequiresReply(boolean)} is {@code true}.
	 * Must be evaluated to a {@link Class} or {@link ResolvableType}.
	 * @param replyPayloadTypeExpression the expression for a reply payload type.
	 */
	public void setReplyPayloadTypeExpression(Expression replyPayloadTypeExpression) {
		this.replyPayloadTypeExpression = replyPayloadTypeExpression;
	}

	/**
	 * Set to true to return the reply as a whole AMQP message.
	 * Used only in the gateway mode.
	 * @param returnMessage true to return the reply as a whole AMQP message.
	 */
	public void setReturnMessage(boolean returnMessage) {
		this.returnMessage = returnMessage;
	}

	@Override
	public String getComponentType() {
		return getRequiresReply() ? "amqp:outbound-gateway" : "amqp:outbound-channel-adapter";
	}

	@Override
	protected void doInit() {
		super.doInit();
		if (this.headerMapper instanceof AbstractHeaderMapper<?> abstractHeaderMapper) {
			abstractHeaderMapper.setBeanClassLoader(getBeanClassLoader());
		}
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());

		Assert.state(this.exchangeExpression == null || this.queueExpression == null,
				"The 'exchange' (and optional 'routingKey') is mutually exclusive with 'queue'");

		Assert.state(this.replyPayloadTypeExpression == null || this.messageConverter instanceof SmartMessageConverter,
				"The 'messageConverter' must be a 'SmartMessageConverter' when 'replyPayloadTypeExpression' is provided");

		Assert.state(this.replyPayloadTypeExpression == null || !this.returnMessage,
				"The 'returnMessage == true' and 'replyPayloadTypeExpression' are mutually exclusive");
	}

	@Override
	protected @Nullable Object handleRequestMessage(org.springframework.messaging.Message<?> requestMessage) {
		MessageProperties messageProperties = new MessageProperties();
		this.headerMapper.fromHeadersToRequest(requestMessage.getHeaders(), messageProperties);
		Message amqpMessage = this.messageConverter.toMessage(requestMessage.getPayload(), messageProperties);

		String queue = null;
		if (this.queueExpression != null) {
			queue = this.queueExpression.getValue(this.evaluationContext, requestMessage, String.class);
		}

		String exchange = null;
		if (this.exchangeExpression != null) {
			exchange = this.exchangeExpression.getValue(this.evaluationContext, requestMessage, String.class);
		}

		String routingKey = null;
		if (this.routingKeyExpression != null) {
			routingKey = this.routingKeyExpression.getValue(this.evaluationContext, requestMessage, String.class);
		}

		if (getRequiresReply()) {
			return doSendAndReceive(requestMessage, amqpMessage, queue, exchange, routingKey);
		}
		else {
			doSend(requestMessage, amqpMessage, queue, exchange, routingKey);
			return null;
		}
	}

	private void doSend(org.springframework.messaging.Message<?> requestMessage, Message amqpMessage,
			@Nullable String queue, @Nullable String exchange, @Nullable String routingKey) {

		CompletableFuture<Boolean> sendResultFuture;

		if (StringUtils.hasText(queue)) {
			sendResultFuture = this.amqpTemplate.send(queue, amqpMessage);
		}
		else if (StringUtils.hasText(exchange)) {
			sendResultFuture = this.amqpTemplate.send(exchange, routingKey, amqpMessage);
		}
		else {
			sendResultFuture = this.amqpTemplate.send(amqpMessage);
		}

		if (isAsync()) {
			sendResultFuture.whenComplete((aBoolean, throwable) -> {
				if (throwable != null) {
					sendErrorMessage(requestMessage, throwable);
				}
			});
		}
		else {
			sendResultFuture.join();
		}
	}

	private Object doSendAndReceive(org.springframework.messaging.Message<?> requestMessage, Message amqpMessage,
			@Nullable String queue, @Nullable String exchange, @Nullable String routingKey) {

		ParameterizedTypeReference<?> replyType = null;
		if (this.replyPayloadTypeExpression != null) {
			Object type = this.replyPayloadTypeExpression.getValue(this.evaluationContext, requestMessage);

			Assert.state(type instanceof Class<?> || type instanceof ResolvableType,
					"The 'replyPayloadTypeExpression' must evaluate to 'Class' or 'ResolvableType'");

			ResolvableType replyResolvableType =
					type instanceof Class<?> aClass
							? ResolvableType.forClass(aClass)
							: (ResolvableType) type;

			replyType = ParameterizedTypeReference.forType(replyResolvableType.getType());
		}

		CompletableFuture<?> replyFuture;

		if (StringUtils.hasText(queue)) {
			replyFuture = this.amqpTemplate.sendAndReceive(queue, amqpMessage);
		}
		else if (StringUtils.hasText(exchange)) {
			replyFuture = this.amqpTemplate.sendAndReceive(exchange, routingKey, amqpMessage);
		}
		else {
			replyFuture = this.amqpTemplate.sendAndReceive(amqpMessage);
		}

		if (!this.returnMessage) {
			ParameterizedTypeReference<?> replyTypeToUse = replyType;
			replyFuture = replyFuture.thenApply((reply) -> buildReplyMessage((Message) reply, replyTypeToUse));
		}

		return isAsync() ? replyFuture : replyFuture.join();
	}

	private AbstractIntegrationMessageBuilder<?> buildReplyMessage(Message message,
			@Nullable ParameterizedTypeReference<?> replyType) {

		Object replyPayload =
				replyType != null
						? ((SmartMessageConverter) this.messageConverter).fromMessage(message, replyType)
						: this.messageConverter.fromMessage(message);

		return getMessageBuilderFactory().withPayload(replyPayload)
				.copyHeaders(this.headerMapper.toHeadersFromReply(message.getMessageProperties()));
	}

}
