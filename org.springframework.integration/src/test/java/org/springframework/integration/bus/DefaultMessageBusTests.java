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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.xml.MessageBusParser;
import org.springframework.integration.endpoint.AbstractReplyProducingMessageConsumer;
import org.springframework.integration.endpoint.PollingConsumerEndpoint;
import org.springframework.integration.endpoint.ReplyHolder;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.endpoint.SubscribingConsumerEndpoint;
import org.springframework.integration.message.ErrorMessage;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageSource;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.scheduling.IntervalTrigger;
import org.springframework.integration.util.TestUtils;

/**
 * @author Mark Fisher
 */
public class DefaultMessageBusTests {

	@Test
	public void endpointRegistrationWithInputChannelReference() {
		GenericApplicationContext context = new GenericApplicationContext();
		QueueChannel sourceChannel = new QueueChannel();
		QueueChannel targetChannel = new QueueChannel();
		sourceChannel.setBeanName("sourceChannel");
		targetChannel.setBeanName("targetChannel");
		context.getBeanFactory().registerSingleton("sourceChannel", sourceChannel);
		context.getBeanFactory().registerSingleton("targetChannel", targetChannel);
		Message<String> message = MessageBuilder.withPayload("test")
				.setReturnAddress("targetChannel").build();
		sourceChannel.send(message);
		AbstractReplyProducingMessageConsumer consumer = new AbstractReplyProducingMessageConsumer() {
			public void handle(Message<?> message, ReplyHolder replyHolder) {
				replyHolder.set(message);
			}
		};
		consumer.setBeanFactory(context);
		PollingConsumerEndpoint endpoint = new PollingConsumerEndpoint(consumer, sourceChannel);
		endpoint.afterPropertiesSet();
		context.getBeanFactory().registerSingleton("testEndpoint", endpoint);
		context.refresh();
		DefaultMessageBus bus = new DefaultMessageBus();
		bus.setTaskScheduler(TestUtils.createTaskScheduler(10));
		context.getBeanFactory().registerSingleton(MessageBusParser.MESSAGE_BUS_BEAN_NAME, bus);
		bus.setApplicationContext(context);
		bus.start();
		Message<?> result = targetChannel.receive(3000);
		assertEquals("test", result.getPayload());
		bus.stop();
	}

	@Test
	public void channelsWithoutHandlers() {
		GenericApplicationContext context = new GenericApplicationContext();
		DefaultMessageBus bus = new DefaultMessageBus();
		bus.setTaskScheduler(TestUtils.createTaskScheduler(10));
		bus.setApplicationContext(context);
		QueueChannel sourceChannel = new QueueChannel();
		sourceChannel.setBeanName("sourceChannel");
		context.getBeanFactory().registerSingleton("sourceChannel", sourceChannel);
		sourceChannel.send(new StringMessage("test"));
		QueueChannel targetChannel = new QueueChannel();
		targetChannel.setBeanName("targetChannel");
		context.getBeanFactory().registerSingleton("targetChannel", targetChannel);
		bus.start();
		Message<?> result = targetChannel.receive(100);
		assertNull(result);
		bus.stop();
	}

