/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.gemfire.inbound;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.DataPolicy;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.asyncqueue.AsyncEventQueueFactory;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.gemfire.CacheFactoryBean;
import org.springframework.data.gemfire.RegionAttributesFactoryBean;
import org.springframework.data.gemfire.RegionFactoryBean;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.Message;

/**
 * @author Patrick Peralta
 */
public class AsyncEventListeningMessageProducerTests {

	private static final SpelExpressionParser PARSER = new SpelExpressionParser();

	@Test
	public void receiveNewValuePayloadForCreateEvent() throws Exception {
		Cache cache = new CacheFactoryBean().getObject();
		Region<String, String> region = createRegion(cache, "test.receiveNewValuePayloadForCreateEvent");

		QueueChannel channel = new QueueChannel();
		AsyncEventListeningMessageProducer producer = new AsyncEventListeningMessageProducer(region,
				createAsyncEventQueueFactory(cache));
		producer.setPayloadExpression(PARSER.parseExpression("key + '=' + deserializedValue"));
		producer.setOutputChannel(channel);
		producer.setBeanFactory(mock(BeanFactory.class));
		producer.afterPropertiesSet();
		producer.start();

		assertNull(channel.receive(0));
		region.put("x", "abc");
		Message<?> message = channel.receive(1000);
		assertNotNull(message);
		assertEquals("x=abc", message.getPayload());
	}

	@Test
	public void receiveNewValuePayloadForUpdateEvent() throws Exception {
		Cache cache = new CacheFactoryBean().getObject();
		Region<String, String> region = createRegion(cache, "test.receiveNewValuePayloadForUpdateEvent");

		QueueChannel channel = new QueueChannel();
		AsyncEventListeningMessageProducer producer = new AsyncEventListeningMessageProducer(region,
				createAsyncEventQueueFactory(cache));
		producer.setPayloadExpression(PARSER.parseExpression("deserializedValue"));
		producer.setOutputChannel(channel);
		producer.setBeanFactory(mock(BeanFactory.class));
		producer.afterPropertiesSet();
		producer.start();

		assertNull(channel.receive(0));
		region.put("x", "abc");
		Message<?> message1 = channel.receive(1000);
		assertNotNull(message1);
		assertEquals("abc", message1.getPayload());
		region.put("x", "xyz");
		Message<?> message2 = channel.receive(1000);
		assertNotNull(message2);
		assertEquals("xyz", message2.getPayload());
	}

	private AsyncEventQueueFactory createAsyncEventQueueFactory(Cache cache) {
		AsyncEventQueueFactory queueFactory = cache.createAsyncEventQueueFactory();
		queueFactory.setPersistent(false);
		queueFactory.setParallel(false);

		return queueFactory;
	}

	@SuppressWarnings("unchecked")
	private Region<String, String> createRegion(Cache cache, String name) throws Exception {
		RegionFactoryBean<String, String> regionFactoryBean = new RegionFactoryBean<String, String>() {};
		regionFactoryBean.setName(name);
		regionFactoryBean.setCache(cache);
		regionFactoryBean.setDataPolicy(DataPolicy.PARTITION);
		RegionAttributesFactoryBean attributesFactoryBean = new RegionAttributesFactoryBean();
		attributesFactoryBean.afterPropertiesSet();
		regionFactoryBean.setAttributes(attributesFactoryBean.getObject());
		regionFactoryBean.afterPropertiesSet();
		return regionFactoryBean.getObject();
	}

}
