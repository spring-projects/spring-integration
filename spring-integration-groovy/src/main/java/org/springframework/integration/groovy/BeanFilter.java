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
 * Strategy to filter getting Spring beans at runtime from the giver {@link ConfigurableListableBeanFactory}.
 * Can work in pair with {@link FilteredBeanFactoryDecorator}.
 *
 * @author Artem Bilan
 * @since 2.2
 */
public interface BeanFilter {

	Object getBeanIfMatch(ConfigurableListableBeanFactory beanFactory, String name);

	<T> T getBeanIfMatch(ConfigurableListableBeanFactory beanFactory, String name, Class<T> requiredType);

	<T> T getBeanIfMatch(ConfigurableListableBeanFactory beanFactory, Class<T> requiredType);

	Object getBeanIfMatch(ConfigurableListableBeanFactory beanFactory, String name, Object... args);
}
