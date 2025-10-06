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

package org.springframework.integration.amqp.dsl;

import java.time.Duration;

import com.rabbitmq.client.amqp.Resource;
import org.aopalliance.aop.Advice;
import org.jspecify.annotations.Nullable;

import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbitmq.client.AmqpConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.integration.amqp.inbound.AmqpClientMessageProducer;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.dsl.MessageProducerSpec;
import org.springframework.scheduling.TaskScheduler;

/**
 * Spec for an {@link AmqpClientMessageProducer}.
 *
 * @author Artem Bilan
 *
 * @since 7.0
 */
public class AmqpClientInboundChannelAdapterSpec
		extends MessageProducerSpec<AmqpClientInboundChannelAdapterSpec, AmqpClientMessageProducer> {

	public AmqpClientInboundChannelAdapterSpec(AmqpConnectionFactory connectionFactory, String... queueNames) {
		super(new AmqpClientMessageProducer(connectionFactory, queueNames));
	}

	public AmqpClientInboundChannelAdapterSpec initialCredits(int initialCredits) {
		this.target.setInitialCredits(initialCredits);
		return this;
	}

	public AmqpClientInboundChannelAdapterSpec priority(int priority) {
		this.target.setPriority(priority);
		return this;
	}

	public AmqpClientInboundChannelAdapterSpec stateListeners(Resource.StateListener... stateListeners) {
		this.target.setStateListeners(stateListeners);
		return this;
	}

	public AmqpClientInboundChannelAdapterSpec afterReceivePostProcessors(
			MessagePostProcessor... afterReceivePostProcessors) {

		this.target.setAfterReceivePostProcessors(afterReceivePostProcessors);
		return this;
	}

	public AmqpClientInboundChannelAdapterSpec batchSize(int batchSize) {
		this.target.setBatchSize(batchSize);
		return this;
	}

	public AmqpClientInboundChannelAdapterSpec batchReceiveTimeout(long batchReceiveTimeout) {
		this.target.setBatchReceiveTimeout(batchReceiveTimeout);
		return this;
	}

	public AmqpClientInboundChannelAdapterSpec taskScheduler(TaskScheduler taskScheduler) {
		this.target.setTaskScheduler(taskScheduler);
		return this;
	}

	public AmqpClientInboundChannelAdapterSpec adviceChain(Advice... advices) {
		this.target.setAdviceChain(advices);
		return this;
	}

	public AmqpClientInboundChannelAdapterSpec autoSettle(boolean autoSettle) {
		this.target.setAutoSettle(autoSettle);
		return this;
	}

	public AmqpClientInboundChannelAdapterSpec defaultRequeue(boolean defaultRequeue) {
		this.target.setDefaultRequeue(defaultRequeue);
		return this;
	}

	public AmqpClientInboundChannelAdapterSpec gracefulShutdownPeriod(Duration gracefulShutdownPeriod) {
		this.target.setGracefulShutdownPeriod(gracefulShutdownPeriod);
		return this;
	}

	public AmqpClientInboundChannelAdapterSpec consumersPerQueue(int consumersPerQueue) {
		this.target.setConsumersPerQueue(consumersPerQueue);
		return this;
	}

	public AmqpClientInboundChannelAdapterSpec messageConverter(@Nullable MessageConverter messageConverter) {
		this.target.setMessageConverter(messageConverter);
		return this;
	}

	public AmqpClientInboundChannelAdapterSpec headerMapper(AmqpHeaderMapper headerMapper) {
		this.target.setHeaderMapper(headerMapper);
		return this;
	}

}
