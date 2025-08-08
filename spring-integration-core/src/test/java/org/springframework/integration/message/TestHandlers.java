/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
