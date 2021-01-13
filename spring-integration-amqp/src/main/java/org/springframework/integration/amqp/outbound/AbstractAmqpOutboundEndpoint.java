/*
 * Copyright 2016-2021 the original author or authors.
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

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;

import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.expression.Expression;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.amqp.support.NackedAmqpMessageException;
import org.springframework.integration.amqp.support.ReturnedAmqpMessageException;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.integration.mapping.AbstractHeaderMapper;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.DefaultErrorMessageStrategy;
import org.springframework.integration.support.ErrorMessageStrategy;
import org.springframework.integration.support.management.ManageableLifecycle;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.concurrent.SettableListenableFuture;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3
 *
 */
public abstract class AbstractAmqpOutboundEndpoint extends AbstractReplyProducingMessageHandler
		implements ManageableLifecycle {

	private static final String NO_ID = new UUID(0L, 0L).toString();

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

	private Duration confirmTimeout;

	private volatile boolean running;

	private volatile ScheduledFuture<?> confirmChecker;

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
	public void setDelayExpressionString(@Nullable String delayExpression) {
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

	/**
	 * Set a timeout after which a nack will be synthesized if no publisher confirm has
	 * been received within that time. Missing confirms will be checked every 50% of this
	 * value so the synthesized nack will be sent between 1x and 1.5x this timeout.
	 * @param confirmTimeout the approximate timeout.
	 * @since 5.2
	 * @see #setConfirmNackChannel(MessageChannel)
	 */
	public void setConfirmTimeout(long confirmTimeout) {
		this.confirmTimeout = Duration.ofMillis(confirmTimeout); // NOSONAR sync inconsistency
	}

	protected final synchronized void setConnectionFactory(ConnectionFactory connectionFactory) {
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

	public AmqpHeaderMapper getHeaderMapper() {
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

	@Nullable
	protected Duration getConfirmTimeout() {
		return this.confirmTimeout;
	}

	@Override
	protected final void doInit() {
		BeanFactory beanFactory = getBeanFactory();
		configureExchangeNameGenerator(beanFactory);
		configureRoutingKeyGenerator(beanFactory);
		configureCorrelationDataGenerator(beanFactory);
		configureDelayGenerator(beanFactory);

		endpointInit();

		if (this.headerMapper instanceof AbstractHeaderMapper) {
			((AbstractHeaderMapper<?>) this.headerMapper).setBeanClassLoader(getBeanClassLoader());
		}
	}

	private void configureExchangeNameGenerator(BeanFactory beanFactory) {
		Assert.state(this.exchangeNameExpression == null || this.exchangeName == null,
				"Either an exchangeName or an exchangeNameExpression can be provided, but not both");
		if (this.exchangeNameExpression != null) {
			this.exchangeNameGenerator =
					new ExpressionEvaluatingMessageProcessor<>(this.exchangeNameExpression, String.class);
			if (beanFactory != null) {
				this.exchangeNameGenerator.setBeanFactory(beanFactory);
			}
		}
	}

	private void configureRoutingKeyGenerator(BeanFactory beanFactory) {
		Assert.state(this.routingKeyExpression == null || this.routingKey == null,
				"Either a routingKey or a routingKeyExpression can be provided, but not both");
		if (this.routingKeyExpression != null) {
			this.routingKeyGenerator =
					new ExpressionEvaluatingMessageProcessor<>(this.routingKeyExpression, String.class);
			if (beanFactory != null) {
				this.routingKeyGenerator.setBeanFactory(beanFactory);
			}
		}
	}

	private void configureCorrelationDataGenerator(BeanFactory beanFactory) {
		if (this.confirmCorrelationExpression != null) {
			this.correlationDataGenerator =
					new ExpressionEvaluatingMessageProcessor<>(this.confirmCorrelationExpression, Object.class);
			if (beanFactory != null) {
				this.correlationDataGenerator.setBeanFactory(beanFactory);
			}
		}
		else {
			NullChannel nullChannel = extractTypeIfPossible(this.confirmAckChannel, NullChannel.class);
			Assert.state(
					(this.confirmAckChannel == null || nullChannel != null) && this.confirmAckChannelName == null,
					"A 'confirmCorrelationExpression' is required when specifying a 'confirmAckChannel'");
			nullChannel = extractTypeIfPossible(this.confirmNackChannel, NullChannel.class);
			Assert.state(
					(this.confirmNackChannel == null || nullChannel != null) && this.confirmNackChannelName == null,
					"A 'confirmCorrelationExpression' is required when specifying a 'confirmNackChannel'");
		}
	}

	private void configureDelayGenerator(BeanFactory beanFactory) {
		if (this.delayExpression != null) {
			this.delayGenerator = new ExpressionEvaluatingMessageProcessor<>(this.delayExpression, Integer.class);
			if (beanFactory != null) {
				this.delayGenerator.setBeanFactory(beanFactory);
			}
		}
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
				catch (RuntimeException ex) {
					logger.error(ex, "Failed to eagerly establish the connection.");
				}
			}
			doStart();
			if (this.confirmTimeout != null && getConfirmNackChannel() != null && getRabbitTemplate() != null) {
				this.confirmChecker = getTaskScheduler()
						.scheduleAtFixedRate(checkUnconfirmed(), this.confirmTimeout.dividedBy(2L));
			}
			this.running = true;
		}
	}

	private Runnable checkUnconfirmed() {
		return () -> {
			RabbitTemplate rabbitTemplate = getRabbitTemplate();
			if (rabbitTemplate != null) {
				Collection<CorrelationData> unconfirmed =
						rabbitTemplate.getUnconfirmed(getConfirmTimeout().toMillis());
				if (unconfirmed != null) {
					unconfirmed.forEach(correlation -> handleConfirm(correlation, false, "Confirm timed out"));
				}
			}
		};
	}

	@Nullable
	protected abstract RabbitTemplate getRabbitTemplate();

	@Override
	public synchronized void stop() {
		if (this.running) {
			doStop();
		}
		this.running = false;
		if (this.confirmChecker != null) {
			this.confirmChecker.cancel(false);
			this.confirmChecker = null;
		}
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
		UUID uuid = requestMessage.getHeaders().getId();
		String messageId;
		if (uuid == null) {
			messageId = NO_ID;
		}
		else {
			messageId = uuid.toString();
		}
		if (this.correlationDataGenerator != null) {
			Object userData = this.correlationDataGenerator.processMessage(requestMessage);
			if (userData != null) {
				correlationData = new CorrelationDataWrapper(messageId, userData, requestMessage);
			}
			else {
				this.logger.debug("'confirmCorrelationExpression' resolved to 'null'; "
						+ "no publisher confirm will be sent to the ack or nack channel");
			}
		}
		if (correlationData == null) {
			Object correlation = requestMessage.getHeaders().get(AmqpHeaders.PUBLISH_CONFIRM_CORRELATION);
			if (correlation instanceof CorrelationData) {
				correlationData = (CorrelationData) correlation;
			}
			if (correlationData != null) {
				correlationData = new CorrelationDataWrapper(messageId, correlationData, requestMessage);
			}
		}
		return correlationData;
	}

	protected String generateExchangeName(Message<?> requestMessage) {
		String exchange = this.exchangeName;
		if (this.exchangeNameGenerator != null) {
			exchange = this.exchangeNameGenerator.processMessage(requestMessage);
		}
		return exchange;
	}

	protected String generateRoutingKey(Message<?> requestMessage) {
		String key = this.routingKey;
		if (this.routingKeyGenerator != null) {
			key = this.routingKeyGenerator.processMessage(requestMessage);
		}
		return key;
	}

	protected void addDelayProperty(Message<?> message, org.springframework.amqp.core.Message amqpMessage) {
		if (this.delayGenerator != null) {
			amqpMessage.getMessageProperties().setDelay(this.delayGenerator.processMessage(message));
		}
	}

	protected AbstractIntegrationMessageBuilder<?> buildReply(MessageConverter converter,
			org.springframework.amqp.core.Message amqpReplyMessage) {

		Object replyObject = converter.fromMessage(amqpReplyMessage);
		AbstractIntegrationMessageBuilder<?> builder = prepareMessageBuilder(replyObject);
		Map<String, ?> headers = getHeaderMapper().toHeadersFromReply(amqpReplyMessage.getMessageProperties());
		builder.copyHeadersIfAbsent(headers);
		return builder;
	}

	private AbstractIntegrationMessageBuilder<?> prepareMessageBuilder(Object replyObject) {
		return replyObject instanceof Message
				? getMessageBuilderFactory().fromMessage((Message<?>) replyObject)
				: getMessageBuilderFactory().withPayload(replyObject);
	}

	/**
	 * Build Spring message object based on the provided returned AMQP message info.
	 * @param message the returned AMQP message
	 * @param replyCode the returned message reason code
	 * @param replyText the returned message reason text
	 * @param exchange the exchange the message returned from
	 * @param returnedRoutingKey the routing key for returned message
	 * @param converter the converter to deserialize body of the returned AMQP message
	 * @return the Spring message which represents a returned AMQP message
	 * @deprecated since 5.4 in favor of {@link #buildReturnedMessage(ReturnedMessage, MessageConverter)}
	 */
	@Deprecated
	protected Message<?> buildReturnedMessage(org.springframework.amqp.core.Message message,
			int replyCode, String replyText, String exchange, String returnedRoutingKey, MessageConverter converter) {

		return buildReturnedMessage(new ReturnedMessage(message, replyCode, replyText, exchange, returnedRoutingKey),
				converter);
	}

	protected Message<?> buildReturnedMessage(ReturnedMessage returnedMessage, MessageConverter converter) {
		org.springframework.amqp.core.Message amqpMessage = returnedMessage.getMessage();
		Object returnedObject = converter.fromMessage(amqpMessage);
		AbstractIntegrationMessageBuilder<?> builder = prepareMessageBuilder(returnedObject);
		Map<String, ?> headers = getHeaderMapper().toHeadersFromReply(amqpMessage.getMessageProperties());
		if (this.errorMessageStrategy == null) {
			builder.copyHeadersIfAbsent(headers)
					.setHeader(AmqpHeaders.RETURN_REPLY_CODE, returnedMessage.getReplyCode())
					.setHeader(AmqpHeaders.RETURN_REPLY_TEXT, returnedMessage.getReplyText())
					.setHeader(AmqpHeaders.RETURN_EXCHANGE, returnedMessage.getExchange())
					.setHeader(AmqpHeaders.RETURN_ROUTING_KEY, returnedMessage.getRoutingKey());
		}
		Message<?> message = builder.build();
		if (this.errorMessageStrategy != null) {
			message = this.errorMessageStrategy.buildErrorMessage(new ReturnedAmqpMessageException(
					message, amqpMessage, returnedMessage.getReplyCode(), returnedMessage.getReplyText(),
					returnedMessage.getExchange(), returnedMessage.getRoutingKey()), null);
		}
		return message;
	}

	protected void handleConfirm(CorrelationData correlationData, boolean ack, String cause) {
		CorrelationDataWrapper wrapper = (CorrelationDataWrapper) correlationData;
		if (correlationData == null) {
			logger.debug(() -> "No correlation data provided for ack: " + ack + " cause:" + cause);
			return;
		}
		Object userCorrelationData = wrapper.getUserData();
		MessageChannel ackChannel = getConfirmAckChannel();
		if (ack && ackChannel != null) {
			sendOutput(buildConfirmMessage(ack, cause, wrapper, userCorrelationData), ackChannel, true);
		}
		else {
			MessageChannel nackChannel = getConfirmNackChannel();
			if (!ack && nackChannel != null) {
				sendOutput(buildConfirmMessage(ack, cause, wrapper, userCorrelationData), nackChannel, true);
			}
			else {
				logger.debug(() -> "Nowhere to send publisher confirm " + (ack ? "ack" : "nack") + " for "
						+ userCorrelationData);
			}
		}
	}

	private Message<?> buildConfirmMessage(boolean ack, String cause, CorrelationDataWrapper wrapper,
			Object userCorrelationData) {

		if (this.errorMessageStrategy == null || ack) {
			Map<String, Object> headers = new HashMap<>();
			headers.put(AmqpHeaders.PUBLISH_CONFIRM, ack);
			if (!ack && StringUtils.hasText(cause)) {
				headers.put(AmqpHeaders.PUBLISH_CONFIRM_NACK_CAUSE, cause);
			}

			return prepareMessageBuilder(userCorrelationData)
					.copyHeaders(headers)
					.build();
		}
		else {
			return this.errorMessageStrategy.buildErrorMessage(new NackedAmqpMessageException(wrapper.getMessage(),
					wrapper.getUserData(), cause), null);
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

		@Override
		public SettableListenableFuture<Confirm> getFuture() {
			if (this.userData instanceof CorrelationData) {
				return ((CorrelationData) this.userData).getFuture();
			}
			else {
				return super.getFuture();
			}
		}

		@Override
		@Deprecated
		public void setReturnedMessage(org.springframework.amqp.core.Message returnedMessage) {
			if (this.userData instanceof CorrelationData) {
				((CorrelationData) this.userData).setReturnedMessage(returnedMessage);
			}
			super.setReturnedMessage(returnedMessage);
		}

		@Override
		public void setReturned(ReturnedMessage returned) {
			if (this.userData instanceof CorrelationData) {
				((CorrelationData) this.userData).setReturned(returned);
			}
			super.setReturned(returned);
		}

	}

}
