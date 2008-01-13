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

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.InitializingBean;
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


	public void setExecutor(ScheduledExecutorService executor) {
		Assert.notNull(executor, "'executor' must not be null");
		this.executor = executor;
	}

	public void setCorePoolSize(int corePoolSize) {
		Assert.isTrue(corePoolSize > 0, "'corePoolSize' must be greater than 0");
		this.corePoolSize = corePoolSize;
	}

	public void afterPropertiesSet() {
		if (this.executor == null) {
			this.executor = new ScheduledThreadPoolExecutor(this.corePoolSize);
		}
	}

	@Override
	public ScheduledFuture<?> schedule(MessagingTask task) {
		if (this.executor == null) {
			this.afterPropertiesSet();
		}
		Schedule schedule = task.getSchedule();
		if (schedule == null) {
			return this.executor.schedule(task, 0, TimeUnit.MILLISECONDS);
		}
		if (schedule instanceof PollingSchedule) {
			PollingSchedule ps = (PollingSchedule) schedule;
			if (ps.getPeriod() <= 0) {
				return this.executor.schedule(new RepeatingTask(task), ps.getInitialDelay(), ps.getTimeUnit());
			}
			if (ps.getFixedRate()) {
				return this.executor.scheduleAtFixedRate(task, ps.getInitialDelay(), ps.getPeriod(), ps.getTimeUnit());
			}
			return this.executor.scheduleWithFixedDelay(task, ps.getInitialDelay(), ps.getPeriod(), ps.getTimeUnit());
		}
		throw new UnsupportedOperationException(this.getClass().getName() + " does not support schedule type '"
				+ schedule.getClass().getName() + "'");
	}


	private class RepeatingTask implements Runnable {

		private MessagingTask task;

		RepeatingTask(MessagingTask task) {
			this.task = task;
		}

		public void run() {
			task.run();
			executor.execute(new RepeatingTask(task));
		}
	}

}
