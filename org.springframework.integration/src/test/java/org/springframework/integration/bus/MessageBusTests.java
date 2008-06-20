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

package org.springframework.integration.bus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.DispatcherPolicy;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.channel.RendezvousChannel;
import org.springframework.integration.endpoint.ConcurrencyPolicy;
import org.springframework.integration.endpoint.SourceEndpoint;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.ErrorMessage;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.Source;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.scheduling.PollingSchedule;
import org.springframework.integration.scheduling.Subscription;

/**
 * @author Mark Fisher
 */
public class MessageBusTests {

	@Test
	public void testOutputChannel() {
		MessageBus bus = new MessageBus();
		MessageChannel sourceChannel = new QueueChannel();
		MessageChannel targetChannel = new QueueChannel();
		bus.registerChannel("sourceChannel", sourceChannel);
		StringMessage message = new StringMessage("test");
		message.getHeader().setReturnAddress("targetChannel");
		sourceChannel.send(message);
		bus.registerChannel("targetChannel", targetChannel);
		MessageHandler handler = new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				return message;
			}
		};
		Subscription subscription = new Subscription(sourceChannel);
		bus.registerHandler("handler", handler, subscription);
		bus.start();
		Message<?> result = targetChannel.receive(3000);
		assertEquals("test", result.getPayload());
		bus.stop();
	}

	@Test
	public void testChannelsWithoutHandlers() {
		MessageBus bus = new MessageBus();
		MessageChannel sourceChannel = new QueueChannel();
		sourceChannel.send(new StringMessage("123", "test"));
		MessageChannel targetChannel = new QueueChannel();
		bus.registerChannel("sourceChannel", sourceChannel);
		bus.registerChannel("targetChannel", targetChannel);
		bus.start();
		Message<?> result = targetChannel.receive(100);
		assertNull(result);
		bus.stop();
	}

	@Test
	public void testAutodetectionWithApplicationContext() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("messageBusTests.xml", this.getClass());
		context.start();
		MessageChannel sourceChannel = (MessageChannel) context.getBean("sourceChannel");
		sourceChannel.send(new GenericMessage<String>("123", "test"));		
		MessageChannel targetChannel = (MessageChannel) context.getBean("targetChannel");
		MessageBus bus = (MessageBus) context.getBean("bus");
		bus.start();
		Message<?> result = targetChannel.receive(1000);
		assertEquals("test", result.getPayload());
	}

	@Test
	public void testExactlyOneHandlerReceivesPointToPointMessage() {
		QueueChannel inputChannel = new QueueChannel();
		QueueChannel outputChannel1 = new QueueChannel();
		QueueChannel outputChannel2 = new QueueChannel();
		MessageHandler handler1 = new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				message.getHeader().setReturnAddress("output1");
				return message;
			}
		};
		MessageHandler handler2 = new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				message.getHeader().setReturnAddress("output2");
				return message;
			}
		};
		MessageBus bus = new MessageBus();
		bus.registerChannel("input", inputChannel);
		bus.registerChannel("output1", outputChannel1);
		bus.registerChannel("output2", outputChannel2);
		bus.registerHandler("handler1", handler1, new Subscription(inputChannel));
		bus.registerHandler("handler2", handler2, new Subscription(inputChannel));
		bus.start();
		inputChannel.send(new StringMessage(1, "testing"));
		Message<?> message1 = outputChannel1.receive(100);
		Message<?> message2 = outputChannel2.receive(0);
		bus.stop();
		assertTrue("exactly one message should be null", message1 == null ^ message2 == null);
	}

	@Test
	public void testBothHandlersReceivePublishSubscribeMessage() throws InterruptedException {
		DispatcherPolicy dispatcherPolicy = new DispatcherPolicy(true);
		QueueChannel inputChannel = new QueueChannel(10, dispatcherPolicy);
		QueueChannel outputChannel1 = new QueueChannel();
		QueueChannel outputChannel2 = new QueueChannel();
		final CountDownLatch latch = new CountDownLatch(2);
		MessageHandler handler1 = new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				message.getHeader().setReturnAddress("output1");
				latch.countDown();
				return message;
			}
		};
		MessageHandler handler2 = new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				message.getHeader().setReturnAddress("output2");
				latch.countDown();
				return message;
			}
		};
		MessageBus bus = new MessageBus();
		bus.registerChannel("input", inputChannel);
		bus.registerChannel("output1", outputChannel1);
		bus.registerChannel("output2", outputChannel2);
		bus.registerHandler("handler1", handler1, new Subscription(inputChannel));
		bus.registerHandler("handler2", handler2, new Subscription(inputChannel));
		bus.start();
		inputChannel.send(new StringMessage(1, "testing"));
		latch.await(500, TimeUnit.MILLISECONDS);
		assertEquals("both handlers should have been invoked", 0, latch.getCount());
		Message<?> message1 = outputChannel1.receive(500);
		Message<?> message2 = outputChannel2.receive(500);
		bus.stop();
		assertNotNull("both handlers should have replied to the message", message1);
		assertNotNull("both handlers should have replied to the message", message2);
	}

	@Test
	public void testErrorChannelWithFailedDispatch() throws InterruptedException {
		MessageBus bus = new MessageBus();
		CountDownLatch latch = new CountDownLatch(1);
		SourceEndpoint sourceEndpoint = new SourceEndpoint(new FailingSource(latch), new QueueChannel(), new PollingSchedule(1000));
		bus.registerEndpoint("testEndpoint", sourceEndpoint);
		bus.start();
		latch.await(2000, TimeUnit.MILLISECONDS);
		Message<?> message = bus.getErrorChannel().receive(100);
		assertNotNull("message should not be null", message);
		assertTrue(message instanceof ErrorMessage);
		assertEquals("intentional test failure", ((ErrorMessage) message).getPayload().getMessage());
		bus.stop();
	}

	@Test
	public void testDefaultConcurrencyPolicy() throws InterruptedException {
		MessageBus bus = new MessageBus();
		bus.setDefaultConcurrencyPolicy(new ConcurrencyPolicy(1, 3));
		final CountDownLatch latch = new CountDownLatch(3);
		MessageHandler testHandler = new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				latch.countDown();
				try {
					Thread.sleep(5000);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				return null;
			}
		};
		DispatcherPolicy dispatcherPolicy = new DispatcherPolicy();
		dispatcherPolicy.setRejectionLimit(1);
		dispatcherPolicy.setRetryInterval(0);
		RendezvousChannel testChannel = new RendezvousChannel(dispatcherPolicy);
		bus.registerChannel("testChannel", testChannel);
		bus.registerHandler("testHandler", testHandler, new Subscription(testChannel));
		bus.start();
		for (int i = 0; i < 4; i++) {
			assertTrue(testChannel.send(new StringMessage("test-"+ i), 1000));
		}
		latch.await(1000, TimeUnit.MILLISECONDS);
		assertEquals(0, latch.getCount());
		MessageChannel errorChannel = bus.getErrorChannel();
		Message<?> errorMessage = errorChannel.receive(500);
		assertNotNull(errorMessage);
		assertEquals(MessageDeliveryException.class, errorMessage.getPayload().getClass());
	}

	@Test(expected = BeanCreationException.class)
	public void testMultipleMessageBusBeans() {
		new ClassPathXmlApplicationContext("multipleMessageBusBeans.xml", this.getClass());
	}

	@Test
	public void testErrorChannelRegistration() {
		MessageChannel errorChannel = new QueueChannel();
		MessageBus bus = new MessageBus();
		bus.setErrorChannel(errorChannel);
		assertEquals(errorChannel, bus.getErrorChannel());
	}

	@Test
	public void testHandlerSubscribedToErrorChannel() throws InterruptedException {
		MessageChannel errorChannel = new QueueChannel();
		MessageBus bus = new MessageBus();
		bus.setErrorChannel(errorChannel);
		final CountDownLatch latch = new CountDownLatch(1);
		MessageHandler handler = new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				latch.countDown();
				return null;
			}
		};
		bus.registerHandler("testHandler", handler, new Subscription(MessageBus.ERROR_CHANNEL_NAME));
		bus.start();
		errorChannel.send(new ErrorMessage(new RuntimeException("test-exception")));
		latch.await(1000, TimeUnit.MILLISECONDS);
		assertEquals("handler should have received error message", 0, latch.getCount());
	}

	@Test
	public void testMessageBusAwareImpl() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("messageBusTests.xml", this.getClass());
		TestMessageBusAwareImpl messageBusAwareBean = (TestMessageBusAwareImpl) context.getBean("messageBusAwareBean");
		assertTrue(messageBusAwareBean.getMessageBus() == context.getBean("bus"));
	}

	private static class FailingSource implements Source<Object> {

		private CountDownLatch latch;

		public FailingSource(CountDownLatch latch) {
			this.latch = latch;
		}

		public Message<Object> receive() {
			latch.countDown();
			throw new RuntimeException("intentional test failure");
		}
	}

}
