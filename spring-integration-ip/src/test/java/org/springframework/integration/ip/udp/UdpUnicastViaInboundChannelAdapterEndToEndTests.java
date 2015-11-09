/*
 * Copyright 2002-2015 the original author or authors.
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

import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.springframework.integration.ip.util.SocketTestUtils.waitListening;

/**
 * Sends and receives a simple message through to the Udp channel adapters, where Outbound
 * Channel Adapter uses Inbound Channel Adapter datagram socket to send outgoing
 * communication instead of creating own separate datagram socket.
 *
 * @author Marcin Pilaczynski
 * @since 4.3
 */
public class UdpUnicastViaInboundChannelAdapterEndToEndTests implements Runnable {

	private volatile CountDownLatch inboundAdapterReadyToReceive = new CountDownLatch(1);

	private volatile CountDownLatch messagesExchanged = new CountDownLatch(1);

	@Test
	public void runIt() throws Exception {
		Thread t = new Thread(this);
		t.start(); // launch the receiver

		inboundAdapterReadyToReceive.await(10, TimeUnit.SECONDS);

		DatagramSocket clientSocket = new DatagramSocket();
		InetAddress inetAddress = InetAddress.getByName("localhost");
		final int SERVER_PORT = 11111;
		final String TEST_TEXT = "Text in message payload";
		byte[] sendData;
		sendData = TEST_TEXT.getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
				inetAddress, SERVER_PORT);
		clientSocket.send(sendPacket);

		byte[] receiveData = new byte[1024];
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		clientSocket.receive(receivePacket);
		String modifiedSentence = new String(receivePacket.getData()).trim();

		assertEquals("Text sent and received are different; text", TEST_TEXT, modifiedSentence);
		assertEquals("Received datagram not from server socket; port", SERVER_PORT, receivePacket.getPort());

		messagesExchanged.countDown();
	}

	/**
	 * Instantiate the receiving context
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		@SuppressWarnings("resource")
		AbstractApplicationContext ctx = new ClassPathXmlApplicationContext(
				"testIp-via-inbound-channel-adapter-context.xml",
				UdpUnicastViaInboundChannelAdapterEndToEndTests.class);
		UnicastReceivingChannelAdapter inbound = ctx.getBean(UnicastReceivingChannelAdapter.class);

		try {
			waitListening(inbound);
		} catch (Exception e) {
			e.printStackTrace();
		}

		inboundAdapterReadyToReceive.countDown();

		QueueChannel inputChannel = ctx.getBean("inputChannel", QueueChannel.class);
		Message<byte[]> message = (Message<byte[]>) inputChannel.receive();

		MessageChannel outputChannel = ctx.getBean("outputChannel", MessageChannel.class);
		outputChannel.send(message);

		try {
			messagesExchanged.await(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		ctx.stop();
		ctx.close();
	}

	public static void main(String[] args) throws Exception {
		new UdpUnicastViaInboundChannelAdapterEndToEndTests().runIt();
	}

}
