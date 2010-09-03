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
 * Cumulative statistics for a series of real numbers with higher weight given to recent data but without storing any
 * history. Older values are given exponentially smaller weight, with a decay factor determined by a "window" size
 * chosen by the client.
 * 
 * @author Dave Syer
 * 
 */
public class ExponentialMovingAverageCumulativeHistory {

	private int count;

	private double weight;

	private double sum;

	private double sumSquares;

	private double min;

	private double max;

	private final double decay;

	/**
	 * @param window the exponential lapse window (number of measurements)
	 */
	public ExponentialMovingAverageCumulativeHistory(int window) {
		this.decay = 1 - 1. / window;
	}

	public void append(double value) {
		if (value > max || count == 0)
			max = value;
		if (value < min || count == 0)
			min = value;
		sum = decay * sum + value;
		sumSquares = decay * sumSquares + value * value;
		weight = decay * weight + 1;
		count++;
	}

	public int getCount() {
		return count;
	}

	public double getMean() {
		return weight > 0 ? sum / weight : 0.;
	}

	public double getStandardDeviation() {
		double mean = getMean();
		double var = weight > 0 ? sumSquares / weight - mean * mean : 0.;
		return var > 0 ? Math.sqrt(var) : 0;
	}

	public double getMax() {
		return max;
	}

	public double getMin() {
		return min;
	}

	@Override
	public String toString() {
		return String.format("[N=%d, min=%f, max=%f, mean=%f, sigma=%f]", count, min, max, getMean(),
				getStandardDeviation());
	}

}
