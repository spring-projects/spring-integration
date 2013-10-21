/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.stream;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @author Mark Fisher
 */
public class ByteStreamWritingMessageHandlerTests {

	private ByteArrayOutputStream stream;

	private ByteStreamWritingMessageHandler handler;

	private QueueChannel channel;

	private PollingConsumer endpoint;

	private TestTrigger trigger = new TestTrigger();

	private ThreadPoolTaskScheduler scheduler; 


	@Before
	public void initialize() {
		stream = new ByteArrayOutputStream();
		handler = new ByteStreamWritingMessageHandler(stream);
		this.channel = new QueueChannel(10);
		this.endpoint = new PollingConsumer(channel, handler);
		scheduler = new ThreadPoolTaskScheduler();
		this.endpoint.setTaskScheduler(scheduler);
		scheduler.afterPropertiesSet();
		trigger.reset();
		endpoint.setTrigger(trigger);
		endpoint.setBeanFactory(mock(BeanFactory.class));
	}

	@After
	public void stop() throws Exception {
		scheduler.destroy();
	}


	@Test
	public void singleByteArray() {
		handler.handleMessage(new GenericMessage<byte[]>(new byte[] {1,2,3}));
		byte[] result = stream.toByteArray();
		assertEquals(3, result.length);
		assertEquals(1, result[0]);
		assertEquals(2, result[1]);
		assertEquals(3, result[2]);
	}

	@Test
	public void singleString() {
		handler.handleMessage(new GenericMessage<String>("foo"));
		byte[] result = stream.toByteArray();
		assertEquals(3, result.length);
		assertEquals("foo", new String(result));
	}

	@Test
	public void maxMessagesPerTaskSameAsMessageCount() {
		endpoint.setTrigger(trigger);
		endpoint.setMaxMessagesPerPoll(3);
		channel.send(new GenericMessage<byte[]>(new byte[] {1,2,3}), 0);
		channel.send(new GenericMessage<byte[]>(new byte[] {4,5,6}), 0);
		channel.send(new GenericMessage<byte[]>(new byte[] {7,8,9}), 0);
		endpoint.start();
		trigger.await();
		endpoint.stop();
		byte[] result = stream.toByteArray();
		assertEquals(9, result.length);
		assertEquals(1, result[0]);
		assertEquals(9, result[8]);
	}

	@Test
	public void maxMessagesPerTaskLessThanMessageCount() {
		endpoint.setTrigger(trigger);
		endpoint.setMaxMessagesPerPoll(2);
		channel.send(new GenericMessage<byte[]>(new byte[] {1,2,3}), 0);
		channel.send(new GenericMessage<byte[]>(new byte[] {4,5,6}), 0);
		channel.send(new GenericMessage<byte[]>(new byte[] {7,8,9}), 0);
		endpoint.start();
		trigger.await();
		endpoint.stop();
		byte[] result = stream.toByteArray();
		assertEquals(6, result.length);
		assertEquals(1, result[0]);
	}

	@Test
	public void maxMessagesPerTaskExceedsMessageCount() {
		endpoint.setTrigger(trigger);
		endpoint.setMaxMessagesPerPoll(5);
		endpoint.setReceiveTimeout(0);
		channel.send(new GenericMessage<byte[]>(new byte[] {1,2,3}), 0);
		channel.send(new GenericMessage<byte[]>(new byte[] {4,5,6}), 0);
		channel.send(new GenericMessage<byte[]>(new byte[] {7,8,9}), 0);
		endpoint.start();
		trigger.await();
		endpoint.stop();
		byte[] result = stream.toByteArray();
		assertEquals(9, result.length);
		assertEquals(1, result[0]);
	}

