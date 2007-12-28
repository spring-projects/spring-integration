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


	@Override
	protected boolean dispatchMessage(Message<?> message) {
		int attempts = 0;
		Iterator<MessageReceivingExecutor> iter = this.getExecutors().iterator();
		if (!iter.hasNext()) {
			if (logger.isWarnEnabled()) {
				logger.warn("dispatcher has no active executors");
			}
			return false;
		}
		while (iter.hasNext()) {
			MessageReceivingExecutor executor = iter.next();
			try {
				if (executor == null || !executor.isRunning()) {
					if (logger.isInfoEnabled()) {
						logger.info("removing inactive executor");
					}
					iter.remove();
					continue;
				}
				executor.processMessage(message);
				return true;
			}
			catch (RejectedExecutionException rex) {
				attempts++;
				if (attempts == policy.getRejectionLimit()) {
					attempts = 0;
					if (logger.isDebugEnabled()) {
						logger.debug("reached rejected execution limit");
					}
					try {
						Thread.sleep(policy.getRejectionLimitWait());
					}
					catch (InterruptedException iex) {
						Thread.currentThread().interrupt();
					}
				}
			}
			catch (Exception e) {
				if (logger.isWarnEnabled()) {
					logger.warn("error occurred during dispatch", e);
				}
			}
		}
		return false;
	}

}
