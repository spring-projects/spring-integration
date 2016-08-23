/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.support.management;

import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Deque;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Ignore;
import org.junit.Test;

import org.springframework.integration.test.util.TestUtils;
import org.springframework.util.StopWatch;

/**
 * @author Dave Syer
 * @author Gary Russell
 * @author Steven Swor
 */
@Ignore("Very sensitive to the time. Don't forget to test after some changes.")
public class ExponentialMovingAverageRateTests {

	private final static Log logger = LogFactory.getLog(ExponentialMovingAverageRateTests.class);

	private final ExponentialMovingAverageRate history = new ExponentialMovingAverageRate(1., 10., 10, true);

	@Test
	public void testGetCount() {
		assertEquals(0, history.getCount());
		history.increment();
		assertEquals(1, history.getCount());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testGetTimeSinceLastMeasurement() throws Exception {
		long sleepTime = 20L;

		// fill history with the same value.
		long now = System.nanoTime() - 2 * sleepTime * 1000000;
		for (int i = 0; i < TestUtils.getPropertyValue(history, "retention", Integer.class); i++) {
			history.increment(now);
		}
		final Deque<Long> times = TestUtils.getPropertyValue(history, "times", Deque.class);
		assertEquals(Long.valueOf(now), times.peekFirst());
		assertEquals(Long.valueOf(now), times.peekLast());

		//increment just so we'll have a different value between first and last
		history.increment(System.nanoTime()  - sleepTime * 1000000);
		assertNotEquals(times.peekFirst(), times.peekLast());

		/*
		 * We've called Thread.sleep twice with the same value in quick
		 * succession. If timeSinceLastSend is pulling off the correct end of
		 * the queue, then we should be closer to the sleep time than we are to
		 * 2 x sleepTime, but we should definitely be greater than the sleep
		 * time.
		*/
		double timeSinceLastMeasurement = history.getTimeSinceLastMeasurement();
		assertTrue(timeSinceLastMeasurement > sleepTime);
		assertTrue(timeSinceLastMeasurement <= (1.5 * sleepTime));
	}

	@Test
	public void testGetEarlyMean() throws Exception {
		long t0 = System.currentTimeMillis();
		assertEquals(0, history.getMean(), 0.01);
		Thread.sleep(20L);
		history.increment();
		long elapsed = System.currentTimeMillis() - t0;
		if (elapsed < 30L) {
			assertTrue(history.getMean() > 10);
		}
		else {
			logger.warn("Test took too long to verify mean");
		}
	}

	@Test
	public void testGetMean() throws Exception {
		long t0 = System.currentTimeMillis();
		assertEquals(0, history.getMean(), 0.01);
		Thread.sleep(20L);
		history.increment();
		Thread.sleep(20L);
		history.increment();
		double before = history.getMean();
		long elapsed = System.currentTimeMillis() - t0;
		if (elapsed < 50L) {
			assertTrue(before > 10);
			Thread.sleep(20L);
			elapsed = System.currentTimeMillis() - t0;
			if (elapsed < 80L) {
				assertThat(history.getMean(), lessThan(before));
			}
			else {
				logger.warn("Test took too long to verify mean");
			}
		}
		else {
			logger.warn("Test took too long to verify mean");
		}
	}

	@Test
	@Ignore
	public void testGetStandardDeviation() throws Exception {
		assertEquals(0, history.getStandardDeviation(), 0.01);
		Thread.sleep(20L);
		history.increment();
		Thread.sleep(22L);
		history.increment();
		Thread.sleep(18L);
		assertTrue("Standard deviation should be non-zero: " + history, history.getStandardDeviation() > 0);
	}

	@Test
	@Ignore
	public void testReset() throws Exception {
		assertEquals(0, history.getStandardDeviation(), 0.01);
		history.increment();
		Thread.sleep(30L);
		history.increment();
		assertFalse(0 == history.getStandardDeviation());
		history.reset();
		assertEquals(0, history.getStandardDeviation(), 0.01);
		assertEquals(0, history.getCount());
		assertEquals(0, history.getTimeSinceLastMeasurement(), 0.01);
		assertEquals(0, history.getMean(), 0.01);
		assertEquals(0, history.getMin(), 0.01);
		assertEquals(0, history.getMax(), 0.01);
	}

	@Test
	@Ignore // tolerance needed is too dependent on hardware
	public void testRate() {
		ExponentialMovingAverageRate rate = new ExponentialMovingAverageRate(1, 60, 10);
		int count = 1000000;
		StopWatch watch = new StopWatch();
		watch.start();
		for (int i = 0; i < count; i++) {
			rate.increment();
		}
		watch.stop();
		double calculatedRate = count / (double) watch.getTotalTimeMillis() * 1000;
		assertEquals(calculatedRate, rate.getMean(), 4000000);
	}

	@Test
	@Ignore
	public void testPerf() {
		ExponentialMovingAverageRate rate = new ExponentialMovingAverageRate(1, 60, 10);
		for (int i = 0; i < 1000000; i++) {
			rate.increment();
		}
	}

}
