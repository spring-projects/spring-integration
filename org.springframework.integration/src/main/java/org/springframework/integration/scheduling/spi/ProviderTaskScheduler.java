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

package org.springframework.integration.scheduling.spi;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.integration.scheduling.PollingSchedule;
import org.springframework.integration.scheduling.SchedulableTask;
import org.springframework.integration.scheduling.TaskScheduler;
import org.springframework.integration.util.ErrorHandler;
import org.springframework.util.Assert;

/**
 * An implementation of {@link org.springframework.integration.scheduling.TaskScheduler} that understands
 * {@link org.springframework.integration.scheduling.PollingSchedule PollingSchedules} and delegates to
 * a {@link ScheduleServiceProvider} instance.
 * 
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public class ProviderTaskScheduler implements TaskScheduler, DisposableBean {

	private final Log logger = LogFactory.getLog(this.getClass());

	private ScheduleServiceProvider scheduleServiceProvider;

	private volatile boolean waitForTasksToCompleteOnShutdown = true;

	private volatile ErrorHandler errorHandler;

	private final Set<Runnable> pendingTasks = new CopyOnWriteArraySet<Runnable>();

	private final Map<Runnable, ScheduledFuture<?>> scheduledTasks =
			new ConcurrentHashMap<Runnable, ScheduledFuture<?>>();

	private volatile boolean running;

	private final Object lifecycleMonitor = new Object();


	public ProviderTaskScheduler(ScheduleServiceProvider scheduleServiceProvider) {
		Assert.notNull(scheduleServiceProvider, "'executor' must not be null");
		this.scheduleServiceProvider = scheduleServiceProvider;
	}


	public void setWaitForTasksToCompleteOnShutdown(boolean waitForTasksToCompleteOnShutdown) {
		this.waitForTasksToCompleteOnShutdown = waitForTasksToCompleteOnShutdown;
	}

	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	public boolean isRunning() {
		return this.running;
	}

	public void start() {
		synchronized (this.lifecycleMonitor) {
			if (this.running) {
				return;
			}
			if (logger.isInfoEnabled()) {
				logger.info("task scheduler starting");
			}
			this.running = true;
			for (Runnable task : this.pendingTasks) {
				if (task instanceof SchedulableTask) {
					this.schedule((SchedulableTask)task);
				}
				else {
					this.execute(task);
				}
			}
			this.pendingTasks.clear();
			if (logger.isInfoEnabled()) {
				logger.info("task scheduler started successfully");
			}
		}
	}

	public void stop() {
		synchronized (this.lifecycleMonitor) {
			if (this.running) {
				if (logger.isInfoEnabled()) {
					logger.info("task scheduler stopping");
				}
				this.running = false;
				for (Runnable task : this.scheduledTasks.keySet()) {
					this.cancel(task, true);
				}
				if (logger.isInfoEnabled()) {
					logger.info("task scheduler stopped successfully");
				}
			}
		}
	}

	public void destroy() {
		synchronized (this.lifecycleMonitor) {
			this.stop();
			scheduleServiceProvider.shutdown(this.waitForTasksToCompleteOnShutdown);
		}
	}

	public boolean prefersShortLivedTasks() {
		return true;
	}

	public void execute(Runnable task) {
		this.scheduleServiceProvider.execute(task);
	}

	public ScheduledFuture<?> schedule(SchedulableTask task) {
		synchronized (this.lifecycleMonitor) {
			if (!this.running) {
				if (logger.isDebugEnabled()) {
					logger.debug("scheduler is not running, adding task to pending list: " + task);
				}
				this.pendingTasks.add(task);
				return null;
			}
			if (logger.isDebugEnabled()) {
				logger.debug("scheduling task: " + task);
			}
			TaskRunner runner = new TaskRunner(task);
			ScheduledFuture<?> future = null;
			if (task.getSchedule() == null) {
				future = this.scheduleServiceProvider.scheduleWithInitialDelay(runner, 0, TimeUnit.MILLISECONDS);
			}
			else if (task.getSchedule() instanceof PollingSchedule) {
				PollingSchedule ps = (PollingSchedule) task.getSchedule();
				if (ps.getPeriod() <= 0) {
					runner.setShouldRepeat(true);
					future = this.scheduleServiceProvider.scheduleWithInitialDelay(runner, ps.getInitialDelay(), ps.getTimeUnit());
				}
				else if (ps.getFixedRate()) {
					future = this.scheduleServiceProvider.scheduleAtFixedRate(runner, ps.getInitialDelay(), ps.getPeriod(), ps.getTimeUnit());
				}
				else {
					future = this.scheduleServiceProvider.scheduleWithFixedDelay(runner, ps.getInitialDelay(), ps.getPeriod(), ps.getTimeUnit());
				}
			}
			if (future == null) {
				throw new UnsupportedOperationException(this.getClass().getName() + " does not support scheduleWithInitialDelay type '"
						+ task.getSchedule().getClass().getName() + "'");
			}
			this.scheduledTasks.put(task, future);
			if (logger.isDebugEnabled()) {
				logger.debug("scheduled task: " + task);
			}
			return future;
		}
	}

	public boolean cancel(Runnable task, boolean mayInterruptIfRunning) {
		synchronized (this.lifecycleMonitor) {
			ScheduledFuture<?> future = this.scheduledTasks.get(task);
			if (future != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("cancelling task: " + task);
				}
				return future.cancel(mayInterruptIfRunning);
			}
			return this.pendingTasks.remove(task);
		}
	}


	private class TaskRunner implements Runnable {

		private final Runnable task;

		private volatile boolean shouldRepeat;


		public TaskRunner(Runnable task) {
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
				else if (logger.isWarnEnabled()) {
					logger.warn("error occurred in task but no 'errorHandler' is available", t);
				}
			}
			if (this.shouldRepeat && isRunning()) {
				TaskRunner runner = new TaskRunner(this.task);
				runner.setShouldRepeat(true);
				scheduleServiceProvider.execute(runner);
			}
		}
	}

}
