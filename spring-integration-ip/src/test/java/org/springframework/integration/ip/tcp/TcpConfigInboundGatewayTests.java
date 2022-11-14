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

package org.springframework.integration.ip.tcp;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import javax.net.SocketFactory;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.serializer.ByteArrayStxEtxSerializer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class TcpConfigInboundGatewayTests {

	@Autowired
	@Qualifier("crLfServer")
	AbstractServerConnectionFactory crLfServer;

	@Autowired
	@Qualifier("stxEtxServer")
	AbstractServerConnectionFactory stxEtxServer;

	@Autowired
	@Qualifier("lengthHeaderServer")
	AbstractServerConnectionFactory lengthHeaderServer;

	@Autowired
	@Qualifier("javaSerialServer")
	AbstractServerConnectionFactory javaSerialServer;

	@Autowired
	@Qualifier("crLfClient")
	AbstractClientConnectionFactory crLfClient;

	@Autowired
	@Qualifier("stxEtxClient")
	AbstractClientConnectionFactory stxEtxClient;

	@Autowired
	@Qualifier("lengthHeaderClient")
	AbstractClientConnectionFactory lengthHeaderClient;

	@Autowired
	@Qualifier("javaSerialClient")
	AbstractClientConnectionFactory javaSerialClient;

	@Autowired
	@Qualifier("crLfServerNio")
	AbstractServerConnectionFactory crLfServerNio;

	@Autowired
	@Qualifier("stxEtxServerNio")
	AbstractServerConnectionFactory stxEtxServerNio;

	@Autowired
	@Qualifier("lengthHeaderServerNio")
	AbstractServerConnectionFactory lengthHeaderServerNio;

	@Autowired
	@Qualifier("javaSerialServerNio")
	AbstractServerConnectionFactory javaSerialServerNio;

	@Autowired
	@Qualifier("crLfClientNio")
	AbstractClientConnectionFactory crLfClientNio;

	@Autowired
	@Qualifier("stxEtxClientNio")
	AbstractClientConnectionFactory stxEtxClientNio;

	@Autowired
	@Qualifier("lengthHeaderClientNio")
	AbstractClientConnectionFactory lengthHeaderClientNio;

	@Autowired
	@Qualifier("javaSerialClientNio")
	AbstractClientConnectionFactory javaSerialClientNio;

	@Autowired
	@Qualifier("gatewayCrLf")
	TcpInboundGateway gatewayCrLf;

	@Autowired
	@Qualifier("gatewayStxEtx")
	TcpInboundGateway gatewayStxEtx;

	@Autowired
	@Qualifier("gatewayLength")
	TcpInboundGateway gatewayLength;

	@Autowired
	@Qualifier("gatewaySerialized")
	TcpInboundGateway gatewaySerialized;

	@Autowired
	@Qualifier("gatewayCrLfNio")
	TcpInboundGateway gatewayCrLfNio;

	@Autowired
	@Qualifier("gatewayStxEtxNio")
	TcpInboundGateway gatewayStxEtxNio;

	@Autowired
	@Qualifier("gatewayLengthNio")
	TcpInboundGateway gatewayLengthNio;

	@Autowired
	@Qualifier("gatewaySerializedNio")
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

	private void crLfGuts(Socket socket) throws IOException {
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
		assertThat(sb.toString()).isEqualTo("echo:" + greetings + "\r\n");
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

	private void stxEtxGuts(Socket socket) throws IOException {
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
		assertThat(sb.toString()).isEqualTo("echo:" + greetings);
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

	private void serializedGuts(Socket socket) throws IOException, ClassNotFoundException {
		socket.setSoTimeout(5000);
		String greetings = "Hello World!";
		new ObjectOutputStream(socket.getOutputStream()).writeObject(greetings);
		String echo = (String) new ObjectInputStream(socket.getInputStream()).readObject();
		assertThat(echo).isEqualTo("echo:" + greetings);
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

	private void lengthGuts(Socket socket) throws IOException {
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
		assertThat(sb.toString()).isEqualTo("echo:" + greetings);
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

}
