/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.amqp.dsl;

import com.rabbitmq.stream.Codec;
import com.rabbitmq.stream.Environment;

import org.springframework.lang.Nullable;
import org.springframework.rabbit.stream.listener.StreamListenerContainer;
import org.springframework.rabbit.stream.producer.RabbitStreamTemplate;

/**
 * Factory class for RabbitMQ components.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 6.0
 *
 */
public final class RabbitStream {

	private RabbitStream() {
	}

	/**
	 * Create an initial {@link RabbitStreamInboundChannelAdapterSpec}
	 * with the provided {@link StreamListenerContainer}.
	 * The {@code streamName} or {@code superStream} must be provided after creation of this spec;
	 * or the {@code listenerContainer} options should be specified
	 * on the provided {@link StreamListenerContainer} using
	 * {@link RabbitStreamInboundChannelAdapterSpec#configureContainer(java.util.function.Consumer)}.
	 * @param listenerContainer the listenerContainer.
	 * @return the RabbitInboundChannelAdapterSLCSpec.
	 */
	public static RabbitStreamInboundChannelAdapterSpec inboundAdapter(StreamListenerContainer listenerContainer) {
		return new RabbitStreamInboundChannelAdapterSpec(listenerContainer);
	}

	/**
	 * Create an initial {@link RabbitStreamInboundChannelAdapterSpec}
	 * with the provided {@link Environment}.
	 * The {@code streamName} or {@code superStream} must be provided after creation of this spec;
	 * or the {@code listenerContainer} options should be specified
	 * on the provided {@link StreamListenerContainer} using
	 * {@link RabbitStreamInboundChannelAdapterSpec#configureContainer(java.util.function.Consumer)}.
	 * @param environment the environment.
	 * @return the RabbitInboundChannelAdapterSLCSpec.
	 */
	public static RabbitStreamInboundChannelAdapterSpec inboundAdapter(Environment environment) {
		return inboundAdapter(environment, null);
	}

	/**
	 * Create an initial {@link RabbitStreamInboundChannelAdapterSpec}
	 * with the provided {@link Environment}.
	 * The {@code streamName} or {@code superStream} must be provided after creation of this spec;
	 * or the {@code listenerContainer} options should be specified
	 * on the provided {@link StreamListenerContainer} using
	 * {@link RabbitStreamInboundChannelAdapterSpec#configureContainer(java.util.function.Consumer)}.
	 * @param environment the environment.
	 * @param codec the codec.
	 * @return the RabbitInboundChannelAdapterSLCSpec.
	 */
	public static RabbitStreamInboundChannelAdapterSpec inboundAdapter(Environment environment, @Nullable Codec codec) {
		return new RabbitStreamInboundChannelAdapterSpec(environment, codec);
	}

	/**
	 * Create an initial {@link RabbitStreamMessageHandlerSpec} (adapter).
	 * @param environment the environment.
	 * @param streamName the name of stream to produce.
	 * @return the RabbitStreamMessageHandlerSpec.
	 * @since 6.1
	 */
	public static RabbitStreamMessageHandlerSpec outboundStreamAdapter(Environment environment, String streamName) {
		return outboundStreamAdapter(new RabbitStreamTemplate(environment, streamName));
	}

	/**
	 * Create an initial {@link RabbitStreamMessageHandlerSpec} (adapter).
	 * @param template the amqpTemplate.
	 * @return the RabbitStreamMessageHandlerSpec.
	 */
	public static RabbitStreamMessageHandlerSpec outboundStreamAdapter(RabbitStreamTemplate template) {
		return new RabbitStreamMessageHandlerSpec(template);
	}

}
