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
