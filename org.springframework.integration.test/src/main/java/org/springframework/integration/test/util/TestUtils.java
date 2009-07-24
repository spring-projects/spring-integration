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

package org.springframework.integration.test.util;

import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.AbstractPollingEndpoint;
import org.springframework.integration.scheduling.IntervalTrigger;
import org.springframework.integration.scheduling.SimpleTaskScheduler;
import org.springframework.integration.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 */
public abstract class TestUtils {

	public static Object getPropertyValue(Object root, String propertyPath) {
		Object value = null;
		DirectFieldAccessor accessor = new DirectFieldAccessor(root);
		String[] tokens = propertyPath.split("\\.");
		for (int i = 0; i < tokens.length; i++) {
			value = accessor.getPropertyValue(tokens[i]);
			if (value != null) {
				accessor = new DirectFieldAccessor(value);
			}
			else if (i == tokens.length - 1) {
				return null;
			}
			else {
				throw new IllegalArgumentException(
						"intermediate property '" + tokens[i] + "' is null");
			}
		}
		return value; 
	}

	@SuppressWarnings("unchecked")
	public static <T> T getPropertyValue(Object root, String propertyPath, Class<T> type) {
		Object value = getPropertyValue(root, propertyPath);
		Assert.isAssignable(type, value.getClass());
		return (T) value; 
	}

	public static TestApplicationContext createTestApplicationContext() {
		TestApplicationContext context = new TestApplicationContext();
		registerBean(IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME, createTaskScheduler(10), context);
		return context;
	}

	public static TaskScheduler createTaskScheduler(int poolSize) {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(poolSize);
		executor.setRejectedExecutionHandler(new CallerRunsPolicy());
		executor.afterPropertiesSet();
		return new SimpleTaskScheduler(executor);
	}

	private static void registerBean(String beanName, Object bean, BeanFactory beanFactory) {
		Assert.notNull(beanName, "bean name must not be null");
		ConfigurableListableBeanFactory configurableListableBeanFactory = null;
		if (beanFactory instanceof ConfigurableListableBeanFactory) {
			configurableListableBeanFactory = (ConfigurableListableBeanFactory) beanFactory;
		}
		else if (beanFactory instanceof GenericApplicationContext) {
			configurableListableBeanFactory = ((GenericApplicationContext) beanFactory).getBeanFactory();
		}
		if (bean instanceof BeanNameAware) {
			((BeanNameAware) bean).setBeanName(beanName);
		}
		if (bean instanceof BeanFactoryAware) {
			((BeanFactoryAware) bean).setBeanFactory(beanFactory);
		}
		if (bean instanceof InitializingBean) {
			try {
				((InitializingBean) bean).afterPropertiesSet();
			}
			catch (Exception e) {
				throw new FatalBeanException("failed to register bean with test context", e);
			}
		}
		configurableListableBeanFactory.registerSingleton(beanName, bean);
	}


	public static class TestApplicationContext extends GenericApplicationContext {

		private TestApplicationContext() {
			super();
		}

		public void registerChannel(String channelName, MessageChannel channel) {
			if (channel.getName() != null) {
				if (channelName == null) {
					Assert.notNull(channel.getName(), "channel name must not be null");
					channelName = channel.getName();
				}
				else {
					Assert.isTrue(channel.getName().equals(channelName),
							"channel name has already been set with a conflicting value");
				}
			}
			registerBean(channelName, channel, this);
		}

		public void registerEndpoint(String endpointName, AbstractEndpoint endpoint) {
			if (endpoint instanceof AbstractPollingEndpoint) {
				DirectFieldAccessor accessor = new DirectFieldAccessor(endpoint);
				if (accessor.getPropertyValue("trigger") == null) {
					((AbstractPollingEndpoint) endpoint).setTrigger(new IntervalTrigger(10));
				}
			}
			registerBean(endpointName, endpoint, this);
		}
	}

}
