/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.gemfire.outbound;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.gemfire.CacheFactoryBean;
import org.springframework.data.gemfire.RegionFactoryBean;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.Region;

/**
 * @author Mark Fisher
 * @author David Turanski
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.1
 */
public class CacheWritingMessageHandlerTests {

	@Test
	public void mapPayloadWritesToCache() throws Exception {
		CacheFactoryBean cacheFactoryBean = new CacheFactoryBean();
		Cache cache = cacheFactoryBean.getObject();

		RegionFactoryBean<String, String> regionFactoryBean = new RegionFactoryBean<String, String>() { };
		regionFactoryBean.setName("test.mapPayloadWritesToCache");
		regionFactoryBean.setCache(cache);
		regionFactoryBean.afterPropertiesSet();
		Region<String, String> region = regionFactoryBean.getObject();

		assertEquals(0, region.size());

		CacheWritingMessageHandler handler = new CacheWritingMessageHandler(region);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		Map<String, String> map = new HashMap<String, String>();
		map.put("foo", "bar");
		Message<?> message = MessageBuilder.withPayload(map).build();
		handler.handleMessage(message);
		assertEquals(1, region.size());
		assertEquals("bar", region.get("foo"));

		regionFactoryBean.destroy();
		cacheFactoryBean.destroy();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void ExpressionsWriteToCache() throws Exception {
		CacheFactoryBean cacheFactoryBean = new CacheFactoryBean();
		Cache cache = cacheFactoryBean.getObject();

		RegionFactoryBean<String, Object> regionFactoryBean = new RegionFactoryBean<String, Object>() { };
		regionFactoryBean.setName("test.expressionsWriteToCache");
		regionFactoryBean.setCache(cache);
		regionFactoryBean.afterPropertiesSet();
		Region<String, Object> region = regionFactoryBean.getObject();

		assertEquals(0, region.size());

		CacheWritingMessageHandler handler = new CacheWritingMessageHandler(region);

		Map<String, String> expressions = new HashMap<String, String>();
		expressions.put("'foo'", "'bar'");
		expressions.put("payload.toUpperCase()", "headers['bar'].toUpperCase()");
		handler.setCacheEntries(expressions);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		Message<?> message = MessageBuilder.withPayload("foo")
				.copyHeaders(Collections.singletonMap("bar", "bar"))
				.build();
		handler.handleMessage(message);
		assertEquals(2, region.size());
		assertEquals("BAR", region.get("FOO"));
		assertEquals("bar", region.get("foo"));

		handler.setCacheEntryExpressions(Collections.<Expression, Expression>singletonMap(new LiteralExpression("baz"),
				new ValueExpression<Long>(10L)));

		handler.handleMessage(new GenericMessage<String>("test"));
		assertEquals(3, region.size());
		assertEquals(10L, region.get("baz"));

		regionFactoryBean.destroy();
		cacheFactoryBean.destroy();
	}

}
