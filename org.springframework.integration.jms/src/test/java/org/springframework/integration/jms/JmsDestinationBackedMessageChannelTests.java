/*
 * Copyright 2002-2009 the original author or authors.
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

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageHandler;
import org.springframework.integration.message.StringMessage;

import javax.jms.Destination;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * @author Mark Fisher
 */
public class JmsDestinationBackedMessageChannelTests {

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
		JmsDestinationBackedMessageChannel channel =
				new JmsDestinationBackedMessageChannel(this.connectionFactory, this.queue);
		channel.afterPropertiesSet();
		channel.start();
		channel.subscribe(handler1);
		channel.subscribe(handler2);
		channel.send(new StringMessage("foo"));
		channel.send(new StringMessage("bar"));
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
		JmsDestinationBackedMessageChannel channel =
				new JmsDestinationBackedMessageChannel(this.connectionFactory, this.topic);
		channel.afterPropertiesSet();
		channel.subscribe(handler1);
        channel.subscribe(handler2);
        channel.start();
        Thread.sleep(1000); // allow time for listener to subscribe
        channel.send(new StringMessage("foo"));
		channel.send(new StringMessage("bar"));
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
		JmsDestinationBackedMessageChannel channel =
				new JmsDestinationBackedMessageChannel(this.connectionFactory, "dynamicQueue", false);
		channel.afterPropertiesSet();
		channel.start();
		channel.subscribe(handler1);
		channel.subscribe(handler2);
		channel.send(new StringMessage("foo"));
		channel.send(new StringMessage("bar"));
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
		JmsDestinationBackedMessageChannel channel =
				new JmsDestinationBackedMessageChannel(this.connectionFactory, "dynamicTopic", true);
		channel.afterPropertiesSet();
		channel.start();
		channel.subscribe(handler1);
		channel.subscribe(handler2);
		channel.send(new StringMessage("foo"));
		channel.send(new StringMessage("bar"));
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
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(JmsDestinationBackedMessageChannel.class);
		builder.addConstructorArgValue(this.connectionFactory);
		builder.addConstructorArgValue("dynamicQueue");
		builder.addConstructorArgValue(false);
		StaticApplicationContext context = new StaticApplicationContext();
		context.registerBeanDefinition("channel", builder.getBeanDefinition());
		JmsDestinationBackedMessageChannel channel = context.getBean("channel", JmsDestinationBackedMessageChannel.class);
		assertFalse(channel.isRunning());
		context.refresh();
		assertTrue(channel.isRunning());
		context.stop();
		assertFalse(channel.isRunning());
	}

}
