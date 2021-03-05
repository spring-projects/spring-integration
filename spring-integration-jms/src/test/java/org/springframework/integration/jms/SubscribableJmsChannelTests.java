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

package org.springframework.integration.jms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.jms.Destination;
import javax.jms.MessageListener;

import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.commons.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.integration.jms.config.JmsChannelFactoryBean;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @since 2.0
 */
public class SubscribableJmsChannelTests extends ActiveMQMultiContextTests {

	private static final int TIMEOUT = 30000;

	private Destination topic;

	private Destination queue;

	@BeforeEach
	public void setup() {
		this.topic = new ActiveMQTopic("testTopic");
		this.queue = new ActiveMQQueue("testQueue");
	}

	@Test
	public void queueReference() throws Exception {
		final CountDownLatch latch = new CountDownLatch(2);
		final List<Message<?>> receivedList1 = Collections.synchronizedList(new ArrayList<>());
		MessageHandler handler1 = message -> {
			receivedList1.add(message);
			latch.countDown();
		};
		final List<Message<?>> receivedList2 = Collections.synchronizedList(new ArrayList<>());
		MessageHandler handler2 = message -> {
			receivedList2.add(message);
			latch.countDown();
		};
		JmsChannelFactoryBean factoryBean = new JmsChannelFactoryBean(true);
		factoryBean.setConnectionFactory(connectionFactory);
		factoryBean.setDestination(this.queue);
		factoryBean.setBeanFactory(mock(BeanFactory.class));
		factoryBean.afterPropertiesSet();
		SubscribableJmsChannel channel = (SubscribableJmsChannel) factoryBean.getObject();
		channel.afterPropertiesSet();
		channel.start();
		channel.subscribe(handler1);
		channel.subscribe(handler2);
		channel.send(new GenericMessage<>("foo"));
		channel.send(new GenericMessage<>("bar"));
		latch.await(TIMEOUT, TimeUnit.MILLISECONDS);
		assertThat(receivedList1.size()).isEqualTo(1);
		assertThat(receivedList1.get(0)).isNotNull();
		assertThat(receivedList1.get(0).getPayload()).isEqualTo("foo");
		assertThat(receivedList2.size()).isEqualTo(1);
		assertThat(receivedList2.get(0)).isNotNull();
		assertThat(receivedList2.get(0).getPayload()).isEqualTo("bar");
		channel.stop();
	}

	@Test
	public void topicReference() throws Exception {
		final CountDownLatch latch = new CountDownLatch(4);
		final List<Message<?>> receivedList1 = Collections.synchronizedList(new ArrayList<>());
		MessageHandler handler1 = message -> {
			receivedList1.add(message);
			latch.countDown();
		};
		final List<Message<?>> receivedList2 = Collections.synchronizedList(new ArrayList<>());
		MessageHandler handler2 = message -> {
			receivedList2.add(message);
			latch.countDown();
		};
		JmsChannelFactoryBean factoryBean = new JmsChannelFactoryBean(true);
		factoryBean.setConnectionFactory(connectionFactory);
		factoryBean.setDestination(this.topic);
		factoryBean.setBeanFactory(mock(BeanFactory.class));
		factoryBean.afterPropertiesSet();
		SubscribableJmsChannel channel = (SubscribableJmsChannel) factoryBean.getObject();
		channel.afterPropertiesSet();
		channel.subscribe(handler1);
		channel.subscribe(handler2);
		channel.start();
		if (!waitUntilRegisteredWithDestination(channel, 10000)) {
			fail("Listener failed to subscribe to topic");
		}
		channel.send(new GenericMessage<>("foo"));
		channel.send(new GenericMessage<>("bar"));
		latch.await(TIMEOUT, TimeUnit.MILLISECONDS);
		assertThat(receivedList1.size()).isEqualTo(2);
		assertThat(receivedList1.get(0).getPayload()).isEqualTo("foo");
		assertThat(receivedList1.get(1).getPayload()).isEqualTo("bar");
		assertThat(receivedList2.size()).isEqualTo(2);
		assertThat(receivedList2.get(0).getPayload()).isEqualTo("foo");
		assertThat(receivedList2.get(1).getPayload()).isEqualTo("bar");
		channel.stop();
	}

