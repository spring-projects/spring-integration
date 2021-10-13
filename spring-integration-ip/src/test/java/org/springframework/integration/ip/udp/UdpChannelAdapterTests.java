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
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.util.SocketTestUtils;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.rule.Log4j2LevelAdjuster;
import org.springframework.messaging.Message;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.util.SocketUtils;

/**
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @author Marcin Pilaczynski
 *
 * @since 2.0
 *
 */
public class UdpChannelAdapterTests {

	@Rule
	public Log4j2LevelAdjuster adjuster = Log4j2LevelAdjuster.trace();

	@Rule
	public MulticastRule multicastRule = new MulticastRule();

	@Test
	public void testUnicastReceiver() throws Exception {
		testUnicastReceiver(false, false);
	}

	@Test
	public void testUnicastReceiverLocalNicOnly() throws Exception {
		testUnicastReceiver(false, true);
	}

	@Test
	public void testUnicastReceiverDeadExecutor() throws Exception {
		testUnicastReceiver(true, false);
	}

	private void testUnicastReceiver(final boolean killExecutor, boolean useLocalAddress) throws Exception {
		QueueChannel channel = new QueueChannel(2);
		final CountDownLatch stopLatch = new CountDownLatch(1);
		final CountDownLatch exitLatch = new CountDownLatch(1);
		final AtomicBoolean stopping = new AtomicBoolean();
		final AtomicReference<Exception> exceptionHolder = new AtomicReference<>();
		UnicastReceivingChannelAdapter adapter = new UnicastReceivingChannelAdapter(0) {

			@Override
			public boolean isActive() {
				if (stopping.get()) {
					try {
						stopLatch.await(10, TimeUnit.SECONDS);
					}
					catch (InterruptedException e) {
						fail("Test is interrupted");
					}
					return true;
				}
				else {
					return super.isActive();
				}
			}

			@Override
			protected DatagramPacket receive() throws IOException {
				if (stopping.get()) {
					return new DatagramPacket(new byte[0], 0);
				}
				else {
					return super.receive();
				}
			}

			@Override
			protected boolean asyncSendMessage(DatagramPacket packet) {
				boolean result = false;
				try {
					result = super.asyncSendMessage(packet);
				}
				catch (Exception e) {
					exceptionHolder.set(e);
				}
				if (stopping.get()) {
					exitLatch.countDown();
				}
				return result;
			}

			@Override
			public Executor getTaskExecutor() {
				Executor taskExecutor = super.getTaskExecutor();
				if (killExecutor && taskExecutor != null) {
					((ExecutorService) taskExecutor).shutdown();
				}
				return taskExecutor;
			}

		};
		adapter.setOutputChannel(channel);
		if (useLocalAddress) {
			adapter.setLocalAddress("127.0.0.1");
		}
		adapter.start();
		SocketTestUtils.waitListening(adapter);
		int port = adapter.getPort();

		Message<byte[]> message = MessageBuilder.withPayload("ABCD".getBytes()).build();
		DatagramPacketMessageMapper mapper = new DatagramPacketMessageMapper();
		DatagramPacket packet = mapper.fromMessage(message);
		packet.setSocketAddress(new InetSocketAddress("localhost", port));
		DatagramSocket datagramSocket = new DatagramSocket(0);
		datagramSocket.send(packet);
		datagramSocket.close();
		@SuppressWarnings("unchecked")
		Message<byte[]> receivedMessage = (Message<byte[]>) channel.receive(10000);
		assertThat(receivedMessage).isNotNull();
		assertThat(new String(receivedMessage.getPayload())).isEqualTo(new String(message.getPayload()));
		stopping.set(true);
		adapter.stop();
		stopLatch.countDown();
		exitLatch.await(10, TimeUnit.SECONDS);
		// Previously it failed with NPE
		assertThat(exceptionHolder.get()).isNull();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testUnicastReceiverWithReply() throws Exception {
		QueueChannel channel = new QueueChannel(2);
		UnicastReceivingChannelAdapter adapter = new UnicastReceivingChannelAdapter(0);
		adapter.setOutputChannel(channel);
		adapter.start();
		SocketTestUtils.waitListening(adapter);
		int port = adapter.getPort();

		Message<byte[]> message = MessageBuilder.withPayload("ABCD".getBytes()).build();
		DatagramPacketMessageMapper mapper = new DatagramPacketMessageMapper();
		DatagramPacket packet = mapper.fromMessage(message);
		packet.setSocketAddress(new InetSocketAddress("localhost", port));
		final DatagramSocket socket = new DatagramSocket(0);
		socket.send(packet);
		final AtomicReference<DatagramPacket> theAnswer = new AtomicReference<DatagramPacket>();
		final CountDownLatch receiverReadyLatch = new CountDownLatch(1);
		final CountDownLatch replyReceivedLatch = new CountDownLatch(1);
		//main thread sends the reply using the headers, this thread will receive it
		new SimpleAsyncTaskExecutor("testUnicastReceiverWithReply-")
				.execute(() -> {
					DatagramPacket answer = new DatagramPacket(new byte[2000], 2000);
					try {
						receiverReadyLatch.countDown();
						socket.receive(answer);
						theAnswer.set(answer);
						replyReceivedLatch.countDown();
					}
					catch (IOException e) {
						e.printStackTrace();
					}
				});
		Message<byte[]> receivedMessage = (Message<byte[]>) channel.receive(10000);
		assertThat(new String(receivedMessage.getPayload())).isEqualTo(new String(message.getPayload()));
		String replyString = "reply:" + System.currentTimeMillis();
		byte[] replyBytes = replyString.getBytes();
		DatagramPacket reply = new DatagramPacket(replyBytes, replyBytes.length);
		reply.setSocketAddress(new InetSocketAddress(
				(String) receivedMessage.getHeaders().get(IpHeaders.IP_ADDRESS),
				(Integer) receivedMessage.getHeaders().get(IpHeaders.PORT)));
		assertThat(receiverReadyLatch.await(10, TimeUnit.SECONDS)).isTrue();
		DatagramSocket datagramSocket = new DatagramSocket();
		datagramSocket.send(reply);
		assertThat(replyReceivedLatch.await(10, TimeUnit.SECONDS)).isTrue();
		DatagramPacket answerPacket = theAnswer.get();
		assertThat(answerPacket).isNotNull();
		assertThat(new String(answerPacket.getData(), 0, answerPacket.getLength())).isEqualTo(replyString);
		datagramSocket.close();
		socket.close();
		adapter.stop();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testUnicastSender() {
		QueueChannel channel = new QueueChannel(2);
		UnicastReceivingChannelAdapter adapter = new UnicastReceivingChannelAdapter(0);
		adapter.setBeanName("test");
		adapter.setOutputChannel(channel);
		adapter.start();
		SocketTestUtils.waitListening(adapter);
		int port = adapter.getPort();

		UnicastSendingMessageHandler handler = new UnicastSendingMessageHandler(
				"localhost", port, false, true,
				"localhost",
				0, 5000);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		handler.start();
		Message<byte[]> message = MessageBuilder.withPayload("ABCD".getBytes()).build();
		handler.handleMessage(message);
		Message<byte[]> receivedMessage = (Message<byte[]>) channel.receive(10000);
		assertThat(new String(receivedMessage.getPayload())).isEqualTo(new String(message.getPayload()));
		adapter.stop();
		handler.stop();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testMulticastReceiver() throws Exception {
		QueueChannel channel = new QueueChannel(2);
		MulticastReceivingChannelAdapter adapter =
				new MulticastReceivingChannelAdapter(this.multicastRule.getGroup(), 0);
		adapter.setOutputChannel(channel);
		NetworkInterface nic = this.multicastRule.getNic();
		if (nic != null) {
			adapter.setLocalAddress(nic.getInetAddresses().nextElement().getHostName());
		}
		adapter.start();
		SocketTestUtils.waitListening(adapter);
		int port = adapter.getPort();

		Message<byte[]> message = MessageBuilder.withPayload("ABCD".getBytes()).build();
		DatagramPacketMessageMapper mapper = new DatagramPacketMessageMapper();
		DatagramPacket packet = mapper.fromMessage(message);
		packet.setSocketAddress(new InetSocketAddress(this.multicastRule.getGroup(), port));
		InetAddress inetAddress = null;
		if (nic != null) {
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

		}
		DatagramSocket datagramSocket =
				new DatagramSocket(SocketUtils.findAvailableUdpPort(), inetAddress);
		datagramSocket.send(packet);
		datagramSocket.close();

		Message<byte[]> receivedMessage = (Message<byte[]>) channel.receive(10000);
		assertThat(receivedMessage).isNotNull();
		assertThat(new String(receivedMessage.getPayload())).isEqualTo(new String(message.getPayload()));
		adapter.stop();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testMulticastSender() {
		QueueChannel channel = new QueueChannel(2);
		UnicastReceivingChannelAdapter adapter =
				new MulticastReceivingChannelAdapter(this.multicastRule.getGroup(), 0);
		adapter.setOutputChannel(channel);
		NetworkInterface nic = this.multicastRule.getNic();
		if (nic != null) {
			adapter.setLocalAddress(nic.getInetAddresses().nextElement().getHostName());
		}
		adapter.start();
		SocketTestUtils.waitListening(adapter);

		MulticastSendingMessageHandler handler =
				new MulticastSendingMessageHandler(this.multicastRule.getGroup(), adapter.getPort());
		if (nic != null) {
			handler.setLocalAddress(nic.getInetAddresses().nextElement().getHostName());
		}
		Message<byte[]> message = MessageBuilder.withPayload("ABCD".getBytes()).build();
		handler.handleMessage(message);

		Message<byte[]> receivedMessage = (Message<byte[]>) channel.receive(10000);
		assertThat(receivedMessage).isNotNull();
		assertThat(new String(receivedMessage.getPayload())).isEqualTo(new String(message.getPayload()));
		adapter.stop();
		handler.stop();
	}

	@Test
	public void testUnicastReceiverException() throws Exception {
		SubscribableChannel channel = new DirectChannel();
		UnicastReceivingChannelAdapter adapter = new UnicastReceivingChannelAdapter(0);
		adapter.setOutputChannel(channel);
		ServiceActivatingHandler handler = new ServiceActivatingHandler(new FailingService());
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		channel.subscribe(handler);
		QueueChannel errorChannel = new QueueChannel();
		adapter.setErrorChannel(errorChannel);
		adapter.start();
		SocketTestUtils.waitListening(adapter);
		int port = adapter.getPort();

		Message<byte[]> message = MessageBuilder.withPayload("ABCD".getBytes()).build();
		DatagramPacketMessageMapper mapper = new DatagramPacketMessageMapper();
		DatagramPacket packet = mapper.fromMessage(message);
		packet.setSocketAddress(new InetSocketAddress("localhost", port));
		DatagramSocket datagramSocket = new DatagramSocket(0);
		datagramSocket.send(packet);
		datagramSocket.close();
		Message<?> receivedMessage = errorChannel.receive(10000);
		assertThat(receivedMessage).isNotNull();
		assertThat(((Exception) receivedMessage.getPayload()).getCause().getMessage()).isEqualTo("Failed");
		adapter.stop();
	}

	@Test
	public void testSocketExpression() throws Exception {
		ConfigurableApplicationContext context =
				new ClassPathXmlApplicationContext("testIp-socket-expression-context.xml", getClass());
		UnicastReceivingChannelAdapter adapter = context.getBean(UnicastReceivingChannelAdapter.class);
		SocketTestUtils.waitListening(adapter);
		int receiverServerPort = adapter.getPort();
		DatagramPacket packet = new DatagramPacket("foo".getBytes(), 3);
		packet.setSocketAddress(new InetSocketAddress("localhost", receiverServerPort));
		DatagramSocket socket = new DatagramSocket();
		socket.send(packet);
		socket.receive(packet);
		assertThat(new String(packet.getData())).isEqualTo("FOO");
		assertThat(packet.getPort()).isEqualTo(receiverServerPort);
		socket.close();
		context.close();
	}

	private class FailingService {

		@SuppressWarnings("unused")
		public String serviceMethod(byte[] bytes) {
			throw new RuntimeException("Failed");
		}

	}

}
