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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.test.util.OnlyOnceTrigger;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class ByteStreamWritingMessageHandlerTests {

	private ByteArrayOutputStream stream;

	private ByteStreamWritingMessageHandler handler;

	private QueueChannel channel;

	private PollingConsumer endpoint;

	private final OnlyOnceTrigger trigger = new OnlyOnceTrigger();

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
		handler.handleMessage(new GenericMessage<byte[]>(new byte[] {1, 2, 3}));
		byte[] result = stream.toByteArray();
		assertThat(result.length).isEqualTo(3);
		assertThat(result[0]).isEqualTo((byte) 1);
		assertThat(result[1]).isEqualTo((byte) 2);
		assertThat(result[2]).isEqualTo((byte) 3);
	}

	@Test
	public void singleString() {
		handler.handleMessage(new GenericMessage<String>("foo"));
		byte[] result = stream.toByteArray();
		assertThat(result.length).isEqualTo(3);
		assertThat(new String(result)).isEqualTo("foo");
	}

	@Test
	public void maxMessagesPerTaskSameAsMessageCount() {
		endpoint.setTrigger(trigger);
		endpoint.setMaxMessagesPerPoll(3);
		channel.send(new GenericMessage<byte[]>(new byte[] {1, 2, 3}), 0);
		channel.send(new GenericMessage<byte[]>(new byte[] {4, 5, 6}), 0);
		channel.send(new GenericMessage<byte[]>(new byte[] {7, 8, 9}), 0);
		endpoint.start();
		trigger.await();
		endpoint.stop();
		byte[] result = stream.toByteArray();
		assertThat(result.length).isEqualTo(9);
		assertThat(result[0]).isEqualTo((byte) 1);
		assertThat(result[8]).isEqualTo((byte) 9);
	}

	@Test
	public void maxMessagesPerTaskLessThanMessageCount() {
		endpoint.setTrigger(trigger);
		endpoint.setMaxMessagesPerPoll(2);
		channel.send(new GenericMessage<byte[]>(new byte[] {1, 2, 3}), 0);
		channel.send(new GenericMessage<byte[]>(new byte[] {4, 5, 6}), 0);
		channel.send(new GenericMessage<byte[]>(new byte[] {7, 8, 9}), 0);
		endpoint.start();
		trigger.await();
		endpoint.stop();
		byte[] result = stream.toByteArray();
		assertThat(result.length).isEqualTo(6);
		assertThat(result[0]).isEqualTo((byte) 1);
	}

	@Test
	public void maxMessagesPerTaskExceedsMessageCount() {
		endpoint.setTrigger(trigger);
		endpoint.setMaxMessagesPerPoll(5);
		endpoint.setReceiveTimeout(0);
		channel.send(new GenericMessage<byte[]>(new byte[] {1, 2, 3}), 0);
		channel.send(new GenericMessage<byte[]>(new byte[] {4, 5, 6}), 0);
		channel.send(new GenericMessage<byte[]>(new byte[] {7, 8, 9}), 0);
		endpoint.start();
		trigger.await();
		endpoint.stop();
		byte[] result = stream.toByteArray();
		assertThat(result.length).isEqualTo(9);
		assertThat(result[0]).isEqualTo((byte) 1);
	}

	@Test
	public void testMaxMessagesLessThanMessageCountWithMultipleDispatches() {
		endpoint.setTrigger(trigger);
		endpoint.setMaxMessagesPerPoll(2);
		endpoint.setReceiveTimeout(0);
		channel.send(new GenericMessage<byte[]>(new byte[] {1, 2, 3}), 0);
		channel.send(new GenericMessage<byte[]>(new byte[] {4, 5, 6}), 0);
		channel.send(new GenericMessage<byte[]>(new byte[] {7, 8, 9}), 0);
		endpoint.start();
		trigger.await();
		endpoint.stop();
		byte[] result1 = stream.toByteArray();
		assertThat(result1.length).isEqualTo(6);
		assertThat(result1[0]).isEqualTo((byte) 1);
		trigger.reset();
		endpoint.start();
		trigger.await();
		endpoint.stop();
		byte[] result2 = stream.toByteArray();
		assertThat(result2.length).isEqualTo(9);
		assertThat(result2[0]).isEqualTo((byte) 1);
		assertThat(result2[6]).isEqualTo((byte) 7);
	}

	@Test
	public void testMaxMessagesExceedsMessageCountWithMultipleDispatches() {
		endpoint.setTrigger(trigger);
		endpoint.setMaxMessagesPerPoll(5);
		endpoint.setReceiveTimeout(0);
		channel.send(new GenericMessage<byte[]>(new byte[] {1, 2, 3}), 0);
		channel.send(new GenericMessage<byte[]>(new byte[] {4, 5, 6}), 0);
		channel.send(new GenericMessage<byte[]>(new byte[] {7, 8, 9}), 0);
		endpoint.start();
		trigger.await();
		endpoint.stop();
		byte[] result1 = stream.toByteArray();
		assertThat(result1.length).isEqualTo(9);
		assertThat(result1[0]).isEqualTo((byte) 1);
		trigger.reset();
		endpoint.start();
		trigger.await();
		endpoint.stop();
		byte[] result2 = stream.toByteArray();
		assertThat(result2.length).isEqualTo(9);
		assertThat(result2[0]).isEqualTo((byte) 1);
	}

	@Test
	public void testStreamResetBetweenDispatches() {
		endpoint.setMaxMessagesPerPoll(2);
		endpoint.setTrigger(trigger);
		endpoint.setReceiveTimeout(0);
		channel.send(new GenericMessage<byte[]>(new byte[] {1, 2, 3}), 0);
		channel.send(new GenericMessage<byte[]>(new byte[] {4, 5, 6}), 0);
		channel.send(new GenericMessage<byte[]>(new byte[] {7, 8, 9}), 0);
		endpoint.start();
		trigger.await();
		endpoint.stop();
		byte[] result1 = stream.toByteArray();
		assertThat(result1.length).isEqualTo(6);
		stream.reset();
		trigger.reset();
		endpoint.start();
		trigger.await();
		endpoint.stop();
		byte[] result2 = stream.toByteArray();
		assertThat(result2.length).isEqualTo(3);
		assertThat(result2[0]).isEqualTo((byte) 7);
	}

	@Test
	public void testStreamWriteBetweenDispatches() throws IOException {
		endpoint.setTrigger(trigger);
		endpoint.setMaxMessagesPerPoll(2);
		endpoint.setReceiveTimeout(0);
		channel.send(new GenericMessage<byte[]>(new byte[] {1, 2, 3}), 0);
		channel.send(new GenericMessage<byte[]>(new byte[] {4, 5, 6}), 0);
		channel.send(new GenericMessage<byte[]>(new byte[] {7, 8, 9}), 0);
		endpoint.start();
		trigger.await();
		endpoint.stop();
		byte[] result1 = stream.toByteArray();
		assertThat(result1.length).isEqualTo(6);
		stream.write(new byte[] {123});
		stream.flush();
		trigger.reset();
		endpoint.start();
		trigger.await();
		endpoint.stop();
		byte[] result2 = stream.toByteArray();
		assertThat(result2.length).isEqualTo(10);
		assertThat(result2[0]).isEqualTo((byte) 1);
		assertThat(result2[6]).isEqualTo((byte) 123);
		assertThat(result2[7]).isEqualTo((byte) 7);
	}

}
