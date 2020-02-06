/*
 * Copyright 2017-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
