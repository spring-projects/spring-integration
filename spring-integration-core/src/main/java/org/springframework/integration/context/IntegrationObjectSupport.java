/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.context;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A base class that provides convenient access to the bean factory as
 * well as {@link TaskScheduler} and {@link ConversionService} instances.
 *
 * <p>This is intended to be used as a base class for internal framework
 * components whereas code built upon the integration framework should not
 * require tight coupling with the context but rather rely on standard
 * dependency injection.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Josh Long
 * @author Stefan Ferstl
 * @author Gary Russell
 * @author Artem Bilan
 */
public abstract class IntegrationObjectSupport implements BeanNameAware, NamedComponent,
		ApplicationContextAware, BeanFactoryAware, InitializingBean {

	/**
	 * Logger that is available to subclasses
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	private final ConversionService defaultConversionService = new DefaultConversionService();

	private volatile String beanName;

	private volatile String componentName;

	private volatile BeanFactory beanFactory;

	private volatile TaskScheduler taskScheduler;

	private volatile Properties integrationProperties = IntegrationProperties.defaults();

	private volatile ConversionService conversionService;

	private volatile ApplicationContext applicationContext;

	private volatile MessageBuilderFactory messageBuilderFactory;

	@Override
	public final void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	/**
	 * Will return the name of this component identified by {@link #componentName} field.
	 * If {@link #componentName} was not set this method will default to the 'beanName' of this component;
	 */
	@Override
	public final String getComponentName() {
		return StringUtils.hasText(this.componentName) ? this.componentName : this.beanName;
	}

	/**
	 * Sets the name of this component.
	 * @param componentName The component name.
	 */
	public void setComponentName(String componentName) {
		this.componentName = componentName;
	}

	/**
	 * Subclasses may implement this method to provide component type information.
	 */
	@Override
	public String getComponentType() {
		return null;
	}

	@Override
	public final void setBeanFactory(BeanFactory beanFactory) {
		Assert.notNull(beanFactory, "'beanFactory' must not be null");
		this.beanFactory = beanFactory;
		this.integrationProperties = IntegrationContextUtils.getIntegrationProperties(this.beanFactory);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		Assert.notNull(applicationContext, "'applicationContext' must not be null");
		this.applicationContext = applicationContext;
	}

	@Override
	public final void afterPropertiesSet() {
		try {
			if (this.messageBuilderFactory == null) {
				this.messageBuilderFactory = IntegrationUtils.getMessageBuilderFactory(this.beanFactory);
			}
			this.onInit();
		}
		catch (Exception e) {
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			throw new BeanInitializationException("failed to initialize", e);
		}
	}

	/**
	 * Subclasses may implement this for initialization logic.
	 * @throws Exception Any exception.
	 */
	protected void onInit() throws Exception {
	}

	protected final BeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	protected TaskScheduler getTaskScheduler() {
		if (this.taskScheduler == null && this.beanFactory != null) {
			this.taskScheduler = IntegrationContextUtils.getTaskScheduler(this.beanFactory);
		}
		return this.taskScheduler;
	}

	protected void setTaskScheduler(TaskScheduler taskScheduler) {
		Assert.notNull(taskScheduler, "taskScheduler must not be null");
		this.taskScheduler = taskScheduler;
	}

	public final ConversionService getConversionService() {
		if (this.conversionService == null && this.beanFactory != null) {
			synchronized (this) {
				if (this.conversionService == null) {
					this.conversionService = IntegrationUtils.getConversionService(this.beanFactory);
				}
			}
			if (this.conversionService == null && this.logger.isDebugEnabled()) {
				this.logger.debug("Unable to attempt conversion of Message payload types. Component '" +
						this.getComponentName() + "' has no explicit ConversionService reference, " +
						"and there is no 'integrationConversionService' bean within the context.");
			}
		}
		return this.conversionService;
	}

	protected void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	/**
	 * Returns the {@link ApplicationContext#getId()} if the
	 * {@link ApplicationContext} is available.
	 * @return The id, or null if there is no application context.
	 */
	public String getApplicationContextId() {
		return this.applicationContext == null ? null : this.applicationContext.getId();
	}

	/**
	 * @return the applicationContext
	 */
	protected ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	/**
	 * @see IntegrationContextUtils#getIntegrationProperties(BeanFactory)
	 * @return The global integration properties.
	 */
	protected Properties getIntegrationProperties() {
		return this.integrationProperties;
	}


	protected MessageBuilderFactory getMessageBuilderFactory() {
		if (this.messageBuilderFactory == null) {
			this.messageBuilderFactory = new DefaultMessageBuilderFactory();
		}
		return this.messageBuilderFactory;
	}

	public void setMessageBuilderFactory(MessageBuilderFactory messageBuilderFactory) {
		this.messageBuilderFactory = messageBuilderFactory;
	}

	/**
	 * @param  key    Integration property.
	 * @param  tClass the class to convert a value of Integration property.
	 * @param <T> The expected type of the property.
	 * @return the value of the Integration property converted to the provide type.
	 */
	protected <T> T getIntegrationProperty(String key, Class<T> tClass) {
		return this.defaultConversionService.convert(this.integrationProperties.getProperty(key), tClass);
	}

	@Override
	public String toString() {
		return (this.beanName != null) ? this.beanName : super.toString();
	}

}
