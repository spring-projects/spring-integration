/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
