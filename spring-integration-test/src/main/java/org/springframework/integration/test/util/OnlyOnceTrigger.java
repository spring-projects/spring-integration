/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.test.util;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;

/**
 *
 * @author Gunnar Hillert
 * @since 2.2
 *
 */
public class OnlyOnceTrigger implements Trigger {

	private static final AtomicBoolean hasRun = new AtomicBoolean();

	private final Date executionTime;

	private static volatile CountDownLatch latch = new CountDownLatch(1);


	public OnlyOnceTrigger() {
		super();
		executionTime = new Date();
	}

	public Date nextExecutionTime(TriggerContext triggerContext) {

		if (OnlyOnceTrigger.hasRun.getAndSet(true)) {
			OnlyOnceTrigger.latch.countDown();
			return null;
		}

		return this.executionTime;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((executionTime == null) ? 0 : executionTime.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OnlyOnceTrigger other = (OnlyOnceTrigger) obj;
		if (executionTime == null) {
			if (other.executionTime != null)
				return false;
		} else if (!executionTime.equals(other.executionTime))
			return false;
		return true;
	}

	public void reset() {
		OnlyOnceTrigger.latch = new CountDownLatch(1);
		OnlyOnceTrigger.hasRun.set(false);
	}

	public void await() {
		try {
			OnlyOnceTrigger.latch.await(5000, TimeUnit.MILLISECONDS);
			if (latch.getCount() != 0) {
				throw new RuntimeException("test latch.await() did not count down");
			}
		}
		catch (InterruptedException e) {
			throw new RuntimeException("test latch.await() interrupted");
		}
	}

}
