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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.Message;
import org.springframework.util.Assert;

/**
 * The base implementation of a polling {@link MessageDispatcher}. If
 * {@link #broadcast} is set to <code>false</code> (the default), each message
 * will be sent to a single {@link MessageHandler}. Otherwise, each
 * retrieved {@link Message} will be sent to all handlers.
 * 
 * @author Mark Fisher
 */
public class BasePollingMessageDispatcher extends AbstractMessageDispatcher {

	private boolean broadcast = false;

	private int rejectionLimit = 5;

	private long retryInterval = 1000;

	private boolean shouldFailOnRejectionLimit = true;


	public BasePollingMessageDispatcher(MessageRetriever retriever) {
		super(retriever);
	}


	public void setBroadcast(boolean broadcast) {
		this.broadcast = broadcast;
	}

	public void setRejectionLimit(int rejectionLimit) {
		Assert.isTrue(rejectionLimit > 0, "'rejectionLimit' must be at least 1");
		this.rejectionLimit = rejectionLimit;
	}

	public void setRetryInterval(long retryInterval) {
		Assert.isTrue(retryInterval > 0, "'retryInterval' must not be negative");
		this.retryInterval = retryInterval;
	}

	/**
	 * Specify whether an exception should be thrown when this dispatcher's
	 * {@link #rejectionLimit} is reached. The default value is 'true'.
	 */
	public void setShouldFailOnRejectionLimit(boolean shouldFailOnRejectionLimit) {
		this.shouldFailOnRejectionLimit = shouldFailOnRejectionLimit;
	}

	@Override
	protected boolean dispatchMessage(Message<?> message) {
		int attempts = 0;
		List<MessageHandler> targets = new ArrayList<MessageHandler>(this.getHandlers());
		while (attempts < this.rejectionLimit) {
			if (attempts > 0) {
				if (logger.isDebugEnabled()) {
					logger.debug("handler(s) rejected message after " + attempts
							+ " attempt(s), will try again after 'retryInterval' of " + this.retryInterval
							+ " milliseconds");
				}
				try {
					Thread.sleep(this.retryInterval);
				}
				catch (InterruptedException iex) {
					Thread.currentThread().interrupt();
					return false;
				}
			}
			Iterator<MessageHandler> iter = targets.iterator();
			if (!iter.hasNext()) {
				if (logger.isWarnEnabled()) {
					logger.warn("dispatcher has no active handlers");
				}
				return false;
			}
			boolean encounteredHandlerException = false;
			while (iter.hasNext()) {
				MessageHandler handler = iter.next();
				try {
					handler.handle(message);
					if (!this.broadcast) {
						return true;
					}
					iter.remove();
				}
				catch (MessageSelectorRejectedException e) {
					if (logger.isDebugEnabled()) {
						logger.debug("selector rejected task, continuing with other handlers if available", e);
					}
				}
				catch (MessageHandlerNotRunningException e) {
					if (logger.isDebugEnabled()) {
						logger.debug("handler not running, continuing with other handlers if available", e);
					}					
				}
				catch (MessageHandlingException e) {
					encounteredHandlerException = true;
					if (logger.isDebugEnabled()) {
						logger.debug("handler threw exception, continuing with other handlers if available", e);
					}
				}
			}
			if (!encounteredHandlerException) {
				return true;
			}
			attempts++;
		}
		if (this.shouldFailOnRejectionLimit) {
			throw new MessageDeliveryException("Dispatcher reached rejection limit of " + this.rejectionLimit
					+ ". Consider increasing the executor's concurrency and/or raising the 'rejectionLimit'.");
		}
		return false;
	}

}
