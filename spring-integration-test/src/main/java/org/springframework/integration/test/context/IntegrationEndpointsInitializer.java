/*
 * Copyright 2017-2019 the original author or authors.
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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.util.Assert;
import org.springframework.util.PatternMatchUtils;

/**
 * A component to customize {@link AbstractEndpoint} beans according
 * to the provided options in the {@link SpringIntegrationTest} annotation
 * after all beans are registered in the application context but before its refresh.
 * <p>
 * This class implements both {@link SmartInitializingSingleton} and {@link BeanPostProcessor}
 * to cover all the possible variants of {@link AbstractEndpoint} beans registration.
 * First of all a bean for this class is registered in the application context, when XML configurations
 * are parsed and registered already by the Spring Testing Framework, therefore a
 * {@link BeanPostProcessor#postProcessBeforeInitialization(Object, String)} hook is not called for those beans.
 * On the other hand we can't always rely on just a {@link SmartInitializingSingleton#afterSingletonsInstantiated()}
 * because {@link SmartInitializingSingleton} beans are not ordered and some implementations may register beans
 * later, than this {@link #afterSingletonsInstantiated()} is called.
 * Plus beans might be registered at runtime, therefore {@link #postProcessBeforeInitialization(Object, String)}
 * is still applied.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
class IntegrationEndpointsInitializer implements SmartInitializingSingleton, BeanPostProcessor, BeanFactoryAware {

	private final String[] patterns;

	private ConfigurableListableBeanFactory beanFactory;

	IntegrationEndpointsInitializer(SpringIntegrationTest springIntegrationTest) {
		this.patterns = springIntegrationTest.noAutoStartup();
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.isInstanceOf(ConfigurableListableBeanFactory.class, beanFactory);
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof AbstractEndpoint && ((AbstractEndpoint) bean).isAutoStartup() && match(beanName)) {
			((AbstractEndpoint) bean).setAutoStartup(false);
		}
		return bean;
	}

	@Override
	public void afterSingletonsInstantiated() {
		this.beanFactory.getBeansOfType(AbstractEndpoint.class)
				.entrySet()
				.stream()
				.filter(entry -> entry.getValue().isAutoStartup() && match(entry.getKey()))
				.forEach(entry -> entry.getValue().setAutoStartup(false));
	}

	private boolean match(String name) {
		return Arrays.stream(this.patterns)
				.anyMatch(pattern -> PatternMatchUtils.simpleMatch(pattern, name));
	}

}
