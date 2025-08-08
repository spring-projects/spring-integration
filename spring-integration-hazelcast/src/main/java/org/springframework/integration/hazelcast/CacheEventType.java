/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.hazelcast;

/**
 * Enumeration of Cache Event Types.
 *
 * @author Eren Avsarogullari
 * @author Artem Bilan
 *
 * @since 6.0
 *
 * @see org.springframework.integration.hazelcast.inbound.AbstractHazelcastMessageProducer
 */
public enum CacheEventType {

	/**
	 * The Hazelcast ADDED event.
	 */
	ADDED,

	/**
	 * The Hazelcast REMOVED event.
	 */
	REMOVED,

	/**
	 * The Hazelcast UPDATED event.
	 */
	UPDATED,

	/**
	 * The Hazelcast EVICTED event.
	 */
	EVICTED,

	/**
	 * The Hazelcast EXPIRED event.
	 */
	EXPIRED,

	/**
	 * The Hazelcast EVICT_ALL event.
	 */
	EVICT_ALL,

	/**
	 * The Hazelcast CLEAR_ALL event.
	 */
	CLEAR_ALL

}
