/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.amqp.dsl;

import java.util.function.Consumer;

import org.springframework.amqp.rabbit.listener.DirectMessageListenerContainer;

/**
 * Spec for an inbound channel adapter with a {@link DirectMessageListenerContainer}.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0
 *
 */
public class AmqpInboundChannelAdapterDMLCSpec
		extends AmqpInboundChannelAdapterSpec<AmqpInboundChannelAdapterDMLCSpec, DirectMessageListenerContainer> {

	protected AmqpInboundChannelAdapterDMLCSpec(DirectMessageListenerContainer listenerContainer) {
		super(new DirectMessageListenerContainerSpec(listenerContainer));
	}

	public AmqpInboundChannelAdapterDMLCSpec configureContainer(
			Consumer<DirectMessageListenerContainerSpec> configurer) {

		configurer.accept((DirectMessageListenerContainerSpec) this.listenerContainerSpec);
		return this;
	}

}
