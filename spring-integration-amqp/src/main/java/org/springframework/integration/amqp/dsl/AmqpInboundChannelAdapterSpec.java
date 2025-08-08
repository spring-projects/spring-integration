/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.amqp.dsl;

import java.util.Collections;
import java.util.Map;

import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.integration.amqp.inbound.AmqpInboundChannelAdapter;
import org.springframework.integration.dsl.ComponentsRegistration;

/**
 * A {@link org.springframework.integration.dsl.MessageProducerSpec} for
 * {@link AmqpInboundChannelAdapter}s.
 *
 * @param <S> the spec type.
 * @param <C> the container type.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public abstract class AmqpInboundChannelAdapterSpec
		<S extends AmqpInboundChannelAdapterSpec<S, C>, C extends MessageListenerContainer>
		extends AmqpBaseInboundChannelAdapterSpec<S>
		implements ComponentsRegistration {

	protected final MessageListenerContainerSpec<?, C> listenerContainerSpec; // NOSONAR final

	protected AmqpInboundChannelAdapterSpec(MessageListenerContainerSpec<?, C> listenerContainerSpec) {
		super(new AmqpInboundChannelAdapter(listenerContainerSpec.getObject()));
		this.listenerContainerSpec = listenerContainerSpec;
	}

	@Override
	public Map<Object, String> getComponentsToRegister() {
		return Collections.singletonMap(this.listenerContainerSpec.getObject(), this.listenerContainerSpec.getId());
	}

}
