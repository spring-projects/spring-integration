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

package org.springframework.integration.handler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.Lifecycle;
import org.springframework.integration.dispatcher.MessageHandlerNotRunningException;
import org.springframework.integration.dispatcher.MessageHandlerRejectedExecutionException;
import org.springframework.integration.message.Message;
import org.springframework.integration.util.ErrorHandler;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.Assert;

/**
 * A {@link MessageHandler} implementation that encapsulates a
 * {@link ThreadPoolTaskExecutor} and delegates to a wrapped handler for
 * concurrent, asynchronous message handling.
 * 
 * @author Mark Fisher
 */
public class ConcurrentHandler implements MessageHandler, Lifecycle, InitializingBean {

	private Log logger = LogFactory.getLog(this.getClass());

	private MessageHandler handler;

	private ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

	private int corePoolSize = 1;

	private int maxPoolSize = 5;

	private int queueCapacity = 0;

	private int keepAliveSeconds = 60;

	private ErrorHandler errorHandler;

	private volatile boolean running;

	private Object lifecycleMonitor = new Object();


	public ConcurrentHandler(MessageHandler handler) {
		Assert.notNull(handler, "'handler' must not be null");
		this.handler = handler;
	}

	public ConcurrentHandler(MessageHandler handler, int corePoolSize, int maxPoolSize) {
		Assert.notNull(handler, "'handler' must not be null");
		Assert.isTrue(corePoolSize > 0, "'corePoolSize' must be at least 1");
		Assert.isTrue(maxPoolSize > 0, "'maxPoolSize' must be at least 1");
		Assert.isTrue(maxPoolSize >= corePoolSize, "'corePoolSize' cannot exceed 'maxPoolSize'");
		this.handler = handler;
		this.corePoolSize = corePoolSize;
		this.maxPoolSize = maxPoolSize;
	}


	public void setExecutor(ThreadPoolTaskExecutor executor) {
		Assert.notNull(executor, "'executor' must not be null");
		this.executor = executor;
	}

	public void setCorePoolSize(int corePoolSize) {
		Assert.isTrue(corePoolSize > 0, "'corePoolSize' must be at least 1");
		this.corePoolSize = corePoolSize;
		if (this.executor != null) {
			this.executor.setCorePoolSize(corePoolSize);
		}
	}

	public void setMaxPoolSize(int maxPoolSize) {
		Assert.isTrue(maxPoolSize > 0, "'maxPoolSize' must be at least 1");
		this.maxPoolSize = maxPoolSize;
		if (this.executor != null) {
			this.executor.setMaxPoolSize(maxPoolSize);
		}
	}

	public void setQueueCapacity(int queueCapacity) {
		this.queueCapacity = queueCapacity;
		if (this.executor != null) {
			this.executor.setQueueCapacity(queueCapacity);
		}
	}

	public void setKeepAliveSeconds(int keepAliveSeconds) {
		this.keepAliveSeconds = keepAliveSeconds;
		if (this.executor != null) {
			this.executor.setKeepAliveSeconds(keepAliveSeconds);
		}
	}

	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	public void afterPropertiesSet() {
		initializeExecutor();
	}

	private void initializeExecutor() {
		if (this.executor == null) {
			this.executor = new ThreadPoolTaskExecutor();
		}
		this.executor.setCorePoolSize(this.corePoolSize);
		this.executor.setMaxPoolSize(this.maxPoolSize);
		this.executor.setQueueCapacity(this.queueCapacity);
		this.executor.setKeepAliveSeconds(this.keepAliveSeconds);
		CustomizableThreadFactory threadFactory = new CustomizableThreadFactory();
		threadFactory.setThreadNamePrefix("handler-");
		this.executor.setThreadFactory(threadFactory);
		this.executor.afterPropertiesSet();
	}

	public boolean isRunning() {
		return this.running;
	}

	public void start() {
		synchronized (this.lifecycleMonitor) {
			if (!this.running) {
				this.afterPropertiesSet();
			}
			this.running = true;
		}
	}

	public void stop() {
		synchronized (this.lifecycleMonitor) {
			if (this.isRunning()) {
				this.executor.shutdown();
			}
			this.running = false;
		}
	}

	public Message<?> handle(Message<?> message) {
		if (!this.isRunning()) {
			throw new MessageHandlerNotRunningException();
		}
		try {
			this.executor.execute(new HandlerTask(message));
			return null;
		}
		catch (RuntimeException e) {
			throw new MessageHandlerRejectedExecutionException(e);
		}
	}


	private class HandlerTask implements Runnable {

		private Message<?> message;

		HandlerTask(Message<?> message) {
			this.message = message;
		}

		public void run() {
			try {
				handler.handle(this.message);
			}
			catch (Throwable t) {
				if (errorHandler != null) {
					errorHandler.handle(t);
				}
				else if (logger.isWarnEnabled()) {
					logger.warn("error occurred in handler execution", t);
				}
			}
		}
	}

}
