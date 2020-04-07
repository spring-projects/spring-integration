/*
 * Copyright 2014-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A {@link RejectedExecutionHandler} that blocks the caller until
 * the executor has room in its queue, or a timeout occurs (in which
 * case a {@link RejectedExecutionException} is thrown.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 3.0.3
 *
 */
public class CallerBlocksPolicy implements RejectedExecutionHandler {

	private static final Log LOGGER = LogFactory.getLog(CallerBlocksPolicy.class);

	private final long maxWait;

	/**
	 * Construct instance based on the provided maximum wait time.
	 * @param maxWait The maximum time to wait for a queue slot to be available, in milliseconds.
	 */
	public CallerBlocksPolicy(long maxWait) {
		this.maxWait = maxWait;
	}

	@Override
	public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
		if (!executor.isShutdown()) {
			try {
				BlockingQueue<Runnable> queue = executor.getQueue();
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Attempting to queue task execution for " + this.maxWait + " milliseconds");
				}
				if (!queue.offer(r, this.maxWait, TimeUnit.MILLISECONDS)) {
					throw new RejectedExecutionException("Max wait time expired to queue task");
				}
				LOGGER.debug("Task execution queued");
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RejectedExecutionException("Interrupted", e);
			}
		}
		else {
			throw new RejectedExecutionException("Executor has been shut down");
		}
	}

}
