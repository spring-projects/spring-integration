/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
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
