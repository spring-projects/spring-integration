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
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.Lifecycle;
import org.springframework.integration.channel.DispatcherPolicy;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.MessageEndpointBeanPostProcessor;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.ConcurrencyPolicy;
import org.springframework.integration.endpoint.HandlerEndpoint;
import org.springframework.integration.endpoint.interceptor.ConcurrencyInterceptor;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.handler.MessageHandlerRejectedExecutionException;
import org.springframework.integration.handler.TestHandlers;
import org.springframework.integration.message.ErrorMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.MessageTarget;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.message.selector.PayloadTypeSelector;
import org.springframework.integration.scheduling.MessagePublishingErrorHandler;
import org.springframework.integration.scheduling.SimpleMessagingTaskScheduler;

/**
 * @author Mark Fisher
 */
public class SubscriptionManagerTests {

	private SimpleMessagingTaskScheduler scheduler = new SimpleMessagingTaskScheduler(new ScheduledThreadPoolExecutor(10));


	@Test
	public void testNonBroadcastingDispatcherSendsToExactlyOneEndpoint() throws InterruptedException {
		final AtomicInteger counter1 = new AtomicInteger();
		final AtomicInteger counter2 = new AtomicInteger();
		final CountDownLatch latch = new CountDownLatch(1);
		MessageHandler handler1 = TestHandlers.countingCountDownHandler(counter1, latch);
		MessageHandler handler2 = TestHandlers.countingCountDownHandler(counter2, latch);
		QueueChannel channel = new QueueChannel();
		channel.send(new StringMessage(1, "test"));
		SubscriptionManager manager = new SubscriptionManager(channel, scheduler);
		manager.addTarget(createEndpoint(handler1, true));
		manager.addTarget(createEndpoint(handler2, true));
		manager.start();
		latch.await(2000, TimeUnit.MILLISECONDS);
		assertEquals("messages should have been dispatched within allotted time", 0, latch.getCount());
		assertEquals("exactly one handler should have received message", 1, counter1.get() + counter2.get());
	}

	@Test
	public void testBroadcastingDispatcherSendsToAllEndpoints() throws InterruptedException {
		final AtomicInteger counter1 = new AtomicInteger();
		final AtomicInteger counter2 = new AtomicInteger();
		final CountDownLatch latch = new CountDownLatch(2);
		MessageHandler handler1 = TestHandlers.countingCountDownHandler(counter1, latch);
		MessageHandler handler2 = TestHandlers.countingCountDownHandler(counter2, latch);
		QueueChannel channel = new QueueChannel(5, new DispatcherPolicy(true));
		channel.send(new StringMessage(1, "test"));
		SubscriptionManager manager = new SubscriptionManager(channel, scheduler);
		manager.addTarget(createEndpoint(handler1, true));
		manager.addTarget(createEndpoint(handler2, true));
		manager.start();
		latch.await(2000, TimeUnit.MILLISECONDS);
		assertEquals("messages should have been dispatched within allotted time", 0, latch.getCount());
		assertEquals("both handlers should have received message", 2, counter1.get() + counter2.get());
	}

	@Test
	public void testNonBroadcastingDispatcherSkipsInactiveExecutor() throws InterruptedException {
		final AtomicInteger counter1 = new AtomicInteger();
		final AtomicInteger counter2 = new AtomicInteger();
		final AtomicInteger counter3 = new AtomicInteger();
		final CountDownLatch latch = new CountDownLatch(1);
		MessageHandler handler1 = TestHandlers.countingCountDownHandler(counter1, latch);
		MessageHandler handler2 = TestHandlers.countingCountDownHandler(counter2, latch);
		MessageHandler handler3 = TestHandlers.countingCountDownHandler(counter3, latch);
		QueueChannel channel = new QueueChannel();
		SubscriptionManager manager = new SubscriptionManager(channel, scheduler);
		MessageTarget inactiveEndpoint = createEndpoint(handler1, true);
		manager.addTarget(inactiveEndpoint);
		manager.addTarget(createEndpoint(handler2, true));
		manager.addTarget(createEndpoint(handler3, true));
		manager.start();
		((Lifecycle) inactiveEndpoint).stop();
		channel.send(new StringMessage(1, "test"));
		latch.await(2000, TimeUnit.MILLISECONDS);
		assertEquals("messages should have been dispatched within allotted time", 0, latch.getCount());
		assertEquals("inactive handler should not have received message", 0, counter1.get());
		assertEquals("exactly one handler should have received message", 1, counter2.get() + counter3.get());
	}

