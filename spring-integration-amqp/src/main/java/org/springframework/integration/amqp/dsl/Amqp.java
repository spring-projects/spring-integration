/*
 * Copyright 2014-2020 the original author or authors.
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

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.DirectMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.integration.amqp.channel.PollableAmqpChannel;
import org.springframework.integration.amqp.inbound.AmqpMessageSource.AmqpAckCallbackFactory;
import org.springframework.lang.Nullable;

/**
 * Factory class for AMQP components.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @author Artem Vozhdayenko
 *
 * @since 5.0
 */
public final class Amqp {

	/**
	 * Create an initial {@link AmqpInboundGatewaySpec}.
	 * @param connectionFactory the connectionFactory.
	 * @param queueNames the queueNames.
	 * @return the AmqpInboundGatewaySpec.
	 */
	public static AmqpInboundGatewaySMLCSpec inboundGateway(ConnectionFactory connectionFactory, String... queueNames) {
		SimpleMessageListenerContainer listenerContainer = new SimpleMessageListenerContainer(connectionFactory);
		listenerContainer.setQueueNames(queueNames);
		return inboundGateway(listenerContainer);
	}

	/**
	 * Create an initial {@link AmqpInboundGatewaySpec}.
	 * @param connectionFactory the connectionFactory.
	 * @param amqpTemplate the {@link AmqpTemplate} to use.
	 * @param queueNames the queueNames.
	 * @return the AmqpInboundGatewaySpec.
	 */
	public static AmqpInboundGatewaySMLCSpec inboundGateway(ConnectionFactory connectionFactory,
			AmqpTemplate amqpTemplate, String... queueNames) {

		SimpleMessageListenerContainer listenerContainer = new SimpleMessageListenerContainer(connectionFactory);
		listenerContainer.setQueueNames(queueNames);
		return inboundGateway(listenerContainer, amqpTemplate);
	}


	/**
	 * Create an initial {@link AmqpInboundGatewaySpec}.
	 * @param connectionFactory the connectionFactory.
	 * @param queues the queues.
	 * @return the AmqpInboundGatewaySpec.
	 */
	public static AmqpInboundGatewaySMLCSpec inboundGateway(ConnectionFactory connectionFactory, Queue... queues) {
		SimpleMessageListenerContainer listenerContainer = new SimpleMessageListenerContainer(connectionFactory);
		listenerContainer.setQueues(queues);
		return inboundGateway(listenerContainer);
	}

	/**
	 * Create an initial {@link AmqpInboundGatewaySpec}.
	 * @param connectionFactory the connectionFactory.
	 * @param amqpTemplate the {@link AmqpTemplate} to use.
	 * @param queues the queues.
	 * @return the AmqpInboundGatewaySpec.
	 */
	public static AmqpInboundGatewaySMLCSpec inboundGateway(ConnectionFactory connectionFactory,
			AmqpTemplate amqpTemplate, Queue... queues) {

		SimpleMessageListenerContainer listenerContainer = new SimpleMessageListenerContainer(connectionFactory);
		listenerContainer.setQueues(queues);
		return inboundGateway(listenerContainer, amqpTemplate);
	}

	/**
	 * Create an initial {@link AmqpInboundGatewaySMLCSpec}
	 * with provided {@link SimpleMessageListenerContainer}.
	 * Note: only endpoint options are available from spec.
	 * The {@code listenerContainer} options should be specified
	 * on the provided {@link SimpleMessageListenerContainer} using
	 * {@link AmqpInboundGatewaySMLCSpec#configureContainer(java.util.function.Consumer)}.
	 * @param listenerContainer the listenerContainer
	 * @return the AmqpInboundGatewaySMLCSpec.
	 */
	public static AmqpInboundGatewaySMLCSpec inboundGateway(SimpleMessageListenerContainer listenerContainer) {
		return new AmqpInboundGatewaySMLCSpec(listenerContainer);
	}

	/**
	 * Create an initial {@link AmqpInboundGatewaySMLCSpec}
	 * with provided {@link SimpleMessageListenerContainer}.
	 * Note: only endpoint options are available from spec.
	 * The {@code listenerContainer} options should be specified
	 * on the provided {@link SimpleMessageListenerContainer}using
	 * {@link AmqpInboundGatewaySMLCSpec#configureContainer(java.util.function.Consumer)}.
	 * @param listenerContainer the listenerContainer
	 * @param amqpTemplate the {@link AmqpTemplate} to use.
	 * @return the AmqpInboundGatewaySMLCSpec.
	 */
	public static AmqpInboundGatewaySMLCSpec inboundGateway(SimpleMessageListenerContainer listenerContainer,
			AmqpTemplate amqpTemplate) {

		return new AmqpInboundGatewaySMLCSpec(listenerContainer, amqpTemplate);
	}

