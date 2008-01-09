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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.channel.SimpleChannel;
import org.springframework.integration.endpoint.GenericMessageEndpoint;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;

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
		ConsumerPolicy policy = new ConsumerPolicy();
		SimpleChannel channel = new SimpleChannel();
		channel.send(new StringMessage(1, "test"));
		MessageRetriever retriever = new ChannelPollingMessageRetriever(channel, policy);
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(retriever);
		dispatcher.addExecutor(new MessageReceivingExecutor(endpoint1, 1, 1));
		dispatcher.addExecutor(new MessageReceivingExecutor(endpoint2, 1, 1));
		dispatcher.dispatch();
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
		ConsumerPolicy policy = new ConsumerPolicy();
		SimpleChannel channel = new SimpleChannel();
		channel.send(new StringMessage(1, "test"));
		MessageRetriever retriever = new ChannelPollingMessageRetriever(channel, policy);
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(retriever);
		dispatcher.setBroadcast(true);
		dispatcher.addExecutor(new MessageReceivingExecutor(endpoint1, 1, 1));
		dispatcher.addExecutor(new MessageReceivingExecutor(endpoint2, 1, 1));
		dispatcher.dispatch();
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
		ConsumerPolicy policy = new ConsumerPolicy();
		SimpleChannel channel = new SimpleChannel();
		channel.send(new StringMessage(1, "test"));
		MessageRetriever retriever = new ChannelPollingMessageRetriever(channel, policy);
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(retriever);
		dispatcher.addExecutor(new MessageReceivingExecutor(endpoint1, 1, 1) {
			@Override
			public void start() {
			}
		});
		dispatcher.addExecutor(new MessageReceivingExecutor(endpoint2, 1, 1));
		dispatcher.addExecutor(new MessageReceivingExecutor(endpoint3, 1, 1));
		dispatcher.dispatch();
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
		ConsumerPolicy policy = new ConsumerPolicy();
		SimpleChannel channel = new SimpleChannel();
		channel.send(new StringMessage(1, "test"));
		MessageRetriever retriever = new ChannelPollingMessageRetriever(channel, policy);
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(retriever);
		dispatcher.setBroadcast(true);
		dispatcher.addExecutor(new MessageReceivingExecutor(endpoint1, 1, 1));
		dispatcher.addExecutor(new MessageReceivingExecutor(endpoint2, 1, 1) {
			@Override
			public void start() {
			}
		});
		dispatcher.addExecutor(new MessageReceivingExecutor(endpoint3, 1, 1));
		dispatcher.dispatch();
		latch.await(100, TimeUnit.MILLISECONDS);
		assertEquals("inactive endpoint should not have received message", 0, counter2.get());
		assertEquals("both active endpoints should have received message", 2, counter1.get() + counter3.get());
	}

	@Test
	public void testDispatcherWithNoExecutors() {
		ConsumerPolicy policy = new ConsumerPolicy();
		SimpleChannel channel = new SimpleChannel();
		channel.send(new StringMessage(1, "test"));
		MessageRetriever retriever = new ChannelPollingMessageRetriever(channel, policy);
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(retriever);
		assertEquals(0, dispatcher.dispatch());
	}

	@Test(expected=MessageDeliveryException.class)
	public void testBroadcastingDispatcherReachesRejectionLimitAndShouldFail() {
		final AtomicInteger counter1 = new AtomicInteger();
		final AtomicInteger counter2 = new AtomicInteger();
		final AtomicInteger counter3 = new AtomicInteger();
		final CountDownLatch latch = new CountDownLatch(2);
		TestEndpoint endpoint1 = new TestEndpoint(counter1, latch);
		TestEndpoint endpoint2 = new TestEndpoint(counter2, latch);
		TestEndpoint endpoint3 = new TestEndpoint(counter3, latch);
		ConsumerPolicy policy = new ConsumerPolicy();
		SimpleChannel channel = new SimpleChannel();
		channel.send(new StringMessage(1, "test"));
		MessageRetriever retriever = new ChannelPollingMessageRetriever(channel, policy);
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(retriever);
		dispatcher.setBroadcast(true);
		dispatcher.setRejectionLimit(2);
		dispatcher.setRetryInterval(3);
		dispatcher.addExecutor(new MessageReceivingExecutor(endpoint1, 1, 1));
		dispatcher.addExecutor(new MessageReceivingExecutor(endpoint2, 1, 1) {
			@Override
			public void processMessage(Message<?> message) {
				throw new RejectedExecutionException();
			}
		});
		dispatcher.addExecutor(new MessageReceivingExecutor(endpoint3, 1, 1));
		dispatcher.dispatch();
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
		ConsumerPolicy policy = new ConsumerPolicy();
		SimpleChannel channel = new SimpleChannel();
		channel.send(new StringMessage(1, "test"));
		MessageRetriever retriever = new ChannelPollingMessageRetriever(channel, policy);
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(retriever);
		dispatcher.setBroadcast(true);
		dispatcher.setRejectionLimit(2);
		dispatcher.setRetryInterval(3);
		dispatcher.setShouldFailOnRejectionLimit(false);
		dispatcher.addExecutor(new MessageReceivingExecutor(endpoint1, 1, 1));
		dispatcher.addExecutor(new MessageReceivingExecutor(endpoint2, 1, 1) {
			@Override
			public void processMessage(Message<?> message) {
				throw new RejectedExecutionException();
			}
		});
		dispatcher.addExecutor(new MessageReceivingExecutor(endpoint3, 1, 1));
		dispatcher.dispatch();
		latch.await(100, TimeUnit.MILLISECONDS);
		assertEquals("rejecting endpoint should not have received message", 0, counter2.get());
		assertEquals("both non-rejecting endpoints should have received message", 2, counter1.get() + counter3.get());
	}

	@Test(expected=MessageDeliveryException.class)
	public void testNonBroadcastingDispatcherReachesRejectionLimitAndShouldFail() {
		final AtomicInteger counter1 = new AtomicInteger();
		final AtomicInteger counter2 = new AtomicInteger();
		final CountDownLatch latch = new CountDownLatch(1);
		TestEndpoint endpoint1 = new TestEndpoint(counter1, latch);
		TestEndpoint endpoint2 = new TestEndpoint(counter2, latch);
		ConsumerPolicy policy = new ConsumerPolicy();
		SimpleChannel channel = new SimpleChannel();
		channel.send(new StringMessage(1, "test"));
		MessageRetriever retriever = new ChannelPollingMessageRetriever(channel, policy);
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(retriever);
		dispatcher.setRejectionLimit(2);
		dispatcher.setRetryInterval(3);
		dispatcher.addExecutor(new MessageReceivingExecutor(endpoint1, 1, 1) {
			@Override
			public void processMessage(Message<?> message) {
				throw new RejectedExecutionException();
			}
		});
		dispatcher.addExecutor(new MessageReceivingExecutor(endpoint2, 1, 1) {
			@Override
			public void processMessage(Message<?> message) {
				throw new RejectedExecutionException();
			}
		});
		dispatcher.dispatch();
	}

	@Test
	public void testNonBroadcastingDispatcherReachesRejectionLimitButShouldNotFail() {
		final AtomicInteger counter1 = new AtomicInteger();
		final AtomicInteger counter2 = new AtomicInteger();
		final AtomicInteger rejectedCounter1 = new AtomicInteger();
		final AtomicInteger rejectedCounter2 = new AtomicInteger();
		final CountDownLatch latch = new CountDownLatch(1);
		TestEndpoint endpoint1 = new TestEndpoint(counter1, latch);
		TestEndpoint endpoint2 = new TestEndpoint(counter2, latch);
		ConsumerPolicy policy = new ConsumerPolicy();
		SimpleChannel channel = new SimpleChannel();
		channel.send(new StringMessage(1, "test"));
		MessageRetriever retriever = new ChannelPollingMessageRetriever(channel, policy);
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(retriever);
		dispatcher.setRejectionLimit(2);
		dispatcher.setRetryInterval(3);
		dispatcher.setShouldFailOnRejectionLimit(false);
		dispatcher.addExecutor(new MessageReceivingExecutor(endpoint1, 1, 1) {
			@Override
			public void processMessage(Message<?> message) {
				rejectedCounter1.incrementAndGet();
				throw new RejectedExecutionException();
			}
		});
		dispatcher.addExecutor(new MessageReceivingExecutor(endpoint2, 1, 1) {
			@Override
			public void processMessage(Message<?> message) {
				rejectedCounter2.incrementAndGet();
				throw new RejectedExecutionException();
			}
		});
		dispatcher.dispatch();
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
		ConsumerPolicy policy = new ConsumerPolicy();
		SimpleChannel channel = new SimpleChannel();
		channel.send(new StringMessage(1, "test"));
		MessageRetriever retriever = new ChannelPollingMessageRetriever(channel, policy);
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(retriever);
		dispatcher.setRejectionLimit(2);
		dispatcher.setRetryInterval(3);
		dispatcher.setShouldFailOnRejectionLimit(false);
		dispatcher.addExecutor(new MessageReceivingExecutor(endpoint1, 1, 1) {
			@Override
			public void processMessage(Message<?> message) {
				rejectedCounter1.incrementAndGet();
				throw new RejectedExecutionException();
			}
		});
		dispatcher.addExecutor(new MessageReceivingExecutor(endpoint2, 1, 1) {
			@Override
			public void processMessage(Message<?> message) {
				if (rejectedCounter2.get() > 0) {
					super.processMessage(message);
					return;
				}
				rejectedCounter2.incrementAndGet();
				throw new RejectedExecutionException();
			}
		});
		dispatcher.addExecutor(new MessageReceivingExecutor(endpoint3, 1, 1) {
			@Override
			public void processMessage(Message<?> message) {
				rejectedCounter3.incrementAndGet();
				throw new RejectedExecutionException();
			}
		});
		dispatcher.dispatch();
		latch.await(100, TimeUnit.MILLISECONDS);
		assertEquals("endpoint1 should not have received message", 0, counter1.get());
		assertEquals("endpoint2 should have received message the second time", 1, counter2.get());
		assertEquals("endpoint3 should not have received message", 0, counter3.get());
		assertEquals("endpoint1 should have rejected two times", 2, rejectedCounter1.get());
		assertEquals("endpoint2 should have rejected one time", 1, rejectedCounter2.get());
		assertEquals("endpoint3 should have rejected one time", 1, rejectedCounter3.get());
	}


	private static class TestEndpoint extends GenericMessageEndpoint<String> {

		private AtomicInteger counter;

		private CountDownLatch latch;


		public TestEndpoint(AtomicInteger counter, CountDownLatch latch) {
			this.counter = counter;
			this.latch = latch;
		}

		@Override
		public void messageReceived(Message<String> message) {
			counter.incrementAndGet();
			latch.countDown();
		}
	}

}
