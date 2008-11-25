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

package org.springframework.integration.scheduling;

import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

/**
 * @author Mark Fisher
 */
public class PollerMetadata {

	private volatile Trigger trigger;

	private volatile int maxMessagesPerPoll;

	private volatile long receiveTimeout = 1000;

	private volatile TaskExecutor taskExecutor;

	private volatile PlatformTransactionManager transactionManager;

	private volatile TransactionDefinition transactionDefinition;


	public void setTrigger(Trigger trigger) {
		this.trigger = trigger;
	}

	public Trigger getTrigger() {
		return this.trigger;
	}

	public void setMaxMessagesPerPoll(int maxMessagesPerPoll) {
		this.maxMessagesPerPoll = maxMessagesPerPoll;
	}

	public int getMaxMessagesPerPoll() {
		return this.maxMessagesPerPoll;
	}

	public void setReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	public long getReceiveTimeout() {
		return this.receiveTimeout;
	}

	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	public TaskExecutor getTaskExecutor() {
		return this.taskExecutor;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public PlatformTransactionManager getTransactionManager() {
		return this.transactionManager;
	}

	public void setTransactionDefinition(TransactionDefinition transactionDefinition) {
		this.transactionDefinition = transactionDefinition;
	}

	public TransactionDefinition getTransactionDefinition() {
		return this.transactionDefinition;
	}

}
