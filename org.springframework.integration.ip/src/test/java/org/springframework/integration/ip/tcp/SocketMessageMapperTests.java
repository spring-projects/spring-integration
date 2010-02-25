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
package org.springframework.integration.ip.tcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Test;
import org.springframework.integration.core.Message;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.message.MessageBuilder;

/**
 * @author Gary Russell
 *
 */
public class SocketMessageMapperTests {

	/**
	 * 
	 */
	private static final String TEST_PAYLOAD = "abcdefghijkl";

	/**
	 * Test method for {@link org.springframework.integration.ip.tcp.SocketMessageMapper#toMessage(org.springframework.integration.ip.tcp.SocketReader)}.
	 * Tests segmented reads into the payload and verifies reassembly.
	 */
	@Test
	public void testToMessage() throws Exception {
		SocketMessageMapper mapper = new SocketMessageMapper();
		Message<byte[]> message = mapper.toMessage(new StubSocketReader());
		assertEquals(TEST_PAYLOAD, new String((byte[]) message.getPayload()));
		assertEquals(InetAddress.getLocalHost().getHostName(), message
				.getHeaders().get(IpHeaders.HOSTNAME));
		assertEquals(InetAddress.getLocalHost().getHostAddress(), message
				.getHeaders().get(IpHeaders.IP_ADDRESS));
	}

	/**
	 * Test method for {@link org.springframework.integration.ip.tcp.SocketMessageMapper#fromMessage(org.springframework.integration.core.Message)}.
	 * @throws Exception 
	 */
	@Test
	public void testFromMessage() throws Exception {
		String s = "test";
		Message<String> message = MessageBuilder.withPayload(s).build();
		SocketMessageMapper mapper = new SocketMessageMapper();
		byte[] bArray = mapper.fromMessage(message);
		assertEquals(s, new String(bArray));
		
	}


	private class StubSocketReader implements SocketReader {
	
		/* (non-Javadoc)
		 * @see org.springframework.integration.ip.tcp.SocketReader#getAddress()
		 */
		public InetAddress getAddress() {
			try {
				return InetAddress.getLocalHost();
			} catch (UnknownHostException e) {
				fail("Unexpected Exception: " + e.getMessage());
			}
			return null;
		}
	
		public byte[] getAssembledData() {
			return TEST_PAYLOAD.getBytes();
		}

		/* (non-Javadoc)
		 * @see org.springframework.integration.ip.tcp.SocketReader#assembleData()
		 */
		public boolean assembleData() {
			return false;
		}

		
	}
	
}