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
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.channel.PollableChannelAdapter;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.DefaultEndpoint;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.ErrorMessage;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.message.PollableSource;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class DefaultMessageBusTests {

	@Test
	public void testRegistrationWithInputChannelReference() {
		DefaultMessageBus bus = new DefaultMessageBus();
		QueueChannel sourceChannel = new QueueChannel();
		QueueChannel targetChannel = new QueueChannel();
		sourceChannel.setBeanName("sourceChannel");
		targetChannel.setBeanName("targetChannel");
		bus.registerChannel(sourceChannel);
		Message<String> message = MessageBuilder.fromPayload("test")
				.setReturnAddress("targetChannel").build();
		sourceChannel.send(message);
		bus.registerChannel(targetChannel);
		MessageHandler handler = new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				return message;
			}
		};
		DefaultEndpoint<MessageHandler> endpoint = new DefaultEndpoint<MessageHandler>(handler);
		endpoint.setBeanName("testEndpoint");
		endpoint.setSource(sourceChannel);
		bus.registerEndpoint(endpoint);
		bus.start();
		Message<?> result = targetChannel.receive(3000);
		assertEquals("test", result.getPayload());
		bus.stop();
	}

	@Test
	public void testRegistrationWithInputChannelName() {
		MessageBus bus = new DefaultMessageBus();
		QueueChannel sourceChannel = new QueueChannel();
		QueueChannel targetChannel = new QueueChannel();
		sourceChannel.setBeanName("sourceChannel");
		targetChannel.setBeanName("targetChannel");
		bus.registerChannel(sourceChannel);
		Message<String> message = MessageBuilder.fromPayload("test")
				.setReturnAddress("targetChannel").build();
		sourceChannel.send(message);
		bus.registerChannel(targetChannel);
		MessageHandler handler = new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				return message;
			}
		};
		DefaultEndpoint<MessageHandler> endpoint = new DefaultEndpoint<MessageHandler>(handler);
		endpoint.setBeanName("testEndpoint");
		endpoint.setInputChannelName("sourceChannel");
		bus.registerEndpoint(endpoint);
		bus.start();
		Message<?> result = targetChannel.receive(3000);
		assertEquals("test", result.getPayload());
		bus.stop();
	}

	@Test
	public void testChannelsWithoutHandlers() {
		MessageBus bus = new DefaultMessageBus();
		QueueChannel sourceChannel = new QueueChannel();
		sourceChannel.setBeanName("sourceChannel");
		sourceChannel.send(new StringMessage("test"));
		QueueChannel targetChannel = new QueueChannel();
		targetChannel.setBeanName("targetChannel");
		bus.registerChannel(sourceChannel);
		bus.registerChannel(targetChannel);
		bus.start();
		Message<?> result = targetChannel.receive(100);
		assertNull(result);
		bus.stop();
	}

	@Test
	public void testAutodetectionWithApplicationContext() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("messageBusTests.xml", this.getClass());
		context.start();
		PollableChannel sourceChannel = (PollableChannel) context.getBean("sourceChannel");
		sourceChannel.send(new GenericMessage<String>("test"));		
		PollableChannel targetChannel = (PollableChannel) context.getBean("targetChannel");
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
				return MessageBuilder.fromMessage(message)
						.setNextTarget("output1").build();
			}
		};
		MessageHandler handler2 = new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				return MessageBuilder.fromMessage(message)
						.setNextTarget("output2").build();
			}
		};
		MessageBus bus = new DefaultMessageBus();
		inputChannel.setBeanName("input");
		outputChannel1.setBeanName("output1");
		outputChannel2.setBeanName("output2");
		bus.registerChannel(inputChannel);
		bus.registerChannel(outputChannel1);
		bus.registerChannel(outputChannel2);
		DefaultEndpoint<MessageHandler> endpoint1 = new DefaultEndpoint<MessageHandler>(handler1);
		endpoint1.setBeanName("testEndpoint1");
		endpoint1.setSource(inputChannel);
		DefaultEndpoint<MessageHandler> endpoint2 = new DefaultEndpoint<MessageHandler>(handler2);
		endpoint2.setBeanName("testEndpoint2");
		endpoint2.setSource(inputChannel);
		bus.registerEndpoint(endpoint1);
		bus.registerEndpoint(endpoint2);
		bus.start();
		inputChannel.send(new StringMessage("testing"));
		Message<?> message1 = outputChannel1.receive(500);
		Message<?> message2 = outputChannel2.receive(0);
		bus.stop();
		assertTrue("exactly one message should be null", message1 == null ^ message2 == null);
	}

	@Test
	public void testBothHandlersReceivePublishSubscribeMessage() throws InterruptedException {
		PublishSubscribeChannel inputChannel = new PublishSubscribeChannel();
		QueueChannel outputChannel1 = new QueueChannel();
		QueueChannel outputChannel2 = new QueueChannel();
		final CountDownLatch latch = new CountDownLatch(2);
		MessageHandler handler1 = new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				Message<?> reply = MessageBuilder.fromMessage(message)
						.setNextTarget("output1").build();
				latch.countDown();
				return reply;
			}
		};
		MessageHandler handler2 = new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				Message<?> reply = MessageBuilder.fromMessage(message)
						.setNextTarget("output2").build();
				latch.countDown();
				return reply;
			}
		};
		MessageBus bus = new DefaultMessageBus();
		inputChannel.setBeanName("input");
		outputChannel1.setBeanName("output1");
		outputChannel2.setBeanName("output2");
		bus.registerChannel(inputChannel);
		bus.registerChannel(outputChannel1);
		bus.registerChannel(outputChannel2);
		DefaultEndpoint<MessageHandler> endpoint1 = new DefaultEndpoint<MessageHandler>(handler1);
		endpoint1.setBeanName("testEndpoint1");
		endpoint1.setSource(inputChannel);
		DefaultEndpoint<MessageHandler> endpoint2 = new DefaultEndpoint<MessageHandler>(handler2);
		endpoint2.setBeanName("testEndpoint2");
		endpoint2.setSource(inputChannel);
		bus.registerEndpoint(endpoint1);
		bus.registerEndpoint(endpoint2);
		bus.start();
		inputChannel.send(new StringMessage("testing"));
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
		MessageBus bus = new DefaultMessageBus();
		CountDownLatch latch = new CountDownLatch(1);
		PollableChannelAdapter channelAdapter = new PollableChannelAdapter(
				"testChannel", new FailingSource(latch), null);
		MessageHandler handler = new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				return message;
			}
		};
		DefaultEndpoint<MessageHandler> endpoint = new DefaultEndpoint<MessageHandler>(handler);
		endpoint.setBeanName("testEndpoint");
		endpoint.setSource(channelAdapter);
		bus.registerEndpoint(endpoint);
		bus.start();
		latch.await(2000, TimeUnit.MILLISECONDS);
		Message<?> message = ((PollableChannel) bus.getErrorChannel()).receive(5000);
		bus.stop();
		assertNotNull("message should not be null", message);
		assertTrue(message instanceof ErrorMessage);
		Throwable exception = ((ErrorMessage) message).getPayload();
		assertTrue(exception instanceof MessagingException);
		assertEquals("intentional test failure", exception.getCause().getMessage());
	}

	@Test(expected = BeanCreationException.class)
	public void testMultipleMessageBusBeans() {
		new ClassPathXmlApplicationContext("multipleMessageBusBeans.xml", this.getClass());
	}

	@Test
	public void testErrorChannelRegistration() {
		DefaultMessageBus bus = new DefaultMessageBus();
		QueueChannel errorChannel = new QueueChannel();
		errorChannel.setBeanName(ChannelRegistry.ERROR_CHANNEL_NAME);
		bus.registerChannel(errorChannel);
		assertEquals(errorChannel, bus.getErrorChannel());
	}

	@Test
	public void testHandlerSubscribedToErrorChannel() throws InterruptedException {
		DefaultMessageBus bus = new DefaultMessageBus();
		QueueChannel errorChannel = new QueueChannel();
		errorChannel.setBeanName(ChannelRegistry.ERROR_CHANNEL_NAME);
		bus.registerChannel(errorChannel);
		final CountDownLatch latch = new CountDownLatch(1);
		MessageHandler handler = new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				latch.countDown();
				return null;
			}
		};
		DefaultEndpoint<MessageHandler> endpoint = new DefaultEndpoint<MessageHandler>(handler);
		endpoint.setBeanName("testEndpoint");
		endpoint.setInputChannelName(MessageBus.ERROR_CHANNEL_NAME);
		bus.registerEndpoint(endpoint);
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


	private static class FailingSource implements PollableSource<Object> {

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
