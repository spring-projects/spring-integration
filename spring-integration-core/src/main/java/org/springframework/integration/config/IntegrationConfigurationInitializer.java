/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * The strategy to initialize the external Integration infrastructure (@{code BeanFactoryPostProcessor}s,
 * global beans etc.) in the provided {@code beanFactory}.
 * <p>
 * Typically implementations are loaded by {@link org.springframework.core.io.support.SpringFactoriesLoader}.
 *
 * @author Artem Bilan
 * @since 4.0
 */
public interface IntegrationConfigurationInitializer {

	void initialize(ConfigurableListableBeanFactory beanFactory) throws BeansException;

}