	@Test
	public void queueName() throws Exception {
		final CountDownLatch latch = new CountDownLatch(2);
		final List<Message<?>> receivedList1 = Collections.synchronizedList(new ArrayList<>());
		MessageHandler handler1 = message -> {
			receivedList1.add(message);
			latch.countDown();
		};
		final List<Message<?>> receivedList2 = Collections.synchronizedList(new ArrayList<>());
		MessageHandler handler2 = message -> {
			receivedList2.add(message);
			latch.countDown();
		};
		JmsChannelFactoryBean factoryBean = new JmsChannelFactoryBean(true);
		factoryBean.setConnectionFactory(connectionFactory);
		factoryBean.setDestinationName("dynamicQueue");
		factoryBean.setPubSubDomain(false);
		factoryBean.setBeanFactory(mock(BeanFactory.class));
		factoryBean.afterPropertiesSet();

		SubscribableJmsChannel channel = (SubscribableJmsChannel) factoryBean.getObject();
		channel.afterPropertiesSet();
		channel.start();
		channel.subscribe(handler1);
		channel.subscribe(handler2);
		channel.send(new GenericMessage<>("foo"));
		channel.send(new GenericMessage<>("bar"));

		assertThat(latch.await(TIMEOUT, TimeUnit.MILLISECONDS))
				.as("Countdown latch should have counted down to 0 but was "
						+ latch.getCount()).isTrue();

		assertThat(receivedList1.size()).isEqualTo(1);
		assertThat(receivedList1.get(0)).isNotNull();
		assertThat(receivedList1.get(0).getPayload()).isEqualTo("foo");
		assertThat(receivedList2.size()).isEqualTo(1);
		assertThat(receivedList2.get(0)).isNotNull();
		assertThat(receivedList2.get(0).getPayload()).isEqualTo("bar");
		channel.stop();
	}

	@Test
	public void topicName() throws Exception {
		final CountDownLatch latch = new CountDownLatch(4);
		final List<Message<?>> receivedList1 = Collections.synchronizedList(new ArrayList<>());
		MessageHandler handler1 = message -> {
			receivedList1.add(message);
			latch.countDown();
		};
		final List<Message<?>> receivedList2 = Collections.synchronizedList(new ArrayList<>());
		MessageHandler handler2 = message -> {
			receivedList2.add(message);
			latch.countDown();
		};

		JmsChannelFactoryBean factoryBean = new JmsChannelFactoryBean(true);
		factoryBean.setConnectionFactory(connectionFactory);
		factoryBean.setDestinationName("dynamicTopic");
		factoryBean.setPubSubDomain(true);
		factoryBean.setBeanFactory(mock(BeanFactory.class));
		factoryBean.afterPropertiesSet();
		SubscribableJmsChannel channel = (SubscribableJmsChannel) factoryBean.getObject();
		channel.afterPropertiesSet();
		channel.start();
		if (!waitUntilRegisteredWithDestination(channel, 10000)) {
			fail("Listener failed to subscribe to topic");
		}
		channel.subscribe(handler1);
		channel.subscribe(handler2);
		channel.send(new GenericMessage<>("foo"));
		channel.send(new GenericMessage<>("bar"));
		latch.await(TIMEOUT, TimeUnit.MILLISECONDS);
		assertThat(receivedList1.size()).isEqualTo(2);
		assertThat(receivedList1.get(0).getPayload()).isEqualTo("foo");
		assertThat(receivedList1.get(1).getPayload()).isEqualTo("bar");
		assertThat(receivedList2.size()).isEqualTo(2);
		assertThat(receivedList2.get(0).getPayload()).isEqualTo("foo");
		assertThat(receivedList2.get(1).getPayload()).isEqualTo("bar");
		channel.stop();
	}

	@Test
	public void contextManagesLifecycle() {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(JmsChannelFactoryBean.class);
		builder.addConstructorArgValue(true);
		builder.addPropertyValue("connectionFactory", connectionFactory);
		builder.addPropertyValue("destinationName", "dynamicQueue");
		builder.addPropertyValue("pubSubDomain", false);
		StaticApplicationContext context = new StaticApplicationContext();
		context.registerBeanDefinition("channel", builder.getBeanDefinition());
		SubscribableJmsChannel channel = context.getBean("channel", SubscribableJmsChannel.class);
		assertThat(channel.isRunning()).isFalse();
		context.refresh();
		assertThat(channel.isRunning()).isTrue();
		context.stop();
		assertThat(channel.isRunning()).isFalse();
		context.close();
	}

