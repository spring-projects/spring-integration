/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.monitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Ignore;
import org.junit.Test;

import org.springframework.integration.test.util.TestUtils;

/**
 * @author Dave Syer
 * @author Gary Russell
 *
 */
public class ExponentialMovingAverageRateTests {

	private final static Log logger = LogFactory.getLog(ExponentialMovingAverageRateTests.class);

	private final ExponentialMovingAverageRate history = new ExponentialMovingAverageRate(1., 10., 10);

	@Test
	public void testWindow() {
		ExponentialMovingAverageRate rate = new ExponentialMovingAverageRate(1., 10., 20);
		double decay = TestUtils.getPropertyValue(rate, "rates.decay", Double.class);
		assertEquals(95, (int) (decay * 100.));
	}

	@Test
	public void testGetCount() {
		assertEquals(0, history.getCount());
		history.increment();
		assertEquals(1, history.getCount());
	}

	@Test
	public void testGetTimeSinceLastMeasurement() throws Exception {
		history.increment();
		Thread.sleep(20L);
		assertTrue(history.getTimeSinceLastMeasurement() > 0);
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
				assertTrue(history.getMean() < before);
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
		// System.err.println(history);
		assertTrue("Standard deviation should be non-zero: " + history, history.getStandardDeviation() > 0);
	}

	@Test
	@Ignore
	public void testReset() throws Exception {
		assertEquals(0, history.getStandardDeviation(), 0.01);
		history.increment();
		Thread.sleep(30L);
		history.increment();
		assertFalse(0==history.getStandardDeviation());
		history.reset();
		assertEquals(0, history.getStandardDeviation(), 0.01);
		assertEquals(0, history.getCount());
		assertEquals(0, history.getTimeSinceLastMeasurement(), 0.01);
		assertEquals(0, history.getMean(), 0.01);
		assertEquals(0, history.getMin(), 0.01);
		assertEquals(0, history.getMax(), 0.01);
	}

}
