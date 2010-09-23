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

package org.springframework.integration.jms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.jms.Destination;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.jms.config.JmsChannelFactoryBean;
import org.springframework.integration.message.GenericMessage;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

/**
 * @author Mark Fisher
 */
public class SubscribableJmsChannelTests {

	private static final int TIMEOUT = 30000;


	private ActiveMQConnectionFactory connectionFactory;

	private Destination topic;

	private Destination queue;


	@Before
	public void setup() throws Exception {
		this.connectionFactory = new ActiveMQConnectionFactory();
		this.connectionFactory.setBrokerURL("vm://localhost?broker.persistent=false");
		this.topic = new ActiveMQTopic("testTopic");
		this.queue = new ActiveMQQueue("testQueue");
	}

	@Test
	public void queueReference() throws Exception {
		final CountDownLatch latch = new CountDownLatch(2);
		final List<Message<?>> receivedList1 = Collections.synchronizedList( new ArrayList<Message<?>>());
		MessageHandler handler1 = new MessageHandler() {
			public void handleMessage(Message<?> message) {
				receivedList1.add(message);
				latch.countDown();
			}
		};
		final List<Message<?>> receivedList2 = Collections.synchronizedList( new ArrayList<Message<?>>());
		MessageHandler handler2 = new MessageHandler() {
			public void handleMessage(Message<?> message) {
				receivedList2.add(message);
				latch.countDown();
			}
		};
		JmsChannelFactoryBean factoryBean = new JmsChannelFactoryBean(true);
		factoryBean.setConnectionFactory(this.connectionFactory);
		factoryBean.setDestination(this.queue);
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
			public void handleMessage(Message<?> message) {
				receivedList1.add(message);
				latch.countDown();
			}
		};
		final List<Message<?>> receivedList2 = Collections.synchronizedList( new ArrayList<Message<?>>());
		MessageHandler handler2 = new MessageHandler() {
			public void handleMessage(Message<?> message) {
				receivedList2.add(message);
				latch.countDown();
			}
		};
		JmsChannelFactoryBean factoryBean = new JmsChannelFactoryBean(true);
		factoryBean.setConnectionFactory(this.connectionFactory);
		factoryBean.setDestination(this.topic);
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
			public void handleMessage(Message<?> message) {
				receivedList1.add(message);
				latch.countDown();
			}
		};
		final List<Message<?>> receivedList2 = Collections.synchronizedList( new ArrayList<Message<?>>());
		MessageHandler handler2 = new MessageHandler() {
			public void handleMessage(Message<?> message) {
				receivedList2.add(message);
				latch.countDown();
			}
		};
		JmsChannelFactoryBean factoryBean = new JmsChannelFactoryBean(true);
		factoryBean.setConnectionFactory(this.connectionFactory);
		factoryBean.setDestinationName("dynamicQueue");
		factoryBean.setPubSubDomain(false);
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
	public void topicName() throws Exception {
		final CountDownLatch latch = new CountDownLatch(4);
		final List<Message<?>> receivedList1 = Collections.synchronizedList( new ArrayList<Message<?>>());
		MessageHandler handler1 = new MessageHandler() {
			public void handleMessage(Message<?> message) {
				receivedList1.add(message);
				latch.countDown();
			}
		};
		final List<Message<?>> receivedList2 = Collections.synchronizedList( new ArrayList<Message<?>>());
		MessageHandler handler2 = new MessageHandler() {
			public void handleMessage(Message<?> message) {
				receivedList2.add(message);
				latch.countDown();
			}
		};
		
		JmsChannelFactoryBean factoryBean = new JmsChannelFactoryBean(true);
		factoryBean.setConnectionFactory(this.connectionFactory);
		factoryBean.setDestinationName("dynamicTopic");
		factoryBean.setPubSubDomain(true);
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

	@Test //@Ignore
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
