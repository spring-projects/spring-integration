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

import org.springframework.context.Lifecycle;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageReceiver;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.util.Assert;

/**
 * Encapsulates a {@link ThreadPoolExecutor} with configurable error thresholds.
 * 
 * @author Mark Fisher
 */
public class MessageReceivingExecutor implements Lifecycle {

	private Log logger = LogFactory.getLog(this.getClass());

	private MessageReceiver receiver;

	private ThreadPoolExecutor threadPoolExecutor;

	private int corePoolSize;

	private int maxPoolSize;

	private volatile boolean running;

	private Object lifecycleMonitor = new Object();

	private int successiveErrorCount;

	private int successiveErrorThreshold = -1;

	private int totalErrorCount;

	private int totalErrorThreshold = -1;


	public MessageReceivingExecutor(MessageReceiver receiver, int corePoolSize, int maxPoolSize) {
		Assert.notNull(receiver, "'receiver' must not be null");
		Assert.isTrue(corePoolSize > 0, "'corePoolSize' must be at least 1");
		Assert.isTrue(maxPoolSize > 0, "'maxPoolSize' must be at least 1");
		Assert.isTrue(maxPoolSize >= corePoolSize, "'corePoolSize' cannot exceed 'maxPoolSize'");
		this.receiver = receiver;
		this.corePoolSize = corePoolSize;
		this.maxPoolSize = maxPoolSize;
	}

	public void setCorePoolSize(int corePoolSize) {
		this.corePoolSize = corePoolSize;
	}

	public void setMaxPoolSize(int maxPoolSize) {
		this.maxPoolSize = maxPoolSize;
	}

	public boolean isRunning() {
		return this.running;
	}

	public void start() {
		synchronized (this.lifecycleMonitor) {
			if (!this.running) {
				this.threadPoolExecutor = new MessageReceivingThreadPoolExecutor(this.corePoolSize, this.maxPoolSize);
			}
			this.running = true;
		}
	}

	public void stop() {
		synchronized (this.lifecycleMonitor) {
			if (this.isRunning()) {
				this.threadPoolExecutor.shutdown();
				this.threadPoolExecutor = null;
			}
			this.running = false;
		}
	}

	public void processMessage(Message<?> message) {
		if (threadPoolExecutor == null) {
			throw new MessageHandlingException("executor is not running");
		}
		this.threadPoolExecutor.execute(new MessageReceivingTask(this.receiver, message));
	}

	/**
	 * Set the maximum number of errors allowed in <em>successive</em>
	 * executions. If this threshold is ever exceeded, the executor
	 * will shutdown.
	 */
	public void setSuccessiveErrorThreshold(int successiveErrorThreshold) {
		this.successiveErrorThreshold = successiveErrorThreshold;
	}

	/**
	 * Set the maximum number of <em>total</em> errors allowed in executions
	 * If this threshold is ever exceeded, the executor will shutdown.
	 */
	public void setTotalErrorThreshold(int totalErrorThreshold) {
		this.totalErrorThreshold = totalErrorThreshold;
	}

	public int getActiveCount() {
		if (this.threadPoolExecutor == null) {
			return 0;
		}
		return this.threadPoolExecutor.getActiveCount();
	}

	public boolean isShutdown() {
		return this.threadPoolExecutor.isShutdown();
	}


	private static class MessageReceivingTask implements Runnable {

		private MessageReceiver receiver;

		private Message<?> message;

		private Throwable error;


		MessageReceivingTask(MessageReceiver receiver, Message<?> message) {
			this.receiver = receiver;
			this.message = message;
		}

		public Throwable getError() {
			return this.error;
		}

		public void run() {
			try {
				this.receiver.messageReceived(this.message);
			}
			catch (Throwable t) {
				this.error = t;
			}
		}
	}


	private class MessageReceivingThreadPoolExecutor extends ThreadPoolExecutor {

		public MessageReceivingThreadPoolExecutor(int corePoolSize, int maximumPoolSize) {
			super(corePoolSize, maximumPoolSize, 0, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>());
			CustomizableThreadFactory threadFactory = new CustomizableThreadFactory();
			threadFactory.setThreadNamePrefix("endpoint-executor-");
			this.setThreadFactory(threadFactory);
		}

		@Override
		protected void afterExecute(Runnable r, Throwable t) {
			MessageReceivingTask task = (MessageReceivingTask) r;
			if (task.getError() != null) {
				if (logger.isWarnEnabled()) {
					logger.warn("Exception occurred during task execution", task.getError());
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
