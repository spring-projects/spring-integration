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

package org.springframework.integration.gemfire.inbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

import org.apache.geode.cache.Region;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.gemfire.CacheFactoryBean;
import org.springframework.data.gemfire.RegionAttributesFactoryBean;
import org.springframework.data.gemfire.RegionFactoryBean;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.Message;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.1
 */
public class CacheListeningMessageProducerTests {

	private static final SpelExpressionParser PARSER = new SpelExpressionParser();

	private static CacheFactoryBean cacheFactoryBean;

	private static RegionFactoryBean<String, String> regionFactoryBean;

	private static Region<String, String> region;

	@BeforeClass
	public static void setup() throws Exception {
		cacheFactoryBean = new CacheFactoryBean();

		regionFactoryBean = new RegionFactoryBean<String, String>() {

		};
		regionFactoryBean.setName("test.receiveNewValuePayloadForCreateEvent");
		regionFactoryBean.setCache(cacheFactoryBean.getObject());
		setRegionAttributes(regionFactoryBean);
		regionFactoryBean.afterPropertiesSet();

		region = regionFactoryBean.getObject();
	}

	@AfterClass
	public static void teardown() throws Exception {
		regionFactoryBean.destroy();
		cacheFactoryBean.destroy();
	}

	@Test
	public void receiveNewValuePayloadForCreateEvent() throws Exception {
		QueueChannel channel = new QueueChannel();
		CacheListeningMessageProducer producer = new CacheListeningMessageProducer(region);
		producer.setPayloadExpression(PARSER.parseExpression("key + '=' + newValue"));
		producer.setOutputChannel(channel);
		producer.setBeanFactory(mock(BeanFactory.class));
		producer.afterPropertiesSet();
		producer.start();

		assertNull(channel.receive(0));
		region.put("x", "abc");
		Message<?> message = channel.receive(0);
		assertNotNull(message);
		assertEquals("x=abc", message.getPayload());

		producer.stop();
	}

	@Test
	public void receiveNewValuePayloadForUpdateEvent() throws Exception {
		QueueChannel channel = new QueueChannel();
		CacheListeningMessageProducer producer = new CacheListeningMessageProducer(region);
		producer.setPayloadExpression(PARSER.parseExpression("newValue"));
		producer.setOutputChannel(channel);
		producer.setBeanFactory(mock(BeanFactory.class));
		producer.afterPropertiesSet();
		producer.start();

		assertNull(channel.receive(0));
		region.put("x", "abc");
		Message<?> message1 = channel.receive(0);
		assertNotNull(message1);
		assertEquals("abc", message1.getPayload());
		region.put("x", "xyz");
		Message<?> message2 = channel.receive(0);
		assertNotNull(message2);
		assertEquals("xyz", message2.getPayload());

		producer.stop();
	}

	@Test
	public void receiveOldValuePayloadForDestroyEvent() throws Exception {
		QueueChannel channel = new QueueChannel();
		CacheListeningMessageProducer producer = new CacheListeningMessageProducer(region);
		producer.setSupportedEventTypes(EventType.DESTROYED);
		producer.setPayloadExpression(PARSER.parseExpression("oldValue"));
		producer.setOutputChannel(channel);
		producer.setBeanFactory(mock(BeanFactory.class));
		producer.afterPropertiesSet();
		producer.start();

		assertNull(channel.receive(0));
		region.put("foo", "abc");
		assertNull(channel.receive(0));
		region.destroy("foo");
		Message<?> message2 = channel.receive(0);
		assertNotNull(message2);
		assertEquals("abc", message2.getPayload());

		producer.stop();
	}

	@Test
	public void receiveOldValuePayloadForInvalidateEvent() throws Exception {
		QueueChannel channel = new QueueChannel();
		CacheListeningMessageProducer producer = new CacheListeningMessageProducer(region);
		producer.setSupportedEventTypes(EventType.INVALIDATED);
		producer.setPayloadExpression(PARSER.parseExpression("key + ' was ' + oldValue"));
		producer.setOutputChannel(channel);
		producer.setBeanFactory(mock(BeanFactory.class));
		producer.afterPropertiesSet();
		producer.start();

		assertNull(channel.receive(0));
		region.put("foo", "abc");
		assertNull(channel.receive(0));
		region.invalidate("foo");
		Message<?> message2 = channel.receive(0);
		assertNotNull(message2);
		assertEquals("foo was abc", message2.getPayload());

		producer.stop();
	}

	@SuppressWarnings("unchecked")
	private static void setRegionAttributes(RegionFactoryBean<String, String> regionFactoryBean) throws Exception {
		RegionAttributesFactoryBean attributesFactoryBean = new RegionAttributesFactoryBean();
		attributesFactoryBean.afterPropertiesSet();
		regionFactoryBean.setAttributes(attributesFactoryBean.getObject());
	}

}
