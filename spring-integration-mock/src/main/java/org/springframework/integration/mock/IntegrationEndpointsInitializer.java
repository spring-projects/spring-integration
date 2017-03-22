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

import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.util.Assert;
import org.springframework.util.PatternMatchUtils;

/**
 * A component to customize {@link AbstractEndpoint} beans according
 * provided options in the {@link SpringIntegrationTest} annotation after all beans
 * are registered in the applicaiton context but before its refresh.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
class IntegrationEndpointsInitializer implements SmartInitializingSingleton, BeanFactoryAware {

	private final SpringIntegrationTest springIntegrationTest;

	private ConfigurableListableBeanFactory beanFactory;

	IntegrationEndpointsInitializer(SpringIntegrationTest springIntegrationTest) {
		this.springIntegrationTest = springIntegrationTest;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.isInstanceOf(ConfigurableListableBeanFactory.class, beanFactory);
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}

	@Override
	public void afterSingletonsInstantiated() {
		Map<String, AbstractEndpoint> endpoints = this.beanFactory.getBeansOfType(AbstractEndpoint.class);

		endpoints.entrySet()
				.stream()
				.filter(entry -> match(entry.getKey()))
				.forEach(entry -> entry.getValue().setAutoStartup(false));
	}

	private boolean match(String name) {
		for (String pattern : this.springIntegrationTest.stopEndpoints()) {
			if (PatternMatchUtils.simpleMatch(pattern, name)) {
				return true;
			}
		}
		return false;
	}

}
