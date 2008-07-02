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

package org.springframework.integration.channel;

import org.springframework.integration.dispatcher.MessageDispatcher;
import org.springframework.util.Assert;

/**
 * Metadata for a {@link MessageDispatcher}.
 * 
 * @author Mark Fisher
 */
public class DispatcherPolicy {

	public final static int DEFAULT_REJECTION_LIMIT = 5;

	public final static long DEFAULT_RETRY_INTERVAL = 1000;


	private final boolean publishSubscribe;

	private volatile int rejectionLimit = DEFAULT_REJECTION_LIMIT;

	private volatile long retryInterval = DEFAULT_RETRY_INTERVAL;

	private volatile boolean shouldFailOnRejectionLimit = true;


	public DispatcherPolicy() {
		this.publishSubscribe = false;
	}

	/**
	 * Create a DispatcherPolicy.
	 * 
	 * @param publishSubscribe whether the dispatcher should attempt to publish
	 * to all of its subscribed handlers. If '<code>false</code>' it will attempt
	 * to send to a single handler (point-to-point).
	 */
	public DispatcherPolicy(boolean publishSubscribe) {
		this.publishSubscribe = publishSubscribe;
	}


	/**
	 * Return whether the dispatcher should attempt to publish to all of its handlers.
	 * This property is immutable.
	 */
	public boolean isPublishSubscribe() {
		return this.publishSubscribe;
	}

	/**
	 * Return the maximum number of retries upon rejection. 
	 */
	public int getRejectionLimit() {
		return this.rejectionLimit;
	}

	/**
	 * Set the maximum number of retries upon rejection. 
	 */
	public void setRejectionLimit(int rejectionLimit) {
		Assert.isTrue(rejectionLimit > 0, "'rejectionLimit' must be at least 1");
		this.rejectionLimit = rejectionLimit;
	}

	/**
	 * Return the amount of time in milliseconds to wait between rejections.
	 */
	public long getRetryInterval() {
		return this.retryInterval;
	}

	/**
	 * Set the amount of time in milliseconds to wait between rejections.
	 */
	public void setRetryInterval(long retryInterval) {
		Assert.isTrue(retryInterval >= 0, "'retryInterval' must not be negative");
		this.retryInterval = retryInterval;
	}

	/**
	 * Return whether an exception should be thrown when this dispatcher's
	 * {@link #rejectionLimit} is reached.
	 */
	public boolean getShouldFailOnRejectionLimit() {
		return this.shouldFailOnRejectionLimit;
	}

	/**
	 * Specify whether an exception should be thrown when this dispatcher's
	 * {@link #rejectionLimit} is reached. The default value is 'true'.
	 */
	public void setShouldFailOnRejectionLimit(boolean shouldFailOnRejectionLimit) {
		this.shouldFailOnRejectionLimit = shouldFailOnRejectionLimit;
	}

}
