/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.bus;

import java.util.concurrent.TimeUnit;

/**
 * A container for Message consumer configuration metadata.
 * 
 * @author Mark Fisher
 */
public class ConsumerPolicy {

	private static final int DEFAULT_CONCURRENCY = 1;

	private static final int DEFAULT_MAX_CONCURRENCY = 10;

	private static final int DEFAULT_MAX_MESSAGES_PER_TASK = 10;

	private static final int DEFAULT_REJECTION_LIMIT = 10;

	private static final int DEFAULT_REJECTION_LIMIT_WAIT = 1000;

	private static final long DEFAULT_RECEIVE_TIMEOUT = 1000;


	private int concurrency = DEFAULT_CONCURRENCY;

	private int maxConcurrency = DEFAULT_MAX_CONCURRENCY;

	private int maxMessagesPerTask = DEFAULT_MAX_MESSAGES_PER_TASK;

	private int rejectionLimit = DEFAULT_REJECTION_LIMIT;

	private int rejectionLimitWait = DEFAULT_REJECTION_LIMIT_WAIT;

	private int initialDelay = 0;

	private int period = -1;

	private TimeUnit timeUnit = TimeUnit.MILLISECONDS;

	private boolean fixedRate = false;

	private long receiveTimeout = DEFAULT_RECEIVE_TIMEOUT;


	public int getInitialDelay() {
		return this.initialDelay;
	}

	public void setInitialDelay(int initialDelay) {
		this.initialDelay = initialDelay;
	}

	public int getPeriod() {
		return this.period;
	}

	public void setPeriod(int period) {
		this.period = period;
	}

	public TimeUnit getTimeUnit() {
		return this.timeUnit;
	}

	public void setTimeUnit(TimeUnit timeUnit) {
		this.timeUnit = timeUnit;
	}

	public boolean isFixedRate() {
		return this.fixedRate;
	}

	public void setFixedRate(boolean fixedRate) {
		this.fixedRate = fixedRate;
	}

	public int getConcurrency() {
		return this.concurrency;
	}

	public void setConcurrency(int concurrency) {
		if (concurrency < 1) {
			throw new IllegalArgumentException("'concurrency' value must be at least 1");
		}
		this.concurrency = concurrency;
	}

	public int getMaxConcurrency() {
		return this.maxConcurrency;
	}

	public void setMaxConcurrency(int maxConcurrency) {
		if (maxConcurrency < 1) {
			throw new IllegalArgumentException("'maxConcurrency' value must be at least 1");
		}
		this.maxConcurrency = maxConcurrency;
	}

	public int getMaxMessagesPerTask() {
		return this.maxMessagesPerTask;
	}

	public void setMaxMessagesPerTask(int maxMessagesPerTask) {
		if (maxMessagesPerTask == 0) {
			throw new IllegalArgumentException("'maxMessagesPerTask' must not be 0");
		}
		this.maxMessagesPerTask = maxMessagesPerTask;
	}

	public int getRejectionLimit() {
		return this.rejectionLimit;
	}

	public void setRejectionLimit(int rejectionLimit) {
		if (rejectionLimit < 1) {
			throw new IllegalArgumentException("'idleTaskExecutionLimit' must be at least 1");
		}
		this.rejectionLimit = rejectionLimit;
	}

	public int getRejectionLimitWait() {
		return this.rejectionLimitWait;
	}

	public void setRejectionLimitWait(int rejectionLimitWait) {
		this.rejectionLimitWait = rejectionLimitWait;
	}

	public long getReceiveTimeout() {
		return this.receiveTimeout;
	}

	public void setReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

}
