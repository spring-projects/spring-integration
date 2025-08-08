/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.hazelcast;

/**
 * Enumeration of Hazelcast Cluster Monitor Types.
 *
 * @author Eren Avsarogullari
 * @author Artem Bilan
 *
 * @since 6.0
 *
 * @see org.springframework.integration.hazelcast.inbound.HazelcastClusterMonitorMessageProducer
 * @see com.hazelcast.core.DistributedObjectListener
 * @see com.hazelcast.core.LifecycleListener
 */
public enum ClusterMonitorType {

	/**
	 * The membership listener mode.
	 */
	MEMBERSHIP,

	/**
	 * The distributed object listener mode.
	 */
	DISTRIBUTED_OBJECT,

	/**
	 * The migration listener mode.
	 */
	MIGRATION,

	/**
	 * The listener listener mode.
	 */
	LIFECYCLE,

	/**
	 * The client listener mode.
	 */
	CLIENT

}
