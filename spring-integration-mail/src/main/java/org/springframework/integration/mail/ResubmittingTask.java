/*
 * Copyright 2002-2011 the original author or authors.
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
package org.springframework.integration.mail;

import java.security.ProviderException;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.scheduling.TaskScheduler;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 2.0.2
 *
 */
class ResubmittingTask implements Runnable {
	private static final Log logger = LogFactory.getLog(ResubmittingTask.class);
	private final Runnable targetTask;
	private final TaskScheduler scheduler;
	private final long delay;
	
	public ResubmittingTask(Runnable targetTask, TaskScheduler scheduler, long delay) {
		this.targetTask = targetTask;
		this.scheduler = scheduler;
		this.delay = delay;
	}

	public void run() {
		try {
			targetTask.run();
			logger.debug("Task completed successfully. Re-scheduling it again right away");
			scheduler.schedule(this, new Date());
		}
		catch (ProviderException e) { //run again after a delay
			logger.warn("Failed to execute IDLE task. Will atempt to resubmit in " + delay + " milliseconds", e);
			scheduler.schedule(this, new Date(System.currentTimeMillis() + delay));
		}
	}
}
