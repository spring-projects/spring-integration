/*
 * Copyright 2002-2012 the original author or authors.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.amqp.AmqpHeaders;
import org.springframework.integration.amqp.inbound.AmqpInboundChannelAdapter;
import org.springframework.integration.amqp.support.JsonAwareInboundMessageConverter;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
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
		assertEquals(0, TestUtils.getPropertyValue(adapter, "phase"));
	}

	@Test
	public void verifyLifeCycle() {
		Object adapter = context.getBean("autoStartFalse.adapter");
		assertEquals(Boolean.FALSE, TestUtils.getPropertyValue(adapter, "autoStartup"));
		assertEquals(123, TestUtils.getPropertyValue(adapter, "phase"));
	}

	@Test
	public void withHeaderMapperStandardAndCustomHeaders() {
		AmqpInboundChannelAdapter adapter = context.getBean("withHeaderMapperStandardAndCustomHeaders", AmqpInboundChannelAdapter.class);

		AbstractMessageListenerContainer mlc =
				TestUtils.getPropertyValue(adapter, "messageListenerContainer", AbstractMessageListenerContainer.class);
		MessageListener listener = TestUtils.getPropertyValue(mlc, "messageListener", MessageListener.class);
		MessageProperties amqpProperties = new MessageProperties();
		amqpProperties.setAppId("test.appId");
		amqpProperties.setClusterId("test.clusterId");
		amqpProperties.setContentEncoding("test.contentEncoding");
		amqpProperties.setContentLength(99L);
		amqpProperties.setContentType("test.contentType");
		amqpProperties.setHeader("foo", "foo");
		amqpProperties.setHeader("bar", "bar");
		Message amqpMessage = new Message("hello".getBytes(), amqpProperties);
		listener.onMessage(amqpMessage);
		QueueChannel requestChannel = context.getBean("requestChannel", QueueChannel.class);
		org.springframework.integration.Message<?> siMessage = requestChannel.receive(0);
		assertEquals("foo", siMessage.getHeaders().get("foo"));
		assertNull(siMessage.getHeaders().get("bar"));
		assertNotNull(siMessage.getHeaders().get(AmqpHeaders.CONTENT_ENCODING));
		assertNotNull(siMessage.getHeaders().get(AmqpHeaders.CLUSTER_ID));
		assertNotNull(siMessage.getHeaders().get(AmqpHeaders.APP_ID));
		assertNotNull(siMessage.getHeaders().get(AmqpHeaders.CONTENT_TYPE));
	}

	@Test
	public void withHeaderMapperOnlyCustomHeaders() {
		AmqpInboundChannelAdapter adapter = context.getBean("withHeaderMapperOnlyCustomHeaders", AmqpInboundChannelAdapter.class);

		AbstractMessageListenerContainer mlc =
				TestUtils.getPropertyValue(adapter, "messageListenerContainer", AbstractMessageListenerContainer.class);
		MessageListener listener = TestUtils.getPropertyValue(mlc, "messageListener", MessageListener.class);
		MessageProperties amqpProperties = new MessageProperties();
		amqpProperties.setAppId("test.appId");
		amqpProperties.setClusterId("test.clusterId");
		amqpProperties.setContentEncoding("test.contentEncoding");
		amqpProperties.setContentLength(99L);
		amqpProperties.setContentType("test.contentType");
		amqpProperties.setHeader("foo", "foo");
		amqpProperties.setHeader("bar", "bar");
		Message amqpMessage = new Message("hello".getBytes(), amqpProperties);
		listener.onMessage(amqpMessage);
		QueueChannel requestChannel = context.getBean("requestChannel", QueueChannel.class);
		org.springframework.integration.Message<?> siMessage = requestChannel.receive(0);
		assertEquals("foo", siMessage.getHeaders().get("foo"));
		assertNull(siMessage.getHeaders().get("bar"));
		assertNull(siMessage.getHeaders().get(AmqpHeaders.CONTENT_ENCODING));
		assertNull(siMessage.getHeaders().get(AmqpHeaders.CLUSTER_ID));
		assertNull(siMessage.getHeaders().get(AmqpHeaders.APP_ID));
		assertNull(siMessage.getHeaders().get(AmqpHeaders.CONTENT_TYPE));
	}

	@Test
	public void withHeaderMapperNothingToMap() {
		AmqpInboundChannelAdapter adapter = context.getBean("withHeaderMapperNothingToMap", AmqpInboundChannelAdapter.class);

		AbstractMessageListenerContainer mlc =
				TestUtils.getPropertyValue(adapter, "messageListenerContainer", AbstractMessageListenerContainer.class);
		MessageListener listener = TestUtils.getPropertyValue(mlc, "messageListener", MessageListener.class);
		MessageProperties amqpProperties = new MessageProperties();
		amqpProperties.setAppId("test.appId");
		amqpProperties.setClusterId("test.clusterId");
		amqpProperties.setContentEncoding("test.contentEncoding");
		amqpProperties.setContentLength(99L);
		amqpProperties.setContentType("test.contentType");
		amqpProperties.setHeader("foo", "foo");
		amqpProperties.setHeader("bar", "bar");
		Message amqpMessage = new Message("hello".getBytes(), amqpProperties);
		listener.onMessage(amqpMessage);

		QueueChannel requestChannel = context.getBean("requestChannel", QueueChannel.class);
		org.springframework.integration.Message<?> siMessage = requestChannel.receive(0);
		assertNull(siMessage.getHeaders().get("foo"));
		assertNull(siMessage.getHeaders().get("bar"));
		assertNull(siMessage.getHeaders().get(AmqpHeaders.CONTENT_ENCODING));
		assertNull(siMessage.getHeaders().get(AmqpHeaders.CLUSTER_ID));
		assertNull(siMessage.getHeaders().get(AmqpHeaders.APP_ID));
		assertNull(siMessage.getHeaders().get(AmqpHeaders.CONTENT_TYPE));
	}

	@Test
	public void withHeaderMapperDefaultMapping() {
		AmqpInboundChannelAdapter adapter = context.getBean("withHeaderMapperDefaultMapping", AmqpInboundChannelAdapter.class);

		AbstractMessageListenerContainer mlc =
				TestUtils.getPropertyValue(adapter, "messageListenerContainer", AbstractMessageListenerContainer.class);
		MessageListener listener = TestUtils.getPropertyValue(mlc, "messageListener", MessageListener.class);
		MessageProperties amqpProperties = new MessageProperties();
		amqpProperties.setAppId("test.appId");
		amqpProperties.setClusterId("test.clusterId");
		amqpProperties.setContentEncoding("test.contentEncoding");
		amqpProperties.setContentLength(99L);
		amqpProperties.setContentType("test.contentType");
		amqpProperties.setHeader("foo", "foo");
		amqpProperties.setHeader("bar", "bar");
		Message amqpMessage = new Message("hello".getBytes(), amqpProperties);
		listener.onMessage(amqpMessage);
		QueueChannel requestChannel = context.getBean("requestChannel", QueueChannel.class);
		org.springframework.integration.Message<?> siMessage = requestChannel.receive(0);
		assertNull(siMessage.getHeaders().get("bar"));
		assertNull(siMessage.getHeaders().get("foo"));
		assertNotNull(siMessage.getHeaders().get(AmqpHeaders.CONTENT_ENCODING));
		assertNotNull(siMessage.getHeaders().get(AmqpHeaders.CLUSTER_ID));
		assertNotNull(siMessage.getHeaders().get(AmqpHeaders.APP_ID));
		assertNotNull(siMessage.getHeaders().get(AmqpHeaders.CONTENT_TYPE));
		assertTrue(TestUtils.getPropertyValue(adapter, "messageConverter") instanceof SimpleMessageConverter);
	}

	@Test
	public void withRequestPayloadTypeString() {
		AmqpInboundChannelAdapter adapter = context.getBean("withRequestPayloadTypeString", AmqpInboundChannelAdapter.class);

		AbstractMessageListenerContainer mlc =
				TestUtils.getPropertyValue(adapter, "messageListenerContainer", AbstractMessageListenerContainer.class);
		MessageListener listener = TestUtils.getPropertyValue(mlc, "messageListener", MessageListener.class);
		MessageProperties amqpProperties = new MessageProperties();
		amqpProperties.setAppId("test.appId");
		amqpProperties.setClusterId("test.clusterId");
		amqpProperties.setContentLength(99L);
		amqpProperties.setContentType("text/plain");
		amqpProperties.setHeader("foo", "foo");
		amqpProperties.setHeader("bar", "bar");
		Message amqpMessage = new Message("hello".getBytes(), amqpProperties);
		listener.onMessage(amqpMessage);
		QueueChannel requestChannel = context.getBean("requestChannel", QueueChannel.class);
		org.springframework.integration.Message<?> siMessage = requestChannel.receive(0);
		assertNull(siMessage.getHeaders().get("bar"));
		assertNull(siMessage.getHeaders().get("foo"));
		assertNotNull(siMessage.getHeaders().get(AmqpHeaders.CLUSTER_ID));
		assertNotNull(siMessage.getHeaders().get(AmqpHeaders.APP_ID));
		assertNotNull(siMessage.getHeaders().get(AmqpHeaders.CONTENT_TYPE));
		assertTrue(TestUtils.getPropertyValue(adapter, "messageConverter") instanceof JsonAwareInboundMessageConverter);
		assertEquals(String.class, TestUtils.getPropertyValue(adapter, "messageConverter.clazz"));
		assertEquals("hello", siMessage.getPayload());

	}

	@Test
	public void withRequestPayloadTypeFoo() {
		AmqpInboundChannelAdapter adapter = context.getBean("withRequestPayloadTypeFoo", AmqpInboundChannelAdapter.class);

		AbstractMessageListenerContainer mlc =
				TestUtils.getPropertyValue(adapter, "messageListenerContainer", AbstractMessageListenerContainer.class);
		MessageListener listener = TestUtils.getPropertyValue(mlc, "messageListener", MessageListener.class);
		MessageProperties amqpProperties = new MessageProperties();
		amqpProperties.setAppId("test.appId");
		amqpProperties.setClusterId("test.clusterId");
		amqpProperties.setContentLength(99L);
		amqpProperties.setContentType("application/json");
		amqpProperties.setHeader("foo", "foo");
		amqpProperties.setHeader("bar", "bar");
		Message amqpMessage = new Message("{\"foo\":\"bar\"}".getBytes(), amqpProperties);
		listener.onMessage(amqpMessage);
		QueueChannel requestChannel = context.getBean("requestChannel", QueueChannel.class);
		org.springframework.integration.Message<?> siMessage = requestChannel.receive(0);
		assertNull(siMessage.getHeaders().get("bar"));
		assertNull(siMessage.getHeaders().get("foo"));
		assertNotNull(siMessage.getHeaders().get(AmqpHeaders.CLUSTER_ID));
		assertNotNull(siMessage.getHeaders().get(AmqpHeaders.APP_ID));
		assertNotNull(siMessage.getHeaders().get(AmqpHeaders.CONTENT_TYPE));
		assertTrue(TestUtils.getPropertyValue(adapter, "messageConverter") instanceof JsonAwareInboundMessageConverter);
		assertEquals(Foo.class, TestUtils.getPropertyValue(adapter, "messageConverter.clazz"));
		assertEquals(new Foo("bar"), siMessage.getPayload());

	}

	@Test
	public void testBadConfig() {
		try {
			new ClassPathXmlApplicationContext(this.getClass().getSimpleName() + "-context-fail.xml", this.getClass());
		}
		catch (BeanDefinitionParsingException e) {
			assertTrue(e.getMessage().contains("Only one of 'message-converter' and 'request-payload-type' is allowed"));
		}
	}

	public static class Foo {
		private String foo;

		public Foo() {
		}

		public Foo(String string) {
			this.foo = string;
		}

		public String getFoo() {
			return foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((foo == null) ? 0 : foo.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Foo other = (Foo) obj;
			if (foo == null) {
				if (other.foo != null)
					return false;
			}
			else if (!foo.equals(other.foo))
				return false;
			return true;
		}
	}

}
