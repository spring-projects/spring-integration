/*
 * Copyright 2002-2010 the original author or authors.
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

import static org.junit.Assert.assertEquals;

import java.util.UUID;

import org.junit.Test;

import org.springframework.data.gemfire.CacheFactoryBean;
import org.springframework.data.gemfire.RegionFactoryBean;
import org.springframework.integration.Message;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.support.MessageBuilder;

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.Region;

/**
 * @author Mark Fisher
 * @since 2.1
 */
public class GemfireMessageStoreTests {

	@Test
	public void addAndGetMessage() throws Exception {
		CacheFactoryBean cacheFactoryBean = new CacheFactoryBean();
		cacheFactoryBean.afterPropertiesSet();
		Cache cache = cacheFactoryBean.getObject();
		RegionFactoryBean<UUID, Message<?>> regionFactoryBean = new RegionFactoryBean<UUID, Message<?>>();
		regionFactoryBean.setName("test.addAndGetMessage");
		regionFactoryBean.setCache(cache);
		regionFactoryBean.afterPropertiesSet();
		Region<UUID, Message<?>> region = regionFactoryBean.getObject();
		MessageStore store = new GemfireMessageStore(region);
		Message<?> message = MessageBuilder.withPayload("test").build();
		store.addMessage(message);
		Message<?> retrieved = store.getMessage(message.getHeaders().getId());
		assertEquals(message, retrieved);
	}

}
