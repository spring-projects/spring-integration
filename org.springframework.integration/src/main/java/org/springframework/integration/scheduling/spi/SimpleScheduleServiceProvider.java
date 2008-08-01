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

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * An implementation
 * @author Marius Bogoevici
 */
public class SimpleScheduleServiceProvider implements ScheduleServiceProvider {

	private final ScheduledExecutorService executor;

	public SimpleScheduleServiceProvider(ScheduledExecutorService executor) {
		this.executor = executor;
	}

	public void execute(Runnable runnable) {
		this.executor.execute(runnable);
	}

	public void shutdown(boolean waitForTasksToCompleteOnShutdown) {
		if (this.executor.isShutdown()) {
				return;
			}
			if (waitForTasksToCompleteOnShutdown) {
				this.executor.shutdown();
			}
			else {
				this.executor.shutdownNow();
			}
	}

	public ScheduledFuture<?> scheduleWithInitialDelay(Runnable runnable, long initialDelay, TimeUnit timeUnit) {
		return this.executor.schedule(runnable, initialDelay, timeUnit);
	}

	public ScheduledFuture<?> scheduleAtFixedRate(Runnable runnable, long initialDelay, long period,
	                                              TimeUnit timeUnit) {
		return this.executor.scheduleAtFixedRate(runnable, initialDelay, period, timeUnit);
	}

	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable runnable, long initialDelay, long delay,
	                                                 TimeUnit timeUnit) {
		return this.executor.scheduleWithFixedDelay(runnable, initialDelay, delay, timeUnit);
	}

}
