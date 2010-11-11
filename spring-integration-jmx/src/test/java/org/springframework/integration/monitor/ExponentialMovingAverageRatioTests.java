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
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * @author Dave Syer
 * 
 */
public class ExponentialMovingAverageRatioTests {

	private ExponentialMovingAverageRatio history = new ExponentialMovingAverageRatio(
			0.5, 10);

	@Test
	public void testGetCount() {
		assertEquals(0, history.getCount());
		history.success();
		assertEquals(1, history.getCount());
	}

	@Test
	public void testGetTimeSinceLastMeasurement() throws Exception {
		history.success();
		Thread.sleep(20L);
		assertTrue(history.getTimeSinceLastMeasurement() > 0);
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
		history.failure();
		Thread.sleep(200L);
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
		assertFalse(0==history.getStandardDeviation());
		history.reset();
		assertEquals(0, history.getStandardDeviation(), 0.01);
		assertEquals(0, history.getCount());
		assertEquals(0, history.getTimeSinceLastMeasurement(), 0.01);
		assertEquals(1, history.getMean(), 0.01);
		assertEquals(0, history.getMin(), 0.01);
		assertEquals(0, history.getMax(), 0.01);
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

}
