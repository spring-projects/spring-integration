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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.ChannelRegistryAware;
import org.springframework.integration.channel.MessageChannelTemplate;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.scheduling.TaskScheduler;
import org.springframework.integration.scheduling.TaskSchedulerAware;
import org.springframework.integration.util.ErrorHandler;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

/**
 * The base class for Message Endpoint implementations.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractEndpoint implements MessageEndpoint, ChannelRegistryAware, TaskSchedulerAware, BeanNameAware, InitializingBean {

	protected final Log logger = LogFactory.getLog(this.getClass());

	private volatile String name;

	private volatile ChannelRegistry channelRegistry;

	private volatile TaskScheduler taskScheduler;

	private volatile PlatformTransactionManager transactionManager;

	private volatile TransactionDefinition transactionDefinition;

	private volatile ErrorHandler errorHandler;

	private final MessageChannelTemplate channelTemplate = new MessageChannelTemplate();


	/**
	 * Return the name of this endpoint.
	 */
	public String getName() {
		return this.name;
	}

	public void setBeanName(String name) {
		this.name = name;
	}

	protected ChannelRegistry getChannelRegistry() {
		return this.channelRegistry;
	}

	public void setChannelRegistry(ChannelRegistry channelRegistry) {
		this.channelRegistry = channelRegistry;
	}

	protected TaskScheduler getTaskScheduler() {
		return this.taskScheduler;
	}

	public void setTaskScheduler(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setTransactionDefinition(TransactionDefinition transactionDefinition) {
		this.transactionDefinition= transactionDefinition;
	}

	protected MessageChannelTemplate getChannelTemplate() {
		return this.channelTemplate;
	}

	/**
	 * Provide an error handler for any Exceptions that occur
	 * upon invocation of this endpoint. If none is provided,
	 * the Exception messages will be logged (at warn level),
	 * and the Exception rethrown.
	 */
	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	public final void afterPropertiesSet() {
		try {
			this.initialize();
		}
		catch (Exception e) {
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			throw new ConfigurationException("failed to initialize endpoint '" + this.getName() + "'", e);
		}
	}

	/**
	 * Subclasses may override this method for custom initialization requirements.
	 */
	protected void initialize()  throws Exception {
	}

	protected void handleException(MessagingException exception) {
		if (this.errorHandler == null) {
			if (this.logger.isWarnEnabled()) {
				this.logger.warn("exception occurred in endpoint '" + this.name + "'", exception);
			}
			throw exception;
		}
		this.errorHandler.handle(exception);
	}

	protected final void configureTransactionSettingsForPoller(AbstractPoller poller) {
		if (this.transactionManager != null) {
			poller.setTransactionManager(this.transactionManager);
		}
		if (this.transactionDefinition != null) {
			poller.setTransactionDefinition(this.transactionDefinition);
		}
	}

	public String toString() {
		return (this.name != null) ? this.name : super.toString();
	}

}
