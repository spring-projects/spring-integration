/*
 * Copyright 2002-2010 the original author or authors.
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

import java.util.List;

import org.aopalliance.aop.Advice;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.PeriodicTrigger;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class PollerMetadata {

	public static final int MAX_MESSAGES_UNBOUNDED = -1;
	
	private volatile Trigger trigger = new PeriodicTrigger(10);

	private volatile long maxMessagesPerPoll = MAX_MESSAGES_UNBOUNDED;

	private volatile long receiveTimeout = 1000;

	private List<Advice> adviceChain;

	private volatile TaskExecutor taskExecutor;

	public void setTrigger(Trigger trigger) {
		this.trigger = trigger;
	}

	public Trigger getTrigger() {
		return this.trigger;
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
	public void setMaxMessagesPerPoll(long maxMessagesPerPoll) {
		this.maxMessagesPerPoll = maxMessagesPerPoll;
	}

	public long getMaxMessagesPerPoll() {
		return this.maxMessagesPerPoll;
	}

	public void setReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	public long getReceiveTimeout() {
		return this.receiveTimeout;
	}

	public void setAdviceChain(List<Advice> adviceChain) {
		this.adviceChain = adviceChain;
	}

	public List<Advice> getAdviceChain() {
		return this.adviceChain;
	}

	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	public TaskExecutor getTaskExecutor() {
		return this.taskExecutor;
	}
}
