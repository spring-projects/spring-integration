/*
 * Copyright 2002-present the original author or authors.
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
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jspecify.annotations.Nullable;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.connection.CorrelationData.Confirm;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate.ConfirmCallback;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.Lifecycle;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.integration.MessageTimeoutException;
import org.springframework.integration.amqp.support.MappingUtils;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Adapter that converts and sends Messages to an AMQP Exchange.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Ngoc Nhan
 *
 * @since 2.1
 */
public class AmqpOutboundEndpoint extends AbstractAmqpOutboundEndpoint
		implements ConfirmCallback, RabbitTemplate.ReturnsCallback {

	private static final Duration DEFAULT_CONFIRM_TIMEOUT = Duration.ofSeconds(5);

	private final AmqpTemplate amqpTemplate;

	private final @Nullable RabbitTemplate rabbitTemplate;

	private boolean expectReply;

	private boolean waitForConfirm;

	private Duration waitForConfirmTimeout = DEFAULT_CONFIRM_TIMEOUT;

	private boolean multiSend;

	@SuppressWarnings("this-escape")
	public AmqpOutboundEndpoint(AmqpTemplate amqpTemplate) {
		Assert.notNull(amqpTemplate, "amqpTemplate must not be null");
		this.amqpTemplate = amqpTemplate;
		if (amqpTemplate instanceof RabbitTemplate castRabbitTemplate) {
			setConnectionFactory(castRabbitTemplate.getConnectionFactory());
			this.rabbitTemplate = castRabbitTemplate;
		}
		else {
			this.rabbitTemplate = null;
		}
	}

	/**
	 * Set to true if this endpoint is a gateway.
	 * @param expectReply true for a gateway.
	 */
	public void setExpectReply(boolean expectReply) {
		this.expectReply = expectReply;
	}

	/**
	 * Set to true if you want to block the calling thread until a publisher confirm has
	 * been received. Requires a template configured for returns. If a confirm is not
	 * received within the confirm timeout or a negative acknowledgment or returned
	 * message is received, an exception will be thrown. Does not apply to the gateway
	 * since it blocks awaiting the reply.
	 * @param waitForConfirm true to block until the confirmation or timeout is received.
	 * @since 5.2
	 * @see #setConfirmTimeout(long)
	 * @see #setMultiSend(boolean)
	 */
	public void setWaitForConfirm(boolean waitForConfirm) {
		this.waitForConfirm = waitForConfirm;
	}

	@Override
	public String getComponentType() {
		return this.expectReply ? "amqp:outbound-gateway" : "amqp:outbound-channel-adapter";
	}

	/**
	 * If true, and the message payload is an {@link Iterable} of {@link Message}, send
	 * the messages in a single invocation of the template (same channel) and optionally
	 * wait for the confirms or die or perform all sends within a transaction (existing or
	 * new).
	 * @param multiSend true to send multiple messages.
	 * @since 5.3
	 * @see #setWaitForConfirm(boolean)
	 */
	public void setMultiSend(boolean multiSend) {
		Assert.isTrue(this.rabbitTemplate != null
						&& (!this.waitForConfirm || this.rabbitTemplate.getConnectionFactory().isSimplePublisherConfirms()),
				() -> "To use multiSend, " + AmqpOutboundEndpoint.this.amqpTemplate
						+ " must be a RabbitTemplate with a ConnectionFactory configured with simple confirms");
		this.multiSend = multiSend;
	}

	@Override
	public IntegrationPatternType getIntegrationPatternType() {
		return this.expectReply ? super.getIntegrationPatternType() : IntegrationPatternType.outbound_channel_adapter;
	}

	@Override
	public @Nullable RabbitTemplate getRabbitTemplate() {
		return this.rabbitTemplate;
	}

	@Override
	protected void endpointInit() {
		if (getConfirmCorrelationExpression() != null) {
			Assert.notNull(this.rabbitTemplate,
					"RabbitTemplate implementation is required for publisher confirms");
			this.rabbitTemplate.setConfirmCallback(this);
			if (!this.rabbitTemplate.getConnectionFactory().isPublisherConfirms()) {
				this.logger.warn("A confirm correlation expression is provided but the underlying connection factory "
						+ "does not support correlated delivery confirmations; no confirmations will be received");
			}
		}
		if (getReturnChannel() != null) {
			Assert.notNull(this.rabbitTemplate,
					"RabbitTemplate implementation is required for publisher confirms");
			this.rabbitTemplate.setReturnsCallback(this);
			if (!this.rabbitTemplate.getConnectionFactory().isPublisherReturns()) {
				this.logger.warn("A return channel is provided but the underlying connection factory "
						+ "does not support returned messages; none will be received");
			}
		}
		Duration confirmTimeout = getConfirmTimeout();
		if (confirmTimeout != null) {
			this.waitForConfirmTimeout = confirmTimeout;
		}
	}

	@Override
	protected void doStop() {
		if (this.amqpTemplate instanceof Lifecycle lifecycle) {
			lifecycle.stop();
		}
	}

	@Override
	protected @Nullable Object handleRequestMessage(Message<?> requestMessage) {
		CorrelationData correlationData = generateCorrelationData(requestMessage);
		String exchangeName = generateExchangeName(requestMessage);
		String routingKey = generateRoutingKey(requestMessage);
		if (this.expectReply) {
			return sendAndReceive(exchangeName, routingKey, requestMessage, correlationData);
		}
		if (this.multiSend && requestMessage.getPayload() instanceof Iterable) {
			multiSend(requestMessage, exchangeName, routingKey);
			return null;
		}
		else {
			send(exchangeName, routingKey, requestMessage, correlationData);
			if (this.waitForConfirm && correlationData != null) {
				waitForConfirm(requestMessage, correlationData);
			}
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private void multiSend(Message<?> requestMessage, @Nullable String exchangeName, @Nullable String routingKey) {
		((Iterable<?>) requestMessage.getPayload()).forEach(payload -> {
			Assert.state(payload instanceof Message,
					"To use multiSend, the payload must be an Iterable<Message<?>>");
		});
		RabbitTemplate rabbitTemplateToUse = this.rabbitTemplate;
		Assert.notNull(rabbitTemplateToUse, "The 'RabbitTemplate' must be provided for multi-send.");
		rabbitTemplateToUse.<@Nullable Object>invoke(template -> {
			((Iterable<Message<?>>) requestMessage.getPayload())
					.forEach(message -> doRabbitSend(exchangeName, routingKey, message, null, rabbitTemplateToUse));
			if (this.waitForConfirm) {
				template.waitForConfirmsOrDie(this.waitForConfirmTimeout.toMillis());
			}
			return null;
		});
	}

	private void waitForConfirm(Message<?> requestMessage, CorrelationData correlationData) {
		try {
			Confirm confirm = correlationData.getFuture().get(this.waitForConfirmTimeout.toMillis(),
					TimeUnit.MILLISECONDS);
			if (!confirm.ack()) {
				throw new AmqpException("Negative publisher confirm received: " + confirm);
			}
			if (correlationData.getReturned() != null) {
				throw new AmqpException("Message was returned by the broker");
			}
		}
		catch (@SuppressWarnings("unused") InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		catch (ExecutionException e) {
			throw new AmqpException("Failed to get publisher confirm", e);
		}
		catch (TimeoutException e) {
			throw new MessageTimeoutException(requestMessage, this + ": Timed out awaiting publisher confirm", e);
		}
	}

	private void send(@Nullable String exchangeName, @Nullable String routingKey,
			final Message<?> requestMessage, @Nullable CorrelationData correlationData) {

		if (this.rabbitTemplate != null) {
			doRabbitSend(exchangeName, routingKey, requestMessage, correlationData, this.rabbitTemplate);
		}
		else {
			this.amqpTemplate.convertAndSend(exchangeName, routingKey, requestMessage.getPayload(),
					message -> {
						getHeaderMapper().fromHeadersToRequest(requestMessage.getHeaders(),
								message.getMessageProperties());
						return message;
					});
		}
	}

	private void doRabbitSend(@Nullable String exchangeName, @Nullable String routingKey, Message<?> requestMessage,
			@Nullable CorrelationData correlationData, RabbitTemplate template) {

		MessageConverter converter = template.getMessageConverter();
		org.springframework.amqp.core.Message amqpMessage = MappingUtils.mapMessage(requestMessage, converter,
				getHeaderMapper(), getDefaultDeliveryMode(), isHeadersMappedLast());
		addDelayProperty(requestMessage, amqpMessage);
		template.send(exchangeName, routingKey, amqpMessage, correlationData);
	}

	private @Nullable AbstractIntegrationMessageBuilder<?> sendAndReceive(@Nullable String exchangeName,
			@Nullable String routingKey, Message<?> requestMessage, @Nullable CorrelationData correlationData) {

		Assert.state(this.rabbitTemplate != null, "RabbitTemplate implementation is required for publisher confirms");
		MessageConverter converter = this.rabbitTemplate.getMessageConverter();
		org.springframework.amqp.core.Message amqpMessage = MappingUtils.mapMessage(requestMessage, converter,
				getHeaderMapper(), getDefaultDeliveryMode(), isHeadersMappedLast());
		addDelayProperty(requestMessage, amqpMessage);
		org.springframework.amqp.core.Message amqpReplyMessage =
				this.rabbitTemplate.sendAndReceive(exchangeName, routingKey, amqpMessage,
						correlationData);

		if (amqpReplyMessage == null) {
			return null;
		}
		return buildReply(converter, amqpReplyMessage);
	}

	@Override
	public void confirm(@Nullable CorrelationData correlationData, boolean ack, @Nullable String cause) {
		handleConfirm(correlationData, ack, cause);
	}

	@Override
	public void returnedMessage(ReturnedMessage returnedMessage) {
		Assert.state(this.rabbitTemplate != null, "RabbitTemplate implementation is required for publisher confirms");
		MessageConverter converter = this.rabbitTemplate.getMessageConverter();
		Message<?> returned = buildReturnedMessage(returnedMessage, converter);
		Objects.requireNonNull(getReturnChannel()).send(returned);
	}

}
