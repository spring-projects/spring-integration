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

package org.springframework.integration.ip.udp;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Ignore;
import org.junit.Test;

import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.ip.util.SocketTestUtils;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * For both .net. and .nio. adapters, creates a single server and 10 clients
 * and sends 3 messages from each client to the associated server.
 * Ensures that all messages are correctly assembled and received ok.
 * Since udp is inherently unreliable, we have to single thread our requests
 * through a blocking queue, to get a reliable test case. Otherwise collisions
 * will cause messages to be lost.
 * Even with this restriction, we are still testing the receiving adapter's
 * ability to handle multiple requests from multiple clients.
 *
 * @author Gary Russell
 * @author Gunnar Hillert
 *
 */
public class MultiClientTests {

	@SuppressWarnings("unchecked")
	@Test
	@Ignore
	public void testNoAck() throws Exception {
		final String payload = largePayload(1000);
		final UnicastReceivingChannelAdapter adapter =
				new UnicastReceivingChannelAdapter(0);
		int drivers = 10;
		adapter.setPoolSize(drivers);
		QueueChannel queue = new QueueChannel(drivers * 3);
		adapter.setOutputChannel(queue);
		adapter.start();
		final QueueChannel queueIn = new QueueChannel(1000);
		SocketTestUtils.waitListening(adapter);

		final AtomicBoolean done = new AtomicBoolean();

		for (int i = 0; i < drivers; i++) {
			Thread t = new Thread(() -> {
				UnicastSendingMessageHandler sender = new UnicastSendingMessageHandler(
						"localhost", adapter.getPort());
				sender.start();
				while (true) {
					Message<?> message = queueIn.receive();
					sender.handleMessage(message);
					if (done.get()) {
						break;
					}
				}
				sender.stop();
			});
			t.setDaemon(true);
			t.start();
		}
		for (int i = 0; i < drivers * 3; i++) {
			queueIn.send(MessageBuilder.withPayload(payload).build());
		}
		for (int i = 0; i < drivers * 3; i++) {
			Message<byte[]> messageOut = (Message<byte[]>) queue.receive(10000);
			assertThat(messageOut).isNotNull();
			assertThat(new String(messageOut.getPayload())).isEqualTo(payload);
		}
		adapter.stop();
		done.set(true);
	}

	@SuppressWarnings("unchecked")
	@Test
	@Ignore
	public void testAck() throws Exception {
		final String payload = largePayload(1000);
		final UnicastReceivingChannelAdapter adapter =
				new UnicastReceivingChannelAdapter(0, false);
		int drivers = 5;
		adapter.setPoolSize(drivers);
		QueueChannel queue = new QueueChannel(drivers * 3);
		adapter.setOutputChannel(queue);
		adapter.start();
		final QueueChannel queueIn = new QueueChannel(1000);
		SocketTestUtils.waitListening(adapter);

		final AtomicBoolean done = new AtomicBoolean();

		for (int i = 0; i < drivers; i++) {
			Thread t = new Thread(() -> {
				UnicastSendingMessageHandler sender = new UnicastSendingMessageHandler(
						"localhost", adapter.getPort(),
						false, true, "localhost", 0,
						10000);
				sender.start();
				while (true) {
					Message<?> message = queueIn.receive();
					sender.handleMessage(message);
					if (done.get()) {
						break;
					}
				}
				sender.stop();
			});
			t.setDaemon(true);
			t.start();
		}
		for (int i = 0; i < drivers * 3; i++) {
			queueIn.send(MessageBuilder.withPayload(payload).build());
		}
		for (int i = 0; i < drivers * 3; i++) {
			Message<byte[]> messageOut = (Message<byte[]>) queue.receive(20000);
			assertThat(messageOut).isNotNull();
			assertThat(new String(messageOut.getPayload())).isEqualTo(payload);
		}
		adapter.stop();
		done.set(true);
	}

	@SuppressWarnings("unchecked")
	@Test
	@Ignore
	public void testAckWithLength() throws Exception {
		final String payload = largePayload(1000);
		final UnicastReceivingChannelAdapter adapter =
				new UnicastReceivingChannelAdapter(0, true);
		int drivers = 10;
		adapter.setPoolSize(drivers);
		QueueChannel queue = new QueueChannel(drivers * 3);
		adapter.setOutputChannel(queue);
		adapter.start();
		final QueueChannel queueIn = new QueueChannel(1000);
		SocketTestUtils.waitListening(adapter);

		final AtomicBoolean done = new AtomicBoolean();

		for (int i = 0; i < drivers; i++) {
			Thread t = new Thread(() -> {
				UnicastSendingMessageHandler sender = new UnicastSendingMessageHandler(
						"localhost", adapter.getPort(),
						true, true, "localhost", 0,
						10000);
				sender.start();
				while (true) {
					Message<?> message = queueIn.receive();
					sender.handleMessage(message);
					if (done.get()) {
						break;
					}
				}
				sender.stop();
			});
			t.setDaemon(true);
			t.start();
		}
		for (int i = 0; i < drivers * 3; i++) {
			queueIn.send(MessageBuilder.withPayload(payload).build());
		}
		for (int i = 0; i < drivers * 3; i++) {
			Message<byte[]> messageOut = (Message<byte[]>) queue.receive(10000);
			assertThat(messageOut).isNotNull();
			assertThat(new String(messageOut.getPayload())).isEqualTo(payload);
		}
		adapter.stop();
		done.set(true);
	}

	private String largePayload(int n) {
		StringBuilder sb = new StringBuilder(n);
		for (int i = 0; i < n; i++) {
			sb.append('x');
		}
		return sb.toString();
	}

}
