/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
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
@FunctionalInterface
public interface IntegrationConfigurationInitializer {

	void initialize(ConfigurableListableBeanFactory beanFactory) throws BeansException;

}