	@Test
	public void testBroadcastingDispatcherSkipsInactiveExecutor() throws InterruptedException {
		final AtomicInteger counter1 = new AtomicInteger();
		final AtomicInteger counter2 = new AtomicInteger();
		final AtomicInteger counter3 = new AtomicInteger();
		final CountDownLatch latch = new CountDownLatch(2);
		MessageHandler handler1 = TestHandlers.countingCountDownHandler(counter1, latch);
		MessageHandler handler2 = TestHandlers.countingCountDownHandler(counter2, latch);
		MessageHandler handler3 = TestHandlers.countingCountDownHandler(counter3, latch);
		QueueChannel channel = new QueueChannel(5, new DispatcherPolicy(true));
		SubscriptionManager manager = new SubscriptionManager(channel, scheduler);
		MessageTarget inactiveEndpoint = createEndpoint(handler2, true);
		manager.addTarget(createEndpoint(handler1, true));
		manager.addTarget(inactiveEndpoint);
		manager.addTarget(createEndpoint(handler3, true));
		manager.start();
		((Lifecycle) inactiveEndpoint).stop();
		channel.send(new StringMessage(1, "test"));
		latch.await(2000, TimeUnit.MILLISECONDS);
		assertEquals("messages should have been dispatched within allotted time", 0, latch.getCount());
		assertEquals("inactive handler should not have received message", 0, counter2.get());
		assertEquals("both active handlers should have received message", 2, counter1.get() + counter3.get());
	}

	@Test
	public void testDispatcherWithNoExecutorsDoesNotFail() {
		QueueChannel channel = new QueueChannel();
		channel.send(new StringMessage(1, "test"));
		SubscriptionManager manager = new SubscriptionManager(channel, scheduler);
		manager.start();
	}

	@Test
	public void testBroadcastingDispatcherReachesRejectionLimitAndShouldFail() throws InterruptedException {
		final AtomicInteger counter1 = new AtomicInteger();
		final AtomicInteger counter3 = new AtomicInteger();
		final CountDownLatch latch = new CountDownLatch(2);
		MessageHandler handler1 = TestHandlers.countingCountDownHandler(counter1, latch);
		MessageHandler handler3 = TestHandlers.countingCountDownHandler(counter3, latch);
		QueueChannel channel = new QueueChannel(5, new DispatcherPolicy(true));
		channel.getDispatcherPolicy().setRejectionLimit(2);
		channel.getDispatcherPolicy().setRetryInterval(3);
		channel.send(new StringMessage(1, "test"));
		SubscriptionManager manager = new SubscriptionManager(channel, scheduler);
		manager.addTarget(createEndpoint(handler1, true));
		manager.addTarget(new MessageTarget() {
			public boolean send(Message<?> message) {
				throw new MessageHandlerRejectedExecutionException(message);
			}
		});
		manager.addTarget(createEndpoint(handler3, true));
		QueueChannel errorChannel = new QueueChannel();
		scheduler.setErrorHandler(new MessagePublishingErrorHandler(errorChannel));
		manager.start();
		latch.await(2000, TimeUnit.MILLISECONDS);
		assertEquals("messages should have been dispatched within allotted time", 0, latch.getCount());
		Message<?> errorMessage = errorChannel.receive(1000);
		assertNotNull(errorMessage);
		assertTrue(errorMessage instanceof ErrorMessage);
		assertEquals(MessageDeliveryException.class, ((ErrorMessage) errorMessage).getPayload().getClass());
	}

