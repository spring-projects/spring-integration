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

package org.springframework.integration.dispatcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageTarget;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class DirectChannelTests {

	@Test
	public void testSend() {
		DirectChannel channel = new DirectChannel();
		ThreadNameExtractingTestTarget target = new ThreadNameExtractingTestTarget();
		channel.subscribe(target);
		StringMessage message = new StringMessage("test");
		assertTrue(channel.send(message));
		assertEquals(Thread.currentThread().getName(), target.threadName);
	}

	@Test
	public void testSendInSeparateThread() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		final DirectChannel channel = new DirectChannel();
		ThreadNameExtractingTestTarget target = new ThreadNameExtractingTestTarget(latch);
		channel.subscribe(target);
		final StringMessage message = new StringMessage("test");
		new Thread(new Runnable() {
			public void run() {
				channel.send(message);
			}
		}, "test-thread").start();
		latch.await(1000, TimeUnit.MILLISECONDS);
		assertEquals("test-thread", target.threadName);
	}


	private static class ThreadNameExtractingTestTarget implements MessageTarget {

		private String threadName;

		private final CountDownLatch latch;


		ThreadNameExtractingTestTarget() {
			this(null);
		}

		ThreadNameExtractingTestTarget(CountDownLatch latch) {
			this.latch = latch;
		}

		public boolean send(Message<?> message) {
			this.threadName = Thread.currentThread().getName();
			if (this.latch != null) {
				this.latch.countDown();
			}
			return true;
		}
	}

}
