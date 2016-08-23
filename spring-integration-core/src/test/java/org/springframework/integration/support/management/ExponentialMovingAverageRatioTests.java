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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;

import java.util.Deque;

import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;

import org.springframework.integration.test.util.TestUtils;

/**
 * @author Dave Syer
 * @author Gary Russell
 * @author Artem Bilan
 * @author Steven Swor
 */
@Ignore("Very sensitive to the time. Don't forget to test after some changes.")
public class ExponentialMovingAverageRatioTests {

	private final ExponentialMovingAverageRatio history = new ExponentialMovingAverageRatio(
			0.5, 10, true);

	@Test
	public void testGetCount() {
		assertEquals(0, history.getCount());
		history.success();
		assertEquals(1, history.getCount());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testGetTimeSinceLastMeasurement() throws Exception {
		long sleepTime = 20L;
		// fill history with the same value.
		long now = System.nanoTime() - 2 * sleepTime * 1000000;
		for (int i = 0; i < TestUtils.getPropertyValue(history, "retention", Integer.class); i++) {
			history.success(now);
		}
		final Deque<Long> times = TestUtils.getPropertyValue(history, "times", Deque.class);
		assertEquals(Long.valueOf(now), times.peekFirst());
		assertEquals(Long.valueOf(now), times.peekLast());

		//increment just so we'll have a different value between first and last
		history.success(System.nanoTime() - sleepTime * 1000000);
		assertNotEquals(times.peekFirst(), times.peekLast());

		/*
		 * We've called Thread.sleep twice with the same value in quick
		 * succession. If timeSinceLastSend is pulling off the correct end of
		 * the queue, then we should be closer to the sleep time than we are to
		 * 2 x sleepTime, but we should definitely be greater than the sleep
		 * time.
		 */
		double timeSinceLastMeasurement = history.getTimeSinceLastMeasurement();
		assertThat(timeSinceLastMeasurement, Matchers.greaterThan((double) (sleepTime / 100)));
		assertThat(timeSinceLastMeasurement, Matchers.lessThanOrEqualTo(1.5 * sleepTime / 100));
	}

	@Test
	public void testGetEarlyMean() throws Exception {
		assertEquals(1, history.getMean(), 0.01);
		history.success();
		assertEquals(1, history.getMean(), 0.01);
	}

	@Test
	public void testGetEarlyFailure() throws Exception {
		assertEquals(1, history.getMean(), 0.01);
		history.failure();
		assertEquals(0, history.getMean(), 0.01);
	}

	@Test
	public void testDecayedMean() throws Exception {
		history.failure(System.nanoTime() - 200000000);
		assertEquals(average(0, Math.exp(-0.4)), history.getMean(), 0.01);
	}

	@Test
	public void testGetMean() throws Exception {
		assertEquals(1, history.getMean(), 0.01);
		history.success();
		assertEquals(1, history.getMean(), 0.01);
		history.success();
		assertEquals(1, history.getMean(), 0.01);
		history.success();
		assertEquals(1, history.getMean(), 0.01);
	}

	@Test
	public void testGetMeanFailuresHighRate() throws Exception {
		assertEquals(1, history.getMean(), 0.01);
		history.success(); // need an extra now that we can't determine the time between the first and previous
		history.success();
		assertEquals(average(1), history.getMean(), 0.01);
		history.failure();
		assertEquals(average(1, 0.5), history.getMean(), 0.1);
		history.success();
		assertEquals(average(1, 0.5, 0.67), history.getMean(), 0.1);
	}

	@Test
	public void testGetMeanFailuresLowRate() throws Exception {
		assertEquals(1, history.getMean(), 0.01);
		history.failure(); // need an extra now that we can't determine the time between the first and previous
		history.failure();
		assertEquals(average(0), history.getMean(), 0.01);
		history.failure();
		assertEquals(average(0, 0), history.getMean(), 0.01);
		history.success();
		assertEquals(average(0, 0, 0.33), history.getMean(), 0.1);
	}

	@Test
	public void testGetStandardDeviation() throws Exception {
		assertEquals(0, history.getStandardDeviation(), 0.01);
		history.success();
		assertEquals(0, history.getStandardDeviation(), 1);
	}

	@Test
	public void testReset() throws Exception {
		assertEquals(0, history.getStandardDeviation(), 0.01);
		history.success();
		history.failure();
		assertThat(history.getStandardDeviation(), not(equalTo(0)));
		history.reset();
		assertEquals(0, history.getStandardDeviation(), 0.01);
		assertEquals(0, history.getCount());
		assertEquals(0, history.getTimeSinceLastMeasurement(), 0.01);
		assertEquals(1, history.getMean(), 0.01);
		assertEquals(0, history.getMin(), 0.01);
		assertEquals(0, history.getMax(), 0.01);
		history.success();
		assertEquals(1, history.getMin(), 0.01);
	}

	private double average(double... values) {
		int count = 0;
		double sum = 0;
		for (double d : values) {
			sum += d;
			count++;
		}
		return sum / count;
	}

	@Test
	public void testRatio() {
		ExponentialMovingAverageRatio ratio = new ExponentialMovingAverageRatio(60, 10, true);
		for (int i = 0; i < 100; i++) {
			if (i % 10 == 1) {
				ratio.failure();
			}
			else {
				ratio.success();
			}
		}
		assertEquals(0.9, ratio.getMax(), 0.02);
		assertEquals(0.9, ratio.getMean(), 0.03);
	}

	@Test
	@Ignore
	public void testPerf() {
		ExponentialMovingAverageRatio ratio = new ExponentialMovingAverageRatio(60, 10);
		for (int i = 0; i < 100000; i++) {
			if (i % 10 == 0) {
				ratio.failure();
			}
			else {
				ratio.success();
			}
		}
	}

}
