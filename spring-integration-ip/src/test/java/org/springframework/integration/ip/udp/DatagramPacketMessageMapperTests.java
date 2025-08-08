/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
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
import static org.assertj.core.api.Assertions.fail;

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
		try {
			mapper.toMessage(packet);
			fail("Truncated message exception expected");
		}
		catch (MessageMappingException e) {
			assertThat(e.getMessage()).contains("expected " + (bigLen + 4) + ", received " + (test.length() + 4));
		}
	}

}
