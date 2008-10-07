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

import java.util.Date;

/**
 * A trigger that uses a cron expression. See {@link CronSequenceGenerator}
 * for a detailed description of the expression pattern syntax.
 * 
 * @author Mark Fisher
 */
public class CronTrigger implements Trigger {

	private final CronSequenceGenerator cronSequenceGenerator;


	/**
	 * Create a trigger for the given cron expression.
	 */
	public CronTrigger(String expression) throws IllegalArgumentException {
		this.cronSequenceGenerator = new CronSequenceGenerator(expression);
	}


	/**
	 * Return the next time a task should run. Determined by consulting this
	 * trigger's cron expression compared with the lastCompleteTime. If the
	 * lastCompleteTime is <code>null</code>, the current time is used.
	 */
	public Date getNextRunTime(Date lastScheduledRunTime, Date lastCompleteTime) {
		Date date = (lastCompleteTime != null) ? lastCompleteTime : new Date();
		return this.cronSequenceGenerator.next(date);
	}

	@Override
	public boolean equals(Object other) {
		if (other == null || !(other instanceof CronTrigger)) {
			return false;
		}
		return this.cronSequenceGenerator.equals(
				((CronTrigger) other).cronSequenceGenerator);
	}

	@Override
	public int hashCode() {
		return this.cronSequenceGenerator.hashCode();
	}

}
