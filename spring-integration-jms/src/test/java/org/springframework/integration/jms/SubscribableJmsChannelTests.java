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

package org.springframework.integration.jms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
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

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.commons.logging.Log;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.integration.jms.config.JmsChannelFactoryBean;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jms.connection.CachingConnectionFactory;
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
 * @since 2.0
 */
public class SubscribableJmsChannelTests {

	private static final int TIMEOUT = 30000;

	private CachingConnectionFactory connectionFactory;

	private Destination topic;

	private Destination queue;

	@Before
	public void setup() throws Exception {
		ActiveMQConnectionFactory targetConnectionFactory = new ActiveMQConnectionFactory();
		this.connectionFactory = new CachingConnectionFactory(targetConnectionFactory);
		targetConnectionFactory.setBrokerURL("vm://localhost?broker.persistent=false");
		this.topic = new ActiveMQTopic("testTopic");
		this.queue = new ActiveMQQueue("testQueue");
	}

	@After
	public void tearDown() throws Exception {
		this.connectionFactory.resetConnection();
	}

	@Test
	public void queueReference() throws Exception {
		final CountDownLatch latch = new CountDownLatch(2);
		final List<Message<?>> receivedList1 = Collections.synchronizedList( new ArrayList<Message<?>>());
		MessageHandler handler1 = new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) {
				receivedList1.add(message);
				latch.countDown();
			}
		};
		final List<Message<?>> receivedList2 = Collections.synchronizedList( new ArrayList<Message<?>>());
		MessageHandler handler2 = new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) {
				receivedList2.add(message);
				latch.countDown();
			}
		};
		JmsChannelFactoryBean factoryBean = new JmsChannelFactoryBean(true);
		factoryBean.setConnectionFactory(this.connectionFactory);
		factoryBean.setDestination(this.queue);
		factoryBean.setBeanFactory(mock(BeanFactory.class));
		factoryBean.afterPropertiesSet();
		SubscribableJmsChannel channel = (SubscribableJmsChannel) factoryBean.getObject();
		channel.afterPropertiesSet();
		channel.start();
		channel.subscribe(handler1);
		channel.subscribe(handler2);
		channel.send(new GenericMessage<String>("foo"));
		channel.send(new GenericMessage<String>("bar"));
		latch.await(TIMEOUT, TimeUnit.MILLISECONDS);
		assertEquals(1, receivedList1.size());
		assertNotNull(receivedList1.get(0));
		assertEquals("foo", receivedList1.get(0).getPayload());
		assertEquals(1, receivedList2.size());
		assertNotNull(receivedList2.get(0));
		assertEquals("bar", receivedList2.get(0).getPayload());
		channel.stop();
	}

	@Test
	public void topicReference() throws Exception {
		final CountDownLatch latch = new CountDownLatch(4);
		final List<Message<?>> receivedList1 = Collections.synchronizedList( new ArrayList<Message<?>>());
		MessageHandler handler1 = new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) {
				receivedList1.add(message);
				latch.countDown();
			}
		};
		final List<Message<?>> receivedList2 = Collections.synchronizedList( new ArrayList<Message<?>>());
		MessageHandler handler2 = new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) {
				receivedList2.add(message);
				latch.countDown();
			}
		};
		JmsChannelFactoryBean factoryBean = new JmsChannelFactoryBean(true);
		factoryBean.setConnectionFactory(this.connectionFactory);
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
		channel.send(new GenericMessage<String>("foo"));
		channel.send(new GenericMessage<String>("bar"));
		latch.await(TIMEOUT, TimeUnit.MILLISECONDS);
		assertEquals(2, receivedList1.size());
		assertEquals("foo", receivedList1.get(0).getPayload());
		assertEquals("bar", receivedList1.get(1).getPayload());
		assertEquals(2, receivedList2.size());
		assertEquals("foo", receivedList2.get(0).getPayload());
		assertEquals("bar", receivedList2.get(1).getPayload());
		channel.stop();
	}

	@Test
	public void queueName() throws Exception {
		final CountDownLatch latch = new CountDownLatch(2);
		final List<Message<?>> receivedList1 = Collections.synchronizedList( new ArrayList<Message<?>>());
		MessageHandler handler1 = new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) {
				receivedList1.add(message);
				latch.countDown();
			}
		};
		final List<Message<?>> receivedList2 = Collections.synchronizedList( new ArrayList<Message<?>>());
		MessageHandler handler2 = new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) {
				receivedList2.add(message);
				latch.countDown();
			}
		};
		JmsChannelFactoryBean factoryBean = new JmsChannelFactoryBean(true);
		factoryBean.setConnectionFactory(this.connectionFactory);
		factoryBean.setDestinationName("dynamicQueue");
		factoryBean.setPubSubDomain(false);
		factoryBean.setBeanFactory(mock(BeanFactory.class));
		factoryBean.afterPropertiesSet();

		SubscribableJmsChannel channel = (SubscribableJmsChannel) factoryBean.getObject();
		channel.afterPropertiesSet();
		channel.start();
		channel.subscribe(handler1);
		channel.subscribe(handler2);
		channel.send(new GenericMessage<String>("foo"));
		channel.send(new GenericMessage<String>("bar"));

		assertTrue("Countdown latch should have counted down to 0 but was "
				+ latch.getCount(), latch.await(TIMEOUT, TimeUnit.MILLISECONDS));

		assertEquals(1, receivedList1.size());
		assertNotNull(receivedList1.get(0));
		assertEquals("foo", receivedList1.get(0).getPayload());
		assertEquals(1, receivedList2.size());
		assertNotNull(receivedList2.get(0));
		assertEquals("bar", receivedList2.get(0).getPayload());
		channel.stop();
	}

	@Test
	public void topicName() throws Exception {
		final CountDownLatch latch = new CountDownLatch(4);
		final List<Message<?>> receivedList1 = Collections.synchronizedList( new ArrayList<Message<?>>());
		MessageHandler handler1 = new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) {
				receivedList1.add(message);
				latch.countDown();
			}
		};
		final List<Message<?>> receivedList2 = Collections.synchronizedList( new ArrayList<Message<?>>());
		MessageHandler handler2 = new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) {
				receivedList2.add(message);
				latch.countDown();
			}
		};

		JmsChannelFactoryBean factoryBean = new JmsChannelFactoryBean(true);
		factoryBean.setConnectionFactory(this.connectionFactory);
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
		channel.send(new GenericMessage<String>("foo"));
		channel.send(new GenericMessage<String>("bar"));
		latch.await(TIMEOUT, TimeUnit.MILLISECONDS);
		assertEquals(2, receivedList1.size());
		assertEquals("foo", receivedList1.get(0).getPayload());
		assertEquals("bar", receivedList1.get(1).getPayload());
		assertEquals(2, receivedList2.size());
		assertEquals("foo", receivedList2.get(0).getPayload());
		assertEquals("bar", receivedList2.get(1).getPayload());
		channel.stop();
	}

	@Test
	public void contextManagesLifecycle() {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(JmsChannelFactoryBean.class);
		builder.addConstructorArgValue(true);
		builder.addPropertyValue("connectionFactory", this.connectionFactory);
		builder.addPropertyValue("destinationName", "dynamicQueue");
		builder.addPropertyValue("pubSubDomain", false);
		StaticApplicationContext context = new StaticApplicationContext();
		context.registerBeanDefinition("channel", builder.getBeanDefinition());
		SubscribableJmsChannel channel = context.getBean("channel", SubscribableJmsChannel.class);
		assertFalse(channel.isRunning());
		context.refresh();
		assertTrue(channel.isRunning());
		context.stop();
		assertFalse(channel.isRunning());
		context.close();
	}

	@Test
	public void dispatcherHasNoSubscribersQueue() throws Exception {
		JmsChannelFactoryBean factoryBean = new JmsChannelFactoryBean(true);
		factoryBean.setConnectionFactory(this.connectionFactory);
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
			assertEquals("Dispatcher has no subscribers for jms-channel 'noSubscribersChannel'.", e.getMessage());
		}
	}

	@Test
	public void dispatcherHasNoSubscribersTopic() throws Exception {
		JmsChannelFactoryBean factoryBean = new JmsChannelFactoryBean(true);
		factoryBean.setConnectionFactory(this.connectionFactory);
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
		List<String> logList  = insertMockLoggerInListener(channel);
		listener.onMessage(new StubTextMessage("Hello, world!"));
		verifyLogReceived(logList);
	}

	private List<String> insertMockLoggerInListener(
			SubscribableJmsChannel channel) {
		AbstractMessageListenerContainer container = TestUtils.getPropertyValue(
				channel, "container", AbstractMessageListenerContainer.class);
		Log logger = mock(Log.class);
		final ArrayList<String> logList = new ArrayList<String>();
		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation)
					throws Throwable {
				String message = (String) invocation.getArguments()[0];
				if (message.startsWith("Dispatcher has no subscribers")) {
					logList.add(message);
				}
				return null;
			}}).when(logger).warn(anyString(), any(Exception.class));
		when(logger.isWarnEnabled()).thenReturn(true);
		Object listener = container.getMessageListener();
		DirectFieldAccessor dfa = new DirectFieldAccessor(listener);
		dfa.setPropertyValue("logger", logger);
		return logList;
	}

	private void verifyLogReceived(final List<String> logList) {
		assertTrue("Failed to get expected exception", logList.size() > 0);
		boolean expectedExceptionFound = false;
		while (logList.size() > 0) {
			String message = logList.remove(0);
			assertNotNull("Failed to get expected exception", message);
			if (message.startsWith("Dispatcher has no subscribers")) {
				expectedExceptionFound = true;
				assertEquals("Dispatcher has no subscribers for jms-channel 'noSubscribersChannel'.", message);
				break;
			}
		}
		assertTrue("Failed to get expected exception", expectedExceptionFound);
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
				} catch (InterruptedException e) { }
				timeout -= 100;
			}
			return false;
		}
		return true;
	}

}
