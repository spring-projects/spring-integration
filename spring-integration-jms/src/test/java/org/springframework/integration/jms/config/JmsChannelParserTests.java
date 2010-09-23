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

package org.springframework.integration.jms.config;

import static org.junit.Assert.assertEquals;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.jms.SubscribableJmsChannel;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
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
	private MessageChannel topicNameWithResolverChannel;

	@Autowired
	private MessageChannel channelWithConcurrencySettings;

	@Autowired
	private Topic topic;


	@Test
	public void queueReferenceChannel() {
		assertEquals(SubscribableJmsChannel.class, queueReferenceChannel.getClass());
		SubscribableJmsChannel channel = (SubscribableJmsChannel) queueReferenceChannel;
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		JmsTemplate jmsTemplate = (JmsTemplate) accessor.getPropertyValue("jmsTemplate");
		AbstractMessageListenerContainer container = (AbstractMessageListenerContainer) accessor.getPropertyValue("container");
		assertEquals(queue, jmsTemplate.getDefaultDestination());
		assertEquals(queue, container.getDestination());
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


	static class TestDestinationResolver implements DestinationResolver {

		@Autowired
		private Queue queue;

		@Autowired
		private Topic topic;

		public Destination resolveDestinationName(Session session, String destinationName, boolean pubSubDomain)
				throws JMSException {
			if (!"foo".equals(destinationName)) {
				throw new IllegalStateException("only destination name of 'foo' is supported");
			}
			return pubSubDomain ? topic : queue;
		}
	}

}
