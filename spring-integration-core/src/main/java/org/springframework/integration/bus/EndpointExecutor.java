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

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.endpoint.MessageEndpoint;
import org.springframework.integration.message.Message;
import org.springframework.util.Assert;

/**
 * Encapsulates a {@link ThreadPoolExecutor} with configurable error thresholds.
 * 
 * @author Mark Fisher
 */
public class EndpointExecutor {

	private Log logger = LogFactory.getLog(this.getClass());

	private MessageEndpoint endpoint;

	private ThreadPoolExecutor threadPoolExecutor;

	private int successiveErrorCount;

	private int successiveErrorThreshold = -1;

	private int totalErrorCount;

	private int totalErrorThreshold = -1;


	public EndpointExecutor(MessageEndpoint endpoint, int corePoolSize, int maximumPoolSize) {
		Assert.notNull(endpoint, "'endpoint' must not be null");
		this.endpoint = endpoint;
		this.threadPoolExecutor = new EndpointThreadPoolExecutor(corePoolSize, maximumPoolSize);
	}

	public void executeTask(Message<?> message) {
		this.threadPoolExecutor.execute(new EndpointTask(this.endpoint, message));
	}

	/**
	 * Set the maximum number of errors allowed in <em>successive</em>
	 * endpoint executions. If this threshold is ever exceeded, the executor
	 * will shutdown.
	 */
	public void setSuccessiveErrorThreshold(int successiveErrorThreshold) {
		this.successiveErrorThreshold = successiveErrorThreshold;
	}

	/**
	 * Set the maximum number of <em>total</em> errors allowed in endpoint
	 * executions. If this threshold is ever exceeded, the executor will
	 * shutdown.
	 */
	public void setTotalErrorThreshold(int totalErrorThreshold) {
		this.totalErrorThreshold = totalErrorThreshold;
	}

	public int getActiveCount() {
		return this.threadPoolExecutor.getActiveCount();
	}

	public boolean isShutdown() {
		return this.threadPoolExecutor.isShutdown();
	}


	private static class EndpointTask implements Runnable {

		private MessageEndpoint endpoint;

		private Message<?> message;

		private Throwable error;


		EndpointTask(MessageEndpoint endpoint, Message<?> message) {
			this.endpoint = endpoint;
			this.message = message;
		}

		public Throwable getError() {
			return this.error;
		}

		public void run() {
			try {
				this.endpoint.messageReceived(this.message);
			}
			catch (Throwable t) {
				this.error = t;
			}
		}
	}


	private class EndpointThreadPoolExecutor extends ThreadPoolExecutor {

		public EndpointThreadPoolExecutor(int corePoolSize, int maximumPoolSize) {
			super(corePoolSize, maximumPoolSize, 0, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>());
		}

		@Override
		protected void afterExecute(Runnable r, Throwable t) {
			EndpointTask task = (EndpointTask) r;
			if (task.getError() != null) {
				if (logger.isWarnEnabled()) {
					logger.warn("Exception occurred in endpoint execution", task.getError());
				}
				successiveErrorCount++;
				totalErrorCount++;
				if ((successiveErrorThreshold >= 0 && successiveErrorCount > successiveErrorThreshold)
						|| (totalErrorThreshold >= 0 && totalErrorCount > totalErrorThreshold)) {
					if (logger.isInfoEnabled()) {
						logger.info("error threshold exceeded, shutting down now");
					}
					this.shutdownNow();
				}
			}
			else {
				successiveErrorCount = 0;
			}
		}
	}

}