	@Test
	public void autodetectionWithApplicationContext() {
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
	public void exactlyOneConsumerReceivesPointToPointMessage() {
		GenericApplicationContext context = new GenericApplicationContext();
		QueueChannel inputChannel = new QueueChannel();
		QueueChannel outputChannel1 = new QueueChannel();
		QueueChannel outputChannel2 = new QueueChannel();
		AbstractReplyProducingMessageConsumer consumer1 = new AbstractReplyProducingMessageConsumer() {
			public void handle(Message<?> message, ReplyHolder replyHolder) {
				replyHolder.set(message);
			}
		};
		AbstractReplyProducingMessageConsumer consumer2 = new AbstractReplyProducingMessageConsumer() {
			public void handle(Message<?> message, ReplyHolder replyHolder) {
				replyHolder.set(message);
			}
		};
		inputChannel.setBeanName("input");
		outputChannel1.setBeanName("output1");
		outputChannel2.setBeanName("output2");
		context.getBeanFactory().registerSingleton("input", inputChannel);
		context.getBeanFactory().registerSingleton("output1", outputChannel1);
		context.getBeanFactory().registerSingleton("output2", outputChannel2);
		consumer1.setOutputChannel(outputChannel1);
		consumer2.setOutputChannel(outputChannel2);
		PollingConsumerEndpoint endpoint1 = new PollingConsumerEndpoint(consumer1, inputChannel);
		endpoint1.afterPropertiesSet();
		PollingConsumerEndpoint endpoint2 = new PollingConsumerEndpoint(consumer2, inputChannel);
		endpoint2.afterPropertiesSet();
		context.getBeanFactory().registerSingleton("testEndpoint1", endpoint1);
		context.getBeanFactory().registerSingleton("testEndpoint2", endpoint2);
		DefaultMessageBus bus = new DefaultMessageBus();
		bus.setTaskScheduler(TestUtils.createTaskScheduler(10));
		bus.setApplicationContext(context);
		bus.start();
		inputChannel.send(new StringMessage("testing"));
		Message<?> message1 = outputChannel1.receive(500);
		Message<?> message2 = outputChannel2.receive(0);
		bus.stop();
		assertTrue("exactly one message should be null", message1 == null ^ message2 == null);
	}

	@Test
	public void bothConsumersReceivePublishSubscribeMessage() throws InterruptedException {
		GenericApplicationContext context = new GenericApplicationContext();
		PublishSubscribeChannel inputChannel = new PublishSubscribeChannel();
		QueueChannel outputChannel1 = new QueueChannel();
		QueueChannel outputChannel2 = new QueueChannel();
		final CountDownLatch latch = new CountDownLatch(2);
		AbstractReplyProducingMessageConsumer consumer1 = new AbstractReplyProducingMessageConsumer() {
			public void handle(Message<?> message, ReplyHolder replyHolder) {
				replyHolder.set(message);
				latch.countDown();
			}
		};
		AbstractReplyProducingMessageConsumer consumer2 = new AbstractReplyProducingMessageConsumer() {
			public void handle(Message<?> message, ReplyHolder replyHolder) {
				replyHolder.set(message);
				latch.countDown();
			}
		};
		inputChannel.setBeanName("input");
		outputChannel1.setBeanName("output1");
		outputChannel2.setBeanName("output2");
		context.getBeanFactory().registerSingleton("input", inputChannel);
		context.getBeanFactory().registerSingleton("output1", outputChannel1);
		context.getBeanFactory().registerSingleton("output2", outputChannel2);
		consumer1.setOutputChannel(outputChannel1);
		consumer2.setOutputChannel(outputChannel2);
		SubscribingConsumerEndpoint endpoint1 = new SubscribingConsumerEndpoint(consumer1, inputChannel);
		SubscribingConsumerEndpoint endpoint2 = new SubscribingConsumerEndpoint(consumer2, inputChannel);
		context.getBeanFactory().registerSingleton("testEndpoint1", endpoint1);
		context.getBeanFactory().registerSingleton("testEndpoint2", endpoint2);
		DefaultMessageBus bus = new DefaultMessageBus();
		bus.setTaskScheduler(TestUtils.createTaskScheduler(10));
		bus.setApplicationContext(context);
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
	public void errorChannelWithFailedDispatch() throws InterruptedException {
		GenericApplicationContext context = new GenericApplicationContext();
		QueueChannel errorChannel = new QueueChannel();
		QueueChannel outputChannel = new QueueChannel();
		errorChannel.setBeanName("errorChannel");
		context.getBeanFactory().registerSingleton("errorChannel", errorChannel);
		CountDownLatch latch = new CountDownLatch(1);
		SourcePollingChannelAdapter channelAdapter = new SourcePollingChannelAdapter();
		channelAdapter.setSource(new FailingSource(latch));
		channelAdapter.setTrigger(new IntervalTrigger(1000));
		channelAdapter.setOutputChannel(outputChannel);
		channelAdapter.setBeanName("testChannel");
		context.getBeanFactory().registerSingleton("testChannel", channelAdapter);
		DefaultMessageBus bus = new DefaultMessageBus();
		bus.setTaskScheduler(TestUtils.createTaskScheduler(10));
		bus.setApplicationContext(context);
		bus.start();
		latch.await(2000, TimeUnit.MILLISECONDS);
		Message<?> message = errorChannel.receive(5000);
		bus.stop();
		assertNull(outputChannel.receive(0));
		assertNotNull("message should not be null", message);
		assertTrue(message instanceof ErrorMessage);
		Throwable exception = ((ErrorMessage) message).getPayload();
		assertEquals("intentional test failure", exception.getMessage());
	}

	@Test(expected = BeanCreationException.class)
	public void multipleMessageBusBeans() {
		new ClassPathXmlApplicationContext("multipleMessageBusBeans.xml", this.getClass());
	}

	@Test
	public void consumerSubscribedToErrorChannel() throws InterruptedException {
		GenericApplicationContext context = new GenericApplicationContext();
		QueueChannel errorChannel = new QueueChannel();
		errorChannel.setBeanName(DefaultMessageBus.ERROR_CHANNEL_BEAN_NAME);
		context.getBeanFactory().registerSingleton(DefaultMessageBus.ERROR_CHANNEL_BEAN_NAME, errorChannel);
		final CountDownLatch latch = new CountDownLatch(1);
		AbstractReplyProducingMessageConsumer consumer = new AbstractReplyProducingMessageConsumer() {
			public void handle(Message<?> message, ReplyHolder replyHolder) {
				latch.countDown();
			}
		};
		PollingConsumerEndpoint endpoint = new PollingConsumerEndpoint(consumer, errorChannel);
		endpoint.afterPropertiesSet();
		context.getBeanFactory().registerSingleton("testEndpoint", endpoint);
		DefaultMessageBus bus = new DefaultMessageBus();
		bus.setTaskScheduler(TestUtils.createTaskScheduler(10));
		bus.setApplicationContext(context);
		bus.start();
		errorChannel.send(new ErrorMessage(new RuntimeException("test-exception")));
		latch.await(1000, TimeUnit.MILLISECONDS);
		assertEquals("handler should have received error message", 0, latch.getCount());
	}

	@Test
	public void lookupRegisteredChannel() {
		GenericApplicationContext context = new GenericApplicationContext();
		QueueChannel testChannel = new QueueChannel();
		testChannel.setBeanName("testChannel");
		context.getBeanFactory().registerSingleton("testChannel", testChannel);
		DefaultMessageBus messageBus = new DefaultMessageBus();
		messageBus.setApplicationContext(context);
		MessageChannel lookedUpChannel = messageBus.lookupChannel("testChannel");
		assertNotNull(testChannel);
		assertSame(testChannel, lookedUpChannel);
	}

	@Test
	public void lookupNonRegisteredChannel() {
		GenericApplicationContext context = new GenericApplicationContext();
		DefaultMessageBus messageBus = new DefaultMessageBus();
		messageBus.setApplicationContext(context);
		MessageChannel noSuchChannel = messageBus.lookupChannel("noSuchChannel");
		assertNull(noSuchChannel);
	}

	@Test
	public void messageBusAware() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("messageBusTests.xml", this.getClass());
		TestMessageBusAwareImpl messageBusAwareBean = (TestMessageBusAwareImpl) context.getBean("messageBusAwareBean");
		assertTrue(messageBusAwareBean.getMessageBus() == context.getBean("bus"));
	}


	private static class FailingSource implements MessageSource<Object> {

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
