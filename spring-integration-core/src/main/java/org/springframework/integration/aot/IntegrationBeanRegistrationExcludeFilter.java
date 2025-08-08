/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.aot;

import java.util.Arrays;

import org.springframework.beans.factory.aot.BeanRegistrationExcludeFilter;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.integration.config.DefaultConfiguringBeanFactoryPostProcessor;
import org.springframework.integration.config.IntegrationComponentScanRegistrar;
import org.springframework.integration.config.IntegrationConfigurationBeanFactoryPostProcessor;

/**
 * The {@link BeanRegistrationExcludeFilter} to exclude beans not need at runtime anymore.
 * Usually {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor} beans
 * are excluded since they have contributed their bean definitions code generated during AOT
 * build phase.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 */
class IntegrationBeanRegistrationExcludeFilter implements BeanRegistrationExcludeFilter {

	@Override
	public boolean isExcludedFromAotProcessing(RegisteredBean registeredBean) {
		Class<?> beanClass = registeredBean.getBeanClass();
		return Arrays.asList(DefaultConfiguringBeanFactoryPostProcessor.class,
						IntegrationConfigurationBeanFactoryPostProcessor.class,
						IntegrationComponentScanRegistrar.class)
				.contains(beanClass);
	}

}
