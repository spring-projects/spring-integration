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
 * Cumulative statistics for an event rate with higher weight given to recent data but without storing any history.
 * Clients call {@link #increment()} when a new event occurs, and then use convenience methods (e.g. {@link #getMean()})
 * to retrieve estimates of the rate of event arrivals and the statistics of the series. Older values are given
 * exponentially smaller weight, with a decay factor determined by a duration chosen by the client. The rate measurement
 * weights decay in two dimensions:
 * <ul>
 * <li>in time according to the lapse period supplied: <code>weight = exp((t0-t)/T)</code> where <code>t0</code> is the
 * last measurement time, <code>t</code> is the current time and <code>T</code> is the lapse period)</li>
 * <li>per measurement according to the lapse window supplied: <code>weight = exp(-i/L)</code> where <code>L</code> is
 * the lapse window and <code>i</code> is the sequence number of the measurement.</li>
 * </ul>
 *
 * @author Dave Syer
 * @author Gary Russell
 *
 */
public class ExponentialMovingAverageRate {

	private final ExponentialMovingAverage rates;

	private volatile double weight;

	private volatile double sum;

	private volatile double min;

	private volatile double max;

	private volatile long t0 = System.currentTimeMillis();

	private final double lapse;

	private final double period;


	/**
	 * @param period the period to base the rate measurement (in seconds)
	 * @param lapsePeriod the exponential lapse rate for the rate average (in seconds)
	 * @param window the exponential lapse window (number of measurements)
	 */
	public ExponentialMovingAverageRate(double period, double lapsePeriod, int window) {
		rates = new ExponentialMovingAverage(window);
		this.lapse = lapsePeriod > 0 ? 0.001 / lapsePeriod : 0; // convert to milliseconds
		this.period = period * 1000; // convert to milliseconds
	}


	public synchronized void reset() {
		min = 0;
		max = 0;
		weight = 0;
		sum = 0;
		t0 = System.currentTimeMillis();
		rates.reset();
	}

	/**
	 * Add a new event to the series.
	 */
	public synchronized void increment() {
		long t = System.currentTimeMillis();
		double value = t > t0 ? (t - t0) / period : 0;
		if (value > max || getCount() == 0) {
			max = value;
		}
		if (value < min || getCount() == 0) {
			min = value;
		}
		double alpha = Math.exp((t0 - t) * lapse);
		t0 = t;
		sum = alpha * sum + value;
		weight = alpha * weight + 1;
		rates.append(sum > 0 ? weight / sum : 0);
	}

	/**
	 * @return the number of measurements recorded
	 */
	public int getCount() {
		return rates.getCount();
	}

	/**
	 * @return the number of measurements recorded
	 * @since 3.0
	 */
	public long getCountLong() {
		return rates.getCountLong();
	}

	/**
	 * @return the time in seconds since the last measurement
	 */
	public double getTimeSinceLastMeasurement() {
		return (System.currentTimeMillis() - t0) / 1000.;
	}

	/**
	 * @return the mean value
	 */
	public double getMean() {
		long count = rates.getCountLong();
		if (count == 0) {
			return 0;
		}
		long t = System.currentTimeMillis();
		double value = t > t0 ? (t - t0) / period : 0;
		return count / (count / rates.getMean() + value);
	}

	/**
	 * @return the approximate standard deviation
	 */
	public double getStandardDeviation() {
		return rates.getStandardDeviation();
	}

	/**
	 * @return the maximum value recorded (not weighted)
	 */
	public double getMax() {
		return min > 0 ? 1 / min : 0;
	}

	/**
	 * @return the minimum value recorded (not weighted)
	 */
	public double getMin() {
		return max > 0 ? 1 / max : 0;
	}

	/**
	 * @return summary statistics (count, mean, standard deviation etc.)
	 */
	public Statistics getStatistics() {
		return new Statistics(getCount(), min, max, getMean(), getStandardDeviation());
	}

	@Override
	public String toString() {
		return String.format("[%s, timeSinceLast=%f]", getStatistics(), getTimeSinceLastMeasurement());
	}

}
