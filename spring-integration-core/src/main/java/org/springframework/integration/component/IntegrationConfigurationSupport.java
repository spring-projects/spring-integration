package org.springframework.integration.component;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;

/**
 * This is the main class providing the configuration behind the Spring Integration Java config.
 *
 * @author Artem Bilan
 * @since 3.0
 */
public abstract class IntegrationConfigurationSupport implements BeanFactoryAware, BeanClassLoaderAware {

	protected final Log logger = LogFactory.getLog(this.getClass());

	protected ClassLoader classLoader;

	protected BeanFactory beanFactory;

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

}
