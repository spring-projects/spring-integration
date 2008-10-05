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

import java.util.concurrent.ScheduledFuture;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.Lifecycle;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.scheduling.IntervalTrigger;
import org.springframework.integration.scheduling.TaskScheduler;
import org.springframework.integration.scheduling.TaskSchedulerAware;
import org.springframework.integration.scheduling.Trigger;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 */
public abstract class AbstractPollingEndpoint implements MessageEndpoint, TaskSchedulerAware, Lifecycle, InitializingBean {

	public static final int MAX_MESSAGES_UNBOUNDED = -1;


	private volatile Trigger trigger;

	protected volatile long maxMessagesPerPoll = MAX_MESSAGES_UNBOUNDED; 

	private volatile TaskExecutor taskExecutor;

	private volatile PlatformTransactionManager transactionManager;

	private volatile TransactionDefinition transactionDefinition;

	private volatile TransactionTemplate transactionTemplate;

	private volatile TaskScheduler taskScheduler;

	private volatile ScheduledFuture<?> runningTask;

	private volatile boolean initialized;

	private final Object lifecycleMonitor = new Object();


	public void setTrigger(Trigger trigger) {
		this.trigger = trigger;
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

	public void setTaskScheduler(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Specify a transaction manager to use for all polling operations.
	 * If none is provided, then the operations will occur without any
	 * transactional behavior (i.e. there is no default transaction manager).
	 */
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setTransactionDefinition(TransactionDefinition transactionDefinition) {
		this.transactionDefinition = transactionDefinition;
	}

	private TransactionTemplate getTransactionTemplate() {
		if (!this.initialized) {
			this.afterPropertiesSet();
		}
		return this.transactionTemplate;
	}

	public void afterPropertiesSet() {
		synchronized (this.lifecycleMonitor) {
			if (this.initialized) {
				return;
			}
			if (this.trigger == null) {
				this.trigger = new IntervalTrigger(0);
			}
			if (this.transactionManager != null) {
				if (this.transactionDefinition == null) {
					this.transactionDefinition = new DefaultTransactionDefinition();
				}
				this.transactionTemplate = new TransactionTemplate(
						this.transactionManager, this.transactionDefinition);
			}
			this.initialized = true;
		}
	}


	// Lifecycle implementation

	public boolean isRunning() {
		synchronized (this.lifecycleMonitor) {
			return this.runningTask != null;
		}
	}

	public void start() {
		synchronized (this.lifecycleMonitor) {
			if (!this.initialized) {
				this.afterPropertiesSet();
			}
			Assert.state(this.taskScheduler != null,
					"unable to start polling, no taskScheduler available");
			this.runningTask = this.taskScheduler.schedule(new Poller(), this.trigger);
		}
	}

	public void stop() {
		synchronized (this.lifecycleMonitor) {
			if (this.runningTask != null) {
				this.runningTask.cancel(true);
			}
			this.runningTask = null;
		}
	}


	protected abstract boolean doPoll();


	private class Poller implements Runnable {

		public void run() {
			if (taskExecutor != null) {
				taskExecutor.execute(new Runnable() {
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
			while (maxMessagesPerPoll < 0 || count < maxMessagesPerPoll) {
				if (!innerPoll()) {
					break;
				}
				count++;
			}
		}

		private boolean innerPoll() {
			TransactionTemplate txTemplate = getTransactionTemplate();
			if (txTemplate != null) {
				return (Boolean) txTemplate.execute(new TransactionCallback() {
					public Object doInTransaction(TransactionStatus status) {
						return doPoll();
					}
				});
			}
			return doPoll();
		}
	}

}
