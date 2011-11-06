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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.gemfire.CacheFactoryBean;
import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;

import com.gemstone.gemfire.cache.Cache;

import static org.junit.Assert.assertEquals;

/**
 * @author Mark Fisher
 * @since 2.1
 */
public class GemfireMessageStoreTests {

	private Cache cache;
	
	@Test
	public void addAndGetMessage() throws Exception {
		GemfireMessageStore store = new GemfireMessageStore(this.cache);
		store.afterPropertiesSet();
		
		Message<?> message = MessageBuilder.withPayload("test").build();
		store.addMessage(message);
		Message<?> retrieved = store.getMessage(message.getHeaders().getId());
		assertEquals(message, retrieved);
	}

	@Before
	public void init() throws Exception{
		CacheFactoryBean cacheFactoryBean = new CacheFactoryBean();
		cacheFactoryBean.afterPropertiesSet();
		this.cache = (Cache)cacheFactoryBean.getObject();
	}
	
	@After
	public void cleanup(){
		this.cache.close();
		Assert.isTrue(this.cache.isClosed(), "Cache did not closed after close() call");
	}
}
