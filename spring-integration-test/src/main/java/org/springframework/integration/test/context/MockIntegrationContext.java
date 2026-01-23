/*
 * Copyright 2017-present the original author or authors.
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

package org.springframework.integration.test.context;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import reactor.core.publisher.Mono;

import org.springframework.beans.BeansException;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.Lifecycle;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.AbstractPollingEndpoint;
import org.springframework.integration.endpoint.IntegrationConsumer;
import org.springframework.integration.endpoint.ReactiveStreamsConsumer;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.test.mock.MockMessageHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.ReactiveMessageHandler;
import org.springframework.scheduling.Trigger;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

/**
 * A {@link BeanFactoryAware} component with an API to customize real beans
 * in the application context from test code.
 * <p>
 * The bean for this class is registered automatically via the {@link SpringIntegrationTest}
 * annotation and can be autowired into test class.
 *
 * @author Artem Bilan
 * @author Yicheng Feng
 * @author Alexander Hain
 * @author Glenn Renfro
 *
 * @since 5.0
 *
 * @see SpringIntegrationTest
 */
public class MockIntegrationContext implements BeanPostProcessor, SmartInitializingSingleton, BeanFactoryAware {

	private static final String HANDLER = "handler";

	private static final String REACTIVE_MESSAGE_HANDLER = "reactiveMessageHandler";

	/**
	 * The bean name for the mock integration context.
	 */
	public static final String MOCK_INTEGRATION_CONTEXT_BEAN_NAME = "mockIntegrationContext";

	private final MultiValueMap<String, Object> beans = new LinkedMultiValueMap<>();

	private final Set<AbstractEndpoint> autoStartupCandidates = new HashSet<>();

