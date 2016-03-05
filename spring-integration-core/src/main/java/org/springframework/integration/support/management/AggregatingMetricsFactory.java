/*
 * Copyright 2015-2016 the original author or authors.
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




/**
 * Implementation that returns aggregating metrics.
 *
 * @author Gary Russell
 * @since 4.2
 *
 */
public class AggregatingMetricsFactory implements MetricsFactory {

	private final int sampleSize;

	/**
	 * @param sampleSize the number of messages over which to aggregate the elapsed time.
	 */
	public AggregatingMetricsFactory(int sampleSize) {
		this.sampleSize = sampleSize;
	}

	@Override
	public AbstractMessageChannelMetrics createChannelMetrics(String name) {
		return new AggregatingMessageChannelMetrics(name, this.sampleSize);
	}

	@Override
	public AbstractMessageHandlerMetrics createHandlerMetrics(String name) {
		return new AggregatingMessageHandlerMetrics(name, this.sampleSize);
	}

}
