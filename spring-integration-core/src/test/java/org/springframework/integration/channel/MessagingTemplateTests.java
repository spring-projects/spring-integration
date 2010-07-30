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

package org.springframework.integration.channel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.context.support.StaticApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.core.ChannelResolutionException;
import org.springframework.integration.core.MessageBuilder;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.core.StringMessage;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.test.util.TestUtils.TestApplicationContext;
import org.springframework.scheduling.support.PeriodicTrigger;

/**
 * @author Mark Fisher
 */
public class MessagingTemplateTests {

	private TestApplicationContext context = TestUtils.createTestApplicationContext();

	private QueueChannel requestChannel;


	@Before
	public void setUp() {
		this.requestChannel = new QueueChannel();
		context.registerChannel("requestChannel", requestChannel);
		AbstractReplyProducingMessageHandler handler = new AbstractReplyProducingMessageHandler() {
			@Override
			public Object handleRequestMessage(Message<?> message) {
				return message.getPayload().toString().toUpperCase();
			}
		};
		PollingConsumer endpoint = new PollingConsumer(requestChannel, handler);
		endpoint.setTrigger(new PeriodicTrigger(10));
		context.registerEndpoint("testEndpoint", endpoint);
		context.refresh();
	}

	@After
	public void tearDown() {
		try {
			context.stop();
		}
		catch (Exception e) {
			// ignore
		}
	}

	@Test
	public void send() {
		MessagingTemplate template = new MessagingTemplate();
		QueueChannel channel = new QueueChannel();
		template.send(channel, new StringMessage("test"));
		Message<?> reply = channel.receive(0);
		assertNotNull(reply);
		assertEquals("test", reply.getPayload());
	}

	@Test
	public void sendWithDefaultChannelProvidedBySetter() {
		QueueChannel channel = new QueueChannel();
		MessagingTemplate template = new MessagingTemplate();
		template.setDefaultChannel(channel);
		template.send(new StringMessage("test"));
		Message<?> reply = channel.receive(0);
		assertNotNull(reply);
		assertEquals("test", reply.getPayload());
	}

	@Test
	public void sendWithDefaultChannelProvidedByConstructor() {
		QueueChannel channel = new QueueChannel();
		MessagingTemplate template = new MessagingTemplate(channel);
		template.send(new StringMessage("test"));
		Message<?> reply = channel.receive(0);
		assertNotNull(reply);
		assertEquals("test", reply.getPayload());
	}

	@Test
	public void sendWithExplicitChannelTakesPrecedenceOverDefault() {
		QueueChannel explicitChannel = new QueueChannel();
		QueueChannel defaultChannel = new QueueChannel();
		MessagingTemplate template = new MessagingTemplate(defaultChannel);
		template.send(explicitChannel, new StringMessage("test"));
		Message<?> reply = explicitChannel.receive(0);
		assertNotNull(reply);
		assertEquals("test", reply.getPayload());
		assertNull(defaultChannel.receive(0));
	}

	@Test(expected = IllegalStateException.class)
	public void sendWithoutChannelArgFailsIfNoDefaultAvailable() {
		MessagingTemplate template = new MessagingTemplate();
		template.send(new StringMessage("test"));
	}

	@Test
	public void receive() {
		QueueChannel channel = new QueueChannel();
		channel.send(new StringMessage("test"));
		MessagingTemplate template = new MessagingTemplate();
		Message<?> reply = template.receive(channel);
		assertEquals("test", reply.getPayload());
	}

	@Test
	public void receiveWithDefaultChannelProvidedBySetter() {
		QueueChannel channel = new QueueChannel();
		channel.send(new StringMessage("test"));
		MessagingTemplate template = new MessagingTemplate();
		template.setDefaultChannel(channel);
		Message<?> reply = template.receive();
		assertEquals("test", reply.getPayload());
	}

