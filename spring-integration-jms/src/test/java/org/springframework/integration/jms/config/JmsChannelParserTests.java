/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.integration.jms.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.jms.PollableJmsChannel;
import org.springframework.integration.jms.SubscribableJmsChannel;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.SimpleMessageListenerContainer;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @author Gary Russell
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class JmsChannelParserTests {

	@Autowired
	private MessageChannel queueReferenceChannel;

	@Autowired
	private MessageChannel queueNameChannel;

	@Autowired
	private MessageChannel queueNameWithResolverChannel;

	@Autowired
	private Queue queue;

	@Autowired
	private MessageChannel topicReferenceChannel;

	@Autowired
	private MessageChannel topicNameChannel;

	@Autowired
	private MessageChannel withPlaceholders;

	@Autowired
	private MessageChannel topicNameWithResolverChannel;

	@Autowired
	private MessageChannel channelWithConcurrencySettings;

	@Autowired
	private MessageChannel queueChannelWithInterceptors;

	@Autowired
	private MessageChannel topicChannelWithInterceptors;

	@Autowired
	private MessageChannel pollableQueueReferenceChannel;

	@Autowired
	private MessageChannel pollableQueueNameChannel;

	@Autowired
	private MessageChannel pollableWithSelectorChannel;

	@Autowired
	private Topic topic;

	@Autowired
	private MessageChannel withDefaultContainer;

	@Autowired
	private MessageChannel withExplicitDefaultContainer;

	@Autowired
	private MessageChannel withSimpleContainer;

	@Autowired
	private MessageChannel withContainerClass;

	@Autowired
	private MessageChannel withContainerClassSpEL;

	@Autowired
	private MessageBuilderFactory messageBuilderFactory;

	@Test
	public void queueReferenceChannel() {
		assertEquals(SubscribableJmsChannel.class, queueReferenceChannel.getClass());
		SubscribableJmsChannel channel = (SubscribableJmsChannel) queueReferenceChannel;
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		JmsTemplate jmsTemplate = (JmsTemplate) accessor.getPropertyValue("jmsTemplate");
		AbstractMessageListenerContainer container = (AbstractMessageListenerContainer) accessor.getPropertyValue("container");
		assertEquals(queue, jmsTemplate.getDefaultDestination());
		assertEquals(queue, container.getDestination());
		assertEquals(true, TestUtils.getPropertyValue(jmsTemplate, "explicitQosEnabled"));
		assertEquals(DeliveryMode.PERSISTENT, TestUtils.getPropertyValue(jmsTemplate, "deliveryMode"));
		assertEquals(123L, TestUtils.getPropertyValue(jmsTemplate, "timeToLive"));
		assertEquals(12, TestUtils.getPropertyValue(jmsTemplate, "priority"));
		assertEquals(Integer.MAX_VALUE, TestUtils.getPropertyValue(
				TestUtils.getPropertyValue(channel, "dispatcher"), "maxSubscribers", Integer.class).intValue());
	}

	@Test
	public void queueNameChannel() {
		assertEquals(SubscribableJmsChannel.class, queueNameChannel.getClass());
		SubscribableJmsChannel channel = (SubscribableJmsChannel) queueNameChannel;
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		JmsTemplate jmsTemplate = (JmsTemplate) accessor.getPropertyValue("jmsTemplate");
		AbstractMessageListenerContainer container = (AbstractMessageListenerContainer) accessor.getPropertyValue("container");
		assertEquals("test.queue", jmsTemplate.getDefaultDestinationName());
		assertEquals("test.queue", container.getDestinationName());
		assertEquals(1, TestUtils.getPropertyValue(
				TestUtils.getPropertyValue(channel, "dispatcher"), "maxSubscribers", Integer.class).intValue());
	}

	@Test
	public void queueNameWithResolverChannel() {
		assertEquals(SubscribableJmsChannel.class, queueNameWithResolverChannel.getClass());
		SubscribableJmsChannel channel = (SubscribableJmsChannel) queueNameWithResolverChannel;
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		JmsTemplate jmsTemplate = (JmsTemplate) accessor.getPropertyValue("jmsTemplate");
		AbstractMessageListenerContainer container = (AbstractMessageListenerContainer) accessor.getPropertyValue("container");
		assertEquals("foo", jmsTemplate.getDefaultDestinationName());
		assertEquals("foo", container.getDestinationName());
	}

	@Test
	public void topicReferenceChannel() {
		assertEquals(SubscribableJmsChannel.class, topicReferenceChannel.getClass());
		SubscribableJmsChannel channel = (SubscribableJmsChannel) topicReferenceChannel;
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		JmsTemplate jmsTemplate = (JmsTemplate) accessor.getPropertyValue("jmsTemplate");
		AbstractMessageListenerContainer container = (AbstractMessageListenerContainer) accessor.getPropertyValue("container");
		assertEquals(topic, jmsTemplate.getDefaultDestination());
		assertEquals(topic, container.getDestination());
		assertSame(this.messageBuilderFactory,
				TestUtils.getPropertyValue(channel, "container.messageListener.messageBuilderFactory"));
	}


	@Test
	public void topicNameChannel() {
		assertEquals(SubscribableJmsChannel.class, topicNameChannel.getClass());
		SubscribableJmsChannel channel = (SubscribableJmsChannel) topicNameChannel;
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		JmsTemplate jmsTemplate = (JmsTemplate) accessor.getPropertyValue("jmsTemplate");
		AbstractMessageListenerContainer container = (AbstractMessageListenerContainer) accessor.getPropertyValue("container");
		assertEquals("test.topic", jmsTemplate.getDefaultDestinationName());
		assertEquals("test.topic", container.getDestinationName());
		assertTrue(container.isSubscriptionShared());
		assertTrue(container.isSubscriptionDurable());
		assertEquals("subName", container.getSubscriptionName());
	}

	@Test
	public void topicNameWithResolverChannel() {
		assertEquals(SubscribableJmsChannel.class, topicNameWithResolverChannel.getClass());
		SubscribableJmsChannel channel = (SubscribableJmsChannel) topicNameWithResolverChannel;
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		JmsTemplate jmsTemplate = (JmsTemplate) accessor.getPropertyValue("jmsTemplate");
		AbstractMessageListenerContainer container = (AbstractMessageListenerContainer) accessor.getPropertyValue("container");
		assertEquals("foo", jmsTemplate.getDefaultDestinationName());
		assertEquals("foo", container.getDestinationName());
	}

	@Test
	public void channelWithConcurrencySettings() {
		assertEquals(SubscribableJmsChannel.class, channelWithConcurrencySettings.getClass());
		SubscribableJmsChannel channel = (SubscribableJmsChannel) channelWithConcurrencySettings;
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		DefaultMessageListenerContainer container = (DefaultMessageListenerContainer) accessor.getPropertyValue("container");
		assertEquals(11, container.getConcurrentConsumers());
		assertEquals(55, container.getMaxConcurrentConsumers());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void queueChannelWithInterceptors() {
		assertEquals(SubscribableJmsChannel.class, queueChannelWithInterceptors.getClass());
		SubscribableJmsChannel channel = (SubscribableJmsChannel) queueChannelWithInterceptors;
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		List<ChannelInterceptor> interceptors = (List<ChannelInterceptor>) new DirectFieldAccessor(
				accessor.getPropertyValue("interceptors")).getPropertyValue("interceptors");
		assertEquals(1, interceptors.size());
		assertEquals(TestInterceptor.class, interceptors.get(0).getClass());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void topicChannelWithInterceptors() {
		assertEquals(SubscribableJmsChannel.class, topicChannelWithInterceptors.getClass());
		SubscribableJmsChannel channel = (SubscribableJmsChannel) topicChannelWithInterceptors;
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		List<ChannelInterceptor> interceptors = (List<ChannelInterceptor>) new DirectFieldAccessor(
				accessor.getPropertyValue("interceptors")).getPropertyValue("interceptors");
		assertEquals(2, interceptors.size());
		assertEquals(TestInterceptor.class, interceptors.get(0).getClass());
		assertEquals(TestInterceptor.class, interceptors.get(1).getClass());
	}

	@Test
	public void queueReferencePollableChannel() {
		assertEquals(PollableJmsChannel.class, pollableQueueReferenceChannel.getClass());
		PollableJmsChannel channel = (PollableJmsChannel) pollableQueueReferenceChannel;
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		JmsTemplate jmsTemplate = (JmsTemplate) accessor.getPropertyValue("jmsTemplate");
		assertEquals(queue, jmsTemplate.getDefaultDestination());
	}

	@Test
	public void queueNamePollableChannel() {
		assertEquals(PollableJmsChannel.class, pollableQueueNameChannel.getClass());
		PollableJmsChannel channel = (PollableJmsChannel) pollableQueueNameChannel;
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		JmsTemplate jmsTemplate = (JmsTemplate) accessor.getPropertyValue("jmsTemplate");
		assertEquals("foo", jmsTemplate.getDefaultDestinationName());
	}

	@Test
	public void selectorPollableChannel() {
		assertEquals(PollableJmsChannel.class, pollableWithSelectorChannel.getClass());
		PollableJmsChannel channel = (PollableJmsChannel) pollableWithSelectorChannel;
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		JmsTemplate jmsTemplate = (JmsTemplate) accessor.getPropertyValue("jmsTemplate");
		assertEquals(queue, jmsTemplate.getDefaultDestination());
		assertEquals("foo='bar'", accessor.getPropertyValue("messageSelector"));
	}

	@Test
	public void withPlaceholders() {
		DefaultMessageListenerContainer container = TestUtils.getPropertyValue(withPlaceholders, "container", DefaultMessageListenerContainer.class);
		assertEquals("queue://test.queue", container.getDestination().toString());
		assertEquals(5, container.getConcurrentConsumers());
		assertEquals(25, container.getMaxConcurrentConsumers());
	}

	@Test
	public void withDefaultContainer() {
		DefaultMessageListenerContainer container = TestUtils.getPropertyValue(
				withDefaultContainer, "container",
				DefaultMessageListenerContainer.class);
		assertEquals("default.container.queue", container.getDestinationName());
	}

	@Test
	public void withExplicitDefaultContainer() {
		DefaultMessageListenerContainer container = TestUtils.getPropertyValue(
				withExplicitDefaultContainer, "container",
				DefaultMessageListenerContainer.class);
		assertEquals("explicit.default.container.queue", container.getDestinationName());
	}

	@Test
	public void withSimpleContainer() {
		SimpleMessageListenerContainer container = TestUtils.getPropertyValue(
				withSimpleContainer, "container",
				SimpleMessageListenerContainer.class);
		assertEquals("simple.container.queue", container.getDestinationName());
	}

	@Test
	public void withContainerClass() {
		CustomTestMessageListenerContainer container = TestUtils.getPropertyValue(
				withContainerClass, "container",
				CustomTestMessageListenerContainer.class);
		assertEquals("custom.container.queue", container.getDestinationName());
	}

	@Test
	public void withContainerClassSpEL() {
		CustomTestMessageListenerContainer container = TestUtils.getPropertyValue(
				withContainerClassSpEL, "container",
				CustomTestMessageListenerContainer.class);
		assertEquals("custom.container.queue", container.getDestinationName());
	}


	static class TestDestinationResolver implements DestinationResolver {

		@Autowired
		private Queue queue;

		@Autowired
		private Topic topic;

		@Override
		public Destination resolveDestinationName(Session session, String destinationName, boolean pubSubDomain)
				throws JMSException {
			if (!"foo".equals(destinationName)) {
				throw new IllegalStateException("only destination name of 'foo' is supported");
			}
			return pubSubDomain ? topic : queue;
		}
	}


	static class TestInterceptor implements ChannelInterceptor {
	}

	static class CustomTestMessageListenerContainer extends DefaultMessageListenerContainer {

	}

}
