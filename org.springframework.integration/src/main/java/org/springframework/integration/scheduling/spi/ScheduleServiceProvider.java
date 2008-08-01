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

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * An SPI interface providing an abstraction of thread pooling and scheduling operations, allowing to switch between
 * different platforms (such as Java 5's java.util.concurrent, Quartz, etc) at runtime.
 *
 * @author Marius Bogoevici
 */
public interface ScheduleServiceProvider {

	void execute(Runnable runnable);

	void shutdown(boolean waitForTasksToCompleteOnShutdown);

	ScheduledFuture<?> scheduleWithInitialDelay(Runnable runnable, long initialDelay, TimeUnit timeUnit)
			throws UnschedulableTaskException;

	ScheduledFuture<?> scheduleAtFixedRate(Runnable runnable, long initialDelay, long period, TimeUnit timeUnit)
			throws UnschedulableTaskException;

	ScheduledFuture<?> scheduleWithFixedDelay(Runnable runnable, long initialDelay, long delay, TimeUnit timeUnit)
			throws UnschedulableTaskException;

}
