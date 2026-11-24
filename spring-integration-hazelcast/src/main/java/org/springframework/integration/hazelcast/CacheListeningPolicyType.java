/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