	/**
	 * Create an initial {@link DirectMessageListenerContainerSpec}
	 * with provided {@link DirectMessageListenerContainer}.
	 * Note: only endpoint options are available from spec.
	 * The {@code listenerContainer} options should be specified
	 * on the provided {@link DirectMessageListenerContainer} using
	 * {@link AmqpInboundGatewayDMLCSpec#configureContainer(java.util.function.Consumer)}.
	 * @param listenerContainer the listenerContainer
	 * @return the AmqpInboundGatewayDMLCSpec.
	 */
	public static AmqpInboundGatewayDMLCSpec inboundGateway(DirectMessageListenerContainer listenerContainer) {
		return new AmqpInboundGatewayDMLCSpec(listenerContainer);
	}

	/**
	 * Create an initial {@link AmqpInboundGatewayDMLCSpec}
	 * with provided {@link DirectMessageListenerContainer}.
	 * Note: only endpoint options are available from spec.
	 * The {@code listenerContainer} options should be specified
	 * on the provided {@link DirectMessageListenerContainer} using
	 * {@link AmqpInboundGatewayDMLCSpec#configureContainer(java.util.function.Consumer)}.
	 * @param listenerContainer the listenerContainer
	 * @param amqpTemplate the {@link AmqpTemplate} to use.
	 * @return the AmqpInboundGatewayDMLCSpec.
	 */
	public static AmqpInboundGatewayDMLCSpec inboundGateway(DirectMessageListenerContainer listenerContainer,
			AmqpTemplate amqpTemplate) {

		return new AmqpInboundGatewayDMLCSpec(listenerContainer, amqpTemplate);
	}

	/**
	 * Create an initial AmqpInboundPolledChannelAdapterSpec
	 * @param connectionFactory the connectionFactory.
	 * @param queue the queue.
	 * @return the AmqpInboundPolledChannelAdapterSpec.
	 * @since 5.0.1
	 */
	public static AmqpInboundPolledChannelAdapterSpec inboundPolledAdapter(ConnectionFactory connectionFactory,
			String queue) {

		return new AmqpInboundPolledChannelAdapterSpec(connectionFactory, queue);
	}

	/**
	 * Create an initial AmqpInboundPolledChannelAdapterSpec
	 * @param connectionFactory the connectionFactory.
	 * @param ackCallbackFactory the ackCallbackFactory
	 * @param queue the queue.
	 * @return the AmqpInboundPolledChannelAdapterSpec.
	 * @since 5.0.1
	 */
	public static AmqpInboundPolledChannelAdapterSpec inboundPolledAdapter(ConnectionFactory connectionFactory,
			AmqpAckCallbackFactory ackCallbackFactory, String queue) {

		return new AmqpInboundPolledChannelAdapterSpec(connectionFactory, ackCallbackFactory, queue);
	}

	/**
	 * Create an initial AmqpInboundChannelAdapterSpec using a
	 * {@link SimpleMessageListenerContainer}.
	 * @param connectionFactory the connectionFactory.
	 * @param queueNames the queueNames.
	 * @return the AmqpInboundChannelAdapterSpec.
	 */
	public static AmqpInboundChannelAdapterSMLCSpec inboundAdapter(ConnectionFactory connectionFactory,
			String... queueNames) {

		SimpleMessageListenerContainer listenerContainer = new SimpleMessageListenerContainer(connectionFactory);
		listenerContainer.setQueueNames(queueNames);
		return new AmqpInboundChannelAdapterSMLCSpec(listenerContainer);
	}

	/**
	 * Create an initial AmqpInboundChannelAdapterSpec using a
	 * {@link SimpleMessageListenerContainer}.
	 * @param connectionFactory the connectionFactory.
	 * @param queues the queues.
	 * @return the AmqpInboundChannelAdapterSpec.
	 */
	public static AmqpInboundChannelAdapterSMLCSpec inboundAdapter(ConnectionFactory connectionFactory,
			Queue... queues) {

		SimpleMessageListenerContainer listenerContainer = new SimpleMessageListenerContainer(connectionFactory);
		listenerContainer.setQueues(queues);
		return new AmqpInboundChannelAdapterSMLCSpec(listenerContainer);
	}

	/**
	 * Create an initial {@link AmqpInboundGatewaySMLCSpec}
	 * with provided {@link SimpleMessageListenerContainer}.
	 * Note: only endpoint options are available from spec.
	 * The {@code listenerContainer} options should be specified
	 * on the provided {@link SimpleMessageListenerContainer} using
	 * {@link AmqpInboundGatewaySMLCSpec#configureContainer(java.util.function.Consumer)}.
	 * @param listenerContainer the listenerContainer
	 * @return the AmqpInboundGatewaySMLCSpec.
	 */
	public static AmqpInboundChannelAdapterSMLCSpec inboundAdapter(SimpleMessageListenerContainer listenerContainer) {
		return new AmqpInboundChannelAdapterSMLCSpec(listenerContainer);
	}

