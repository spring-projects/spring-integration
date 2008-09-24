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
import java.util.concurrent.TimeUnit;

import org.springframework.util.Assert;

/**
 * A trigger for periodic execution. The interval may be applied as either
 * fixed-rate or fixed-delay, and an initial delay value may also be
 * configured. The default initial delay is 0, and the default behavior is
 * fixed-delay: each subsequent delay is measured from the last completion
 * time. To enable execution between the scheduled start time of each
 * execution, set 'fixedRate' to true.
 * 
 * @author Mark Fisher
 */
public class IntervalTrigger implements Trigger {

	private final long interval;

	private volatile long initialDelay = 0;

	private volatile boolean fixedRate = false;


	/**
	 * Create a trigger with the given interval in milliseconds.
	 */
	public IntervalTrigger(long interval) {
		Assert.isTrue(interval >= 0, "interval must not be negative");
		this.interval = interval;
	}

	/**
	 * Create a trigger with the given interval and time unit.
	 */
	public IntervalTrigger(long interval, TimeUnit unit) {
		this(unit.toMillis(interval));
	}


	/**
	 * Specify the delay for the initial execution.
	 */
	public void setInitialDelay(long initialDelay) {
		this.initialDelay = initialDelay;
	}

	/**
	 * Specify the delay for the initial execution using the given time unit.
	 */
	public void setInitialDelay(long initialDelay, TimeUnit unit) {
		this.initialDelay = unit.toMillis(initialDelay);
	}

	/**
	 * Specify whether the interval should be measured between the
	 * scheduled start times rather than between actual completion times
	 * (the latter, "fixed delay" behavior, is the default).
	 */
	public void setFixedRate(boolean fixedRate) {
		this.fixedRate = fixedRate;
	}

	/**
	 * Returns the next time a task should run.
	 */
	public Date getNextRunTime(Date lastScheduledRunTime, Date lastCompleteTime) {
		if (lastScheduledRunTime == null) {
			return new Date(System.currentTimeMillis() + this.initialDelay);
		}
		else if (this.fixedRate) {
			return new Date(lastScheduledRunTime.getTime() + this.interval);
		}
		return new Date(lastCompleteTime.getTime() + this.interval);
	}

}
