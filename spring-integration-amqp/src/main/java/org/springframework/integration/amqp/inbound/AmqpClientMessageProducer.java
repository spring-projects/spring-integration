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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.rabbitmq.client.amqp.Consumer;
import com.rabbitmq.client.amqp.Resource;
import org.aopalliance.aop.Advice;
import org.jspecify.annotations.Nullable;

import org.springframework.amqp.core.AmqpAcknowledgment;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.amqp.rabbitmq.client.AmqpConnectionFactory;
import org.springframework.amqp.rabbitmq.client.RabbitAmqpUtils;
import org.springframework.amqp.rabbitmq.client.listener.RabbitAmqpListenerContainer;
import org.springframework.amqp.rabbitmq.client.listener.RabbitAmqpMessageListener;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.amqp.support.postprocessor.MessagePostProcessorUtils;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.acks.AcknowledgmentCallback;
import org.springframework.integration.acks.SimpleAcknowledgment;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.core.Pausable;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.support.MutableMessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.scheduling.TaskScheduler;

/**
 * A {@link MessageProducerSupport} implementation for AMQP 1.0 client.
 * <p>
 * Based on the {@link RabbitAmqpListenerContainer} and requires an {@link AmqpConnectionFactory}.
 *
 * @author Artem Bilan
 *
 * @since 7.0
 *
 * @see RabbitAmqpListenerContainer
 * @see org.springframework.amqp.rabbitmq.client.listener.RabbitAmqpMessageListenerAdapter
 */
public class AmqpClientMessageProducer extends MessageProducerSupport implements Pausable {

	private final RabbitAmqpListenerContainer listenerContainer;

	private @Nullable MessageConverter messageConverter = new SimpleMessageConverter();

	private AmqpHeaderMapper headerMapper = DefaultAmqpHeaderMapper.inboundMapper();

	private @Nullable Collection<MessagePostProcessor> afterReceivePostProcessors;

	private volatile boolean paused;

	public AmqpClientMessageProducer(AmqpConnectionFactory connectionFactory, String... queueNames) {
		this.listenerContainer = new RabbitAmqpListenerContainer(connectionFactory);
		this.listenerContainer.setQueueNames(queueNames);
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

	public void setBatchSize(int batchSize) {
		this.listenerContainer.setBatchSize(batchSize);
	}

	public void setBatchReceiveTimeout(long batchReceiveTimeout) {
		this.listenerContainer.setBatchReceiveTimeout(batchReceiveTimeout);
	}

	@Override
	public void setTaskScheduler(TaskScheduler taskScheduler) {
		this.listenerContainer.setTaskScheduler(taskScheduler);
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
	 * @param messageConverter the {@link MessageConverter} to use or null.
	 */
	public void setMessageConverter(@Nullable MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	public void setHeaderMapper(AmqpHeaderMapper headerMapper) {
		this.headerMapper = headerMapper;
	}

	@Override
	public String getComponentType() {
		return "amqp:inbound-channel-adapter";
	}

	@Override
	protected void onInit() {
		super.onInit();
		this.listenerContainer.setBeanName(getComponentName() + ".listenerContainer");
		this.listenerContainer.setupMessageListener(new IntegrationRabbitAmqpMessageListener());
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

	private final class IntegrationRabbitAmqpMessageListener implements RabbitAmqpMessageListener {

		@Override
		public void onAmqpMessage(com.rabbitmq.client.amqp.Message amqpMessage, Consumer.@Nullable Context context) {
			org.springframework.amqp.core.Message message = RabbitAmqpUtils.fromAmqpMessage(amqpMessage, context);
			Message<?> messageToSend = toSpringMessage(message);

			try {
				sendMessage(messageToSend);
			}
			catch (Exception ex) {
				throw new ListenerExecutionFailedException(getComponentName() + ".onAmqpMessage() failed", ex, message);
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
				sendMessage(messageToSend);
			}
			catch (Exception ex) {
				throw new ListenerExecutionFailedException(getComponentName() + ".onMessageBatch() failed", ex,
						messages.toArray(org.springframework.amqp.core.Message[]::new));
			}
		}

		private Message<?> toSpringMessage(org.springframework.amqp.core.Message message) {
			if (AmqpClientMessageProducer.this.afterReceivePostProcessors != null) {
				for (MessagePostProcessor processor : AmqpClientMessageProducer.this.afterReceivePostProcessors) {
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
			if (AmqpClientMessageProducer.this.messageConverter != null) {
				payload = AmqpClientMessageProducer.this.messageConverter.fromMessage(message);
				headers = AmqpClientMessageProducer.this.headerMapper.toHeadersFromRequest(messageProperties);
			}

			return getMessageBuilderFactory()
					.withPayload(payload)
					.copyHeaders(headers)
					.setHeader(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK, acknowledgmentCallback)
					.build();
		}

		@Override
		public void onMessage(org.springframework.amqp.core.Message message) {
			throw new UnsupportedOperationException("The 'RabbitAmqpMessageListener' does not implement 'onMessage()'");
		}

	}

	/**
	 * The {@link AcknowledgmentCallback} adapter for an {@link AmqpAcknowledgment}.
	 * @param delegate the {@link AmqpAcknowledgment} to delegate to.
	 */
	private record AmqpAcknowledgmentCallback(AmqpAcknowledgment delegate) implements AcknowledgmentCallback {

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
