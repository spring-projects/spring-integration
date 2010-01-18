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

package org.springframework.integration.ip;

import static org.junit.Assert.assertEquals;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.junit.Ignore;
import org.junit.Test;

import org.springframework.integration.core.Message;
import org.springframework.integration.ip.udp.DatagramPacketMessageMapper;
import org.springframework.integration.message.MessageBuilder;

/**
 * @author Gary Russell
 * @since 2.0
 */
public class DatagramPacketMessageMapperTests {

	@Test
	public void testFromToMessage() throws Exception {
		test(false, false);
		test(true, false);
		test(false, true);
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
			assertEquals(message.getHeaders().getId().toString(), 
					     messageOut.getHeaders().getId().toString());
		}
	}

	@Test
	@Ignore
	public void testTruncation() throws Exception {
		Message<byte[]> message = MessageBuilder.withPayload("ABCD".getBytes()).build();
		DatagramPacketMessageMapper mapper = new DatagramPacketMessageMapper();
		mapper.setAckAddress("localhost:11111");
		mapper.setAcknowledge(false);
		mapper.setLengthCheck(true);
		DatagramPacket packet = mapper.fromMessage(message);
		// Force a truncation failure
		ByteBuffer bb = ByteBuffer.wrap(packet.getData());
		bb.putInt(99999);
		packet.setSocketAddress(new InetSocketAddress("localhost", 22222));
		Message<byte[]> messageOut = mapper.toMessage(packet);
		assertEquals(new String(message.getPayload()), new String(messageOut.getPayload()));
	}

}
