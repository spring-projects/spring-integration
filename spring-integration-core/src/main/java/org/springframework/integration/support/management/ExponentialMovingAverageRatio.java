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
import java.util.Iterator;
import java.util.List;



/**
 * Cumulative statistics for success ratio with higher weight given to recent data.
 * Clients call {@link #success()} or {@link #failure()} when an event occurs, and the ratio of success to total events
 * is accumulated. Older values are given exponentially smaller weight, with a decay factor determined by a duration
 * chosen by the client. The rate measurement weights decay in two dimensions:
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
 * @since 2.0
 */
public class ExponentialMovingAverageRatio {

	private volatile double t0;

	private volatile long count;

	private volatile double min = Double.MAX_VALUE;

	private volatile double max;

	private final double lapse;

	private final Deque<Long> times = new ArrayDeque<Long>();

	private final Deque<Integer> values = new ArrayDeque<Integer>();

	private final int retention;

	private final int window;

	private final double factor;

	/**
	 * @param lapsePeriod the exponential lapse rate for the rate average (in seconds)
	 * @param window the exponential lapse window (number of measurements)
	 */
	public ExponentialMovingAverageRatio(double lapsePeriod, int window) {
		this(lapsePeriod, window, false);
	}

	/**
	 * @param lapsePeriod the exponential lapse rate for the rate average (in seconds)
	 * @param window the exponential lapse window (number of measurements)
	 * @param millis when true, analyze the data as milliseconds instead of the native nanoseconds
	 * @since 4.2
	 */
	public ExponentialMovingAverageRatio(double lapsePeriod, int window, boolean millis) {
		this.lapse = lapsePeriod > 0 ? 0.001 / lapsePeriod : 0; // convert to milliseconds
		this.window = window;
		this.retention = window * 5;
		this.factor = millis ? 1000000 : 1;
		this.t0 = System.nanoTime() / this.factor;
	}


	/**
	 * Add a new event with successful outcome.
	 */
	public void success() {
		append(1, System.nanoTime());
	}

	/**
	 * Add a new event with successful outcome at time t.
	 * @param t the System.nanoTime().
	 */
	public void success(long t) {
		append(1, t);
	}

	/**
	 * Add a new event with failed outcome.
	 */
	public void failure() {
		append(0, System.nanoTime());
	}

	/**
	 * Add a new event with failed outcome at time t.
	 * @param t a new event with failed outcome in milliseconds.
	 */
	public void failure(long t) {
		append(0, t);
	}

	public synchronized void reset() {
		this.t0 = System.nanoTime() / this.factor;
		this.times.clear();
		this.values.clear();
		this.count = 0;
		this.max = 0;
		this.min = Double.MAX_VALUE;
	}

	private synchronized void append(int value, long t) {
		if (this.times.size() == this.retention) {
			this.times.poll();
			this.values.poll();
		}
		this.times.add(t);
		this.values.add(value);
		this.count++; //NOSONAR - false positive, we're synchronized
	}

	private Statistics calc() {
		List<Long> copyTimes;
		List<Integer> copyValues;
		long count;
		synchronized (this) {
			copyTimes = new ArrayList<Long>(this.times);
			copyValues = new ArrayList<Integer>(this.values);
			count = this.count;
		}
		ExponentialMovingAverage cumulative = new ExponentialMovingAverage(this.window);
		double t0 = 0;
		double sum = 0;
		double weight = 0;
		double min = this.min;
		double max = this.max;
		int size = copyTimes.size();
		Iterator<Integer> values = copyValues.iterator();
		for (Long time : copyTimes) {
			double t = time / this.factor;
			if (size == 1) {
				t0 = this.t0;
			}
			else if (t0 == 0) {
				t0 = t;
				values.next();
				continue;
			}
			double alpha = Math.exp((t0 - t) * this.lapse);
			t0 = t;
			sum = alpha * sum + values.next();
			weight = alpha * weight + 1;
			double value = sum / weight;
			if (value > max) {
				max = value;
			}
			if (value < min) {
				min = value;
			}
			cumulative.append(value);
		}
		synchronized (this) {
			if (max > this.max) {
				this.max = max;
			}
			if (min < this.min) {
				this.min = min;
			}
		}
		return new Statistics(count, min < Double.MAX_VALUE ? min : 0, max, cumulative.getMean(),
				cumulative.getStandardDeviation());
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
	 * @return the time in seconds since the last measurement
	 */
	public double getTimeSinceLastMeasurement() {
		double delta = System.nanoTime() - lastTime();
		return delta / 1000. / this.factor;
	}

	/**
	 * @return the mean success rate
	 */
	public double getMean() {
		if (this.count == 0) {
			// Optimistic to start: success rate is 100%
			return 1;
		}
		Statistics statistics = calc();
		double t = System.nanoTime() / this.factor;
		double mean = statistics.getMean();
		double alpha = Math.exp((lastTime() / this.factor - t) * this.lapse);
		return alpha * mean + 1 - alpha;
	}

	private synchronized double lastTime() {
		if (this.times.size() > 0) {
			return this.times.peekLast();
		}
		else {
			return this.t0 * this.factor;
		}
	}

	/**
	 * @return the approximate standard deviation of the success rate measurements
	 */
	public double getStandardDeviation() {
		return calc().getStandardDeviation();
	}

	/**
	 * @return the maximum value recorded of the exponential weighted average (per measurement) success rate
	 */
	public double getMax() {
		return calc().getMax();
	}

	/**
	 * @return the minimum value recorded of the exponential weighted average (per measurement) success rate
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
		return String.format("[%s, timeSinceLast=%f]", getStatistics(), getTimeSinceLastMeasurement());
	}

}
