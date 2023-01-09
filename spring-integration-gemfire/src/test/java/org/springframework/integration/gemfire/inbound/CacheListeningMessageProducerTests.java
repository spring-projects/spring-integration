/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.integration.gemfire.inbound;

import org.apache.geode.cache.Region;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.gemfire.CacheFactoryBean;
import org.springframework.data.gemfire.GenericRegionFactoryBean;
import org.springframework.data.gemfire.RegionAttributesFactoryBean;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.1
 */
public class CacheListeningMessageProducerTests {

	private static final SpelExpressionParser PARSER = new SpelExpressionParser();

	private static CacheFactoryBean cacheFactoryBean;

	private static GenericRegionFactoryBean<String, String> regionFactoryBean;

	private static Region<String, String> region;

	@BeforeClass
	public static void setup() throws Exception {
		cacheFactoryBean = new CacheFactoryBean();

		regionFactoryBean = new GenericRegionFactoryBean<>();
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
	public void receiveNewValuePayloadForCreateEvent() {
		QueueChannel channel = new QueueChannel();
		CacheListeningMessageProducer producer = new CacheListeningMessageProducer(region);
		producer.setPayloadExpression(PARSER.parseExpression("key + '=' + newValue"));
		producer.setOutputChannel(channel);
		producer.setBeanFactory(mock(BeanFactory.class));
		producer.afterPropertiesSet();
		producer.start();

		assertThat(channel.receive(0)).isNull();
		region.put("x", "abc");
		Message<?> message = channel.receive(0);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("x=abc");

		producer.stop();
	}

	@Test
	public void receiveNewValuePayloadForUpdateEvent() {
		QueueChannel channel = new QueueChannel();
		CacheListeningMessageProducer producer = new CacheListeningMessageProducer(region);
		producer.setPayloadExpression(PARSER.parseExpression("newValue"));
		producer.setOutputChannel(channel);
		producer.setBeanFactory(mock(BeanFactory.class));
		producer.afterPropertiesSet();
		producer.start();

		assertThat(channel.receive(0)).isNull();
		region.put("x", "abc");
		Message<?> message1 = channel.receive(0);
		assertThat(message1).isNotNull();
		assertThat(message1.getPayload()).isEqualTo("abc");
		region.put("x", "xyz");
		Message<?> message2 = channel.receive(0);
		assertThat(message2).isNotNull();
		assertThat(message2.getPayload()).isEqualTo("xyz");

		producer.stop();
	}

	@Test
	public void receiveOldValuePayloadForDestroyEvent() {
		QueueChannel channel = new QueueChannel();
		CacheListeningMessageProducer producer = new CacheListeningMessageProducer(region);
		producer.setSupportedEventTypes(EventType.DESTROYED);
		producer.setPayloadExpression(PARSER.parseExpression("oldValue"));
		producer.setOutputChannel(channel);
		producer.setBeanFactory(mock(BeanFactory.class));
		producer.afterPropertiesSet();
		producer.start();

		assertThat(channel.receive(0)).isNull();
		region.put("foo", "abc");
		assertThat(channel.receive(0)).isNull();
		region.destroy("foo");
		Message<?> message2 = channel.receive(0);
		assertThat(message2).isNotNull();
		assertThat(message2.getPayload()).isEqualTo("abc");

		producer.stop();
	}

	@Test
	public void receiveOldValuePayloadForInvalidateEvent() {
		QueueChannel channel = new QueueChannel();
		CacheListeningMessageProducer producer = new CacheListeningMessageProducer(region);
		producer.setSupportedEventTypes(EventType.INVALIDATED);
		producer.setPayloadExpression(PARSER.parseExpression("key + ' was ' + oldValue"));
		producer.setOutputChannel(channel);
		producer.setBeanFactory(mock(BeanFactory.class));
		producer.afterPropertiesSet();
		producer.start();

		assertThat(channel.receive(0)).isNull();
		region.put("foo", "abc");
		assertThat(channel.receive(0)).isNull();
		region.invalidate("foo");
		Message<?> message2 = channel.receive(0);
		assertThat(message2).isNotNull();
		assertThat(message2.getPayload()).isEqualTo("foo was abc");

		producer.stop();
	}

	private static void setRegionAttributes(GenericRegionFactoryBean<String, String> regionFactoryBean)
			throws Exception {

		RegionAttributesFactoryBean<String, String> attributesFactoryBean = new RegionAttributesFactoryBean<>();
		attributesFactoryBean.afterPropertiesSet();
		regionFactoryBean.setAttributes(attributesFactoryBean.getObject());
	}

}