	@Test
	public void receiveWithDefaultChannelProvidedByConstructor() {
		QueueChannel channel = new QueueChannel();
		channel.send(new StringMessage("test"));
		MessagingTemplate template = new MessagingTemplate(channel);
		Message<?> reply = template.receive();
		assertEquals("test", reply.getPayload());
	}

	@Test
	public void receiveWithExplicitChannelTakesPrecedenceOverDefault() {
		QueueChannel explicitChannel = new QueueChannel();
		QueueChannel defaultChannel = new QueueChannel();
		explicitChannel.send(new StringMessage("test"));
		MessagingTemplate template = new MessagingTemplate(defaultChannel);
		template.setReceiveTimeout(0);
		Message<?> reply = template.receive(explicitChannel);
		assertEquals("test", reply.getPayload());
		assertNull(template.receive());
	}

	@Test(expected = IllegalStateException.class)
	public void receiveWithoutChannelArgFailsIfNoDefaultAvailable() {
		MessagingTemplate template = new MessagingTemplate();
		template.receive();
	}

	@Test(expected = IllegalStateException.class)
	public void receiveWithNonPollableDefaultFails() {
		DirectChannel channel = new DirectChannel();
		MessagingTemplate template = new MessagingTemplate(channel);
		template.receive();
	}

	@Test
	public void sendAndReceive() {
		MessagingTemplate template = new MessagingTemplate();
		template.setReceiveTimeout(3000);
		Message<?> reply = template.sendAndReceive(this.requestChannel, new StringMessage("test"));
		assertEquals("TEST", reply.getPayload());
	}

	@Test
	public void sendAndReceiveWithDefaultChannel() {
		MessagingTemplate template = new MessagingTemplate();
		template.setReceiveTimeout(3000);
		template.setDefaultChannel(this.requestChannel);
		Message<?> reply = template.sendAndReceive(new StringMessage("test"));
		assertEquals("TEST", reply.getPayload());
	}

	@Test
	public void sendAndReceiveWithExplicitChannelTakesPrecedenceOverDefault() {
		QueueChannel defaultChannel = new QueueChannel();
		MessagingTemplate template = new MessagingTemplate(defaultChannel);
		template.setReceiveTimeout(3000);
		Message<?> message = new StringMessage("test");
		Message<?> reply = template.sendAndReceive(this.requestChannel, message);
		assertEquals("TEST", reply.getPayload());
		assertNull(defaultChannel.receive(0));
	}

	@Test(expected = IllegalStateException.class)
	public void sendAndReceiveWithoutChannelArgFailsIfNoDefaultAvailable() {
		MessagingTemplate template = new MessagingTemplate();
		template.sendAndReceive(new StringMessage("test"));
	}

	@Test
	public void sendWithReturnAddress() throws InterruptedException {
		final List<String> replies = new ArrayList<String>(3);
		final CountDownLatch latch = new CountDownLatch(3);
		MessageChannel replyChannel = new AbstractMessageChannel() {
			@Override
			protected boolean doSend(Message<?> message, long timeout) {
				replies.add((String) message.getPayload());
				latch.countDown();
				return true;
			}
		};
		MessagingTemplate template = new MessagingTemplate();
		Message<String> message1 = MessageBuilder.withPayload("test1").setReplyChannel(replyChannel).build();
		Message<String> message2 = MessageBuilder.withPayload("test2").setReplyChannel(replyChannel).build();
		Message<String> message3 = MessageBuilder.withPayload("test3").setReplyChannel(replyChannel).build();
		template.send(this.requestChannel, message1);
		template.send(this.requestChannel, message2);
		template.send(this.requestChannel, message3);
		latch.await(2000, TimeUnit.MILLISECONDS);
		assertEquals(0, latch.getCount());
		assertTrue(replies.contains("TEST1"));
		assertTrue(replies.contains("TEST2"));
		assertTrue(replies.contains("TEST3"));
	}

