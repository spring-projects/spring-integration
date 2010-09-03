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
 * Cumulative statistics for success rate (ratio) with higher weight given to recent data but without storing any
 * history. Older values are given exponentially smaller weight, with a decay factor determined by a duration chosen by
 * the client.
 * 
 * @author Dave Syer
 * 
 */
public class ExponentialMovingAverageRatioCumulativeHistory {

	private double weight;

	private double sum;

	private volatile long t0 = System.currentTimeMillis();

	private final double lapse;

	private final ExponentialMovingAverageCumulativeHistory cumulative;

	/**
	 * @param lapsePeriod the exponential lapse rate for the rate average (in seconds)
	 * @param window the exponential lapse window (number of measurements)
	 */
	public ExponentialMovingAverageRatioCumulativeHistory(double lapsePeriod, int window) {
		this.cumulative = new ExponentialMovingAverageCumulativeHistory(window);
		this.lapse = lapsePeriod > 0 ? 0.001 / lapsePeriod : 0; // convert to millisecs
	}

	public void success() {
		append(1);
	}

	public void failure() {
		append(0);
	}

	private void append(int value) {

		long t = System.currentTimeMillis();
		double alpha = Math.exp((t0 - t) * lapse);
		t0 = t;
		sum = alpha * sum + value;
		weight = alpha * weight + 1;
		cumulative.append(sum / weight);

	}

	public int getCount() {
		return cumulative.getCount();
	}

	/**
	 * @return the time in seconds since the last measurement
	 */
	public double getTimeSinceLastMeasurement() {
		return (System.currentTimeMillis() - t0) / 1000.;
	}

	public double getMean() {
		int count = cumulative.getCount();
		if (count == 0) {
			return 0;
		}
		long t = System.currentTimeMillis();
		double alpha = Math.exp((t0 - t) * lapse);
		return alpha * cumulative.getMean() + 1 - alpha;
	}

	public double getStandardDeviation() {
		return cumulative.getStandardDeviation();
	}

	public double getMax() {
		return cumulative.getMax();
	}

	public double getMin() {
		return cumulative.getMin();
	}

	@Override
	public String toString() {
		return String.format("[N=%d, min=%f, max=%f, mean=%f, sigma=%f, timeSinceLast=%f]", getCount(), getMin(),
				getMax(), getMean(), getStandardDeviation(), getTimeSinceLastMeasurement());
	}

}
