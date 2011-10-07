/*
 * Copyright 2002-2011 the original author or authors.
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

import org.springframework.data.gemfire.RegionFactoryBean;
import org.springframework.integration.store.AbstractKeyValueMessageStore;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.util.Assert;
import org.springframework.util.PatternMatchUtils;

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.Region;

/**
 * Gemfire implementation of the key/value style {@link MessageStore} and {@link MessageGroupStore}
 * 
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 2.1
 */
public class GemfireMessageStore extends AbstractKeyValueMessageStore{

	private final Region<Object, Object> messageStoreRegion;
	
	public GemfireMessageStore(Cache cache) {
		Assert.notNull(cache, "'cache' must not be null");
		try {
			RegionFactoryBean<Object, Object> messageRegionFactoryBean = new RegionFactoryBean<Object, Object>();
			messageRegionFactoryBean.setBeanName("messageStoreRegion");
			messageRegionFactoryBean.setCache(cache);
			messageRegionFactoryBean.afterPropertiesSet();
			this.messageStoreRegion = messageRegionFactoryBean.getObject();
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to initialize Gemfire Region");
		}
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
			if (PatternMatchUtils.simpleMatch(keyPattern, keyValue)){
				keyList.add(keyValue);
			}
		}
		return keyList;
	}
}