	@SuppressWarnings("NullAway.Init")
	private ConfigurableListableBeanFactory beanFactory;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.isAssignable(ConfigurableListableBeanFactory.class, beanFactory.getClass(),
				"a ConfigurableListableBeanFactory is required");
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof AbstractEndpoint endpoint && endpoint.isAutoStartup()) {
			addAutoStartupCandidates(endpoint);
		}
		return bean;
	}

	private void addAutoStartupCandidates(AbstractEndpoint endpoint) {
		if (this.autoStartupCandidates.add(endpoint)) {
			endpoint.setAutoStartup(false);
		}
	}

	@Override
	public void afterSingletonsInstantiated() {
		this.beanFactory.getBeansOfType(AbstractEndpoint.class)
				.values()
				.stream()
				.filter(AbstractEndpoint::isAutoStartup)
				.forEach(this::addAutoStartupCandidates);
	}

	Set<AbstractEndpoint> getAutoStartupCandidates() {
		return Collections.unmodifiableSet(this.autoStartupCandidates);
	}

	/**
	 * Reinstate the mocked beans after execution test to their real state.
	 * Typically, this method is used from JUnit clean up methods.
	 * @param beanNames the bean names to reset.
	 * If {@code null}, all the mocked beans are reset
	 */
	public void resetBeans(String... beanNames) {
		final Collection<String> names;
		if (!ObjectUtils.isEmpty(beanNames)) {
			names = Arrays.asList(beanNames);
		}
		else {
			names = null;
		}

		this.beans.entrySet()
				.stream()
				.filter((bean) -> names == null || names.contains(bean.getKey()))
				.forEach((bean) -> {
					Object endpoint = this.beanFactory.getBean(bean.getKey());
					bean.getValue()
							.forEach((value) -> resetBean(endpoint, value));
				});

		if (!ObjectUtils.isEmpty(beanNames)) {
			for (String name : beanNames) {
				this.beans.remove(name);
			}
		}
		else {
			this.beans.clear();
		}
	}

	private void resetBean(Object endpoint, Object component) {
		DirectFieldAccessor directFieldAccessor = new DirectFieldAccessor(endpoint);
		SmartLifecycle lifecycle = null;
		if (endpoint instanceof SmartLifecycle lifecycleEndpoint && lifecycleEndpoint.isRunning()) {
			lifecycle = lifecycleEndpoint;
			lifecycle.stop();
		}
		if (endpoint instanceof SourcePollingChannelAdapter && component instanceof MessageSource<?>) {
			directFieldAccessor.setPropertyValue("source", component);
		}
		else if (endpoint instanceof IntegrationConsumer && component instanceof MessageHandler) {
			directFieldAccessor.setPropertyValue(HANDLER, component);
		}
		else if (endpoint instanceof ReactiveStreamsConsumer && component instanceof ReactiveMessageHandler) {
			directFieldAccessor.setPropertyValue(REACTIVE_MESSAGE_HANDLER, component);
		}
		else if (component instanceof Trigger) {
			directFieldAccessor.setPropertyValue("trigger", component);
		}
		if (lifecycle != null && lifecycle.isAutoStartup()) {
			lifecycle.start();
		}
	}

	/**
	 * Replace the real {@link MessageSource} in the {@link SourcePollingChannelAdapter} bean
	 * with provided {@link MessageSource} instance.
	 * Can be a mock object.
	 * @param pollingAdapterId the endpoint bean name
	 * @param mockMessageSource the {@link MessageSource} to replace in the endpoint bean
	 * @see org.springframework.integration.test.mock.MockIntegration#mockMessageSource
	 */
	public void substituteMessageSourceFor(String pollingAdapterId, MessageSource<?> mockMessageSource) {
		substituteMessageSourceFor(pollingAdapterId, mockMessageSource, true);
	}

	/**
	 * Replace the real {@link MessageSource} in the {@link SourcePollingChannelAdapter} bean
	 * with provided {@link MessageSource} instance.
	 * Can be a mock object.
	 * The endpoint is not started when {@code autoStartup == false}.
	 * @param pollingAdapterId the endpoint bean name
	 * @param mockMessageSource the {@link MessageSource} to replace in the endpoint bean
	 * @param autoStartup start or not the endpoint after replacing its {@link MessageSource}
	 * @see org.springframework.integration.test.mock.MockIntegration#mockMessageSource
	 */
	public void substituteMessageSourceFor(String pollingAdapterId, MessageSource<?> mockMessageSource,
			boolean autoStartup) {

		substituteComponentFor(pollingAdapterId, mockMessageSource, SourcePollingChannelAdapter.class, "source",
				autoStartup);
	}

	public void substituteMessageHandlerFor(String consumerEndpointId, MessageHandler mockMessageHandler) {
		substituteMessageHandlerFor(consumerEndpointId, mockMessageHandler, true);
	}

	public void substituteMessageHandlerFor(String consumerEndpointId, // NOSONAR - complexity
			MessageHandler mockMessageHandler, boolean autoStartup) {

		Object endpoint = this.beanFactory.getBean(consumerEndpointId, IntegrationConsumer.class);
		if (autoStartup && endpoint instanceof Lifecycle lifecycle) {
			lifecycle.stop();
		}
		DirectFieldAccessor directFieldAccessor = new DirectFieldAccessor(endpoint);
		Object targetMessageHandler = directFieldAccessor.getPropertyValue(HANDLER);
		Assert.notNull(targetMessageHandler, () -> "'handler' must not be null in the: " + endpoint);
		this.beans.add(consumerEndpointId, targetMessageHandler);
		if (endpoint instanceof ReactiveStreamsConsumer) {
			Object targetReactiveMessageHandler = directFieldAccessor.getPropertyValue(REACTIVE_MESSAGE_HANDLER);
			if (targetReactiveMessageHandler != null) {
				this.beans.add(consumerEndpointId, targetReactiveMessageHandler);
			}
		}

		if (mockMessageHandler instanceof MessageProducer mockMessageProducer) {
			if (targetMessageHandler instanceof MessageProducer messageProducer) {
				MessageChannel outputChannel = messageProducer.getOutputChannel();
				if (outputChannel != null) {
					mockMessageProducer.setOutputChannel(outputChannel);
				}
			}
			else {
				if (mockMessageHandler instanceof MockMessageHandler) {
					if (Boolean.TRUE.equals(TestUtils.<Boolean>getPropertyValue(mockMessageHandler, "hasReplies"))) {
						throw new IllegalStateException("The [" + mockMessageHandler + "] " +
								"with replies can't replace simple MessageHandler [" + targetMessageHandler + "]");
					}
				}
				else {
					throw new IllegalStateException("The MessageProducer handler [" + mockMessageHandler + "] " +
							"can't replace simple MessageHandler [" + targetMessageHandler + "]");
				}
			}
		}

		directFieldAccessor.setPropertyValue(HANDLER, mockMessageHandler);

		if (endpoint instanceof ReactiveStreamsConsumer) {
			ReactiveMessageHandler reactiveMessageHandler =
					(message) -> Mono.fromRunnable(() -> mockMessageHandler.handleMessage(message));
			directFieldAccessor.setPropertyValue(REACTIVE_MESSAGE_HANDLER, reactiveMessageHandler);
		}

		if (autoStartup && endpoint instanceof Lifecycle lifecycle) {
			lifecycle.start();
		}
	}

	/**
	 * Replace the real {@link Trigger} in the {@link AbstractPollingEndpoint} bean with provided instance.
	 * @param pollingEndpointId the {@link AbstractPollingEndpoint} bean id to replace
	 * @param trigger the {@link Trigger} to set into {@link AbstractPollingEndpoint}
	 * @since 6.3
	 */
	public void substituteTriggerFor(String pollingEndpointId, Trigger trigger) {
		substituteTriggerFor(pollingEndpointId, trigger, true);
	}

	/**
	 * Replace the real {@link Trigger} in the {@link AbstractPollingEndpoint} bean with provided instance.
	 * The endpoint is not started when {@code autoStartup == false}.
	 * @param pollingEndpointId the {@link AbstractPollingEndpoint} bean id to replace
	 * @param trigger the {@link Trigger} to set into {@link AbstractPollingEndpoint}
	 * @param autoStartup start or not the endpoint after replacing its {@link MessageSource}
	 * @since 6.3
	 */
	public void substituteTriggerFor(String pollingEndpointId, Trigger trigger, boolean autoStartup) {
		substituteComponentFor(pollingEndpointId, trigger, AbstractPollingEndpoint.class, "trigger", autoStartup);
	}

	private void substituteComponentFor(String endpointId, Object messagingComponent, Class<?> endpointClass,
			String property, boolean autoStartup) {

		Object endpoint = this.beanFactory.getBean(endpointId, endpointClass);
		if (autoStartup && endpoint instanceof Lifecycle lifecycle) {
			lifecycle.stop();
		}
		DirectFieldAccessor directFieldAccessor = new DirectFieldAccessor(endpoint);
		this.beans.add(endpointId, directFieldAccessor.getPropertyValue(property));
		directFieldAccessor.setPropertyValue(property, messagingComponent);
		if (autoStartup && endpoint instanceof Lifecycle lifecycle) {
			lifecycle.start();
		}
	}

}
