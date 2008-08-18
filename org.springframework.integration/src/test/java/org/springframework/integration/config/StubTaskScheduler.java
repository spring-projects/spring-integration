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

package org.springframework.integration.config;

import java.util.concurrent.ScheduledFuture;

import org.springframework.integration.scheduling.SchedulableTask;
import org.springframework.integration.scheduling.TaskScheduler;
import org.springframework.integration.util.ErrorHandler;

/**
 * @author Mark Fisher
 */
public class StubTaskScheduler implements TaskScheduler {

	public boolean cancel(Runnable task, boolean mayInterruptIfRunning) {
		return false;
	}

	public ScheduledFuture<?> schedule(SchedulableTask task) {
		return null;
	}

	public void setErrorHandler(ErrorHandler errorHandler) {
	}

	public boolean prefersShortLivedTasks() {
		return false;
	}

	public void execute(Runnable task) {
	}

	public boolean isRunning() {
		return false;
	}

	public void start() {
	}

	public void stop() {
	}

}
