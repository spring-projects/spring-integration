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
import java.util.concurrent.Executor;

import org.aopalliance.aop.Advice;
import org.springframework.integration.endpoint.PollerCallbackDecorator;
import org.springframework.scheduling.Trigger;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class PollerMetadata {

	private volatile Trigger trigger;

	private volatile int maxMessagesPerPoll;

	private volatile long receiveTimeout = 1000;

	private List<Advice> adviceChain;

	private volatile Executor taskExecutor;
	
	private PollerCallbackDecorator pollingDecorator;

	public PollerCallbackDecorator getPollingDecorator() {
		return pollingDecorator;
	}

	public void setPollingDecorator(PollerCallbackDecorator pollingDecorator) {
		this.pollingDecorator = pollingDecorator;
	}

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

	public void setAdviceChain(List<Advice> adviceChain) {
		this.adviceChain = adviceChain;
	}

	public List<Advice> getAdviceChain() {
		return this.adviceChain;
	}

	public void setTaskExecutor(Executor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	public Executor getTaskExecutor() {
		return this.taskExecutor;
	}
}
