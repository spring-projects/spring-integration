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

import org.springframework.integration.message.Message;

/**
 * Factory for {@link MessageHandler} implementations that are useful for testing.
 * 
 * @author Mark Fisher
 */
public class TestHandlers {

	/**
	 * Create a {@link MessageHandler} that always returns null.
	 */
	public static MessageHandler nullHandler() {
		return new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				return null;
			}
		};
	}

	/**
	 * Create a {@link MessageHandler} that counts down on the provided latch.
	 */
	public static MessageHandler countDownHandler(final CountDownLatch latch) {
		return new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				latch.countDown();
				return null;
			}
		};
	}

}
