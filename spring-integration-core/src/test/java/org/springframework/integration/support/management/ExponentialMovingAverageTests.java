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

import org.assertj.core.data.Offset;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Dave Syer
 * @author Artem Bilan
 * @author Gary Russell
 */
@Ignore("Very sensitive to the time. Don't forget to test after some changes.")
public class ExponentialMovingAverageTests {

	private final ExponentialMovingAverage history = new ExponentialMovingAverage(10);


	@Test
	@Ignore // used to compare LinkedList to ArrayDeque which was 35% faster
	public void perf() {
		for (int i = 0; i < 100000000; i++) {
			history.append(0.0);
		}
	}

	@Test
	public void testGetCount() {
		assertThat(history.getCount()).isEqualTo(0);
		history.append(1);
		assertThat(history.getCount()).isEqualTo(1);
	}

	@Test
	public void testGetMean() {
		assertThat(history.getMean()).isCloseTo(0, Offset.offset(0.01));
		history.append(1);
		history.append(1);
		assertThat(history.getMean()).isCloseTo(1, Offset.offset(0.01));
	}

	@Test
	public void testGetStandardDeviation() {
		assertThat(history.getStandardDeviation()).isCloseTo(0, Offset.offset(0.01));
		history.append(1);
		history.append(1);
		assertThat(history.getStandardDeviation()).isCloseTo(0, Offset.offset(0.01));
	}

	@Test
	public void testReset() {
		assertThat(history.getStandardDeviation()).isCloseTo(0, Offset.offset(0.01));
		history.append(1);
		history.append(2);
		assertThat(0 == history.getStandardDeviation()).isFalse();
		history.reset();
		assertThat(history.getStandardDeviation()).isCloseTo(0, Offset.offset(0.01));
		// INT-2165
		assertThat(history.toString())
				.isEqualTo(String.format("[N=%d, min=%f, max=%f, mean=%f, sigma=%f]", 0, 0d, 0d, 0d, 0d));
		history.append(1);
		assertThat(history.getMin()).isCloseTo(1, Offset.offset(0.01));
	}

	@Test
	public void testAv() {
		ExponentialMovingAverage av = new ExponentialMovingAverage(10);
		for (int i = 0; i < 10000; i++) {
			switch (i % 3) {
				case 0:
					av.append(20);
					break;
				case 1:
					av.append(30);
					break;
				case 2:
					av.append(40);
					break;
			}
		}
		assertThat(av.getMax()).isCloseTo(40, Offset.offset(0.1));
		assertThat(av.getMin()).isCloseTo(20, Offset.offset(0.1));
		assertThat(av.getMean()).isCloseTo(30, Offset.offset(1.0));
	}

	@Test
	@Ignore
	public void testPerf() {
		ExponentialMovingAverage av = new ExponentialMovingAverage(10);
		for (int i = 0; i < 10000000; i++) {
			switch (i % 4) {
				case 0:
					av.append(20);
					break;
				case 1:
					av.append(30);
					break;
				case 2:
					av.append(40);
					break;

				case 3:
					av.append(50);
					break;
			}
		}
	}

}
