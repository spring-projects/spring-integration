/*
 * Copyright 2014-2023 the original author or authors.
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