	@Test
	public void testBroadcastingDispatcherReachesRejectionLimitAndShouldNotFail() throws InterruptedException {
		final AtomicInteger counter1 = new AtomicInteger();
		final AtomicInteger counter2 = new AtomicInteger();
		final CountDownLatch latch = new CountDownLatch(3);
		MessageHandler handler1 = TestHandlers.countingCountDownHandler(counter1, latch);
		MessageHandler handler2 = TestHandlers.countingCountDownHandler(counter2, latch);
		QueueChannel channel = new QueueChannel(5, new DispatcherPolicy(true));
		channel.getDispatcherPolicy().setRejectionLimit(2);
		channel.getDispatcherPolicy().setRetryInterval(3);
		channel.getDispatcherPolicy().setShouldFailOnRejectionLimit(false);
		channel.send(new StringMessage(1, "test"));
		SubscriptionManager manager = new SubscriptionManager(channel, scheduler);
		manager.addTarget(createEndpoint(handler1, false));
		manager.addTarget(createEndpoint(new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				latch.countDown();
				throw new MessageHandlerRejectedExecutionException(message);
			}
		}, false));
		manager.addTarget(createEndpoint(handler2, false));
		manager.start();
		latch.await(2000, TimeUnit.MILLISECONDS);
		assertEquals("messages should have been dispatched within allotted time", 0, latch.getCount());
		assertEquals("both non-rejecting handlers should have received message", 2, counter1.get() + counter2.get());
	}

	@Test
	public void testNonBroadcastingDispatcherReachesRejectionLimitAndShouldFail() throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(4);
		MessageHandler handler1 = TestHandlers.rejectingCountDownHandler(latch);
		MessageHandler handler2 = TestHandlers.rejectingCountDownHandler(latch);
		QueueChannel channel = new QueueChannel(5, new DispatcherPolicy(false));
		channel.send(new StringMessage(1, "test"));
		SubscriptionManager manager = new SubscriptionManager(channel, scheduler);
		channel.getDispatcherPolicy().setRejectionLimit(2);
		channel.getDispatcherPolicy().setRetryInterval(3);
		manager.addTarget(createEndpoint(handler1, false));
		manager.addTarget(createEndpoint(handler2, false));
		QueueChannel errorChannel = new QueueChannel();
		scheduler.setErrorHandler(new MessagePublishingErrorHandler(errorChannel));
		manager.start();
		latch.await(2000, TimeUnit.MILLISECONDS);
		assertEquals("messages should have been dispatched within allotted time", 0, latch.getCount());
		Message<?> errorMessage = errorChannel.receive(500);
		assertNotNull(errorMessage);
		assertTrue(errorMessage instanceof ErrorMessage);
		assertEquals(MessageDeliveryException.class, ((ErrorMessage) errorMessage).getPayload().getClass());
	}

	@Test
	public void testNonBroadcastingDispatcherReachesRejectionLimitButShouldNotFail() throws InterruptedException {
		final AtomicInteger counter1 = new AtomicInteger();
		final AtomicInteger counter2 = new AtomicInteger();
		final AtomicInteger rejectedCounter1 = new AtomicInteger();
		final AtomicInteger rejectedCounter2 = new AtomicInteger();
		final CountDownLatch latch = new CountDownLatch(4);
		QueueChannel channel = new QueueChannel();
		channel.getDispatcherPolicy().setRejectionLimit(2);
		channel.getDispatcherPolicy().setRetryInterval(3);
		channel.getDispatcherPolicy().setShouldFailOnRejectionLimit(false);
		channel.send(new StringMessage(1, "test"));
		SubscriptionManager manager = new SubscriptionManager(channel, scheduler);
		manager.addTarget(createEndpoint(new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				rejectedCounter1.incrementAndGet();
				latch.countDown();
				throw new MessageHandlerRejectedExecutionException(message);
			}
		}, false));
		manager.addTarget(createEndpoint(new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				rejectedCounter2.incrementAndGet();
				latch.countDown();
				throw new MessageHandlerRejectedExecutionException(message);
			}
		}, false));
		manager.start();
		latch.await(2000, TimeUnit.MILLISECONDS);
		assertEquals("messages should have been dispatched within allotted time", 0, latch.getCount());
		assertEquals("latch should have counted down within allotted time", 0, latch.getCount());
		assertEquals("rejecting handlers should not have received message", 0, counter1.get() + counter2.get());
		assertEquals("handler1 should have rejected two times", 2, rejectedCounter1.get());
		assertEquals("handler2 should have rejected two times", 2, rejectedCounter2.get());
	}

	@Test
	public void testNonBroadcastingDispatcherWithOneEndpointSucceeding() throws InterruptedException {
		final AtomicInteger counter1 = new AtomicInteger();
		final AtomicInteger counter2 = new AtomicInteger();
		final AtomicInteger counter3 = new AtomicInteger();
		final AtomicInteger rejectedCounter1 = new AtomicInteger();
		final AtomicInteger rejectedCounter2 = new AtomicInteger();
		final AtomicInteger rejectedCounter3 = new AtomicInteger();
		final CountDownLatch latch = new CountDownLatch(5);
		final MessageHandler handler2 = TestHandlers.countingCountDownHandler(counter2, latch);
		DispatcherPolicy dispatcherPolicy = new DispatcherPolicy();
		dispatcherPolicy.setRejectionLimit(2);
		dispatcherPolicy.setRetryInterval(3);
		dispatcherPolicy.setShouldFailOnRejectionLimit(false);
		QueueChannel channel = new QueueChannel(25, dispatcherPolicy);
		channel.send(new StringMessage(1, "test"));
		SubscriptionManager manager = new SubscriptionManager(channel, scheduler);
		manager.addTarget(createEndpoint(new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				rejectedCounter1.incrementAndGet();
				latch.countDown();
				throw new MessageHandlerRejectedExecutionException(message);
			}
		}, false));
		manager.addTarget(createEndpoint(new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				if (rejectedCounter2.get() == 1) {
					return handler2.handle(message);
				}
				rejectedCounter2.incrementAndGet();
				latch.countDown();
				throw new MessageHandlerRejectedExecutionException(message);
			}
		}, false));
		manager.addTarget(createEndpoint(new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				rejectedCounter3.incrementAndGet();
				latch.countDown();
				throw new MessageHandlerRejectedExecutionException(message);
			}
		}, false));
		manager.start();
		latch.await(2000, TimeUnit.MILLISECONDS);
		assertEquals("messages should have been dispatched within allotted time", 0, latch.getCount());
		assertEquals("handler1 should not have received message", 0, counter1.get());
		assertEquals("handler2 should have received message the second time", 1, counter2.get());
		assertEquals("handler3 should not have received message", 0, counter3.get());
		assertEquals("handler1 should have rejected two times", 2, rejectedCounter1.get());
		assertEquals("handler2 should have rejected one time", 1, rejectedCounter2.get());
		assertEquals("handler3 should have rejected one time", 1, rejectedCounter3.get());
	}

	@Test
	public void testBroadcastingDispatcherStillRetriesRejectedExecutorAfterOtherSucceeds() throws InterruptedException {
		final AtomicInteger counter1 = new AtomicInteger();
		final AtomicInteger counter2 = new AtomicInteger();
		final AtomicInteger rejectedCounter1 = new AtomicInteger();
		final AtomicInteger rejectedCounter2 = new AtomicInteger();
		final CountDownLatch latch = new CountDownLatch(8);
		final MessageHandler handler1 = TestHandlers.countingCountDownHandler(counter1, latch);
		final MessageHandler handler2 = TestHandlers.countingCountDownHandler(counter2, latch);
		DispatcherPolicy dispatcherPolicy = new DispatcherPolicy(true);
		dispatcherPolicy.setRejectionLimit(5);
		dispatcherPolicy.setRetryInterval(3);
		dispatcherPolicy.setShouldFailOnRejectionLimit(false);
		QueueChannel channel = new QueueChannel(25, dispatcherPolicy);
		channel.send(new StringMessage(1, "test"));
		SubscriptionManager manager = new SubscriptionManager(channel, scheduler);
		manager.addTarget(createEndpoint(new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				if (rejectedCounter1.get() == 2) {
					return handler1.handle(message);
				}
				rejectedCounter1.incrementAndGet();
				latch.countDown();
				throw new MessageHandlerRejectedExecutionException(message);
			}
		}, false));
		manager.addTarget(createEndpoint(new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				if (rejectedCounter2.get() == 4) {
					return handler2.handle(message);
				}
				rejectedCounter2.incrementAndGet();
				latch.countDown();
				throw new MessageHandlerRejectedExecutionException(message);
			}
		}, false));
		manager.start();
		latch.await(2000, TimeUnit.MILLISECONDS);
		assertEquals("messages should have been dispatched within allotted time", 0, latch.getCount());
		assertEquals("handler1 should have received one message", 1, counter1.get());
		assertEquals("handler2 should have received one message", 1, counter2.get());
		assertEquals("handler1 should have rejected two times", 2, rejectedCounter1.get());
		assertEquals("handler2 should have rejected four times", 4, rejectedCounter2.get());
	}

	@Test
	public void testTwoExecutorsWithSelectorsAndOneAccepts() throws InterruptedException {
		final AtomicInteger counter1 = new AtomicInteger();
		final AtomicInteger counter2 = new AtomicInteger();
		final CountDownLatch latch = new CountDownLatch(1);
		MessageHandler handler1 = TestHandlers.countingCountDownHandler(counter1, latch);
		MessageHandler handler2 = TestHandlers.countingCountDownHandler(counter2, latch);
		QueueChannel channel = new QueueChannel();
		channel.send(new StringMessage(1, "test"));
		SubscriptionManager manager = new SubscriptionManager(channel, scheduler);
		HandlerEndpoint endpoint1 = new HandlerEndpoint(handler1);
		HandlerEndpoint endpoint2 = new HandlerEndpoint(handler2);
		endpoint1.setMessageSelector(new PayloadTypeSelector(Integer.class));
		endpoint2.setMessageSelector(new PayloadTypeSelector(String.class));
		manager.addTarget(endpoint1);
		manager.addTarget(endpoint2);
		manager.start();
		latch.await(2000, TimeUnit.MILLISECONDS);
		assertEquals("messages should have been dispatched within allotted time", 0, latch.getCount());
		assertEquals("handler1 should not have accepted the message", 0, counter1.get());
		assertEquals("handler2 should have accepted the message", 1, counter2.get());
	}

	@Test
	public void testTwoExecutorsWithSelectorsAndNeitherAccepts() throws InterruptedException {
		final AtomicInteger counter1 = new AtomicInteger();
		final AtomicInteger counter2 = new AtomicInteger();
		final AtomicInteger selectorCounter1 = new AtomicInteger();
		final AtomicInteger selectorCounter2 = new AtomicInteger();
		final CountDownLatch selectorLatch = new CountDownLatch(2);
		final CountDownLatch handlerLatch = new CountDownLatch(1);
		MessageHandler handler1 = TestHandlers.countingCountDownHandler(counter1, handlerLatch);
		MessageHandler handler2 = TestHandlers.countingCountDownHandler(counter2, handlerLatch);
		QueueChannel channel = new QueueChannel();
		channel.send(new StringMessage(1, "test"));
		SubscriptionManager manager = new SubscriptionManager(channel, scheduler);
		final HandlerEndpoint endpoint1 = new HandlerEndpoint(handler1);
		final HandlerEndpoint endpoint2 = new HandlerEndpoint(handler2);
		endpoint1.setMessageSelector(new PayloadTypeSelector(Integer.class) {
			@Override
			public boolean accept(Message<?> message) {
				selectorCounter1.incrementAndGet();
				selectorLatch.countDown();
				return super.accept(message);
			}
		});
		endpoint2.setMessageSelector(new PayloadTypeSelector(Integer.class) {
			@Override
			public boolean accept(Message<?> message) {
				selectorCounter2.incrementAndGet();
				selectorLatch.countDown();
				return super.accept(message);
			}
		});
		manager.addTarget(endpoint1);
		manager.addTarget(endpoint2);
		manager.start();
		selectorLatch.await(2000, TimeUnit.MILLISECONDS);
		assertEquals("messages should have been dispatched within allotted time", 0, selectorLatch.getCount());
		assertEquals("handler1 should not have accepted the message", 0, counter1.get());
		assertEquals("handler2 should not have accepted the message", 0, counter2.get());
		assertEquals("executor1 should have had exactly one attempt", 1, selectorCounter1.get());
		assertEquals("executor2 should have had exactly one attempt", 1, selectorCounter2.get());
		assertEquals("handlerLatch should not have counted down", 1, handlerLatch.getCount());
	}

	@Test
	public void testBroadcastingDispatcherWithSelectorsAndOneAccepts() throws InterruptedException {
		final AtomicInteger counter1 = new AtomicInteger();
		final AtomicInteger counter2 = new AtomicInteger();
		final CountDownLatch latch = new CountDownLatch(1);
		MessageHandler handler1 = TestHandlers.countingCountDownHandler(counter1, latch);
		MessageHandler handler2 = TestHandlers.countingCountDownHandler(counter2, latch);
		QueueChannel channel = new QueueChannel(5, new DispatcherPolicy(true));
		channel.send(new StringMessage(1, "test"));
		SubscriptionManager manager = new SubscriptionManager(channel, scheduler);
		HandlerEndpoint endpoint1 = new HandlerEndpoint(handler1);
		HandlerEndpoint endpoint2 = new HandlerEndpoint(handler2);
		endpoint1.setMessageSelector(new PayloadTypeSelector(Integer.class));
		endpoint2.setMessageSelector(new PayloadTypeSelector(String.class));
		manager.addTarget(endpoint1);
		manager.addTarget(endpoint2);
		manager.start();
		latch.await(2000, TimeUnit.MILLISECONDS);
		assertEquals("messages should have been dispatched within allotted time", 0, latch.getCount());
		assertEquals("endpoint1 should not have accepted the message", 0, counter1.get());
		assertEquals("endpoint2 should have accepted the message", 1, counter2.get());
	}


	private static MessageTarget createEndpoint(MessageHandler handler, boolean asynchronous) {
		MessageTarget endpoint = new HandlerEndpoint(handler);
		if (asynchronous) {
			MessageEndpointBeanPostProcessor postProcessor = new MessageEndpointBeanPostProcessor();
			((AbstractEndpoint) endpoint).addInterceptor(new ConcurrencyInterceptor(new ConcurrencyPolicy(1, 1)));
			endpoint = (MessageTarget) postProcessor.postProcessAfterInitialization(endpoint, "test-endpoint");
		}
		try {
			((InitializingBean) endpoint).afterPropertiesSet();
		}
		catch (Exception e) {
			throw new RuntimeException("failed to initialize endpoint", e);
		}
		return endpoint;
	}

}
