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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import com.rabbitmq.client.amqp.Consumer;
import org.jspecify.annotations.Nullable;

import org.springframework.amqp.core.AmqpAcknowledgment;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.listener.ListenerExecutionFailedException;
import org.springframework.amqp.rabbitmq.client.RabbitAmqpUtils;
import org.springframework.amqp.rabbitmq.client.listener.RabbitAmqpMessageListener;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.acks.AcknowledgmentCallback;
import org.springframework.integration.acks.SimpleAcknowledgment;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.MutableMessageBuilder;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.messaging.Message;

/**
 * An internal {@link RabbitAmqpMessageListener} implementation for the AMQP 1.0 endpoints.
 *
 * @param amqpInboundEndpoint the endpoint this listener is used in
 * @param requestAction the action to perform on the message to produce from the endpoint
 * @param headerMapper a mapper to convert AMQP headers to Spring Integration headers
 * @param messageConverter the message converter from AMQP message to the Spring Integration message
 * @param afterReceivePostProcessors the post-processors to apply on the received AMQP message
 *
 * @author Artem Bilan
 *
 * @since 7.0
 */
record IntegrationRabbitAmqpMessageListener(NamedComponent amqpInboundEndpoint,
											BiConsumer<Message<?>, org.springframework.amqp.core.@Nullable Message> requestAction,
											AmqpHeaderMapper headerMapper, @Nullable MessageConverter messageConverter,
											@Nullable Collection<MessagePostProcessor> afterReceivePostProcessors)
		implements RabbitAmqpMessageListener {

	@Override
	public void onAmqpMessage(com.rabbitmq.client.amqp.Message amqpMessage, Consumer.@Nullable Context context) {
		org.springframework.amqp.core.Message message = RabbitAmqpUtils.fromAmqpMessage(amqpMessage, context);
		Message<?> messageToSend = toSpringMessage(message);
		try {
			this.requestAction.accept(messageToSend, message);
		}
		catch (Exception ex) {
			throw new ListenerExecutionFailedException(
					this.amqpInboundEndpoint.getComponentName() + ".onAmqpMessage() failed", ex, message);
		}
	}

	@Override
	public void onMessageBatch(List<org.springframework.amqp.core.Message> messages) {
		SimpleAcknowledgment acknowledgmentCallback = null;
		List<Message<?>> springMessages = new ArrayList<>(messages.size());
		for (org.springframework.amqp.core.Message message : messages) {
			Message<?> springMessage = toSpringMessage(message);
			if (acknowledgmentCallback == null) {
				acknowledgmentCallback = StaticMessageHeaderAccessor.getAcknowledgment(springMessage);
			}
			springMessages.add(springMessage);
		}

		Message<List<Message<?>>> messageToSend =
				MutableMessageBuilder.withPayload(springMessages)
						.setHeader(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK, acknowledgmentCallback)
						.build();

		try {
			this.requestAction.accept(messageToSend, null);
		}
		catch (Exception ex) {
			throw new ListenerExecutionFailedException(
					this.amqpInboundEndpoint.getComponentName() + ".onMessageBatch() failed", ex,
					messages.toArray(org.springframework.amqp.core.Message[]::new));
		}
	}

	private Message<?> toSpringMessage(org.springframework.amqp.core.Message message) {
		if (this.afterReceivePostProcessors != null) {
			for (MessagePostProcessor processor : this.afterReceivePostProcessors) {
				message = processor.postProcessMessage(message);
			}
		}
		MessageProperties messageProperties = message.getMessageProperties();
		AmqpAcknowledgment amqpAcknowledgment = messageProperties.getAmqpAcknowledgment();
		AmqpAcknowledgmentCallback acknowledgmentCallback = null;
		if (amqpAcknowledgment != null) {
			acknowledgmentCallback = new AmqpAcknowledgmentCallback(amqpAcknowledgment);
		}

		Object payload = message;
		Map<String, @Nullable Object> headers = null;
		if (this.messageConverter != null) {
			payload = this.messageConverter.fromMessage(message);
			headers = this.headerMapper.toHeadersFromRequest(messageProperties);
		}

		return MessageBuilder.withPayload(payload)
				.copyHeaders(headers)
				.setHeader(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK, acknowledgmentCallback)
				.build();
	}

	@Override
	public void onMessage(org.springframework.amqp.core.Message message) {
		throw new UnsupportedOperationException(
				"The 'IntegrationRabbitAmqpMessageListener' does not implement 'onMessage()'");
	}

	/**
	 * The {@link AcknowledgmentCallback} adapter for an {@link AmqpAcknowledgment}.
	 *
	 * @param delegate the {@link AmqpAcknowledgment} to delegate to.
	 *
	 * @author Artem Bilan
	 *
	 * @since 7.0
	 */
	record AmqpAcknowledgmentCallback(AmqpAcknowledgment delegate) implements AcknowledgmentCallback {

		@Override
		public void acknowledge(Status status) {
			this.delegate.acknowledge(AmqpAcknowledgment.Status.valueOf(status.name()));
		}

		@Override
		public boolean isAutoAck() {
			return false;
		}

	}

}
