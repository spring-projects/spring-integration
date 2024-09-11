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

package org.springframework.integration.dsl;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;

import org.springframework.integration.context.ComponentSourceAware;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.integration.support.management.ManageableSmartLifecycle;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

/**
 * The base {@code Adapter} class for the {@link IntegrationFlow} abstraction.
 * Requires the implementation for the {@link #buildFlow()} method to produce
 * {@link IntegrationFlowDefinition} using one of {@link #from} support methods.
 * <p>
 * Typically, is used for target service implementation:
 * <pre class="code">
 *  &#64;Component
 *  public class MyFlowAdapter extends IntegrationFlowAdapter {
 *
 *     &#64;Autowired
 *     private ConnectionFactory rabbitConnectionFactory;
 *
 *     &#64;Override
 *     protected IntegrationFlowDefinition&lt;?&gt; buildFlow() {
 *          return from(Amqp.inboundAdapter(this.rabbitConnectionFactory, "myQueue"))
 *                   .&lt;String, String&gt;transform(String::toLowerCase)
 *                   .channel(c -&gt; c.queue("myFlowAdapterOutput"));
 *     }
 *
 * }
 * </pre>
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public abstract class IntegrationFlowAdapter
		implements IntegrationFlow, ManageableSmartLifecycle, ComponentSourceAware {

	private final AtomicBoolean running = new AtomicBoolean();

	private StandardIntegrationFlow targetIntegrationFlow;

	private String beanName;

	private Object beanSource;

	private String beanDescription;

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	@Nullable
	@Override
	public String getBeanName() {
		return this.beanName;
	}

	@Override
	public void setComponentSource(Object source) {
		this.beanSource = source;
	}

	@Nullable
	@Override
	public Object getComponentSource() {
		return this.beanSource;
	}

	@Override
	public void setComponentDescription(String description) {
		this.beanDescription = description;
	}

	@Nullable
	@Override
	public String getComponentDescription() {
		return this.beanDescription;
	}

	@Override
	public final void configure(IntegrationFlowDefinition<?> flow) {
		IntegrationFlowDefinition<?> targetFlow = buildFlow();
		flow.integrationComponents.clear();
		flow.integrationComponents.putAll(targetFlow.integrationComponents);
		this.targetIntegrationFlow = flow.get();
		if (this.beanSource != null) {
			this.targetIntegrationFlow.setComponentSource(this.beanSource);
		}
		if (this.beanDescription != null) {
			this.targetIntegrationFlow.setComponentDescription(this.beanDescription);
		}
	}

	@Nullable
	@Override
	public MessageChannel getInputChannel() {
		assertTargetIntegrationFlow();
		return this.targetIntegrationFlow.getInputChannel();
	}

	@Override
	public Map<Object, String> getIntegrationComponents() {
		return this.targetIntegrationFlow.getIntegrationComponents();
	}

	@Override
	public void start() {
		assertTargetIntegrationFlow();
		if (!this.running.getAndSet(true)) {
			this.targetIntegrationFlow.start();
		}
	}

	private void assertTargetIntegrationFlow() {
		Assert.state(this.targetIntegrationFlow != null,
				this + " hasn't been initialized properly via BeanFactory.\n"
						+ "Missed @EnableIntegration ?");
	}

	@Override
	public void stop(Runnable callback) {
		assertTargetIntegrationFlow();
		if (this.running.getAndSet(false)) {
			this.targetIntegrationFlow.stop(callback);
		}
		else {
			callback.run();
		}
	}

	@Override
	public void stop() {
		assertTargetIntegrationFlow();
		if (this.running.getAndSet(false)) {
			this.targetIntegrationFlow.stop();
		}
	}

	@Override
	public boolean isRunning() {
		return this.running.get();
	}

	@Override
	public boolean isAutoStartup() {
		return false;
	}

	@Override
	public int getPhase() {
		return 0;
	}

	protected IntegrationFlowDefinition<?> from(String messageChannelName) {
		return IntegrationFlow.from(messageChannelName);
	}

	protected IntegrationFlowDefinition<?> from(MessageChannel messageChannel) {
		return IntegrationFlow.from(messageChannel);
	}

	protected IntegrationFlowDefinition<?> from(String messageChannelName, boolean fixedSubscriber) {
		return IntegrationFlow.from(messageChannelName, fixedSubscriber);
	}

	protected IntegrationFlowDefinition<?> from(MessageSourceSpec<?, ? extends MessageSource<?>> messageSourceSpec,
			Consumer<SourcePollingChannelAdapterSpec> endpointConfigurer) {

		return IntegrationFlow.from(messageSourceSpec, endpointConfigurer);
	}

	protected IntegrationFlowDefinition<?> from(MessageSource<?> messageSource,
			Consumer<SourcePollingChannelAdapterSpec> endpointConfigurer) {

		return IntegrationFlow.from(messageSource, endpointConfigurer);
	}

	protected IntegrationFlowDefinition<?> from(MessageProducerSupport messageProducer) {
		return IntegrationFlow.from(messageProducer);
	}

	protected IntegrationFlowDefinition<?> from(MessageSource<?> messageSource) {
		return IntegrationFlow.from(messageSource);
	}

	protected IntegrationFlowDefinition<?> from(MessagingGatewaySupport inboundGateway) {
		return IntegrationFlow.from(inboundGateway);
	}

	protected IntegrationFlowDefinition<?> from(MessageChannelSpec<?, ?> messageChannelSpec) {
		return IntegrationFlow.from(messageChannelSpec);
	}

	protected IntegrationFlowDefinition<?> from(MessageProducerSpec<?, ?> messageProducerSpec) {
		return IntegrationFlow.from(messageProducerSpec);
	}

	protected IntegrationFlowDefinition<?> from(MessageSourceSpec<?, ? extends MessageSource<?>> messageSourceSpec) {
		return IntegrationFlow.from(messageSourceSpec);
	}

	protected IntegrationFlowDefinition<?> from(MessagingGatewaySpec<?, ?> inboundGatewaySpec) {
		return IntegrationFlow.from(inboundGatewaySpec);
	}

	protected <T> IntegrationFlowBuilder fromSupplier(Supplier<T> messageSource) {
		return IntegrationFlow.fromSupplier(messageSource);
	}

	protected <T> IntegrationFlowBuilder fromSupplier(Supplier<T> messageSource,
			Consumer<SourcePollingChannelAdapterSpec> endpointConfigurer) {

		return IntegrationFlow.fromSupplier(messageSource, endpointConfigurer);
	}

	protected IntegrationFlowBuilder from(Class<?> serviceInterface) {
		return IntegrationFlow.from(serviceInterface);
	}

	/**
	 * Start a flow from a proxy for the service interface.
	 * @param serviceInterface the service interface class.
	 * @param endpointConfigurer the {@link Consumer} to configure proxy bean for gateway.
	 * @return new {@link IntegrationFlowBuilder}.
	 * @since 5.2
	 */
	protected IntegrationFlowBuilder from(Class<?> serviceInterface,
			@Nullable Consumer<GatewayProxySpec> endpointConfigurer) {

		return IntegrationFlow.from(serviceInterface, endpointConfigurer);
	}

	protected IntegrationFlowBuilder from(Publisher<? extends Message<?>> publisher) {
		return IntegrationFlow.from(publisher);
	}

	protected abstract IntegrationFlowDefinition<?> buildFlow();

}
