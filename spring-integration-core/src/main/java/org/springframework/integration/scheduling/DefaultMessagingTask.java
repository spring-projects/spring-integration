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

/**
 * A wrapper for {@link Runnable Runnables} that provides a schedule and also
 * captures any error that may be thrown from the run() method.
 * 
 * @author Mark Fisher
 */
public class DefaultMessagingTask implements MessagingTask {

	private Runnable runnable;

	private Schedule schedule;

	private Throwable lastError;


	public DefaultMessagingTask(Runnable runnable) {
		this(runnable, null);
	}

	public DefaultMessagingTask(Runnable runnable, Schedule schedule) {
		this.runnable = runnable;
		this.schedule = schedule;
	}


	public Schedule getSchedule() {
		return this.schedule;
	}

	public Throwable getLastError() {
		return this.lastError;
	}

	public void run() {
		try {
			this.runnable.run();
		}
		catch (Throwable t) {
			this.lastError = t;
		}
	}

}
