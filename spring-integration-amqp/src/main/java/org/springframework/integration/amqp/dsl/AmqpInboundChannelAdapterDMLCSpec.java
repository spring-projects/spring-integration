/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
