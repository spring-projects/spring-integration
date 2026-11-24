/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.support.management;

import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.support.MetricType;

/**
 * Metrics for subscribable channels.
 *
 * @author Gary Russell
 * @since 4.3.6
 *
 */
public interface SubscribableChannelManagement {

	/**
	 * The number of message handlers currently subscribed to this channel.
	 * @return the number of subscribers.
	 */
	@ManagedMetric(metricType = MetricType.COUNTER, displayName = "MessageChannel Subscriber Count")
	int getSubscriberCount();

}
