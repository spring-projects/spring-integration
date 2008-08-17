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

package org.springframework.integration.config;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.dispatcher.PollingDispatcher;
import org.springframework.integration.dispatcher.SimpleDispatcher;
import org.springframework.integration.message.AsyncMessageExchangeTemplate;
import org.springframework.integration.message.MessageExchangeTemplate;
import org.springframework.integration.message.MessageSource;
import org.springframework.integration.message.PollableSource;
import org.springframework.integration.scheduling.PollingSchedule;
import org.springframework.integration.scheduling.Schedule;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * @author Mark Fisher
 */
public class PollingDispatcherFactoryBean implements FactoryBean, InitializingBean {

	private volatile PollingDispatcher poller;

	private volatile MessageSource<?> source;

	private volatile Schedule schedule;

	private volatile long receiveTimeout = -1;

	private volatile long sendTimeout = -1;

	private volatile int maxMessagesPerPoll = -1;

	private volatile TaskExecutor taskExecutor; 

	private volatile PlatformTransactionManager transactionManager;

	private volatile String propagationBehaviorName;

	private volatile String isolationLevelName;

	private volatile int transactionTimeout;

	private volatile boolean transactionReadOnly;

	private volatile boolean validated;

	private volatile boolean initialized;

	private final Object initializationMonitor = new Object();


	public void setSource(MessageSource<?> source) {
		this.source = source;
	}

	public void setSchedule(Schedule schedule) {
		this.schedule = schedule;
	}

	public void setReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	public void setSendTimeout(long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	public void setMaxMessagesPerPoll(int maxMessagesPerPoll) {
		this.maxMessagesPerPoll = maxMessagesPerPoll;
	}

	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setPropagationBehaviorName(String propagationBehaviorName) {
		this.propagationBehaviorName = propagationBehaviorName;
	}

	public void setIsolationLevelName(String isolationLevelName) {
		this.isolationLevelName = isolationLevelName;
	}

	public void setTransactionTimeout(int transactionTimeout) {
		this.transactionTimeout = transactionTimeout;
	}

	public void setTransactionReadOnly(boolean transactionReadOnly) {
		this.transactionReadOnly = transactionReadOnly;
	}

	public void afterPropertiesSet() {
		synchronized (this.initializationMonitor) {
			if (this.source == null) {
				throw new ConfigurationException("source is required");
			}
			if (!(this.source instanceof PollableSource)) {
				throw new BeanCreationException("Poller requires a PollableSource, but actual type of '"
						+ this.source + "' is [" + this.source.getClass() + "]");
			}
			this.validated = true;
		}
	}

	public Object getObject() throws Exception {
		if (!this.initialized) {
			this.initPoller();
		}
		return this.poller;
	}

	public Class<?> getObjectType() {
		return PollingDispatcher.class;
	}

	public boolean isSingleton() {
		return true;
	}

	private void initPoller() {
		synchronized (this.initializationMonitor) {
			if (this.initialized) {
				return;
			}
			if (!this.validated) {
				this.afterPropertiesSet();
			}
			if (this.schedule == null) {
				this.schedule = new PollingSchedule(0);
			}
			MessageExchangeTemplate template = this.createMessageExchangeTemplate();
			this.poller = new PollingDispatcher((PollableSource<?>) this.source, this.schedule, new SimpleDispatcher(), template);
			this.poller.setMaxMessagesPerPoll(this.maxMessagesPerPoll);
			this.initialized = true;
		}
	}

	private MessageExchangeTemplate createMessageExchangeTemplate() {
		MessageExchangeTemplate template = (this.taskExecutor != null) ?
				new AsyncMessageExchangeTemplate(this.taskExecutor) : new MessageExchangeTemplate();
		template.setTransactionManager(this.transactionManager);
		template.setPropagationBehaviorName(DefaultTransactionDefinition.PREFIX_PROPAGATION + this.propagationBehaviorName);
		template.setIsolationLevelName(DefaultTransactionDefinition.PREFIX_ISOLATION + this.isolationLevelName);
		template.setTransactionTimeout(this.transactionTimeout);
		template.setTransactionReadOnly(this.transactionReadOnly);
		template.setReceiveTimeout(this.receiveTimeout);
		template.setSendTimeout(this.sendTimeout);
		return template;
	}

}
