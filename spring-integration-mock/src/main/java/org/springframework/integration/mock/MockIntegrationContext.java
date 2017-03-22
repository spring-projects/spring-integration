/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.integration.mock;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.Lifecycle;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.util.Assert;

/**
 * A {@link BeanFactoryAware} component with an API to customize real beans
 * in the application context from tests code.
 * <p>
 * The bean for this class is registered automatically via {@link SpringIntegrationTest}
 * annotation and can be autowired into test class.
 *
 * @author Artem Bilan
 *
 * @since 5.0
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
	 * Reinstate all the mocked beans after execution test to their real state.
	 * Typically is used from the {@link org.junit.After} method.
	 */
	public void resetMocks() {
		this.beans.forEach((key, value) -> {
			Object endpoint = this.beanFactory.getBean(key);
			DirectFieldAccessor directFieldAccessor = new DirectFieldAccessor(endpoint);
			if (endpoint instanceof SourcePollingChannelAdapter) {
				directFieldAccessor.setPropertyValue("source", value);
			}
		});
	}

	/**
	 * Replace the real {@link MessageSource} in the {@link SourcePollingChannelAdapter} bean
	 * with provided {@link MessageSource} instance.
	 * Can be a mock object.
	 * @param pollingAdapterId the endpoint bean name
	 * @param mockMessageSource the {@link MessageSource} to replace in the endpoint bean
	 * @see MockIntegration#mockMessageSource
	 */
	public void instead(String pollingAdapterId, MessageSource<?> mockMessageSource) {
		instead(pollingAdapterId, mockMessageSource, true);
	}

	/**
	 * Replace the real {@link MessageSource} in the {@link SourcePollingChannelAdapter} bean
	 * with provided {@link MessageSource} instance.
	 * Can be a mock object.
	 * The endpoint is not started when {@code autoStartup == false}.
	 * @param pollingAdapterId the endpoint bean name
	 * @param mockMessageSource the {@link MessageSource} to replace in the endpoint bean
	 * @param autoStartup start or not the endpoint after replacing its {@link MessageSource}
	 * @see MockIntegration#mockMessageSource
	 */
	public void instead(String pollingAdapterId, MessageSource<?> mockMessageSource, boolean autoStartup) {
		instead(pollingAdapterId, mockMessageSource, SourcePollingChannelAdapter.class, "source", autoStartup);
	}

	private void instead(String endpointId, Object mock, Class<?> endpointClass, String property,
			boolean autoStartup) {
		Object endpoint = this.beanFactory.getBean(endpointId, endpointClass);
		DirectFieldAccessor directFieldAccessor = new DirectFieldAccessor(endpoint);
		this.beans.put(endpointId, directFieldAccessor.getPropertyValue(property));
		directFieldAccessor.setPropertyValue("source", mock);
		if (autoStartup && endpoint instanceof Lifecycle) {
			((Lifecycle) endpoint).start();
		}
	}

}
