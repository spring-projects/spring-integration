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
package org.springframework.integration.ip.tcp.connection;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.springframework.integration.Message;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.tcp.connection.TcpConnection;
import org.springframework.integration.ip.tcp.connection.TcpMessageMapper;
import org.springframework.integration.support.MessageBuilder;

/**
 * @author Gary Russell
 *
 */
public class TcpMessageMapperTests {

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
		
		TcpMessageMapper mapper = new TcpMessageMapper();
		TcpConnection connection = mock(TcpConnection.class);
		when(connection.getPayload()).thenReturn(TEST_PAYLOAD.getBytes());
		when(connection.getHostName()).thenReturn("MyHost");
		when(connection.getHostAddress()).thenReturn("1.1.1.1");
		when(connection.getPort()).thenReturn(1234);
		Message<Object> message = mapper.toMessage(connection);
		assertEquals(TEST_PAYLOAD, new String((byte[]) message.getPayload()));
		assertEquals("MyHost", message
				.getHeaders().get(IpHeaders.HOSTNAME));
		assertEquals("1.1.1.1", message
				.getHeaders().get(IpHeaders.IP_ADDRESS));
		assertEquals(1234, message
				.getHeaders().get(IpHeaders.REMOTE_PORT));
	}

	/**
	 * Test method for {@link org.springframework.integration.ip.tcp.SocketMessageMapper#fromMessage(org.springframework.integration.Message)}.
	 * @throws Exception 
	 */
	@Test
	public void testFromMessageBytes() throws Exception {
		String s = "test";
		Message<String> message = MessageBuilder.withPayload(s).build();
		TcpMessageMapper mapper = new TcpMessageMapper();
		mapper.setStringToBytes(true);
		byte[] bArray = (byte[]) mapper.fromMessage(message);
		assertEquals(s, new String(bArray));
		
	}

	@Test
	public void testFromMessage() throws Exception {
		String s = "test";
		Message<String> message = MessageBuilder.withPayload(s).build();
		TcpMessageMapper mapper = new TcpMessageMapper();
		mapper.setStringToBytes(false);
		String out = (String) mapper.fromMessage(message);
		assertEquals(s, out);
		
	}
	
	
}