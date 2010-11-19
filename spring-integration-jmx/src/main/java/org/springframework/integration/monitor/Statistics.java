/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.monitor;

/**
 * @author Dave Syer
 * @since 2.0
 */
public class Statistics {

	private final int count;

	private final double min;

	private final double max;

	private final double mean;

	private final double standardDeviation;


	public Statistics(int count, double min, double max, double mean, double standardDeviation) {
		this.count = count;
		this.min = min;
		this.max = max;
		this.mean = mean;
		this.standardDeviation = standardDeviation;
	}


	public int getCount() {
		return count;
	}

	public double getMin() {
		return min;
	}

	public double getMax() {
		return max;
	}

	public double getMean() {
		return mean;
	}

	public double getStandardDeviation() {
		return standardDeviation;
	}

	@Override
	public String toString() {
		return String.format("[N=%d, min=%f, max=%f, mean=%f, sigma=%f]",
				count, min, max, getMean(), getStandardDeviation());
	}

}
