/*
 * Copyright 2009-2016 the original author or authors.
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;



/**
 * Cumulative statistics for a series of real numbers with higher weight given to recent data.
 * Clients call {@link #append(double)} every time there is a new measurement, and then can collect summary
 * statistics from the convenience getters (e.g. {@link #getStatistics()}). Older values are given exponentially smaller
 * weight, with a decay factor determined by a "window" size chosen by the caller. The result is a good approximation to
 * the statistics of the series but with more weight given to recent measurements, so if the statistics change over time
 * those trends can be approximately reflected. For performance reasons, the calculation is performed on retrieval,
 * {@code window * 5} samples are retained meaning that the earliest retained value contributes just 0.5% to the
 * sum.
 *
 * @author Dave Syer
 * @author Gary Russell
 * @since 2.0
 */
public class ExponentialMovingAverage {

	private volatile long count;

	private volatile double min = Double.MAX_VALUE;

	private volatile double max;

	private final Deque<Double> samples = new ArrayDeque<Double>();

	private final int retention;

	private final int window;

	private final double factor;


	/**
	 * Create a moving average accumulator with decay lapse window provided. Measurements older than this will have
	 * smaller weight than <code>1/e</code>.
	 * @param window the exponential lapse window (number of measurements)
	 */
	public ExponentialMovingAverage(int window) {
		this(window, 1);
	}

	/**
	 * Create a moving average accumulator with decay lapse window provided. Measurements older than this will have
	 * smaller weight than <code>1/e</code>.
	 * @param window the exponential lapse window (number of measurements)
	 * @param factor a factor by which raw values are reduced during analysis; e.g. to analyze in ms and
	 * raw values are ns, set the factor to 1000000.0.
	 * @since 4.2
	 */
	public ExponentialMovingAverage(int window, double factor) {
		this.window = window;
		this.retention = window * 5; // last retained value contributes just 0.5% to the sum
		this.factor = factor;
	}

	public synchronized void reset() {
		this.count = 0;
		this.min = Double.MAX_VALUE;
		this.max = 0;
		this.samples.clear();
	}

	/**
	 * Add a new measurement to the series.
	 * @param value the measurement to append
	 */
	public synchronized void append(double value) {
		if (this.samples.size() == this.retention) {
			this.samples.poll();
		}
		this.samples.add(value);
		this.count++; //NOSONAR - false positive, we're synchronized
	}

	private Statistics calc() {
		List<Double> copy;
		long count;
		synchronized (this) {
			copy = new ArrayList<Double>(this.samples);
			count = this.count;
		}
		double sum = 0;
		double decay = 1 - 1. / this.window;
		double sumSquares = 0;
		double weight = 0;
		double min = this.min;
		double max = this.max;
		for (Double value : copy) {
			value /= this.factor;
			if (value > max) {
				max = value;
			}
			if (value < min) {
				min = value;
			}
			sum = decay * sum + value;
			sumSquares = decay * sumSquares + value * value;
			weight = decay * weight + 1;
		}
		synchronized (this) {
			if (max > this.max) {
				this.max = max;
			}
			if (min < this.min) {
				this.min = min;
			}
		}
		double mean = weight > 0 ? sum / weight : 0.;
		double var = weight > 0 ? sumSquares / weight - mean * mean : 0.;
		double standardDeviation =  var > 0 ? Math.sqrt(var) : 0;
		return new Statistics(count, min == Double.MAX_VALUE ? 0 : min, max, mean, standardDeviation); //NOSONAR
	}

	/**
	 * @return the number of measurements recorded
	 */
	public int getCount() {
		return (int) this.count;
	}

	/**
	 * @return the number of measurements recorded
	 */
	public long getCountLong() {
		return this.count;
	}

	/**
	 * @return the mean value
	 */
	public double getMean() {
		return calc().getMean();
	}

	/**
	 * @return the approximate standard deviation
	 */
	public double getStandardDeviation() {
		return calc().getStandardDeviation();
	}

	/**
	 * @return the maximum value recorded (not weighted)
	 */
	public double getMax() {
		return calc().getMax();
	}

	/**
	 * @return the minimum value recorded (not weighted)
	 */
	public double getMin() {
		return calc().getMin();
	}

	/**
	 * @return summary statistics (count, mean, standard deviation etc.)
	 */
	public Statistics getStatistics() {
		return calc();
	}

	@Override
	public String toString() {
		return getStatistics().toString();
	}

}
