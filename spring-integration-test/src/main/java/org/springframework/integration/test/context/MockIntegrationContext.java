/*
 * Copyright 2017-2018 the original author or authors.
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

package org.springframework.integration.test.context;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.Lifecycle;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.endpoint.IntegrationConsumer;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.test.mock.MockMessageHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;
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
 *
 * @since 5.0
 *
 * @see SpringIntegrationTest
 */
public class MockIntegrationContext implements BeanFactoryAware {

	public static final String MOCK_INTEGRATION_CONTEXT_BEAN_NAME = "mockIntegrationContext";

	private final Map<String, Object> beans = new HashMap<>();

	private ConfigurableListableBeanFactory beanFactory;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.isAssignable(ConfigurableListableBeanFactory.class, beanFactory.getClass(),
				"a ConfigurableListableBeanFactory is required");
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}

	/**
	 * Reinstate the mocked beans after execution test to their real state.
	 * Typically is used from the {@link org.junit.After} method.
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
				.filter(e -> names == null || names.contains(e.getKey()))
				.forEach(e -> {
					Object endpoint = this.beanFactory.getBean(e.getKey());
					DirectFieldAccessor directFieldAccessor = new DirectFieldAccessor(endpoint);
					if (endpoint instanceof SourcePollingChannelAdapter) {
						directFieldAccessor.setPropertyValue("source", e.getValue());
					}
					else if (endpoint instanceof IntegrationConsumer) {
						directFieldAccessor.setPropertyValue("handler", e.getValue());
					}
				});
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
		substituteMessageSourceFor(pollingAdapterId, mockMessageSource, SourcePollingChannelAdapter.class, "source",
				autoStartup);
	}

	public void substituteMessageHandlerFor(String consumerEndpointId, MessageHandler mockMessageHandler) {
		substituteMessageHandlerFor(consumerEndpointId, mockMessageHandler, true);
	}

	public void substituteMessageHandlerFor(String consumerEndpointId, MessageHandler mockMessageHandler,
			boolean autoStartup) {
		Object endpoint = this.beanFactory.getBean(consumerEndpointId, IntegrationConsumer.class);
		if (autoStartup && endpoint instanceof Lifecycle) {
			((Lifecycle) endpoint).stop();
		}
		DirectFieldAccessor directFieldAccessor = new DirectFieldAccessor(endpoint);
		Object targetMessageHandler = directFieldAccessor.getPropertyValue("handler");
		this.beans.put(consumerEndpointId, targetMessageHandler);

		if (mockMessageHandler instanceof MessageProducer) {
			if (targetMessageHandler instanceof MessageProducer) {
				MessageChannel outputChannel = ((MessageProducer) targetMessageHandler).getOutputChannel();
				((MessageProducer) mockMessageHandler).setOutputChannel(outputChannel);
			}
			else {
				if (mockMessageHandler instanceof MockMessageHandler) {
					if (TestUtils.getPropertyValue(mockMessageHandler, "hasReplies", Boolean.class)) {
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

		directFieldAccessor.setPropertyValue("handler", mockMessageHandler);

		if (autoStartup && endpoint instanceof Lifecycle) {
			((Lifecycle) endpoint).start();
		}
	}

	private void substituteMessageSourceFor(String endpointId, Object messagingComponent, Class<?> endpointClass,
			String property, boolean autoStartup) {
		Object endpoint = this.beanFactory.getBean(endpointId, endpointClass);
		if (autoStartup && endpoint instanceof Lifecycle) {
			((Lifecycle) endpoint).stop();
		}
		DirectFieldAccessor directFieldAccessor = new DirectFieldAccessor(endpoint);
		this.beans.put(endpointId, directFieldAccessor.getPropertyValue(property));
		directFieldAccessor.setPropertyValue(property, messagingComponent);
		if (autoStartup && endpoint instanceof Lifecycle) {
			((Lifecycle) endpoint).start();
		}
	}

}
