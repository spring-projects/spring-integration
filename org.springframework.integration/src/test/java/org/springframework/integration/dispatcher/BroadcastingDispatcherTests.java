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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import org.springframework.integration.endpoint.HandlerEndpoint;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.handler.TestHandlers;
import org.springframework.integration.message.MessageTarget;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class BroadcastingDispatcherTests {

	@Test
	public void testPublishSubscribe() throws InterruptedException {
		BroadcastingDispatcher dispatcher = new BroadcastingDispatcher();
		final CountDownLatch latch = new CountDownLatch(2);
		final AtomicInteger counter1 = new AtomicInteger();
		final AtomicInteger counter2 = new AtomicInteger();
		dispatcher.addTarget(createEndpoint(TestHandlers.countingCountDownHandler(counter1, latch)));
		dispatcher.addTarget(createEndpoint(TestHandlers.countingCountDownHandler(counter2, latch)));
		dispatcher.send(new StringMessage("test"));
		latch.await(500, TimeUnit.MILLISECONDS);
		assertEquals(0, latch.getCount());
		assertEquals(1, counter1.get());
		assertEquals(1, counter2.get());
	}


	private static MessageTarget createEndpoint(MessageHandler handler) {
		HandlerEndpoint endpoint = new HandlerEndpoint(handler);
		endpoint.start();
		return endpoint;
	}

}
