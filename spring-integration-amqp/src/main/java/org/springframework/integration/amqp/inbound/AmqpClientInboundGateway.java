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

package org.springframework.integration.amqp.inbound;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;

import com.rabbitmq.client.amqp.Resource;
import org.aopalliance.aop.Advice;
import org.jspecify.annotations.Nullable;

import org.springframework.amqp.core.Address;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.listener.adapter.ReplyPostProcessor;
import org.springframework.amqp.rabbitmq.client.AmqpConnectionFactory;
import org.springframework.amqp.rabbitmq.client.RabbitAmqpTemplate;
import org.springframework.amqp.rabbitmq.client.listener.RabbitAmqpListenerContainer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.amqp.support.postprocessor.MessagePostProcessorUtils;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.core.Pausable;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A {@link MessagingGatewaySupport} implementation for AMQP 1.0 client.
 * <p>
 * Based on the {@link RabbitAmqpListenerContainer} and requires an {@link AmqpConnectionFactory}.
 * An internal {@link RabbitAmqpTemplate} is used to send replies.
 *
 * @author Artem Bilan
 *
 * @since 7.0
 *
 * @see RabbitAmqpListenerContainer
 * @see RabbitAmqpTemplate
 * @see org.springframework.amqp.rabbitmq.client.listener.RabbitAmqpMessageListenerAdapter
 */
public class AmqpClientInboundGateway extends MessagingGatewaySupport implements Pausable {

	private final RabbitAmqpListenerContainer listenerContainer;

	private final RabbitAmqpTemplate replyTemplate;

	private @Nullable MessageConverter messageConverter = new SimpleMessageConverter();

	private AmqpHeaderMapper headerMapper = DefaultAmqpHeaderMapper.inboundMapper();

	private @Nullable Collection<MessagePostProcessor> afterReceivePostProcessors;

	private @Nullable ReplyPostProcessor replyPostProcessor;

	private volatile boolean paused;

	public AmqpClientInboundGateway(AmqpConnectionFactory connectionFactory, String... queueNames) {
		this.listenerContainer = new RabbitAmqpListenerContainer(connectionFactory);
		this.listenerContainer.setQueueNames(queueNames);
		this.replyTemplate = new RabbitAmqpTemplate(connectionFactory);
	}

	public void setInitialCredits(int initialCredits) {
		this.listenerContainer.setInitialCredits(initialCredits);
	}

	public void setPriority(int priority) {
		this.listenerContainer.setPriority(priority);
	}

	public void setStateListeners(Resource.StateListener... stateListeners) {
		this.listenerContainer.setStateListeners(stateListeners);
	}

	public void setAfterReceivePostProcessors(MessagePostProcessor... afterReceivePostProcessors) {
		this.afterReceivePostProcessors = MessagePostProcessorUtils.sort(Arrays.asList(afterReceivePostProcessors));
	}

	public void setAdviceChain(Advice... advices) {
		this.listenerContainer.setAdviceChain(advices);
	}

	public void setAutoSettle(boolean autoSettle) {
		this.listenerContainer.setAutoSettle(autoSettle);
	}

	public void setDefaultRequeue(boolean defaultRequeue) {
		this.listenerContainer.setDefaultRequeue(defaultRequeue);
	}

	public void setGracefulShutdownPeriod(Duration gracefulShutdownPeriod) {
		this.listenerContainer.setGracefulShutdownPeriod(gracefulShutdownPeriod);
	}

	public void setConsumersPerQueue(int consumersPerQueue) {
		this.listenerContainer.setConsumersPerQueue(consumersPerQueue);
	}

