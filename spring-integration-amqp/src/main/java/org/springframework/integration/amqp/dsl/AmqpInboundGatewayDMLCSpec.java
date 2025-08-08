/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.amqp.dsl;

import java.util.function.Consumer;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.listener.DirectMessageListenerContainer;

/**
 * Spec for a gateway with a {@link DirectMessageListenerContainer}.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0
 *
 */
public class AmqpInboundGatewayDMLCSpec
		extends AmqpInboundGatewaySpec<AmqpInboundGatewayDMLCSpec, DirectMessageListenerContainer> {

	protected AmqpInboundGatewayDMLCSpec(DirectMessageListenerContainer listenerContainer, AmqpTemplate amqpTemplate) {
		super(new DirectMessageListenerContainerSpec(listenerContainer), amqpTemplate);
	}

	protected AmqpInboundGatewayDMLCSpec(DirectMessageListenerContainer listenerContainer) {
		super(new DirectMessageListenerContainerSpec(listenerContainer));
	}

	public AmqpInboundGatewayDMLCSpec configureContainer(Consumer<DirectMessageListenerContainerSpec> configurer) {
		configurer.accept((DirectMessageListenerContainerSpec) this.listenerContainerSpec);
		return this;
	}

}
