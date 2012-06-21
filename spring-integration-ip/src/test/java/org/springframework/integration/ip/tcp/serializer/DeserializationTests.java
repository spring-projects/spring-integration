/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.integration.ip.tcp.serializer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import javax.net.ServerSocketFactory;

import org.junit.Test;
import org.springframework.core.serializer.DefaultDeserializer;
import org.springframework.integration.ip.util.SocketTestUtils;
import org.springframework.integration.test.util.SocketUtils;

/**
 * @author Gary Russell
 * @since 2.0
 */
public class DeserializationTests {

	@Test
	public void testReadLength() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		server.setSoTimeout(10000);
		SocketTestUtils.testSendLength(port, null);
		Socket socket = server.accept();
		socket.setSoTimeout(5000);
		ByteArrayLengthHeaderSerializer serializer = new ByteArrayLengthHeaderSerializer();
		byte[] out = serializer.deserialize(socket.getInputStream());
		assertEquals("Data", SocketTestUtils.TEST_STRING + SocketTestUtils.TEST_STRING,
								 new String(out));
		out = serializer.deserialize(socket.getInputStream());
		assertEquals("Data", SocketTestUtils.TEST_STRING + SocketTestUtils.TEST_STRING,
				 new String(out));
		server.close();
	}

	@Test
	public void testReadStxEtx() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		server.setSoTimeout(10000);
		SocketTestUtils.testSendStxEtx(port, null);
		Socket socket = server.accept();
		socket.setSoTimeout(5000);
		ByteArrayStxEtxSerializer serializer = new ByteArrayStxEtxSerializer();
		byte[] out = serializer.deserialize(socket.getInputStream());
		assertEquals("Data", SocketTestUtils.TEST_STRING + SocketTestUtils.TEST_STRING,
								 new String(out));
		out = serializer.deserialize(socket.getInputStream());
		assertEquals("Data", SocketTestUtils.TEST_STRING + SocketTestUtils.TEST_STRING,
				 new String(out));
		server.close();
	}

	@Test
	public void testReadCrLf() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		server.setSoTimeout(10000);
		SocketTestUtils.testSendCrLf(port, null);
		Socket socket = server.accept();
		socket.setSoTimeout(5000);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		byte[] out = serializer.deserialize(socket.getInputStream());
		assertEquals("Data", SocketTestUtils.TEST_STRING + SocketTestUtils.TEST_STRING,
								 new String(out));
		out = serializer.deserialize(socket.getInputStream());
		assertEquals("Data", SocketTestUtils.TEST_STRING + SocketTestUtils.TEST_STRING,
				 new String(out));
		server.close();
	}

	@Test
	public void testReadRaw() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		server.setSoTimeout(10000);
		SocketTestUtils.testSendRaw(port);
		Socket socket = server.accept();
		socket.setSoTimeout(5000);
		ByteArrayRawSerializer serializer = new ByteArrayRawSerializer();
		byte[] out = serializer.deserialize(socket.getInputStream());
		assertEquals("Data", SocketTestUtils.TEST_STRING + SocketTestUtils.TEST_STRING,
								 new String(out));
		server.close();
	}

	@Test
	public void testReadSerialized() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		server.setSoTimeout(10000);
		SocketTestUtils.testSendSerialized(port);
		Socket socket = server.accept();
		socket.setSoTimeout(5000);
		DefaultDeserializer deserializer = new DefaultDeserializer();
		Object out = deserializer.deserialize(socket.getInputStream());
		assertEquals("Data", SocketTestUtils.TEST_STRING, out);
		out = deserializer.deserialize(socket.getInputStream());
		assertEquals("Data", SocketTestUtils.TEST_STRING, out);
		server.close();
	}

	@Test
	public void testReadLengthOverflow() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		server.setSoTimeout(10000);
		SocketTestUtils.testSendLengthOverflow(port);
		Socket socket = server.accept();
		socket.setSoTimeout(5000);
		ByteArrayLengthHeaderSerializer serializer = new ByteArrayLengthHeaderSerializer();
		try {
			serializer.deserialize(socket.getInputStream());
	    	fail("Expected message length exceeded exception");
		} catch (IOException e) {
			if (!e.getMessage().startsWith("Message length")) {
				e.printStackTrace();
				fail("Unexpected IO Error:" + e.getMessage());
			}
		}
		server.close();
	}

	@Test
	public void testReadStxEtxTimeout() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		server.setSoTimeout(10000);
		SocketTestUtils.testSendStxEtxOverflow(port);
		Socket socket = server.accept();
		socket.setSoTimeout(500);
		ByteArrayStxEtxSerializer serializer = new ByteArrayStxEtxSerializer();
		try {
			serializer.deserialize(socket.getInputStream());
	    	fail("Expected timeout exception");
		} catch (IOException e) {
			if (!e.getMessage().startsWith("Read timed out")) {
				e.printStackTrace();
				fail("Unexpected IO Error:" + e.getMessage());
			}
		}
		server.close();
	}

	@Test
	public void testReadStxEtxOverflow() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		server.setSoTimeout(10000);
		SocketTestUtils.testSendStxEtxOverflow(port);
		Socket socket = server.accept();
		socket.setSoTimeout(5000);
		ByteArrayStxEtxSerializer serializer = new ByteArrayStxEtxSerializer();
		serializer.setMaxMessageSize(1024);
		try {
			serializer.deserialize(socket.getInputStream());
	    	fail("Expected message length exceeded exception");
		} catch (IOException e) {
			if (!e.getMessage().startsWith("ETX not found")) {
				e.printStackTrace();
				fail("Unexpected IO Error:" + e.getMessage());
			}
		}
		server.close();
	}

	@Test
	public void testReadCrLfTimeout() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		server.setSoTimeout(10000);
		SocketTestUtils.testSendCrLfOverflow(port);
		Socket socket = server.accept();
		socket.setSoTimeout(500);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		try {
			serializer.deserialize(socket.getInputStream());
	    	fail("Expected timout exception");
		} catch (IOException e) {
			if (!e.getMessage().startsWith("Read timed out")) {
				e.printStackTrace();
				fail("Unexpected IO Error:" + e.getMessage());
			}
		}
		server.close();
	}

	@Test
	public void testReadCrLfOverflow() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		server.setSoTimeout(10000);
		SocketTestUtils.testSendCrLfOverflow(port);
		Socket socket = server.accept();
		socket.setSoTimeout(5000);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		serializer.setMaxMessageSize(1024);
		try {
			serializer.deserialize(socket.getInputStream());
	    	fail("Expected message length exceeded exception");
		} catch (IOException e) {
			if (!e.getMessage().startsWith("CRLF not found")) {
				e.printStackTrace();
				fail("Unexpected IO Error:" + e.getMessage());
			}
		}
		server.close();
	}

}
