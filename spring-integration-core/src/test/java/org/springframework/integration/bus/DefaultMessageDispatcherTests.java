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

package org.springframework.integration.bus;

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
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.handler.PooledMessageHandler;
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
		TestEndpoint endpoint1 = new TestEndpoint(counter1, latch);
		TestEndpoint endpoint2 = new TestEndpoint(counter2, latch);		
		SimpleChannel channel = new SimpleChannel();
		channel.send(new StringMessage(1, "test"));
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(channel);
		dispatcher.addHandler(new PooledMessageHandler(endpoint1, 1, 1));
		dispatcher.addHandler(new PooledMessageHandler(endpoint2, 1, 1));
		SimpleMessagingTaskScheduler scheduler = new SimpleMessagingTaskScheduler();
		scheduler.start();
		dispatcher.setMessagingTaskScheduler(scheduler);
		dispatcher.start();
		latch.await(100, TimeUnit.MILLISECONDS);
		assertEquals("exactly one endpoint should have received message", 1, counter1.get() + counter2.get());
	}

	@Test
	public void testBroadcastingDispatcherSendsToAllEndpoints() throws InterruptedException {
		final AtomicInteger counter1 = new AtomicInteger();
		final AtomicInteger counter2 = new AtomicInteger();
		final CountDownLatch latch = new CountDownLatch(2);
		TestEndpoint endpoint1 = new TestEndpoint(counter1, latch);
		TestEndpoint endpoint2 = new TestEndpoint(counter2, latch);		
		SimpleChannel channel = new SimpleChannel();
		channel.setPublishSubscribe(true);
		channel.send(new StringMessage(1, "test"));
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(channel);
		dispatcher.addHandler(new PooledMessageHandler(endpoint1, 1, 1));
		dispatcher.addHandler(new PooledMessageHandler(endpoint2, 1, 1));
		dispatcher.start();
		latch.await(100, TimeUnit.MILLISECONDS);
		assertEquals("both endpoints should have received message", 2, counter1.get() + counter2.get());
	}

	@Test
	public void testNonBroadcastingDispatcherSkipsInactiveExecutor() throws InterruptedException {
		final AtomicInteger counter1 = new AtomicInteger();
		final AtomicInteger counter2 = new AtomicInteger();
		final AtomicInteger counter3 = new AtomicInteger();
		final CountDownLatch latch = new CountDownLatch(1);
		TestEndpoint endpoint1 = new TestEndpoint(counter1, latch);
		TestEndpoint endpoint2 = new TestEndpoint(counter2, latch);
		TestEndpoint endpoint3 = new TestEndpoint(counter3, latch);
		SimpleChannel channel = new SimpleChannel();
		channel.send(new StringMessage(1, "test"));
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(channel);
		dispatcher.addHandler(new PooledMessageHandler(endpoint1, 1, 1) {
			@Override
			public void start() {
			}
		});
		dispatcher.addHandler(new PooledMessageHandler(endpoint2, 1, 1));
		dispatcher.addHandler(new PooledMessageHandler(endpoint3, 1, 1));
		dispatcher.start();
		latch.await(100, TimeUnit.MILLISECONDS);
		assertEquals("inactive endpoint should not have received message", 0, counter1.get());
		assertEquals("exactly one endpoint should have received message", 1, counter2.get() + counter3.get());
	}

	@Test
	public void testBroadcastingDispatcherSkipsInactiveExecutor() throws InterruptedException {
		final AtomicInteger counter1 = new AtomicInteger();
		final AtomicInteger counter2 = new AtomicInteger();
		final AtomicInteger counter3 = new AtomicInteger();
		final CountDownLatch latch = new CountDownLatch(2);
		TestEndpoint endpoint1 = new TestEndpoint(counter1, latch);
		TestEndpoint endpoint2 = new TestEndpoint(counter2, latch);
		TestEndpoint endpoint3 = new TestEndpoint(counter3, latch);
		SimpleChannel channel = new SimpleChannel();
		channel.setPublishSubscribe(true);
		channel.send(new StringMessage(1, "test"));
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(channel);
		dispatcher.addHandler(new PooledMessageHandler(endpoint1, 1, 1));
		dispatcher.addHandler(new PooledMessageHandler(endpoint2, 1, 1) {
			@Override
			public void start() {
			}
		});
		dispatcher.addHandler(new PooledMessageHandler(endpoint3, 1, 1));
		dispatcher.start();
		latch.await(100, TimeUnit.MILLISECONDS);
		assertEquals("inactive endpoint should not have received message", 0, counter2.get());
		assertEquals("both active endpoints should have received message", 2, counter1.get() + counter3.get());
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
		TestEndpoint endpoint1 = new TestEndpoint(counter1, latch);
		TestEndpoint endpoint2 = new TestEndpoint(counter2, latch);
		TestEndpoint endpoint3 = new TestEndpoint(counter3, latch);
		SimpleChannel channel = new SimpleChannel();
		channel.setPublishSubscribe(true);
		channel.send(new StringMessage(1, "test"));
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(channel);
		dispatcher.setRejectionLimit(2);
		dispatcher.setRetryInterval(3);
		dispatcher.addHandler(new PooledMessageHandler(endpoint1, 1, 1));
		dispatcher.addHandler(new PooledMessageHandler(endpoint2, 1, 1) {
			@Override
			public Message<?> handle(Message<?> message) {
				throw new MessageHandlerRejectedExecutionException();
			}
		});
		dispatcher.addHandler(new PooledMessageHandler(endpoint3, 1, 1));
		SimpleChannel errorChannel = new SimpleChannel();
		SimpleMessagingTaskScheduler scheduler = new SimpleMessagingTaskScheduler();
		scheduler.setErrorHandler(new MessagePublishingErrorHandler(errorChannel));
		dispatcher.setMessagingTaskScheduler(scheduler);
		dispatcher.start();
		latch.await(500, TimeUnit.MILLISECONDS);
		Message<?> errorMessage = errorChannel.receive(100);
		assertNotNull(errorMessage);
		assertTrue(errorMessage instanceof ErrorMessage);
		assertEquals(MessageDeliveryException.class, ((ErrorMessage) errorMessage).getPayload().getClass());
	}

	@Test
	public void testBroadcastingDispatcherReachesRejectionLimitAndShouldNotFail() throws InterruptedException {
		final AtomicInteger counter1 = new AtomicInteger();
		final AtomicInteger counter2 = new AtomicInteger();
		final AtomicInteger counter3 = new AtomicInteger();
		final CountDownLatch latch = new CountDownLatch(2);
		TestEndpoint endpoint1 = new TestEndpoint(counter1, latch);
		TestEndpoint endpoint2 = new TestEndpoint(counter2, latch);
		TestEndpoint endpoint3 = new TestEndpoint(counter3, latch);
		SimpleChannel channel = new SimpleChannel();
		channel.setPublishSubscribe(true);
		channel.send(new StringMessage(1, "test"));
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(channel);
		dispatcher.setRejectionLimit(2);
		dispatcher.setRetryInterval(3);
		dispatcher.setShouldFailOnRejectionLimit(false);
		dispatcher.addHandler(new PooledMessageHandler(endpoint1, 1, 1));
		dispatcher.addHandler(new PooledMessageHandler(endpoint2, 1, 1) {
			@Override
			public Message<?> handle(Message<?> message) {
				throw new MessageHandlerRejectedExecutionException();
			}
		});
		dispatcher.addHandler(new PooledMessageHandler(endpoint3, 1, 1));
		dispatcher.start();
		latch.await(1000, TimeUnit.MILLISECONDS);
		assertEquals("rejecting endpoint should not have received message", 0, counter2.get());
		assertEquals("both non-rejecting endpoints should have received message", 2, counter1.get() + counter3.get());
	}

	@Test
	public void testNonBroadcastingDispatcherReachesRejectionLimitAndShouldFail() {
		final AtomicInteger counter1 = new AtomicInteger();
		final AtomicInteger counter2 = new AtomicInteger();
		final CountDownLatch latch = new CountDownLatch(2);
		TestEndpoint endpoint1 = new TestEndpoint(counter1, latch);
		TestEndpoint endpoint2 = new TestEndpoint(counter2, latch);
		SimpleChannel channel = new SimpleChannel();
		channel.send(new StringMessage(1, "test"));
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(channel);
		dispatcher.setRejectionLimit(2);
		dispatcher.setRetryInterval(3);
		dispatcher.addHandler(new PooledMessageHandler(endpoint1, 1, 1) {
			@Override
			public Message<?> handle(Message<?> message) {
				latch.countDown();
				throw new MessageHandlerRejectedExecutionException();
			}
		});
		dispatcher.addHandler(new PooledMessageHandler(endpoint2, 1, 1) {
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
		Message<?> errorMessage = errorChannel.receive(100);
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
		TestEndpoint endpoint1 = new TestEndpoint(counter1, latch);
		TestEndpoint endpoint2 = new TestEndpoint(counter2, latch);
		SimpleChannel channel = new SimpleChannel();
		channel.send(new StringMessage(1, "test"));
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(channel);
		dispatcher.setRejectionLimit(2);
		dispatcher.setRetryInterval(3);
		dispatcher.setShouldFailOnRejectionLimit(false);
		dispatcher.addHandler(new PooledMessageHandler(endpoint1, 1, 1) {
			@Override
			public Message<?> handle(Message<?> message) {
				rejectedCounter1.incrementAndGet();
				latch.countDown();
				throw new MessageHandlerRejectedExecutionException();
			}
		});
		dispatcher.addHandler(new PooledMessageHandler(endpoint2, 1, 1) {
			@Override
			public Message<?> handle(Message<?> message) {
				rejectedCounter2.incrementAndGet();
				latch.countDown();
				throw new MessageHandlerRejectedExecutionException();
			}
		});
		dispatcher.start();
		latch.await(300, TimeUnit.MILLISECONDS);
		assertEquals("latch should have counted down within allotted time", 0, latch.getCount());
		assertEquals("rejecting endpoints should not have received message", 0, counter1.get() + counter2.get());
		assertEquals("endpoint1 should have rejected two times", 2, rejectedCounter1.get());
		assertEquals("endpoint2 should have rejected two times", 2, rejectedCounter2.get());
	}

	@Test
	public void testNonBroadcastingDispatcherWithOneEndpointSucceeding() throws InterruptedException {
		final AtomicInteger counter1 = new AtomicInteger();
		final AtomicInteger counter2 = new AtomicInteger();
		final AtomicInteger counter3 = new AtomicInteger();
		final AtomicInteger rejectedCounter1 = new AtomicInteger();
		final AtomicInteger rejectedCounter2 = new AtomicInteger();
		final AtomicInteger rejectedCounter3 = new AtomicInteger();
		final CountDownLatch latch = new CountDownLatch(1);
		TestEndpoint endpoint1 = new TestEndpoint(counter1, latch);
		TestEndpoint endpoint2 = new TestEndpoint(counter2, latch);
		TestEndpoint endpoint3 = new TestEndpoint(counter3, latch);
		SimpleChannel channel = new SimpleChannel();
		channel.send(new StringMessage(1, "test"));
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(channel);
		dispatcher.setRejectionLimit(2);
		dispatcher.setRetryInterval(3);
		dispatcher.setShouldFailOnRejectionLimit(false);
		dispatcher.addHandler(new PooledMessageHandler(endpoint1, 1, 1) {
			@Override
			public Message<?> handle(Message<?> message) {
				rejectedCounter1.incrementAndGet();
				throw new MessageHandlerRejectedExecutionException();
			}
		});
		dispatcher.addHandler(new PooledMessageHandler(endpoint2, 1, 1) {
			@Override
			public Message<?> handle(Message<?> message) {
				if (rejectedCounter2.get() == 1) {
					return super.handle(message);
				}
				rejectedCounter2.incrementAndGet();
				throw new MessageHandlerRejectedExecutionException();
			}
		});
		dispatcher.addHandler(new PooledMessageHandler(endpoint3, 1, 1) {
			@Override
			public Message<?> handle(Message<?> message) {
				rejectedCounter3.incrementAndGet();
				throw new MessageHandlerRejectedExecutionException();
			}
		});
		dispatcher.start();
		latch.await(300, TimeUnit.MILLISECONDS);
		assertEquals("latch should have counted down within allotted time", 0, latch.getCount());
		assertEquals("endpoint1 should not have received message", 0, counter1.get());
		assertEquals("endpoint2 should have received message the second time", 1, counter2.get());
		assertEquals("endpoint3 should not have received message", 0, counter3.get());
		assertEquals("endpoint1 should have rejected two times", 2, rejectedCounter1.get());
		assertEquals("endpoint2 should have rejected one time", 1, rejectedCounter2.get());
		assertEquals("endpoint3 should have rejected one time", 1, rejectedCounter3.get());
	}

	@Test
	public void testBroadcastingDispatcherStillRetriesRejectedExecutorAfterOtherSucceeds() throws InterruptedException {
		final AtomicInteger counter1 = new AtomicInteger();
		final AtomicInteger counter2 = new AtomicInteger();
		final AtomicInteger rejectedCounter1 = new AtomicInteger();
		final AtomicInteger rejectedCounter2 = new AtomicInteger();
		final CountDownLatch latch = new CountDownLatch(8);
		TestEndpoint endpoint1 = new TestEndpoint(counter1, latch);
		TestEndpoint endpoint2 = new TestEndpoint(counter2, latch);
		SimpleChannel channel = new SimpleChannel();
		channel.setPublishSubscribe(true);
		channel.send(new StringMessage(1, "test"));
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(channel);
		dispatcher.setRejectionLimit(5);
		dispatcher.setRetryInterval(3);
		dispatcher.setShouldFailOnRejectionLimit(false);
		dispatcher.addHandler(new PooledMessageHandler(endpoint1, 1, 1) {
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
		dispatcher.addHandler(new PooledMessageHandler(endpoint2, 1, 1) {
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
		latch.await(300, TimeUnit.MILLISECONDS);
		assertEquals("latch should have counted down within allotted time", 0, latch.getCount());
		assertEquals("endpoint1 should have received one message", 1, counter1.get());
		assertEquals("endpoint2 should have received one message", 1, counter2.get());
		assertEquals("endpoint1 should have rejected two times", 2, rejectedCounter1.get());
		assertEquals("endpoint2 should have rejected four times", 4, rejectedCounter2.get());
	}

	@Test
	public void testTwoExecutorsWithSelectorsAndOneAccepts() throws InterruptedException {
		final AtomicInteger counter1 = new AtomicInteger();
		final AtomicInteger counter2 = new AtomicInteger();
		final CountDownLatch latch = new CountDownLatch(1);
		TestEndpoint endpoint1 = new TestEndpoint(counter1, latch);
		TestEndpoint endpoint2 = new TestEndpoint(counter2, latch);
		SimpleChannel channel = new SimpleChannel();
		channel.send(new StringMessage(1, "test"));
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(channel);
		PooledMessageHandler executor1 = new PooledMessageHandler(endpoint1, 1, 1);
		PooledMessageHandler executor2 = new PooledMessageHandler(endpoint2, 1, 1);
		executor1.addMessageSelector(new PayloadTypeSelector(Integer.class));
		executor2.addMessageSelector(new PayloadTypeSelector(String.class));
		dispatcher.addHandler(executor1);
		dispatcher.addHandler(executor2);
		dispatcher.start();
		latch.await(300, TimeUnit.MILLISECONDS);
		assertEquals("latch should have counted down within allotted time", 0, latch.getCount());
		assertEquals("endpoint1 should not have accepted the message", 0, counter1.get());
		assertEquals("endpoint2 should have accepted the message", 1, counter2.get());
	}

	@Test
	public void testTwoExecutorsWithSelectorsAndNeitherAccepts() throws InterruptedException {
		final AtomicInteger counter1 = new AtomicInteger();
		final AtomicInteger counter2 = new AtomicInteger();
		final AtomicInteger attemptedCounter1 = new AtomicInteger();
		final AtomicInteger attemptedCounter2 = new AtomicInteger();
		final CountDownLatch attemptedLatch = new CountDownLatch(2);
		final CountDownLatch endpointLatch = new CountDownLatch(1);
		TestEndpoint endpoint1 = new TestEndpoint(counter1, endpointLatch);
		TestEndpoint endpoint2 = new TestEndpoint(counter2, endpointLatch);
		SimpleChannel channel = new SimpleChannel();
		channel.send(new StringMessage(1, "test"));
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(channel);
		PooledMessageHandler executor1 = new PooledMessageHandler(endpoint1, 1, 1) {
			@Override
			public Message<?> handle(Message<?> message) {
				attemptedCounter1.incrementAndGet();
				attemptedLatch.countDown();
				return super.handle(message);
			}
		};
		PooledMessageHandler executor2 = new PooledMessageHandler(endpoint2, 1, 1) {
			@Override
			public Message<?> handle(Message<?> message) {
				attemptedCounter2.incrementAndGet();
				attemptedLatch.countDown();
				return super.handle(message);
			}
		};
		executor1.addMessageSelector(new PayloadTypeSelector(Integer.class));
		executor2.addMessageSelector(new PayloadTypeSelector(Integer.class));
		dispatcher.addHandler(executor1);
		dispatcher.addHandler(executor2);
		dispatcher.start();
		attemptedLatch.await(300, TimeUnit.MILLISECONDS);
		assertEquals("attemptedLatch should have counted down within allotted time", 0, attemptedLatch.getCount());
		assertEquals("endpoint1 should not have accepted the message", 0, counter1.get());
		assertEquals("endpoint2 should not have accepted the message", 0, counter2.get());
		assertEquals("executor1 should have had exactly one attempt", 1, attemptedCounter1.get());
		assertEquals("executor2 should have had exactly one attempt", 1, attemptedCounter2.get());
		assertEquals("endpointLatch should not have counted down", 1, endpointLatch.getCount());
		assertEquals("attemptedLatch should have counted down", 0, attemptedLatch.getCount());
	}

	@Test
	public void testBroadcastingDispatcherWithSelectorsAndOneAccepts() throws InterruptedException {
		final AtomicInteger counter1 = new AtomicInteger();
		final AtomicInteger counter2 = new AtomicInteger();
		final CountDownLatch latch = new CountDownLatch(1);
		TestEndpoint endpoint1 = new TestEndpoint(counter1, latch);
		TestEndpoint endpoint2 = new TestEndpoint(counter2, latch);
		SimpleChannel channel = new SimpleChannel();
		channel.setPublishSubscribe(true);
		channel.send(new StringMessage(1, "test"));
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(channel);
		PooledMessageHandler executor1 = new PooledMessageHandler(endpoint1, 1, 1);
		PooledMessageHandler executor2 = new PooledMessageHandler(endpoint2, 1, 1);
		executor1.addMessageSelector(new PayloadTypeSelector(Integer.class));
		executor2.addMessageSelector(new PayloadTypeSelector(String.class));
		dispatcher.addHandler(executor1);
		dispatcher.addHandler(executor2);
		dispatcher.start();
		latch.await(300, TimeUnit.MILLISECONDS);
		assertEquals("latch should have counted down within allotted time", 0, latch.getCount());
		assertEquals("endpoint1 should not have accepted the message", 0, counter1.get());
		assertEquals("endpoint2 should have accepted the message", 1, counter2.get());
	}


	private static class TestEndpoint implements MessageHandler {

		private AtomicInteger counter;

		private CountDownLatch latch;

		public TestEndpoint(AtomicInteger counter, CountDownLatch latch) {
			this.counter = counter;
			this.latch = latch;
		}

		public Message<?> handle(Message<?> message) {
			counter.incrementAndGet();
			latch.countDown();
			return null;
		}
	}

}
