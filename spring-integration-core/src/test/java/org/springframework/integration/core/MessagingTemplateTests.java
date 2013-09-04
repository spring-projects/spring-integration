/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.MessagingException;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.mapping.InboundMessageMapper;
import org.springframework.integration.mapping.OutboundMessageMapper;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.channel.ChannelResolutionException;
import org.springframework.integration.support.channel.ChannelResolver;
import org.springframework.integration.support.converter.SimpleMessageConverter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.test.util.TestUtils.TestApplicationContext;

/**
 * @author Mark Fisher
 * @author Gary Russell
 */
public class MessagingTemplateTests {

	private final TestApplicationContext context = TestUtils.createTestApplicationContext();

	private QueueChannel requestChannel;


	@Before
	public void setUp() {
		this.requestChannel = new QueueChannel();
		context.registerChannel("requestChannel", requestChannel);
		PollingConsumer endpoint = new PollingConsumer(requestChannel, new TestHandler());
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
		template.send(channel, new GenericMessage<String>("test"));
		Message<?> reply = channel.receive(0);
		assertNotNull(reply);
		assertEquals("test", reply.getPayload());
	}

	@Test
	public void sendWithDefaultChannelProvidedBySetter() {
		QueueChannel channel = new QueueChannel();
		MessagingTemplate template = new MessagingTemplate();
		template.setDefaultChannel(channel);
		template.send(new GenericMessage<String>("test"));
		Message<?> reply = channel.receive(0);
		assertNotNull(reply);
		assertEquals("test", reply.getPayload());
	}

	@Test
	public void sendWithDefaultChannelProvidedByConstructor() {
		QueueChannel channel = new QueueChannel();
		MessagingTemplate template = new MessagingTemplate(channel);
		template.send(new GenericMessage<String>("test"));
		Message<?> reply = channel.receive(0);
		assertNotNull(reply);
		assertEquals("test", reply.getPayload());
	}

	@Test
	public void sendWithExplicitChannelTakesPrecedenceOverDefault() {
		QueueChannel explicitChannel = new QueueChannel();
		QueueChannel defaultChannel = new QueueChannel();
		MessagingTemplate template = new MessagingTemplate(defaultChannel);
		template.send(explicitChannel, new GenericMessage<String>("test"));
		Message<?> reply = explicitChannel.receive(0);
		assertNotNull(reply);
		assertEquals("test", reply.getPayload());
		assertNull(defaultChannel.receive(0));
	}

	@Test(expected = IllegalStateException.class)
	public void sendWithoutChannelArgFailsIfNoDefaultAvailable() {
		MessagingTemplate template = new MessagingTemplate();
		template.send(new GenericMessage<String>("test"));
	}

	@Test
	public void receive() {
		QueueChannel channel = new QueueChannel();
		channel.send(new GenericMessage<String>("test"));
		MessagingTemplate template = new MessagingTemplate();
		Message<?> reply = template.receive(channel);
		assertEquals("test", reply.getPayload());
	}

	@Test
	public void receiveWithDefaultChannelProvidedBySetter() {
		QueueChannel channel = new QueueChannel();
		channel.send(new GenericMessage<String>("test"));
		MessagingTemplate template = new MessagingTemplate();
		template.setDefaultChannel(channel);
		Message<?> reply = template.receive();
		assertEquals("test", reply.getPayload());
	}

	@Test
	public void receiveWithDefaultChannelProvidedByConstructor() {
		QueueChannel channel = new QueueChannel();
		channel.send(new GenericMessage<String>("test"));
		MessagingTemplate template = new MessagingTemplate(channel);
		Message<?> reply = template.receive();
		assertEquals("test", reply.getPayload());
	}

	@Test
	public void receiveWithExplicitChannelTakesPrecedenceOverDefault() {
		QueueChannel explicitChannel = new QueueChannel();
		QueueChannel defaultChannel = new QueueChannel();
		explicitChannel.send(new GenericMessage<String>("test"));
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
		Message<?> reply = template.sendAndReceive(this.requestChannel, new GenericMessage<String>("test"));
		assertEquals("TEST", reply.getPayload());
	}

	@Test
	public void sendAndReceiveWithDefaultChannel() {
		MessagingTemplate template = new MessagingTemplate();
		template.setReceiveTimeout(3000);
		template.setDefaultChannel(this.requestChannel);
		Message<?> reply = template.sendAndReceive(new GenericMessage<String>("test"));
		assertEquals("TEST", reply.getPayload());
	}

