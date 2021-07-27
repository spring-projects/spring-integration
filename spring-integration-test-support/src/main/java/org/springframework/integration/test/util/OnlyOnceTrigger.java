/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.2
 *
 */
public class OnlyOnceTrigger implements Trigger {

	private final AtomicBoolean hasRun = new AtomicBoolean();

	private final Date executionTime;

	private volatile CountDownLatch latch = new CountDownLatch(1);


	public OnlyOnceTrigger() {
		this.executionTime = new Date();
	}

	@Override
	public Date nextExecutionTime(TriggerContext triggerContext) {
		if (this.hasRun.getAndSet(true)) {
			this.latch.countDown();
			return null;
		}

		return this.executionTime; // NOSONAR - expose internals
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.executionTime.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		OnlyOnceTrigger other = (OnlyOnceTrigger) obj;
		return this.executionTime.equals(other.executionTime);
	}

	public void reset() {
		this.latch = new CountDownLatch(1);
		this.hasRun.set(false);
	}

	public void await() {
		try {
			if (!this.latch.await(10000, TimeUnit.MILLISECONDS)) { // NOSONAR magic number
				throw new IllegalStateException("test latch.await() did not count down");
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("test latch.await() interrupted", e);
		}
	}

}
