/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.config;

import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.core.io.support.SpringFactoriesLoader;

/**
 * {@link BeanDefinitionRegistryPostProcessor} to apply external Integration
 * infrastructure configurations via loading {@link IntegrationConfigurationInitializer}
 * implementations using {@link SpringFactoriesLoader}.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.0
 */
public class IntegrationConfigurationBeanFactoryPostProcessor implements BeanDefinitionRegistryPostProcessor {

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		ConfigurableListableBeanFactory beanFactory = (ConfigurableListableBeanFactory) registry;

		List<IntegrationConfigurationInitializer> initializers =
				SpringFactoriesLoader.loadFactories(IntegrationConfigurationInitializer.class,
						beanFactory.getBeanClassLoader());

		for (IntegrationConfigurationInitializer initializer : initializers) {
			initializer.initialize(beanFactory);
		}
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
	}

}
