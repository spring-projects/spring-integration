/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.amqp.dsl;

import java.util.function.Consumer;

import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.integration.amqp.inbound.AmqpInboundChannelAdapter.BatchMode;

/**
 * Spec for an inbound channel adapter with a {@link SimpleMessageListenerContainer}.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0
 *
 */
public class AmqpInboundChannelAdapterSMLCSpec
		extends AmqpInboundChannelAdapterSpec<AmqpInboundChannelAdapterSMLCSpec, SimpleMessageListenerContainer> {

	protected AmqpInboundChannelAdapterSMLCSpec(SimpleMessageListenerContainer listenerContainer) {
		super(new SimpleMessageListenerContainerSpec(listenerContainer));
	}

	public AmqpInboundChannelAdapterSMLCSpec configureContainer(
			Consumer<SimpleMessageListenerContainerSpec> configurer) {

		configurer.accept((SimpleMessageListenerContainerSpec) this.listenerContainerSpec);
		return this;
	}

	/**
	 * Set the {@link BatchMode} to use when the container is configured to support
	 * batching consumed records.
	 * @param batchMode the batch mode.
	 * @return the spec.
	 * @since 5.3
	 */
	public AmqpInboundChannelAdapterSMLCSpec batchMode(BatchMode batchMode) {
		this.target.setBatchMode(batchMode);
		return this;
	}

}
