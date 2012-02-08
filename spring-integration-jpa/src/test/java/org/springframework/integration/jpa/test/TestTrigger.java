/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.jpa.test;

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
public class TestTrigger implements Trigger {

	private static final AtomicBoolean hasRun = new AtomicBoolean();

	private final Date executionTime;
	
	private static volatile CountDownLatch latch = new CountDownLatch(1);


	public TestTrigger() {
		super();
		executionTime = new Date();		
	}

	public Date nextExecutionTime(TriggerContext triggerContext) {

		if (TestTrigger.hasRun.getAndSet(true)) {
			TestTrigger.latch.countDown();
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
		TestTrigger other = (TestTrigger) obj;
		if (executionTime == null) {
			if (other.executionTime != null)
				return false;
		} else if (!executionTime.equals(other.executionTime))
			return false;
		return true;
	}

	public void reset() {
		TestTrigger.latch = new CountDownLatch(1);
		TestTrigger.hasRun.set(false);
	}

	public void await() {
		try {
			TestTrigger.latch.await(5000, TimeUnit.MILLISECONDS);
			if (latch.getCount() != 0) {
				throw new RuntimeException("test latch.await() did not count down");
			}
		}
		catch (InterruptedException e) {
			throw new RuntimeException("test latch.await() interrupted");
		}
	}
	
}
