/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.amqp.outbound;

import java.util.Map;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate.ReturnCallback;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.amqp.AmqpHeaders;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * Adapter that converts and sends Messages to an AMQP Exchange.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.1
 */
public class AmqpOutboundEndpoint extends AbstractReplyProducingMessageHandler
	implements RabbitTemplate.ConfirmCallback, ReturnCallback {

	private static final ExpressionParser expressionParser = new SpelExpressionParser(new SpelParserConfiguration(true, true));


	private final AmqpTemplate amqpTemplate;

	private volatile boolean expectReply;

	private volatile String exchangeName;

	private volatile String routingKey;

	private volatile String exchangeNameExpression;

	private volatile String routingKeyExpression;

	private volatile ExpressionEvaluatingMessageProcessor<String> routingKeyGenerator;

	private volatile ExpressionEvaluatingMessageProcessor<String> exchangeNameGenerator;

	private volatile AmqpHeaderMapper headerMapper = new DefaultAmqpHeaderMapper();

	private volatile String confirmCorrelationExpression;

	private volatile ExpressionEvaluatingMessageProcessor<Object> correlationDataGenerator;

	private volatile MessageChannel confirmAckChannel;

	private volatile MessageChannel confirmNackChannel;

	private volatile MessageChannel returnChannel;

	@Override
	protected void onInit() {
		super.onInit();
		Assert.state(exchangeNameExpression == null || exchangeName == null,
				"Either an exchangeName or an exchangeNameExpression can be provided, but not both");
		Assert.state(this.confirmCorrelationExpression == null || !this.expectReply,
				"Confirm correlation expression does not apply to a gateway");
		if (exchangeNameExpression != null) {
			Expression expression = expressionParser.parseExpression(this.exchangeNameExpression);
			this.exchangeNameGenerator = new ExpressionEvaluatingMessageProcessor<String>(expression, String.class);
		}
		Assert.state(routingKeyExpression == null || routingKey == null,
				"Either a routingKey or a routingKeyExpression can be provided, but not both");
		if (routingKeyExpression != null) {
			Expression expression = expressionParser.parseExpression(this.routingKeyExpression);
			this.routingKeyGenerator = new ExpressionEvaluatingMessageProcessor<String>(expression, String.class);
		}
		if (this.confirmCorrelationExpression != null) {
			Expression expression = expressionParser.parseExpression(this.confirmCorrelationExpression);
			this.correlationDataGenerator = new ExpressionEvaluatingMessageProcessor<Object>(expression, Object.class);
			Assert.isTrue(amqpTemplate instanceof RabbitTemplate, "RabbitTemplate implementation is required for publisher confirms");
			((RabbitTemplate) this.amqpTemplate).setConfirmCallback(this);
		}
		if (this.returnChannel != null) {
			Assert.isTrue(amqpTemplate instanceof RabbitTemplate, "RabbitTemplate implementation is required for publisher returns");
			(		(RabbitTemplate) this.amqpTemplate).setReturnCallback(this);
		}
	}

	public AmqpOutboundEndpoint(AmqpTemplate amqpTemplate) {
		Assert.notNull(amqpTemplate, "amqpTemplate must not be null");
		this.amqpTemplate = amqpTemplate;
	}

	public void setHeaderMapper(AmqpHeaderMapper headerMapper) {
		Assert.notNull(headerMapper, "headerMapper must not be null");
		this.headerMapper = headerMapper;
	}

	public void setExchangeName(String exchangeName) {
		Assert.notNull(exchangeName, "exchangeName must not be null");
		this.exchangeName = exchangeName;
	}

	public void setExchangeNameExpression(String exchangeNameExpression) {
		this.exchangeNameExpression = exchangeNameExpression;
	}

	public void setRoutingKey(String routingKey) {
		Assert.notNull(routingKey, "routingKey must not be null");
		this.routingKey = routingKey;
	}

	public void setRoutingKeyExpression(String routingKeyExpression) {
		this.routingKeyExpression = routingKeyExpression;
	}

	public void setExpectReply(boolean expectReply) {
		this.expectReply = expectReply;
	}

	public void setConfirmCorrelationExpression(String confirmCorrelationExpression) {
		this.confirmCorrelationExpression = confirmCorrelationExpression;
	}

	public void setConfirmAckChannel(MessageChannel ackChannel) {
		this.confirmAckChannel = ackChannel;
	}

	public void setConfirmNackChannel(MessageChannel nackChannel) {
		this.confirmNackChannel = nackChannel;
	}

	public void setReturnChannel(MessageChannel returnChannel) {
		this.returnChannel = returnChannel;
	}

	@Override
	public String getComponentType() {
		return expectReply ? "amqp:outbound-gateway" : "amqp:outbound-channel-adapter";
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		String exchangeName = this.exchangeName;
		String routingKey = this.routingKey;
		CorrelationData correlationData = null;
		if (this.correlationDataGenerator != null) {
			Object userCorrelationData = this.correlationDataGenerator
					.processMessage(requestMessage);
			if (userCorrelationData != null) {
				if (userCorrelationData instanceof CorrelationData) {
					correlationData = (CorrelationData) userCorrelationData;
				} else {
					correlationData = new CorrelationDataWrapper(requestMessage
							.getHeaders().getId().toString(), userCorrelationData);
				}
			}
		}
		if (this.exchangeNameGenerator != null) {
			exchangeName = this.exchangeNameGenerator.processMessage(requestMessage);
		}
		if (this.routingKeyGenerator != null) {
			routingKey = this.routingKeyGenerator.processMessage(requestMessage);
		}
		if (this.expectReply) {
			return this.sendAndReceive(exchangeName, routingKey, requestMessage);
		}
		else {
			this.send(exchangeName, routingKey, requestMessage, correlationData);
			return null;
		}
	}

	private void send(String exchangeName, String routingKey,
			final Message<?> requestMessage, CorrelationData correlationData) {
		if (this.amqpTemplate instanceof RabbitTemplate) {
			((RabbitTemplate) this.amqpTemplate).convertAndSend(exchangeName, routingKey, requestMessage.getPayload(),
					new MessagePostProcessor() {
						public org.springframework.amqp.core.Message postProcessMessage(
								org.springframework.amqp.core.Message message) throws AmqpException {
							headerMapper.fromHeadersToRequest(requestMessage.getHeaders(), message.getMessageProperties());
							return message;
						}
					},
					correlationData);
		}
		else {
			this.amqpTemplate.convertAndSend(exchangeName, routingKey, requestMessage.getPayload(),
					new MessagePostProcessor() {
						public org.springframework.amqp.core.Message postProcessMessage(
								org.springframework.amqp.core.Message message) throws AmqpException {
							headerMapper.fromHeadersToRequest(requestMessage.getHeaders(), message.getMessageProperties());
							return message;
						}
					});
		}
	}

	private Message<?> sendAndReceive(String exchangeName, String routingKey, Message<?> requestMessage) {
		// TODO: add a convertSendAndReceive method that accepts a MessagePostProcessor so we can map headers?
		Assert.isTrue(amqpTemplate instanceof RabbitTemplate, "RabbitTemplate implementation is required for send and receive");
		MessageConverter converter = ((RabbitTemplate) this.amqpTemplate).getMessageConverter();
		MessageProperties amqpMessageProperties = new MessageProperties();
		this.headerMapper.fromHeadersToRequest(requestMessage.getHeaders(), amqpMessageProperties);
		org.springframework.amqp.core.Message amqpMessage = converter.toMessage(requestMessage.getPayload(), amqpMessageProperties);
		org.springframework.amqp.core.Message amqpReplyMessage = this.amqpTemplate.sendAndReceive(exchangeName, routingKey, amqpMessage);
		if (amqpReplyMessage == null) {
			return null;
		}
		Object replyObject = converter.fromMessage(amqpReplyMessage);
		MessageBuilder<?> builder = (replyObject instanceof Message)
				? MessageBuilder.fromMessage((Message<?>) replyObject)
				: MessageBuilder.withPayload(replyObject);
		Map<String, ?> headers = this.headerMapper.toHeadersFromReply(amqpReplyMessage.getMessageProperties());
		builder.copyHeadersIfAbsent(headers);
		return builder.build();
	}

	public void confirm(CorrelationData correlationData, boolean ack) {
		Object userCorrelationData = correlationData;
		if (correlationData instanceof CorrelationDataWrapper) {
			userCorrelationData = ((CorrelationDataWrapper) correlationData).getUserData();
		}
		Message<Object> confirmMessage = MessageBuilder.withPayload(userCorrelationData)
				.setHeader(AmqpHeaders.PUBLISH_CONFIRM, ack)
				.build();
		if (ack && this.confirmAckChannel != null) {
			this.confirmAckChannel.send(confirmMessage);
		}
		else if (!ack && this.confirmNackChannel != null) {
			this.confirmNackChannel.send(confirmMessage);
		}
		else {
			if (logger.isInfoEnabled()) {
				logger.info("Nowhere to send publisher confirm "
						+ (ack ? "ack" : "nack") + " for "
						+ userCorrelationData);
			}
		}
	}

	private static class CorrelationDataWrapper extends CorrelationData {

		private final Object userData;

		public CorrelationDataWrapper(String id, Object userData) {
			super(id);
			this.userData = userData;
		}

		public Object getUserData() {
			return userData;
		}

	}

	public void returnedMessage(org.springframework.amqp.core.Message message, int replyCode, String replyText,
			String exchange, String routingKey) {
		// safe to cast; we asserted we have a RabbitTemplate in onInit()
		MessageConverter converter = ((RabbitTemplate) this.amqpTemplate).getMessageConverter();
		Object returnedObject = converter.fromMessage(message);
		MessageBuilder<?> builder = (returnedObject instanceof Message)
				? MessageBuilder.fromMessage((Message<?>) returnedObject)
				: MessageBuilder.withPayload(returnedObject);
		Map<String, ?> headers = this.headerMapper.toHeadersFromReply(message.getMessageProperties());
		builder.copyHeadersIfAbsent(headers)
			.setHeader(AmqpHeaders.RETURN_REPLY_CODE, replyCode)
			.setHeader(AmqpHeaders.RETURN_REPLY_TEXT, replyText)
			.setHeader(AmqpHeaders.RETURN_EXCHANGE, exchange)
			.setHeader(AmqpHeaders.RETURN_ROUTING_KEY, routingKey);
		this.returnChannel.send(builder.build());
	}
}
