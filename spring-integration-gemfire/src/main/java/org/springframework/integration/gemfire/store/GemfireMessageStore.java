/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.gemfire.store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.geode.cache.Region;

import org.springframework.integration.store.AbstractKeyValueMessageStore;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.MessageStore;
import org.springframework.util.Assert;
import org.springframework.util.PatternMatchUtils;

/**
 * Gemfire implementation of the key/value style {@link MessageStore} and
 * {@link MessageGroupStore}
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author David Turanski
 * @author Artem Bilan
 *
 * @since 2.1
 */
public class GemfireMessageStore extends AbstractKeyValueMessageStore {

	private final Region<Object, Object> messageStoreRegion;

	/**
	 * Provides the region to be used for the message store. This is useful when
	 * using a configured region. This is also required if using a client region
	 * on a remote cache server.
	 * @param messageStoreRegion The region.
	 */
	public GemfireMessageStore(Region<Object, Object> messageStoreRegion) {
		this(messageStoreRegion, "");
	}

	/**
	 * Construct a {@link GemfireMessageStore} instance based on the provided
	 * @param messageStoreRegion the region to use.
	 * @param prefix the key prefix to use, allowing the same region to be used for
	 * multiple stores.
	 * @since 4.3.12
	 */
	public GemfireMessageStore(Region<Object, Object> messageStoreRegion, String prefix) {
		super(prefix);
		Assert.notNull(messageStoreRegion, "'messageStoreRegion' must not be null");
		this.messageStoreRegion = messageStoreRegion;
	}

	@Override
	protected Object doRetrieve(Object id) {
		Assert.notNull(id, "'id' must not be null");
		return this.messageStoreRegion.get(id);
	}

	@Override
	protected void doStore(Object id, Object objectToStore) {
		Assert.notNull(id, "'id' must not be null");
		Assert.notNull(objectToStore, "'objectToStore' must not be null");
		this.messageStoreRegion.put(id, objectToStore);
	}

	@Override
	protected void doStoreIfAbsent(Object id, Object objectToStore) {
		Assert.notNull(id, "'id' must not be null");
		Assert.notNull(objectToStore, "'objectToStore' must not be null");
		Object present = this.messageStoreRegion.putIfAbsent(id, objectToStore);
		if (present != null && logger.isDebugEnabled()) {
			logger.debug("The message: [" + present + "] is already present in the store. " +
					"The [" + objectToStore + "] is ignored.");
		}
	}

	@Override
	protected Object doRemove(Object id) {
		Assert.notNull(id, "'id' must not be null");
		return this.messageStoreRegion.remove(id);
	}

	@Override
	protected Collection<?> doListKeys(String keyPattern) {
		Assert.hasText(keyPattern, "'keyPattern' must not be empty");
		Collection<Object> keys = this.messageStoreRegion.keySet();
		List<Object> keyList = new ArrayList<Object>();
		for (Object key : keys) {
			String keyValue = key.toString();
			if (PatternMatchUtils.simpleMatch(keyPattern, keyValue)) {
				keyList.add(keyValue);
			}
		}
		return keyList;
	}

}
