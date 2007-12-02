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

package org.springframework.integration.channel.consumer;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.springframework.context.Lifecycle;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.MessageSource;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.Message;
import org.springframework.scheduling.SchedulingAwareRunnable;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.Assert;

/**
 * A consumer that runs tasks repeatedly in order to invoke the handler as soon
 * as a message is received by one of those tasks.
 * 
 * @author Mark Fisher
 * @author Juergen Hoeller
 */
public class EventDrivenConsumer extends AbstractConsumer implements Lifecycle {

	private static final int DEFAULT_CONCURRENCY = 1;

	private static final int DEFAULT_MAX_CONCURRENCY = 10;

	private static final int DEFAULT_MAX_MESSAGES_PER_TASK = 10;

	private static final int DEFAULT_IDLE_TASK_EXECUTION_LIMIT = 1;


	private TaskExecutor executor;

	private int concurrency = DEFAULT_CONCURRENCY;

	private int maxConcurrency = DEFAULT_MAX_CONCURRENCY;

	private int maxMessagesPerTask = DEFAULT_MAX_MESSAGES_PER_TASK;

	private int idleTaskExecutionLimit = DEFAULT_IDLE_TASK_EXECUTION_LIMIT;

	private final Set<MessageHandlerInvoker> scheduledInvokers = new HashSet<MessageHandlerInvoker>();

	private int activeInvokerCount = 0;

	private final Object activeInvokerMonitor = new Object();

	private final List<MessageHandlerInvoker> pausedInvokers = new LinkedList<MessageHandlerInvoker>();


	public EventDrivenConsumer(MessageSource source, MessageHandler handler) {
		super(source, handler);
	}


	public void setExecutor(TaskExecutor executor) {
		Assert.notNull(executor, "executor must not be null");
		this.executor = executor;
	}

	public void setConcurrency(int concurrency) {
		if (concurrency < 1) {
			throw new IllegalArgumentException("'concurrency' value must be at least 1");
		}
		synchronized (this.activeInvokerMonitor) {
			this.concurrency = concurrency;
			if (this.maxConcurrency < concurrency) {
				this.maxConcurrency = concurrency;
			}
		}
	}

	public void setMaxConcurrency(int maxConcurrency) {
		if (maxConcurrency < 1) {
			throw new IllegalArgumentException("'maxConcurrency' value must be at least 1");
		}
		synchronized (this.activeInvokerMonitor) {
			this.maxConcurrency = Math.max(maxConcurrency, this.concurrency);
		}
	}

	public void setMaxMessagesPerTask(int maxMessagesPerTask) {
		if (maxMessagesPerTask == 0) {
			throw new IllegalArgumentException("'maxMessagesPerTask' must not be 0");
		}
		synchronized (this.activeInvokerMonitor) {
			this.maxMessagesPerTask = maxMessagesPerTask;
		}
	}

	public void setIdleTaskExecutionLimit(int idleTaskExecutionLimit) {
		if (idleTaskExecutionLimit < 1) {
			throw new IllegalArgumentException("'idleTaskExecutionLimit' must be at least 1");
		}
		synchronized (this.activeInvokerMonitor) {
			this.idleTaskExecutionLimit = idleTaskExecutionLimit;
		}
	}

	public void doStart() {
		synchronized (this.lifecycleMonitor) {
			this.resumePausedTasks();
		}
	}

	public void doStop() {
		this.shutdown();
	}

	public void doInitialize() {
		synchronized (this.activeInvokerMonitor) {
			if (this.executor == null) {
				this.executor = createDefaultExecutor();
			}
			else if (this.executor instanceof SchedulingTaskExecutor &&
					((SchedulingTaskExecutor) this.executor).prefersShortLivedTasks() &&
					this.maxMessagesPerTask == Integer.MIN_VALUE) {
				this.maxMessagesPerTask = 1;
			}
			initializeExecutorIfPossible();
			for (int i = 0; i < this.concurrency; i++) {
				scheduleNewInvoker();
			}
		}
	}

