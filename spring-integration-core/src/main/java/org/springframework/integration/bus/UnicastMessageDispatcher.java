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

import java.util.Iterator;
import java.util.concurrent.RejectedExecutionException;

import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.message.Message;

/**
 * A {@link MessageDispatcher} implementation that dispatches each retrieved
 * {@link Message} to a single {@link MessageReceivingExecutor}.
 * 
 * @author Mark Fisher
 */
public class UnicastMessageDispatcher extends AbstractMessageDispatcher {

	private ConsumerPolicy policy;


	public UnicastMessageDispatcher(MessageRetriever retriever, ConsumerPolicy policy) {
		super(retriever);
		this.policy = policy;
	}


	public ConsumerPolicy getConsumerPolicy() {
		return this.policy;
	}

	@Override
	protected boolean dispatchMessage(Message<?> message) {
		int attempts = 0;
		while (attempts < policy.getRejectionLimit()) {
			if (attempts > 0) {
				if (logger.isDebugEnabled()) {
					logger.debug("executor(s) rejected message after " + attempts
							+ " attempt(s), will try again after 'retryInterval' of " + this.policy.getRetryInterval()
							+ " milliseconds");
				}
				try {
					Thread.sleep(policy.getRetryInterval());
				}
				catch (InterruptedException iex) {
					Thread.currentThread().interrupt();
					return false;
				}
			}
			Iterator<MessageReceivingExecutor> iter = this.getExecutors().iterator();
			if (!iter.hasNext()) {
				if (logger.isWarnEnabled()) {
					logger.warn("dispatcher has no active executors");
				}
				return false;
			}
			while (iter.hasNext()) {
				MessageReceivingExecutor executor = iter.next();
				if (executor == null || !executor.isRunning()) {
					if (logger.isInfoEnabled()) {
						logger.info("skipping inactive executor");
					}
					continue;
				}
				try {
					executor.processMessage(message);
					return true;
				}
				catch (RejectedExecutionException rex) {
					if (logger.isDebugEnabled()) {
						logger.debug("executor rejected task, continuing with other executors if available", rex);
					}
				}
			}
			attempts++;
		}
		throw new MessageDeliveryException("Dispatcher reached rejection limit of " +
				this.policy.getRejectionLimit() + ". Consider increasing the concurrency and/or raising the limit.");
	}

}
