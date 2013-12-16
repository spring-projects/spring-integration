/*
 * Copyright 2002-2013 the original author or authors.
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

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;

import org.junit.Test;

import org.springframework.integration.ip.tcp.serializer.AbstractByteArraySerializer;
import org.springframework.integration.ip.tcp.serializer.ByteArrayCrLfSerializer;
import org.springframework.integration.ip.tcp.serializer.ByteArrayLengthHeaderSerializer;
import org.springframework.integration.ip.tcp.serializer.ByteArrayStxEtxSerializer;
import org.springframework.integration.ip.util.SocketTestUtils;
import org.springframework.integration.ip.util.TestingUtilities;
import org.springframework.integration.test.util.SocketUtils;
import org.springframework.messaging.Message;

/**
 * @author Gary Russell
 * @since 2.0
 */
public class TcpNioConnectionReadTests {

	private final CountDownLatch latch = new CountDownLatch(1);

	private AbstractServerConnectionFactory getConnectionFactory(int port,
			AbstractByteArraySerializer serializer, TcpListener listener) throws Exception {
		return getConnectionFactory(port, serializer, listener, null);
	}

	private AbstractServerConnectionFactory getConnectionFactory(int port,
			AbstractByteArraySerializer serializer, TcpListener listener, TcpSender sender) throws Exception {
		AbstractServerConnectionFactory scf = new TcpNioServerConnectionFactory(port);
		scf.setSerializer(serializer);
		scf.setDeserializer(serializer);
		scf.registerListener(listener);
		if (sender != null) {
			scf.registerSender(sender);
		}
		scf.start();
		TestingUtilities.waitListening(scf, null);
		return scf;
	}

	@Test
	public void testReadLength() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ByteArrayLengthHeaderSerializer serializer = new ByteArrayLengthHeaderSerializer();
		final List<Message<?>> responses = new ArrayList<Message<?>>();
		final Semaphore semaphore = new Semaphore(0);
		AbstractServerConnectionFactory scf = getConnectionFactory(port, serializer,new TcpListener() {
			@Override
			public boolean onMessage(Message<?> message) {
				responses.add(message);
				semaphore.release();
				return false;
			}
		});

		// Fire up the sender.

