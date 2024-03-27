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

package org.springframework.integration.channel.interceptor;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessageSelector;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class MessageSelectingInterceptorTests {

	@Test
	public void testSingleSelectorAccepts() {
		final AtomicInteger counter = new AtomicInteger();
		MessageSelector selector = new TestMessageSelector(true, counter);
		MessageSelectingInterceptor interceptor = new MessageSelectingInterceptor(selector);
		QueueChannel channel = new QueueChannel();
		channel.addInterceptor(interceptor);
		assertThat(channel.send(new GenericMessage<>("test1"))).isTrue();
	}

	@Test(expected = MessageDeliveryException.class)
	public void testSingleSelectorRejects() {
		final AtomicInteger counter = new AtomicInteger();
		MessageSelector selector = new TestMessageSelector(false, counter);
		MessageSelectingInterceptor interceptor = new MessageSelectingInterceptor(selector);
		QueueChannel channel = new QueueChannel();
		channel.addInterceptor(interceptor);
		channel.send(new GenericMessage<>("test1"));
	}

	@Test
	public void testMultipleSelectorsAccept() {
		final AtomicInteger counter = new AtomicInteger();
		MessageSelector selector1 = new TestMessageSelector(true, counter);
		MessageSelector selector2 = new TestMessageSelector(true, counter);
		MessageSelectingInterceptor interceptor = new MessageSelectingInterceptor(selector1, selector2);
		QueueChannel channel = new QueueChannel();
		channel.addInterceptor(interceptor);
		assertThat(channel.send(new GenericMessage<>("test1"))).isTrue();
		assertThat(counter.get()).isEqualTo(2);
	}

	@Test
	public void testMultipleSelectorsReject() {
		boolean exceptionThrown = false;
		final AtomicInteger counter = new AtomicInteger();
		MessageSelector selector1 = new TestMessageSelector(true, counter);
		MessageSelector selector2 = new TestMessageSelector(false, counter);
		MessageSelector selector3 = new TestMessageSelector(false, counter);
		MessageSelector selector4 = new TestMessageSelector(true, counter);
		MessageSelectingInterceptor interceptor =
				new MessageSelectingInterceptor(selector1, selector2, selector3, selector4);
		QueueChannel channel = new QueueChannel();
		channel.addInterceptor(interceptor);
		try {
			channel.send(new GenericMessage<>("test1"));
		}
		catch (MessageDeliveryException e) {
			exceptionThrown = true;
		}
		assertThat(exceptionThrown).isTrue();
		assertThat(counter.get()).isEqualTo(2);
	}

	private static class TestMessageSelector implements MessageSelector {

		private final boolean shouldAccept;

		private final AtomicInteger counter;

		TestMessageSelector(boolean shouldAccept, AtomicInteger counter) {
			this.shouldAccept = shouldAccept;
			this.counter = counter;
		}

		public boolean accept(Message<?> message) {
			this.counter.incrementAndGet();
			return this.shouldAccept;
		}

	}

}
