/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.stream.outbound;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

	@BeforeEach
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

	@AfterEach
	public void stop() {
		scheduler.destroy();
	}

	@Test
	public void singleByteArray() {
		handler.handleMessage(new GenericMessage<>(new byte[] {1, 2, 3}));
		byte[] result = stream.toByteArray();
		assertThat(result).hasSize(3).containsExactly(1, 2, 3);
	}

	@Test
	public void singleString() {
		handler.handleMessage(new GenericMessage<>("test"));
		byte[] result = stream.toByteArray();
		assertThat(result).hasSize(4);
		assertThat(new String(result)).isEqualTo("test");
	}

	@Test
	public void maxMessagesPerTaskSameAsMessageCount() {
		endpoint.setTrigger(trigger);
		endpoint.setMaxMessagesPerPoll(3);
		channel.send(new GenericMessage<>(new byte[] {1, 2, 3}), 0);
		channel.send(new GenericMessage<>(new byte[] {4, 5, 6}), 0);
		channel.send(new GenericMessage<>(new byte[] {7, 8, 9}), 0);
		endpoint.start();
		trigger.await();
		endpoint.stop();
		byte[] result = stream.toByteArray();
		assertThat(result).hasSize(9);
		assertThat(result[0]).isEqualTo((byte) 1);
		assertThat(result[8]).isEqualTo((byte) 9);
	}

	@Test
	public void maxMessagesPerTaskLessThanMessageCount() {
		endpoint.setTrigger(trigger);
		endpoint.setMaxMessagesPerPoll(2);
		channel.send(new GenericMessage<>(new byte[] {1, 2, 3}), 0);
		channel.send(new GenericMessage<>(new byte[] {4, 5, 6}), 0);
		channel.send(new GenericMessage<>(new byte[] {7, 8, 9}), 0);
		endpoint.start();
		trigger.await();
		endpoint.stop();
		byte[] result = stream.toByteArray();
		assertThat(result).hasSize(6);
		assertThat(result[0]).isEqualTo((byte) 1);
	}

	@Test
	public void maxMessagesPerTaskExceedsMessageCount() {
		endpoint.setTrigger(trigger);
		endpoint.setMaxMessagesPerPoll(5);
		endpoint.setReceiveTimeout(0);
		channel.send(new GenericMessage<>(new byte[] {1, 2, 3}), 0);
		channel.send(new GenericMessage<>(new byte[] {4, 5, 6}), 0);
		channel.send(new GenericMessage<>(new byte[] {7, 8, 9}), 0);
		endpoint.start();
		trigger.await();
		endpoint.stop();
		byte[] result = stream.toByteArray();
		assertThat(result).hasSize(9);
		assertThat(result[0]).isEqualTo((byte) 1);
	}

	@Test
	public void testMaxMessagesLessThanMessageCountWithMultipleDispatches() {
		endpoint.setTrigger(trigger);
		endpoint.setMaxMessagesPerPoll(2);
		endpoint.setReceiveTimeout(0);
		channel.send(new GenericMessage<>(new byte[] {1, 2, 3}), 0);
		channel.send(new GenericMessage<>(new byte[] {4, 5, 6}), 0);
		channel.send(new GenericMessage<>(new byte[] {7, 8, 9}), 0);
		endpoint.start();
		trigger.await();
		endpoint.stop();
		byte[] result1 = stream.toByteArray();
		assertThat(result1).hasSize(6);
		assertThat(result1[0]).isEqualTo((byte) 1);
		trigger.reset();
		endpoint.start();
		trigger.await();
		endpoint.stop();
		byte[] result2 = stream.toByteArray();
		assertThat(result2).hasSize(9);
		assertThat(result2[0]).isEqualTo((byte) 1);
		assertThat(result2[6]).isEqualTo((byte) 7);
	}

	@Test
	public void testMaxMessagesExceedsMessageCountWithMultipleDispatches() {
		endpoint.setTrigger(trigger);
		endpoint.setMaxMessagesPerPoll(5);
		endpoint.setReceiveTimeout(0);
		channel.send(new GenericMessage<>(new byte[] {1, 2, 3}), 0);
		channel.send(new GenericMessage<>(new byte[] {4, 5, 6}), 0);
		channel.send(new GenericMessage<>(new byte[] {7, 8, 9}), 0);
		endpoint.start();
		trigger.await();
		endpoint.stop();
		byte[] result1 = stream.toByteArray();
		assertThat(result1).hasSize(9);
		assertThat(result1[0]).isEqualTo((byte) 1);
		trigger.reset();
		endpoint.start();
		trigger.await();
		endpoint.stop();
		byte[] result2 = stream.toByteArray();
		assertThat(result2).hasSize(9);
		assertThat(result2[0]).isEqualTo((byte) 1);
	}

	@Test
	public void testStreamResetBetweenDispatches() {
		endpoint.setMaxMessagesPerPoll(2);
		endpoint.setTrigger(trigger);
		endpoint.setReceiveTimeout(0);
		channel.send(new GenericMessage<>(new byte[] {1, 2, 3}), 0);
		channel.send(new GenericMessage<>(new byte[] {4, 5, 6}), 0);
		channel.send(new GenericMessage<>(new byte[] {7, 8, 9}), 0);
		endpoint.start();
		trigger.await();
		endpoint.stop();
		byte[] result1 = stream.toByteArray();
		assertThat(result1).hasSize(6);
		stream.reset();
		trigger.reset();
		endpoint.start();
		trigger.await();
		endpoint.stop();
		byte[] result2 = stream.toByteArray();
		assertThat(result2).hasSize(3);
		assertThat(result2[0]).isEqualTo((byte) 7);
	}

	@Test
	public void testStreamWriteBetweenDispatches() throws IOException {
		endpoint.setTrigger(trigger);
		endpoint.setMaxMessagesPerPoll(2);
		endpoint.setReceiveTimeout(0);
		channel.send(new GenericMessage<>(new byte[] {1, 2, 3}), 0);
		channel.send(new GenericMessage<>(new byte[] {4, 5, 6}), 0);
		channel.send(new GenericMessage<>(new byte[] {7, 8, 9}), 0);
		endpoint.start();
		trigger.await();
		endpoint.stop();
		byte[] result1 = stream.toByteArray();
		assertThat(result1).hasSize(6);
		stream.write(new byte[] {123});
		stream.flush();
		trigger.reset();
		endpoint.start();
		trigger.await();
		endpoint.stop();
		byte[] result2 = stream.toByteArray();
		assertThat(result2).hasSize(10);
		assertThat(result2[0]).isEqualTo((byte) 1);
		assertThat(result2[6]).isEqualTo((byte) 123);
		assertThat(result2[7]).isEqualTo((byte) 7);
	}

}
