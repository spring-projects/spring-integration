/*
 * Copyright 2026 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.ip.udp.inbound;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * @author Glenn Renfro
 *
 * @since 5.5.22
 */
class UnicastReceivingChannelAdapterTests {

	static final String LOCALHOST_IP = "127.0.0.1";

	@Test
	void sendAckSendsToAckAddressInHeader() throws Exception {
		UnicastReceivingChannelAdapter adapter = new UnicastReceivingChannelAdapter(0);
		adapter.setTrustedAckAddresses(LOCALHOST_IP + ":*");

		try (DatagramSocket serverSocket = new DatagramSocket(0)) {
			int port = serverSocket.getLocalPort();
			serverSocket.setSoTimeout(5000);

			UUID ackId = UUID.randomUUID();
			Message<byte[]> message = MessageBuilder.withPayload(new byte[0])
					.setHeader(IpHeaders.ACK_ADDRESS, LOCALHOST_IP + ":" + port)
					.setHeader(IpHeaders.ACK_ID, ackId)
					.build();

			adapter.sendAck(message);

			byte[] buf = new byte[1024];
			DatagramPacket packet = expectPacketFrom(buf, serverSocket, LOCALHOST_IP);
			assertThat(packet.getData()).contains(ackId.toString().getBytes());
		}
	}

	@Test
	void noTrustedPatternsSkipsAck() throws Exception {
		try (DatagramSocket serverSocket = new DatagramSocket(0)) {
			serverSocket.setSoTimeout(200);
			TrackingAdapter adapter = new TrackingAdapter();
			adapter.doSend(buildAckPacket(LOCALHOST_IP, serverSocket.getLocalPort()));

			byte[] buf = new byte[1024];
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			assertThatThrownBy(() -> serverSocket.receive(packet))
					.isInstanceOf(SocketTimeoutException.class);
		}
	}

	@Test
	void trustedExactPatternSendsAck() throws Exception {
		try (DatagramSocket serverSocket = new DatagramSocket(0)) {
			serverSocket.setSoTimeout(5000);
			int port = serverSocket.getLocalPort();
			TrackingAdapter adapter = new TrackingAdapter();
			adapter.setTrustedAckAddresses(LOCALHOST_IP + ":" + port);
			adapter.doSend(buildAckPacket(LOCALHOST_IP, port));

			byte[] buf = new byte[1024];
			expectPacketFrom(buf, serverSocket, LOCALHOST_IP);
		}
	}

	@Test
	void trustedWildcardPatternSendsAck() throws Exception {
		try (DatagramSocket serverSocket = new DatagramSocket(0)) {
			serverSocket.setSoTimeout(5000);
			int port = serverSocket.getLocalPort();
			TrackingAdapter adapter = new TrackingAdapter();
			adapter.setTrustedAckAddresses(LOCALHOST_IP + ":*");
			adapter.doSend(buildAckPacket(LOCALHOST_IP, port));

			byte[] buf = new byte[1024];
			expectPacketFrom(buf, serverSocket, LOCALHOST_IP);
		}
	}

	@Test
	void untrustedAddressDoesNotMatchPattern() throws Exception {
		try (DatagramSocket serverSocket = new DatagramSocket(0)) {
			serverSocket.setSoTimeout(200);
			TrackingAdapter adapter = new TrackingAdapter();
			adapter.setTrustedAckAddresses("192.168.1.*:*");
			assertThatThrownBy(() -> adapter.doSend(buildAckPacket(LOCALHOST_IP, serverSocket.getLocalPort())))
					.isInstanceOf(MessagingException.class);
		}
	}

	@Test
	void fullTrustedAckAddress() throws Exception {
		try (DatagramSocket serverSocket = new DatagramSocket(0)) {
			serverSocket.setSoTimeout(5000);
			int port = serverSocket.getLocalPort();
			TrackingAdapter adapter = new TrackingAdapter();
			adapter.setTrustedAckAddresses("*");
			adapter.doSend(buildAckPacket(LOCALHOST_IP, port));

			byte[] buf = new byte[1024];
			expectPacketFrom(buf, serverSocket, LOCALHOST_IP);
		}
	}

	/**
	 * Build a raw UDP packet carrying ACK headers as UnicastSendingMessageHandler would.
	 */
	private static DatagramPacket buildAckPacket(String ackHost, int ackPort) throws Exception {
		UUID ackId = UUID.randomUUID();
		String ackAddress = ackHost + ":" + ackPort;
		String headers = IpHeaders.ACK_ADDRESS + "=" + ackAddress + ";"
				+ MessageHeaders.ID + "=" + ackId + ";";
		byte[] headerBytes = headers.getBytes();
		byte[] payload = "hello".getBytes();
		byte[] data = ByteBuffer.allocate(headerBytes.length + payload.length)
				.put(headerBytes).put(payload).array();
		return new DatagramPacket(data, data.length, InetAddress.getByName(LOCALHOST_IP), 0);
	}

	private static DatagramPacket expectPacketFrom(byte[] buf, DatagramSocket serverSocket, String address) throws IOException {
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		serverSocket.receive(packet);
		assertThat(packet.getAddress().getHostAddress()).isEqualTo(address);
		return packet;
	}

	private class TrackingAdapter extends UnicastReceivingChannelAdapter {

		TrackingAdapter() {
			super(0);
			setBeanFactory(mock(BeanFactory.class));
			setApplicationEventPublisher(mock(ApplicationEventPublisher.class));
			afterPropertiesSet();
		}
	}

}
