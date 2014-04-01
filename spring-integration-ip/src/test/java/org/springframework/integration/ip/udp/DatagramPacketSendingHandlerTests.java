/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.ip.udp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.LogFactory;
import org.junit.Ignore;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.SocketUtils;
import org.springframework.messaging.Message;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
public class DatagramPacketSendingHandlerTests {

	private boolean noMulticast;

	@Test
	public void verifySend() throws Exception {
		final int testPort = SocketUtils.findAvailableUdpSocket();
		byte[] buffer = new byte[8];
		final DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
		final CountDownLatch latch = new CountDownLatch(1);
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			@Override
			public void run() {
				try {
					DatagramSocket socket = new DatagramSocket(testPort);
					socket.receive(receivedPacket);
					latch.countDown();
					socket.close();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		Thread.sleep(1000);
		UnicastSendingMessageHandler handler =
				new UnicastSendingMessageHandler("localhost", testPort);
		String payload = "foo";
		handler.handleMessage(MessageBuilder.withPayload(payload).build());
		assertTrue(latch.await(3000, TimeUnit.MILLISECONDS));
		byte[] src = receivedPacket.getData();
		int length = receivedPacket.getLength();
		int offset = receivedPacket.getOffset();
		byte[] dest = new byte[length];
		System.arraycopy(src, offset, dest, 0, length);
		assertEquals(payload, new String(dest));
		handler.stop();
	}

	@Test
	public void verifySendWithAck() throws Exception {

		final List<Integer> openPorts = SocketUtils.findAvailableUdpSockets(SocketUtils.getRandomSeedPort(), 2);

		final int testPort = openPorts.get(0);
		final int ackPort = openPorts.get(1);

		byte[] buffer = new byte[1000];
		final DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);
		UnicastSendingMessageHandler handler =
				new UnicastSendingMessageHandler("localhost", testPort, true,
						true, "localhost", ackPort, 5000);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		handler.start();
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			@Override
			public void run() {
				try {
					DatagramSocket socket = new DatagramSocket(testPort);
					latch1.countDown();
					socket.receive(receivedPacket);
					socket.close();
					DatagramPacketMessageMapper mapper = new DatagramPacketMessageMapper();
					mapper.setAcknowledge(true);
					mapper.setLengthCheck(true);
					Message<byte[]> message = mapper.toMessage(receivedPacket);
					Object id = message.getHeaders().get(IpHeaders.ACK_ID);
					byte[] ack = id.toString().getBytes();
					DatagramPacket ackPack = new DatagramPacket(ack, ack.length,
							                        new InetSocketAddress("localHost", ackPort));
					DatagramSocket out = new DatagramSocket();
					out.send(ackPack);
					out.close();
					latch2.countDown();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		latch1.await(3000, TimeUnit.MILLISECONDS);
		String payload = "foobar";
		handler.handleMessage(MessageBuilder.withPayload(payload).build());
		assertTrue(latch2.await(10000, TimeUnit.MILLISECONDS));
		byte[] src = receivedPacket.getData();
		int length = receivedPacket.getLength();
		int offset = receivedPacket.getOffset();
		byte[] dest = new byte[6];
		System.arraycopy(src, offset+length-6, dest, 0, 6);
		assertEquals(payload, new String(dest));
		handler.stop();
	}

	@Test
	@Ignore
	public void verifySendMulticast() throws Exception {
		final int testPort = SocketUtils.findAvailableUdpSocket();
		final String multicastAddress = "225.6.7.8";
		final String payload = "foo";
		final CountDownLatch latch1 = new CountDownLatch(2);
		final CountDownLatch latch2 = new CountDownLatch(2);
		Runnable catcher = new Runnable() {
			@Override
			public void run() {
				try {
					byte[] buffer = new byte[8];
					DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
					MulticastSocket socket = new MulticastSocket(testPort);
					InetAddress group = InetAddress.getByName(multicastAddress);
					socket.joinGroup(group);
					latch1.countDown();
					LogFactory.getLog(getClass())
						.debug(Thread.currentThread().getName() + " waiting for packet");
					socket.receive(receivedPacket);
					socket.close();
					byte[] src = receivedPacket.getData();
					int length = receivedPacket.getLength();
					int offset = receivedPacket.getOffset();
					byte[] dest = new byte[length];
					System.arraycopy(src, offset, dest, 0, length);
					assertEquals(payload, new String(dest));
					LogFactory.getLog(getClass())
						.debug(Thread.currentThread().getName() + " received packet");
					latch2.countDown();
				}
				catch (Exception e) {
					noMulticast = true;
					latch1.countDown();
					e.printStackTrace();
				}
			}
		};
		Executor executor = Executors.newFixedThreadPool(2);
		executor.execute(catcher);
		executor.execute(catcher);
		latch1.await(3000, TimeUnit.MILLISECONDS);
		if (noMulticast) {
			return;
		}
		MulticastSendingMessageHandler handler = new MulticastSendingMessageHandler(multicastAddress, testPort);
		handler.handleMessage(MessageBuilder.withPayload(payload).build());
		assertTrue(latch2.await(3000, TimeUnit.MILLISECONDS));
		handler.stop();
	}

	@Test
	@Ignore
	public void verifySendMulticastWithAcks() throws Exception {

		final List<Integer> openPorts = SocketUtils.findAvailableUdpSockets(SocketUtils.getRandomSeedPort(), 2);

		final int testPort = openPorts.get(0);
		final int ackPort = openPorts.get(1);

		final String multicastAddress = "225.6.7.8";
		final String payload = "foobar";
		final CountDownLatch latch1 = new CountDownLatch(2);
		final CountDownLatch latch2 = new CountDownLatch(2);
		Runnable catcher = new Runnable() {
			@Override
			public void run() {
				try {
					byte[] buffer = new byte[1000];
					DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
					MulticastSocket socket = new MulticastSocket(testPort);
					InetAddress group = InetAddress.getByName(multicastAddress);
					socket.joinGroup(group);
					latch1.countDown();
					LogFactory.getLog(getClass()).debug(Thread.currentThread().getName() + " waiting for packet");
					socket.receive(receivedPacket);
					socket.close();
					byte[] src = receivedPacket.getData();
					int length = receivedPacket.getLength();
					int offset = receivedPacket.getOffset();
					byte[] dest = new byte[6];
					System.arraycopy(src, offset+length-6, dest, 0, 6);
					assertEquals(payload, new String(dest));
					LogFactory.getLog(getClass()).debug(Thread.currentThread().getName() + " received packet");
					DatagramPacketMessageMapper mapper = new DatagramPacketMessageMapper();
					mapper.setAcknowledge(true);
					mapper.setLengthCheck(true);
					Message<byte[]> message = mapper.toMessage(receivedPacket);
					Object id = message.getHeaders().get(IpHeaders.ACK_ID);
					byte[] ack = id.toString().getBytes();
					DatagramPacket ackPack = new DatagramPacket(ack, ack.length,
							                        new InetSocketAddress("localHost", ackPort));
					DatagramSocket out = new DatagramSocket();
					out.send(ackPack);
					out.close();
					latch2.countDown();
				}
				catch (Exception e) {
					noMulticast = true;
					latch1.countDown();
					e.printStackTrace();
				}
			}
		};
		Executor executor = Executors.newFixedThreadPool(2);
		executor.execute(catcher);
		executor.execute(catcher);
		latch1.await(3000, TimeUnit.MILLISECONDS);
		if (noMulticast) {
			return;
		}
		MulticastSendingMessageHandler handler =
			new MulticastSendingMessageHandler(multicastAddress, testPort, true,
                    							true, "localhost", ackPort, 500000);
		handler.setMinAcksForSuccess(2);
		handler.afterPropertiesSet();
		handler.start();
		handler.handleMessage(MessageBuilder.withPayload(payload).build());
		assertTrue(latch2.await(10000, TimeUnit.MILLISECONDS));
		handler.stop();
	}

}
