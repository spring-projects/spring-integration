/*
 * Copyright 2002-2021 the original author or authors.
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
import static org.mockito.Mockito.mock;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class DatagramPacketMulticastSendingHandlerTests {

	@Rule
	public MulticastRule multicastRule = new MulticastRule();

	@Test
	public void verifySendMulticast() throws Exception {
		MulticastSocket socket;
		try {
			socket = new MulticastSocket();
		}
		catch (Exception e) {
			return;
		}
		final int testPort = socket.getLocalPort();
		final String multicastAddress = this.multicastRule.getGroup();
		final String payload = "foo";
		final CountDownLatch listening = new CountDownLatch(2);
		final CountDownLatch received = new CountDownLatch(2);
		Runnable catcher = () -> {
			try {
				byte[] buffer = new byte[8];
				DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
				MulticastSocket socket1 = new MulticastSocket(testPort);
				socket1.setNetworkInterface(multicastRule.getNic());
				InetAddress group = InetAddress.getByName(multicastAddress);
				socket1.joinGroup(new InetSocketAddress(group, 0), null);
				listening.countDown();
				socket1.receive(receivedPacket);
				socket1.close();
				byte[] src = receivedPacket.getData();
				int length = receivedPacket.getLength();
				int offset = receivedPacket.getOffset();
				byte[] dest = new byte[length];
				System.arraycopy(src, offset, dest, 0, length);
				assertThat(new String(dest)).isEqualTo(payload);
				received.countDown();
			}
			catch (Exception e) {
				listening.countDown();
				e.printStackTrace();
			}
		};
		Executor executor = new SimpleAsyncTaskExecutor("verifySendMulticast-");
		executor.execute(catcher);
		executor.execute(catcher);
		assertThat(listening.await(10000, TimeUnit.MILLISECONDS)).isTrue();
		MulticastSendingMessageHandler handler = new MulticastSendingMessageHandler(multicastAddress, testPort);
		handler.setBeanFactory(mock(BeanFactory.class));
		NetworkInterface nic = this.multicastRule.getNic();
		if (nic != null) {
			String hostName = null;
			Enumeration<InetAddress> addressesFromNetworkInterface = nic.getInetAddresses();
			while (addressesFromNetworkInterface.hasMoreElements()) {
				InetAddress inetAddress = addressesFromNetworkInterface.nextElement();
				if (inetAddress.isSiteLocalAddress()
						&& !inetAddress.isAnyLocalAddress()
						&& !inetAddress.isLinkLocalAddress()
						&& !inetAddress.isLoopbackAddress()) {

					hostName = inetAddress.getHostName();
					break;
				}
			}

			handler.setLocalAddress(hostName);
		}
		handler.afterPropertiesSet();
		handler.handleMessage(MessageBuilder.withPayload(payload).build());
		assertThat(received.await(10000, TimeUnit.MILLISECONDS)).isTrue();
		handler.stop();
		socket.close();
	}

	@Test
	public void verifySendMulticastWithAcks() throws Exception {

		MulticastSocket socket;
		try {
			socket = new MulticastSocket();
		}
		catch (Exception e) {
			return;
		}
		final int testPort = socket.getLocalPort();
		final AtomicInteger ackPort = new AtomicInteger();

		final String multicastAddress = this.multicastRule.getGroup();
		final String payload = "foobar";
		final CountDownLatch listening = new CountDownLatch(2);
		final CountDownLatch ackListening = new CountDownLatch(1);
		final CountDownLatch ackSent = new CountDownLatch(2);
		NetworkInterface nic = this.multicastRule.getNic();
		Runnable catcher = () -> {
			try {
				byte[] buffer = new byte[1000];
				DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
				MulticastSocket socket1 = new MulticastSocket(testPort);
				socket1.setNetworkInterface(multicastRule.getNic());
				socket1.setSoTimeout(8000);
				InetAddress group = InetAddress.getByName(multicastAddress);
				socket1.joinGroup(new InetSocketAddress(group, 0), null);
				listening.countDown();
				assertThat(ackListening.await(10, TimeUnit.SECONDS)).isTrue();
				socket1.receive(receivedPacket);
				socket1.close();
				byte[] src = receivedPacket.getData();
				int length = receivedPacket.getLength();
				int offset = receivedPacket.getOffset();
				byte[] dest = new byte[6];
				System.arraycopy(src, offset + length - 6, dest, 0, 6);
				assertThat(new String(dest)).isEqualTo(payload);
				DatagramPacketMessageMapper mapper = new DatagramPacketMessageMapper();
				mapper.setAcknowledge(true);
				mapper.setLengthCheck(true);
				Message<byte[]> message = mapper.toMessage(receivedPacket);
				Object id = message.getHeaders().get(IpHeaders.ACK_ID);
				byte[] ack = id.toString().getBytes();
				InetAddress inetAddress = null;
				Enumeration<InetAddress> addressesFromNetworkInterface = nic.getInetAddresses();
				while (addressesFromNetworkInterface.hasMoreElements()) {
					InetAddress address = addressesFromNetworkInterface.nextElement();
					if (address.isSiteLocalAddress()
							&& !address.isAnyLocalAddress()
							&& !address.isLinkLocalAddress()
							&& !address.isLoopbackAddress()) {

						inetAddress = address;
						break;
					}
				}
				DatagramPacket ackPack = new DatagramPacket(ack, ack.length,
						new InetSocketAddress(inetAddress, ackPort.get()));
				DatagramSocket out = new DatagramSocket();
				out.send(ackPack);
				out.close();
				ackSent.countDown();
				socket1.close();
			}
			catch (Exception e) {
				listening.countDown();
				e.printStackTrace();
			}
		};
		Executor executor = new SimpleAsyncTaskExecutor("verifySendMulticastWithAcks-");
		executor.execute(catcher);
		executor.execute(catcher);
		assertThat(listening.await(10000, TimeUnit.MILLISECONDS)).isTrue();
		MulticastSendingMessageHandler handler =
				new MulticastSendingMessageHandler(multicastAddress, testPort, true, true, "localhost", 0, 10000);

		if (nic != null) {
			String hostName = null;
			Enumeration<InetAddress> addressesFromNetworkInterface = nic.getInetAddresses();
			while (addressesFromNetworkInterface.hasMoreElements()) {
				InetAddress inetAddress = addressesFromNetworkInterface.nextElement();
				if (inetAddress.isSiteLocalAddress()
						&& !inetAddress.isAnyLocalAddress()
						&& !inetAddress.isLinkLocalAddress()
						&& !inetAddress.isLoopbackAddress()) {

					hostName = inetAddress.getHostName();
					break;
				}
			}

			handler.setLocalAddress(hostName);
		}
		handler.setMinAcksForSuccess(2);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		handler.start();
		waitAckListening(handler);
		ackPort.set(handler.getAckPort());
		ackListening.countDown();
		handler.handleMessage(MessageBuilder.withPayload(payload).build());
		assertThat(ackSent.await(10000, TimeUnit.MILLISECONDS)).isTrue();
		handler.stop();
		socket.close();
	}

	public void waitAckListening(UnicastSendingMessageHandler handler) throws InterruptedException {
		int n = 0;
		while (n++ < 100 && handler.getAckPort() == 0) {
			Thread.sleep(100);
		}
		assertThat(n < 100).isTrue();
	}

}
