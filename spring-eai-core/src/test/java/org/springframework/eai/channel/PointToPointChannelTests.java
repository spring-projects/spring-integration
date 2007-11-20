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

package org.springframework.eai.channel;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import org.springframework.eai.message.DocumentMessage;

/**
 * @author Mark Fisher
 */
public class PointToPointChannelTests {

	@Test
	public void testSimpleReceive() throws Exception {
		final AtomicBoolean messageReceived = new AtomicBoolean(false);
		final CountDownLatch latch = new CountDownLatch(1);
		final PointToPointChannel channel = new PointToPointChannel();
		new Thread(new Runnable() {
			public void run() {
				channel.receive();
				messageReceived.set(true);
				latch.countDown();
			}
		}).start();
		assertFalse(messageReceived.get());
		channel.send(new DocumentMessage(1, "testing"));
		latch.await(25, TimeUnit.MILLISECONDS);
		assertTrue(messageReceived.get());
	}

}
