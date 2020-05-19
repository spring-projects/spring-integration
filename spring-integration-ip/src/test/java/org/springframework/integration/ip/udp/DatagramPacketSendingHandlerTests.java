/*
 * Copyright 2002-2020 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Marcin Pilaczynski
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class DatagramPacketSendingHandlerTests {

	@Test
	public void verifySend() throws Exception {
		byte[] buffer = new byte[8];
		final DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
		final CountDownLatch received = new CountDownLatch(1);
		final AtomicInteger testPort = new AtomicInteger();
		final CountDownLatch listening = new CountDownLatch(1);
		new SimpleAsyncTaskExecutor()
				.execute(() -> {
					try {
						DatagramSocket socket = new DatagramSocket();
						testPort.set(socket.getLocalPort());
						listening.countDown();
						socket.receive(receivedPacket);
						received.countDown();
						socket.close();
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				});
		assertThat(listening.await(10, TimeUnit.SECONDS)).isTrue();
		UnicastSendingMessageHandler handler =
				new UnicastSendingMessageHandler("localhost", testPort.get());
		String payload = "foo";
		handler.handleMessage(MessageBuilder.withPayload(payload).build());
		assertThat(received.await(3000, TimeUnit.MILLISECONDS)).isTrue();
		byte[] src = receivedPacket.getData();
		int length = receivedPacket.getLength();
		int offset = receivedPacket.getOffset();
		byte[] dest = new byte[length];
		System.arraycopy(src, offset, dest, 0, length);
		assertThat(new String(dest)).isEqualTo(payload);
		handler.stop();
	}

	@Test
	public void verifySendWithAck() throws Exception {

		final AtomicInteger testPort = new AtomicInteger();
		final AtomicInteger ackPort = new AtomicInteger();

		byte[] buffer = new byte[1000];
		final DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
		final CountDownLatch listening = new CountDownLatch(1);
		final CountDownLatch ackListening = new CountDownLatch(1);
		final CountDownLatch ackSent = new CountDownLatch(1);
		new SimpleAsyncTaskExecutor()
				.execute(() -> {
					try {
						DatagramSocket socket = new DatagramSocket();
						testPort.set(socket.getLocalPort());
						listening.countDown();
						assertThat(ackListening.await(10, TimeUnit.SECONDS)).isTrue();
						socket.receive(receivedPacket);
						socket.close();
						DatagramPacketMessageMapper mapper = new DatagramPacketMessageMapper();
						mapper.setAcknowledge(true);
						mapper.setLengthCheck(true);
						Message<byte[]> message = mapper.toMessage(receivedPacket);
						Object id = message.getHeaders().get(IpHeaders.ACK_ID);
						byte[] ack = id.toString().getBytes();
						DatagramPacket ackPack = new DatagramPacket(ack, ack.length,
								new InetSocketAddress("localHost", ackPort.get()));
						DatagramSocket out = new DatagramSocket();
						out.send(ackPack);
						out.close();
						ackSent.countDown();
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				});
		listening.await(10000, TimeUnit.MILLISECONDS);
		UnicastSendingMessageHandler handler =
				new UnicastSendingMessageHandler("localhost", testPort.get(), true, true, "localhost", 0, 5000);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		handler.start();
		waitAckListening(handler);
		ackPort.set(handler.getAckPort());
		ackListening.countDown();
		String payload = "foobar";
		handler.handleMessage(MessageBuilder.withPayload(payload).build());
		assertThat(ackSent.await(10000, TimeUnit.MILLISECONDS)).isTrue();
		byte[] src = receivedPacket.getData();
		int length = receivedPacket.getLength();
		int offset = receivedPacket.getOffset();
		byte[] dest = new byte[6];
		System.arraycopy(src, offset + length - 6, dest, 0, 6);
		assertThat(new String(dest)).isEqualTo(payload);
		handler.stop();
	}

	public void waitAckListening(UnicastSendingMessageHandler handler) {
		await("Handler not listening").atMost(Duration.ofSeconds(10)).until(() -> handler.getAckPort() > 0);
	}

}
