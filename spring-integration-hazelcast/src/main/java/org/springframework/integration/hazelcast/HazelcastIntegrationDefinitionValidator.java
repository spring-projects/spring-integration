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

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.hazelcast.collection.IList;
import com.hazelcast.collection.IQueue;
import com.hazelcast.collection.ISet;
import com.hazelcast.core.DistributedObject;
import com.hazelcast.map.IMap;
import com.hazelcast.multimap.MultiMap;
import com.hazelcast.replicatedmap.ReplicatedMap;
import com.hazelcast.topic.ITopic;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Common Validator for Hazelcast Integration. It validates cache types and events.
 *
 * @author Eren Avsarogullari
 * @author Artem Bilan
 *
 * @since 6.0
 */
public final class HazelcastIntegrationDefinitionValidator {

	public static <E extends Enum<E>> Set<String> validateEnumType(final Class<E> enumType, final String types) {
		Set<String> typeSet = StringUtils.commaDelimitedListToSet(StringUtils.trimAllWhitespace(types));
		for (String type : typeSet) {
			Enum.valueOf(enumType, type);
		}

		return typeSet;
	}

	public static void validateCacheTypeForEventDrivenMessageProducer(final DistributedObject distributedObject) {
		if (!(distributedObject instanceof IMap
				|| distributedObject instanceof MultiMap
				|| distributedObject instanceof ReplicatedMap
				|| distributedObject instanceof IList
				|| distributedObject instanceof ISet
				|| distributedObject instanceof IQueue
				|| distributedObject instanceof ITopic)) {
			throw new IllegalArgumentException(
					"Invalid 'cache' type is set. IMap, MultiMap, ReplicatedMap, IList, ISet, IQueue and ITopic" +
							" cache object types are acceptable for Hazelcast Inbound Channel Adapter.");
		}
	}

	public static void validateCacheEventsByDistributedObject(
			final DistributedObject distributedObject, final Set<String> cacheEventTypeSet) {
		List<String> supportedCacheEventTypes = getSupportedCacheEventTypes(distributedObject);
		if (!CollectionUtils.isEmpty(supportedCacheEventTypes)) {
			validateCacheEventsByDistributedObject(distributedObject, cacheEventTypeSet, supportedCacheEventTypes);
		}
	}

	private static List<String> getSupportedCacheEventTypes(final DistributedObject distributedObject) {
		if ((distributedObject instanceof IList)
				|| (distributedObject instanceof ISet)
				|| (distributedObject instanceof IQueue)) {
			return Arrays.asList(CacheEventType.ADDED.toString(), CacheEventType.REMOVED.toString());
		}
		else if (distributedObject instanceof MultiMap) {
			return Arrays.asList(CacheEventType.ADDED.toString(),
					CacheEventType.REMOVED.toString(),
					CacheEventType.CLEAR_ALL.toString());
		}
		else if (distributedObject instanceof ReplicatedMap) {
			return Arrays.asList(CacheEventType.ADDED.toString(),
					CacheEventType.REMOVED.toString(),
					CacheEventType.UPDATED.toString(),
					CacheEventType.EVICTED.toString());
		}

		return null;
	}

	private static void validateCacheEventsByDistributedObject(DistributedObject distributedObject,
			Set<String> cacheEventTypeSet, List<String> supportedCacheEventTypes) {
		if (!supportedCacheEventTypes.containsAll(cacheEventTypeSet)) {
			throw new IllegalArgumentException("'cache-events' attribute of "
					+ distributedObject.getName() + " can be set as " + supportedCacheEventTypes);
		}
	}

	private HazelcastIntegrationDefinitionValidator() {
	}

}
