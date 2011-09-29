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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.gemfire.RegionFactoryBean;
import org.springframework.integration.Message;
import org.springframework.integration.store.AbstractKeyValueMessageStore;
import org.springframework.integration.store.MessageGroupMetadata;
import org.springframework.util.Assert;

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.Region;

/**
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
			messageRegionFactoryBean.setBeanName("messageRegionFactoryBean");
			messageRegionFactoryBean.setCache(cache);
			messageRegionFactoryBean.afterPropertiesSet();
			this.messageStoreRegion = messageRegionFactoryBean.getObject();
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to initialize Gemfire Regions");
		}
	}
	
	protected void storeHolderMap(String key, Object value){
		messageStoreRegion.put(key, value);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	protected Map<UUID, Message<?>> getHolderMapForMessage(){
		if (messageStoreRegion.containsKey(MESSAGES_HOLDER_MAP_NAME)){
			return (Map<UUID, Message<?>>) messageStoreRegion.get(MESSAGES_HOLDER_MAP_NAME);
		}
		else {
			Map<UUID, Message<?>> messageHolderMap = new HashMap<UUID, Message<?>>();
			messageStoreRegion.put(MESSAGES_HOLDER_MAP_NAME, messageHolderMap);
			return messageHolderMap;
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	protected Map<Object, MessageGroupMetadata> getHolderMapForMessageGroups(){
		if (messageStoreRegion.containsKey(MESSAGE_GROUPS_HOLDER_MAP_NAME)){
			return (Map<Object, MessageGroupMetadata>) messageStoreRegion.get(MESSAGE_GROUPS_HOLDER_MAP_NAME);
		}
		else {
			Map<Object, MessageGroupMetadata> messageHolderMap = new HashMap<Object, MessageGroupMetadata>();
			messageStoreRegion.put(MESSAGE_GROUPS_HOLDER_MAP_NAME, messageHolderMap);
			return messageHolderMap;
		}
	}
}
