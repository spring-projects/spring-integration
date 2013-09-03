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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;

import javax.net.SocketFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.ip.util.TestingUtilities;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 2.0.2
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class SOLingerTests {

	@Autowired
	private AbstractServerConnectionFactory inCFNet;

	@Autowired
	private AbstractServerConnectionFactory inCFNio;

	@Autowired
	private AbstractServerConnectionFactory inCFNetRst;

	@Autowired
	private AbstractServerConnectionFactory inCFNioRst;

	@Autowired
	private AbstractServerConnectionFactory inCFNetLinger;

	@Autowired
	private AbstractServerConnectionFactory inCFNioLinger;

	@Test
	public void configOk() {}

	@Test
	public void finReceivedNet() {
		finReceived(inCFNet);
	}

	@Test
	public void finReceivedNio() {
		finReceived(inCFNio);
	}

	@Test
	public void rstReceivedNet() {
		rstReceived(inCFNetRst);
	}

	@Test
	public void rstReceivedNio() {
		rstReceived(inCFNioRst);
	}

	@Test
	public void finReceivedNetLinger() {
		finReceived(inCFNetLinger);
	}

	@Test
	public void finReceivedNioLinger() {
		finReceived(inCFNioLinger);
	}

	private void finReceived(AbstractServerConnectionFactory inCF) {
		int port = inCF.getPort();
		TestingUtilities.waitListening(inCF, null);
		try {
			Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
			socket.setSoTimeout(10000);
			String test = "Test\r\n";
			socket.getOutputStream().write(test.getBytes());
			byte[] buff = new byte[test.length() + 5];
			readFully(socket.getInputStream(), buff);
			assertEquals("echo:" + test, new String(buff));
			int n = socket.getInputStream().read();
			// we expect an orderly close
			assertEquals(-1, n);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected Exception  " + e.getMessage());
		}

	}

	private void rstReceived(AbstractServerConnectionFactory inCF) {
		int port = inCF.getPort();
		TestingUtilities.waitListening(inCF, null);
		try {
			Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
			socket.setSoTimeout(10000);
			String test = "Test\r\n";
			socket.getOutputStream().write(test.getBytes());
			byte[] buff = new byte[test.length() + 5];
			readFully(socket.getInputStream(), buff);
			assertEquals("echo:" + test, new String(buff));
			try {
				socket.getInputStream().read();
				fail("Expected IOException");
			} catch (IOException ioe) {
				assertTrue(ioe instanceof SocketException);
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected Exception  " + e.getMessage());
		}

	}
	private void readFully(InputStream is, byte[] buff) throws IOException {
		for (int i = 0; i < buff.length; i++) {
			buff[i] = (byte) is.read();
		}
	}

}
