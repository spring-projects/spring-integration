/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.dispatcher;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.message.TestHandlers;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class FailOverDispatcherTests {

	@Test
	public void singleMessage() throws InterruptedException {
		UnicastingDispatcher dispatcher = new UnicastingDispatcher();
		final CountDownLatch latch = new CountDownLatch(1);
		dispatcher.addHandler(createConsumer(TestHandlers.countDownHandler(latch)));
		dispatcher.dispatch(new GenericMessage<>("test"));
		assertThat(latch.await(500, TimeUnit.MILLISECONDS)).isTrue();
	}

	@Test
	public void pointToPoint() throws InterruptedException {
		UnicastingDispatcher dispatcher = new UnicastingDispatcher();
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicInteger counter1 = new AtomicInteger();
		final AtomicInteger counter2 = new AtomicInteger();
		dispatcher.addHandler(createConsumer(TestHandlers.countingCountDownHandler(counter1, latch)));
		dispatcher.addHandler(createConsumer(TestHandlers.countingCountDownHandler(counter2, latch)));
		dispatcher.dispatch(new GenericMessage<>("test"));
		assertThat(latch.await(500, TimeUnit.MILLISECONDS)).isTrue();
		assertThat(counter1.get() + counter2.get()).as("only 1 handler should have received the message").isEqualTo(1);
	}

	@Test
	public void noDuplicateSubscriptions() {
		UnicastingDispatcher dispatcher = new UnicastingDispatcher();
		final AtomicInteger counter = new AtomicInteger();
		MessageHandler target = new CountingTestEndpoint(counter, false);
		dispatcher.addHandler(target);
		dispatcher.addHandler(target);
		assertThatExceptionOfType(MessageRejectedException.class)
				.isThrownBy(() -> dispatcher.dispatch(new GenericMessage<>("test")));
		assertThat(counter.get()).as("target should not have duplicate subscriptions").isEqualTo(1);
	}

	@Test
	public void removeConsumerBeforeSend() {
		UnicastingDispatcher dispatcher = new UnicastingDispatcher();
		final AtomicInteger counter = new AtomicInteger();
		MessageHandler target1 = new CountingTestEndpoint(counter, false);
		MessageHandler target2 = new CountingTestEndpoint(counter, false);
		MessageHandler target3 = new CountingTestEndpoint(counter, false);
		dispatcher.addHandler(target1);
		dispatcher.addHandler(target2);
		dispatcher.addHandler(target3);
		dispatcher.removeHandler(target2);
		assertThatExceptionOfType(AggregateMessageDeliveryException.class)
				.isThrownBy(() -> dispatcher.dispatch(new GenericMessage<>("test")));
		assertThat(counter.get()).isEqualTo(2);
	}

	@Test
	public void removeConsumerBetweenSends() {
		UnicastingDispatcher dispatcher = new UnicastingDispatcher();
		final AtomicInteger counter = new AtomicInteger();
		MessageHandler target1 = new CountingTestEndpoint(counter, false);
		MessageHandler target2 = new CountingTestEndpoint(counter, false);
		MessageHandler target3 = new CountingTestEndpoint(counter, false);
		dispatcher.addHandler(target1);
		dispatcher.addHandler(target2);
		dispatcher.addHandler(target3);
		assertThatExceptionOfType(AggregateMessageDeliveryException.class)
				.isThrownBy(() -> dispatcher.dispatch(new GenericMessage<>("test1")));
		assertThat(counter.get()).isEqualTo(3);
		dispatcher.removeHandler(target2);
		assertThatExceptionOfType(AggregateMessageDeliveryException.class)
				.isThrownBy(() -> dispatcher.dispatch(new GenericMessage<>("test2")));
		assertThat(counter.get()).isEqualTo(5);
		dispatcher.removeHandler(target1);
		assertThatExceptionOfType(MessageRejectedException.class)
				.isThrownBy(() -> dispatcher.dispatch(new GenericMessage<>("test3")));
		assertThat(counter.get()).isEqualTo(6);
	}

	@Test
	public void removeConsumerLastTargetCausesDeliveryException() {
		UnicastingDispatcher dispatcher = new UnicastingDispatcher();
		final AtomicInteger counter = new AtomicInteger();
		MessageHandler target = new CountingTestEndpoint(counter, false);
		dispatcher.addHandler(target);
		assertThatExceptionOfType(MessageRejectedException.class)
				.isThrownBy(() -> dispatcher.dispatch(new GenericMessage<>("test1")));
		assertThat(counter.get()).isEqualTo(1);
		dispatcher.removeHandler(target);
		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> dispatcher.dispatch(new GenericMessage<>("test2")));
	}

	@Test
	public void firstHandlerReturnsTrue() {
		UnicastingDispatcher dispatcher = new UnicastingDispatcher();
		final AtomicInteger counter = new AtomicInteger();
		MessageHandler target1 = new CountingTestEndpoint(counter, true);
		MessageHandler target2 = new CountingTestEndpoint(counter, false);
		MessageHandler target3 = new CountingTestEndpoint(counter, false);
		dispatcher.addHandler(target1);
		dispatcher.addHandler(target2);
		dispatcher.addHandler(target3);
		assertThat(dispatcher.dispatch(new GenericMessage<>("test"))).isTrue();
		assertThat(counter.get()).as("only the first target should have been invoked").isEqualTo(1);
	}

	@Test
	public void middleHandlerReturnsTrue() {
		UnicastingDispatcher dispatcher = new UnicastingDispatcher();
		final AtomicInteger counter = new AtomicInteger();
		MessageHandler target1 = new CountingTestEndpoint(counter, false);
		MessageHandler target2 = new CountingTestEndpoint(counter, true);
		MessageHandler target3 = new CountingTestEndpoint(counter, false);
		dispatcher.addHandler(target1);
		dispatcher.addHandler(target2);
		dispatcher.addHandler(target3);
		assertThat(dispatcher.dispatch(new GenericMessage<>("test"))).isTrue();
		assertThat(counter.get()).as("first two targets should have been invoked").isEqualTo(2);
	}

	@Test
	public void allHandlersReturnFalse() {
		UnicastingDispatcher dispatcher = new UnicastingDispatcher();
		final AtomicInteger counter = new AtomicInteger();
		MessageHandler target1 = new CountingTestEndpoint(counter, false);
		MessageHandler target2 = new CountingTestEndpoint(counter, false);
		MessageHandler target3 = new CountingTestEndpoint(counter, false);
		dispatcher.addHandler(target1);
		dispatcher.addHandler(target2);
		dispatcher.addHandler(target3);
		assertThatExceptionOfType(AggregateMessageDeliveryException.class)
				.isThrownBy(() -> dispatcher.dispatch(new GenericMessage<>("test")));
		assertThat(counter.get()).as("each target should have been invoked").isEqualTo(3);
	}

	@Test
	public void failoverStrategyRejects() {
		UnicastingDispatcher dispatcher = new UnicastingDispatcher();
		dispatcher.setFailoverStrategy((exception) -> !(exception instanceof MessageRejectedException));
		AtomicInteger counter = new AtomicInteger();
		MessageHandler target1 = new CountingTestEndpoint(counter, false);
		MessageHandler target2 = new CountingTestEndpoint(counter, true);
		MessageHandler target3 = new CountingTestEndpoint(counter, true);
		dispatcher.addHandler(target1);
		dispatcher.addHandler(target2);
		dispatcher.addHandler(target3);
		assertThatExceptionOfType(MessageRejectedException.class)
				.isThrownBy(() -> dispatcher.dispatch(new GenericMessage<>("test")));
		assertThat(counter.get()).as("only first should have been invoked").isEqualTo(1);
	}

	private static ServiceActivatingHandler createConsumer(Object object) {
		ServiceActivatingHandler handler = new ServiceActivatingHandler(object);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		return handler;
	}

	private static class CountingTestEndpoint implements MessageHandler {

		private final AtomicInteger counter;

		private final boolean shouldAccept;

		CountingTestEndpoint(AtomicInteger counter, boolean shouldAccept) {
			this.counter = counter;
			this.shouldAccept = shouldAccept;
		}

		public void handleMessage(Message<?> message) {
			this.counter.incrementAndGet();
			if (!this.shouldAccept) {
				throw new MessageRejectedException(message, "intentional test failure");
			}
		}

	}

}
