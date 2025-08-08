/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.hazelcast;

/**
 * Enumeration of Cache Listening Policy Type.
 *
 * @author Eren Avsarogullari
 * @author Artem Bilan
 *
 * @since 6.0
 *
 * @see org.springframework.integration.hazelcast.inbound.AbstractHazelcastMessageProducer
 */
public enum CacheListeningPolicyType {

	/**
	 * Only the local Hazelcast node can accept event.
	 */
	SINGLE,

	/**
	 * All subscribed members can accept event.
	 */
	ALL

}
