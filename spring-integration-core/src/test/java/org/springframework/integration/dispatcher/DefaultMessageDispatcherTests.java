/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.dispatcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.channel.SimpleChannel;
import org.springframework.integration.dispatcher.DefaultMessageDispatcher;
import org.springframework.integration.dispatcher.MessageHandlerRejectedExecutionException;
import org.springframework.integration.endpoint.ConcurrencyPolicy;
import org.springframework.integration.endpoint.DefaultMessageEndpoint;
import org.springframework.integration.handler.ConcurrentHandler;
import org.springframework.integration.handler.InterceptingMessageHandler;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.handler.TestHandlers;
import org.springframework.integration.message.ErrorMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.message.selector.PayloadTypeSelector;
import org.springframework.integration.scheduling.MessagePublishingErrorHandler;
import org.springframework.integration.scheduling.SimpleMessagingTaskScheduler;

/**
 * @author Mark Fisher
 */
public class DefaultMessageDispatcherTests {

	@Test
	public void testNonBroadcastingDispatcherSendsToExactlyOneEndpoint() throws InterruptedException {
		final AtomicInteger counter1 = new AtomicInteger();
		final AtomicInteger counter2 = new AtomicInteger();
		final CountDownLatch latch = new CountDownLatch(1);
		MessageHandler handler1 = TestHandlers.countingCountDownHandler(counter1, latch);
		MessageHandler handler2 = TestHandlers.countingCountDownHandler(counter2, latch);
		SimpleChannel channel = new SimpleChannel();
		channel.send(new StringMessage(1, "test"));
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(channel);
		dispatcher.addHandler(new ConcurrentHandler(handler1, 1, 1));
		dispatcher.addHandler(new ConcurrentHandler(handler2, 1, 1));
		SimpleMessagingTaskScheduler scheduler = new SimpleMessagingTaskScheduler();
		scheduler.start();
		dispatcher.setMessagingTaskScheduler(scheduler);
		dispatcher.start();
		latch.await(500, TimeUnit.MILLISECONDS);
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
		SimpleChannel channel = new SimpleChannel();
		channel.setPublishSubscribe(true);
		channel.send(new StringMessage(1, "test"));
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(channel);
		dispatcher.addHandler(new ConcurrentHandler(handler1, 1, 1));
		dispatcher.addHandler(new ConcurrentHandler(handler2, 1, 1));
		dispatcher.start();
		latch.await(500, TimeUnit.MILLISECONDS);
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
		SimpleChannel channel = new SimpleChannel();
		channel.send(new StringMessage(1, "test"));
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(channel);
		dispatcher.addHandler(new ConcurrentHandler(handler1, 1, 1) {
			@Override
			public void start() {
			}
		});
		dispatcher.addHandler(new ConcurrentHandler(handler2, 1, 1));
		dispatcher.addHandler(new ConcurrentHandler(handler3, 1, 1));
		dispatcher.start();
		latch.await(500, TimeUnit.MILLISECONDS);
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
		SimpleChannel channel = new SimpleChannel();
		channel.setPublishSubscribe(true);
		channel.send(new StringMessage(1, "test"));
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(channel);
		dispatcher.addHandler(new ConcurrentHandler(handler1, 1, 1));
		dispatcher.addHandler(new ConcurrentHandler(handler2, 1, 1) {
			@Override
			public void start() {
			}
		});
		dispatcher.addHandler(new ConcurrentHandler(handler3, 1, 1));
		dispatcher.start();
		latch.await(500, TimeUnit.MILLISECONDS);
		assertEquals("messages should have been dispatched within allotted time", 0, latch.getCount());
		assertEquals("inactive handler should not have received message", 0, counter2.get());
		assertEquals("both active handlers should have received message", 2, counter1.get() + counter3.get());
	}

	@Test
	public void testDispatcherWithNoExecutorsDoesNotFail() {
		SimpleChannel channel = new SimpleChannel();
		channel.send(new StringMessage(1, "test"));
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(channel);
		dispatcher.start();
	}

