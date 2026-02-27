/*
 * Copyright 2002-present the original author or authors.
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

import java.util.List;

import jakarta.jms.DeliveryMode;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.Topic;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.jms.ActiveMQMultiContextTests;
import org.springframework.integration.jms.channel.PollableJmsChannel;
import org.springframework.integration.jms.channel.SubscribableJmsChannel;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatObject;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
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
		assertThat(this.queueReferenceChannel).isInstanceOf(SubscribableJmsChannel.class);
		JmsTemplate jmsTemplate = TestUtils.getPropertyValue(this.queueReferenceChannel, "jmsTemplate");
		AbstractMessageListenerContainer container = TestUtils.getPropertyValue(this.queueReferenceChannel, "container");
		assertThat(jmsTemplate.getDefaultDestination()).isEqualTo(this.queue);
		assertThat(container.getDestination()).isEqualTo(this.queue);
		assertThat(TestUtils.<Boolean>getPropertyValue(jmsTemplate, "explicitQosEnabled")).isEqualTo(true);
		assertThat(TestUtils.<Integer>getPropertyValue(jmsTemplate, "deliveryMode")).isEqualTo(DeliveryMode.PERSISTENT);
		assertThat(TestUtils.<Long>getPropertyValue(jmsTemplate, "timeToLive")).isEqualTo(123L);
		assertThat(TestUtils.<Integer>getPropertyValue(jmsTemplate, "priority")).isEqualTo(12);
		assertThat(
				TestUtils.<Integer>getPropertyValue(this.queueReferenceChannel, "dispatcher.maxSubscribers").intValue())
				.isEqualTo(Integer.MAX_VALUE);
		assertThatObject(TestUtils.getPropertyValue(this.queueReferenceChannel, "messageBuilderFactory"))
				.isSameAs(this.messageBuilderFactory);
	}

	@Test
	public void queueNameChannel() {
		assertThat(this.queueNameChannel).isInstanceOf(SubscribableJmsChannel.class);
		JmsTemplate jmsTemplate = TestUtils.getPropertyValue(this.queueNameChannel, "jmsTemplate");
		AbstractMessageListenerContainer container = TestUtils.getPropertyValue(this.queueNameChannel, "container");
		assertThat(jmsTemplate.getDefaultDestinationName()).isEqualTo("test.queue");
		assertThat(container.getDestinationName()).isEqualTo("test.queue");
		assertThat(TestUtils.<Integer>getPropertyValue(this.queueNameChannel, "dispatcher.maxSubscribers").intValue())
				.isEqualTo(1);
		assertThat(TestUtils.<String>getPropertyValue(container, "taskExecutor.threadNamePrefix"))
				.isEqualTo("queueNameChannel.container-");
		assertThatObject(TestUtils.getPropertyValue(this.queueNameChannel, "messageBuilderFactory"))
				.isSameAs(this.messageBuilderFactory);
	}

	@Test
	public void queueNameWithResolverChannel() {
		assertThat(this.queueNameWithResolverChannel).isInstanceOf(SubscribableJmsChannel.class);
		JmsTemplate jmsTemplate = TestUtils.getPropertyValue(this.queueNameWithResolverChannel, "jmsTemplate");
		AbstractMessageListenerContainer container =
				TestUtils.getPropertyValue(this.queueNameWithResolverChannel, "container");
		assertThat(jmsTemplate.getDefaultDestinationName()).isEqualTo("foo");
		assertThat(container.getDestinationName()).isEqualTo("foo");
		assertThatObject(TestUtils.getPropertyValue(this.queueNameWithResolverChannel, "messageBuilderFactory"))
				.isSameAs(this.messageBuilderFactory);
	}

	@Test
	public void topicReferenceChannel() {
		assertThat(this.topicReferenceChannel).isInstanceOf(SubscribableJmsChannel.class);
		JmsTemplate jmsTemplate = TestUtils.getPropertyValue(this.topicReferenceChannel, "jmsTemplate");
		AbstractMessageListenerContainer container = TestUtils.getPropertyValue(this.topicReferenceChannel, "container");
		assertThat(jmsTemplate.getDefaultDestination()).isEqualTo(this.topic);
		assertThat(container.getDestination()).isEqualTo(this.topic);
		assertThatObject(TestUtils.getPropertyValue(this.topicReferenceChannel, "messageBuilderFactory"))
				.isSameAs(this.messageBuilderFactory);
	}

	@Test
	public void topicNameChannel() {
		assertThat(this.topicNameChannel).isInstanceOf(SubscribableJmsChannel.class);
		JmsTemplate jmsTemplate = TestUtils.getPropertyValue(this.topicNameChannel, "jmsTemplate");
		AbstractMessageListenerContainer container = TestUtils.getPropertyValue(this.topicNameChannel, "container");
		assertThat(jmsTemplate.getDefaultDestinationName()).isEqualTo("test.topic");
		assertThat(container.getDestinationName()).isEqualTo("test.topic");
		assertThat(container.isSubscriptionShared()).isTrue();
		assertThat(container.isSubscriptionDurable()).isTrue();
		assertThat(container.getSubscriptionName()).isEqualTo("subName");
	}

	@Test
	public void topicNameWithResolverChannel() {
		assertThat(this.topicNameWithResolverChannel).isInstanceOf(SubscribableJmsChannel.class);
		JmsTemplate jmsTemplate = TestUtils.getPropertyValue(this.topicNameWithResolverChannel, "jmsTemplate");
		AbstractMessageListenerContainer container =
				TestUtils.getPropertyValue(this.topicNameWithResolverChannel, "container");
		assertThat(jmsTemplate.getDefaultDestinationName()).isEqualTo("foo");
		assertThat(container.getDestinationName()).isEqualTo("foo");
	}

	@Test
	public void channelWithConcurrencySettings() {
		assertThat(this.channelWithConcurrencySettings).isInstanceOf(SubscribableJmsChannel.class);
		DefaultMessageListenerContainer container =
				TestUtils.getPropertyValue(this.channelWithConcurrencySettings, "container");
		assertThat(container.getConcurrentConsumers()).isEqualTo(11);
		assertThat(container.getMaxConcurrentConsumers()).isEqualTo(55);
	}

	@Test
	public void queueChannelWithInterceptors() {
		assertThat(this.queueChannelWithInterceptors).isInstanceOf(SubscribableJmsChannel.class);
		List<ChannelInterceptor> interceptors =
				TestUtils.getPropertyValue(this.queueChannelWithInterceptors, "interceptors.interceptors");
		assertThat(interceptors)
				.hasSize(1)
				.first()
				.isInstanceOf(TestInterceptor.class);
	}

	@Test
	public void topicChannelWithInterceptors() {
		assertThat(this.topicChannelWithInterceptors).isInstanceOf(SubscribableJmsChannel.class);
		List<ChannelInterceptor> interceptors =
				TestUtils.getPropertyValue(this.topicChannelWithInterceptors, "interceptors.interceptors");
		assertThat(interceptors).hasSize(2);
		assertThat(interceptors).first().isInstanceOf(TestInterceptor.class);
		assertThat(interceptors.get(1)).isInstanceOf(TestInterceptor.class);
	}

	@Test
	public void queueReferencePollableChannel() {
		assertThat(this.pollableQueueReferenceChannel).isInstanceOf(PollableJmsChannel.class);
		JmsTemplate jmsTemplate = TestUtils.getPropertyValue(this.pollableQueueReferenceChannel, "jmsTemplate");
		assertThat(jmsTemplate.getDefaultDestination()).isEqualTo(this.queue);
	}

	@Test
	public void queueNamePollableChannel() {
		assertThat(this.pollableQueueNameChannel).isInstanceOf(PollableJmsChannel.class);
		JmsTemplate jmsTemplate = TestUtils.getPropertyValue(this.pollableQueueNameChannel, "jmsTemplate");
		assertThat(jmsTemplate.getDefaultDestinationName()).isEqualTo("foo");
	}

	@Test
	public void selectorPollableChannel() {
		assertThat(this.pollableWithSelectorChannel).isInstanceOf(PollableJmsChannel.class);
		JmsTemplate jmsTemplate = TestUtils.getPropertyValue(this.pollableWithSelectorChannel, "jmsTemplate");
		assertThat(jmsTemplate.getDefaultDestination()).isEqualTo(queue);
		assertThatObject(TestUtils.getPropertyValue(this.pollableWithSelectorChannel, "messageSelector"))
				.isEqualTo("foo='bar'");
	}

	@Test
	public void withPlaceholders() {
		DefaultMessageListenerContainer container = TestUtils.getPropertyValue(withPlaceholders, "container");
		assertThat(container.getDestination().toString()).isEqualTo("ActiveMQQueue[test.queue]");
		assertThat(container.getConcurrentConsumers()).isEqualTo(5);
		assertThat(container.getMaxConcurrentConsumers()).isEqualTo(25);
	}

	@Test
	public void withDefaultContainer() {
		DefaultMessageListenerContainer container = TestUtils.getPropertyValue(withDefaultContainer, "container");
		assertThat(container.getDestinationName()).isEqualTo("default.container.queue");
	}

	@Test
	public void withExplicitDefaultContainer() {
		DefaultMessageListenerContainer container =
				TestUtils.getPropertyValue(withExplicitDefaultContainer, "container");
		assertThat(container.getDestinationName()).isEqualTo("explicit.default.container.queue");
	}

	@Test
	public void withSimpleContainer() {
		SimpleMessageListenerContainer container = TestUtils.getPropertyValue(withSimpleContainer, "container");
		assertThat(container.getDestinationName()).isEqualTo("simple.container.queue");
	}

	@Test
	public void withContainerClass() {
		CustomTestMessageListenerContainer container = TestUtils.getPropertyValue(withContainerClass, "container");
		assertThat(container.getDestinationName()).isEqualTo("custom.container.queue");
	}

	@Test
	public void withContainerClassSpEL() {
		CustomTestMessageListenerContainer container = TestUtils.getPropertyValue(withContainerClassSpEL, "container");
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
