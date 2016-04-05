/*
 * Copyright 2002-2016 the original author or authors.
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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

import javax.net.SocketFactory;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.serializer.ByteArrayStxEtxSerializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class TcpConfigInboundGatewayTests {

	static AbstractApplicationContext staticContext;

	@Autowired
	AbstractApplicationContext ctx;

	@Autowired
	@Qualifier(value = "crLfServer")
	AbstractServerConnectionFactory crLfServer;

	@Autowired
	@Qualifier(value = "stxEtxServer")
	AbstractServerConnectionFactory stxEtxServer;

	@Autowired
	@Qualifier(value = "lengthHeaderServer")
	AbstractServerConnectionFactory lengthHeaderServer;

	@Autowired
	@Qualifier(value = "javaSerialServer")
	AbstractServerConnectionFactory javaSerialServer;

	@Autowired
	@Qualifier(value = "crLfClient")
	AbstractClientConnectionFactory crLfClient;

	@Autowired
	@Qualifier(value = "stxEtxClient")
	AbstractClientConnectionFactory stxEtxClient;

	@Autowired
	@Qualifier(value = "lengthHeaderClient")
	AbstractClientConnectionFactory lengthHeaderClient;

	@Autowired
	@Qualifier(value = "javaSerialClient")
	AbstractClientConnectionFactory javaSerialClient;

	@Autowired
	@Qualifier(value = "crLfServerNio")
	AbstractServerConnectionFactory crLfServerNio;

	@Autowired
	@Qualifier(value = "stxEtxServerNio")
	AbstractServerConnectionFactory stxEtxServerNio;

	@Autowired
	@Qualifier(value = "lengthHeaderServerNio")
	AbstractServerConnectionFactory lengthHeaderServerNio;

	@Autowired
	@Qualifier(value = "javaSerialServerNio")
	AbstractServerConnectionFactory javaSerialServerNio;

	@Autowired
	@Qualifier(value = "crLfClientNio")
	AbstractClientConnectionFactory crLfClientNio;

	@Autowired
	@Qualifier(value = "stxEtxClientNio")
	AbstractClientConnectionFactory stxEtxClientNio;

	@Autowired
	@Qualifier(value = "lengthHeaderClientNio")
	AbstractClientConnectionFactory lengthHeaderClientNio;

	@Autowired
	@Qualifier(value = "javaSerialClientNio")
	AbstractClientConnectionFactory javaSerialClientNio;

	@Autowired
	@Qualifier(value = "gatewayCrLf")
	TcpInboundGateway gatewayCrLf;

	@Autowired
	@Qualifier(value = "gatewayStxEtx")
	TcpInboundGateway gatewayStxEtx;

	@Autowired
	@Qualifier(value = "gatewayLength")
	TcpInboundGateway gatewayLength;

	@Autowired
	@Qualifier(value = "gatewaySerialized")
	TcpInboundGateway gatewaySerialized;

	@Autowired
	@Qualifier(value = "gatewayCrLfNio")
	TcpInboundGateway gatewayCrLfNio;

	@Autowired
	@Qualifier(value = "gatewayStxEtxNio")
	TcpInboundGateway gatewayStxEtxNio;

	@Autowired
	@Qualifier(value = "gatewayLengthNio")
	TcpInboundGateway gatewayLengthNio;

	@Autowired
	@Qualifier(value = "gatewaySerializedNio")
	TcpInboundGateway gatewaySerializedNio;

	@Test
	public void testCrLf() throws Exception {
		waitListening(gatewayCrLf);
		Socket socket = SocketFactory.getDefault().createSocket("localhost", crLfServer.getPort());
		crLfGuts(socket);
	}

	@Test
	public void testCrLfNio() throws Exception {
		waitListening(gatewayCrLfNio);
		Socket socket = SocketFactory.getDefault().createSocket("localhost", crLfServerNio.getPort());
		crLfGuts(socket);
	}

	private void crLfGuts(Socket socket) throws SocketException, IOException {
		socket.setSoTimeout(5000);
		String greetings = "Hello World!";
		socket.getOutputStream().write((greetings + "\r\n").getBytes());
		StringBuilder sb = new StringBuilder();
		int c;
		while (true) {
			c = socket.getInputStream().read();
			sb.append((char) c);
			if (c == '\n') {
				break;
			}
		}
		assertEquals("echo:" + greetings + "\r\n", sb.toString());
	}

	@Test
	public void testStxEtx() throws Exception {
		waitListening(gatewayStxEtx);
		Socket socket = SocketFactory.getDefault().createSocket("localhost", stxEtxServer.getPort());
		stxEtxGuts(socket);
	}

	@Test
	public void testStxEtxNio() throws Exception {
		waitListening(gatewayStxEtxNio);
		Socket socket = SocketFactory.getDefault().createSocket("localhost", stxEtxServerNio.getPort());
		stxEtxGuts(socket);
	}

	private void stxEtxGuts(Socket socket) throws SocketException, IOException {
		socket.setSoTimeout(5000);
		String greetings = "Hello World!";
		socket.getOutputStream().write(ByteArrayStxEtxSerializer.STX);
		socket.getOutputStream().write((greetings).getBytes());
		socket.getOutputStream().write(ByteArrayStxEtxSerializer.ETX);
		StringBuilder sb = new StringBuilder();
		int c;
		while (true) {
			c = socket.getInputStream().read();
			if (c == ByteArrayStxEtxSerializer.STX) {
				continue;
			}
			if (c == ByteArrayStxEtxSerializer.ETX) {
				break;
			}
			sb.append((char) c);
		}
		assertEquals("echo:" + greetings, sb.toString());
	}

	@Test
	public void testSerialized() throws Exception {
		waitListening(gatewaySerialized);
		Socket socket = SocketFactory.getDefault().createSocket("localhost", javaSerialServer.getPort());
		serializedGuts(socket);
	}

	@Test
	public void testSerializedNio() throws Exception {
		waitListening(gatewaySerializedNio);
		Socket socket = SocketFactory.getDefault().createSocket("localhost", javaSerialServerNio.getPort());
		serializedGuts(socket);
	}

	private void serializedGuts(Socket socket) throws SocketException,
			IOException, ClassNotFoundException {
		socket.setSoTimeout(5000);
		String greetings = "Hello World!";
		new ObjectOutputStream(socket.getOutputStream()).writeObject(greetings);
		String echo = (String) new ObjectInputStream(socket.getInputStream()).readObject();
		assertEquals("echo:" + greetings, echo);
	}

	@Test
	public void testLength() throws Exception {
		waitListening(gatewayLength);
		Socket socket = SocketFactory.getDefault().createSocket("localhost", lengthHeaderServer.getPort());
		lengthGuts(socket);
	}

	@Test
	public void testLengthNio() throws Exception {
		waitListening(gatewayLengthNio);
		Socket socket = SocketFactory.getDefault().createSocket("localhost", lengthHeaderServerNio.getPort());
		lengthGuts(socket);
	}

	private void lengthGuts(Socket socket) throws SocketException, IOException {
		socket.setSoTimeout(5000);
		String greetings = "Hello World!";
		byte[] header = new byte[4];
		header[3] = (byte) greetings.length();
		socket.getOutputStream().write(header);
		socket.getOutputStream().write((greetings).getBytes());
		StringBuilder sb = new StringBuilder();
		int c;
		int n = 0;
		int size = 0;
		while (true) {
			c = socket.getInputStream().read();
			if (n++ < 3) {
				continue;
			}
			if (n == 4) {
				size = c;
				continue;
			}
			sb.append((char) c);
			if (n - 4 >= size) {
				break;
			}
		}
		assertEquals("echo:" + greetings, sb.toString());
	}

	private void waitListening(TcpInboundGateway gateway) throws Exception {
		int n = 0;
		while (!gateway.isListening()) {
			Thread.sleep(100);
			if (n++ > 100) {
				throw new Exception("Gateway failed to listen");
			}
		}

	}

	@Before
	public void copyContext() {
		if (staticContext == null) {
			staticContext = ctx;
		}
	}

	@AfterClass
	public static void shutDown() {
		staticContext.close();
	}
}