	private void initializeExecutorIfPossible() {
		try {
			Method initMethod = this.executor.getClass().getMethod("initialize");
			initMethod.invoke(this.executor);
		}
		catch (Exception e) {
			// do nothing
		}
	}

	private void shutdown() {
		synchronized (this.lifecycleMonitor) {
			this.shutdownExecutorIfPossible();
		}
	}

	private void shutdownExecutorIfPossible() {
		try {
			if (this.executor instanceof Lifecycle) {
				((Lifecycle) this.executor).stop();
			}
			else {
				Method shutdownMethod = this.executor.getClass().getMethod("shutdown");
				shutdownMethod.invoke(this.executor);
			}
		}
		catch (Exception e) {
			// do nothing
		}
	}

	private TaskExecutor createDefaultExecutor() {
		ThreadPoolTaskExecutor defaultExecutor = new ThreadPoolTaskExecutor();
		defaultExecutor.setCorePoolSize(this.maxConcurrency);
		defaultExecutor.setMaxPoolSize(this.maxConcurrency);
		defaultExecutor.setQueueCapacity(5);
		return defaultExecutor;
	}

	/**
	 * Try scheduling a new invoker, since we know messages are being received.
	 * @see #scheduleNewInvokerIfAppropriate()
	 */
	@Override
	protected void messageReceived(Message message) {
		scheduleNewInvokerIfAppropriate();
	}

	@Override
	protected void handlerReplied(Message message) {
	}

	/**
	 * Schedule a new invoker, increasing the total number of scheduled
	 * invokers for this consumer.
	 */
	private void scheduleNewInvoker() {
		MessageHandlerInvoker invoker = new MessageHandlerInvoker();
		if (rescheduleInvokerIfNecessary(invoker)) {
			this.scheduledInvokers.add(invoker);
		}
	}

	private boolean rescheduleInvokerIfNecessary(MessageHandlerInvoker invoker) {
		synchronized (this.lifecycleMonitor) {
			if (this.isRunning()) {
				try {
					doRescheduleInvoker(invoker);
				}
				catch (RuntimeException ex) {
					logRejectedInvoker(invoker, ex);
					this.pausedInvokers.add(invoker);
				}
				return true;
			}
			if (this.isActive()) {
				this.pausedInvokers.add(invoker);
				return true;
			}
			else {
				return false;
			}
		}
	}

	protected void doRescheduleInvoker(final MessageHandlerInvoker invoker) {
		this.executor.execute(invoker);
	}

	private boolean shouldRescheduleInvoker(int idleTaskExecutionCount) {
		synchronized (this.activeInvokerMonitor) {
			boolean idle = (idleTaskExecutionCount >= this.idleTaskExecutionLimit);
			return (this.scheduledInvokers.size() <= (idle ? this.concurrency : this.maxConcurrency));
		}
	}

