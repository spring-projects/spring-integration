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

package org.springframework.integration.quartz;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.quartz.Scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.scheduling.spi.ScheduleServiceProvider;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

/**
 * An integration test for the Quartz-based implementation of the Scheduling SPI.
 *
 * @author Marius Bogoevici
 */
@ContextConfiguration(locations = "quartz-scheduler-context.xml")
public class TestQuartzScheduleServiceProvider extends AbstractJUnit4SpringContextTests {

	@Autowired
	private ScheduleServiceProvider scheduleServiceProvider;

	@Autowired
	private Scheduler scheduler;

	@Test
	@DirtiesContext
	public void testExecute() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		AtomicBoolean hasRun = new AtomicBoolean(false);
		scheduleServiceProvider.execute(new SimpleRunnable(latch, hasRun));
		latch.await(1000, TimeUnit.MILLISECONDS);
		assertTrue(hasRun.get());
	}

	@Test
	@DirtiesContext
	public void testReturnedScheduledFutureCancelsJobWithoutInterruption() throws Exception {
		CountDownLatch latch = new CountDownLatch(2);
		AtomicBoolean hasBeenInterrupted = new AtomicBoolean(false);
		ScheduledFuture<?> scheduledFuture = scheduleServiceProvider.scheduleAtFixedRate(new SimpleRunnable(latch,
				hasBeenInterrupted), 500, 500,
				TimeUnit.MILLISECONDS);
		// the job is scheduled
		assertEquals(1, scheduler.getJobNames(Scheduler.DEFAULT_GROUP).length);
		scheduledFuture.cancel(false);
		// the job is not scheduled anymore
		assertEquals(0, scheduler.getJobNames(Scheduler.DEFAULT_GROUP).length);
	}

	@Test
	@DirtiesContext
	public void testReturnedScheduledFutureCancelsJobWithInterruption() throws Exception {
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch endLatch = new CountDownLatch(1);
		AtomicBoolean hasBeenInterrupted = new AtomicBoolean(false);
		ScheduledFuture<?> scheduledFuture = scheduleServiceProvider.scheduleAtFixedRate(new LongRunningRunnable(startLatch,
				endLatch, hasBeenInterrupted), 500, 10000, TimeUnit.MILLISECONDS);
		// the job is scheduled
		assertEquals(1, scheduler.getJobNames(Scheduler.DEFAULT_GROUP).length);
		startLatch.await(1000, TimeUnit.MILLISECONDS);
		scheduledFuture.cancel(true);
		endLatch.await(1000, TimeUnit.MILLISECONDS);
		// the job is not scheduled anymore
		assertEquals(0, scheduler.getJobNames(Scheduler.DEFAULT_GROUP).length);
		assertTrue(hasBeenInterrupted.get());
	}

	@Test
	@DirtiesContext
	public void testExecuteWithFixedRate() throws Exception {
		CountDownLatch latch = new CountDownLatch(2);
		AtomicBoolean hasRun = new AtomicBoolean(false);
		SimpleRunnable runnable = new SimpleRunnable(latch, hasRun);
		scheduleServiceProvider.scheduleAtFixedRate(runnable, 500, 500,	TimeUnit.MILLISECONDS);
		latch.await(3000, TimeUnit.MILLISECONDS);
		assertTrue(hasRun.get());
	}

	@Test
	@DirtiesContext
	public void testExecuteWithCron() throws Exception {
		CountDownLatch latch = new CountDownLatch(2);
		AtomicBoolean hasRun = new AtomicBoolean(false);
		SimpleRunnable runnable = new SimpleRunnable(latch, hasRun);
		scheduleServiceProvider.scheduleWithCronExpression(runnable, "0/2 * * * * ?");
		latch.await(6000, TimeUnit.MILLISECONDS);
		assertTrue(hasRun.get());
	}

	@Test
	@DirtiesContext
	public void testExecuteWithFixedDelay() throws Exception {
		CountDownLatch latch = new CountDownLatch(2);
		AtomicBoolean hasRun = new AtomicBoolean(false);
		SimpleRunnable runnable = new SimpleRunnable(latch, hasRun);
		scheduleServiceProvider.scheduleWithFixedDelay(runnable, 500, 500, TimeUnit.MILLISECONDS);
		latch.await(3000, TimeUnit.MILLISECONDS);
		assertTrue(hasRun.get());
	}


	private class SimpleRunnable implements Runnable {

		private final CountDownLatch latch;

		private final AtomicBoolean hasRun;

		private final List<Long> executionTimes = new ArrayList<Long>();

		private final long startTime;


		public SimpleRunnable(CountDownLatch latch, AtomicBoolean hasRun) {
			this.latch = latch;
			this.hasRun = hasRun;
			startTime = 0;
		}


		public List<Long> getExecutionTimes() {
			return executionTimes;
		}

		public long getStartTime() {
			return startTime;
		}

		public void run() {
			this.executionTimes.add(new Date().getTime());
			try {
				Thread.sleep(500);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			latch.countDown();
			if (latch.getCount() == 0) {
				hasRun.set(true);
			}
		}

	}

	private class LongRunningRunnable implements Runnable {

		private final CountDownLatch startLatch;

		private final CountDownLatch endLatch;

		private final AtomicBoolean hasBeenInterrupted;


		public LongRunningRunnable(CountDownLatch startLatch, CountDownLatch endLatch, AtomicBoolean hasBeenInterupted) {
			this.startLatch = startLatch;
			this.endLatch = endLatch;
			this.hasBeenInterrupted = hasBeenInterupted;
		}
		

		public void run() {
			// This method differentiates between the interruptions caused by the test and the external ones
			try {
				startLatch.countDown();
				// Sleep for a long time, waiting for an interruption
				try {
					Thread.sleep(2000);
				}
				catch (InterruptedException e) {
					hasBeenInterrupted.set(true);
					Thread.currentThread().interrupt();
				}
				finally {
					endLatch.countDown();
				}
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}

		}
	}
}