	@Test
	public void testBroadcastingDispatcherReachesRejectionLimitAndShouldFail() throws InterruptedException {
		final AtomicInteger counter1 = new AtomicInteger();
		final AtomicInteger counter2 = new AtomicInteger();
		final AtomicInteger counter3 = new AtomicInteger();
		final CountDownLatch latch = new CountDownLatch(2);
		MessageHandler handler1 = TestHandlers.countingCountDownHandler(counter1, latch);
		MessageHandler handler2 = TestHandlers.countingCountDownHandler(counter2, latch);
		MessageHandler handler3 = TestHandlers.countingCountDownHandler(counter3, latch);
		SimpleChannel channel = new SimpleChannel();
		channel.setPublishSubscribe(true);
		channel.send(new StringMessage(1, "test"));
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(channel);
		dispatcher.setRejectionLimit(2);
		dispatcher.setRetryInterval(3);
		dispatcher.addHandler(new ConcurrentHandler(handler1, 1, 1));
		dispatcher.addHandler(new ConcurrentHandler(handler2, 1, 1) {
			@Override
			public Message<?> handle(Message<?> message) {
				throw new MessageHandlerRejectedExecutionException();
			}
		});
		dispatcher.addHandler(new ConcurrentHandler(handler3, 1, 1));
		SimpleChannel errorChannel = new SimpleChannel();
		SimpleMessagingTaskScheduler scheduler = new SimpleMessagingTaskScheduler();
		scheduler.setErrorHandler(new MessagePublishingErrorHandler(errorChannel));
		dispatcher.setMessagingTaskScheduler(scheduler);
		dispatcher.start();
		latch.await(500, TimeUnit.MILLISECONDS);
		assertEquals("messages should have been dispatched within allotted time", 0, latch.getCount());
		Message<?> errorMessage = errorChannel.receive(500);
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
		SimpleChannel channel = new SimpleChannel();
		channel.setPublishSubscribe(true);
		channel.send(new StringMessage(1, "test"));
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(channel);
		dispatcher.setRejectionLimit(2);
		dispatcher.setRetryInterval(3);
		dispatcher.setShouldFailOnRejectionLimit(false);
		dispatcher.addHandler(handler1);
		dispatcher.addHandler(new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				latch.countDown();
				throw new MessageHandlerRejectedExecutionException();
			}
		});
		dispatcher.addHandler(handler2);
		dispatcher.start();
		latch.await(500, TimeUnit.MILLISECONDS);
		assertEquals("messages should have been dispatched within allotted time", 0, latch.getCount());
		assertEquals("both non-rejecting handlers should have received message", 2, counter1.get() + counter2.get());
	}

	@Test
	public void testNonBroadcastingDispatcherReachesRejectionLimitAndShouldFail() throws InterruptedException {
		final AtomicInteger counter1 = new AtomicInteger();
		final AtomicInteger counter2 = new AtomicInteger();
		final CountDownLatch latch = new CountDownLatch(4);
		MessageHandler handler1 = TestHandlers.countingCountDownHandler(counter1, latch);
		MessageHandler handler2 = TestHandlers.countingCountDownHandler(counter2, latch);
		SimpleChannel channel = new SimpleChannel();
		channel.send(new StringMessage(1, "test"));
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(channel);
		dispatcher.setRejectionLimit(2);
		dispatcher.setRetryInterval(3);
		dispatcher.addHandler(new ConcurrentHandler(handler1, 1, 1) {
			@Override
			public Message<?> handle(Message<?> message) {
				latch.countDown();
				throw new MessageHandlerRejectedExecutionException();
			}
		});
		dispatcher.addHandler(new ConcurrentHandler(handler2, 1, 1) {
			@Override
			public Message<?> handle(Message<?> message) {
				latch.countDown();
				throw new MessageHandlerRejectedExecutionException();
			}
		});
		SimpleChannel errorChannel = new SimpleChannel();
		SimpleMessagingTaskScheduler scheduler = new SimpleMessagingTaskScheduler();
		scheduler.setErrorHandler(new MessagePublishingErrorHandler(errorChannel));
		dispatcher.setMessagingTaskScheduler(scheduler);
		dispatcher.start();
		latch.await(500, TimeUnit.MILLISECONDS);
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
		MessageHandler handler1 = TestHandlers.countingCountDownHandler(counter1, latch);
		MessageHandler handler2 = TestHandlers.countingCountDownHandler(counter2, latch);
		SimpleChannel channel = new SimpleChannel();
		channel.send(new StringMessage(1, "test"));
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(channel);
		dispatcher.setRejectionLimit(2);
		dispatcher.setRetryInterval(3);
		dispatcher.setShouldFailOnRejectionLimit(false);
		dispatcher.addHandler(new ConcurrentHandler(handler1, 1, 1) {
			@Override
			public Message<?> handle(Message<?> message) {
				rejectedCounter1.incrementAndGet();
				latch.countDown();
				throw new MessageHandlerRejectedExecutionException();
			}
		});
		dispatcher.addHandler(new ConcurrentHandler(handler2, 1, 1) {
			@Override
			public Message<?> handle(Message<?> message) {
				rejectedCounter2.incrementAndGet();
				latch.countDown();
				throw new MessageHandlerRejectedExecutionException();
			}
		});
		dispatcher.start();
		latch.await(500, TimeUnit.MILLISECONDS);
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
		MessageHandler handler1 = TestHandlers.countingCountDownHandler(counter1, latch);
		MessageHandler handler2 = TestHandlers.countingCountDownHandler(counter2, latch);
		MessageHandler handler3 = TestHandlers.countingCountDownHandler(counter3, latch);
		SimpleChannel channel = new SimpleChannel();
		channel.send(new StringMessage(1, "test"));
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(channel);
		dispatcher.setRejectionLimit(2);
		dispatcher.setRetryInterval(3);
		dispatcher.setShouldFailOnRejectionLimit(false);
		dispatcher.addHandler(new ConcurrentHandler(handler1, 1, 1) {
			@Override
			public Message<?> handle(Message<?> message) {
				rejectedCounter1.incrementAndGet();
				latch.countDown();
				throw new MessageHandlerRejectedExecutionException();
			}
		});
		dispatcher.addHandler(new ConcurrentHandler(handler2, 1, 1) {
			@Override
			public Message<?> handle(Message<?> message) {
				if (rejectedCounter2.get() == 1) {
					return super.handle(message);
				}
				rejectedCounter2.incrementAndGet();
				latch.countDown();
				throw new MessageHandlerRejectedExecutionException();
			}
		});
		dispatcher.addHandler(new ConcurrentHandler(handler3, 1, 1) {
			@Override
			public Message<?> handle(Message<?> message) {
				rejectedCounter3.incrementAndGet();
				latch.countDown();
				throw new MessageHandlerRejectedExecutionException();
			}
		});
		dispatcher.start();
		latch.await(500, TimeUnit.MILLISECONDS);
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
		MessageHandler handler1 = TestHandlers.countingCountDownHandler(counter1, latch);
		MessageHandler handler2 = TestHandlers.countingCountDownHandler(counter2, latch);
		SimpleChannel channel = new SimpleChannel();
		channel.setPublishSubscribe(true);
		channel.send(new StringMessage(1, "test"));
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(channel);
		dispatcher.setRejectionLimit(5);
		dispatcher.setRetryInterval(3);
		dispatcher.setShouldFailOnRejectionLimit(false);
		dispatcher.addHandler(new ConcurrentHandler(handler1, 1, 1) {
			@Override
			public Message<?> handle(Message<?> message) {
				if (rejectedCounter1.get() == 2) {
					return super.handle(message);
				}
				rejectedCounter1.incrementAndGet();
				latch.countDown();
				throw new MessageHandlerRejectedExecutionException();
			}
		});
		dispatcher.addHandler(new ConcurrentHandler(handler2, 1, 1) {
			@Override
			public Message<?> handle(Message<?> message) {
				if (rejectedCounter2.get() == 4) {
					return super.handle(message);
				}
				rejectedCounter2.incrementAndGet();
				latch.countDown();
				throw new MessageHandlerRejectedExecutionException();
			}
		});
		dispatcher.start();
		latch.await(500, TimeUnit.MILLISECONDS);
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
		SimpleChannel channel = new SimpleChannel();
		channel.send(new StringMessage(1, "test"));
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(channel);
		DefaultMessageEndpoint endpoint1 = new DefaultMessageEndpoint(handler1);
		DefaultMessageEndpoint endpoint2 = new DefaultMessageEndpoint(handler2);
		endpoint1.addMessageSelector(new PayloadTypeSelector(Integer.class));
		endpoint2.addMessageSelector(new PayloadTypeSelector(String.class));
		dispatcher.addHandler(endpoint1);
		dispatcher.addHandler(endpoint2);
		dispatcher.start();
		latch.await(500, TimeUnit.MILLISECONDS);
		assertEquals("messages should have been dispatched within allotted time", 0, latch.getCount());
		assertEquals("handler1 should not have accepted the message", 0, counter1.get());
		assertEquals("handler2 should have accepted the message", 1, counter2.get());
	}

	@Test
	public void testTwoExecutorsWithSelectorsAndNeitherAccepts() throws InterruptedException {
		final AtomicInteger counter1 = new AtomicInteger();
		final AtomicInteger counter2 = new AtomicInteger();
		final AtomicInteger attemptedCounter1 = new AtomicInteger();
		final AtomicInteger attemptedCounter2 = new AtomicInteger();
		final CountDownLatch attemptedLatch = new CountDownLatch(2);
		final CountDownLatch handlerLatch = new CountDownLatch(1);
		MessageHandler handler1 = TestHandlers.countingCountDownHandler(counter1, attemptedLatch);
		MessageHandler handler2 = TestHandlers.countingCountDownHandler(counter2, attemptedLatch);
		SimpleChannel channel = new SimpleChannel();
		channel.send(new StringMessage(1, "test"));
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(channel);
		DefaultMessageEndpoint endpoint1 = new DefaultMessageEndpoint(handler1);
		DefaultMessageEndpoint endpoint2 = new DefaultMessageEndpoint(handler2);
		endpoint1.addMessageSelector(new PayloadTypeSelector(Integer.class));
		endpoint2.addMessageSelector(new PayloadTypeSelector(Integer.class));
		MessageHandler interceptor1 = new InterceptingMessageHandler(endpoint1) {
			@Override
			public Message<?> handle(Message<?> message, MessageHandler target) {
				attemptedCounter1.incrementAndGet();
				attemptedLatch.countDown();
				return target.handle(message);
			}
		};
		MessageHandler interceptor2 = new InterceptingMessageHandler(endpoint2) {
			@Override
			public Message<?> handle(Message<?> message, MessageHandler target) {
				attemptedCounter2.incrementAndGet();
				attemptedLatch.countDown();
				return target.handle(message);
			}
		};
		dispatcher.addHandler(interceptor1);
		dispatcher.addHandler(interceptor2);
		dispatcher.start();
		attemptedLatch.await(500, TimeUnit.MILLISECONDS);
		assertEquals("messages should have been dispatched within allotted time", 0, attemptedLatch.getCount());
		assertEquals("handler1 should not have accepted the message", 0, counter1.get());
		assertEquals("handler2 should not have accepted the message", 0, counter2.get());
		assertEquals("executor1 should have had exactly one attempt", 1, attemptedCounter1.get());
		assertEquals("executor2 should have had exactly one attempt", 1, attemptedCounter2.get());
		assertEquals("handlerLatch should not have counted down", 1, handlerLatch.getCount());
		assertEquals("attemptedLatch should have counted down", 0, attemptedLatch.getCount());
	}

	@Test
	public void testBroadcastingDispatcherWithSelectorsAndOneAccepts() throws InterruptedException {
		final AtomicInteger counter1 = new AtomicInteger();
		final AtomicInteger counter2 = new AtomicInteger();
		final CountDownLatch latch = new CountDownLatch(1);
		MessageHandler handler1 = TestHandlers.countingCountDownHandler(counter1, latch);
		MessageHandler handler2 = TestHandlers.countingCountDownHandler(counter2, latch);
		SimpleChannel channel = new SimpleChannel();
		channel.setPublishSubscribe(true);
		channel.send(new StringMessage(1, "test"));
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(channel);
		DefaultMessageEndpoint endpoint1 = new DefaultMessageEndpoint();
		endpoint1.setHandler(handler1);
		endpoint1.setConcurrencyPolicy(new ConcurrencyPolicy(1, 1));
		DefaultMessageEndpoint endpoint2 = new DefaultMessageEndpoint();
		endpoint2.setHandler(handler2);
		endpoint2.setConcurrencyPolicy(new ConcurrencyPolicy(1, 1));
		endpoint1.addMessageSelector(new PayloadTypeSelector(Integer.class));
		endpoint2.addMessageSelector(new PayloadTypeSelector(String.class));
		dispatcher.addHandler(endpoint1);
		dispatcher.addHandler(endpoint2);
		dispatcher.start();
		latch.await(500, TimeUnit.MILLISECONDS);
		assertEquals("messages should have been dispatched within allotted time", 0, latch.getCount());
		assertEquals("endpoint1 should not have accepted the message", 0, counter1.get());
		assertEquals("endpoint2 should have accepted the message", 1, counter2.get());
	}

}
