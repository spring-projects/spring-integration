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
import java.util.concurrent.TimeUnit;

import org.springframework.integration.util.ErrorHandler;
import org.springframework.util.Assert;

/**
 * An implementation of {@link MessagingTaskScheduler} that understands
 * {@link PollingSchedule PollingSchedules}.
 * 
 * @author Mark Fisher
 */
public class SimpleMessagingTaskScheduler extends AbstractMessagingTaskScheduler {

	private final ScheduledExecutorService executor;

	private volatile ErrorHandler errorHandler;

	private final Set<Runnable> pendingTasks = new CopyOnWriteArraySet<Runnable>();

	private volatile boolean starting;

	private volatile boolean running;

	private final Object lifecycleMonitor = new Object();


	public SimpleMessagingTaskScheduler(int corePoolSize) {
		this(new ScheduledThreadPoolExecutor(corePoolSize));
	}

	public SimpleMessagingTaskScheduler(ScheduledExecutorService executor) {
		Assert.notNull(executor, "'executor' must not be null");
		this.executor = executor;
	}


	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	public boolean isRunning() {
		return this.running;
	}

	public void start() {
		synchronized (this.lifecycleMonitor) {
			if (this.running || this.starting) {
				return;
			}
			this.starting = true;
		}
		for (Runnable task : this.pendingTasks) {
			this.schedule(task);
		}
		this.pendingTasks.clear();
		this.running = true;
		this.starting = false;
	}

	public void stop() {
		synchronized (this.lifecycleMonitor) {
			if (this.running) {
				this.executor.shutdownNow();
				this.running = false;
			}
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
