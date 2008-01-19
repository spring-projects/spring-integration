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

package org.springframework.integration.handler;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.integration.dispatcher.MessageHandlerRejectedExecutionException;
import org.springframework.integration.message.Message;

/**
 * Factory for {@link MessageHandler} implementations that are useful for
 * testing.
 * 
 * @author Mark Fisher
 */
public abstract class TestHandlers {

	/**
	 * Create a {@link MessageHandler} that always returns null.
	 */
	public final static MessageHandler nullHandler() {
		return new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				return null;
			}
		};
	}

	/**
	 * Create a {@link MessageHandler} that throws a {@link MessageHandlerRejectedExecutionException}.
	 */
	public final static MessageHandler rejectingHandler() {
		return new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				throw new MessageHandlerRejectedExecutionException();
			}
		};
	}

	/**
	 * Create a {@link MessageHandler} that counts down on the provided latch and 
	 * then throws a {@link MessageHandlerRejectedExecutionException}.
	 */
	public final static MessageHandler rejectingCountDownHandler(final CountDownLatch latch) {
		return new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				latch.countDown();
				throw new MessageHandlerRejectedExecutionException();
			}
		};
	}

	/**
	 * Create a {@link MessageHandler} that increments the provided counter.
	 */
	public final static MessageHandler countingHandler(final AtomicInteger counter) {
		return new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				counter.incrementAndGet();
				return null;
			}
		};
	}

	/**
	 * Create a {@link MessageHandler} that counts down on the provided latch.
	 */
	public final static MessageHandler countDownHandler(final CountDownLatch latch) {
		return new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				latch.countDown();
				return null;
			}
		};
	}

	/**
	 * Create a {@link MessageHandler} that counts down on the provided latch
	 * and also increments the provided counter.
	 */
	public final static MessageHandler countingCountDownHandler(final AtomicInteger counter, final CountDownLatch latch) {
		return new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				counter.incrementAndGet();
				latch.countDown();
				return null;
			}
		};
	}

}
