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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;

import org.springframework.context.SmartLifecycle;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

/**
 * The base {@code Adapter} class for the {@link IntegrationFlow} abstraction.
 * Requires the implementation for the {@link #buildFlow()} method to produce
 * {@link IntegrationFlowDefinition} using one of {@link #from} support methods.
 * <p>
 * Typically is used for target service implementation:
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
public abstract class IntegrationFlowAdapter implements IntegrationFlow, SmartLifecycle {

	private final AtomicBoolean running = new AtomicBoolean();

	private StandardIntegrationFlow targetIntegrationFlow;

	@Override
	public final void configure(IntegrationFlowDefinition<?> flow) {
		IntegrationFlowDefinition<?> targetFlow = buildFlow();
		Assert.state(targetFlow != null, "the 'buildFlow()' must not return null");
		flow.integrationComponents.clear();
		flow.integrationComponents.putAll(targetFlow.integrationComponents);
		this.targetIntegrationFlow = flow.get();
	}

	@Override
	public MessageChannel getInputChannel() {
		assertTargetIntegrationFlow();
		return this.targetIntegrationFlow.getInputChannel();
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
		return IntegrationFlows.from(messageChannelName);
	}

	protected IntegrationFlowDefinition<?> from(MessageChannel messageChannel) {
		return IntegrationFlows.from(messageChannel);
	}

	protected IntegrationFlowDefinition<?> from(String messageChannelName, boolean fixedSubscriber) {
		return IntegrationFlows.from(messageChannelName, fixedSubscriber);
	}

	protected IntegrationFlowDefinition<?> from(MessageSourceSpec<?, ? extends MessageSource<?>> messageSourceSpec,
			Consumer<SourcePollingChannelAdapterSpec> endpointConfigurer) {
		return IntegrationFlows.from(messageSourceSpec, endpointConfigurer);
	}

	protected IntegrationFlowDefinition<?> from(MessageSource<?> messageSource,
			Consumer<SourcePollingChannelAdapterSpec> endpointConfigurer) {
		return IntegrationFlows.from(messageSource, endpointConfigurer);
	}

	protected IntegrationFlowDefinition<?> from(MessageProducerSupport messageProducer) {
		return IntegrationFlows.from(messageProducer);
	}

	protected IntegrationFlowDefinition<?> from(MessageSource<?> messageSource) {
		return IntegrationFlows.from(messageSource);
	}

	protected IntegrationFlowDefinition<?> from(MessagingGatewaySupport inboundGateway) {
		return IntegrationFlows.from(inboundGateway);
	}

	protected IntegrationFlowDefinition<?> from(MessageChannelSpec<?, ?> messageChannelSpec) {
		return IntegrationFlows.from(messageChannelSpec);
	}

	protected IntegrationFlowDefinition<?> from(MessageProducerSpec<?, ?> messageProducerSpec) {
		return IntegrationFlows.from(messageProducerSpec);
	}

	protected IntegrationFlowDefinition<?> from(MessageSourceSpec<?, ? extends MessageSource<?>> messageSourceSpec) {
		return IntegrationFlows.from(messageSourceSpec);
	}

	protected IntegrationFlowDefinition<?> from(MessagingGatewaySpec<?, ?> inboundGatewaySpec) {
		return IntegrationFlows.from(inboundGatewaySpec);
	}

	protected IntegrationFlowBuilder from(Object service, String methodName) {
		return IntegrationFlows.from(service, methodName);
	}

	protected IntegrationFlowBuilder from(Object service, String methodName,
			Consumer<SourcePollingChannelAdapterSpec> endpointConfigurer) {
		return IntegrationFlows.from(service, methodName, endpointConfigurer);
	}

	protected <T> IntegrationFlowBuilder from(Supplier<T> messageSource) {
		return IntegrationFlows.from(messageSource);
	}

	protected <T> IntegrationFlowBuilder from(Supplier<T> messageSource,
			Consumer<SourcePollingChannelAdapterSpec> endpointConfigurer) {
		return IntegrationFlows.from(messageSource, endpointConfigurer);
	}

	protected IntegrationFlowBuilder from(Class<?> serviceInterface) {
		return IntegrationFlows.from(serviceInterface);
	}

	protected IntegrationFlowBuilder from(Class<?> serviceInterface, String beanName) {
		return IntegrationFlows.from(serviceInterface, beanName);
	}

	protected IntegrationFlowBuilder from(Publisher<Message<?>> publisher) {
		return IntegrationFlows.from(publisher);
	}

	protected abstract IntegrationFlowDefinition<?> buildFlow();

}
