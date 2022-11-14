/*
 * Copyright 2002-2022 the original author or authors.
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
