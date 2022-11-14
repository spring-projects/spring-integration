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

package org.springframework.integration.ip.tcp.connection;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;

import javax.net.SocketFactory;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.ip.util.TestingUtilities;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Gary Russell
 * @since 2.0.2
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@DirtiesContext
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
	public void configOk() {
	}

	@Test
	public void finReceivedNet() {
		finReceived(inCFNet, false);
	}

	@Test
	public void finReceivedNio() {
		finReceived(inCFNio, false);
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
		finReceived(inCFNetLinger, true);
	}

	@Test
	public void finReceivedNioLinger() {
		finReceived(inCFNioLinger, true);
	}

	private void finReceived(AbstractServerConnectionFactory inCF, boolean hasLinger) {
		/*
		 * Default (no linger) means the OS may still deliver everything before the
		 * FIN, but it's not guaranteed.
		 */
		TestingUtilities.waitListening(inCF, null);
		int port = inCF.getPort();
		try {
			Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
			socket.setSoTimeout(10000);
			String test = "Test\r\n";
			socket.getOutputStream().write(test.getBytes());
			byte[] buff = new byte[test.length() + 5];
			try {
				readFully(socket.getInputStream(), buff);
				assertThat(new String(buff)).isEqualTo("echo:" + test);
			}
			catch (SocketException se) {
				if (hasLinger) {
					fail("SocketException not expected with SO_LINGER");
				}
				else {
					return;
				}
			}
			int n = socket.getInputStream().read();
			// we expect an orderly close
			assertThat(n).isEqualTo(-1);
		}
		catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected Exception  " + e.getMessage());
		}

	}

	private void rstReceived(AbstractServerConnectionFactory inCF) {
		TestingUtilities.waitListening(inCF, null);
		int port = inCF.getPort();
		try {
			Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
			socket.setSoTimeout(10000);
			String test = "Test\r\n";
			socket.getOutputStream().write(test.getBytes());
			byte[] buff = new byte[test.length() + 5];
			try {
				// with SO_LINGER=0 we may, or may not, get the data
				// if we do, verify it is as expected, if not, the RST
				// arrived before the final data.
				readFully(socket.getInputStream(), buff);
				assertThat(new String(buff)).isEqualTo("echo:" + test);
				socket.getInputStream().read();
				fail("Expected SocketException");
			}
			catch (SocketException se) {
			}
		}
		catch (Exception e) {
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
