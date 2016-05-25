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
 * Cumulative statistics for an event rate with higher weight given to recent data.
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
 * For performance reasons, the calculation is performed on retrieval,
 * {@code window * 5} samples are retained meaning that the earliest retained value contributes just 0.5% to the
 * sum.
 * @author Dave Syer
 * @author Gary Russell
 * @author Steven Swor
 *
 */
public class ExponentialMovingAverageRate {

	private volatile double min = Double.MAX_VALUE;

	private volatile double max;

	private volatile double t0;

	private volatile long count;

	private final double lapse;

	private final double period;

	private final Deque<Long> times = new ArrayDeque<Long>();

	private final int retention;

	private final int window;

	private final double factor;



	/**
	 * @param period the period to base the rate measurement (in seconds)
	 * @param lapsePeriod the exponential lapse rate for the rate average (in seconds)
	 * @param window the exponential lapse window (number of measurements)
	 */
	public ExponentialMovingAverageRate(double period, double lapsePeriod, int window) {
		this(period, lapsePeriod, window, false);
	}

	/**
	 * @param period the period to base the rate measurement (in seconds)
	 * @param lapsePeriod the exponential lapse rate for the rate average (in seconds)
	 * @param window the exponential lapse window (number of measurements)
	 * @param millis when true, analyze the data as milliseconds instead of the native nanoseconds
	 * @since 4.2
	 */
	public ExponentialMovingAverageRate(double period, double lapsePeriod, int window, boolean millis) {
		this.lapse = lapsePeriod > 0 ? 0.001 / lapsePeriod : 0; // convert to milliseconds
		this.period = period * 1000; // convert to milliseconds
		this.window = window;
		this.retention = window * 5;
		this.factor = millis ? 1000000 : 1;
		this.t0 = System.nanoTime() / this.factor;
	}


	public synchronized void reset() {
		this.min = Double.MAX_VALUE;
		this.max = 0;
		this.count = 0;
		this.times.clear();
		this.t0 = System.nanoTime() / this.factor;
	}

	/**
	 * Add a new event to the series.
	 */
	public synchronized void increment() {
		increment(System.nanoTime());
	}

	/**
	 * Add a new event to the series at time t.
	 * @param t a new event to the series (System.nanoTime()).
	 */
	public synchronized void increment(long t) {
		if (this.times.size() == this.retention) {
			this.times.poll();
		}
		this.times.add(t);
		this.count++; //NOSONAR - false positive, we're synchronized
	}

	private Statistics calc() {
		List<Long> copy;
		long count;
		synchronized (this) {
			copy = new ArrayList<Long>(this.times);
			count = this.count;
		}
		ExponentialMovingAverage rates = new ExponentialMovingAverage(this.window);
		double t0 = 0;
		double sum = 0;
		double weight = 0;
		double min = this.min;
		double max = this.max;
		int size = copy.size();
		for (Long time : copy) {
			double t = time / this.factor;
			if (size == 1) {
				t0 = this.t0;
			}
			else if (t0 == 0) {
				t0 = t;
				continue;
			}
			double delta = t - t0;
			double value = delta > 0 ? delta / this.period : 0;
			if (value > max) {
				max = value;
			}
			if (value < min) {
				min = value;
			}
			double alpha = Math.exp(-delta * this.lapse);
			t0 = t;
			sum = alpha * sum + value;
			weight = alpha * weight + 1;
			rates.append(sum > 0 ? weight / sum : 0);
		}
		synchronized (this) {
			if (max > this.max) {
				this.max = max;
			}
			if (min < this.min) {
				this.min = min;
			}
		}
		return new Statistics(count, min < Double.MAX_VALUE ? min : 0, max, rates.getMean(),
				rates.getStandardDeviation());
	}

	/**
	 * @return the number of measurements recorded
	 */
	public int getCount() {
		return (int) this.count;
	}

	/**
	 * @return the number of measurements recorded
	 * @since 3.0
	 */
	public long getCountLong() {
		return this.count;
	}

	/**
	 * @return the time in milliseconds since the last measurement
	 */
	public double getTimeSinceLastMeasurement() {
		if (this.count == 0) {
			return 0;
		}
		double t0 = lastTime();
		return (System.nanoTime() / this.factor - t0);
	}

	/**
	 * @return the mean value
	 */
	public double getMean() {
		long count = this.count;
		count = count > this.retention ? this.retention : count;
		if (count == 0) {
			return 0;
		}
		double t0 = lastTime();
		double t = System.nanoTime() / this.factor;
		double value = t > t0 ? (t - t0) / this.period : 0;
		return count / (count / calc().getMean() + value);
	}

	private synchronized double lastTime() {
		if (this.times.size() > 0) {
			return this.times.peekLast() / this.factor;
		}
		else {
			 return this.t0;
		}
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
		double min = calc().getMin();
		return min > 0 ? 1 / min : 0;
	}

	/**
	 * @return the minimum value recorded (not weighted)
	 */
	public double getMin() {
		double max = calc().getMax();
		return max > 0 ? 1 / max : 0;
	}

	/**
	 * @return summary statistics (count, mean, standard deviation etc.)
	 */
	public Statistics getStatistics() {
		return calc();
	}

	@Override
	public String toString() {
		return String.format("[%s, timeSinceLast=%f]", getStatistics(), getTimeSinceLastMeasurement());
	}

}
