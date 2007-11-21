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

package org.springframework.integration.channel;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.springframework.integration.message.DocumentMessage;
import org.springframework.integration.message.Message;

/**
 * @author Mark Fisher
 */
public class PointToPointChannelTests {

	@Test
	public void testSimpleSendAndReceive() throws Exception {
		final AtomicBoolean messageReceived = new AtomicBoolean(false);
		final CountDownLatch latch = new CountDownLatch(1);
		final PointToPointChannel channel = new PointToPointChannel();
		new Thread(new Runnable() {
			public void run() {
				Message message = channel.receive();
				if (message != null) {
					messageReceived.set(true);
					latch.countDown();
				}
			}
		}).start();
		assertFalse(messageReceived.get());
		channel.send(new DocumentMessage(1, "testing"));
		latch.await(25, TimeUnit.MILLISECONDS);
		assertTrue(messageReceived.get());
	}

	@Test
	public void testImmediateReceive() throws Exception {
		final AtomicBoolean messageReceived = new AtomicBoolean(false);
		final PointToPointChannel channel = new PointToPointChannel();
		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);
		Executor singleThreadExecutor = Executors.newSingleThreadExecutor();
		Runnable receiveTask1 = new Runnable() {
			public void run() {
				Message message = channel.receive(0);
				if (message != null) {
					messageReceived.set(true);
				}
				latch1.countDown();
			}
		};
		Runnable sendTask = new Runnable() {
			public void run() {
				channel.send(new DocumentMessage(1, "testing"));
			}
		};
		singleThreadExecutor.execute(receiveTask1);
		latch1.await();
		singleThreadExecutor.execute(sendTask);
		assertFalse(messageReceived.get());
		Runnable receiveTask2 = new Runnable() {
			public void run() {
				Message message = channel.receive(0);
				if (message != null) {
					messageReceived.set(true);
				}
				latch2.countDown();
			}
		};
		singleThreadExecutor.execute(receiveTask2);
		latch2.await();
		assertTrue(messageReceived.get());
	}

	@Test
	public void testBlockingReceive() throws Exception{
		final PointToPointChannel channel = new PointToPointChannel();
		final AtomicBoolean receiveInterrupted = new AtomicBoolean(false);
		final CountDownLatch latch = new CountDownLatch(1);
		Thread t = new Thread(new Runnable() {
			public void run() {
				Message message = channel.receive();
				receiveInterrupted.set(true);
				assertTrue(message == null);
				latch.countDown();
			}
		});
		t.start();
		assertFalse(receiveInterrupted.get());
		t.interrupt();
		latch.await();
		assertTrue(receiveInterrupted.get());
	}

	@Test
	public void testImmediateSend() {
		PointToPointChannel channel = new PointToPointChannel(3);
		boolean result1 = channel.send(new DocumentMessage(1, "test-1"));
		assertTrue(result1);
		boolean result2 = channel.send(new DocumentMessage(2, "test-2"), 100);
		assertTrue(result2);
		boolean result3 = channel.send(new DocumentMessage(3, "test-3"), 0);
		assertTrue(result3);
		boolean result4 = channel.send(new DocumentMessage(4, "test-4"), 0);
		assertFalse(result4);
	}

	@Test
	public void testBlockingSend() throws Exception{
		final PointToPointChannel channel = new PointToPointChannel(1);
		boolean result1 = channel.send(new DocumentMessage(1, "test-1"));
		assertTrue(result1);
		final AtomicBoolean sendInterrupted = new AtomicBoolean(false);
		final CountDownLatch latch = new CountDownLatch(1);
		Thread t = new Thread(new Runnable() {
			public void run() {
				channel.send(new DocumentMessage(2, "test-2"));
				sendInterrupted.set(true);
				latch.countDown();
			}
		});
		t.start();
		assertFalse(sendInterrupted.get());
		t.interrupt();
		latch.await();
		assertTrue(sendInterrupted.get());
	}

}
