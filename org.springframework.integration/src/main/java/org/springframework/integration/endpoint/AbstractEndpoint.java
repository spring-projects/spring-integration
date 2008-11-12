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

package org.springframework.integration.endpoint;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.integration.channel.BeanFactoryChannelResolver;
import org.springframework.integration.channel.ChannelResolver;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.scheduling.TaskScheduler;
import org.springframework.integration.util.LifecycleSupport;
import org.springframework.util.Assert;

/**
 * The base class for Message Endpoint implementations.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractEndpoint extends LifecycleSupport implements MessageEndpoint, BeanNameAware, BeanFactoryAware {

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
			this.setTaskScheduler(taskScheduler);
		}
	}

	protected BeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	public void setTaskScheduler(TaskScheduler taskScheduler) {
		Assert.notNull(taskScheduler, "taskScheduler must not be null");
		this.taskScheduler = taskScheduler;
	}

	protected TaskScheduler getTaskScheduler() {
		return this.taskScheduler;
	}

	protected ChannelResolver getChannelResolver() {
		return this.channelResolver;
	}

	@Override
	public String toString() {
		return (this.beanName != null) ? this.beanName : super.toString();
	}

}
