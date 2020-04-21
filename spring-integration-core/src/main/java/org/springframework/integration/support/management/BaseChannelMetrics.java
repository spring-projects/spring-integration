/*
 * Copyright 2019-2020 the original author or authors.
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

import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.support.MetricType;

/**
 * Primary interface for channels that provide metrics.
 *
 * @author Gary Russell
 * @since 5.2
 *
 * @deprecated in favor of Micrometer metrics.
 */
@Deprecated
public interface BaseChannelMetrics extends IntegrationStatsManagement {

	/**
	 * @return the number of successful sends
	 */
	@ManagedMetric(metricType = MetricType.COUNTER, displayName = "Channel Send Count")
	long sendCount();

	/**
	 * @return the number of failed sends (either throwing an exception or rejected by the channel)
	 */
	@ManagedMetric(metricType = MetricType.COUNTER, displayName = "Channel Send Error Count")
	long sendErrorCount();

}
