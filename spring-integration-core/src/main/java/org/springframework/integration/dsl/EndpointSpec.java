/*
 * Copyright 2016-2018 the original author or authors.
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

package org.springframework.integration.dsl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.ResolvableType;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.endpoint.AbstractPollingEndpoint;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;

import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

/**
 * An {@link IntegrationComponentSpec} for endpoints.
 *
 * @param <S> the target {@link ConsumerEndpointSpec} implementation type.
 * @param <F> the target {@link BeanNameAware} implementation type.
 * @param <H> the target {@link MessageHandler} implementation type.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public abstract class EndpointSpec<S extends EndpointSpec<S, F, H>, F extends BeanNameAware, H>
		extends IntegrationComponentSpec<S, Tuple2<F, H>>
		implements ComponentsRegistration {

	protected final Map<Object, String> componentsToRegister = new LinkedHashMap<>();

	protected H handler;

	protected F endpointFactoryBean;

	@SuppressWarnings("unchecked")
	protected EndpointSpec(H handler) {
		try {
			Class<?> fClass = ResolvableType.forClass(this.getClass()).as(EndpointSpec.class).resolveGenerics()[1];
			this.endpointFactoryBean = (F) fClass.newInstance();
			this.handler = handler;
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public S id(String id) {
		this.endpointFactoryBean.setBeanName(id);
		return super.id(id);
	}

	/**
	 * @param pollers the pollers
	 * @return the endpoint spec.
	 * @see AbstractPollingEndpoint
	 * @see PollerFactory
	 */
	public S poller(Function<PollerFactory, PollerSpec> pollers) {
		return poller(pollers.apply(new PollerFactory()));
	}

	/**
	 * @param pollerMetadataSpec the pollerMetadataSpec
	 * @return the endpoint spec.
	 * @see AbstractPollingEndpoint
	 * @see PollerSpec
	 */
	public S poller(PollerSpec pollerMetadataSpec) {
		Map<Object, String> componentsToRegister = pollerMetadataSpec.getComponentsToRegister();
		if (componentsToRegister != null) {
			this.componentsToRegister.putAll(componentsToRegister);
		}
		return poller(pollerMetadataSpec.get());
	}

	/**
	 * @param pollerMetadata the pollerMetadata
	 * @return the endpoint spec.
	 * @see AbstractPollingEndpoint
	 */
	public abstract S poller(PollerMetadata pollerMetadata);

	/**
	 * @param phase the phase.
	 * @return the endpoint spec.
	 * @see SmartLifecycle
	 */
	public abstract S phase(int phase);

	/**
	 * @param autoStartup the autoStartup.
	 * @return the endpoint spec
	 * @see SmartLifecycle
	 */
	public abstract S autoStartup(boolean autoStartup);

	/**
	 * Specify the role for the endpoint.
	 * Such endpoints can be started/stopped as a group.
	 * @param role the role for this endpoint.
	 * @return the endpoint spec
	 * @see SmartLifecycle
	 * @see org.springframework.integration.support.SmartLifecycleRoleController
	 */
	public abstract S role(String role);

	@Override
	public Map<Object, String> getComponentsToRegister() {
		return this.componentsToRegister.isEmpty()
				? null
				: this.componentsToRegister;
	}

	@Override
	protected Tuple2<F, H> doGet() {
		return Tuples.of(this.endpointFactoryBean, this.handler);
	}

	protected void assertHandler() {
		Assert.state(this.handler != null, "'this.handler' must not be null.");
	}

	/**
	 * Try to get a {@link MessageChannel} as an input for the provided {@link IntegrationFlow}
	 * or create one and wrap the provided flow to a new one.
	 * @param subFlow the {@link IntegrationFlow} to extract input channel.
	 * @return the input channel of the flow of create one
	 * @since 5.0.4
	 */
	protected MessageChannel obtainInputChannelFromFlow(IntegrationFlow subFlow) {
		return obtainInputChannelFromFlow(subFlow, true);
	}

	/**
	 * Try to get a {@link MessageChannel} as an input for the provided {@link IntegrationFlow}
	 * or create one and wrap the provided flow to a new one.
	 * @param subFlow the {@link IntegrationFlow} to extract input channel.
	 * @param evaluateInternalBuilder true if an internal {@link IntegrationFlowDefinition} should be
	 * evaluated to an {@link IntegrationFlow} component or left as a builder in the {@link #componentsToRegister}
	 * for future use-case. For example the builder is used for router configurations to retain beans
	 * registration order for parent-child dependencies.
	 * @return the input channel of the flow of create one
	 * @since 5.0.4
	 */
	protected MessageChannel obtainInputChannelFromFlow(IntegrationFlow subFlow, boolean evaluateInternalBuilder) {
		Assert.notNull(subFlow, "'subFlow' must not be null");
		MessageChannel messageChannel = subFlow.getInputChannel();
		if (messageChannel == null) {
			messageChannel = new DirectChannel();
			IntegrationFlowDefinition<?> flowBuilder = IntegrationFlows.from(messageChannel);
			subFlow.configure(flowBuilder);
			this.componentsToRegister.put(evaluateInternalBuilder ? flowBuilder.get() : flowBuilder, null);
		}
		else {
			this.componentsToRegister.put(subFlow, null);
		}

		return messageChannel;
	}

}
