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

package org.springframework.integration.amqp.outbound;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.Lifecycle;
import org.springframework.expression.Expression;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Gary Russell
 * @since 4.3
 *
 */
public abstract class AbstractAmqpOutboundEndpoint extends AbstractReplyProducingMessageHandler
	implements Lifecycle {

	private volatile String exchangeName;

	private volatile String routingKey;

	private volatile Expression exchangeNameExpression;

	private volatile Expression routingKeyExpression;

	private volatile ExpressionEvaluatingMessageProcessor<String> routingKeyGenerator;

	private volatile ExpressionEvaluatingMessageProcessor<String> exchangeNameGenerator;

	private volatile AmqpHeaderMapper headerMapper = DefaultAmqpHeaderMapper.outboundMapper();

	private volatile Expression confirmCorrelationExpression;

	private volatile ExpressionEvaluatingMessageProcessor<Object> correlationDataGenerator;

	private volatile MessageChannel confirmAckChannel;

	private volatile MessageChannel confirmNackChannel;

	private volatile MessageChannel returnChannel;

	private volatile MessageDeliveryMode defaultDeliveryMode;

	private volatile boolean lazyConnect = true;

	private volatile ConnectionFactory connectionFactory;

	private volatile boolean running;

	public void setHeaderMapper(AmqpHeaderMapper headerMapper) {
		Assert.notNull(headerMapper, "headerMapper must not be null");
		this.headerMapper = headerMapper;
	}

	public void setExchangeName(String exchangeName) {
		Assert.notNull(exchangeName, "exchangeName must not be null");
		this.exchangeName = exchangeName;
	}

	/**
	 * @param exchangeNameExpression the expression to use.
	 * @since 4.3
	 */
	public void setExchangeNameExpression(Expression exchangeNameExpression) {
		this.exchangeNameExpression = exchangeNameExpression;
	}

	/**
	 * @param exchangeNameExpression the String in SpEL syntax.
	 * @since 4.3
	 */
	public void setExchangeNameExpressionString(String exchangeNameExpression) {
		Assert.hasText(exchangeNameExpression, "'exchangeNameExpression' must not be empty");
		this.exchangeNameExpression = EXPRESSION_PARSER.parseExpression(exchangeNameExpression);
	}

	public void setRoutingKey(String routingKey) {
		Assert.notNull(routingKey, "routingKey must not be null");
		this.routingKey = routingKey;
	}

	/**
	 * @param routingKeyExpression the expression to use.
	 * @since 4.3
	 */
	public void setRoutingKeyExpression(Expression routingKeyExpression) {
		this.routingKeyExpression = routingKeyExpression;
	}

	/**
	 * @param routingKeyExpression the String in SpEL syntax.
	 * @since 4.3
	 */
	public void setRoutingKeyExpressionString(String routingKeyExpression) {
		Assert.hasText(routingKeyExpression, "'routingKeyExpression' must not be empty");
		this.routingKeyExpression = EXPRESSION_PARSER.parseExpression(routingKeyExpression);
	}

	/**
	 * @param confirmCorrelationExpression the expression to use.
	 * @since 4.3
	 */
	public void setConfirmCorrelationExpression(Expression confirmCorrelationExpression) {
		this.confirmCorrelationExpression = confirmCorrelationExpression;
	}

	/**
	 * @param confirmCorrelationExpression the String in SpEL syntax.
	 * @since 4.3
	 */
	public void setConfirmCorrelationExpressionString(String confirmCorrelationExpression) {
		Assert.hasText(confirmCorrelationExpression, "'confirmCorrelationExpression' must not be empty");
		this.confirmCorrelationExpression = EXPRESSION_PARSER.parseExpression(confirmCorrelationExpression);
	}

	/**
	 * Set the channel to which acks are send (publisher confirms).
	 * @param ackChannel the channel.
	 */
	public void setConfirmAckChannel(MessageChannel ackChannel) {
		this.confirmAckChannel = ackChannel;
	}

	/**
	 * Set the channel to which nacks are send (publisher confirms).
	 * @param nackChannel the channel.
	 */
	public void setConfirmNackChannel(MessageChannel nackChannel) {
		this.confirmNackChannel = nackChannel;
	}

	/**
	 * Set the channel to which returned messages are sent.
	 * @param returnChannel the channel.
	 */
	public void setReturnChannel(MessageChannel returnChannel) {
		this.returnChannel = returnChannel;
	}

	/**
	 * Set the default delivery mode.
	 * @param defaultDeliveryMode the delivery mode.
	 */
	public void setDefaultDeliveryMode(MessageDeliveryMode defaultDeliveryMode) {
		this.defaultDeliveryMode = defaultDeliveryMode;
	}

	/**
	 * Set to {@code false} to attempt to connect during endpoint start;
	 * default {@code true}, meaning the connection will be attempted
	 * to be established on the arrival of the first message.
	 * @param lazyConnect the lazyConnect to set
	 * @since 4.1
	 */
	public void setLazyConnect(boolean lazyConnect) {
		this.lazyConnect = lazyConnect;
	}

	protected final void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	protected String getExchangeName() {
		return this.exchangeName;
	}

	protected String getRoutingKey() {
		return this.routingKey;
	}

	protected Expression getExchangeNameExpression() {
		return this.exchangeNameExpression;
	}

	protected Expression getRoutingKeyExpression() {
		return this.routingKeyExpression;
	}

	protected ExpressionEvaluatingMessageProcessor<String> getRoutingKeyGenerator() {
		return this.routingKeyGenerator;
	}

	protected ExpressionEvaluatingMessageProcessor<String> getExchangeNameGenerator() {
		return this.exchangeNameGenerator;
	}

	protected AmqpHeaderMapper getHeaderMapper() {
		return this.headerMapper;
	}

	protected Expression getConfirmCorrelationExpression() {
		return this.confirmCorrelationExpression;
	}

	protected ExpressionEvaluatingMessageProcessor<Object> getCorrelationDataGenerator() {
		return this.correlationDataGenerator;
	}

	protected MessageChannel getConfirmAckChannel() {
		return this.confirmAckChannel;
	}

	protected MessageChannel getConfirmNackChannel() {
		return this.confirmNackChannel;
	}

	protected MessageChannel getReturnChannel() {
		return this.returnChannel;
	}

	protected MessageDeliveryMode getDefaultDeliveryMode() {
		return this.defaultDeliveryMode;
	}

	protected boolean isLazyConnect() {
		return this.lazyConnect;
	}

	@Override
	protected final void doInit() {
		Assert.state(this.exchangeNameExpression == null || this.exchangeName == null,
				"Either an exchangeName or an exchangeNameExpression can be provided, but not both");
		BeanFactory beanFactory = getBeanFactory();
		if (this.exchangeNameExpression != null) {
			this.exchangeNameGenerator = new ExpressionEvaluatingMessageProcessor<String>(this.exchangeNameExpression,
					String.class);
			if (beanFactory != null) {
				this.exchangeNameGenerator.setBeanFactory(beanFactory);
			}
		}
		Assert.state(this.routingKeyExpression == null || this.routingKey == null,
				"Either a routingKey or a routingKeyExpression can be provided, but not both");
		if (this.routingKeyExpression != null) {
			this.routingKeyGenerator = new ExpressionEvaluatingMessageProcessor<String>(this.routingKeyExpression,
					String.class);
			if (beanFactory != null) {
				this.routingKeyGenerator.setBeanFactory(beanFactory);
			}
		}
		if (this.confirmCorrelationExpression != null) {
			this.correlationDataGenerator =
					new ExpressionEvaluatingMessageProcessor<Object>(this.confirmCorrelationExpression, Object.class);
			if (beanFactory != null) {
				this.correlationDataGenerator.setBeanFactory(beanFactory);
			}
		}
		else {
			NullChannel nullChannel = extractTypeIfPossible(this.confirmAckChannel, NullChannel.class);
			Assert.state(this.confirmAckChannel == null || nullChannel != null,
					"A 'confirmCorrelationExpression' is required when specifying a 'confirmAckChannel'");
			nullChannel = extractTypeIfPossible(this.confirmNackChannel, NullChannel.class);
			Assert.state(this.confirmNackChannel == null || nullChannel != null,
					"A 'confirmCorrelationExpression' is required when specifying a 'confirmNackChannel'");
		}
		endpointInit();
	}

	/**
	 * Subclasses can override to perform any additional initialization.
	 * Called from afterPropertiesSet().
	 */
	protected void endpointInit() {
	}

	@Override
	public synchronized void start() {
		if (!this.running) {
			if (!this.lazyConnect && this.connectionFactory != null) {
				try {
					Connection connection = this.connectionFactory.createConnection();
					if (connection != null) {
						connection.close();
					}
				}
				catch (RuntimeException e) {
					logger.error("Failed to eagerly establish the connection.", e);
				}
			}
			doStart();
			this.running = true;
		}
	}

	@Override
	public synchronized void stop() {
		if (this.running) {
			doStop();
		}
		this.running = false;
	}

	protected void doStart() {
	}

	protected void doStop() {
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	protected CorrelationData generateCorrelationData(Message<?> requestMessage) {
		CorrelationData correlationData = null;
		if (this.correlationDataGenerator != null) {
			Object userCorrelationData = this.correlationDataGenerator.processMessage(requestMessage);
			if (userCorrelationData != null) {
				if (userCorrelationData instanceof CorrelationData) {
					correlationData = (CorrelationData) userCorrelationData;
				}
				else {
					correlationData = new CorrelationDataWrapper(requestMessage
							.getHeaders().getId().toString(), userCorrelationData);
				}
			}
		}
		return correlationData;
	}

	protected String generateExchangeName(Message<?> requestMessage) {
		String exchangeName = this.exchangeName;
		if (this.exchangeNameGenerator != null) {
			exchangeName = this.exchangeNameGenerator.processMessage(requestMessage);
		}
		return exchangeName;
	}

	protected String generateRoutingKey(Message<?> requestMessage) {
		String routingKey = this.routingKey;
		if (this.routingKeyGenerator != null) {
			routingKey = this.routingKeyGenerator.processMessage(requestMessage);
		}
		return routingKey;
	}

	protected Message<?> buildReplyMessage(MessageConverter converter,
			org.springframework.amqp.core.Message amqpReplyMessage) {
		Object replyObject = converter.fromMessage(amqpReplyMessage);
		AbstractIntegrationMessageBuilder<?> builder = (replyObject instanceof Message)
				? this.getMessageBuilderFactory().fromMessage((Message<?>) replyObject)
				: this.getMessageBuilderFactory().withPayload(replyObject);
		Map<String, ?> headers = getHeaderMapper().toHeadersFromReply(amqpReplyMessage.getMessageProperties());
		builder.copyHeadersIfAbsent(headers);
		return builder.build();
	}

	protected Message<?> buildReturnedMessage(org.springframework.amqp.core.Message message,
			int replyCode, String replyText, String exchange, String routingKey, MessageConverter converter) {
		Object returnedObject = converter.fromMessage(message);
		AbstractIntegrationMessageBuilder<?> builder = (returnedObject instanceof Message)
				? this.getMessageBuilderFactory().fromMessage((Message<?>) returnedObject)
				: this.getMessageBuilderFactory().withPayload(returnedObject);
		Map<String, ?> headers = getHeaderMapper().toHeadersFromReply(message.getMessageProperties());
		builder.copyHeadersIfAbsent(headers)
				.setHeader(AmqpHeaders.RETURN_REPLY_CODE, replyCode)
				.setHeader(AmqpHeaders.RETURN_REPLY_TEXT, replyText)
				.setHeader(AmqpHeaders.RETURN_EXCHANGE, exchange)
				.setHeader(AmqpHeaders.RETURN_ROUTING_KEY, routingKey);
		return builder.build();
	}

	protected void handleConfirm(CorrelationData correlationData, boolean ack, String cause) {
		Object userCorrelationData = correlationData;
		if (correlationData == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("No correlation data provided for ack: " + ack + " cause:" + cause);
			}
			return;
		}
		if (correlationData instanceof CorrelationDataWrapper) {
			userCorrelationData = ((CorrelationDataWrapper) correlationData).getUserData();
		}

		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put(AmqpHeaders.PUBLISH_CONFIRM, ack);
		if (!ack && StringUtils.hasText(cause)) {
			headers.put(AmqpHeaders.PUBLISH_CONFIRM_NACK_CAUSE, cause);
		}

		AbstractIntegrationMessageBuilder<?> builder = userCorrelationData instanceof Message
				? this.getMessageBuilderFactory().fromMessage((Message<?>) userCorrelationData)
				: this.getMessageBuilderFactory().withPayload(userCorrelationData);

		Message<?> confirmMessage = builder
				.copyHeaders(headers)
				.build();
		if (ack && this.confirmAckChannel != null) {
			sendOutput(confirmMessage, this.confirmAckChannel, true);
		}
		else if (!ack && this.confirmNackChannel != null) {
			sendOutput(confirmMessage, this.confirmNackChannel, true);
		}
		else {
			if (logger.isInfoEnabled()) {
				logger.info("Nowhere to send publisher confirm "
						+ (ack ? "ack" : "nack") + " for "
						+ userCorrelationData);
			}
		}
	}

	protected static final class CorrelationDataWrapper extends CorrelationData {

		private final Object userData;

		private CorrelationDataWrapper(String id, Object userData) {
			super(id);
			this.userData = userData;
		}

		public Object getUserData() {
			return this.userData;
		}

	}

}