	/**
	 * Create an initial {@link AmqpInboundGatewayDMLCSpec}
	 * with provided {@link DirectMessageListenerContainer}.
	 * Note: only endpoint options are available from spec.
	 * The {@code listenerContainer} options should be specified
	 * on the provided {@link DirectMessageListenerContainer} using
	 * {@link AmqpInboundGatewaySMLCSpec#configureContainer(java.util.function.Consumer)}.
	 * @param listenerContainer the listenerContainer
	 * @return the AmqpInboundGatewaySMLCSpec.
	 */
	public static AmqpInboundChannelAdapterDMLCSpec inboundAdapter(DirectMessageListenerContainer listenerContainer) {
		return new AmqpInboundChannelAdapterDMLCSpec(listenerContainer);
	}

	/**
	 * Create an initial AmqpOutboundEndpointSpec (adapter).
	 * @param amqpTemplate the amqpTemplate.
	 * @return the AmqpOutboundEndpointSpec.
	 */
	public static AmqpOutboundChannelAdapterSpec outboundAdapter(AmqpTemplate amqpTemplate) {
		return new AmqpOutboundChannelAdapterSpec(amqpTemplate);
	}

	/**
	 * Create an initial AmqpOutboundEndpointSpec (gateway).
	 * @param amqpTemplate the amqpTemplate.
	 * @return the AmqpOutboundEndpointSpec.
	 */
	public static AmqpOutboundGatewaySpec outboundGateway(AmqpTemplate amqpTemplate) {
		return new AmqpOutboundGatewaySpec(amqpTemplate);
	}

	/**
	 * Create an initial AmqpAsyncOutboundGatewaySpec.
	 * @param asyncRabbitTemplate the {@link AsyncRabbitTemplate}.
	 * @return the AmqpOutboundEndpointSpec.
	 */
	public static AmqpAsyncOutboundGatewaySpec asyncOutboundGateway(AsyncRabbitTemplate asyncRabbitTemplate) {
		return new AmqpAsyncOutboundGatewaySpec(asyncRabbitTemplate);
	}

	/**
	 * Create an initial AmqpPollableMessageChannelSpec.
	 * @param connectionFactory the connectionFactory.
	 * @return the AmqpPollableMessageChannelSpec.
	 */
	public static AmqpPollableMessageChannelSpec<?, PollableAmqpChannel> pollableChannel(ConnectionFactory connectionFactory) {
		return pollableChannel(null, connectionFactory);
	}

	/**
	 * Create an initial AmqpPollableMessageChannelSpec.
	 * @param id the id.
	 * @param connectionFactory the connectionFactory.
	 * @return the AmqpPollableMessageChannelSpec.
	 */
	public static AmqpPollableMessageChannelSpec<?, PollableAmqpChannel> pollableChannel(@Nullable String id,
			ConnectionFactory connectionFactory) {

		AmqpPollableMessageChannelSpec<?, PollableAmqpChannel> spec = new AmqpPollableMessageChannelSpec<>(connectionFactory);
		return spec.id(id);
	}

	/**
	 * Create an initial AmqpMessageChannelSpec.
	 * @param connectionFactory the connectionFactory.
	 * @return the AmqpMessageChannelSpec.
	 */
	public static AmqpMessageChannelSpec<?, ?> channel(ConnectionFactory connectionFactory) {
		return channel(null, connectionFactory);
	}

	/**
	 * Create an initial AmqpMessageChannelSpec.
	 * @param id the id.
	 * @param connectionFactory the connectionFactory.
	 * @return the AmqpMessageChannelSpec.
	 */
	public static AmqpMessageChannelSpec<?, ?> channel(@Nullable String id, ConnectionFactory connectionFactory) {
		return new AmqpMessageChannelSpec<>(connectionFactory)
				.id(id);
	}

	/**
	 * Create an initial AmqpPublishSubscribeMessageChannelSpec.
	 * @param connectionFactory the connectionFactory.
	 * @return the AmqpPublishSubscribeMessageChannelSpec.
	 */
	public static AmqpPublishSubscribeMessageChannelSpec publishSubscribeChannel(ConnectionFactory connectionFactory) {
		return publishSubscribeChannel(null, connectionFactory);
	}

	/**
	 * Create an initial AmqpPublishSubscribeMessageChannelSpec.
	 * @param id the id.
	 * @param connectionFactory the connectionFactory.
	 * @return the AmqpPublishSubscribeMessageChannelSpec.
	 */
	public static AmqpPublishSubscribeMessageChannelSpec publishSubscribeChannel(@Nullable String id,
			ConnectionFactory connectionFactory) {

		return new AmqpPublishSubscribeMessageChannelSpec(connectionFactory).id(id);
	}

	private Amqp() {
	}

}
