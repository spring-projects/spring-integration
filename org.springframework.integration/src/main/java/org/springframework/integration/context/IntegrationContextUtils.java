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

import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Mark Fisher
 */
public abstract class IntegrationContextUtils {

	public static final String TASK_SCHEDULER_BEAN_NAME = "taskScheduler";

	public static final String ERROR_CHANNEL_BEAN_NAME = "errorChannel";


	public static MessageChannel getErrorChannel(BeanFactory beanFactory) {
		return getBeanOfType(beanFactory, ERROR_CHANNEL_BEAN_NAME, MessageChannel.class);
	}

	public static TaskScheduler getTaskScheduler(BeanFactory beanFactory) {
		return getBeanOfType(beanFactory, TASK_SCHEDULER_BEAN_NAME, TaskScheduler.class);
	}

	public static TaskScheduler getRequiredTaskScheduler(BeanFactory beanFactory) {
		TaskScheduler taskScheduler = getTaskScheduler(beanFactory);
		Assert.state(taskScheduler != null, "No such bean '" + TASK_SCHEDULER_BEAN_NAME + "'");
		return taskScheduler;
	}

	@SuppressWarnings("unchecked")
	private static <T> T getBeanOfType(BeanFactory beanFactory, String beanName, Class<T> type) {
		if (!beanFactory.containsBean(beanName)) {
			return null;
		}
		Object bean = beanFactory.getBean(beanName);
		Assert.state(type.isAssignableFrom(bean.getClass()), "incorrect type for bean '" + beanName
				+ "' expected [" + type + "], but actual type is [" + bean.getClass() + "].");
		return (T) bean;
	}

	public static TaskExecutor createTaskExecutor(int coreSize, int maxSize, int queueCapacity, String threadPrefix) {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(coreSize);
		executor.setMaxPoolSize(maxSize);
		executor.setQueueCapacity(queueCapacity);
		if (StringUtils.hasText(threadPrefix)) {
			executor.setThreadFactory(new CustomizableThreadFactory(threadPrefix));
		}
		executor.setRejectedExecutionHandler(new CallerRunsPolicy());
		executor.afterPropertiesSet();
		return executor;
	}

}
