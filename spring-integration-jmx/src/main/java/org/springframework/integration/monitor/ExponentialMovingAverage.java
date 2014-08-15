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
 * Cumulative statistics for a series of real numbers with higher weight given to recent data but without storing any
 * history. Clients call {@link #append(double)} every time there is a new measurement, and then can collect summary
 * statistics from the convenience getters (e.g. {@link #getStatistics()}). Older values are given exponentially smaller
 * weight, with a decay factor determined by a "window" size chosen by the caller. The result is a good approximation to
 * the statistics of the series but with more weight given to recent measurements, so if the statistics change over time
 * those trends can be approximately reflected.
 *
 * @author Dave Syer
 * @since 2.0
 */
public class ExponentialMovingAverage {

	private volatile long count;

	private volatile double weight;

	private volatile double sum;

	private volatile double sumSquares;

	private volatile double min;

	private volatile double max;

	private final double decay;


	/**
	 * Create a moving average accumulator with decay lapse window provided. Measurements older than this will have
	 * smaller weight than <code>1/e</code>.
	 *
	 * @param window the exponential lapse window (number of measurements)
	 */
	public ExponentialMovingAverage(int window) {
		this.decay = 1 - 1. / window;
	}


	public synchronized void reset() {
		weight = 0;
		sum = 0;
		sumSquares = 0;
		count = 0;
		min = 0;
		max = 0;
	}

	/**
	 * Add a new measurement to the series.
	 *
	 * @param value the measurement to append
	 */
	public synchronized void append(double value) {
		if (value > max || count == 0) {
			max = value;
		}
		if (value < min || count == 0) {
			min = value;
		}
		sum = decay * sum + value;
		sumSquares = decay * sumSquares + value * value;
		weight = decay * weight + 1;
		count++;
	}

	/**
	 * @return the number of measurements recorded
	 */
	public int getCount() {
		return (int) count;
	}

	/**
	 * @return the number of measurements recorded
	 */
	public long getCountLong() {
		return count;
	}

	/**
	 * @return the mean value
	 */
	public double getMean() {
		return weight > 0 ? sum / weight : 0.;
	}

	/**
	 * @return the approximate standard deviation
	 */
	public double getStandardDeviation() {
		double mean = getMean();
		double var = weight > 0 ? sumSquares / weight - mean * mean : 0.;
		return var > 0 ? Math.sqrt(var) : 0;
	}

	/**
	 * @return the maximum value recorded (not weighted)
	 */
	public double getMax() {
		return max;
	}

	/**
	 * @return the minimum value recorded (not weighted)
	 */
	public double getMin() {
		return min;
	}

	/**
	 * @return summary statistics (count, mean, standard deviation etc.)
	 */
	public Statistics getStatistics() {
		return new Statistics(count, min, max, getMean(), getStandardDeviation());
	}

	@Override
	public String toString() {
		return getStatistics().toString();
	}

}
