/*
 * Copyright 2016-2024 the original author or authors.
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

package org.springframework.integration.nats.dsl;

import java.util.Collections;
import java.util.Map;

import org.springframework.integration.dsl.ComponentsRegistration;
import org.springframework.integration.dsl.MessageProducerSpec;
import org.springframework.integration.nats.AbstractNatsMessageListenerContainer;
import org.springframework.integration.nats.NatsMessageDrivenChannelAdapter;
import org.springframework.integration.nats.converter.MessageConverter;
import org.springframework.lang.NonNull;

/**
 * A {@link MessageProducerSpec} for {@link NatsMessageDrivenChannelAdapter}s.
 *
 * @param <S> the target {@link NatsMessageDrivenChannelAdapterSpec} implementation type.
 *
 * @author Viktor Rohlenko
 * @author Vennila Pazhamalai
 * @author Vivek Duraisamy
 * @since 6.4.x
 *
 * @see <a
 * href="https://rohlenko.github.io/spring-integration-nats-site/gws-spring-integration-nats/index.html#stakeholders">See
 * all stakeholders and contact</a>
 */
public class NatsMessageDrivenChannelAdapterSpec<S extends NatsMessageDrivenChannelAdapterSpec<S>>
		extends MessageProducerSpec<S, NatsMessageDrivenChannelAdapter>
		implements ComponentsRegistration {

	private final AbstractNatsMessageListenerContainer container;

	public NatsMessageDrivenChannelAdapterSpec(
			@NonNull final AbstractNatsMessageListenerContainer natsMessageListenerContainer) {
		super(new NatsMessageDrivenChannelAdapter(natsMessageListenerContainer));
		this.container = natsMessageListenerContainer;
	}

	public NatsMessageDrivenChannelAdapterSpec(
			@NonNull final AbstractNatsMessageListenerContainer natsMessageListenerContainer,
			@NonNull final MessageConverter<?> messageConverter) {
		super(new NatsMessageDrivenChannelAdapter(natsMessageListenerContainer, messageConverter));
		this.container = natsMessageListenerContainer;
	}

	@Override
	public Map<Object, String> getComponentsToRegister() {
		return Collections.singletonMap(
				this.container, getId() == null ? null : getId() + ".container");
	}

	@Override
	public S id(final String id) {
		this.target.setBeanName(id);
		return super.id(id);
	}

	public static class NatsMessageDrivenChannelAdpaterListenerContainer
			extends NatsMessageDrivenChannelAdapterSpec<
			NatsMessageDrivenChannelAdpaterListenerContainer> {

		private final NatsMessageListenerContainerSpec spec;

		public NatsMessageDrivenChannelAdpaterListenerContainer(
				final NatsMessageListenerContainerSpec spec) {
			super(spec.getObject());
			this.spec = spec;
		}

		public NatsMessageDrivenChannelAdpaterListenerContainer(
				final NatsMessageListenerContainerSpec spec, final MessageConverter messageConverter) {
			super(spec.getObject(), messageConverter);
			this.spec = spec;
		}

		@Override
		public Map<Object, String> getComponentsToRegister() {
			return Collections.singletonMap(this.spec.getObject(), this.spec.getId());
		}
	}
}
