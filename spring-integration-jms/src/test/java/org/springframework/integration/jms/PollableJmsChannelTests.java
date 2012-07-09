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

package org.springframework.integration.jms;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.jms.Destination;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.ChannelInterceptor;
import org.springframework.integration.jms.config.ActiveMqTestUtils;
import org.springframework.integration.jms.config.JmsChannelFactoryBean;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

/**
 * @author Mark Fisher
 * @author Gary Russell
 */
public class PollableJmsChannelTests {

	private ActiveMQConnectionFactory connectionFactory;

	private Destination queue;

	@Test
	public void queueReference() throws Exception {
		ActiveMqTestUtils.prepare();
		this.connectionFactory = new ActiveMQConnectionFactory();
		this.connectionFactory.setBrokerURL("vm://localhost");
		this.queue = new ActiveMQQueue("pollableJmsChannelTestQueue");

		JmsChannelFactoryBean factoryBean = new JmsChannelFactoryBean(false);
		factoryBean.setConnectionFactory(this.connectionFactory);
		factoryBean.setDestination(this.queue);
		factoryBean.afterPropertiesSet();
		PollableJmsChannel channel = (PollableJmsChannel) factoryBean.getObject();
		boolean sent1 = channel.send(new GenericMessage<String>("foo"));
		assertTrue(sent1);
		boolean sent2 = channel.send(new GenericMessage<String>("bar"));
		assertTrue(sent2);
		Message<?> result1 = channel.receive(1000);
		assertNotNull(result1);
		assertEquals("foo", result1.getPayload());
		Message<?> result2 = channel.receive(1000);
		assertNotNull(result2);
		assertEquals("bar", result2.getPayload());
	}

	@Test
	public void queueName() throws Exception {
		ActiveMqTestUtils.prepare();
		this.connectionFactory = new ActiveMQConnectionFactory();
		this.connectionFactory.setBrokerURL("vm://localhost");

		JmsChannelFactoryBean factoryBean = new JmsChannelFactoryBean(false);
		factoryBean.setConnectionFactory(this.connectionFactory);
		factoryBean.setDestinationName("someDynamicQueue");
		factoryBean.setPubSubDomain(false);
		factoryBean.afterPropertiesSet();
		PollableJmsChannel channel = (PollableJmsChannel) factoryBean.getObject();
		boolean sent1 = channel.send(new GenericMessage<String>("foo"));
		assertTrue(sent1);
		boolean sent2 = channel.send(new GenericMessage<String>("bar"));
		assertTrue(sent2);
		Message<?> result1 = channel.receive(10000);
		assertNotNull(result1);
		assertEquals("foo", result1.getPayload());
		Message<?> result2 = channel.receive(1000);
		assertNotNull(result2);
		assertEquals("bar", result2.getPayload());
	}

	@Test
	public void queueNameWithFalsePreReceiveInterceptors() throws Exception {
		ActiveMqTestUtils.prepare();
		this.connectionFactory = new ActiveMQConnectionFactory();
		this.connectionFactory.setBrokerURL("vm://localhost");

		JmsChannelFactoryBean factoryBean = new JmsChannelFactoryBean(false);
		factoryBean.setConnectionFactory(this.connectionFactory);
		factoryBean.setDestinationName("someDynamicQueue");
		factoryBean.setPubSubDomain(false);
		List<ChannelInterceptor> interceptorList = new ArrayList<ChannelInterceptor>();
		ChannelInterceptor interceptor = spy(new SampleInterceptor(false));
		interceptorList.add(interceptor);
		factoryBean.setInterceptors(interceptorList);
		factoryBean.afterPropertiesSet();
		PollableJmsChannel channel = (PollableJmsChannel) factoryBean.getObject();
		boolean sent1 = channel.send(new GenericMessage<String>("foo"));
		assertTrue(sent1);
		Message<?> result1 = channel.receive(10000);
		assertNull(result1);
		verify(interceptor, times(1)).preReceive(Mockito.any(MessageChannel.class));
		verify(interceptor, times(0)).postReceive(Mockito.any(Message.class), Mockito.any(MessageChannel.class));
	}

