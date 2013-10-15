/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.integration.mqtt.config.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.integration.mqtt.support.MqttMessageConverter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 1.0
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class MqttOutboundChannelAdapterParserTests {

	@Autowired @Qualifier("withConverter.handler")
	private MqttPahoMessageHandler withConverterHandler;

	@Autowired @Qualifier("withDefaultConverter.handler")
	private MqttPahoMessageHandler withDefaultConverterHandler;

	@Autowired
	private MqttMessageConverter converter;

	@Autowired
	private DefaultMqttPahoClientFactory clientFactory;

	@Test
	public void testWithConverter() {
		assertEquals("tcp://localhost:1883", TestUtils.getPropertyValue(withConverterHandler, "url"));
		assertFalse(TestUtils.getPropertyValue(withConverterHandler, "autoStartup", Boolean.class));
		assertEquals(25, TestUtils.getPropertyValue(withConverterHandler, "phase"));
		assertEquals("foo", TestUtils.getPropertyValue(withConverterHandler, "clientId"));
		assertEquals("bar", TestUtils.getPropertyValue(withConverterHandler, "defaultTopic"));
		assertSame(converter, TestUtils.getPropertyValue(withConverterHandler, "converter"));
		assertSame(clientFactory, TestUtils.getPropertyValue(withConverterHandler, "clientFactory"));
	}

	@Test
	public void testWithDefaultConverter() {
		assertEquals("tcp://localhost:1883", TestUtils.getPropertyValue(withDefaultConverterHandler, "url"));
		assertFalse(TestUtils.getPropertyValue(withDefaultConverterHandler, "autoStartup", Boolean.class));
		assertEquals(25, TestUtils.getPropertyValue(withDefaultConverterHandler, "phase"));
		assertEquals("foo", TestUtils.getPropertyValue(withDefaultConverterHandler, "clientId"));
		assertEquals("bar", TestUtils.getPropertyValue(withDefaultConverterHandler, "defaultTopic"));
		assertEquals(1, TestUtils.getPropertyValue(withDefaultConverterHandler, "defaultQos"));
		assertTrue(TestUtils.getPropertyValue(withDefaultConverterHandler, "defaultRetained", Boolean.class));
		MqttMessageConverter defaultConverter = TestUtils.getPropertyValue(withDefaultConverterHandler, "converter",
				MqttMessageConverter.class);
		assertTrue(defaultConverter instanceof DefaultPahoMessageConverter);
		assertEquals(1, TestUtils.getPropertyValue(defaultConverter, "defaultQos"));
		assertTrue(TestUtils.getPropertyValue(defaultConverter, "defaultRetained", Boolean.class));
		assertSame(clientFactory, TestUtils.getPropertyValue(withDefaultConverterHandler, "clientFactory"));
	}

}
