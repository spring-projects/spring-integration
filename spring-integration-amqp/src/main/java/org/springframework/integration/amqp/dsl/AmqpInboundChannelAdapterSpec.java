/*
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.amqp.dsl;

import java.util.Collections;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.amqp.core.MessageListenerContainer;
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
	public Map<Object, @Nullable String> getComponentsToRegister() {
		return Collections.<Object, @Nullable String>singletonMap(
				this.listenerContainerSpec.getObject(), this.listenerContainerSpec.getId());
	}

}
