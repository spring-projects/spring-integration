/*
 * Copyright 2016-2020 the original author or authors.
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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.SmartLifecycle;
import org.springframework.messaging.MessageChannel;

/**
 * The standard implementation of the {@link IntegrationFlow} interface instantiated
 * by the Framework. Represents a logical container for the components configured
 * for the integration flow. It can be treated as a single component, especially
 * when declaring dynamically, using the
 * {@link org.springframework.integration.dsl.context.IntegrationFlowContext}.
 * <p>
 * Being the logical container for the target integration components, this class controls
 * the lifecycle of all those components, when its {@code start()} and {@code stop()} are
 * invoked.
 * <p>
 * This component is never {@code autoStartup}, because all the components are
 * registered as beans in the application context and their initial start up phase is
 * controlled from the lifecycle processor automatically.
 * <p>
 * However, when we register an {@link IntegrationFlow} dynamically using the
 * {@link org.springframework.integration.dsl.context.IntegrationFlowContext} API,
 * the lifecycle processor from the application context is not involved;
 * therefore we should control the lifecycle of the beans manually, or rely on the
 * {@link org.springframework.integration.dsl.context.IntegrationFlowContext} API.
 * Its created registration <b>is</b> {@code autoStartup} by default and
 * starts the flow when it is registered. If you disable the registration's auto-
 * startup behavior, you are responsible for starting the flow or its component
 * beans.
 * <p>
 * This component doesn't track its {@code running} state during {@code stop()} action
 * and delegates directly to stop the registered components, to avoid dangling processes
 * after a registered {@link IntegrationFlow} is removed from the flow context.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 *
 * @see IntegrationFlows
 * @see org.springframework.integration.dsl.context.IntegrationFlowBeanPostProcessor
 * @see org.springframework.integration.dsl.context.IntegrationFlowContext
 * @see SmartLifecycle
 */
public class StandardIntegrationFlow implements IntegrationFlow, SmartLifecycle {

	private final Map<Object, String> integrationComponents;

	private MessageChannel inputChannel;

	private boolean running;

	StandardIntegrationFlow(Map<Object, String> integrationComponents) {
		this.integrationComponents = new LinkedHashMap<>(integrationComponents);
	}

	@Override
	public void configure(IntegrationFlowDefinition<?> flow) {
		throw new UnsupportedOperationException();
	}

	@Override
	public MessageChannel getInputChannel() {
		if (this.inputChannel == null) {
			this.inputChannel =
					this.integrationComponents.keySet()
							.stream()
							.filter(MessageChannel.class::isInstance)
							.map(MessageChannel.class::cast)
							.findFirst()
							.orElseThrow(() -> new IllegalStateException("The 'IntegrationFlow' [" + this + "] " +
									"doesn't start with 'MessageChannel' for direct message sending."));
		}

		return this.inputChannel;
	}

	public void setIntegrationComponents(Map<Object, String> integrationComponents) {
		this.integrationComponents.clear();
		this.integrationComponents.putAll(integrationComponents);
	}

	public Map<Object, String> getIntegrationComponents() {
		return Collections.unmodifiableMap(this.integrationComponents);
	}

	@Override
	public void start() {
		if (!this.running) {
			List<Object> components = new LinkedList<>(this.integrationComponents.keySet());
			ListIterator<Object> iterator = components.listIterator(this.integrationComponents.size());
			while (iterator.hasPrevious()) {
				Object component = iterator.previous();
				if (component instanceof SmartLifecycle) {
					((SmartLifecycle) component).start();
				}
			}
			this.running = true;
		}
	}

	@Override
	public void stop(Runnable callback) {
		AggregatingCallback aggregatingCallback = new AggregatingCallback(this.integrationComponents.size(), callback);
		for (Object component : this.integrationComponents.keySet()) {
			if (component instanceof SmartLifecycle) {
				SmartLifecycle lifecycle = (SmartLifecycle) component;
				if (lifecycle.isRunning()) {
					lifecycle.stop(aggregatingCallback);
					continue;
				}
			}
			aggregatingCallback.run();
		}
		this.running = false;
	}

	@Override
	public void stop() {
		for (Object component : this.integrationComponents.keySet()) {
			if (component instanceof SmartLifecycle) {
				SmartLifecycle lifecycle = (SmartLifecycle) component;
				if (lifecycle.isRunning()) {
					lifecycle.stop();
				}
			}
		}
		this.running = false;
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	public boolean isAutoStartup() {
		return false;
	}

	@Override
	public int getPhase() {
		return 0;
	}

	@Override
	public String toString() {
		return "StandardIntegrationFlow{integrationComponents=" + this.integrationComponents + '}';
	}

	private static final class AggregatingCallback implements Runnable {

		private final AtomicInteger count;

		private final Runnable finishCallback;

		AggregatingCallback(int count, Runnable finishCallback) {
			this.count = new AtomicInteger(count);
			this.finishCallback = finishCallback;
		}

		@Override
		public void run() {
			if (this.count.decrementAndGet() <= 0) {
				this.finishCallback.run();
			}
		}

	}

}
