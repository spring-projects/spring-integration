/*
 * Copyright 2009-2010 the original author or authors.
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
 * Cumulative statistics for rate with higher weight given to recent data but without storing any history. Older values
 * are given exponentially smaller weight, with a decay factor determined by a duration chosen by the client.
 * 
 * @author Dave Syer
 * 
 */
public class ExponentialMovingAverageRateCumulativeHistory {

	private final ExponentialMovingAverageCumulativeHistory rates;

	private double weight;

	private double sum;

	private double min;

	private double max;

	private volatile long t0 = System.currentTimeMillis();

	private final double lapse;

	private final double period;

	/**
	 * @param period the period to base the rate measurement (in seconds)
	 * @param lapsePeriod the exponential lapse rate for the rate average (in seconds)
	 * @param window the exponential lapse window (number of measurements)
	 */
	public ExponentialMovingAverageRateCumulativeHistory(double period, double lapsePeriod, int window) {
		rates = new ExponentialMovingAverageCumulativeHistory(10);
		this.lapse = lapsePeriod > 0 ? 0.001 / lapsePeriod : 0; // convert to millisecs
		this.period = period * 1000; // convert to millisecs
	}

	public void increment() {

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

	public int getCount() {
		return rates.getCount();
	}

	/**
	 * @return the time in seconds since the last measurement
	 */
	public double getTimeSinceLastMeasurement() {
		return (System.currentTimeMillis() - t0) / 1000.;
	}

	public double getMean() {
		int count = rates.getCount();
		if (count==0) {
			return 0;
		}
		long t = System.currentTimeMillis();
		double value = t > t0 ? (t - t0) / period : 0;
		return count / (count / rates.getMean() + value);
	}

	public double getStandardDeviation() {
		return rates.getStandardDeviation();
	}

	public double getMax() {
		return min > 0 ? 1 / min : 0;
	}

	public double getMin() {
		return max > 0 ? 1 / max : 0;
	}

	@Override
	public String toString() {
		return String.format("[N=%d, min=%f, max=%f, mean=%f, sigma=%f, timeSinceLast=%f]", getCount(), getMin(),
				getMax(), getMean(), getStandardDeviation(), getTimeSinceLastMeasurement());
	}

}