	@Test
	public void queueNameWithTruePreReceiveInterceptors() throws Exception {
		ActiveMqTestUtils.prepare();
		this.connectionFactory = new ActiveMQConnectionFactory();
		this.connectionFactory.setBrokerURL("vm://localhost");

		JmsChannelFactoryBean factoryBean = new JmsChannelFactoryBean(false);
		factoryBean.setConnectionFactory(this.connectionFactory);
		factoryBean.setDestinationName("someDynamicQueue");
		factoryBean.setPubSubDomain(false);
		List<ChannelInterceptor> interceptorList = new ArrayList<ChannelInterceptor>();
		ChannelInterceptor interceptor = spy(new SampleInterceptor(true));
		interceptorList.add(interceptor);
		factoryBean.setInterceptors(interceptorList);
		factoryBean.afterPropertiesSet();
		PollableJmsChannel channel = (PollableJmsChannel) factoryBean.getObject();
		boolean sent1 = channel.send(new GenericMessage<String>("foo"));
		assertTrue(sent1);
		Message<?> result1 = channel.receive(10000);
		assertNotNull(result1);
		verify(interceptor, times(1)).preReceive(Mockito.any(MessageChannel.class));
		verify(interceptor, times(1)).postReceive(Mockito.any(Message.class), Mockito.any(MessageChannel.class));
	}

	@Test
	public void qos() throws Exception {
		ActiveMqTestUtils.prepare();
		this.connectionFactory = new ActiveMQConnectionFactory();
		this.connectionFactory.setBrokerURL("vm://localhost");
		this.queue = new ActiveMQQueue("pollableJmsChannelTestQueue");
		CachingConnectionFactory ccf = new CachingConnectionFactory(connectionFactory);

		JmsChannelFactoryBean factoryBean = new JmsChannelFactoryBean(false);
		factoryBean.setConnectionFactory(ccf);
		factoryBean.setDestination(this.queue);
		factoryBean.setExplicitQosEnabled(true);
		factoryBean.setPriority(5);
		int ttl = 10000;
		factoryBean.setTimeToLive(ttl);
		factoryBean.setDeliveryPersistent(false);
		factoryBean.afterPropertiesSet();
		PollableJmsChannel channel = (PollableJmsChannel) factoryBean.getObject();
		final JmsTemplate receiver = new JmsTemplate(this.connectionFactory);
		boolean sent1 = channel.send(new GenericMessage<String>("foo"));
		assertTrue(sent1);
		final AtomicReference<javax.jms.Message> message = new AtomicReference<javax.jms.Message>();
		final CountDownLatch latch1 = new CountDownLatch(1);
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			public void run() {
				message.set(receiver.receive(queue));
				latch1.countDown();
			}});
		assertTrue(latch1.await(10, TimeUnit.SECONDS));
		assertNotNull(message.get());
		assertEquals(5, message.get().getJMSPriority());
		assertTrue(message.get().getJMSExpiration() <= System.currentTimeMillis() + ttl);
		assertTrue(message.get().toString().contains("persistent = false"));
		message.set(null);
		final CountDownLatch latch2 = new CountDownLatch(1);
		boolean sent2 = channel.send(MessageBuilder.withPayload("foo").setPriority(6).build());
		assertTrue(sent2);
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			public void run() {
				message.set(receiver.receive(queue));
				latch2.countDown();
			}});
		assertTrue(latch2.await(10, TimeUnit.SECONDS));
		assertNotNull(message.get());
		assertEquals(6, message.get().getJMSPriority());
		assertTrue(message.get().getJMSExpiration() <= System.currentTimeMillis() + ttl);
		assertTrue(message.get().toString().contains("persistent = false"));
	}

	public static class SampleInterceptor implements ChannelInterceptor {
		private final boolean preReceiveFlag;
		public SampleInterceptor(boolean preReceiveFlag){
			this.preReceiveFlag = preReceiveFlag;
		}

		public Message<?> preSend(Message<?> message, MessageChannel channel) {
			return message;
		}

		public void postSend(Message<?> message, MessageChannel channel,
				boolean sent) {
		}

		public boolean preReceive(MessageChannel channel) {
			return this.preReceiveFlag;
		}

		public Message<?> postReceive(Message<?> message, MessageChannel channel) {
			return message;
		}

	}
}
