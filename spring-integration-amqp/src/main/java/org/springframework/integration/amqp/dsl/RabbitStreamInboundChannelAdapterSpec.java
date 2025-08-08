/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.amqp.dsl;

import java.util.function.Consumer;

import com.rabbitmq.stream.Codec;
import com.rabbitmq.stream.Environment;

import org.springframework.lang.Nullable;
import org.springframework.rabbit.stream.listener.StreamListenerContainer;

/**
 * Spec for an inbound channel adapter with a {@link StreamListenerContainer}.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 6.0
 *
 */
public class RabbitStreamInboundChannelAdapterSpec
		extends AmqpInboundChannelAdapterSpec<RabbitStreamInboundChannelAdapterSpec, StreamListenerContainer> {

	protected RabbitStreamInboundChannelAdapterSpec(StreamListenerContainer listenerContainer) {
		super(new RabbitStreamMessageListenerContainerSpec(listenerContainer));
	}

	protected RabbitStreamInboundChannelAdapterSpec(Environment environment, @Nullable Codec codec) {
		super(new RabbitStreamMessageListenerContainerSpec(environment, codec));
	}

	/**
	 * Configure a name for Rabbit stream to consume from.
	 * @param streamName the name of Rabbit stream.
	 * @return the spec
	 * @since 6.1
	 */
	public RabbitStreamInboundChannelAdapterSpec streamName(String streamName) {
		this.listenerContainerSpec.queueName(streamName);
		return this;
	}

	/**
	 * Configure a name for Rabbit super stream to consume from.
	 * @param superStream the name of Rabbit super stream.
	 * @param consumerName the logical name to enable offset tracking.
	 * @return the spec
	 * @since 6.1
	 */
	public RabbitStreamInboundChannelAdapterSpec superStream(String superStream, String consumerName) {
		return superStream(superStream, consumerName, 1);
	}

	/**
	 * Configure a name for Rabbit super stream to consume from.
	 * @param superStream the name of Rabbit super stream.
	 * @param consumerName the logical name to enable offset tracking.
	 * @param consumers the number of consumers.
	 * @return the spec
	 * @since 6.1
	 */
	public RabbitStreamInboundChannelAdapterSpec superStream(String superStream, String consumerName, int consumers) {
		((RabbitStreamMessageListenerContainerSpec) this.listenerContainerSpec)
				.superStream(superStream, consumerName, consumers);
		return this;
	}

	public RabbitStreamInboundChannelAdapterSpec configureContainer(
			Consumer<RabbitStreamMessageListenerContainerSpec> configurer) {

		configurer.accept((RabbitStreamMessageListenerContainerSpec) this.listenerContainerSpec);
		return this;
	}

}
