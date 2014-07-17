/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.amqp.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.ChannelAwareMessageListener;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.amqp.AmqpHeaders;
import org.springframework.integration.amqp.inbound.AmqpInboundChannelAdapter;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 * @since 2.1
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class AmqpInboundChannelAdapterParserTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void verifyIdAsChannel() {
		Object channel = context.getBean("rabbitInbound");
		Object adapter = context.getBean("rabbitInbound.adapter");
		assertEquals(DirectChannel.class, channel.getClass());
		assertEquals(AmqpInboundChannelAdapter.class, adapter.getClass());
		assertEquals(Boolean.TRUE, TestUtils.getPropertyValue(adapter, "autoStartup"));
		assertEquals(Integer.MAX_VALUE / 2, TestUtils.getPropertyValue(adapter, "phase"));
		assertTrue(TestUtils.getPropertyValue(adapter, "messageListenerContainer.missingQueuesFatal", Boolean.class));
	}

	@Test
	public void verifyLifeCycle() {
		Object adapter = context.getBean("autoStartFalse.adapter");
		assertEquals(Boolean.FALSE, TestUtils.getPropertyValue(adapter, "autoStartup"));
		assertEquals(123, TestUtils.getPropertyValue(adapter, "phase"));
		assertEquals(AcknowledgeMode.NONE, TestUtils.getPropertyValue(adapter, "messageListenerContainer.acknowledgeMode"));
		assertFalse(TestUtils.getPropertyValue(adapter, "messageListenerContainer.missingQueuesFatal", Boolean.class));
	}

	@Test
	public void withHeaderMapperStandardAndCustomHeaders() throws Exception {
		AmqpInboundChannelAdapter adapter = context.getBean("withHeaderMapperStandardAndCustomHeaders",
				AmqpInboundChannelAdapter.class);

		AbstractMessageListenerContainer mlc =
				TestUtils.getPropertyValue(adapter, "messageListenerContainer", AbstractMessageListenerContainer.class);
		ChannelAwareMessageListener listener = TestUtils.getPropertyValue(mlc, "messageListener",
				ChannelAwareMessageListener.class);
		MessageProperties amqpProperties = new MessageProperties();
		amqpProperties.setAppId("test.appId");
		amqpProperties.setClusterId("test.clusterId");
		amqpProperties.setContentEncoding("test.contentEncoding");
		amqpProperties.setContentLength(99L);
		amqpProperties.setContentType("test.contentType");
		amqpProperties.setHeader("foo", "foo");
		amqpProperties.setHeader("bar", "bar");
		Message amqpMessage = new Message("hello".getBytes(), amqpProperties);
		listener.onMessage(amqpMessage, null);
		QueueChannel requestChannel = context.getBean("requestChannel", QueueChannel.class);
		org.springframework.messaging.Message<?> siMessage = requestChannel.receive(0);
		assertEquals("foo", siMessage.getHeaders().get("foo"));
		assertNull(siMessage.getHeaders().get("bar"));
		assertNotNull(siMessage.getHeaders().get(AmqpHeaders.CONTENT_ENCODING));
		assertNotNull(siMessage.getHeaders().get(AmqpHeaders.CLUSTER_ID));
		assertNotNull(siMessage.getHeaders().get(AmqpHeaders.APP_ID));
		assertNotNull(siMessage.getHeaders().get(AmqpHeaders.CONTENT_TYPE));
	}

	@Test
	public void withHeaderMapperOnlyCustomHeaders() throws Exception {
		AmqpInboundChannelAdapter adapter = context.getBean("withHeaderMapperOnlyCustomHeaders",
				AmqpInboundChannelAdapter.class);

		AbstractMessageListenerContainer mlc =
				TestUtils.getPropertyValue(adapter, "messageListenerContainer", AbstractMessageListenerContainer.class);
		ChannelAwareMessageListener listener = TestUtils.getPropertyValue(mlc, "messageListener",
				ChannelAwareMessageListener.class);
		MessageProperties amqpProperties = new MessageProperties();
		amqpProperties.setAppId("test.appId");
		amqpProperties.setClusterId("test.clusterId");
		amqpProperties.setContentEncoding("test.contentEncoding");
		amqpProperties.setContentLength(99L);
		amqpProperties.setContentType("test.contentType");
		amqpProperties.setHeader("foo", "foo");
		amqpProperties.setHeader("bar", "bar");
		Message amqpMessage = new Message("hello".getBytes(), amqpProperties);
		listener.onMessage(amqpMessage, null);
		QueueChannel requestChannel = context.getBean("requestChannel", QueueChannel.class);
		org.springframework.messaging.Message<?> siMessage = requestChannel.receive(0);
		assertEquals("foo", siMessage.getHeaders().get("foo"));
		assertNull(siMessage.getHeaders().get("bar"));
		assertNull(siMessage.getHeaders().get(AmqpHeaders.CONTENT_ENCODING));
		assertNull(siMessage.getHeaders().get(AmqpHeaders.CLUSTER_ID));
		assertNull(siMessage.getHeaders().get(AmqpHeaders.APP_ID));
		assertNull(siMessage.getHeaders().get(AmqpHeaders.CONTENT_TYPE));
	}

	@Test
	public void withHeaderMapperNothingToMap() throws Exception {
		AmqpInboundChannelAdapter adapter = context.getBean("withHeaderMapperNothingToMap",
				AmqpInboundChannelAdapter.class);

		AbstractMessageListenerContainer mlc =
				TestUtils.getPropertyValue(adapter, "messageListenerContainer", AbstractMessageListenerContainer.class);
		ChannelAwareMessageListener listener = TestUtils.getPropertyValue(mlc, "messageListener",
				ChannelAwareMessageListener.class);
		MessageProperties amqpProperties = new MessageProperties();
		amqpProperties.setAppId("test.appId");
		amqpProperties.setClusterId("test.clusterId");
		amqpProperties.setContentEncoding("test.contentEncoding");
		amqpProperties.setContentLength(99L);
		amqpProperties.setContentType("test.contentType");
		amqpProperties.setHeader("foo", "foo");
		amqpProperties.setHeader("bar", "bar");
		Message amqpMessage = new Message("hello".getBytes(), amqpProperties);
		listener.onMessage(amqpMessage, null);

		QueueChannel requestChannel = context.getBean("requestChannel", QueueChannel.class);
		org.springframework.messaging.Message<?> siMessage = requestChannel.receive(0);
		assertNull(siMessage.getHeaders().get("foo"));
		assertNull(siMessage.getHeaders().get("bar"));
		assertNull(siMessage.getHeaders().get(AmqpHeaders.CONTENT_ENCODING));
		assertNull(siMessage.getHeaders().get(AmqpHeaders.CLUSTER_ID));
		assertNull(siMessage.getHeaders().get(AmqpHeaders.APP_ID));
		assertNull(siMessage.getHeaders().get(AmqpHeaders.CONTENT_TYPE));
	}

	@Test
	public void withHeaderMapperDefaultMapping() throws Exception {
		AmqpInboundChannelAdapter adapter = context.getBean("withHeaderMapperDefaultMapping",
				AmqpInboundChannelAdapter.class);

		AbstractMessageListenerContainer mlc =
				TestUtils.getPropertyValue(adapter, "messageListenerContainer", AbstractMessageListenerContainer.class);
		ChannelAwareMessageListener listener = TestUtils.getPropertyValue(mlc, "messageListener",
				ChannelAwareMessageListener.class);
		MessageProperties amqpProperties = new MessageProperties();
		amqpProperties.setAppId("test.appId");
		amqpProperties.setClusterId("test.clusterId");
		amqpProperties.setContentEncoding("test.contentEncoding");
		amqpProperties.setContentLength(99L);
		amqpProperties.setContentType("test.contentType");
		amqpProperties.setHeader("foo", "foo");
		amqpProperties.setHeader("bar", "bar");
		Message amqpMessage = new Message("hello".getBytes(), amqpProperties);
		listener.onMessage(amqpMessage, null);
		QueueChannel requestChannel = context.getBean("requestChannel", QueueChannel.class);
		org.springframework.messaging.Message<?> siMessage = requestChannel.receive(0);
		assertNull(siMessage.getHeaders().get("bar"));
		assertNull(siMessage.getHeaders().get("foo"));
		assertNotNull(siMessage.getHeaders().get(AmqpHeaders.CONTENT_ENCODING));
		assertNotNull(siMessage.getHeaders().get(AmqpHeaders.CLUSTER_ID));
		assertNotNull(siMessage.getHeaders().get(AmqpHeaders.APP_ID));
		assertNotNull(siMessage.getHeaders().get(AmqpHeaders.CONTENT_TYPE));
	}

	@Test
	public void testInt2971HeaderMapperAndMappedHeadersExclusivity() {
		try {
			new ClassPathXmlApplicationContext("AmqpInboundChannelAdapterParserTests-headerMapper-fail-context.xml",
					this.getClass());
		}
		catch (BeanDefinitionParsingException e) {
			assertTrue(e.getMessage().startsWith("Configuration problem: The 'header-mapper' attribute " +
					"is mutually exclusive with 'mapped-request-headers' or 'mapped-reply-headers'"));
		}
	}

}
