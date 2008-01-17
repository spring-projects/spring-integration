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

package org.springframework.integration.dispatcher;

import org.springframework.util.Assert;

/**
 * Metadata for a {@link MessageDispatcher}.
 * 
 * @author Mark Fisher
 */
public class DispatcherPolicy {

	public final static int DEFAULT_MAX_MESSAGES_PER_TASK = 1;

	public final static long DEFAULT_RECEIVE_TIMEOUT = 1000;

	public final static int DEFAULT_REJECTION_LIMIT = 5;

	public final static long DEFAULT_RETRY_INTERVAL = 1000;


	private int maxMessagesPerTask = DEFAULT_MAX_MESSAGES_PER_TASK;

	private long receiveTimeout = DEFAULT_RECEIVE_TIMEOUT;

	private int rejectionLimit = DEFAULT_REJECTION_LIMIT;

	private long retryInterval = DEFAULT_RETRY_INTERVAL;


	public int getMaxMessagesPerTask() {
		return this.maxMessagesPerTask;
	}

	public void setMaxMessagesPerTask(int maxMessagesPerTask) {
		Assert.isTrue(maxMessagesPerTask > 0, "'maxMessagePerTask' must be at least 1");
		this.maxMessagesPerTask = maxMessagesPerTask;
	}

	public long getReceiveTimeout() {
		return this.receiveTimeout;
	}

	public void setReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	public int getRejectionLimit() {
		return this.rejectionLimit;
	}

	public void setRejectionLimit(int rejectionLimit) {
		Assert.isTrue(rejectionLimit > 0, "'rejectionLimit' must be at least 1");
		this.rejectionLimit = rejectionLimit;
	}

	public long getRetryInterval() {
		return this.retryInterval;
	}

	public void setRetryInterval(long retryInterval) {
		Assert.isTrue(retryInterval >= 0, "'retryInterval' must not be negative");
		this.retryInterval = retryInterval;
	}

}
