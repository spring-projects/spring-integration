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

import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.MessageRejectedException;
import org.springframework.integration.message.MessageTarget;
import org.springframework.util.Assert;

/**
 * Basic implementation of {@link MessageDispatcher} that will attempt
 * to send a {@link Message} to one of its targets (the first that accepts).
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
		if (this.targets.size() == 0) {
			if (logger.isWarnEnabled()) {
				logger.warn("Dispatcher has no targets.");
			}
			return false;
		}
		int attempts = 0;
		MessageHandlingException lastException = null;
		while (attempts < this.rejectionLimit) {
			Iterator<MessageTarget> iter = new ArrayList<MessageTarget>(this.targets).iterator();
			if (!iter.hasNext()) {
				return false;
			}
			if (attempts > 0) {
				try {
					this.waitBetweenAttempts(attempts);
				}
				catch (InterruptedException iex) {
					Thread.currentThread().interrupt();
					return false;
				}
			}
			lastException = sendMessageToFirstAcceptingTarget(message, iter);
			if (lastException == null) {
				return true;
			}
			attempts++;
		}
		if (this.shouldFailOnRejectionLimit) {
			throw new MessageDeliveryException(message, "Dispatcher reached rejection limit of " + this.rejectionLimit, lastException);
		}
		return false;
	}

	private MessageHandlingException sendMessageToFirstAcceptingTarget(Message<?> message, Iterator<MessageTarget> iter) {
		MessageHandlingException lastException = null;
		int count = 0;
		int rejectedExceptionCount = 0;
		while (iter.hasNext()) {
			count++;
			MessageTarget target = iter.next();
			try {
				if (this.sendMessageToTarget(message, target)) {
					return null;
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Failed to send message to target, continuing with other targets if available.");
				}
			}
			catch (MessageRejectedException e) {
				rejectedExceptionCount++;
				if (logger.isDebugEnabled()) {
					logger.debug("Target '" + target + "' rejected Message, continuing with other targets if available.", e);
				}
			}
			catch (MessageHandlingException e) {
				lastException = e;
				if (logger.isDebugEnabled()) {
					logger.debug("Target '" + target + "' threw an exception, continuing with other targets if available.", e);
				}
			}
		}
		if (rejectedExceptionCount == count) {
			throw new MessageRejectedException(message, "All of dispatcher's targets rejected Message.");
		}
		return lastException;
	}

	private void waitBetweenAttempts(int attempts) throws InterruptedException {
		if (logger.isDebugEnabled()) {
			logger.debug("target(s) unable to handle message after " + attempts +
					" attempt(s), will try again after 'retryInterval' of " +
					this.retryInterval + " milliseconds");
		}
		Thread.sleep(this.retryInterval);
	}

}
