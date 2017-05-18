/*
 * Copyright 2014-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.amqp.dsl;

import java.util.Collection;
import java.util.Collections;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.integration.amqp.inbound.AmqpInboundGateway;
import org.springframework.integration.dsl.ComponentsRegistration;

/**
 * An {@link AmqpBaseInboundGatewaySpec} implementation for a {@link AmqpInboundGateway}.
 * Allows to provide {@link AbstractMessageListenerContainer} options.
 *
 * @author Artem Bilan
 * @since 5.0
 */
public abstract class AmqpInboundGatewaySpec
				<S extends AmqpInboundGatewaySpec<S, C>, C extends AbstractMessageListenerContainer>
		extends AmqpBaseInboundGatewaySpec<S> implements ComponentsRegistration {

	protected final C listenerContainer;

	AmqpInboundGatewaySpec(C listenerContainer) {
		super(new AmqpInboundGateway(listenerContainer));
		this.listenerContainer = listenerContainer;
	}

	/**
	 * Instantiate {@link AmqpInboundGateway} based on the provided {@link AbstractMessageListenerContainer}
	 * and {@link AmqpTemplate}.
	 * @param listenerContainer the {@link SimpleMessageListenerContainer} to use.
	 * @param amqpTemplate the {@link AmqpTemplate} to use.
	 */
	AmqpInboundGatewaySpec(C listenerContainer, AmqpTemplate amqpTemplate) {
		super(new AmqpInboundGateway(listenerContainer, amqpTemplate));
		this.listenerContainer = listenerContainer;
	}

	@Override
	public Collection<Object> getComponentsToRegister() {
		return Collections.<Object>singleton(this.listenerContainer);
	}

}
