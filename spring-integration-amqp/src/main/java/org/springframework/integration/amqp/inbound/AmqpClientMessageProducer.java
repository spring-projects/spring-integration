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

import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbitmq.client.AmqpConnectionFactory;
import org.springframework.amqp.rabbitmq.client.listener.RabbitAmqpListenerContainer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.amqp.support.postprocessor.MessagePostProcessorUtils;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.core.Pausable;
import org.springframework.integration.endpoint.MessageProducerSupport;
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
	 * @param requestMessage the request AMQP message. Ignored in this implementation.
	 */
	private void processRequest(Message<?> messageToSend,
			org.springframework.amqp.core.@Nullable Message requestMessage) {

		sendMessage(messageToSend);
	}

}
