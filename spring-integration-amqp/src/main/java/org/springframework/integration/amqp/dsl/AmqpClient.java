/*
 * Copyright 2025-present the original author or authors.
 */

package org.springframework.integration.amqp.dsl;

import org.springframework.amqp.core.AsyncAmqpTemplate;
import org.springframework.amqp.rabbitmq.client.AmqpConnectionFactory;

/**
 * Factory class for AMQP 1.0 components.
 *
 * @author Artem Bilan
 *
 * @since 7.0
 */
public final class AmqpClient {

	/**
	 * Create an initial {@link AmqpClientInboundGatewaySpec}.
	 * @param connectionFactory the connectionFactory.
	 * @param queueNames the queueNames.
	 * @return the AmqpClientInboundGatewaySpec.
	 */
	public static AmqpClientInboundGatewaySpec inboundGateway(AmqpConnectionFactory connectionFactory,
			String... queueNames) {

		return new AmqpClientInboundGatewaySpec(connectionFactory, queueNames);
	}

	/**
	 * Create an initial AmqpClientInboundChannelAdapterSpec.
	 * @param connectionFactory the connectionFactory.
	 * @param queueNames the queues to consume from.
	 * @return the AmqpClientInboundChannelAdapterSpec.
	 */
	public static AmqpClientInboundChannelAdapterSpec inboundChannelAdapter(AmqpConnectionFactory connectionFactory,
			String... queueNames) {

		return new AmqpClientInboundChannelAdapterSpec(connectionFactory, queueNames);
	}

	/**
	 * Create an initial AmqpClientMessageHandlerSpec in an outbound channel adapter mode.
	 * @param amqpTemplate the amqpTemplate.
	 * @return the AmqpClientMessageHandlerSpec.
	 */
	public static AmqpClientMessageHandlerSpec outboundAdapter(AsyncAmqpTemplate amqpTemplate) {
		return new AmqpClientMessageHandlerSpec(amqpTemplate);
	}

	/**
	 * Create an initial AmqpClientMessageHandlerSpec in a gateway mode.
	 * @param amqpTemplate the amqpTemplate.
	 * @return the AmqpClientMessageHandlerSpec.
	 */
	public static AmqpClientMessageHandlerSpec outboundGateway(AsyncAmqpTemplate amqpTemplate) {
		AmqpClientMessageHandlerSpec amqpClientMessageHandlerSpec = new AmqpClientMessageHandlerSpec(amqpTemplate);
		amqpClientMessageHandlerSpec.getObject().setRequiresReply(true);
		return amqpClientMessageHandlerSpec;
	}

	private AmqpClient() {
	}

}
