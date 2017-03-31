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

package org.springframework.integration.test.util;

import static org.junit.Assert.assertNotEquals;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;

import javax.net.ServerSocketFactory;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Gunnar Hillert
 * @author Gary Russell
 */
public class SocketUtilsTests {

	@Test
	public void testFindAvailableServerSocketWithNegativeSeedPort() {

		try {
			SocketUtils.findAvailableServerSocket(-500);
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("'seed' must not be negative", e.getMessage());
			return;

		}

		Assert.fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	public void testFindAvailableUdpSocketWithNegativeSeedPort() {

		try {
			SocketUtils.findAvailableUdpSocket(-500);
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("'seed' must not be negative", e.getMessage());
			return;

		}

		Assert.fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	public void testTcpLocalhost() throws Exception {
		int available = SocketUtils.findAvailableServerSocket();
		ServerSocket ss = ServerSocketFactory.getDefault().createServerSocket(available, 1,
				InetAddress.getByName("localhost"));
		assertNotEquals(available, SocketUtils.findAvailableServerSocket(available));
		ss.close();
	}

	@Test
	public void testUdpLocalhost() throws Exception {
		int available = SocketUtils.findAvailableUdpSocket(2000);
		DatagramSocket dgs = new DatagramSocket(available, InetAddress.getByName("localhost"));
		assertNotEquals(available, SocketUtils.findAvailableUdpSocket(available));
		dgs.close();
	}

}
