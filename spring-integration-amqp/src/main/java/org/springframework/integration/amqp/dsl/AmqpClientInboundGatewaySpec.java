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
import org.springframework.amqp.rabbit.listener.adapter.ReplyPostProcessor;
import org.springframework.amqp.rabbitmq.client.AmqpConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.integration.amqp.inbound.AmqpClientInboundGateway;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.dsl.MessagingGatewaySpec;
import org.springframework.scheduling.TaskScheduler;

/**
 * Spec for an {@link AmqpClientInboundGateway}.
 *
 * @author Artem Bilan
 *
 * @since 7.0
 */
public class AmqpClientInboundGatewaySpec
		extends MessagingGatewaySpec<AmqpClientInboundGatewaySpec, AmqpClientInboundGateway> {

	public AmqpClientInboundGatewaySpec(AmqpConnectionFactory connectionFactory, String... queueNames) {
		super(new AmqpClientInboundGateway(connectionFactory, queueNames));
	}

	public AmqpClientInboundGatewaySpec initialCredits(int initialCredits) {
		this.target.setInitialCredits(initialCredits);
		return this;
	}

	public AmqpClientInboundGatewaySpec priority(int priority) {
		this.target.setPriority(priority);
		return this;
	}

	public AmqpClientInboundGatewaySpec stateListeners(Resource.StateListener... stateListeners) {
		this.target.setStateListeners(stateListeners);
		return this;
	}

	public AmqpClientInboundGatewaySpec afterReceivePostProcessors(
			MessagePostProcessor... afterReceivePostProcessors) {

		this.target.setAfterReceivePostProcessors(afterReceivePostProcessors);
		return this;
	}

	public AmqpClientInboundGatewaySpec taskScheduler(TaskScheduler taskScheduler) {
		this.target.setTaskScheduler(taskScheduler);
		return this;
	}

	public AmqpClientInboundGatewaySpec adviceChain(Advice... advices) {
		this.target.setAdviceChain(advices);
		return this;
	}

	public AmqpClientInboundGatewaySpec autoSettle(boolean autoSettle) {
		this.target.setAutoSettle(autoSettle);
		return this;
	}

	public AmqpClientInboundGatewaySpec defaultRequeue(boolean defaultRequeue) {
		this.target.setDefaultRequeue(defaultRequeue);
		return this;
	}

	public AmqpClientInboundGatewaySpec gracefulShutdownPeriod(Duration gracefulShutdownPeriod) {
		this.target.setGracefulShutdownPeriod(gracefulShutdownPeriod);
		return this;
	}

	public AmqpClientInboundGatewaySpec consumersPerQueue(int consumersPerQueue) {
		this.target.setConsumersPerQueue(consumersPerQueue);
		return this;
	}

	public AmqpClientInboundGatewaySpec messageConverter(@Nullable MessageConverter messageConverter) {
		this.target.setMessageConverter(messageConverter);
		return this;
	}

	public AmqpClientInboundGatewaySpec headerMapper(AmqpHeaderMapper headerMapper) {
		this.target.setHeaderMapper(headerMapper);
		return this;
	}

	public AmqpClientInboundGatewaySpec replyPostProcessor(ReplyPostProcessor replyPostProcessor) {
		this.target.setReplyPostProcessor(replyPostProcessor);
		return this;
	}

	public AmqpClientInboundGatewaySpec replyExchange(String exchange) {
		this.target.setReplyExchange(exchange);
		return this;
	}

	public AmqpClientInboundGatewaySpec replyRoutingKey(String routingKey) {
		this.target.setReplyRoutingKey(routingKey);
		return this;
	}

	public AmqpClientInboundGatewaySpec replyQueue(String queue) {
		this.target.setReplyQueue(queue);
		return this;
	}

}
