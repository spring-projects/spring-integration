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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.net.ServerSocketFactory;

import org.junit.Test;
import org.springframework.integration.ip.util.SocketTestUtils;
import org.springframework.integration.support.MessageBuilder;


/**
 * @author Gary Russell
 * @since 2.0
 *
 */
public class TcpNioConnectionTests {

	@Test
	public void testWriteTimeout() throws Exception {
		final int port = SocketTestUtils.findAvailableServerSocket();
		TcpNioClientConnectionFactory factory = new TcpNioClientConnectionFactory("localhost", port);
		factory.setSoTimeout(1000);
		factory.start();
		final CountDownLatch latch = new CountDownLatch(1);
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			@SuppressWarnings("unused")
			public void run() {
				try {
					ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
					latch.countDown();
					Socket s = server.accept();
					// block so we fill the buffer
					server.accept();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		assertTrue(latch.await(10000, TimeUnit.MILLISECONDS));
		try {
			TcpConnection connection = factory.getConnection();
			connection.send(MessageBuilder.withPayload(new byte[1000000]).build());
		} catch (Exception e) {
			assertTrue("Expected SocketTimeoutException, got " + e.getClass().getSimpleName() + 
					   ":" + e.getMessage(), e instanceof SocketTimeoutException);
		}
	}

	@Test
	public void testReadTimeout() throws Exception {
		final int port = SocketTestUtils.findAvailableServerSocket();
		TcpNioClientConnectionFactory factory = new TcpNioClientConnectionFactory("localhost", port);
		factory.setSoTimeout(1000);
		factory.start();
		final CountDownLatch latch = new CountDownLatch(1);
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			public void run() {
				try {
					ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
					latch.countDown();
					Socket socket = server.accept();
					byte[] b = new byte[6]; 
					readFully(socket.getInputStream(), b);
					// block to cause timeout on read.
					server.accept();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		assertTrue(latch.await(10000, TimeUnit.MILLISECONDS));
		try {
			TcpConnection connection = factory.getConnection();
			connection.send(MessageBuilder.withPayload("Test").build());
			int n = 0;
			while (connection.isOpen()) {
				Thread.sleep(100);
				if (n++ > 200) {
					break;
				}
			}
			assertTrue(!connection.isOpen());
		} catch (Exception e) {
			fail("Unexpected exception " + e);
		}

	}
	
	private void readFully(InputStream is, byte[] buff) throws IOException {
		for (int i = 0; i < buff.length; i++) {
			buff[i] = (byte) is.read();
		}
	}
	
}
