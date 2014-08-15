/*
 * Copyright 2009-2014 the original author or authors.
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

/**
 * Cumulative statistics for success ratio with higher weight given to recent data but without storing any history.
 * Clients call {@link #success()} or {@link #failure()} when an event occurs, and the ratio of success to total events
 * is accumulated. Older values are given exponentially smaller weight, with a decay factor determined by a duration
 * chosen by the client. The rate measurement weights decay in two dimensions:
 * <ul>
 * <li>in time according to the lapse period supplied: <code>weight = exp((t0-t)/T)</code> where <code>t0</code> is the
 * last measurement time, <code>t</code> is the current time and <code>T</code> is the lapse period)</li>
 * <li>per measurement according to the lapse window supplied: <code>weight = exp(-i/L)</code> where <code>L</code> is
 * the lapse window and <code>i</code> is the sequence number of the measurement.</li>
 * </ul>
 *
 * @author Dave Syer
 * @since 2.0
 */
public class ExponentialMovingAverageRatio {

	private volatile double weight;

	private volatile double sum;

	private volatile long t0 = System.currentTimeMillis();

	private final double lapse;

	private final ExponentialMovingAverage cumulative;


	/**
	 * @param lapsePeriod the exponential lapse rate for the rate average (in seconds)
	 * @param window the exponential lapse window (number of measurements)
	 */
	public ExponentialMovingAverageRatio(double lapsePeriod, int window) {
		this.cumulative = new ExponentialMovingAverage(window);
		this.lapse = lapsePeriod > 0 ? 0.001 / lapsePeriod : 0; // convert to millisecs
	}


	/**
	 * Add a new event with successful outcome.
	 */
	public void success() {
		append(1);
	}

	/**
	 * Add a new event with failed outcome.
	 */
	public void failure() {
		append(0);
	}

	public synchronized void reset() {
		weight = 0;
		sum = 0;
		t0 = System.currentTimeMillis();
		cumulative.reset();
	}

	private synchronized void append(int value) {
		long t = System.currentTimeMillis();
		double alpha = Math.exp((t0 - t) * lapse);
		t0 = t;
		sum = alpha * sum + value;
		weight = alpha * weight + 1;
		cumulative.append(sum / weight);
	}

	/**
	 * @return the number of measurements recorded
	 */
	public int getCount() {
		return cumulative.getCount();
	}

	/**
	 * @return the number of measurements recorded
	 */
	public long getCountLong() {
		return cumulative.getCountLong();
	}

	/**
	 * @return the time in seconds since the last measurement
	 */
	public double getTimeSinceLastMeasurement() {
		return (System.currentTimeMillis() - t0) / 1000.;
	}

	/**
	 * @return the mean success rate
	 */
	public double getMean() {
		long count = cumulative.getCountLong();
		if (count == 0) {
			// Optimistic to start: success rate is 100%
			return 1;
		}
		long t = System.currentTimeMillis();
		double alpha = Math.exp((t0 - t) * lapse);
		return alpha * cumulative.getMean() + 1 - alpha;
	}

	/**
	 * @return the approximate standard deviation of the success rate measurements
	 */
	public double getStandardDeviation() {
		return cumulative.getStandardDeviation();
	}

	/**
	 * @return the maximum value recorded of the exponential weighted average (per measurement) success rate
	 */
	public double getMax() {
		return cumulative.getMax();
	}

	/**
	 * @return the minimum value recorded of the exponential weighted average (per measurement) success rate
	 */
	public double getMin() {
		return cumulative.getMin();
	}

	/**
	 * @return summary statistics (count, mean, standard deviation etc.)
	 */
	public Statistics getStatistics() {
		return new Statistics(getCount(), getMin(), getMax(), getMean(), getStandardDeviation());
	}

	@Override
	public String toString() {
		return String.format("[%s, timeSinceLast=%f]", getStatistics(), getTimeSinceLastMeasurement());
	}

}
