/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
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
