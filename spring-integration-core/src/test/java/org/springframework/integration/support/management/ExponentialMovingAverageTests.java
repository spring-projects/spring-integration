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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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


	@Test @Ignore // used to compare LinkedList to ArrayDeque which was 35% faster
	public void perf() {
		for (int i = 0; i < 100000000; i++) {
			history.append(0.0);
		}
	}

	@Test
	public void testGetCount() {
		assertEquals(0, history.getCount());
		history.append(1);
		assertEquals(1, history.getCount());
	}

	@Test
	public void testGetMean() throws Exception {
		assertEquals(0, history.getMean(), 0.01);
		history.append(1);
		history.append(1);
		assertEquals(1, history.getMean(), 0.01);
	}

	@Test
	public void testGetStandardDeviation() throws Exception {
		assertEquals(0, history.getStandardDeviation(), 0.01);
		history.append(1);
		history.append(1);
		assertEquals(0, history.getStandardDeviation(), 0.01);
	}

	@Test
	public void testReset() throws Exception {
		assertEquals(0, history.getStandardDeviation(), 0.01);
		history.append(1);
		history.append(2);
		assertFalse(0 == history.getStandardDeviation());
		history.reset();
		assertEquals(0, history.getStandardDeviation(), 0.01);
		// INT-2165
		assertEquals(String.format("[N=%d, min=%f, max=%f, mean=%f, sigma=%f]", 0, 0d, 0d, 0d, 0d), history.toString());
		history.append(1);
		assertEquals(1, history.getMin(), 0.01);
	}

	@Test
	public void testAv() throws Exception {
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
		assertEquals(40, av.getMax(), 0.1);
		assertEquals(20, av.getMin(), 0.1);
		assertEquals(30, av.getMean(), 1.0);
	}

	@Test @Ignore
	public void testPerf() throws Exception {
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
