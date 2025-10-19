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

	/**
	 * Create an instance based on a {@link AmqpConnectionFactory} and queues to consume from.
	 * @param connectionFactory the {@link AmqpConnectionFactory} to connect
	 * @param queueNames queues to consume from
	 */
	public AmqpClientInboundChannelAdapterSpec(AmqpConnectionFactory connectionFactory, String... queueNames) {
		super(new AmqpClientMessageProducer(connectionFactory, queueNames));
	}

	/**
	 * The initial number credits to grant to the AMQP receiver.
	 * The default is {@code 100}.
	 * @param initialCredits number of initial credits
	 * @return the spec
	 */
	public AmqpClientInboundChannelAdapterSpec initialCredits(int initialCredits) {
		this.target.setInitialCredits(initialCredits);
		return this;
	}

	/**
	 * The consumer priority.
	 * @param priority consumer priority
	 * @return the spec
	 */
	public AmqpClientInboundChannelAdapterSpec priority(int priority) {
		this.target.setPriority(priority);
		return this;
	}

	/**
	 * Add {@link Resource.StateListener} instances to the consumer.
	 * @param stateListeners listeners to add
	 * @return the spec
	 */
	public AmqpClientInboundChannelAdapterSpec stateListeners(Resource.StateListener... stateListeners) {
		this.target.setStateListeners(stateListeners);
		return this;
	}

	/**
	 * Add {@link MessagePostProcessor} instances to apply on just received messages.
	 * @param afterReceivePostProcessors listeners to add
	 * @return the spec
	 */
	public AmqpClientInboundChannelAdapterSpec afterReceivePostProcessors(
			MessagePostProcessor... afterReceivePostProcessors) {

		this.target.setAfterReceivePostProcessors(afterReceivePostProcessors);
		return this;
	}

	/**
	 * Set a number of AMQP messages to gather before producing as a single message downstream.
	 * Default 1 - no batching.
	 * @param batchSize the batch size to use.
	 * @return the spec
	 * @see #batchReceiveTimeout(long)
	 */
	public AmqpClientInboundChannelAdapterSpec batchSize(int batchSize) {
		this.target.setBatchSize(batchSize);
		return this;
	}

	/**
	 * Set a timeout in milliseconds for how long a batch gathering process should go.
	 * Therefore, the batch is released as a single message whatever first happens:
	 * this timeout or {@link #batchSize(int)}.
	 * Default 30 seconds.
	 * @param batchReceiveTimeout the timeout for gathering a batch.
	 * @return the spec
	 */
	public AmqpClientInboundChannelAdapterSpec batchReceiveTimeout(long batchReceiveTimeout) {
		this.target.setBatchReceiveTimeout(batchReceiveTimeout);
		return this;
	}

	/**
	 * Set a {@link TaskScheduler} for monitoring batch releases.
	 * @param taskScheduler the taskScheduler to use
	 * @return the spec
	 */
	public AmqpClientInboundChannelAdapterSpec taskScheduler(TaskScheduler taskScheduler) {
		this.target.setTaskScheduler(taskScheduler);
		return this;
	}

	/**
	 * Set {@link Advice} instances to proxy message listener.
	 * @param advices the advices to add
	 * @return the spec
	 */
	public AmqpClientInboundChannelAdapterSpec adviceChain(Advice... advices) {
		this.target.setAdviceChain(advices);
		return this;
	}

	/**
	 * Set to {@code false} to propagate an acknowledgement callback into message headers
	 * for downstream flow manual settlement.
	 * @param autoSettle {@code true} to acknowledge messages automatically.
	 * @return the spec
	 */
	public AmqpClientInboundChannelAdapterSpec autoSettle(boolean autoSettle) {
		this.target.setAutoSettle(autoSettle);
		return this;
	}

	/**
	 * Set the default behavior when a message processing has failed.
	 * When true, messages will be requeued, when false, they will be discarded.
	 * This option can be overruled by throwing
	 * {@link org.springframework.amqp.AmqpRejectAndDontRequeueException} or
	 * {@link org.springframework.amqp.ImmediateRequeueAmqpException} from the downstream flow.
	 * Default true.
	 * @param defaultRequeue true to requeue by default.
	 * @return the spec
	 */
	public AmqpClientInboundChannelAdapterSpec defaultRequeue(boolean defaultRequeue) {
		this.target.setDefaultRequeue(defaultRequeue);
		return this;
	}

	/**
	 * Set a duration for how long to wait for all the consumers to shut down successfully on listener container stop.
	 * Default 30 seconds.
	 * @param gracefulShutdownPeriod the timeout to wait on stop.
	 * @return the spec
	 */
	public AmqpClientInboundChannelAdapterSpec gracefulShutdownPeriod(Duration gracefulShutdownPeriod) {
		this.target.setGracefulShutdownPeriod(gracefulShutdownPeriod);
		return this;
	}

	/**
	 * Each queue runs in its own consumer; set this property to create multiple
	 * consumers for each queue.
	 * Can be treated as {@code concurrency}, but per queue.
	 * @param consumersPerQueue the consumers per queue.
	 * @return the spec
	 */
	public AmqpClientInboundChannelAdapterSpec consumersPerQueue(int consumersPerQueue) {
		this.target.setConsumersPerQueue(consumersPerQueue);
		return this;
	}

	/**
	 * Set a {@link MessageConverter} to replace the default
	 * {@link org.springframework.amqp.support.converter.SimpleMessageConverter}.
	 * If set to null, an AMQP message is sent as is into a message payload.
	 * And a reply message has to return an AMQP message as its payload.
	 * @param messageConverter the {@link MessageConverter} to use or null.
	 * @return the spec
	 */
	public AmqpClientInboundChannelAdapterSpec messageConverter(@Nullable MessageConverter messageConverter) {
		this.target.setMessageConverter(messageConverter);
		return this;
	}

	/**
	 * Set an {@link AmqpHeaderMapper} to map request headers.
	 * @param headerMapper the {@link AmqpHeaderMapper} to use.
	 * @return the spec
	 */
	public AmqpClientInboundChannelAdapterSpec headerMapper(AmqpHeaderMapper headerMapper) {
		this.target.setHeaderMapper(headerMapper);
		return this;
	}

}
