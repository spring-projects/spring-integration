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

import java.net.Socket;

import javax.net.SocketFactory;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class SimpleTcpNetInboundGatewayTests {
	
	private static int startup = 2000;
	
	@Autowired
	@Qualifier(value="gatewayCrLf")
	SimpleTcpNetInboundGateway gatewayCrLf;
	
	@Autowired
	@Qualifier(value="gatewayStxEtx")
	SimpleTcpNetInboundGateway gatewayStxEtx;
	
	@Autowired
	@Qualifier(value="gatewayLength")
	SimpleTcpNetInboundGateway gatewayLength;

	@Autowired
	@Qualifier(value="gatewayCustom")
	SimpleTcpNetInboundGateway gatewayCustom;
	
	@Test
	public void testCrLf() throws Exception {
		Thread.sleep(startup);
		startup = 0;
		Socket socket = SocketFactory.getDefault().createSocket("localhost", gatewayCrLf.getPort());
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
		Thread.sleep(startup);
		startup = 0;
		Socket socket = SocketFactory.getDefault().createSocket("localhost", gatewayStxEtx.getPort());
		String greetings = "Hello World!";
		socket.getOutputStream().write(MessageFormats.STX);
		socket.getOutputStream().write((greetings).getBytes());
		socket.getOutputStream().write(MessageFormats.ETX);
		StringBuilder sb = new StringBuilder();
		int c;
		while (true) {
			c = socket.getInputStream().read();
			if (c == MessageFormats.STX) {
				continue;
			}
			if (c == MessageFormats.ETX) {
				break;
			}
			sb.append((char) c);
		}
		assertEquals("echo:" + greetings, sb.toString());
	}

	@Test
	public void testLength() throws Exception {
		Thread.sleep(startup);
		startup = 0;
		Socket socket = SocketFactory.getDefault().createSocket("localhost", gatewayLength.getPort());
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

	@Test
	public void testCustom() throws Exception {
		Thread.sleep(startup);
		startup = 0;
		Socket socket = SocketFactory.getDefault().createSocket("localhost", gatewayCustom.getPort());
		String greetings = "Hello World!";
		String pad = "                        ";
		socket.getOutputStream().write((greetings).getBytes());
		socket.getOutputStream().write(pad.getBytes()); // will be truncated
		StringBuilder sb = new StringBuilder();
		int c;
		int n = 0;
		int size = 24; // custom format is fixed 24 bytes
		while (true) {
			c = socket.getInputStream().read();
			sb.append((char) c);
			if (++n  >= size) {
				break;
			}
		}
		assertEquals("echo:" + greetings, sb.toString().trim());
	}
}
