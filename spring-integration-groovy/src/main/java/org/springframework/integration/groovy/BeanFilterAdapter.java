/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.groovy;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * A {@link BeanFilter} with methods' implementations as delegation to the given <code>beanFactory</code>
 * with default {@link ConfigurableListableBeanFactory#getBean} behaviour
 * so that subclasses do not have to implement all of the interface's methods.
 *
 * @author Artem Bilan
 * @since 2.2
 */
public abstract class BeanFilterAdapter implements BeanFilter {

	public Object getBeanIfMatch(ConfigurableListableBeanFactory beanFactory, String name) {
		return beanFactory.getBean(name);
	}

	public <T> T getBeanIfMatch(ConfigurableListableBeanFactory beanFactory, String name, Class<T> requiredType) {
		return beanFactory.getBean(name, requiredType);
	}

	public <T> T getBeanIfMatch(ConfigurableListableBeanFactory beanFactory, Class<T> requiredType) {
		return beanFactory.getBean(requiredType);
	}

	public Object getBeanIfMatch(ConfigurableListableBeanFactory beanFactory, String name, Object... args) {
		return beanFactory.getBean(name, args);
	}
}
