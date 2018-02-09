/*
 * Copyright 2016-2018 the original author or authors.
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
import org.springframework.integration.amqp.support.NackedAmqpMessageException;
import org.springframework.integration.amqp.support.ReturnedAmqpMessageException;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.DefaultErrorMessageStrategy;
import org.springframework.integration.support.ErrorMessageStrategy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3
 *
 */
public abstract class AbstractAmqpOutboundEndpoint extends AbstractReplyProducingMessageHandler
		implements Lifecycle {

	private String exchangeName;

	private String routingKey;

	private Expression exchangeNameExpression;

	private Expression routingKeyExpression;

	private ExpressionEvaluatingMessageProcessor<String> routingKeyGenerator;

	private ExpressionEvaluatingMessageProcessor<String> exchangeNameGenerator;

	private AmqpHeaderMapper headerMapper = DefaultAmqpHeaderMapper.outboundMapper();

	private Expression confirmCorrelationExpression;

	private ExpressionEvaluatingMessageProcessor<Object> correlationDataGenerator;

	private MessageChannel confirmAckChannel;

	private String confirmAckChannelName;

	private MessageChannel confirmNackChannel;

	private String confirmNackChannelName;

	private MessageChannel returnChannel;

	private MessageDeliveryMode defaultDeliveryMode;

	private boolean lazyConnect = true;

	private ConnectionFactory connectionFactory;

	private Expression delayExpression;

	private ExpressionEvaluatingMessageProcessor<Integer> delayGenerator;

	private boolean headersMappedLast;

	private ErrorMessageStrategy errorMessageStrategy = new DefaultErrorMessageStrategy();

	private volatile boolean running;

	/**
	 * Set a custom {@link AmqpHeaderMapper} for mapping request and reply headers.
	 * Defaults to {@link DefaultAmqpHeaderMapper#outboundMapper()}.
	 * @param headerMapper the {@link AmqpHeaderMapper} to use.
	 */
	public void setHeaderMapper(AmqpHeaderMapper headerMapper) {
		Assert.notNull(headerMapper, "headerMapper must not be null");
		this.headerMapper = headerMapper;
	}

	/**
	 * When mapping headers for the outbound message, determine whether the headers are
	 * mapped before the message is converted, or afterwards. This only affects headers
	 * that might be added by the message converter. When false, the converter's headers
	 * win; when true, any headers added by the converter will be overridden (if the
	 * source message has a header that maps to those headers). You might wish to set this
	 * to true, for example, when using a
	 * {@link org.springframework.amqp.support.converter.SimpleMessageConverter} with a
	 * String payload that contains json; the converter will set the content type to
	 * {@code text/plain} which can be overridden to {@code application/json} by setting
	 * the {@link AmqpHeaders#CONTENT_TYPE} message header. Default: false.
	 * @param headersMappedLast true if headers are mapped after conversion.
	 * @since 5.0
	 */
	public void setHeadersMappedLast(boolean headersMappedLast) {
		this.headersMappedLast = headersMappedLast;
	}

	/**
	 * Configure an AMQP exchange name for sending messages.
	 * @param exchangeName the exchange name for sending messages.
	 */
	public void setExchangeName(String exchangeName) {
		Assert.notNull(exchangeName, "exchangeName must not be null");
		this.exchangeName = exchangeName;
	}

	/**
	 * Configure a SpEL expression to evaluate an exchange name at runtime.
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

	/**
	 * Configure an AMQP routing key for sending messages.
	 * @param routingKey the routing key to use
	 */
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
	 * Set a SpEL expression to evaluate confirm correlation at runtime.
	 * @param confirmCorrelationExpression the expression to use.
	 * @since 4.3
	 */
	public void setConfirmCorrelationExpression(Expression confirmCorrelationExpression) {
		this.confirmCorrelationExpression = confirmCorrelationExpression;
	}

	/**
	 * Set a SpEL expression to evaluate confirm correlation at runtime.
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
	 * Set the channel name to which acks are send (publisher confirms).
	 * @param ackChannelName the channel name.
	 * @since 4.3.12
	 */
	public void setConfirmAckChannelName(String ackChannelName) {
		this.confirmAckChannelName = ackChannelName;
	}

	/**
	 * Set the channel to which nacks are send (publisher confirms).
	 * @param nackChannel the channel.
	 */
	public void setConfirmNackChannel(MessageChannel nackChannel) {
		this.confirmNackChannel = nackChannel;
	}

	/**
	 * Set the channel name to which nacks are send (publisher confirms).
	 * @param nackChannelName the channel name.
	 * @since 4.3.12
	 */
	public void setConfirmNackChannelName(String nackChannelName) {
		this.confirmNackChannelName = nackChannelName;
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

	/**
	 * Set the value to set in the {@code x-delay} header when using the
	 * RabbitMQ delayed message exchange plugin. By default, the {@link AmqpHeaders#DELAY}
	 * header (if present) is mapped; setting the delay here overrides that value.
	 * @param delay the delay.
	 * @since 4.3.5
	 */
	public void setDelay(int delay) {
		this.delayExpression = new ValueExpression<>(delay);
	}

	/**
	 * Set the SpEL expression to calculate the {@code x-delay} header when using the
	 * RabbitMQ delayed message exchange plugin. By default, the {@link AmqpHeaders#DELAY}
	 * header (if present) is mapped; setting the expression here overrides that value.
	 * @param delayExpression the expression.
	 * @since 4.3.5
	 */
	public void setDelayExpression(Expression delayExpression) {
		this.delayExpression = delayExpression;
	}

	/**
	 * Set the SpEL expression to calculate the {@code x-delay} header when using the
	 * RabbitMQ delayed message exchange plugin. By default, the {@link AmqpHeaders#DELAY}
	 * header (if present) is mapped; setting the expression here overrides that value.
	 * @param delayExpression the expression.
	 * @since 4.3.5
	 */
	public void setDelayExpressionString(String delayExpression) {
		if (delayExpression == null) {
			this.delayExpression = null;
		}
		else {
			this.delayExpression = EXPRESSION_PARSER.parseExpression(delayExpression);
		}
	}

	/**
	 * Set the error message strategy to use for returned (or negatively confirmed)
	 * messages.
	 * @param errorMessageStrategy the strategy.
	 * @since 4.3.12
	 */
	public void setErrorMessageStrategy(ErrorMessageStrategy errorMessageStrategy) {
		this.errorMessageStrategy = errorMessageStrategy;
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
		if (this.confirmAckChannel == null && this.confirmAckChannelName != null) {
			this.confirmAckChannel = getChannelResolver().resolveDestination(this.confirmAckChannelName);
		}
		return this.confirmAckChannel;
	}

	protected MessageChannel getConfirmNackChannel() {
		if (this.confirmNackChannel == null && this.confirmNackChannelName != null) {
			this.confirmNackChannel = getChannelResolver().resolveDestination(this.confirmNackChannelName);
		}
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

	protected boolean isHeadersMappedLast() {
		return this.headersMappedLast;
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
			Assert.state((this.confirmAckChannel == null || nullChannel != null) && this.confirmAckChannelName == null,
					"A 'confirmCorrelationExpression' is required when specifying a 'confirmAckChannel'");
			nullChannel = extractTypeIfPossible(this.confirmNackChannel, NullChannel.class);
			Assert.state(
					(this.confirmNackChannel == null || nullChannel != null) && this.confirmNackChannelName == null,
					"A 'confirmCorrelationExpression' is required when specifying a 'confirmNackChannel'");
		}
		if (this.delayExpression != null) {
			this.delayGenerator = new ExpressionEvaluatingMessageProcessor<Integer>(this.delayExpression,
					Integer.class);
			if (beanFactory != null) {
				this.delayGenerator.setBeanFactory(beanFactory);
			}
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
					Connection connection = this.connectionFactory.createConnection(); // NOSONAR (close)
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
			correlationData = new CorrelationDataWrapper(requestMessage.getHeaders().getId().toString(),
					this.correlationDataGenerator.processMessage(requestMessage), requestMessage);
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

	protected void addDelayProperty(Message<?> message, org.springframework.amqp.core.Message amqpMessage) {
		if (this.delayGenerator != null) {
			amqpMessage.getMessageProperties().setDelay(this.delayGenerator.processMessage(message));
		}
	}

	protected AbstractIntegrationMessageBuilder<?> buildReply(MessageConverter converter,
			org.springframework.amqp.core.Message amqpReplyMessage) {
		Object replyObject = converter.fromMessage(amqpReplyMessage);
		AbstractIntegrationMessageBuilder<?> builder = (replyObject instanceof Message)
				? this.getMessageBuilderFactory().fromMessage((Message<?>) replyObject)
				: this.getMessageBuilderFactory().withPayload(replyObject);
		Map<String, ?> headers = getHeaderMapper().toHeadersFromReply(amqpReplyMessage.getMessageProperties());
		builder.copyHeadersIfAbsent(headers);
		return builder;
	}

	protected Message<?> buildReturnedMessage(org.springframework.amqp.core.Message message,
			int replyCode, String replyText, String exchange, String routingKey, MessageConverter converter) {
		Object returnedObject = converter.fromMessage(message);
		AbstractIntegrationMessageBuilder<?> builder = (returnedObject instanceof Message)
				? this.getMessageBuilderFactory().fromMessage((Message<?>) returnedObject)
				: this.getMessageBuilderFactory().withPayload(returnedObject);
		Map<String, ?> headers = getHeaderMapper().toHeadersFromReply(message.getMessageProperties());
		if (this.errorMessageStrategy == null) {
			builder.copyHeadersIfAbsent(headers)
					.setHeader(AmqpHeaders.RETURN_REPLY_CODE, replyCode)
					.setHeader(AmqpHeaders.RETURN_REPLY_TEXT, replyText)
					.setHeader(AmqpHeaders.RETURN_EXCHANGE, exchange)
					.setHeader(AmqpHeaders.RETURN_ROUTING_KEY, routingKey);
		}
		Message<?> returnedMessage = builder.build();
		if (this.errorMessageStrategy != null) {
			returnedMessage = this.errorMessageStrategy.buildErrorMessage(new ReturnedAmqpMessageException(
					returnedMessage, message, replyCode, replyText, exchange, routingKey), null);
		}
		return returnedMessage;
	}

	protected void handleConfirm(CorrelationData correlationData, boolean ack, String cause) {
		CorrelationDataWrapper wrapper = (CorrelationDataWrapper) correlationData;
		if (correlationData == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("No correlation data provided for ack: " + ack + " cause:" + cause);
			}
			return;
		}
		Object userCorrelationData = wrapper.getUserData();
		Message<?> confirmMessage;
		if (this.errorMessageStrategy == null || ack) {
			Map<String, Object> headers = new HashMap<String, Object>();
			headers.put(AmqpHeaders.PUBLISH_CONFIRM, ack);
			if (!ack && StringUtils.hasText(cause)) {
				headers.put(AmqpHeaders.PUBLISH_CONFIRM_NACK_CAUSE, cause);
			}

			AbstractIntegrationMessageBuilder<?> builder = userCorrelationData instanceof Message
					? this.getMessageBuilderFactory().fromMessage((Message<?>) userCorrelationData)
					: this.getMessageBuilderFactory().withPayload(userCorrelationData);

			confirmMessage = builder
					.copyHeaders(headers)
					.build();
		}
		else {
			confirmMessage = this.errorMessageStrategy.buildErrorMessage(
					new NackedAmqpMessageException(wrapper.getMessage(), wrapper.getUserData(), cause), null);
		}
		if (ack && getConfirmAckChannel() != null) {
			sendOutput(confirmMessage, getConfirmAckChannel(), true);
		}
		else if (!ack && getConfirmNackChannel() != null) {
			sendOutput(confirmMessage, getConfirmNackChannel(), true);
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

		private final Message<?> message;

		CorrelationDataWrapper(String id, Object userData, Message<?> message) {
			super(id);
			this.userData = userData;
			this.message = message;
		}

		public Object getUserData() {
			return this.userData;
		}

		public Message<?> getMessage() {
			return this.message;
		}

	}

}
