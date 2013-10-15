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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.MqttMessageConverter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 1.0
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class MqttMessageDrivenChannelAdapterParserTests {

	@Autowired
	private MqttPahoMessageDrivenChannelAdapter oneTopicAdapter;

	@Autowired
	private MqttPahoMessageDrivenChannelAdapter twoTopicsAdapter;

	@Autowired
	private MessageChannel out;

	@Autowired
	private MqttMessageConverter converter;

	@Autowired
	private DefaultMqttPahoClientFactory clientFactory;

	@Test
	public void testOneTopic() {
		assertEquals("tcp://localhost:1883", TestUtils.getPropertyValue(oneTopicAdapter, "url"));
		assertFalse(TestUtils.getPropertyValue(oneTopicAdapter, "autoStartup", Boolean.class));
		assertEquals(25, TestUtils.getPropertyValue(oneTopicAdapter, "phase"));
		assertEquals("foo", TestUtils.getPropertyValue(oneTopicAdapter, "clientId"));
		assertEquals("bar", TestUtils.getPropertyValue(oneTopicAdapter, "topic", String[].class)[0]);
		assertSame(converter, TestUtils.getPropertyValue(oneTopicAdapter, "converter"));
		assertEquals(123L, TestUtils.getPropertyValue(oneTopicAdapter, "messagingTemplate.sendTimeout"));
		assertSame(out, TestUtils.getPropertyValue(oneTopicAdapter, "outputChannel"));
		assertSame(clientFactory, TestUtils.getPropertyValue(oneTopicAdapter, "clientFactory"));
	}

	@Test
	public void testTwoTopics() {
		assertEquals("tcp://localhost:1883", TestUtils.getPropertyValue(oneTopicAdapter, "url"));
		assertFalse(TestUtils.getPropertyValue(twoTopicsAdapter, "autoStartup", Boolean.class));
		assertEquals(25, TestUtils.getPropertyValue(twoTopicsAdapter, "phase"));
		assertEquals("foo", TestUtils.getPropertyValue(twoTopicsAdapter, "clientId"));
		assertEquals("bar", TestUtils.getPropertyValue(twoTopicsAdapter, "topic", String[].class)[0]);
		assertEquals("baz", TestUtils.getPropertyValue(twoTopicsAdapter, "topic", String[].class)[1]);
		assertSame(converter, TestUtils.getPropertyValue(twoTopicsAdapter, "converter"));
		assertEquals(123L, TestUtils.getPropertyValue(twoTopicsAdapter, "messagingTemplate.sendTimeout"));
		assertSame(out, TestUtils.getPropertyValue(twoTopicsAdapter, "outputChannel"));
		assertSame(clientFactory, TestUtils.getPropertyValue(twoTopicsAdapter, "clientFactory"));
	}

}
