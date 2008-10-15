/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.channel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.bus.DefaultMessageBus;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.endpoint.AbstractReplyProducingMessageConsumer;
import org.springframework.integration.endpoint.PollingConsumerEndpoint;
import org.springframework.integration.endpoint.ReplyMessageHolder;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.util.TestUtils;

/**
 * @author Mark Fisher
 */
public class MessageChannelTemplateTests {

	private QueueChannel requestChannel;


	@Before
	public void setUp() {
		this.requestChannel = new QueueChannel();
		this.requestChannel.setBeanName("requestChannel");
		AbstractReplyProducingMessageConsumer consumer = new AbstractReplyProducingMessageConsumer() {
			public void onMessage(Message<?> message, ReplyMessageHolder replyHolder) {
				replyHolder.set(message.getPayload().toString().toUpperCase());
			}
		};
		PollingConsumerEndpoint endpoint = new PollingConsumerEndpoint(consumer, requestChannel);
		endpoint.afterPropertiesSet();
		GenericApplicationContext context = new GenericApplicationContext();
		context.getBeanFactory().registerSingleton("requestChannel", requestChannel);
		context.getBeanFactory().registerSingleton("testEndpoint", endpoint);
		DefaultMessageBus bus = new DefaultMessageBus();
		bus.setTaskScheduler(TestUtils.createTaskScheduler(10));
		bus.setApplicationContext(context);
		bus.start();
	}


	@Test
	public void send() {
		MessageChannelTemplate template = new MessageChannelTemplate();
		QueueChannel channel = new QueueChannel();
		template.send(new StringMessage("test"), channel);
		Message<?> reply = channel.receive(0);
		assertNotNull(reply);
		assertEquals("test", reply.getPayload());
	}

	@Test
	public void sendWithDefaultChannelProvidedBySetter() {
		QueueChannel channel = new QueueChannel();
		MessageChannelTemplate template = new MessageChannelTemplate();
		template.setDefaultChannel(channel);
		template.send(new StringMessage("test"));
		Message<?> reply = channel.receive(0);
		assertNotNull(reply);
		assertEquals("test", reply.getPayload());
	}

	@Test
	public void sendWithDefaultChannelProvidedByConstructor() {
		QueueChannel channel = new QueueChannel();
		MessageChannelTemplate template = new MessageChannelTemplate(channel);
		template.send(new StringMessage("test"));
		Message<?> reply = channel.receive(0);
		assertNotNull(reply);
		assertEquals("test", reply.getPayload());
	}

	@Test
	public void sendWithExplicitChannelTakesPrecedenceOverDefault() {
		QueueChannel explicitChannel = new QueueChannel();
		QueueChannel defaultChannel = new QueueChannel();
		MessageChannelTemplate template = new MessageChannelTemplate(defaultChannel);
		template.send(new StringMessage("test"), explicitChannel);
		Message<?> reply = explicitChannel.receive(0);
		assertNotNull(reply);
		assertEquals("test", reply.getPayload());
		assertNull(defaultChannel.receive(0));
	}

	@Test(expected = IllegalStateException.class)
	public void sendWithoutChannelArgFailsIfNoDefaultAvailable() {
		MessageChannelTemplate template = new MessageChannelTemplate();
		template.send(new StringMessage("test"));
	}

	@Test
	public void receive() {
		QueueChannel channel = new QueueChannel();
		channel.send(new StringMessage("test"));
		MessageChannelTemplate template = new MessageChannelTemplate();
		Message<?> reply = template.receive(channel);
		assertEquals("test", reply.getPayload());
	}

	@Test
	public void receiveWithDefaultChannelProvidedBySetter() {
		QueueChannel channel = new QueueChannel();
		channel.send(new StringMessage("test"));
		MessageChannelTemplate template = new MessageChannelTemplate();
		template.setDefaultChannel(channel);
		Message<?> reply = template.receive();
		assertEquals("test", reply.getPayload());
	}

