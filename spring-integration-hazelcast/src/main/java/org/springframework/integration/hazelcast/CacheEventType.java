/*
 * Copyright 2015-2022 the original author or authors.
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
