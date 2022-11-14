/*
 * Copyright 2017-2022 the original author or authors.
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

package org.springframework.integration.hazelcast.store;

import java.util.Collection;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.query.Predicates;
import com.hazelcast.query.QueryConstants;

import org.springframework.integration.store.AbstractKeyValueMessageStore;
import org.springframework.util.Assert;

/**
 * The Hazelcast {@link IMap}-based {@link AbstractKeyValueMessageStore} implementation.
 *
 * @author Vinicius Carvalho
 * @author Artem Bilan
 */
public class HazelcastMessageStore extends AbstractKeyValueMessageStore {

	private static final String MESSAGE_STORE_MAP_NAME = "SPRING_INTEGRATION_MESSAGE_STORE";

	private final IMap<Object, Object> map;

	public HazelcastMessageStore(HazelcastInstance hazelcastInstance) {
		Assert.notNull(hazelcastInstance, "Hazelcast instance can't be null");
		this.map = hazelcastInstance.getMap(MESSAGE_STORE_MAP_NAME);
	}

	public HazelcastMessageStore(IMap<Object, Object> map) {
		Assert.notNull(map, "IMap reference can not be null");
		this.map = map;
	}

	@Override
	protected Object doRetrieve(Object id) {
		return this.map.get(id);
	}

	@Override
	protected void doStore(Object id, Object objectToStore) {
		this.map.put(id, objectToStore);
	}

	@Override
	protected void doStoreIfAbsent(Object id, Object objectToStore) {
		this.map.putIfAbsent(id, objectToStore);
	}

	@Override
	protected void doRemoveAll(Collection<Object> ids) {
		this.map.removeAll((mapEntry) -> ids.contains(mapEntry.getKey()));
	}

	@Override
	protected Object doRemove(Object id) {
		return this.map.remove(id);
	}

	@Override
	protected Collection<?> doListKeys(String keyPattern) {
		Assert.hasText(keyPattern, "'keyPattern' must not be empty");
		return this.map.keySet(Predicates.like(QueryConstants.KEY_ATTRIBUTE_NAME.value(),
				keyPattern.replaceAll("\\*", "%")));
	}

}
