/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.jms.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;

import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.jms.ActiveMQMultiContextTests;
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
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class JmsChannelParserTests extends ActiveMQMultiContextTests {

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
		assertThat(queueReferenceChannel.getClass()).isEqualTo(SubscribableJmsChannel.class);
		SubscribableJmsChannel channel = (SubscribableJmsChannel) queueReferenceChannel;
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		JmsTemplate jmsTemplate = (JmsTemplate) accessor.getPropertyValue("jmsTemplate");
		AbstractMessageListenerContainer container =
				(AbstractMessageListenerContainer) accessor.getPropertyValue("container");
		assertThat(jmsTemplate.getDefaultDestination()).isEqualTo(queue);
		assertThat(container.getDestination()).isEqualTo(queue);
		assertThat(TestUtils.getPropertyValue(jmsTemplate, "explicitQosEnabled")).isEqualTo(true);
		assertThat(TestUtils.getPropertyValue(jmsTemplate, "deliveryMode")).isEqualTo(DeliveryMode.PERSISTENT);
		assertThat(TestUtils.getPropertyValue(jmsTemplate, "timeToLive")).isEqualTo(123L);
		assertThat(TestUtils.getPropertyValue(jmsTemplate, "priority")).isEqualTo(12);
		assertThat(TestUtils.getPropertyValue(
				TestUtils.getPropertyValue(channel, "dispatcher"), "maxSubscribers", Integer.class).intValue())
				.isEqualTo(Integer.MAX_VALUE);
	}

	@Test
	public void queueNameChannel() {
		assertThat(queueNameChannel.getClass()).isEqualTo(SubscribableJmsChannel.class);
		SubscribableJmsChannel channel = (SubscribableJmsChannel) queueNameChannel;
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		JmsTemplate jmsTemplate = (JmsTemplate) accessor.getPropertyValue("jmsTemplate");
		AbstractMessageListenerContainer container = (AbstractMessageListenerContainer) accessor.getPropertyValue(
				"container");
		assertThat(jmsTemplate.getDefaultDestinationName()).isEqualTo("test.queue");
		assertThat(container.getDestinationName()).isEqualTo("test.queue");
		assertThat(TestUtils.getPropertyValue(
				TestUtils.getPropertyValue(channel, "dispatcher"), "maxSubscribers", Integer.class).intValue())
				.isEqualTo(1);
		assertThat(TestUtils.getPropertyValue(container, "taskExecutor.threadNamePrefix"))
				.isEqualTo("queueNameChannel.container-");
	}

	@Test
	public void queueNameWithResolverChannel() {
		assertThat(queueNameWithResolverChannel.getClass()).isEqualTo(SubscribableJmsChannel.class);
		SubscribableJmsChannel channel = (SubscribableJmsChannel) queueNameWithResolverChannel;
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		JmsTemplate jmsTemplate = (JmsTemplate) accessor.getPropertyValue("jmsTemplate");
		AbstractMessageListenerContainer container = (AbstractMessageListenerContainer) accessor.getPropertyValue(
				"container");
		assertThat(jmsTemplate.getDefaultDestinationName()).isEqualTo("foo");
		assertThat(container.getDestinationName()).isEqualTo("foo");
	}

	@Test
	public void topicReferenceChannel() {
		assertThat(topicReferenceChannel.getClass()).isEqualTo(SubscribableJmsChannel.class);
		SubscribableJmsChannel channel = (SubscribableJmsChannel) topicReferenceChannel;
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		JmsTemplate jmsTemplate = (JmsTemplate) accessor.getPropertyValue("jmsTemplate");
		AbstractMessageListenerContainer container = (AbstractMessageListenerContainer) accessor.getPropertyValue(
				"container");
		assertThat(jmsTemplate.getDefaultDestination()).isEqualTo(topic);
		assertThat(container.getDestination()).isEqualTo(topic);
		assertThat(TestUtils.getPropertyValue(channel, "container.messageListener.messageBuilderFactory"))
				.isSameAs(this.messageBuilderFactory);
	}


	@Test
	public void topicNameChannel() {
		assertThat(topicNameChannel.getClass()).isEqualTo(SubscribableJmsChannel.class);
		SubscribableJmsChannel channel = (SubscribableJmsChannel) topicNameChannel;
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		JmsTemplate jmsTemplate = (JmsTemplate) accessor.getPropertyValue("jmsTemplate");
		AbstractMessageListenerContainer container = (AbstractMessageListenerContainer) accessor.getPropertyValue(
				"container");
		assertThat(jmsTemplate.getDefaultDestinationName()).isEqualTo("test.topic");
		assertThat(container.getDestinationName()).isEqualTo("test.topic");
		assertThat(container.isSubscriptionShared()).isTrue();
		assertThat(container.isSubscriptionDurable()).isTrue();
		assertThat(container.getSubscriptionName()).isEqualTo("subName");
	}

	@Test
	public void topicNameWithResolverChannel() {
		assertThat(topicNameWithResolverChannel.getClass()).isEqualTo(SubscribableJmsChannel.class);
		SubscribableJmsChannel channel = (SubscribableJmsChannel) topicNameWithResolverChannel;
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		JmsTemplate jmsTemplate = (JmsTemplate) accessor.getPropertyValue("jmsTemplate");
		AbstractMessageListenerContainer container = (AbstractMessageListenerContainer) accessor.getPropertyValue(
				"container");
		assertThat(jmsTemplate.getDefaultDestinationName()).isEqualTo("foo");
		assertThat(container.getDestinationName()).isEqualTo("foo");
	}

	@Test
	public void channelWithConcurrencySettings() {
		assertThat(channelWithConcurrencySettings.getClass()).isEqualTo(SubscribableJmsChannel.class);
		SubscribableJmsChannel channel = (SubscribableJmsChannel) channelWithConcurrencySettings;
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		DefaultMessageListenerContainer container = (DefaultMessageListenerContainer) accessor.getPropertyValue(
				"container");
		assertThat(container.getConcurrentConsumers()).isEqualTo(11);
		assertThat(container.getMaxConcurrentConsumers()).isEqualTo(55);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void queueChannelWithInterceptors() {
		assertThat(queueChannelWithInterceptors.getClass()).isEqualTo(SubscribableJmsChannel.class);
		SubscribableJmsChannel channel = (SubscribableJmsChannel) queueChannelWithInterceptors;
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		List<ChannelInterceptor> interceptors = (List<ChannelInterceptor>) new DirectFieldAccessor(
				accessor.getPropertyValue("interceptors")).getPropertyValue("interceptors");
		assertThat(interceptors.size()).isEqualTo(1);
		assertThat(interceptors.get(0).getClass()).isEqualTo(TestInterceptor.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void topicChannelWithInterceptors() {
		assertThat(topicChannelWithInterceptors.getClass()).isEqualTo(SubscribableJmsChannel.class);
		SubscribableJmsChannel channel = (SubscribableJmsChannel) topicChannelWithInterceptors;
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		List<ChannelInterceptor> interceptors = (List<ChannelInterceptor>) new DirectFieldAccessor(
				accessor.getPropertyValue("interceptors")).getPropertyValue("interceptors");
		assertThat(interceptors.size()).isEqualTo(2);
		assertThat(interceptors.get(0).getClass()).isEqualTo(TestInterceptor.class);
		assertThat(interceptors.get(1).getClass()).isEqualTo(TestInterceptor.class);
	}

	@Test
	public void queueReferencePollableChannel() {
		assertThat(pollableQueueReferenceChannel.getClass()).isEqualTo(PollableJmsChannel.class);
		PollableJmsChannel channel = (PollableJmsChannel) pollableQueueReferenceChannel;
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		JmsTemplate jmsTemplate = (JmsTemplate) accessor.getPropertyValue("jmsTemplate");
		assertThat(jmsTemplate.getDefaultDestination()).isEqualTo(queue);
	}

	@Test
	public void queueNamePollableChannel() {
		assertThat(pollableQueueNameChannel.getClass()).isEqualTo(PollableJmsChannel.class);
		PollableJmsChannel channel = (PollableJmsChannel) pollableQueueNameChannel;
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		JmsTemplate jmsTemplate = (JmsTemplate) accessor.getPropertyValue("jmsTemplate");
		assertThat(jmsTemplate.getDefaultDestinationName()).isEqualTo("foo");
	}

	@Test
	public void selectorPollableChannel() {
		assertThat(pollableWithSelectorChannel.getClass()).isEqualTo(PollableJmsChannel.class);
		PollableJmsChannel channel = (PollableJmsChannel) pollableWithSelectorChannel;
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		JmsTemplate jmsTemplate = (JmsTemplate) accessor.getPropertyValue("jmsTemplate");
		assertThat(jmsTemplate.getDefaultDestination()).isEqualTo(queue);
		assertThat(accessor.getPropertyValue("messageSelector")).isEqualTo("foo='bar'");
	}

	@Test
	public void withPlaceholders() {
		DefaultMessageListenerContainer container = TestUtils.getPropertyValue(withPlaceholders, "container",
				DefaultMessageListenerContainer.class);
		assertThat(container.getDestination().toString()).isEqualTo("queue://test.queue");
		assertThat(container.getConcurrentConsumers()).isEqualTo(5);
		assertThat(container.getMaxConcurrentConsumers()).isEqualTo(25);
	}

	@Test
	public void withDefaultContainer() {
		DefaultMessageListenerContainer container = TestUtils.getPropertyValue(
				withDefaultContainer, "container",
				DefaultMessageListenerContainer.class);
		assertThat(container.getDestinationName()).isEqualTo("default.container.queue");
	}

	@Test
	public void withExplicitDefaultContainer() {
		DefaultMessageListenerContainer container = TestUtils.getPropertyValue(
				withExplicitDefaultContainer, "container",
				DefaultMessageListenerContainer.class);
		assertThat(container.getDestinationName()).isEqualTo("explicit.default.container.queue");
	}

	@Test
	public void withSimpleContainer() {
		SimpleMessageListenerContainer container = TestUtils.getPropertyValue(
				withSimpleContainer, "container",
				SimpleMessageListenerContainer.class);
		assertThat(container.getDestinationName()).isEqualTo("simple.container.queue");
	}

	@Test
	public void withContainerClass() {
		CustomTestMessageListenerContainer container = TestUtils.getPropertyValue(
				withContainerClass, "container",
				CustomTestMessageListenerContainer.class);
		assertThat(container.getDestinationName()).isEqualTo("custom.container.queue");
	}

	@Test
	public void withContainerClassSpEL() {
		CustomTestMessageListenerContainer container = TestUtils.getPropertyValue(
				withContainerClassSpEL, "container",
				CustomTestMessageListenerContainer.class);
		assertThat(container.getDestinationName()).isEqualTo("custom.container.queue");
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
