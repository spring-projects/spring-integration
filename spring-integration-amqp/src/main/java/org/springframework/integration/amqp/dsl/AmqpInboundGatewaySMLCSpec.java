/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.amqp.dsl;

import java.util.function.Consumer;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;

/**
 * Spec for a gateway with a {@link SimpleMessageListenerContainer}.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0
 *
 */
public class AmqpInboundGatewaySMLCSpec
		extends AmqpInboundGatewaySpec<AmqpInboundGatewaySMLCSpec, SimpleMessageListenerContainer> {

	protected AmqpInboundGatewaySMLCSpec(SimpleMessageListenerContainer listenerContainer, AmqpTemplate amqpTemplate) {
		super(new SimpleMessageListenerContainerSpec(listenerContainer), amqpTemplate);
	}

	protected AmqpInboundGatewaySMLCSpec(SimpleMessageListenerContainer listenerContainer) {
		super(new SimpleMessageListenerContainerSpec(listenerContainer));
	}

	public AmqpInboundGatewaySMLCSpec configureContainer(Consumer<SimpleMessageListenerContainerSpec> configurer) {
		configurer.accept((SimpleMessageListenerContainerSpec) this.listenerContainerSpec);
		return this;
	}

}