	@Test
	public void sendAndReceiveWithExplicitChannelTakesPrecedenceOverDefault() {
		QueueChannel defaultChannel = new QueueChannel();
		MessagingTemplate template = new MessagingTemplate(defaultChannel);
		template.setReceiveTimeout(3000);
		Message<?> message = new GenericMessage<String>("test");
		Message<?> reply = template.sendAndReceive(this.requestChannel, message);
		assertEquals("TEST", reply.getPayload());
		assertNull(defaultChannel.receive(0));
	}

	@Test(expected = IllegalStateException.class)
	public void sendAndReceiveWithoutChannelArgFailsIfNoDefaultAvailable() {
		MessagingTemplate template = new MessagingTemplate();
		template.sendAndReceive(new GenericMessage<String>("test"));
	}

	@Test
	public void convertSendAndReceive() {
		MessagingTemplate template = new MessagingTemplate();
		template.setReceiveTimeout(3000);
		Object result = template.convertSendAndReceive(this.requestChannel, "test");
		assertNotNull(result);
		assertEquals("TEST", result);
	}

	@Test
	public void convertSendAndReceiveWithDefaultChannel() {
		MessagingTemplate template = new MessagingTemplate();
		template.setDefaultChannel(this.requestChannel);
		template.setReceiveTimeout(3000);
		Object result = template.convertSendAndReceive("test");
		assertNotNull(result);
		assertEquals("TEST", result);
	}

	@Test
	public void convertSendAndReceiveWithResolvedChannel() {
		StaticApplicationContext context = new StaticApplicationContext();
		context.registerSingleton("testChannel", DirectChannel.class);
		context.refresh();
		SubscribableChannel testChannel = context.getBean("testChannel", SubscribableChannel.class);
		testChannel.subscribe(new TestHandler());
		MessagingTemplate template = new MessagingTemplate();
		template.setBeanFactory(context);
		template.setReceiveTimeout(3000);
		Object result = template.convertSendAndReceive("testChannel", "test");
		assertNotNull(result);
		assertEquals("TEST", result);
	}

