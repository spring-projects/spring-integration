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

package org.springframework.integration.handler;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class ResponseCorrelatorTests {

	@Test
	public void testReceiversPrecedeResponse() throws InterruptedException {
		final ResponseCorrelator correlator = new ResponseCorrelator(10);
		final AtomicInteger responseCounter = new AtomicInteger();
		CountDownLatch latch = startReceivers(correlator, responseCounter, 5, 500);
		Message<?> message = new StringMessage("test");
		message.getHeader().setCorrelationId("123");
		correlator.handle(message);
		latch.await(1000, TimeUnit.MILLISECONDS);
		assertEquals(0, latch.getCount());
		assertEquals(1, responseCounter.get());
	}

	@Test
	public void testResponsePrecedeReceivers() throws InterruptedException {
		final ResponseCorrelator correlator = new ResponseCorrelator(10);
		Message<?> message = new StringMessage("test");
		message.getHeader().setCorrelationId("123");
		correlator.handle(message);
		final AtomicInteger responseCounter = new AtomicInteger();
		CountDownLatch latch = startReceivers(correlator, responseCounter, 5, 50);
		latch.await(1000, TimeUnit.MILLISECONDS);
		assertEquals(0, latch.getCount());
		assertEquals(1, responseCounter.get());
	}


	private static CountDownLatch startReceivers(final ResponseCorrelator correlator,
			final AtomicInteger responseCounter, int numReceivers, final long timeout) {
		final CountDownLatch latch = new CountDownLatch(numReceivers);
		Executor executor = Executors.newFixedThreadPool(numReceivers);
		for (int i = 0; i < numReceivers; i++) {
			executor.execute(new Runnable() {
				public void run() {
					Message<?> response = correlator.getResponse("123", timeout);
					if (response != null) {
						responseCounter.incrementAndGet();
					}
					latch.countDown();
				}
			});
		}
		return latch;
	}

}
