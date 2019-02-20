/*
 * Copyright 2002-2019 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Deque;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.assertj.core.data.Offset;
import org.junit.Ignore;
import org.junit.Test;

import org.springframework.integration.test.util.TestUtils;
import org.springframework.util.StopWatch;

/**
 * @author Dave Syer
 * @author Gary Russell
 * @author Steven Swor
 * @author Artem Bilan
 */
@Ignore("Very sensitive to the time. Don't forget to test after some changes.")
public class ExponentialMovingAverageRateTests {

	private static final Log logger = LogFactory.getLog(ExponentialMovingAverageRateTests.class);

	private final ExponentialMovingAverageRate history = new ExponentialMovingAverageRate(1., 10., 10, true);

	@Test
	public void testGetCount() {
		assertThat(history.getCount()).isEqualTo(0);
		history.increment();
		assertThat(history.getCount()).isEqualTo(1);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testGetTimeSinceLastMeasurement() {
		long sleepTime = 20L;

		// fill history with the same value.
		long now = System.nanoTime() - 2 * sleepTime * 1000000;
		for (int i = 0; i < TestUtils.getPropertyValue(history, "retention", Integer.class); i++) {
			history.increment(now);
		}
		final Deque<Long> times = TestUtils.getPropertyValue(history, "times", Deque.class);
		assertThat(times.peekFirst()).isEqualTo(Long.valueOf(now));
		assertThat(times.peekLast()).isEqualTo(Long.valueOf(now));

		//increment just so we'll have a different value between first and last
		history.increment(System.nanoTime() - sleepTime * 1000000);
		assertThat(times.peekLast()).isNotEqualTo(times.peekFirst());

		/*
		 * We've called Thread.sleep twice with the same value in quick
		 * succession. If timeSinceLastSend is pulling off the correct end of
		 * the queue, then we should be closer to the sleep time than we are to
		 * 2 x sleepTime, but we should definitely be greater than the sleep
		 * time.
		 */
		double timeSinceLastMeasurement = history.getTimeSinceLastMeasurement();
		assertThat(timeSinceLastMeasurement > sleepTime).isTrue();
		assertThat(timeSinceLastMeasurement <= (1.5 * sleepTime)).isTrue();
	}

	@Test
	public void testGetEarlyMean() throws Exception {
		long t0 = System.currentTimeMillis();
		assertThat(history.getMean()).isCloseTo(0, Offset.offset(0.01));
		Thread.sleep(20L);
		history.increment();
		long elapsed = System.currentTimeMillis() - t0;
		if (elapsed < 30L) {
			assertThat(history.getMean() > 10).isTrue();
		}
		else {
			logger.warn("Test took too long to verify mean");
		}
	}

	@Test
	public void testGetMean() throws Exception {
		long t0 = System.currentTimeMillis();
		assertThat(history.getMean()).isCloseTo(0, Offset.offset(0.01));
		Thread.sleep(20L);
		history.increment();
		Thread.sleep(20L);
		history.increment();
		double before = history.getMean();
		Statistics statisticsBefore = history.getStatistics();
		long elapsed = System.currentTimeMillis() - t0;
		if (elapsed < 50L) {
			assertThat(before > 10).isTrue();
			Thread.sleep(20L);
			elapsed = System.currentTimeMillis() - t0;
			if (elapsed < 80L) {
				assertThat(history.getMean()).isLessThan(before);
				assertThat(history.getStatistics().getMean()).isLessThan(statisticsBefore.getMean());
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
	public void testGetStandardDeviation() throws Exception {
		assertThat(history.getStandardDeviation()).isCloseTo(0, Offset.offset(0.01));
		Thread.sleep(20L);
		history.increment();
		Thread.sleep(22L);
		history.increment();
		Thread.sleep(18L);
		assertThat(history.getStandardDeviation() > 0).as("Standard deviation should be non-zero: " + history).isTrue();
	}

	@Test
	public void testReset() throws Exception {
		assertThat(history.getStandardDeviation()).isCloseTo(0, Offset.offset(0.01));
		history.increment();
		Thread.sleep(30L);
		history.increment();
		assertThat(0.0).isNotEqualTo(history.getStandardDeviation());
		history.reset();
		assertThat(history.getStandardDeviation()).isCloseTo(0, Offset.offset(0.01));
		assertThat(history.getCount()).isEqualTo(0);
		assertThat(history.getTimeSinceLastMeasurement()).isCloseTo(0, Offset.offset(0.01));
		assertThat(history.getMean()).isCloseTo(0, Offset.offset(0.01));
		assertThat(history.getMin()).isCloseTo(0, Offset.offset(0.01));
		assertThat(history.getMax()).isCloseTo(0, Offset.offset(0.01));
	}

	@Test
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
		assertThat(rate.getMean()).isEqualTo(calculatedRate, Offset.offset(4000000d));
	}

	@Test
	public void testPerf() {
		ExponentialMovingAverageRate rate = new ExponentialMovingAverageRate(1, 60, 10);
		for (int i = 0; i < 1000000; i++) {
			rate.increment();
		}
	}

}
