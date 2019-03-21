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

package org.springframework.integration.support.management;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Deque;

import org.assertj.core.data.Offset;
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

	private final ExponentialMovingAverageRatio history = new ExponentialMovingAverageRatio(0.5, 10, true);

	@Test
	public void testGetCount() {
		assertThat(history.getCount()).isEqualTo(0);
		history.success();
		assertThat(history.getCount()).isEqualTo(1);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testGetTimeSinceLastMeasurement() {
		long sleepTime = 20L;
		// fill history with the same value.
		long now = System.nanoTime() - 2 * sleepTime * 1000000;
		for (int i = 0; i < TestUtils.getPropertyValue(history, "retention", Integer.class); i++) {
			history.success(now);
		}
		final Deque<Long> times = TestUtils.getPropertyValue(history, "times", Deque.class);
		assertThat(times.peekFirst()).isEqualTo(Long.valueOf(now));
		assertThat(times.peekLast()).isEqualTo(Long.valueOf(now));

		//increment just so we'll have a different value between first and last
		history.success(System.nanoTime() - sleepTime * 1000000);
		assertThat(times.peekLast()).isNotEqualTo(times.peekFirst());

		/*
		 * We've called Thread.sleep twice with the same value in quick
		 * succession. If timeSinceLastSend is pulling off the correct end of
		 * the queue, then we should be closer to the sleep time than we are to
		 * 2 x sleepTime, but we should definitely be greater than the sleep
		 * time.
		 */
		double timeSinceLastMeasurement = history.getTimeSinceLastMeasurement();
		assertThat(timeSinceLastMeasurement).isGreaterThan((double) (sleepTime / 100));
		assertThat(timeSinceLastMeasurement).isLessThanOrEqualTo(1.5 * sleepTime / 100);
	}

	@Test
	public void testGetEarlyMean() {
		assertThat(history.getMean()).isCloseTo(1, Offset.offset(0.01));
		history.success();
		assertThat(history.getMean()).isCloseTo(1, Offset.offset(0.01));
	}

	@Test
	public void testGetEarlyFailure() {
		assertThat(history.getMean()).isCloseTo(1, Offset.offset(0.01));
		history.failure();
		assertThat(history.getMean()).isCloseTo(0, Offset.offset(0.01));
	}

	@Test
	public void testDecayedMean() throws Exception {
		history.failure(System.nanoTime() - 200000000);
		assertThat(history.getMean()).isCloseTo(average(0, Math.exp(-0.4)), Offset.offset(0.01));
		history.success();
		history.failure();
		double mean = history.getMean();
		Statistics statistics = history.getStatistics();
		Thread.sleep(50);
		assertThat(history.getMean()).isGreaterThan(mean);
		assertThat(history.getStatistics().getMean()).isGreaterThan(statistics.getMean());
	}

	@Test
	public void testGetMean() {
		assertThat(history.getMean()).isCloseTo(1, Offset.offset(0.01));
		history.success();
		assertThat(history.getMean()).isCloseTo(1, Offset.offset(0.01));
		history.success();
		assertThat(history.getMean()).isCloseTo(1, Offset.offset(0.01));
		history.success();
		assertThat(history.getMean()).isCloseTo(1, Offset.offset(0.01));
	}

	@Test
	public void testGetMeanFailuresHighRate() {
		assertThat(history.getMean()).isCloseTo(1, Offset.offset(0.01));
		history.success(); // need an extra now that we can't determine the time between the first and previous
		history.success();
		assertThat(history.getMean()).isCloseTo(average(1), Offset.offset(0.01));
		history.failure();
		assertThat(history.getMean()).isCloseTo(average(1, 0.5), Offset.offset(0.1));
		history.success();
		assertThat(history.getMean()).isCloseTo(average(1, 0.5, 0.67), Offset.offset(0.1));
	}

	@Test
	public void testGetMeanFailuresLowRate() {
		assertThat(history.getMean()).isCloseTo(1, Offset.offset(0.01));
		history.failure(); // need an extra now that we can't determine the time between the first and previous
		history.failure();
		assertThat(history.getMean()).isCloseTo(average(0), Offset.offset(0.01));
		history.failure();
		assertThat(history.getMean()).isCloseTo(average(0, 0), Offset.offset(0.01));
		history.success();
		assertThat(history.getMean()).isCloseTo(average(0, 0, 0.33), Offset.offset(0.1));
	}

	@Test
	public void testGetStandardDeviation() {
		assertThat(history.getStandardDeviation()).isCloseTo(0, Offset.offset(0.01));
		history.success();
		assertThat(history.getStandardDeviation()).isCloseTo(0, Offset.offset(1d));
	}

	@Test
	public void testReset() {
		assertThat(history.getStandardDeviation()).isCloseTo(0, Offset.offset(0.01));
		history.success();
		history.failure();
		assertThat(history.getStandardDeviation()).isNotEqualTo(0);
		history.reset();
		assertThat(history.getStandardDeviation()).isCloseTo(0, Offset.offset(0.01));
		assertThat(history.getCount()).isEqualTo(0);
		assertThat(history.getTimeSinceLastMeasurement()).isCloseTo(0, Offset.offset(0.01));
		assertThat(history.getMean()).isCloseTo(1, Offset.offset(0.01));
		assertThat(history.getMin()).isCloseTo(0, Offset.offset(0.01));
		assertThat(history.getMax()).isCloseTo(0, Offset.offset(0.01));
		history.success();
		assertThat(history.getMin()).isCloseTo(1, Offset.offset(0.01));
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
		assertThat(ratio.getMax()).isCloseTo(0.9, Offset.offset(0.02));
		assertThat(ratio.getMean()).isCloseTo(0.9, Offset.offset(0.03));
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
