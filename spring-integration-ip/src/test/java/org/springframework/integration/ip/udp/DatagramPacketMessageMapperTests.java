/*
 * Copyright 2002-2010 the original author or authors.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.junit.Test;
import org.springframework.messaging.Message;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.mapping.MessageMappingException;
import org.springframework.integration.support.MessageBuilder;

/**
 * @author Gary Russell
 * @author Dave Syer
 * @since 2.0
 */
public class DatagramPacketMessageMapperTests {

	@Test
	public void testFromToMessageNoAckNoLengthCheck() throws Exception {
		test(false, false);
	}

	@Test
	public void testFromToMessageAckNoLengthCheck() throws Exception {
		test(true, false);
	}

	@Test
	public void testFromToMessageNoAckLengthCheck() throws Exception {
		test(false, true);
	}

	@Test
	public void testFromToMessageAckLengthCheck() throws Exception {
		test(true, true);
	}

	private void test(boolean ack, boolean lengthCheck) throws Exception {
		Message<byte[]> message = MessageBuilder.withPayload("ABCD".getBytes()).build();
		DatagramPacketMessageMapper mapper = new DatagramPacketMessageMapper();
		mapper.setAckAddress("localhost:11111");
		mapper.setAcknowledge(ack);
		mapper.setLengthCheck(lengthCheck);
		DatagramPacket packet = mapper.fromMessage(message);
		packet.setSocketAddress(new InetSocketAddress("localhost", 22222));
		Message<byte[]> messageOut = mapper.toMessage(packet);
		assertEquals(new String(message.getPayload()), new String(messageOut.getPayload()));
		if (ack) {
			assertEquals(messageOut.getHeaders().get(IpHeaders.ACK_ID).toString(), 
					     message.getHeaders().getId().toString());
		}
		assertTrue(((String)messageOut.getHeaders().get(IpHeaders.HOSTNAME)).contains("localhost"));
		mapper.setLookupHost(false);
		messageOut = mapper.toMessage(packet);
		assertEquals(new String(message.getPayload()), new String(messageOut.getPayload()));
		if (ack) {
			assertEquals(messageOut.getHeaders().get(IpHeaders.ACK_ID).toString(), 
					     message.getHeaders().getId().toString());
		}
		assertFalse(((String)messageOut.getHeaders().get(IpHeaders.HOSTNAME)).contains("localhost"));		
	}

	@Test
	public void testTruncation() throws Exception {
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
			assertTrue(e.getMessage().contains("expected " + (bigLen + 4) + ", received " + (test.length() + 4)));
		}
	}

}
