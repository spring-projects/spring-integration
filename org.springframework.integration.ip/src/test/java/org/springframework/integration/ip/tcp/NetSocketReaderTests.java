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

import java.net.ServerSocket;
import java.net.Socket;

import javax.net.ServerSocketFactory;

import org.junit.Test;
import org.springframework.integration.ip.util.SocketUtils;

/**
 * @author Gary Russell
 *
 */
public class NetSocketReaderTests {

	/**
	 * Test method for {@link org.springframework.integration.ip.tcp.NioSocketReader#readFully()},
	 * using &lt;length&gt;&lt;message&gt;.
	 */
	@Test
	public void testReadLength() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		SocketUtils.testSendLength(port, null);
		Socket socket = server.accept();
		socket.setSoTimeout(5000);
		NetSocketReader reader = new NetSocketReader(socket);
		if (reader.assembleData()) {
			assertEquals("Data", SocketUtils.TEST_STRING + SocketUtils.TEST_STRING, 
								 new String(reader.getAssembledData()));
		}
		else {
			fail("Failed to assemble first message");
		}
		if (reader.assembleData()) {
			assertEquals("Data", SocketUtils.TEST_STRING + SocketUtils.TEST_STRING, 
								 new String(reader.getAssembledData()));
		}
		else {
			fail("Failed to assemble second message");
		}
		server.close();
	}
	
	/**
	 * Test method for {@link org.springframework.integration.ip.tcp.NioSocketReader#readFully()},
	 * using STX&lt;message&gt;ETX
	 */
	@Test
	public void testReadStxEtx() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		SocketUtils.testSendStxEtx(port, null);
		Socket socket = server.accept();
		socket.setSoTimeout(5000);
		NetSocketReader reader = new NetSocketReader(socket);
		reader.setMessageFormat(MessageFormats.FORMAT_STX_ETX);
		if (reader.assembleData()) {
			assertEquals("Data", SocketUtils.TEST_STRING + SocketUtils.TEST_STRING, 
								 new String(reader.getAssembledData()));
		}
		else {
			fail("Failed to assemble first message");
		}
		if (reader.assembleData()) {
			assertEquals("Data", SocketUtils.TEST_STRING + SocketUtils.TEST_STRING, 
								 new String(reader.getAssembledData()));
		}
		else {
			fail("Failed to assemble second message");
		}
		server.close();
	}

	/**
	 * Test method for {@link org.springframework.integration.ip.tcp.NioSocketReader#readFully()},
	 * using STX&lt;message&gt;ETX
	 */
	@Test
	public void testReadCrLf() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		SocketUtils.testSendCrLf(port, null);
		Socket socket = server.accept();
		socket.setSoTimeout(5000);
		NetSocketReader reader = new NetSocketReader(socket);
		reader.setMessageFormat(MessageFormats.FORMAT_CRLF);
		if (reader.assembleData()) {
			assertEquals("Data", SocketUtils.TEST_STRING + SocketUtils.TEST_STRING, 
								 new String(reader.getAssembledData()));
		}
		else {
			fail("Failed to assemble first message");
		}
		if (reader.assembleData()) {
			assertEquals("Data", SocketUtils.TEST_STRING + SocketUtils.TEST_STRING, 
								 new String(reader.getAssembledData()));
		}
		else {
			fail("Failed to assemble second message");
		}
		server.close();
	}

}
