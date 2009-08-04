/*
 * Copyright 2002-2009 the original author or authors.
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

import java.util.Date;
import java.util.concurrent.ScheduledFuture;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;

/**
 * @author Mark Fisher
 */
public class StubTaskScheduler implements TaskScheduler {

	public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
		return null;
	}

	public ScheduledFuture<?> schedule(Runnable task, Date startTime) {
		return null;
	}

	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long period) {
		return null;
	}

	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Date startTime, long period) {
		return null;
	}

	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long delay) {
		return null;
	}

	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Date startTime, long delay) {
		return null;
	}

}
