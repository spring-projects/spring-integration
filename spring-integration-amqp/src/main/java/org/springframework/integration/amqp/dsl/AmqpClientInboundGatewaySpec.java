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
import org.springframework.amqp.listener.adapter.ReplyPostProcessor;
import org.springframework.amqp.rabbitmq.client.AmqpConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.integration.amqp.inbound.AmqpClientInboundGateway;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.dsl.MessagingGatewaySpec;

/**
 * Spec for an {@link AmqpClientInboundGateway}.
 *
 * @author Artem Bilan
 *
 * @since 7.0
 */
public class AmqpClientInboundGatewaySpec
		extends MessagingGatewaySpec<AmqpClientInboundGatewaySpec, AmqpClientInboundGateway> {

	/**
	 * Create an instance based on a {@link AmqpConnectionFactory} and queues to consume from.
	 * @param connectionFactory the {@link AmqpConnectionFactory} to connect
	 * @param queueNames queues to consume from
	 */
	public AmqpClientInboundGatewaySpec(AmqpConnectionFactory connectionFactory, String... queueNames) {
		super(new AmqpClientInboundGateway(connectionFactory, queueNames));
	}

	/**
	 * The initial number credits to grant to the AMQP receiver.
	 * The default is {@code 100}.
	 * @param initialCredits number of initial credits
	 * @return the spec
	 */
	public AmqpClientInboundGatewaySpec initialCredits(int initialCredits) {
		this.target.setInitialCredits(initialCredits);
		return this;
	}

	/**
	 * The consumer priority.
	 * @param priority consumer priority
	 * @return the spec
	 */
	public AmqpClientInboundGatewaySpec priority(int priority) {
		this.target.setPriority(priority);
		return this;
	}

	/**
	 * Add {@link Resource.StateListener} instances to the consumer.
	 * @param stateListeners listeners to add
	 * @return the spec
	 */
	public AmqpClientInboundGatewaySpec stateListeners(Resource.StateListener... stateListeners) {
		this.target.setStateListeners(stateListeners);
		return this;
	}

	/**
	 * Add {@link MessagePostProcessor} instances to apply on just received messages.
	 * @param afterReceivePostProcessors listeners to add
	 * @return the spec
	 */
	public AmqpClientInboundGatewaySpec afterReceivePostProcessors(
			MessagePostProcessor... afterReceivePostProcessors) {

		this.target.setAfterReceivePostProcessors(afterReceivePostProcessors);
		return this;
	}

	/**
	 * Set {@link Advice} instances to proxy message listener.
	 * @param advices the advices to add
	 * @return the spec
	 */
	public AmqpClientInboundGatewaySpec adviceChain(Advice... advices) {
		this.target.setAdviceChain(advices);
		return this;
	}

	/**
	 * Set to {@code false} to propagate an acknowledgement callback into message headers
	 * for downstream flow manual settlement.
	 * @param autoSettle {@code true} to acknowledge messages automatically.
	 * @return the spec
	 */
	public AmqpClientInboundGatewaySpec autoSettle(boolean autoSettle) {
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
	public AmqpClientInboundGatewaySpec defaultRequeue(boolean defaultRequeue) {
		this.target.setDefaultRequeue(defaultRequeue);
		return this;
	}

	/**
	 * Set a duration for how long to wait for all the consumers to shut down successfully on listener container stop.
	 * Default 30 seconds.
	 * @param gracefulShutdownPeriod the timeout to wait on stop.
	 * @return the spec
	 */
	public AmqpClientInboundGatewaySpec gracefulShutdownPeriod(Duration gracefulShutdownPeriod) {
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
	public AmqpClientInboundGatewaySpec consumersPerQueue(int consumersPerQueue) {
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
	public AmqpClientInboundGatewaySpec messageConverter(@Nullable MessageConverter messageConverter) {
		this.target.setMessageConverter(messageConverter);
		return this;
	}

	/**
	 * Set an {@link AmqpHeaderMapper} to map request and reply headers.
	 * @param headerMapper the {@link AmqpHeaderMapper} to use.
	 * @return the spec
	 */
	public AmqpClientInboundGatewaySpec headerMapper(AmqpHeaderMapper headerMapper) {
		this.target.setHeaderMapper(headerMapper);
		return this;
	}

	/**
	 * Set an {@link ReplyPostProcessor} to modify the reply AMQP message before producing.
	 * @param replyPostProcessor the {@link ReplyPostProcessor} to use.
	 * @return the spec
	 */
	public AmqpClientInboundGatewaySpec replyPostProcessor(ReplyPostProcessor replyPostProcessor) {
		this.target.setReplyPostProcessor(replyPostProcessor);
		return this;
	}

	/**
	 * Set an exchange for publishing reply.
	 * Mutually exclusive with {@link #replyQueue(String)}.
	 * If neither is set, the {@code replyTo} property from the request message
	 * is used to determine where to produce a reply.
	 * @param exchange the exchange to send a reply.
	 * @return the spec
	 */
	public AmqpClientInboundGatewaySpec replyExchange(String exchange) {
		this.target.setReplyExchange(exchange);
		return this;
	}

	/**
	 * Set a routing key for publishing reply.
	 * Used only together with {@link #replyExchange(String)}.
	 * If neither is set, the {@code replyTo} property from the request message
	 * is used to determine where to produce a reply.
	 * @param routingKey the routing key to send a reply.
	 * @return the spec
	 */
	public AmqpClientInboundGatewaySpec replyRoutingKey(String routingKey) {
		this.target.setReplyRoutingKey(routingKey);
		return this;
	}

	/**
	 * Set a queue for publishing reply.
	 * Mutually exclusive with {@link #replyExchange(String)}.
	 * If neither is set, the {@code replyTo} property from the request message
	 * is used to determine where to produce a reply.
	 * @param queue the queue to send a reply.
	 * @return the spec
	 */
	public AmqpClientInboundGatewaySpec replyQueue(String queue) {
		this.target.setReplyQueue(queue);
		return this;
	}

}
