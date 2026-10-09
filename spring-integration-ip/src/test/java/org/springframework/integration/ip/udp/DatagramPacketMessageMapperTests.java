/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ip.udp;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.mapping.MessageMappingException;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * @author Gary Russell
 * @author Dave Syer
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class DatagramPacketMessageMapperTests {

	@Test
	public void testFromToMessageNoAckNoLengthCheck() {
		test(false, false);
	}

	@Test
	public void testFromToMessageAckNoLengthCheck() {
		test(true, false);
	}

	@Test
	public void testFromToMessageNoAckLengthCheck() {
		test(false, true);
	}

	@Test
	public void testFromToMessageAckLengthCheck() {
		test(true, true);
	}

	private void test(boolean ack, boolean lengthCheck) {
		Message<byte[]> message = MessageBuilder.withPayload("ABCD".getBytes()).build();
		DatagramPacketMessageMapper mapper = new DatagramPacketMessageMapper();
		mapper.setAckAddress("localhost:11111");
		mapper.setAcknowledge(ack);
		mapper.setLengthCheck(lengthCheck);
		mapper.setBeanFactory(mock());
		DatagramPacket packet = mapper.fromMessage(message);
		packet.setSocketAddress(new InetSocketAddress("localhost", 22222));
		Message<byte[]> messageOut = mapper.toMessage(packet);
		assertThat(new String(messageOut.getPayload())).isEqualTo(new String(message.getPayload()));
		if (ack) {
			assertThat(message.getHeaders().getId().toString())
					.isEqualTo(messageOut.getHeaders().get(IpHeaders.ACK_ID).toString());
		}
		assertThat(((String) messageOut.getHeaders().get(IpHeaders.HOSTNAME))).doesNotContain("localhost");
		mapper.setLookupHost(true);
		messageOut = mapper.toMessage(packet);
		assertThat(new String(messageOut.getPayload())).isEqualTo(new String(message.getPayload()));
		if (ack) {
			assertThat(message.getHeaders().getId().toString())
					.isEqualTo(messageOut.getHeaders().get(IpHeaders.ACK_ID).toString());
		}
		assertThat(((String) messageOut.getHeaders().get(IpHeaders.HOSTNAME))).contains("localhost");
	}

	@Test
	public void testAckHeaderWithMultibyteAddress() {
		Message<byte[]> message = MessageBuilder.withPayload("ABCD".getBytes()).build();
		DatagramPacketMessageMapper mapper = new DatagramPacketMessageMapper();
		mapper.setAckAddress("café:11111");
		mapper.setAcknowledge(true);
		mapper.setBeanFactory(mock());
		DatagramPacket packet = mapper.fromMessage(message);
		packet.setSocketAddress(new InetSocketAddress("localhost", 22222));
		Message<byte[]> messageOut = mapper.toMessage(packet);
		assertThat(messageOut.getPayload()).isEqualTo(message.getPayload());
		assertThat(messageOut.getHeaders().get(IpHeaders.ACK_ADDRESS)).isEqualTo("café:11111");
	}

	@Test
	public void testAckHeaderWithMultibyteAddressLengthCheck() {
		Message<byte[]> message = MessageBuilder.withPayload("ABCD".getBytes()).build();
		DatagramPacketMessageMapper mapper = new DatagramPacketMessageMapper();
		mapper.setAckAddress("café:11111");
		mapper.setAcknowledge(true);
		mapper.setLengthCheck(true);
		mapper.setBeanFactory(mock());
		DatagramPacket packet = mapper.fromMessage(message);
		packet.setSocketAddress(new InetSocketAddress("localhost", 22222));
		Message<byte[]> messageOut = mapper.toMessage(packet);
		assertThat(messageOut.getPayload()).isEqualTo(message.getPayload());
	}

	@Test
	public void testTruncation() {
		String test = "ABCD";
		Message<byte[]> message = MessageBuilder.withPayload(test.getBytes()).build();
		DatagramPacketMessageMapper mapper = new DatagramPacketMessageMapper();
		mapper.setAckAddress("localhost:11111");
		mapper.setAcknowledge(false);
		mapper.setLengthCheck(true);
		DatagramPacket packet = mapper.fromMessage(message);
		// Force a truncation failure
		ByteBuffer bb = ByteBuffer.wrap(packet.getData());
		int bigLen = 99999;
		bb.putInt(bigLen);
		packet.setSocketAddress(new InetSocketAddress("localhost", 22222));

		assertThatExceptionOfType(MessageMappingException.class)
				.isThrownBy(() -> mapper.toMessage(packet))
				.withMessageContaining("expected " + (bigLen + 4) + ", received " + (test.length() + 4));
	}

}
