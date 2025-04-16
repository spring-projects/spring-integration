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

package org.springframework.integration.hazelcast.metadata;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.map.listener.EntryAddedListener;
import com.hazelcast.map.listener.EntryRemovedListener;
import com.hazelcast.map.listener.EntryUpdatedListener;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.metadata.ListenableMetadataStore;
import org.springframework.integration.metadata.MetadataStoreListener;
import org.springframework.util.Assert;

/**
 * The Hazelcast {@link IMap}-based {@link ListenableMetadataStore} implementation.
 *
 * @author Vinicius Carvalho
 * @author Artem Bilan
 */
public class HazelcastMetadataStore implements ListenableMetadataStore, InitializingBean {

	private static final String METADATA_STORE_MAP_NAME = "SPRING_INTEGRATION_METADATA_STORE";

	private final IMap<String, String> map;

	private final List<MetadataStoreListener> listeners = new CopyOnWriteArrayList<>();

	public HazelcastMetadataStore(HazelcastInstance hazelcastInstance) {
		Assert.notNull(hazelcastInstance, "Hazelcast instance can't be null");
		this.map = hazelcastInstance.getMap(METADATA_STORE_MAP_NAME);
	}

	public HazelcastMetadataStore(IMap<String, String> map) {
		Assert.notNull(map, "IMap reference can not be null");
		this.map = map;
	}

	@Override
	public String putIfAbsent(String key, String value) {
		assertKey(key);
		Assert.notNull(value, "'value' must not be null.");
		return this.map.putIfAbsent(key, value);
	}

	@Override
	public boolean replace(String key, String oldValue, String newValue) {
		assertKey(key);
		Assert.notNull(oldValue, "'oldValue' must not be null.");
		Assert.notNull(newValue, "'newValue' must not be null.");
		return this.map.replace(key, oldValue, newValue);
	}

	@Override
	public void put(String key, String value) {
		assertKey(key);
		Assert.notNull(value, "'value' must not be null.");
		this.map.put(key, value);
	}

	@Override
	public String get(String key) {
		assertKey(key);
		return this.map.get(key);
	}

	@Override
	public String remove(String key) {
		assertKey(key);
		return this.map.remove(key);
	}

	private static void assertKey(String key) {
		Assert.notNull(key, "'key' must not be null.");
	}

	@Override
	public void addListener(MetadataStoreListener callback) {
		Assert.notNull(callback, "callback object can not be null");
		this.listeners.add(callback);
	}

	@Override
	public void removeListener(MetadataStoreListener callback) {
		this.listeners.remove(callback);
	}

	@Override
	public void afterPropertiesSet() {
		this.map.addEntryListener(new MapListener(this.listeners), true);
	}

	private record MapListener(List<MetadataStoreListener> listeners)
			implements EntryAddedListener<String, String>,
			EntryRemovedListener<String, String>,
			EntryUpdatedListener<String, String> {

		@Override
		public void entryAdded(EntryEvent<String, String> event) {
			for (MetadataStoreListener listener : this.listeners) {
				listener.onAdd(event.getKey(), event.getValue());
			}
		}

		@Override
		public void entryRemoved(EntryEvent<String, String> event) {
			for (MetadataStoreListener listener : this.listeners) {
				listener.onRemove(event.getKey(), event.getOldValue());
			}
		}

		@Override
		public void entryUpdated(EntryEvent<String, String> event) {
			for (MetadataStoreListener listener : this.listeners) {
				listener.onUpdate(event.getKey(), event.getValue());
			}
		}

	}

}
