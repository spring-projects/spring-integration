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

import java.util.HashSet;
import java.util.Set;

import com.hazelcast.collection.IList;
import com.hazelcast.core.DistributedObject;
import com.hazelcast.instance.impl.HazelcastInstanceFactory;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hazelcast Integration Definition Validator Test Class
 *
 * @author Eren Avsarogullari
 * @author Artem Bilan
 *
 * @since 6.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@DirtiesContext
@SuppressWarnings({"rawtypes"})
public class HazelcastIntegrationDefinitionValidatorTests {

	@Autowired
	private IList distList;

	@AfterClass
	public static void shutdown() {
		HazelcastInstanceFactory.terminateAll();
	}

	@Test
	public void testValidateEnumType() {
		final String cacheEventTypes =
				" ADDED, REMOVED, UPDATED, EVICTED, EVICT_ALL, CLEAR_ALL ";
		final Set<String> typeSet = HazelcastIntegrationDefinitionValidator
				.validateEnumType(CacheEventType.class, cacheEventTypes);
		assertThat(typeSet.size()).isEqualTo(6);
		for (String type : typeSet) {
			Enum.valueOf(CacheEventType.class, type);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateEnumTypeWithInvalidValue() {
		final String cacheEventTypes = "Invalid_Enum_Type";
		HazelcastIntegrationDefinitionValidator
				.validateEnumType(CacheEventType.class, cacheEventTypes);
	}

	@Test
	public void testValidateCacheEventsByDistributedObject() {
		Set<String> cacheEventTypeSet = new HashSet<>(2);
		cacheEventTypeSet.add(CacheEventType.ADDED.toString());
		cacheEventTypeSet.add(CacheEventType.REMOVED.toString());
		HazelcastIntegrationDefinitionValidator
				.validateCacheEventsByDistributedObject(this.distList, cacheEventTypeSet);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateCacheEventsByDistributedObjectWithInvalidValue() {
		Set<String> cacheEventTypeSet = new HashSet<>(1);
		cacheEventTypeSet.add("Invalid_Cache_Event_Type");
		HazelcastIntegrationDefinitionValidator
				.validateCacheEventsByDistributedObject(this.distList, cacheEventTypeSet);
	}

	@Test
	public void testValidateCacheTypeForEventDrivenMessageProducer() {
		HazelcastIntegrationDefinitionValidator
				.validateCacheTypeForEventDrivenMessageProducer(this.distList);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateCacheTypeForEventDrivenMessageProducerWithUnexpectedDistObject() {
		HazelcastIntegrationDefinitionValidator
				.validateCacheTypeForEventDrivenMessageProducer(new DistributedObject() {

					@Override
					public String getPartitionKey() {
						return null;
					}

					@Override
					public String getName() {
						return null;
					}

					@Override
					public String getServiceName() {
						return null;
					}

					@Override
					public void destroy() {

					}

				});
	}

}
