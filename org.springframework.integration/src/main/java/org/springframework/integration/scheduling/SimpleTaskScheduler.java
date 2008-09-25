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

package org.springframework.integration.scheduling;

import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.util.ErrorHandler;
import org.springframework.scheduling.SchedulingException;
import org.springframework.util.Assert;

/**
 * An implementation of {@link TaskScheduler} that delegates to a {@link TaskExecutor}.
 *
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public class SimpleTaskScheduler implements TaskScheduler {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final TaskExecutor executor;

	private volatile ErrorHandler errorHandler;

	private volatile SchedulerTask schedulerTask = new SchedulerTask();

	private final DelayQueue<TriggeredTask<?>> scheduledTasks = new DelayQueue<TriggeredTask<?>>();

	private final Set<TriggeredTask<?>> executingTasks = Collections.synchronizedSet(new TreeSet<TriggeredTask<?>>());

	private volatile boolean running;

	private final ReentrantLock lifecycleLock = new ReentrantLock();


	public SimpleTaskScheduler(TaskExecutor executor) {
		Assert.notNull(executor, "executor must not be null");
		this.executor = executor;
	}


	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	public final ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
		TriggeredTask<Void> triggeredTask = new TriggeredTask<Void>(task, trigger);
		return this.schedule(triggeredTask, null, null);
	}

	private <V> ScheduledFuture<V> schedule(TriggeredTask<V> triggeredTask, Date lastScheduledRunTime, Date lastCompleteTime) {
		Date nextRunTime = triggeredTask.trigger.getNextRunTime(lastScheduledRunTime, lastCompleteTime);
		if (nextRunTime != null) {
			triggeredTask.setScheduledTime(nextRunTime);
			this.scheduledTasks.offer(triggeredTask);
		}
		return triggeredTask;
	}


	// Lifecycle implementation

	public boolean isRunning() {
		this.lifecycleLock.lock();
		try {
			return this.running;
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

	public void start() {
		this.lifecycleLock.lock();
		try {
			if (this.running) {
				return;
			}
			this.running = true;
			this.executor.execute(this.schedulerTask);
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

	public void stop() {
		this.lifecycleLock.lock();
		try {
			if (!this.running) {
				return;
			}
			this.running = false;
			Thread executingThread = this.schedulerTask.executingThread.get();
			if (executingThread != null) {
				executingThread.interrupt();
			}
			synchronized (this.executingTasks) {
				for (TriggeredTask<?> task : this.executingTasks) {
					task.cancel(true);
				}
				this.executingTasks.clear();
			}
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

	public boolean prefersShortLivedTasks() {
		return true;
	}

	public void execute(Runnable task) {
		this.executor.execute(task);
	}


	private class SchedulerTask implements Runnable {

		private final AtomicReference<Thread> executingThread = new AtomicReference<Thread>();


		public void run() {
			if (!this.executingThread.compareAndSet(null, Thread.currentThread())) {
				throw new SchedulingException("The SchedulerTask is already running.");
			}
			while (SimpleTaskScheduler.this.isRunning()) {
				try {
					TriggeredTask<?> task = SimpleTaskScheduler.this.scheduledTasks.take();
					if (SimpleTaskScheduler.this.isRunning()) {
						SimpleTaskScheduler.this.executor.execute(task);
					}
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
			}
			this.executingThread.set(null);
		}
	}


	/**
	 * Wrapper class that enables rescheduling of a task based on a Trigger.
	 */
	private class TriggeredTask<V> extends FutureTask<V> implements Delayed, ScheduledFuture<V> {

		private final Trigger trigger;

		private volatile Date scheduledTime;


		public TriggeredTask(Runnable task, Trigger trigger) {
			super(new ErrorHandlingRunnableWrapper(task), null);
			this.trigger = trigger;
		}


		public void setScheduledTime(Date scheduledTime) {
			this.scheduledTime = scheduledTime;
		}

		public void run() {
			SimpleTaskScheduler.this.executingTasks.add(this);
			super.runAndReset();
			SimpleTaskScheduler.this.executingTasks.remove(this);
			if (SimpleTaskScheduler.this.isRunning() && !this.isCancelled()) {
				SimpleTaskScheduler.this.schedule(this, this.scheduledTime, new Date());
			}
		}

		public int compareTo(Delayed other) {
			long thisDelay = this.getDelay(TimeUnit.MILLISECONDS);
			long otherDelay = other.getDelay(TimeUnit.MILLISECONDS);
			if (thisDelay < otherDelay)  {
				return -1;
			}
			if (thisDelay == otherDelay) {
				return 0;
			}
			return 1;
		}

		public long getDelay(TimeUnit unit) {
			long now = new Date().getTime();
			long scheduled = this.scheduledTime.getTime();
			return (scheduled > now) ? unit.convert(scheduled - now, TimeUnit.MILLISECONDS) : 0;
		}

		public synchronized boolean cancel(boolean mayInterruptIfRunning) {
			if (!this.isCancelled()) {
				SimpleTaskScheduler.this.scheduledTasks.remove(this);
			}
			return super.cancel(mayInterruptIfRunning);
		}
	}


	/**
	 * Wrapper that catches any Throwable thrown by a target task and
	 * delegates to the {@link ErrorHandler} if available. If no error handler
	 * has been configured, the error will be logged at warn-level.
	 */
	private class ErrorHandlingRunnableWrapper implements Runnable {

		private final Runnable target;


		public ErrorHandlingRunnableWrapper(Runnable target) {
			this.target = target;
		}


		public void run() {
			try {
				this.target.run();
			}
			catch (Throwable t) {
				if (SimpleTaskScheduler.this.errorHandler != null) {
					SimpleTaskScheduler.this.errorHandler.handle(t);
				}
				else if (logger.isWarnEnabled()) {
					SimpleTaskScheduler.this.logger.warn("Error occurred in task but no 'errorHandler' is available.", t);
				}
			}
		}
	}

}
