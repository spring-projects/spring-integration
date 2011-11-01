/*
 * Copyright 2002-2010 the original author or authors.
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
package org.springframework.integration.monitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Dave Syer
 * @author Artem Bilan
 *
 */
public class ExponentialMovingAverageTests {

	private ExponentialMovingAverage history = new ExponentialMovingAverage(10);

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
		assertFalse(0==history.getStandardDeviation());
		history.reset();
		assertEquals(0, history.getStandardDeviation(), 0.01);
		// INT-2165
		assertEquals(String.format("[N=%d, min=%f, max=%f, mean=%f, sigma=%f]", 0, 0d, 0d, 0d, 0d), history.toString());
	}

}