		CountDownLatch done = SocketTestUtils.testSendLength(port, latch);
		latch.countDown();
		assertTrue(semaphore.tryAcquire(1, 10000, TimeUnit.MILLISECONDS));
		assertTrue(semaphore.tryAcquire(1, 10000, TimeUnit.MILLISECONDS));
		assertEquals("Did not receive data", 2, responses.size());
		assertEquals("Data", SocketTestUtils.TEST_STRING + SocketTestUtils.TEST_STRING,
				new String((byte[]) responses.get(0).getPayload()));
		assertEquals("Data", SocketTestUtils.TEST_STRING + SocketTestUtils.TEST_STRING,
		         new String((byte[]) responses.get(1).getPayload()));
		scf.stop();
		done.countDown();
	}



	@SuppressWarnings("unchecked")
	@Test
	public void testFragmented() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ByteArrayLengthHeaderSerializer serializer = new ByteArrayLengthHeaderSerializer();
		final List<Message<?>> responses = new ArrayList<Message<?>>();
		final Semaphore semaphore = new Semaphore(0);
		AbstractServerConnectionFactory scf = getConnectionFactory(port, serializer,new TcpListener() {
			@Override
			public boolean onMessage(Message<?> message) {
				responses.add(message);
				try {
					Thread.sleep(1000);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				semaphore.release();
				return false;
			}
		});

		int howMany = 2;
		scf.setBacklog(howMany + 5);
		// Fire up the sender.
		CountDownLatch done = SocketTestUtils.testSendFragmented(port, howMany, false);
		assertTrue(semaphore.tryAcquire(howMany, 20000, TimeUnit.MILLISECONDS));
		assertEquals("Expected", howMany, responses.size());
		for (int i = 0; i < howMany; i++) {
			assertEquals("Data", "xx",
				new String(((Message<byte[]>) responses.get(0)).getPayload()));
		}
		scf.stop();
		done.countDown();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testReadStxEtx() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ByteArrayStxEtxSerializer serializer = new ByteArrayStxEtxSerializer();
		final List<Message<?>> responses = new ArrayList<Message<?>>();
		final Semaphore semaphore = new Semaphore(0);
		AbstractServerConnectionFactory scf = getConnectionFactory(port, serializer,new TcpListener() {
			@Override
			public boolean onMessage(Message<?> message) {
				responses.add(message);
				semaphore.release();
				return false;
			}
		});

		// Fire up the sender.

		CountDownLatch done = SocketTestUtils.testSendStxEtx(port, latch);
		latch.countDown();
		assertTrue(semaphore.tryAcquire(1, 10000, TimeUnit.MILLISECONDS));
		assertTrue(semaphore.tryAcquire(1, 10000, TimeUnit.MILLISECONDS));
		assertEquals("Did not receive data", 2, responses.size());
		assertEquals("Data", SocketTestUtils.TEST_STRING + SocketTestUtils.TEST_STRING,
						         new String(((Message<byte[]>) responses.get(0)).getPayload()));
		assertEquals("Data", SocketTestUtils.TEST_STRING + SocketTestUtils.TEST_STRING,
		         new String(((Message<byte[]>) responses.get(1)).getPayload()));
		scf.stop();
		done.countDown();
	}

	/**
	 * Test method for {@link org.springframework.integration.ip.tcp.NioSocketReader}.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testReadCrLf() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		final List<Message<?>> responses = new ArrayList<Message<?>>();
		final Semaphore semaphore = new Semaphore(0);
		AbstractServerConnectionFactory scf = getConnectionFactory(port, serializer,new TcpListener() {
			@Override
			public boolean onMessage(Message<?> message) {
				responses.add(message);
				semaphore.release();
				return false;
			}
		});

		// Fire up the sender.

		CountDownLatch done = SocketTestUtils.testSendCrLf(port, latch);
		latch.countDown();
		assertTrue(semaphore.tryAcquire(1, 10000, TimeUnit.MILLISECONDS));
		assertTrue(semaphore.tryAcquire(1, 10000, TimeUnit.MILLISECONDS));
		assertEquals("Did not receive data", 2, responses.size());
		assertEquals("Data", SocketTestUtils.TEST_STRING + SocketTestUtils.TEST_STRING,
						         new String(((Message<byte[]>) responses.get(0)).getPayload()));
		assertEquals("Data", SocketTestUtils.TEST_STRING + SocketTestUtils.TEST_STRING,
		         new String(((Message<byte[]>) responses.get(1)).getPayload()));
		scf.stop();
		done.countDown();
	}

	@Test
	public void testReadLengthOverflow() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ByteArrayLengthHeaderSerializer serializer = new ByteArrayLengthHeaderSerializer();
		final Semaphore semaphore = new Semaphore(0);
		final List<TcpConnection> added = new ArrayList<TcpConnection>();
		final List<TcpConnection> removed = new ArrayList<TcpConnection>();
		AbstractServerConnectionFactory scf = getConnectionFactory(port, serializer,new TcpListener() {
			@Override
			public boolean onMessage(Message<?> message) {
				semaphore.release();
				return false;
			}
		}, new TcpSender() {
			@Override
			public void addNewConnection(TcpConnection connection) {
				added.add(connection);
				semaphore.release();
			}
			@Override
			public void removeDeadConnection(TcpConnection connection) {
				removed.add(connection);
				semaphore.release();
			}
		});

		// Fire up the sender.

		CountDownLatch done = SocketTestUtils.testSendLengthOverflow(port);
		whileOpen(semaphore, added);
		assertEquals(1, added.size());
		assertTrue(semaphore.tryAcquire(10000, TimeUnit.MILLISECONDS));
		assertTrue(removed.size() > 0);
		scf.stop();
		done.countDown();
	}

	@Test
	public void testReadStxEtxOverflow() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ByteArrayStxEtxSerializer serializer = new ByteArrayStxEtxSerializer();
		serializer.setMaxMessageSize(1024);
		final Semaphore semaphore = new Semaphore(0);
		final List<TcpConnection> added = new ArrayList<TcpConnection>();
		final List<TcpConnection> removed = new ArrayList<TcpConnection>();
		AbstractServerConnectionFactory scf = getConnectionFactory(port, serializer,new TcpListener() {
			@Override
			public boolean onMessage(Message<?> message) {
				semaphore.release();
				return false;
			}
		}, new TcpSender() {
			@Override
			public void addNewConnection(TcpConnection connection) {
				added.add(connection);
				semaphore.release();
			}
			@Override
			public void removeDeadConnection(TcpConnection connection) {
				removed.add(connection);
				semaphore.release();
			}
		});

		// Fire up the sender.

		CountDownLatch done = SocketTestUtils.testSendStxEtxOverflow(port);
		whileOpen(semaphore, added);
		assertEquals(1, added.size());
		assertTrue(semaphore.tryAcquire(10000, TimeUnit.MILLISECONDS));
		assertTrue(removed.size() > 0);
		scf.stop();
		done.countDown();
	}

	@Test
	public void testReadCrLfOverflow() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		serializer.setMaxMessageSize(1024);
		final Semaphore semaphore = new Semaphore(0);
		final List<TcpConnection> added = new ArrayList<TcpConnection>();
		final List<TcpConnection> removed = new ArrayList<TcpConnection>();
		AbstractServerConnectionFactory scf = getConnectionFactory(port, serializer,new TcpListener() {
			@Override
			public boolean onMessage(Message<?> message) {
				semaphore.release();
				return false;
			}
		}, new TcpSender() {
			@Override
			public void addNewConnection(TcpConnection connection) {
				added.add(connection);
				semaphore.release();
			}
			@Override
			public void removeDeadConnection(TcpConnection connection) {
				removed.add(connection);
				semaphore.release();
			}
		});

		// Fire up the sender.

		CountDownLatch done = SocketTestUtils.testSendCrLfOverflow(port);
		whileOpen(semaphore, added);
		assertEquals(1, added.size());
		assertTrue(semaphore.tryAcquire(10000, TimeUnit.MILLISECONDS));
		assertTrue(removed.size() > 0);
		scf.stop();
		done.countDown();
	}

	/**
	 * Tests socket closure when no data received.
	 *
	 * @throws Exception
	 */
	@Test
	public void testCloseCleanupNoData() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		serializer.setMaxMessageSize(1024);
		final Semaphore semaphore = new Semaphore(0);
		final List<TcpConnection> added = new ArrayList<TcpConnection>();
		final List<TcpConnection> removed = new ArrayList<TcpConnection>();
		AbstractServerConnectionFactory scf = getConnectionFactory(port, serializer,new TcpListener() {
			@Override
			public boolean onMessage(Message<?> message) {
				semaphore.release();
				return false;
			}
		}, new TcpSender() {
			@Override
			public void addNewConnection(TcpConnection connection) {
				added.add(connection);
				semaphore.release();
			}
			@Override
			public void removeDeadConnection(TcpConnection connection) {
				removed.add(connection);
				semaphore.release();
			}
		});
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.close();
		whileOpen(semaphore, added);
		assertEquals(1, added.size());
		assertTrue(semaphore.tryAcquire(10000, TimeUnit.MILLISECONDS));
		assertTrue(removed.size() > 0);
		scf.stop();
	}

	/**
	 * Tests socket closure when no data received.
	 *
	 * @throws Exception
	 */
	@Test
	public void testCloseCleanupPartialData() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		serializer.setMaxMessageSize(1024);
		final Semaphore semaphore = new Semaphore(0);
		final List<TcpConnection> added = new ArrayList<TcpConnection>();
		final List<TcpConnection> removed = new ArrayList<TcpConnection>();
		AbstractServerConnectionFactory scf = getConnectionFactory(port, serializer,new TcpListener() {
			@Override
			public boolean onMessage(Message<?> message) {
				semaphore.release();
				return false;
			}
		}, new TcpSender() {
			@Override
			public void addNewConnection(TcpConnection connection) {
				added.add(connection);
				semaphore.release();
			}
			@Override
			public void removeDeadConnection(TcpConnection connection) {
				removed.add(connection);
				semaphore.release();
			}
		});
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.getOutputStream().write("partial".getBytes());
		socket.close();
		whileOpen(semaphore, added);
		assertEquals(1, added.size());
		assertTrue(semaphore.tryAcquire(10000, TimeUnit.MILLISECONDS));
		assertTrue(removed.size() > 0);
		scf.stop();
	}

	/**
	 * Tests socket closure when mid-message
	 *
	 * @throws Exception
	 */
	@Test
	public void testCloseCleanupCrLf() throws Exception {
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		testClosureMidMessageGuts(serializer, "xx");
	}

	/**
	 * Tests socket closure when mid-message
	 *
	 * @throws Exception
	 */

	@Test
	public void testCloseCleanupStxEtx() throws Exception {
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		testClosureMidMessageGuts(serializer, ByteArrayStxEtxSerializer.STX + "xx");
	}

	/**
	 * Tests socket closure when mid-message
	 *
	 * @throws Exception
	 */

	@Test
	public void testCloseCleanupLengthHeader() throws Exception {
		ByteArrayLengthHeaderSerializer serializer = new ByteArrayLengthHeaderSerializer();
		testClosureMidMessageGuts(serializer, "\u0000\u0000\u0000\u0003xx");
	}

	private void testClosureMidMessageGuts(AbstractByteArraySerializer serializer, String shortMessage)
			throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		final List<Message<?>> responses = new ArrayList<Message<?>>();
		final Semaphore semaphore = new Semaphore(0);
		final List<TcpConnection> added = new ArrayList<TcpConnection>();
		final List<TcpConnection> removed = new ArrayList<TcpConnection>();
		AbstractServerConnectionFactory scf = getConnectionFactory(port, serializer,new TcpListener() {
			@Override
			public boolean onMessage(Message<?> message) {
				responses.add(message);
				return false;
			}
		}, new TcpSender() {
			@Override
			public void addNewConnection(TcpConnection connection) {
				added.add(connection);
				semaphore.release();
			}
			@Override
			public void removeDeadConnection(TcpConnection connection) {
				removed.add(connection);
				semaphore.release();
			}
		});
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.getOutputStream().write(shortMessage.getBytes());
		socket.close();
		whileOpen(semaphore, added);
		assertEquals(1, added.size());
		assertTrue(semaphore.tryAcquire(10000, TimeUnit.MILLISECONDS));
		assertTrue(removed.size() > 0);
		scf.stop();
	}

	private void whileOpen(Semaphore semaphore, final List<TcpConnection> added)
			throws InterruptedException {
		int n = 0;
		assertTrue(semaphore.tryAcquire(10000, TimeUnit.MILLISECONDS));
		while (added.get(0).isOpen()) {
			Thread.sleep(50);
			if (n++ > 200) {
				fail("Failed to close socket");
			}
		}
	}

}
