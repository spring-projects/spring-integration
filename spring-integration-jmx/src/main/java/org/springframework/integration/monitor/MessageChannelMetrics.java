/*
 * Copyright 2002-2014 the original author or authors.
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

import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.support.MetricType;

/**
 * Interface for all message channel monitors containing accessors for various useful metrics that are generic for all
 * channel types.
 *
 * @author Dave Syer
 * @since 2.0
 */
public interface MessageChannelMetrics {

	@ManagedOperation
	void reset();

	/**
	 * @return the number of successful sends
	 */
	@ManagedMetric(metricType = MetricType.COUNTER, displayName = "MessageChannel Send Count")
	int getSendCount();

	/**
	 * @return the number of successful sends
	 * @since 3.0
	 */
	@ManagedMetric(metricType = MetricType.COUNTER, displayName = "MessageChannel Send Count")
	long getSendCountLong();

	/**
	 * @return the number of failed sends (either throwing an exception or rejected by the channel)
	 */
	@ManagedMetric(metricType = MetricType.COUNTER, displayName = "MessageChannel Send Error Count")
	int getSendErrorCount();

	/**
	 * @return the number of failed sends (either throwing an exception or rejected by the channel)
	 * @since 3.0
	 */
	@ManagedMetric(metricType = MetricType.COUNTER, displayName = "MessageChannel Send Error Count")
	long getSendErrorCountLong();

	/**
	 * @return the time in seconds since the last send
	 */
	@ManagedMetric(metricType = MetricType.GAUGE, displayName = "Channel Time Since Last Send in Seconds")
	double getTimeSinceLastSend();

	/**
	 * @return the mean send rate (per second)
	 */
	@ManagedMetric(metricType = MetricType.GAUGE, displayName = "Channel Send Rate per Second")
	double getMeanSendRate();

	/**
	 * @return the mean error rate (per second).  Errors comprise all failed sends.
	 */
	@ManagedMetric(metricType = MetricType.GAUGE, displayName = "Channel Error Rate per Second")
	double getMeanErrorRate();

	/**
	 * @return the mean ratio of failed to successful sends in approximately the last minute
	 */
	@ManagedMetric(metricType = MetricType.GAUGE, displayName = "Mean Channel Error Ratio per Minute")
	double getMeanErrorRatio();

	/**
	 * @return the mean send duration (milliseconds)
	 */
	@ManagedMetric(metricType = MetricType.GAUGE, displayName = "Channel Send Mean Duration in Milliseconds")
	double getMeanSendDuration();

	/**
	 * @return the minimum send duration (milliseconds) since startup
	 */
	@ManagedMetric(metricType = MetricType.GAUGE, displayName = "Channel Send Min Duration in Milliseconds")
	double getMinSendDuration();

	/**
	 * @return the maximum send duration (milliseconds) since startup
	 */
	@ManagedMetric(metricType = MetricType.GAUGE, displayName = "Channel Send Max Duration in Milliseconds")
	double getMaxSendDuration();

	/**
	 * @return the standard deviation send duration (milliseconds)
	 */
	@ManagedMetric(metricType = MetricType.GAUGE, displayName = "Channel Send Standard Deviation Duration in Milliseconds")
	double getStandardDeviationSendDuration();

	/**
	 * @return summary statistics about the send duration (milliseconds)
	 */
	Statistics getSendDuration();

	/**
	 * @return summary statistics about the send rates (per second)
	 */
	Statistics getSendRate();

	/**
	 * @return summary statistics about the error rates (per second)
	 */
	Statistics getErrorRate();

}
