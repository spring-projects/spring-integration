/*
 * Copyright 2002-2008 the original author or authors.
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
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.integration.channel.BeanFactoryChannelResolver;
import org.springframework.integration.channel.ChannelResolver;
import org.springframework.integration.scheduling.TaskScheduler;
import org.springframework.util.Assert;

/**
 * A base class that provides convenient access to the bean factory as
 * well as {@link ChannelResolver} and {@link TaskScheduler} instances.
 * 
 * <p>This is intended to be used as a base class for internal framework
 * components whereas code built upon the integration framework should not
 * require tight coupling with the context but rather rely on standard
 * dependency injection.
 * 
 * @author Mark Fisher
 */
public abstract class IntegrationObjectSupport implements BeanNameAware, BeanFactoryAware {

	/** Logger that is available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private volatile String beanName;

	private volatile BeanFactory beanFactory;

	private volatile ChannelResolver channelResolver;

	private volatile TaskScheduler taskScheduler;


	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public final void setBeanFactory(BeanFactory beanFactory) {
		Assert.notNull(beanFactory, "beanFactory must not be null");
		this.beanFactory = beanFactory;
		this.channelResolver = new BeanFactoryChannelResolver(beanFactory);
		TaskScheduler taskScheduler = IntegrationContextUtils.getTaskScheduler(beanFactory);
		if (taskScheduler != null) {
			this.taskScheduler = taskScheduler;
		}
	}

	protected BeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	protected ChannelResolver getChannelResolver() {
		return this.channelResolver;
	}

	protected TaskScheduler getTaskScheduler() {
		return this.taskScheduler;
	}

	protected void setTaskScheduler(TaskScheduler taskScheduler) {
		Assert.notNull(taskScheduler, "taskScheduler must not be null");
		this.taskScheduler = taskScheduler;
	}


	@Override
	public String toString() {
		return (this.beanName != null) ? this.beanName : super.toString();
	}

}