	@Test
	public void sendByChannelName() {
		StaticApplicationContext context = new StaticApplicationContext();
		context.registerSingleton("testChannel", QueueChannel.class);
		context.refresh();
		MessagingTemplate template = new MessagingTemplate();
		template.setBeanFactory(context);
		template.afterPropertiesSet();
		Message<?> message = MessageBuilder.withPayload("test").build();
		template.send("testChannel", message);
		PollableChannel channel = context.getBean("testChannel", PollableChannel.class);
		assertEquals(message, channel.receive(0));
	}

	@Test
	public void sendByChannelNameWithCustomChannelResolver() {
		QueueChannel testChannel = new QueueChannel();
		Map<String, MessageChannel> channelMap = new HashMap<String, MessageChannel>();
		channelMap.put("testChannel", testChannel);
		MapBasedChannelResolver channelResolver = new MapBasedChannelResolver(channelMap);
		MessagingTemplate template = new MessagingTemplate();
		template.setChannelResolver(channelResolver);
		template.afterPropertiesSet();
		Message<?> message = MessageBuilder.withPayload("test").build();
		template.send("testChannel", message);
		assertEquals(message, testChannel.receive(0));
	}

	@Test(expected = IllegalStateException.class)
	public void sendByChannelNameWithoutChannelResolver() {
		MessagingTemplate template = new MessagingTemplate();
		template.send("testChannel", MessageBuilder.withPayload("test").build());
	}

	@Test(expected = ChannelResolutionException.class)
	public void sendByChannelNameWithUnresolvableChannel() {
		StaticApplicationContext context = new StaticApplicationContext();
		context.registerSingleton("testChannel", QueueChannel.class);
		context.refresh();
		MessagingTemplate template = new MessagingTemplate();
		template.setBeanFactory(context);
		template.afterPropertiesSet();
		Message<?> message = MessageBuilder.withPayload("test").build();
		template.send("noSuchChannel", message);
	}

	@Test
	public void receiveByChannelName() {
		StaticApplicationContext context = new StaticApplicationContext();
		context.registerSingleton("testChannel", QueueChannel.class);
		context.refresh();
		MessagingTemplate template = new MessagingTemplate();
		template.setBeanFactory(context);
		template.afterPropertiesSet();
		PollableChannel channel = context.getBean("testChannel", PollableChannel.class);
		Message<?> message = MessageBuilder.withPayload("test").build();
		channel.send(message);
		assertEquals(message, template.receive("testChannel"));
	}

	@Test
	public void receiveByChannelNameWithCustomChannelResolver() {
		QueueChannel testChannel = new QueueChannel();
		Map<String, MessageChannel> channelMap = new HashMap<String, MessageChannel>();
		channelMap.put("testChannel", testChannel);
		MapBasedChannelResolver channelResolver = new MapBasedChannelResolver(channelMap);
		MessagingTemplate template = new MessagingTemplate();
		template.setChannelResolver(channelResolver);
		template.afterPropertiesSet();
		Message<?> message = MessageBuilder.withPayload("test").build();
		testChannel.send(message);
		assertEquals(message, template.receive("testChannel"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void receiveByChannelNameWithNonPollableChannel() {
		StaticApplicationContext context = new StaticApplicationContext();
		context.registerSingleton("testChannel", DirectChannel.class);
		context.refresh();
		MessagingTemplate template = new MessagingTemplate();
		template.setBeanFactory(context);
		template.afterPropertiesSet();
		template.receive("testChannel");
	}

	@Test(expected = IllegalStateException.class)
	public void receiveByChannelNameWithoutChannelResolver() {
		MessagingTemplate template = new MessagingTemplate();
		template.receive("testChannel");
	}

	@Test(expected = ChannelResolutionException.class)
	public void receiveByChannelNameWithUnresolvableChannel() {
		StaticApplicationContext context = new StaticApplicationContext();
		context.registerSingleton("testChannel", QueueChannel.class);
		context.refresh();
		MessagingTemplate template = new MessagingTemplate();
		template.setBeanFactory(context);
		template.afterPropertiesSet();
		template.receive("noSuchChannel");
	}

}
