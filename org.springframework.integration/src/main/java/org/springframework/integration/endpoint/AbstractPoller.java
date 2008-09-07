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

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.scheduling.SchedulableTask;
import org.springframework.integration.scheduling.Schedule;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 */
public abstract class AbstractPoller implements SchedulableTask, InitializingBean {

	public static final int MAX_MESSAGES_UNBOUNDED = -1;


	private final Schedule schedule;

	private volatile long maxMessagesPerPoll = MAX_MESSAGES_UNBOUNDED; 

	private volatile TaskExecutor taskExecutor;

	private volatile PlatformTransactionManager transactionManager;

	private volatile TransactionTemplate transactionTemplate;

	private volatile int propagationBehavior = DefaultTransactionDefinition.PROPAGATION_REQUIRED;

	private volatile int isolationLevel = DefaultTransactionDefinition.ISOLATION_DEFAULT;

	private volatile int transactionTimeout = DefaultTransactionDefinition.TIMEOUT_DEFAULT;

	private volatile boolean readOnly = false;

	private volatile boolean initialized;

	private final Object initializationMonitor = new Object();


	public AbstractPoller(Schedule schedule) {
		Assert.notNull(schedule, "schedule must not be null");
		this.schedule = schedule;
	}


	public Schedule getSchedule() {
		return this.schedule;
	}

	/**
	 * Set the maximum number of messages to receive for each poll.
	 * A non-positive value indicates that polling should repeat as long
	 * as non-null messages are being received and successfully sent.
	 * 
	 * <p>The default is unbounded.
	 * 
	 * @see #MAX_MESSAGES_UNBOUNDED
	 */
	public void setMaxMessagesPerPoll(int maxMessagesPerPoll) {
		this.maxMessagesPerPoll = maxMessagesPerPoll;
	}

	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Specify a transaction manager to use for all exchange operations.
	 * If none is provided, then the operations will occur without any
	 * transactional behavior (i.e. there is no default transaction manager).
	 */
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setPropagationBehavior(int propagationBehavior) {
		this.propagationBehavior = propagationBehavior;
	}

	public void setIsolationLevel(int isolationLevel) {
		this.isolationLevel = isolationLevel;
	}

	public void setTransactionTimeout(int transactionTimeout) {
		this.transactionTimeout = transactionTimeout;
	}

	public void setTransactionReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	private TransactionTemplate getTransactionTemplate() {
		if (!this.initialized) {
			this.afterPropertiesSet();
		}
		return this.transactionTemplate;
	}

	public void afterPropertiesSet() {
		synchronized (this.initializationMonitor) {
			if (this.initialized) {
				return;
			}
			if (this.transactionManager != null) {
				TransactionTemplate template = new TransactionTemplate(this.transactionManager);
				template.setPropagationBehavior(this.propagationBehavior);
				template.setIsolationLevel(this.isolationLevel);
				template.setTimeout(this.transactionTimeout);
				template.setReadOnly(this.readOnly);
				this.transactionTemplate = template;
			}
			this.initialized = true;
		}
	}

	public void run() {
		if (this.taskExecutor != null) {
			this.taskExecutor.execute(new Runnable() {
				public void run() {
					poll();
				}
			});
		}
		else {
			poll();
		}
	}

	private void poll() {
		int count = 0;
		while (this.maxMessagesPerPoll < 0 || count < this.maxMessagesPerPoll) {
			if (!this.pollWithinTransaction()) {
				break;
			}
			count++;
		}
	}

	private boolean pollWithinTransaction() {
		TransactionTemplate txTemplate = this.getTransactionTemplate();
		if (txTemplate != null) {
			return (Boolean) txTemplate.execute(new TransactionCallback() {
				public Object doInTransaction(TransactionStatus status) {
					return doPoll();
				}
			});
		}
		return doPoll();
	}

	protected abstract boolean doPoll();

}
