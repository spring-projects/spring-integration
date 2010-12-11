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

import org.springframework.integration.channel.QueueChannel;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.support.MetricType;

/**
 * @author Dave Syer
 * @since 2.0
 */
public class QueueChannelMetrics extends PollableChannelMetrics {

	private final QueueChannel channel;


	public QueueChannelMetrics(QueueChannel channel, String name) {
		super(channel, name);
		this.channel = channel;
	}

	@ManagedMetric(metricType = MetricType.GAUGE, displayName = "QueueChannel Queue Size")
	public int getQueueSize() {
		return channel.getQueueSize();
	}

	@ManagedMetric(metricType = MetricType.GAUGE, displayName = "QueueChannel Remaining Capacity")
	public int getRemainingCapacity() {
		return channel.getRemainingCapacity();
	}

}
