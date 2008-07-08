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

package org.springframework.integration.dispatcher;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.integration.handler.MessageHandlerNotRunningException;
import org.springframework.integration.handler.MessageHandlerRejectedExecutionException;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.MessageTarget;
import org.springframework.util.Assert;

/**
 * Basic implementation of {@link MessageDispatcher}.
 * 
 * @author Mark Fisher
 */
public class SimpleDispatcher extends AbstractDispatcher {

	public final static int DEFAULT_REJECTION_LIMIT = 1;

	public final static long DEFAULT_RETRY_INTERVAL = 1000;


	private volatile int rejectionLimit = DEFAULT_REJECTION_LIMIT;

	private volatile long retryInterval = DEFAULT_RETRY_INTERVAL;

	private volatile boolean shouldFailOnRejectionLimit = true;


	/**
	 * Set the maximum number of retries upon rejection. 
	 */
	public void setRejectionLimit(int rejectionLimit) {
		Assert.isTrue(rejectionLimit > 0, "'rejectionLimit' must be at least 1");
		this.rejectionLimit = rejectionLimit;
	}

	/**
	 * Set the amount of time in milliseconds to wait between rejections.
	 */
	public void setRetryInterval(long retryInterval) {
		Assert.isTrue(retryInterval >= 0, "'retryInterval' must not be negative");
		this.retryInterval = retryInterval;
	}

	/**
	 * Specify whether an exception should be thrown when this dispatcher's
	 * {@link #rejectionLimit} is reached. The default value is 'true'.
	 */
	public void setShouldFailOnRejectionLimit(boolean shouldFailOnRejectionLimit) {
		this.shouldFailOnRejectionLimit = shouldFailOnRejectionLimit;
	}

	public boolean send(Message<?> message) {
		int attempts = 0;
		List<MessageTarget> targetList = new ArrayList<MessageTarget>(this.targets);
		while (attempts < this.rejectionLimit) {
			if (attempts > 0) {
				if (logger.isDebugEnabled()) {
					logger.debug("target(s) rejected message after " + attempts +
							" attempt(s), will try again after 'retryInterval' of " +
							this.retryInterval + " milliseconds");
				}
				try {
					Thread.sleep(this.retryInterval);
				}
				catch (InterruptedException iex) {
					Thread.currentThread().interrupt();
					return false;
				}
			}
			Iterator<MessageTarget> iter = targetList.iterator();
			if (!iter.hasNext()) {
				if (logger.isWarnEnabled()) {
					logger.warn("no active targets");
				}
				return false;
			}
			boolean rejected = false;
			while (iter.hasNext()) {
				MessageTarget target = iter.next();
				try {
					if (this.sendMessageToTarget(message, target)) {
						return true;
					}
					if (logger.isDebugEnabled()) {
						logger.debug("target rejected message, continuing with other targets if available");
					}
					iter.remove();
				}
				catch (MessageHandlerNotRunningException e) {
					if (logger.isDebugEnabled()) {
						logger.debug("target is not running, continuing with other targets if available", e);
					}
				}
				catch (MessageHandlerRejectedExecutionException e) {
					rejected = true;
					if (logger.isDebugEnabled()) {
						logger.debug("target '" + target + "' is busy, continuing with other targets if available", e);
					}
				}
			}
			if (!rejected) {
				return true;
			}
			attempts++;
		}
		if (this.shouldFailOnRejectionLimit) {
			throw new MessageDeliveryException(message, "Dispatcher reached rejection limit of "
					+ this.rejectionLimit
					+ ". Consider increasing the target's concurrency and/or "
					+ "the dispatcherPolicy's 'rejectionLimit'.");
		}
		return false;
	}

}
