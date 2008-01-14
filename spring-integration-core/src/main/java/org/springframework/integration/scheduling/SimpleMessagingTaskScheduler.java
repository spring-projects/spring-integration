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

package org.springframework.integration.scheduling;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.util.ErrorHandler;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.util.Assert;

/**
 * An implementation of {@link MessagingTaskScheduler} that understands
 * {@link PollingSchedule PollingSchedules}.
 * 
 * @author Mark Fisher
 */
public class SimpleMessagingTaskScheduler extends AbstractMessagingTaskScheduler implements InitializingBean {

	private ScheduledExecutorService executor;

	private int corePoolSize = 10;

	private ThreadFactory threadFactory;

	private String threadNamePrefix = this.getClass().getSimpleName() + "-";

	private ErrorHandler errorHandler;

	private Set<Runnable> pendingTasks = new CopyOnWriteArraySet<Runnable>();

	private volatile boolean running;

	private Object lifecycleMonitor = new Object();


	public void setExecutor(ScheduledExecutorService executor) {
		Assert.notNull(executor, "'executor' must not be null");
		this.executor = executor;
	}

	public void setCorePoolSize(int corePoolSize) {
		Assert.isTrue(corePoolSize > 0, "'corePoolSize' must be greater than 0");
		this.corePoolSize = corePoolSize;
	}

	public void setThreadFactory(ThreadFactory threadFactory) {
		this.threadFactory = threadFactory;
	}

	public void setThreadNamePrefix(String threadNamePrefix) {
		Assert.notNull(threadNamePrefix, "'threadNamePrefix' must not be null");
		this.threadNamePrefix = threadNamePrefix;
	}

	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	public void afterPropertiesSet() {
		if (this.executor == null) {
			if (this.threadFactory == null) {
				this.threadFactory = new CustomizableThreadFactory(this.threadNamePrefix);
			}
			this.executor = new ScheduledThreadPoolExecutor(this.corePoolSize, this.threadFactory);
		}
	}

	public boolean isRunning() {
		return this.running;
	}

	public void start() {
		if (this.executor == null) {
			this.afterPropertiesSet();
		}
		synchronized (this.lifecycleMonitor) {
			this.running = true;
			for (Runnable task : this.pendingTasks) {
				this.schedule(task);
			}
			this.pendingTasks.clear();
		}
	}

	public void stop() {
		if (this.isRunning()) {
			this.running = false;
			this.executor.shutdownNow();
		}
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable task) {
		if (!this.isRunning()) {
			this.pendingTasks.add(task);
			return null;
		}
		Schedule schedule = (task instanceof MessagingTask) ? ((MessagingTask) task).getSchedule() : null;
		MessagingTaskRunner runner = new MessagingTaskRunner(task);
		if (schedule == null) {
			return this.executor.schedule(runner, 0, TimeUnit.MILLISECONDS);
		}
		if (schedule instanceof PollingSchedule) {
			PollingSchedule ps = (PollingSchedule) schedule;
			if (ps.getPeriod() <= 0) {
				runner.setShouldRepeat(true);
				return this.executor.schedule(runner, ps.getInitialDelay(), ps.getTimeUnit());
			}
			if (ps.getFixedRate()) {
				return this.executor.scheduleAtFixedRate(runner, ps.getInitialDelay(), ps.getPeriod(), ps.getTimeUnit());
			}
			return this.executor.scheduleWithFixedDelay(runner, ps.getInitialDelay(), ps.getPeriod(), ps.getTimeUnit());
		}
		throw new UnsupportedOperationException(this.getClass().getName() + " does not support schedule type '"
				+ schedule.getClass().getName() + "'");
	}


	private class MessagingTaskRunner implements Runnable {

		private Runnable task;

		private boolean shouldRepeat;


		public MessagingTaskRunner(Runnable task) {
			this.task = task;
		}

		public void setShouldRepeat(boolean shouldRepeat) {
			this.shouldRepeat = shouldRepeat;
		}

		public void run() {
			try {
				this.task.run();
			}
			catch (Throwable t) {
				if (errorHandler != null) {
					errorHandler.handle(t);
				}
			}
			if (this.shouldRepeat) {
				MessagingTaskRunner runner = new MessagingTaskRunner(this.task);
				runner.setShouldRepeat(true);
				executor.execute(runner);
			}
		}
	}

}