	/**
	 * Set a {@link MessageConverter} to replace the default {@link SimpleMessageConverter}.
	 * If set to null, an AMQP message is sent as is into a {@link Message} payload.
	 * And a reply message has to return an AMQP message as its payload.
	 * @param messageConverter the {@link MessageConverter} to use or null.
	 */
	public void setMessageConverter(@Nullable MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	public void setHeaderMapper(AmqpHeaderMapper headerMapper) {
		this.headerMapper = headerMapper;
	}

	public void setReplyPostProcessor(ReplyPostProcessor replyPostProcessor) {
		this.replyPostProcessor = replyPostProcessor;
	}

	/**
	 * Set a default {@code exchange} for sending replies
	 * if {@code replyTo} address is not provided in the request message.
	 * Mutually exclusive with {@link #setReplyQueue(String)}.
	 * @param exchange the default exchange for sending replies
	 */
	public void setReplyExchange(String exchange) {
		this.replyTemplate.setExchange(exchange);
	}

	/**
	 * Set a default {@code routingKey} for sending replies
	 * if {@code replyTo} address is not provided in the request message.
	 * Used only if {@link #setReplyExchange(String)} is provided.
	 * @param routingKey the default routing key for sending replies
	 */
	public void setReplyRoutingKey(String routingKey) {
		this.replyTemplate.setRoutingKey(routingKey);
	}

	/**
	 * Set a default {@code queue} for sending replies
	 * if {@code replyTo} address is not provided in the request message.
	 * Mutually exclusive with {@link #setReplyExchange(String)}.
	 * @param queue the default queue for sending replies
	 */
	public void setReplyQueue(String queue) {
		this.replyTemplate.setQueue(queue);
	}

	@Override
	public String getComponentType() {
		return "amqp:inbound-gateway";
	}

	@Override
	protected void onInit() {
		super.onInit();
		this.listenerContainer.setBeanName(getComponentName() + ".listenerContainer");
		IntegrationRabbitAmqpMessageListener messageListener =
				new IntegrationRabbitAmqpMessageListener(this, this::processRequest, this.headerMapper,
						this.messageConverter, this.afterReceivePostProcessors);
		this.listenerContainer.setupMessageListener(messageListener);
		this.listenerContainer.afterPropertiesSet();
	}

	@Override
	protected void doStart() {
		super.doStart();
		this.listenerContainer.start();
	}

	@Override
	protected void doStop() {
		super.doStop();
		this.listenerContainer.stop();
	}

	@Override
	public void destroy() {
		super.destroy();
		this.listenerContainer.destroy();
		this.replyTemplate.destroy();
	}

	@Override
	public void pause() {
		this.listenerContainer.pause();
		this.paused = true;
	}

	@Override
	public void resume() {
		this.listenerContainer.resume();
		this.paused = false;
	}

	@Override
	public boolean isPaused() {
		return this.paused;
	}

	/**
	 * Use as {@link java.util.function.BiConsumer} for the {@link IntegrationRabbitAmqpMessageListener}.
	 * @param messageToSend the message to produce from this endpoint.
	 * @param requestMessage the request AMQP message.
	 */
	private void processRequest(Message<?> messageToSend, org.springframework.amqp.core.Message requestMessage) {
		Message<?> receivedMessage = sendAndReceiveMessage(messageToSend);
		if (receivedMessage != null) {
			org.springframework.amqp.core.Message replyMessage = fromSpringMessage(receivedMessage, requestMessage);
			publishReply(requestMessage, replyMessage);
		}
		else {
			this.logger.warn(() -> "No reply received for message: " + requestMessage);
		}
	}

	private org.springframework.amqp.core.Message fromSpringMessage(Message<?> receivedMessage,
			org.springframework.amqp.core.Message requestMessage) {

		org.springframework.amqp.core.Message replyMessage;
		MessageProperties messageProperties = new MessageProperties();
		Object payload = receivedMessage.getPayload();
		if (payload instanceof org.springframework.amqp.core.Message amqpMessage) {
			replyMessage = amqpMessage;
		}
		else {
			Assert.state(this.messageConverter != null,
					"If reply payload is not an 'org.springframework.amqp.core.Message', " +
							"the 'messageConverter' must be provided.");

			replyMessage = this.messageConverter.toMessage(payload, messageProperties);
			this.headerMapper.fromHeadersToReply(receivedMessage.getHeaders(),
					messageProperties);
		}

		postProcessResponse(requestMessage, replyMessage);
		if (this.replyPostProcessor != null) {
			replyMessage = this.replyPostProcessor.apply(requestMessage, replyMessage);
		}

		return replyMessage;
	}

	private void publishReply(org.springframework.amqp.core.Message requestMessage,
			org.springframework.amqp.core.Message replyMessage) {

		Address replyTo = requestMessage.getMessageProperties().getReplyToAddress();
		if (replyTo != null) {
			String exchangeName = replyTo.getExchangeName();
			String routingKey = replyTo.getRoutingKey();
			if (StringUtils.hasText(exchangeName)) {
				this.replyTemplate.send(exchangeName, routingKey, replyMessage).join();
			}
			else {
				Assert.hasText(routingKey, "A 'replyTo' property must be provided in the requestMessage.");
				String queue = routingKey.replaceFirst("queues/", "");
				this.replyTemplate.send(queue, replyMessage).join();
			}
		}
		else {
			this.replyTemplate.send(replyMessage).join();
		}
	}

	/**
	 * Post-process the given response message before it will be sent.
	 * The default implementation sets the response's correlation id to the request message's correlation id, if any;
	 * otherwise to the request message id.
	 * @param request the original incoming Rabbit message
	 * @param response the outgoing Rabbit message about to be sent
	 */
	private static void postProcessResponse(org.springframework.amqp.core.Message request,
			org.springframework.amqp.core.Message response) {

		String correlation = request.getMessageProperties().getCorrelationId();

		if (correlation == null) {
			String messageId = request.getMessageProperties().getMessageId();
			if (messageId != null) {
				correlation = messageId;
			}
		}
		response.getMessageProperties().setCorrelationId(correlation);
	}

}