	@Test
	public void testMaxMessagesLessThanMessageCountWithMultipleDispatches() {
		endpoint.setTrigger(trigger);
		endpoint.setMaxMessagesPerPoll(2);
		endpoint.setReceiveTimeout(0);
		channel.send(new GenericMessage<byte[]>(new byte[] {1,2,3}), 0);
		channel.send(new GenericMessage<byte[]>(new byte[] {4,5,6}), 0);
		channel.send(new GenericMessage<byte[]>(new byte[] {7,8,9}), 0);
		endpoint.start();
		trigger.await();
		endpoint.stop();
		byte[] result1 = stream.toByteArray();
		assertEquals(6, result1.length);
		assertEquals(1, result1[0]);
		trigger.reset();
		endpoint.start();
		trigger.await();
		endpoint.stop();
		byte[] result2 = stream.toByteArray();
		assertEquals(9, result2.length);
		assertEquals(1, result2[0]);
		assertEquals(7, result2[6]);
	}

	@Test
	public void testMaxMessagesExceedsMessageCountWithMultipleDispatches() {
		endpoint.setTrigger(trigger);
		endpoint.setMaxMessagesPerPoll(5);
		endpoint.setReceiveTimeout(0);
		channel.send(new GenericMessage<byte[]>(new byte[] {1,2,3}), 0);
		channel.send(new GenericMessage<byte[]>(new byte[] {4,5,6}), 0);
		channel.send(new GenericMessage<byte[]>(new byte[] {7,8,9}), 0);
		endpoint.start();
		trigger.await();
		endpoint.stop();
		byte[] result1 = stream.toByteArray();
		assertEquals(9, result1.length);
		assertEquals(1, result1[0]);
		trigger.reset();
		endpoint.start();
		trigger.await();
		endpoint.stop();
		byte[] result2 = stream.toByteArray();
		assertEquals(9, result2.length);	
		assertEquals(1, result2[0]);
	}

	@Test
	public void testStreamResetBetweenDispatches() {
		endpoint.setMaxMessagesPerPoll(2);
		endpoint.setTrigger(trigger);
		endpoint.setReceiveTimeout(0);
		channel.send(new GenericMessage<byte[]>(new byte[] {1,2,3}), 0);
		channel.send(new GenericMessage<byte[]>(new byte[] {4,5,6}), 0);
		channel.send(new GenericMessage<byte[]>(new byte[] {7,8,9}), 0);
		endpoint.start();
		trigger.await();
		endpoint.stop();
		byte[] result1 = stream.toByteArray();
		assertEquals(6, result1.length);
		stream.reset();
		trigger.reset();
		endpoint.start();
		trigger.await();
		endpoint.stop();
		byte[] result2 = stream.toByteArray();
		assertEquals(3, result2.length);
		assertEquals(7, result2[0]);
	}

	@Test
	public void testStreamWriteBetweenDispatches() throws IOException {
		endpoint.setTrigger(trigger);
		endpoint.setMaxMessagesPerPoll(2);
		endpoint.setReceiveTimeout(0);
		channel.send(new GenericMessage<byte[]>(new byte[] {1,2,3}), 0);
		channel.send(new GenericMessage<byte[]>(new byte[] {4,5,6}), 0);
		channel.send(new GenericMessage<byte[]>(new byte[] {7,8,9}), 0);
		endpoint.start();
		trigger.await();
		endpoint.stop();
		byte[] result1 = stream.toByteArray();
		assertEquals(6, result1.length);
		stream.write(new byte[] {123});
		stream.flush();
		trigger.reset();
		endpoint.start();
		trigger.await();
		endpoint.stop();
		byte[] result2 = stream.toByteArray();
		assertEquals(10, result2.length);
		assertEquals(1, result2[0]);
		assertEquals(123, result2[6]);
		assertEquals(7, result2[7]);
	}


	private static class TestTrigger implements Trigger {

		private final AtomicBoolean hasRun = new AtomicBoolean();

		private volatile CountDownLatch latch = new CountDownLatch(1);

		public Date nextExecutionTime(TriggerContext triggerContext) {
			if (!hasRun.getAndSet(true)) {
				return new Date();
			}
			this.latch.countDown();
			return null;
		}

		public void reset() {
			this.latch = new CountDownLatch(1);
			this.hasRun.set(false);
		}

		public void await() {
			try {
				this.latch.await(3000, TimeUnit.MILLISECONDS);
				if (latch.getCount() != 0) {
					throw new RuntimeException("test timeout");
				}
			}
			catch (InterruptedException e) {
				throw new RuntimeException("test latch.await() interrupted");
			}
		}
	}

}