	@Test(expected = ChannelResolutionException.class)
	public void convertSendAndReceiveWithUnresolvableChannel() {
		StaticApplicationContext context = new StaticApplicationContext();
		context.refresh();
		MessagingTemplate template = new MessagingTemplate();
		template.setBeanFactory(context);
		template.setReceiveTimeout(3000);
		template.convertSendAndReceive("testChannel", "test");
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
		final QueueChannel anotherChannel = new QueueChannel();

		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("testChannel", testChannel);

		MessagingTemplate template = new MessagingTemplate();
		template.setBeanFactory(beanFactory);

		template.afterPropertiesSet();
		Message<?> message = MessageBuilder.withPayload("test").build();
		template.send("testChannel", message);
		assertEquals(message, testChannel.receive(0));

		template.setChannelResolver(new ChannelResolver() {
			public MessageChannel resolveChannelName(String channelName) {
				return anotherChannel;
			}
		});
		message = MessageBuilder.withPayload("test").build();
		template.send("testChannel", message);
		assertEquals(message, anotherChannel.receive(0));
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
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("testChannel", testChannel);

		MessagingTemplate template = new MessagingTemplate();
		template.setBeanFactory(beanFactory);
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

	@Test
	public void convertAndSendToChannel() {
		MessagingTemplate template = new MessagingTemplate();
		QueueChannel channel = new QueueChannel();
		template.convertAndSend(channel, "test");
		Message<?> reply = channel.receive(0);
		assertNotNull(reply);
		assertEquals("test", reply.getPayload());
	}

	@Test
	public void convertAndSendToDefaultChannel() {
		QueueChannel channel = new QueueChannel();
		MessagingTemplate template = new MessagingTemplate();
		template.setDefaultChannel(channel);
		template.convertAndSend("test");
		Message<?> reply = channel.receive(0);
		assertNotNull(reply);
		assertEquals("test", reply.getPayload());
	}

	@Test
	public void convertAndSendToResolvedChannel() {
		StaticApplicationContext context = new StaticApplicationContext();
		context.registerSingleton("testChannel", QueueChannel.class);
		context.refresh();
		MessagingTemplate template = new MessagingTemplate();
		template.setBeanFactory(context);
		template.afterPropertiesSet();
		template.convertAndSend("testChannel", "test");
		PollableChannel channel = context.getBean("testChannel", PollableChannel.class);
		Message<?> reply = channel.receive(0);
		assertEquals("test", reply.getPayload());
	}

	@Test(expected = ChannelResolutionException.class)
	public void convertAndSendToUnresolvableChannel() {
		StaticApplicationContext context = new StaticApplicationContext();
		context.refresh();
		MessagingTemplate template = new MessagingTemplate();
		template.setBeanFactory(context);
		template.afterPropertiesSet();
		template.convertAndSend("testChannel", "test");
	}

	@Test
	public void convertAndSendWithCustomConverter() {
		MessagingTemplate template = new MessagingTemplate();
		TestMapper mapper = new TestMapper();
		template.setMessageConverter(new SimpleMessageConverter(mapper, mapper));
		QueueChannel channel = new QueueChannel();
		template.convertAndSend(channel, "test");
		Message<?> reply = channel.receive(0);
		assertNotNull(reply);
		assertEquals("to:test", reply.getPayload());
	}

	@Test
	public void receiveAndConvertFromChannel() {
		MessagingTemplate template = new MessagingTemplate();
		QueueChannel channel = new QueueChannel();
		channel.send(new GenericMessage<String>("test"));
		Object result = template.receiveAndConvert(channel);
		assertNotNull(result);
		assertEquals("test", result);
	}

	@Test
	public void receiveAndConvertFromDefaultChannel() {
		QueueChannel channel = new QueueChannel();
		channel.send(new GenericMessage<String>("test"));
		MessagingTemplate template = new MessagingTemplate();
		template.setDefaultChannel(channel);
		Object result = template.receiveAndConvert();
		assertNotNull(result);
		assertEquals("test", result);
	}

	@Test
	public void receiveAndConvertFromResolvedChannel() {
		StaticApplicationContext context = new StaticApplicationContext();
		context.registerSingleton("testChannel", QueueChannel.class);
		context.refresh();
		PollableChannel channel = context.getBean("testChannel", PollableChannel.class);
		channel.send(new GenericMessage<String>("test"));
		MessagingTemplate template = new MessagingTemplate();
		template.setBeanFactory(context);
		template.afterPropertiesSet();
		Object result = template.receiveAndConvert("testChannel");
		assertNotNull(result);
		assertEquals("test", result);
	}

	@Test(expected = ChannelResolutionException.class)
	public void receiveAndConvertFromUnresolvableChannel() {
		StaticApplicationContext context = new StaticApplicationContext();
		context.refresh();
		MessagingTemplate template = new MessagingTemplate();
		template.setBeanFactory(context);
		template.afterPropertiesSet();
		template.receiveAndConvert("testChannel");
	}

	@Test
	public void receiveAndConvertWithCustomConverter() {
		MessagingTemplate template = new MessagingTemplate();
		TestMapper mapper = new TestMapper();
		template.setMessageConverter(new SimpleMessageConverter(mapper, mapper));
		QueueChannel channel = new QueueChannel();
		channel.send(new GenericMessage<String>("test"));
		Object result = template.receiveAndConvert(channel);
		assertNotNull(result);
		assertEquals("from:test", result);
	}

	@Test
	public void convertSendAndReceiveWithCustomConverter() {
		TestMapper mapper = new TestMapper();
		MessagingTemplate template = new MessagingTemplate();
		template.setDefaultChannel(this.requestChannel);
		template.setMessageConverter(new SimpleMessageConverter(mapper, mapper));
		Object result = template.convertSendAndReceive("test");
		assertNotNull(result);
		assertEquals("from:TO:TEST", result);
	}

	@Test
	public void testLateReply() {
		MessagingTemplate template = new MessagingTemplate();
		QueueChannel channel = new QueueChannel();
		template.setDefaultChannel(channel);
		template.setReceiveTimeout(1);
		template.setThrowExceptionOnLateReply(true);
		Object result = template.sendAndReceive(new GenericMessage<String>("foo"));
		assertNull(result);
		Message<?> message = channel.receive();
		try {
			((MessageChannel) message.getHeaders().getReplyChannel()).send(new GenericMessage<String>("bar"));
			fail("Exception expected");
		}
		catch (MessagingException e) {
			assertEquals("Reply message being sent, but the receiving thread has already timed out", e.getMessage());
		}
	}

	@Test
	public void testNoSubs() {
		MessagingTemplate template = new MessagingTemplate();
		PublishSubscribeChannel channel = new PublishSubscribeChannel();
		template.setDefaultChannel(channel);
		template.convertAndSend("foo");
	}

	@Test
	public void testSubsAllFail() {
		MessagingTemplate template = new MessagingTemplate();
		PublishSubscribeChannel channel = new PublishSubscribeChannel();
		channel.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				throw new RuntimeException();
			}
		});
		channel.setIgnoreFailures(true);
		template.setDefaultChannel(channel);
		template.convertAndSend("foo");
	}

	@Test
	public void testEnoughSubs() {
		MessagingTemplate template = new MessagingTemplate();
		PublishSubscribeChannel channel = new PublishSubscribeChannel();
		channel.setMinSubscribers(1);
		channel.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
			}
		});
		template.setDefaultChannel(channel);
		template.convertAndSend("foo");
	}

	@Test(expected=MessageDeliveryException.class)
	public void testNoSubsFatal() {
		MessagingTemplate template = new MessagingTemplate();
		PublishSubscribeChannel channel = new PublishSubscribeChannel();
		channel.setMinSubscribers(1);
		template.setDefaultChannel(channel);
		template.convertAndSend("foo");
	}

	@Test(expected=MessageDeliveryException.class)
	public void testNotEnoughSubsFatal() {
		MessagingTemplate template = new MessagingTemplate();
		PublishSubscribeChannel channel = new PublishSubscribeChannel();
		channel.setMinSubscribers(2);
		channel.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
			}
		});
		template.setDefaultChannel(channel);
		template.convertAndSend("foo");
	}

	@Test
	public void testNeverReceive() {
		MessagingTemplate template = new MessagingTemplate();
		DirectChannel channel = new DirectChannel();
		final AtomicReference<MessageChannel> replyChannel = new AtomicReference<MessageChannel>();
		channel.subscribe(new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				replyChannel.set((MessageChannel) message.getHeaders().getReplyChannel());
				throw new MessagingException("foo");
			}
		});
		template.setDefaultChannel(channel);
		template.setReceiveTimeout(10000);
		template.setThrowExceptionOnLateReply(true);
		try {
			template.sendAndReceive(new GenericMessage<String>("foo"));
			fail("Exception expected");
		}
		catch (MessagingException e) {
			assertTrue(e.getMessage().equals("foo"));
		}
		try {
			replyChannel.get().send(new GenericMessage<String>("bar"));
			fail("Exception expected");
		}
		catch (MessagingException e) {
			assertEquals(
					"Reply message being sent, but the receiving thread has already caught an exception and won't receive",
					e.getMessage());
		}
	}

	@Test
	public void testTwoReplies() {
		MessagingTemplate template = new MessagingTemplate();
		DirectChannel channel = new DirectChannel();
		final AtomicReference<MessageChannel> replyChannel = new AtomicReference<MessageChannel>();
		channel.subscribe(new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				MessageChannel replyChannelHeader = (MessageChannel) message.getHeaders().getReplyChannel();
				replyChannel.set(replyChannelHeader);
				replyChannelHeader.send(new GenericMessage<String>("bar"));
			}
		});
		template.setDefaultChannel(channel);
		template.setReceiveTimeout(10000);
		template.setThrowExceptionOnLateReply(true);
		Message<?> reply = template.sendAndReceive(new GenericMessage<String>("foo"));
		assertTrue(reply.getPayload().equals("bar"));
		try {
			replyChannel.get().send(new GenericMessage<String>("baz"));
			fail("Exception expected");
		}
		catch (MessagingException e) {
			assertEquals(
					"Reply message being sent, but the receiving thread has already received a reply",
					e.getMessage());
		}
	}

	private static class TestMapper implements InboundMessageMapper<Object>, OutboundMessageMapper<Object> {

		public Object fromMessage(Message<?> message) throws Exception {
			return "from:" + message.getPayload();
		}

		public Message<?> toMessage(Object object) throws Exception {
			return new GenericMessage<String>("to:" + object);
		}
	}


	private static class TestHandler extends AbstractReplyProducingMessageHandler {

		@Override
		public Object handleRequestMessage(Message<?> message) {
			return message.getPayload().toString().toUpperCase();
		}
	}

}
