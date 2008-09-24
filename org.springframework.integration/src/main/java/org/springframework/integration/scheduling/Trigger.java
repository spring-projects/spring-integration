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
 * A strategy for providing the next time a task should run.
 *
 * @author Mark Fisher
 */
public interface Trigger {

	/**
	 * Returns the next time that a task should run or <code>null</code> if the
	 * task should not run again.
	 * 
	 * @param lastScheduledRunTime last time the relevant task was scheduled to
	 * run, or <code>null</code> if it has never been scheduled
	 * @param lastCompleteTime last time the relevant task finished or
	 * <code>null</code> if it did not run to completion
	 * @return next time that a task should run
	 */
	public Date getNextRunTime(Date lastScheduledRunTime, Date lastCompleteTime);

}
