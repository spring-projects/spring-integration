/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.support.management;

/**
 * Statistics.
 * @deprecated in favor of dimensional metrics via
 * {@link org.springframework.integration.support.management.metrics.MeterFacade}.
 * Built-in metrics will be removed in a future release.
 *
 * @author Dave Syer
 * @author Gary Russell
 * @since 2.0
 */
@Deprecated
public class Statistics {

	private final long count;

	private final double min;

	private final double max;

	private double mean;

	private final double standardDeviation;


	public Statistics(long count, double min, double max, double mean, double standardDeviation) {
		this.count = count;
		this.min = min;
		this.max = max;
		this.mean = mean;
		this.standardDeviation = standardDeviation;
	}


	public int getCount() {
		return (int) this.count;
	}

	public long getCountLong() {
		return this.count;
	}

	public double getMin() {
		return this.min;
	}

	public double getMax() {
		return this.max;
	}

	public double getMean() {
		return this.mean;
	}

	public void setMean(double mean) {
		this.mean = mean;
	}

	public double getStandardDeviation() {
		return this.standardDeviation;
	}

	@Override
	public String toString() {
		return String.format("[N=%d, min=%f, max=%f, mean=%f, sigma=%f]",
				this.count, this.min, this.max, getMean(), getStandardDeviation());
	}

}