	@Test
	public void receiveWithDefaultChannelProvidedByConstructor() {
		QueueChannel channel = new QueueChannel();
		channel.send(new StringMessage("test"));
		MessageChannelTemplate template = new MessageChannelTemplate(channel);
		Message<?> reply = template.receive();
		assertEquals("test", reply.getPayload());
	}

	@Test
	public void receiveWithExplicitChannelTakesPrecedenceOverDefault() {
		QueueChannel explicitChannel = new QueueChannel();
		QueueChannel defaultChannel = new QueueChannel();
		explicitChannel.send(new StringMessage("test"));
		MessageChannelTemplate template = new MessageChannelTemplate(defaultChannel);
		template.setReceiveTimeout(0);
		Message<?> reply = template.receive(explicitChannel);
		assertEquals("test", reply.getPayload());
		assertNull(template.receive());
	}

	@Test(expected = IllegalStateException.class)
	public void receiveWithoutChannelArgFailsIfNoDefaultAvailable() {
		MessageChannelTemplate template = new MessageChannelTemplate();
		template.receive();
	}

	@Test(expected = IllegalStateException.class)
	public void receiveWithNonPollableDefaultFails() {
		DirectChannel channel = new DirectChannel();
		MessageChannelTemplate template = new MessageChannelTemplate(channel);
		template.receive();
	}

	@Test
	public void sendAndReceive() {
		MessageChannelTemplate template = new MessageChannelTemplate();
		Message<?> reply = template.sendAndReceive(new StringMessage("test"), this.requestChannel);
		assertEquals("TEST", reply.getPayload());
	}

	@Test
	public void sendAndReceiveWithDefaultChannel() {
		MessageChannelTemplate template = new MessageChannelTemplate();
		template.setDefaultChannel(this.requestChannel);
		Message<?> reply = template.sendAndReceive(new StringMessage("test"));
		assertEquals("TEST", reply.getPayload());
	}

	@Test
	public void sendAndReceiveWithExplicitChannelTakesPrecedenceOverDefault() {
		QueueChannel defaultChannel = new QueueChannel();
		MessageChannelTemplate template = new MessageChannelTemplate(defaultChannel);
		Message<?> message = new StringMessage("test");
		Message<?> reply = template.sendAndReceive(message, this.requestChannel);
		assertEquals("TEST", reply.getPayload());
		assertNull(defaultChannel.receive(0));
	}

	@Test(expected = IllegalStateException.class)
	public void sendAndReceiveWithoutChannelArgFailsIfNoDefaultAvailable() {
		MessageChannelTemplate template = new MessageChannelTemplate();
		template.sendAndReceive(new StringMessage("test"));
	}

	@Test
	public void sendWithReturnAddress() throws InterruptedException {
		final List<String> replies = new ArrayList<String>(3);
		final CountDownLatch latch = new CountDownLatch(3);
		MessageChannel replyChannel = new MessageChannel() {
			public String getName() {
				return "testReplyChannel";
			}
			public boolean send(Message<?> replyMessage) {
				replies.add((String) replyMessage.getPayload());
				latch.countDown();
				return true;
			}
		};
		MessageChannelTemplate template = new MessageChannelTemplate();
		Message<String> message1 = MessageBuilder.withPayload("test1").setReplyChannel(replyChannel).build();
		Message<String> message2 = MessageBuilder.withPayload("test2").setReplyChannel(replyChannel).build();
		Message<String> message3 = MessageBuilder.withPayload("test3").setReplyChannel(replyChannel).build();
		template.send(message1, this.requestChannel);
		template.send(message2, this.requestChannel);
		template.send(message3, this.requestChannel);
		latch.await(2000, TimeUnit.MILLISECONDS);
		assertEquals(0, latch.getCount());
		assertTrue(replies.contains("TEST1"));
		assertTrue(replies.contains("TEST2"));
		assertTrue(replies.contains("TEST3"));
	}

}
