/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.integration.message;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.messaging.Message;

/**
 * Factory for handler beans that are useful for testing.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 */
@SuppressWarnings("unused")
public abstract class TestHandlers {

	/**
	 * Create a handler that always returns null.
	 */
	public static final Object nullHandler() {
		return new Object() {

			public Message<?> handle(Message<?> message) {
				return null;
			}
		};
	}

	/**
	 * Create a handler that simply returns the {@link Message} it receives.
	 */
	public static final Object echoHandler() {
		return new Object() {

			public Message<?> handle(Message<?> message) {
				return message;
			}
		};
	}

	/**
	 * Create a handler that increments the provided counter.
	 */
	public static final Object countingHandler(final AtomicInteger counter) {
		return new Object() {

			public Message<?> handle(Message<?> message) {
				counter.incrementAndGet();
				return null;
			}
		};
	}

	/**
	 * Create a handler that counts down on the provided latch.
	 */
	public static final Object countDownHandler(final CountDownLatch latch) {
		return new Object() {

			public Message<?> handle(Message<?> message) {
				latch.countDown();
				return null;
			}
		};
	}

	/**
	 * Create a handler that counts down on the provided latch
	 * and also increments the provided counter.
	 */
	public static final Object countingCountDownHandler(final AtomicInteger counter, final CountDownLatch latch) {
		return new Object() {

			public Message<?> handle(Message<?> message) {
				counter.incrementAndGet();
				latch.countDown();
				return null;
			}
		};
	}

}
