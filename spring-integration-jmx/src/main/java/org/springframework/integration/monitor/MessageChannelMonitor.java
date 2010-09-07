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

import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.support.MetricType;

/**
 * @author dsyer
 *
 */
public interface MessageChannelMonitor {

	@ManagedMetric(metricType = MetricType.COUNTER, displayName = "MessageChannel Sends")
	int getSendCount();

	@ManagedMetric(metricType = MetricType.COUNTER, displayName = "MessageChannel Send Errors")
	int getSendErrorCount();

	@ManagedMetric(metricType = MetricType.GAUGE, displayName = "Channel Time Since Last Send in Seconds")
	double getTimeSinceLastSend();

	@ManagedMetric(metricType = MetricType.GAUGE, displayName = "Channel Send Rate per Second")
	double getMeanSendRate();

	@ManagedMetric(metricType = MetricType.GAUGE, displayName = "Channel Error Rate per Second")
	double getMeanErrorRate();

	@ManagedMetric(metricType = MetricType.GAUGE, displayName = "Mean Channel Error Ratio per Minute")
	double getMeanErrorRatio();

	@ManagedMetric(metricType = MetricType.GAUGE, displayName = "Channel Send Mean Duration")
	double getMeanSendDuration();

	@ManagedMetric(metricType = MetricType.GAUGE, displayName = "Channel Send Min Duration")
	double getMinSendDuration();

	@ManagedMetric(metricType = MetricType.GAUGE, displayName = "Channel Send Max Duration")
	double getMaxSendDuration();

	@ManagedMetric(metricType = MetricType.GAUGE, displayName = "Channel Send Standard Deviation Duration")
	double getStandardDeviationSendDuration();

	Statistics getSendDuration();

	Statistics getSendRate();

	Statistics getErrorRate();

}