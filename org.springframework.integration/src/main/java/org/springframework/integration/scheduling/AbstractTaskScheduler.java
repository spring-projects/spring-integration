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

import java.util.concurrent.ScheduledFuture;

/**
 * Base class for {@link TaskScheduler} implementations.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractTaskScheduler implements TaskScheduler {

	public boolean prefersShortLivedTasks() {
		return true;
	}

	/**
	 * Submit a task to be run once.
	 */
	public void execute(Runnable task) {
		this.schedule(task);
	}

	public abstract ScheduledFuture<?> schedule(Runnable task);

}
