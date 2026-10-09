/*
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.amqp.dsl;

import java.util.Collections;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.integration.amqp.inbound.AmqpInboundGateway;
import org.springframework.integration.dsl.ComponentsRegistration;

/**
 * An {@link AmqpBaseInboundGatewaySpec} implementation for a {@link AmqpInboundGateway}.
 * Allows to provide {@link AbstractMessageListenerContainer} options.
 *
 * @param <S> the spec type.
 * @param <C> the container type.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public abstract class AmqpInboundGatewaySpec
		<S extends AmqpInboundGatewaySpec<S, C>, C extends AbstractMessageListenerContainer>
		extends AmqpBaseInboundGatewaySpec<S>
		implements ComponentsRegistration {

	protected final AbstractMessageListenerContainerSpec<?, C> listenerContainerSpec; // NOSONAR final

	protected AmqpInboundGatewaySpec(AbstractMessageListenerContainerSpec<?, C> listenerContainerSpec) {
		super(new AmqpInboundGateway(listenerContainerSpec.getObject()));
		this.listenerContainerSpec = listenerContainerSpec;
	}

	/**
	 * Instantiate {@link AmqpInboundGateway} based on the provided {@link AbstractMessageListenerContainer}
	 * and {@link AmqpTemplate}.
	 * @param listenerContainerSpec the {@link AbstractMessageListenerContainerSpec} to use.
	 * @param amqpTemplate the {@link AmqpTemplate} to use.
	 */
	AmqpInboundGatewaySpec(AbstractMessageListenerContainerSpec<?, C> listenerContainerSpec,
			AmqpTemplate amqpTemplate) {

		super(new AmqpInboundGateway((MessageListenerContainer) listenerContainerSpec.getObject(), amqpTemplate));
		this.listenerContainerSpec = listenerContainerSpec;
	}

	@Override
	public Map<Object, @Nullable String> getComponentsToRegister() {
		return Collections.<Object, @Nullable String>singletonMap(
				this.listenerContainerSpec.getObject(), this.listenerContainerSpec.getId());
	}

}
