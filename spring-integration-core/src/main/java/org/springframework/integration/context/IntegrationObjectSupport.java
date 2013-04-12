/*
 * Copyright 2002-2013 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.convert.ConversionService;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A base class that provides convenient access to the bean factory as
 * well as {@link TaskScheduler} and {@link ConversionService} instances.
 * <p>
 * <p>This is intended to be used as a base class for internal framework
 * components whereas code built upon the integration framework should not
 * require tight coupling with the context but rather rely on standard
 * dependency injection.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Josh Long
 * @author Stefan Ferstl
 */
public abstract class IntegrationObjectSupport implements BeanNameAware, NamedComponent, BeanFactoryAware, InitializingBean {

	/**
	 * Logger that is available to subclasses
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	private volatile String beanName;

	private volatile String componentName;

	private volatile BeanFactory beanFactory;

	private volatile TaskScheduler taskScheduler;

	private volatile ConversionService conversionService;


	public final void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	/**
	 * Will return the name of this component identified by {@link #componentName} field.
	 * If {@link #componentName} was not set this method will default to the 'beanName' of this component;
	 */
	public final String getComponentName() {
		return StringUtils.hasText(this.componentName) ? this.componentName : this.beanName;
	}

	/**
	 * Sets the name of this component.
	 *
	 * @param componentName
	 */
	public void setComponentName(String componentName) {
		this.componentName = componentName;
	}

	/**
	 * Subclasses may implement this method to provide component type information.
	 */
	public String getComponentType() {
		return null;
	}

	public final void setBeanFactory(BeanFactory beanFactory) {
		Assert.notNull(beanFactory, "beanFactory must not be null");
		this.beanFactory = beanFactory;
	}

	public final void afterPropertiesSet() {
		try {
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

	protected final ConversionService getConversionService() {
		if (this.conversionService == null && this.beanFactory != null) {
			synchronized (this) {
				if (this.conversionService == null) {
					this.conversionService = IntegrationContextUtils.getConversionService(this.beanFactory);
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

	@Override
	public String toString() {
		return (this.beanName != null) ? this.beanName : super.toString();
	}

}