	private boolean hasIdleInvokers() {
		for (MessageHandlerInvoker invoker : this.scheduledInvokers) {
			if (invoker.isIdle()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Try to resume all paused tasks.
	 * Tasks for which rescheduling failed simply remain in paused mode.
	 */
	protected void resumePausedTasks() {
		synchronized (this.lifecycleMonitor) {
			if (!this.pausedInvokers.isEmpty()) {
				for (Iterator<MessageHandlerInvoker> it = this.pausedInvokers.iterator(); it.hasNext();) {
					MessageHandlerInvoker invoker = it.next();
					try {
						doRescheduleInvoker(invoker);
						it.remove();
						if (logger.isDebugEnabled()) {
							logger.debug("Resumed paused invoker: " + invoker);
						}
					}
					catch (RuntimeException e) {
						logRejectedInvoker(invoker, e);
						// Keep the task in paused mode...
					}
				}
			}
		}
	}

	public int getPausedInvokerCount() {
		synchronized (this.lifecycleMonitor) {
			return this.pausedInvokers.size();
		}
	}

	/**
	 * Log an invoker that has been rejected by {@link #doRescheduleInvoker}.
	 * <p>The default implementation simply logs a corresponding message
	 * at debug level.
	 * @param invoker the rejected invoker object
	 * @param ex the exception thrown from {@link #doRescheduleInvoker}
	 */
	protected void logRejectedInvoker(Object invoker, RuntimeException ex) {
		if (logger.isDebugEnabled()) {
			logger.debug("Invoker [" + invoker + "] has been rejected and paused: " + ex);
		}
	}

	private void scheduleNewInvokerIfAppropriate() {
		if (this.isRunning()) {
			this.resumePausedTasks();
			synchronized (this.activeInvokerMonitor) {
				if (this.scheduledInvokers.size() < this.maxConcurrency && !hasIdleInvokers()) {
					scheduleNewInvoker();
					if (logger.isDebugEnabled()) {
						logger.debug("Raised scheduled invoker count: " + scheduledInvokers.size());
					}
				}
			}
		}
	}

	public final int getScheduledInvokerCount() {
		synchronized (this.activeInvokerMonitor) {
			return this.scheduledInvokers.size();
		}
	}

	public final int getActiveInvokerCount() {
		synchronized (this.activeInvokerMonitor) {
			return this.activeInvokerCount;
		}
	}


	private class MessageHandlerInvoker implements SchedulingAwareRunnable {

		private int idleTaskExecutionCount = 0;

		private volatile boolean idle = true;


		public void run() {
			synchronized (activeInvokerMonitor) {
				activeInvokerCount++;
				activeInvokerMonitor.notifyAll();
			}
			boolean messageReceived = false;
			//TODO: try {
				if (maxMessagesPerTask < 0) {
					while (isActive()) {
						waitWhileNotRunning();
						if (isActive()) {
							messageReceived = invokeHandler();
						}
					}
				}
				else {
					int messageCount = 0;
					while (isRunning() && messageCount < maxMessagesPerTask) {
						boolean messageHandled = invokeHandler();
						this.idle = !messageHandled;
						messageReceived = (messageHandled || messageReceived);
						messageCount++;
					}
				}
			// TODO: } catch (Throwable t) { check if last message succeeded, else sleep between recovery attempts } 
			synchronized (activeInvokerMonitor) {
				activeInvokerCount--;
				activeInvokerMonitor.notifyAll();
			}
			if (!messageReceived) {
				this.idleTaskExecutionCount++;
			}
			else {
				this.idleTaskExecutionCount = 0;
			}
			if (!shouldRescheduleInvoker(this.idleTaskExecutionCount) || !rescheduleInvokerIfNecessary(this)) {
				this.shutdown();
			}
			else if (isRunning()) {
				int nonPausedInvokers = getScheduledInvokerCount() - getPausedInvokerCount();
				if (nonPausedInvokers < 1) {
					logger.error("All scheduled invokers have been paused, probably due to tasks having been rejected. " +
							"Check your thread pool configuration! Manual recovery necessary through a start() call.");
				}
				else if (nonPausedInvokers < concurrency) {
					logger.warn("Number of scheduled invokers has dropped below concurrency limit, probably " +
							"due to tasks having been rejected. Check your thread pool configuration! Automatic recovery " +
							"to be triggered by remaining invokers.");
				}
			}
		}

		private void shutdown() {
			synchronized (activeInvokerMonitor) {
				scheduledInvokers.remove(this);
				if (logger.isDebugEnabled()) {
					logger.debug("Lowered scheduled invoker count: " + scheduledInvokers.size());
				}
				activeInvokerMonitor.notifyAll();
			}			
		}

		private boolean invokeHandler() {
			boolean messageReceived = receiveAndHandle();
			this.idle = !messageReceived;
			return messageReceived;
		}

		public boolean isLongLived() {
			return (maxMessagesPerTask < 0);
		}

		public boolean isIdle() {
			return this.idle;
		}

	}

}
