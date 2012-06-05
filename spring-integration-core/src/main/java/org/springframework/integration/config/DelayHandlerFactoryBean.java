/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.config;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.channel.MessagePublishingErrorHandler;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.handler.DelayHandler;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ExecutorConfigurationSupport;
import org.springframework.util.Assert;
import org.springframework.util.ErrorHandler;

/**
 * FactoryBean for creating {@link DelayHandler}.
 *
 * @author Artem Bilan
 * @since 2.2
 */
public class DelayHandlerFactoryBean extends AbstractSimpleMessageHandlerFactoryBean<DelayHandler> implements SmartLifecycle, DisposableBean {

	private static final Set<String> usedDelayersTaskSchedulers = new HashSet<String>();

	protected final Log logger = LogFactory.getLog(getClass());

	private String messageGroupId;

	private long defaultDelay;

	private String delayHeaderName;

	private boolean waitForTasksToCompleteOnShutdown;

	private MessageGroupStore messageStore;

	private String taskSchedulerBeanName;

	private long sendTimeout;

	private volatile boolean autoStartup = true;

	private volatile int phase = Integer.MAX_VALUE;

	private volatile DelayHandler delayHandler;

	@Override
	protected DelayHandler createHandler() {
		Assert.isTrue(this.taskSchedulerBeanName == null || usedDelayersTaskSchedulers.add(this.taskSchedulerBeanName),
				"DelayHandlers can't share taskSchedulers. Provide unique TaskScheduler bean for this DelayHandler or " +
						"don't use it at all preferring global shared ThreadPoolTaskScheduler.");
		DelayHandler handler = new DelayHandler(this.messageGroupId);
		handler.setDefaultDelay(this.defaultDelay);
		handler.setDelayHeaderName(this.delayHeaderName);
		handler.setSendTimeout(this.sendTimeout);
		handler.setAutoStartup(this.autoStartup);
		handler.setPhase(this.phase);
		handler.setMessageStore(this.messageStore != null ? this.messageStore : new SimpleMessageStore());
		if (this.taskSchedulerBeanName != null) {
			TaskScheduler taskScheduler = this.resolveAndConfigureTaskScheduler();
			handler.setTaskScheduler(taskScheduler);
		}

		handler.afterPropertiesSet();

		this.delayHandler = handler;

		return this.delayHandler;
	}

	private TaskScheduler resolveAndConfigureTaskScheduler() {
		TaskScheduler taskScheduler = this.getBeanFactory().getBean(this.taskSchedulerBeanName, TaskScheduler.class);
		if (taskScheduler instanceof ExecutorConfigurationSupport) {
			((ExecutorConfigurationSupport) taskScheduler).setWaitForTasksToCompleteOnShutdown(this.waitForTasksToCompleteOnShutdown);
		}
		else if (this.logger.isWarnEnabled()) {
			this.logger.warn("The 'waitForJobsToCompleteOnShutdown' property is not supported for TaskScheduler of type [" +
					taskScheduler.getClass() + "]");
		}
		DirectFieldAccessor taskSchedulerFieldAccessor = new DirectFieldAccessor(taskScheduler);
		if (taskSchedulerFieldAccessor.isReadableProperty("errorHandler") && taskSchedulerFieldAccessor.getPropertyValue("errorHandler") == null) {
			ErrorHandler errorHandler = this.createTaskSchedulerErrorHandler();
			taskSchedulerFieldAccessor.setPropertyValue("errorHandler", errorHandler);
		}

		return taskScheduler;
	}

	private ErrorHandler createTaskSchedulerErrorHandler() {
		MessagePublishingErrorHandler errorHandler = new MessagePublishingErrorHandler();
		errorHandler.setBeanFactory(this.getBeanFactory());
		errorHandler.setDefaultErrorChannel(IntegrationContextUtils.getErrorChannel(this.getBeanFactory()));
		return errorHandler;
	}

	public void setMessageGroupId(String messageGroupId) {
		this.messageGroupId = messageGroupId;
	}

	public void setDefaultDelay(long defaultDelay) {
		this.defaultDelay = defaultDelay;
	}

	public void setDelayHeaderName(String delayHeaderName) {
		this.delayHeaderName = delayHeaderName;
	}

	/**
	 * Set whether to wait for scheduled tasks to complete on shutdown.
	 * <p>Default is "false". Switch this to "true" if you prefer
	 * fully completed tasks at the expense of a longer shutdown phase.
	 * <p/>
	 * This property will only have an effect for TaskScheduler implementations
	 * that extend from {@link ExecutorConfigurationSupport}.
	 *
	 * @see ExecutorConfigurationSupport#setWaitForTasksToCompleteOnShutdown(boolean)
	 */
	public void setWaitForTasksToCompleteOnShutdown(boolean waitForTasksToCompleteOnShutdown) {
		this.waitForTasksToCompleteOnShutdown = waitForTasksToCompleteOnShutdown;
	}

	public void setMessageStore(MessageGroupStore messageStore) {
		this.messageStore = messageStore;
	}

	public void setTaskSchedulerBeanName(String taskSchedulerBeanName) {
		this.taskSchedulerBeanName = taskSchedulerBeanName;
	}

	public void setSendTimeout(long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	public void setPhase(int phase) {
		this.phase = phase;
	}

	@Override
	public int getPhase() {
		return this.delayHandler.getPhase();
	}

	@Override
	public boolean isAutoStartup() {
		return this.delayHandler.isAutoStartup();
	}

	@Override
	public void stop(Runnable callback) {
		this.delayHandler.stop(callback);
	}

	@Override
	public void start() {
		this.delayHandler.start();
	}

	@Override
	public void stop() {
		this.delayHandler.stop();
	}

	@Override
	public boolean isRunning() {
		return this.delayHandler.isRunning();
	}

	@Override
	public void destroy() throws Exception {
		usedDelayersTaskSchedulers.clear();
	}

}
