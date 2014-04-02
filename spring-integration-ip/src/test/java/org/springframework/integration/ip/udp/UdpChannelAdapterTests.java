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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.LogFactory;
import org.junit.Ignore;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.util.SocketTestUtils;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.SocketUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.SubscribableChannel;

/**
 *
 * @author Gary Russell
 * @since 2.0
 *
 */
public class UdpChannelAdapterTests {

	@SuppressWarnings("unchecked")
	@Test
	public void testUnicastReceiver() throws Exception {
		QueueChannel channel = new QueueChannel(2);
		int port = SocketUtils.findAvailableUdpSocket();
		UnicastReceivingChannelAdapter adapter = new UnicastReceivingChannelAdapter(port);
		adapter.setOutputChannel(channel);
//		SocketUtils.setLocalNicIfPossible(adapter);
		adapter.start();
		SocketTestUtils.waitListening(adapter);

		Message<byte[]> message = MessageBuilder.withPayload("ABCD".getBytes()).build();
		DatagramPacketMessageMapper mapper = new DatagramPacketMessageMapper();
		DatagramPacket packet = mapper.fromMessage(message);
		packet.setSocketAddress(new InetSocketAddress("localhost", port));
		DatagramSocket datagramSocket = new DatagramSocket(SocketUtils.findAvailableUdpSocket());
		datagramSocket.send(packet);
		datagramSocket.close();
		Message<byte[]> receivedMessage = (Message<byte[]>) channel.receive(2000);
		assertEquals(new String(message.getPayload()), new String(receivedMessage.getPayload()));
		adapter.stop();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testUnicastReceiverWithReply() throws Exception {
		QueueChannel channel = new QueueChannel(2);
		int port = SocketUtils.findAvailableUdpSocket();
		UnicastReceivingChannelAdapter adapter = new UnicastReceivingChannelAdapter(port);
		adapter.setOutputChannel(channel);
		adapter.start();
		SocketTestUtils.waitListening(adapter);

		Message<byte[]> message = MessageBuilder.withPayload("ABCD".getBytes()).build();
		DatagramPacketMessageMapper mapper = new DatagramPacketMessageMapper();
		DatagramPacket packet = mapper.fromMessage(message);
		packet.setSocketAddress(new InetSocketAddress("localhost", port));
		final DatagramSocket socket = new DatagramSocket(SocketUtils.findAvailableUdpSocket());
		socket.send(packet);
		final AtomicReference<DatagramPacket> theAnswer = new AtomicReference<DatagramPacket>();
		final CountDownLatch receiverReadyLatch = new CountDownLatch(1);
		final CountDownLatch replyReceivedLatch = new CountDownLatch(1);
		//main thread sends the reply using the headers, this thread will receive it
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			@Override
			public void run() {
				DatagramPacket answer = new DatagramPacket(new byte[2000], 2000);
				try {
					receiverReadyLatch.countDown();
					socket.receive(answer);
					theAnswer.set(answer);
					replyReceivedLatch.countDown();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		Message<byte[]> receivedMessage = (Message<byte[]>) channel.receive(2000);
		assertEquals(new String(message.getPayload()), new String(receivedMessage.getPayload()));
		String replyString = "reply:" + System.currentTimeMillis();
		byte[] replyBytes = replyString.getBytes();
		DatagramPacket reply = new DatagramPacket(replyBytes, replyBytes.length);
		reply.setSocketAddress(new InetSocketAddress(
				(String) receivedMessage.getHeaders().get(IpHeaders.IP_ADDRESS),
				(Integer) receivedMessage.getHeaders().get(IpHeaders.PORT)));
		assertTrue(receiverReadyLatch.await(10, TimeUnit.SECONDS));
		DatagramSocket datagramSocket = new DatagramSocket();
		datagramSocket.send(reply);
		assertTrue(replyReceivedLatch.await(10, TimeUnit.SECONDS));
		DatagramPacket answerPacket = theAnswer.get();
		assertNotNull(answerPacket);
		assertEquals(replyString, new String(answerPacket.getData(), 0, answerPacket.getLength()));
		datagramSocket.close();
		socket.close();
		adapter.stop();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testUnicastSender() throws Exception {
		QueueChannel channel = new QueueChannel(2);
		int port = SocketUtils.findAvailableUdpSocket();
		UnicastReceivingChannelAdapter adapter = new UnicastReceivingChannelAdapter(port);
		adapter.setBeanName("test");
		adapter.setOutputChannel(channel);
//		SocketUtils.setLocalNicIfPossible(adapter);
		adapter.start();
		SocketTestUtils.waitListening(adapter);

//		String whichNic = SocketUtils.chooseANic(false);
		UnicastSendingMessageHandler handler = new UnicastSendingMessageHandler(
				"localhost", port, false, true,
				"localhost",
//				whichNic,
				SocketUtils.findAvailableUdpSocket(), 5000);
//		handler.setLocalAddress(whichNic);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		handler.start();
		Message<byte[]> message = MessageBuilder.withPayload("ABCD".getBytes()).build();
		handler.handleMessage(message);
		Message<byte[]> receivedMessage = (Message<byte[]>) channel.receive(2000);
		assertEquals(new String(message.getPayload()), new String(receivedMessage.getPayload()));
		adapter.stop();
		handler.stop();
	}

	@SuppressWarnings("unchecked")
	@Test @Ignore
	public void testMulticastReceiver() throws Exception {
		QueueChannel channel = new QueueChannel(2);
		int port = SocketUtils.findAvailableUdpSocket();
		MulticastReceivingChannelAdapter adapter = new MulticastReceivingChannelAdapter("225.6.7.8", port);
		adapter.setOutputChannel(channel);
		String nic = SocketTestUtils.chooseANic(true);
		if (nic == null) {	// no multicast support
			LogFactory.getLog(this.getClass()).error("No Multicast support");
			return;
		}
		adapter.setLocalAddress(nic);
		adapter.start();
		SocketTestUtils.waitListening(adapter);

		Message<byte[]> message = MessageBuilder.withPayload("ABCD".getBytes()).build();
		DatagramPacketMessageMapper mapper = new DatagramPacketMessageMapper();
		DatagramPacket packet = mapper.fromMessage(message);
		packet.setSocketAddress(new InetSocketAddress("225.6.7.8", port));
		DatagramSocket datagramSocket = new DatagramSocket(0, Inet4Address.getByName(nic));
		datagramSocket.send(packet);
		datagramSocket.close();

		Message<byte[]> receivedMessage = (Message<byte[]>) channel.receive(2000);
		assertNotNull(receivedMessage);
		assertEquals(new String(message.getPayload()), new String(receivedMessage.getPayload()));
		adapter.stop();
	}

	@SuppressWarnings("unchecked")
	@Test @Ignore
	public void testMulticastSender() throws Exception {
		QueueChannel channel = new QueueChannel(2);
		int port = SocketUtils.findAvailableUdpSocket();
		UnicastReceivingChannelAdapter adapter = new MulticastReceivingChannelAdapter("225.6.7.9", port);
		adapter.setOutputChannel(channel);
		String nic = SocketTestUtils.chooseANic(true);
		if (nic == null) {	// no multicast support
			LogFactory.getLog(this.getClass()).error("No Multicast support");
			return;
		}
		adapter.setLocalAddress(nic);
		adapter.start();
		SocketTestUtils.waitListening(adapter);

		MulticastSendingMessageHandler handler = new MulticastSendingMessageHandler("225.6.7.9", port);
		handler.setLocalAddress(nic);
		Message<byte[]> message = MessageBuilder.withPayload("ABCD".getBytes()).build();
		handler.handleMessage(message);

		Message<byte[]> receivedMessage = (Message<byte[]>) channel.receive(2000);
		assertNotNull(receivedMessage);
		assertEquals(new String(message.getPayload()), new String(receivedMessage.getPayload()));
		adapter.stop();
	}

	@Test
	public void testUnicastReceiverException() throws Exception {
		SubscribableChannel channel = new DirectChannel();
		int port = SocketUtils.findAvailableUdpSocket();
		UnicastReceivingChannelAdapter adapter = new UnicastReceivingChannelAdapter(port);
		adapter.setOutputChannel(channel);
//		SocketUtils.setLocalNicIfPossible(adapter);
		adapter.setOutputChannel(channel);
		ServiceActivatingHandler handler = new ServiceActivatingHandler(new FailingService());
		channel.subscribe(handler);
		QueueChannel errorChannel = new QueueChannel();
		adapter.setErrorChannel(errorChannel);
		adapter.start();
		SocketTestUtils.waitListening(adapter);

		Message<byte[]> message = MessageBuilder.withPayload("ABCD".getBytes()).build();
		DatagramPacketMessageMapper mapper = new DatagramPacketMessageMapper();
		DatagramPacket packet = mapper.fromMessage(message);
		packet.setSocketAddress(new InetSocketAddress("localhost", port));
		DatagramSocket datagramSocket = new DatagramSocket(SocketUtils.findAvailableUdpSocket());
		datagramSocket.send(packet);
		datagramSocket.close();
		Message<?> receivedMessage = errorChannel.receive(2000);
		assertNotNull(receivedMessage);
		assertEquals("Failed", ((Exception) receivedMessage.getPayload()).getCause().getMessage());
		adapter.stop();
	}

	private class FailingService {
		@SuppressWarnings("unused")
		public String serviceMethod(byte[] bytes) {
			throw new RuntimeException("Failed");
		}
	}


}
