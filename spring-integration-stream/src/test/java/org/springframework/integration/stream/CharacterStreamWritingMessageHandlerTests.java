/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.stream;

import java.io.StringWriter;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class CharacterStreamWritingMessageHandlerTests {

	private StringWriter writer;

	private CharacterStreamWritingMessageHandler handler;

	private QueueChannel channel;

	private PollingConsumer endpoint;

	private final TestTrigger trigger = new TestTrigger();

	private ThreadPoolTaskScheduler scheduler;

	@BeforeEach
	public void initialize() {
		writer = new StringWriter();
		handler = new CharacterStreamWritingMessageHandler(writer);
		this.channel = new QueueChannel(10);
		trigger.reset();
		this.endpoint = new PollingConsumer(channel, handler);
		scheduler = new ThreadPoolTaskScheduler();
		this.endpoint.setTaskScheduler(scheduler);
		scheduler.afterPropertiesSet();
		trigger.reset();
		endpoint.setTrigger(trigger);
		endpoint.setBeanFactory(mock(BeanFactory.class));
	}

	@AfterEach
	public void stop() {
		scheduler.destroy();
	}

	@Test
	public void singleString() {
		handler.handleMessage(new GenericMessage<>("foo"));
		assertThat(writer.toString()).isEqualTo("foo");
	}

	@Test
	public void twoStringsAndNoNewLinesByDefault() {
		endpoint.setMaxMessagesPerPoll(1);
		endpoint.setTrigger(trigger);
		channel.send(new GenericMessage<>("foo"), 0);
		channel.send(new GenericMessage<>("bar"), 0);
		endpoint.start();
		trigger.await();
		endpoint.stop();
		assertThat(writer.toString()).isEqualTo("foo");
		trigger.reset();
		endpoint.start();
		trigger.await();
		endpoint.stop();
		assertThat(writer.toString()).isEqualTo("foobar");
	}

	@Test
	public void twoStringsWithNewLines() {
		handler.setShouldAppendNewLine(true);
		endpoint.setTrigger(trigger);
		endpoint.setMaxMessagesPerPoll(1);
		channel.send(new GenericMessage<>("foo"), 0);
		channel.send(new GenericMessage<>("bar"), 0);
		endpoint.start();
		trigger.await();
		endpoint.stop();
		String newLine = System.getProperty("line.separator");
		assertThat(writer.toString()).isEqualTo("foo" + newLine);
		trigger.reset();
		endpoint.start();
		trigger.await();
		endpoint.stop();
		assertThat(writer.toString()).isEqualTo("foo" + newLine + "bar" + newLine);
	}

	@Test
	public void maxMessagesPerTaskSameAsMessageCount() {
		endpoint.setTrigger(trigger);
		endpoint.setMaxMessagesPerPoll(2);
		channel.send(new GenericMessage<>("foo"), 0);
		channel.send(new GenericMessage<>("bar"), 0);
		endpoint.start();
		trigger.await();
		endpoint.stop();
		assertThat(writer.toString()).isEqualTo("foobar");
	}

	@Test
	public void maxMessagesPerTaskExceedsMessageCountWithAppendedNewLines() {
		endpoint.setTrigger(trigger);
		endpoint.setMaxMessagesPerPoll(10);
		endpoint.setReceiveTimeout(0);
		handler.setShouldAppendNewLine(true);
		channel.send(new GenericMessage<>("foo"), 0);
		channel.send(new GenericMessage<>("bar"), 0);
		endpoint.start();
		trigger.await();
		endpoint.stop();
		String newLine = System.getProperty("line.separator");
		assertThat(writer.toString()).isEqualTo("foo" + newLine + "bar" + newLine);
	}

	@Test
	public void singleNonStringObject() {
		endpoint.setTrigger(trigger);
		endpoint.setMaxMessagesPerPoll(1);
		TestObject testObject = new TestObject("foo");
		channel.send(new GenericMessage<>(testObject));
		endpoint.start();
		trigger.await();
		endpoint.stop();
		assertThat(writer.toString()).isEqualTo("foo");
	}

	@Test
	public void twoNonStringObjectWithOutNewLines() {
		endpoint.setReceiveTimeout(0);
		endpoint.setTrigger(trigger);
		endpoint.setMaxMessagesPerPoll(2);
		TestObject testObject1 = new TestObject("foo");
		TestObject testObject2 = new TestObject("bar");
		channel.send(new GenericMessage<>(testObject1), 0);
		channel.send(new GenericMessage<>(testObject2), 0);
		endpoint.start();
		trigger.await();
		endpoint.stop();
		assertThat(writer.toString()).isEqualTo("foobar");
	}

	@Test
	public void twoNonStringObjectWithNewLines() {
		handler.setShouldAppendNewLine(true);
		endpoint.setReceiveTimeout(0);
		endpoint.setMaxMessagesPerPoll(2);
		endpoint.setTrigger(trigger);
		TestObject testObject1 = new TestObject("foo");
		TestObject testObject2 = new TestObject("bar");
		channel.send(new GenericMessage<>(testObject1), 0);
		channel.send(new GenericMessage<>(testObject2), 0);
		endpoint.start();
		trigger.await();
		endpoint.stop();
		String newLine = System.getProperty("line.separator");
		assertThat(writer.toString()).isEqualTo("foo" + newLine + "bar" + newLine);
	}

	private record TestObject(String text) {

		@Override
		public String toString() {
			return this.text;
		}

	}

	private static class TestTrigger implements Trigger {

		private final AtomicBoolean hasRun = new AtomicBoolean();

		private volatile CountDownLatch latch = new CountDownLatch(1);

		TestTrigger() {
			super();
		}

		@Override
		public Instant nextExecution(TriggerContext triggerContext) {
			if (!hasRun.getAndSet(true)) {
				return Instant.now();
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
				this.latch.await(10000, TimeUnit.MILLISECONDS);
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
