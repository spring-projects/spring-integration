/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.hazelcast;

/**
 * Enumeration of Distributed SQL Iteration Type.
 *
 * @author Eren Avsarogullari
 * @author Artem Bilan
 *
 * @since 6.0
 *
 * @see org.springframework.integration.hazelcast.inbound.HazelcastDistributedSQLMessageSource
 * @see com.hazelcast.map.IMap
 */
public enum DistributedSQLIterationType {

	/**
	 * The {@link com.hazelcast.map.IMap#entrySet()} to iterate.
	 */
	ENTRY,

	/**
	 * The {@link com.hazelcast.map.IMap#keySet()} to iterate.
	 */
	KEY,

	/**
	 * The {@link com.hazelcast.map.IMap#localKeySet()} to iterate.
	 */
	LOCAL_KEY,

	/**
	 * The {@link com.hazelcast.map.IMap#values()} to iterate.
	 */
	VALUE

}