	@Test
	public void dispatcherHasNoSubscribersQueue() throws Exception {
		JmsChannelFactoryBean factoryBean = new JmsChannelFactoryBean(true);
		factoryBean.setConnectionFactory(connectionFactory);
		factoryBean.setDestinationName("noSubscribersQueue");
		factoryBean.setBeanName("noSubscribersChannel");
		factoryBean.setBeanFactory(mock(BeanFactory.class));
		factoryBean.afterPropertiesSet();
		SubscribableJmsChannel channel = (SubscribableJmsChannel) factoryBean.getObject();
		channel.afterPropertiesSet();

		AbstractMessageListenerContainer container = TestUtils
				.getPropertyValue(channel, "container",
						AbstractMessageListenerContainer.class);
		MessageListener listener = (MessageListener) container.getMessageListener();
		try {
			listener.onMessage(new StubTextMessage("Hello, world!"));
			fail("Exception expected");
		}
		catch (MessageDeliveryException e) {
			assertThat(e.getMessage())
					.contains("Dispatcher has no subscribers for jms-channel 'noSubscribersChannel'.");
		}
	}

	@Test
	public void dispatcherHasNoSubscribersTopic() throws Exception {
		JmsChannelFactoryBean factoryBean = new JmsChannelFactoryBean(true);
		factoryBean.setConnectionFactory(connectionFactory);
		factoryBean.setDestinationName("noSubscribersTopic");
		factoryBean.setBeanName("noSubscribersChannel");
		factoryBean.setPubSubDomain(true);
		factoryBean.setBeanFactory(mock(BeanFactory.class));
		factoryBean.afterPropertiesSet();
		SubscribableJmsChannel channel = (SubscribableJmsChannel) factoryBean.getObject();
		channel.afterPropertiesSet();

		AbstractMessageListenerContainer container = TestUtils
				.getPropertyValue(channel, "container",
						AbstractMessageListenerContainer.class);
		MessageListener listener = (MessageListener) container.getMessageListener();
		List<String> logList = insertMockLoggerInListener(channel);
		listener.onMessage(new StubTextMessage("Hello, world!"));
		verifyLogReceived(logList);
	}

	private List<String> insertMockLoggerInListener(
			SubscribableJmsChannel channel) {
		AbstractMessageListenerContainer container = TestUtils.getPropertyValue(
				channel, "container", AbstractMessageListenerContainer.class);
		Log logger = mock(Log.class);
		final ArrayList<String> logList = new ArrayList<>();
		doAnswer(invocation -> {
			String message = invocation.getArgument(0);
			if (message.startsWith("Dispatcher has no subscribers")) {
				logList.add(message);
			}
			return null;
		}).when(logger).warn(anyString(), any(Exception.class));
		when(logger.isWarnEnabled()).thenReturn(true);
		Object listener = container.getMessageListener();
		DirectFieldAccessor dfa = new DirectFieldAccessor(listener);
		dfa.setPropertyValue("logger", logger);
		return logList;
	}

	private void verifyLogReceived(final List<String> logList) {
		assertThat(logList.size() > 0).as("Failed to get expected exception").isTrue();
		boolean expectedExceptionFound = false;
		while (logList.size() > 0) {
			String message = logList.remove(0);
			assertThat(message).as("Failed to get expected exception").isNotNull();
			if (message.startsWith("Dispatcher has no subscribers")) {
				expectedExceptionFound = true;
				assertThat(message).isEqualTo("Dispatcher has no subscribers for jms-channel 'noSubscribersChannel'.");
				break;
			}
		}
		assertThat(expectedExceptionFound).as("Failed to get expected exception").isTrue();
	}

	/**
	 * Blocks until the listener container has subscribed; if the container does not support
	 * this test, or the caching mode is incompatible, true is returned. Otherwise blocks
	 * until timeout milliseconds have passed, or the consumer has registered.
	 * @see DefaultMessageListenerContainer#isRegisteredWithDestination()
	 * @param timeout Timeout in milliseconds.
	 * @return True if a subscriber has connected or the container/attributes does not support
	 * the test. False if a valid container does not have a registered consumer within
	 * timeout milliseconds.
	 */
	private static boolean waitUntilRegisteredWithDestination(SubscribableJmsChannel channel, long timeout) {
		AbstractMessageListenerContainer container =
				(AbstractMessageListenerContainer) new DirectFieldAccessor(channel).getPropertyValue("container");
		if (container instanceof DefaultMessageListenerContainer) {
			DefaultMessageListenerContainer listenerContainer =
					(DefaultMessageListenerContainer) container;
			if (listenerContainer.getCacheLevel() != DefaultMessageListenerContainer.CACHE_CONSUMER) {
				return true;
			}
			while (timeout > 0) {
				if (listenerContainer.isRegisteredWithDestination()) {
					return true;
				}
				try {
					Thread.sleep(100);
				}
				catch (InterruptedException e) {

				}
				timeout -= 100;
			}
			return false;
		}
		return true;
	}

}
