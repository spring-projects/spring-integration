/*
 * Copyright 2002-2020 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.with;

import java.net.Socket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.SocketFactory;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.integration.ip.tcp.serializer.AbstractByteArraySerializer;
import org.springframework.integration.ip.tcp.serializer.ByteArrayCrLfSerializer;
import org.springframework.integration.ip.tcp.serializer.ByteArrayLengthHeaderSerializer;
import org.springframework.integration.ip.tcp.serializer.ByteArrayStxEtxSerializer;
import org.springframework.integration.ip.util.SocketTestUtils;
import org.springframework.integration.ip.util.TestingUtilities;
import org.springframework.integration.test.support.LongRunningIntegrationTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class TcpNioConnectionReadTests {

	@Rule
	public LongRunningIntegrationTest longRunningIntegrationTest = new LongRunningIntegrationTest();

	private final CountDownLatch latch = new CountDownLatch(1);

	private AbstractServerConnectionFactory getConnectionFactory(
			AbstractByteArraySerializer serializer, TcpListener listener) throws Exception {
		return getConnectionFactory(serializer, listener, null);
	}

	private AbstractServerConnectionFactory getConnectionFactory(
			AbstractByteArraySerializer serializer, TcpListener listener, TcpSender sender) throws Exception {
		TcpNioServerConnectionFactory scf = new TcpNioServerConnectionFactory(0);
		scf.setUsingDirectBuffers(true);
		scf.setApplicationEventPublisher(e -> {
		});
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
		ByteArrayLengthHeaderSerializer serializer = new ByteArrayLengthHeaderSerializer();
		final List<Message<?>> responses = new ArrayList<Message<?>>();
		final Semaphore semaphore = new Semaphore(0);
		AbstractServerConnectionFactory scf = getConnectionFactory(serializer, message -> {
			responses.add(message);
			semaphore.release();
			return false;
		});

		// Fire up the sender.

		CountDownLatch done = SocketTestUtils.testSendLength(scf.getPort(), latch);
		latch.countDown();
		assertThat(semaphore.tryAcquire(1, 10000, TimeUnit.MILLISECONDS)).isTrue();
		assertThat(semaphore.tryAcquire(1, 10000, TimeUnit.MILLISECONDS)).isTrue();
		assertThat(responses.size()).as("Did not receive data").isEqualTo(2);
		assertThat(new String((byte[]) responses.get(0).getPayload())).as("Data")
				.isEqualTo(SocketTestUtils.TEST_STRING + SocketTestUtils.TEST_STRING);
		assertThat(new String((byte[]) responses.get(1).getPayload())).as("Data")
				.isEqualTo(SocketTestUtils.TEST_STRING + SocketTestUtils.TEST_STRING);
		scf.stop();
		done.countDown();
	}


	@SuppressWarnings("unchecked")
	@Test
	public void testFragmented() throws Exception {
		ByteArrayLengthHeaderSerializer serializer = new ByteArrayLengthHeaderSerializer();
		final List<Message<?>> responses = new ArrayList<Message<?>>();
		final Semaphore semaphore = new Semaphore(0);
		AbstractServerConnectionFactory scf = getConnectionFactory(serializer, message -> {
			responses.add(message);
			try {
				Thread.sleep(10);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			semaphore.release();
			return false;
		});

		int howMany = 2;
		scf.setBacklog(howMany + 5);
		// Fire up the sender.
		CountDownLatch done = SocketTestUtils.testSendFragmented(scf.getPort(), howMany, false);
		assertThat(semaphore.tryAcquire(howMany, 20000, TimeUnit.MILLISECONDS)).isTrue();
		assertThat(responses.size()).as("Expected").isEqualTo(howMany);
		for (int i = 0; i < howMany; i++) {
			assertThat(new String(((Message<byte[]>) responses.get(0)).getPayload())).as("Data").isEqualTo("xx");
		}
		scf.stop();
		done.countDown();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testReadStxEtx() throws Exception {
		ByteArrayStxEtxSerializer serializer = new ByteArrayStxEtxSerializer();
		final List<Message<?>> responses = new ArrayList<Message<?>>();
		final Semaphore semaphore = new Semaphore(0);
		AbstractServerConnectionFactory scf = getConnectionFactory(serializer, message -> {
			responses.add(message);
			semaphore.release();
			return false;
		});

		// Fire up the sender.

		CountDownLatch done = SocketTestUtils.testSendStxEtx(scf.getPort(), latch);
		latch.countDown();
		assertThat(semaphore.tryAcquire(1, 10000, TimeUnit.MILLISECONDS)).isTrue();
		assertThat(semaphore.tryAcquire(1, 10000, TimeUnit.MILLISECONDS)).isTrue();
		assertThat(responses.size()).as("Did not receive data").isEqualTo(2);
		assertThat(new String(((Message<byte[]>) responses.get(0)).getPayload())).as("Data")
				.isEqualTo(SocketTestUtils.TEST_STRING + SocketTestUtils.TEST_STRING);
		assertThat(new String(((Message<byte[]>) responses.get(1)).getPayload())).as("Data")
				.isEqualTo(SocketTestUtils.TEST_STRING + SocketTestUtils.TEST_STRING);
		scf.stop();
		done.countDown();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testReadCrLf() throws Exception {
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		final List<Message<?>> responses = new ArrayList<Message<?>>();
		final Semaphore semaphore = new Semaphore(0);
		AbstractServerConnectionFactory scf = getConnectionFactory(serializer, message -> {
			responses.add(message);
			semaphore.release();
			return false;
		});

		// Fire up the sender.

		CountDownLatch done = SocketTestUtils.testSendCrLf(scf.getPort(), latch);
		latch.countDown();
		assertThat(semaphore.tryAcquire(1, 10000, TimeUnit.MILLISECONDS)).isTrue();
		assertThat(semaphore.tryAcquire(1, 10000, TimeUnit.MILLISECONDS)).isTrue();
		assertThat(responses.size()).as("Did not receive data").isEqualTo(2);
		assertThat(new String(((Message<byte[]>) responses.get(0)).getPayload())).as("Data")
				.isEqualTo(SocketTestUtils.TEST_STRING + SocketTestUtils.TEST_STRING);
		assertThat(new String(((Message<byte[]>) responses.get(1)).getPayload())).as("Data")
				.isEqualTo(SocketTestUtils.TEST_STRING + SocketTestUtils.TEST_STRING);
		scf.stop();
		done.countDown();
	}

	@Test
	public void testReadLengthOverflow() throws Exception {
		ByteArrayLengthHeaderSerializer serializer = new ByteArrayLengthHeaderSerializer();
		final Semaphore semaphore = new Semaphore(0);
		final List<TcpConnection> added = new ArrayList<TcpConnection>();
		final List<TcpConnection> removed = new ArrayList<TcpConnection>();

		final CountDownLatch errorMessageLetch = new CountDownLatch(1);
		final AtomicReference<Throwable> errorMessageRef = new AtomicReference<Throwable>();

		AbstractServerConnectionFactory scf = getConnectionFactory(serializer, message -> {
			if (message instanceof ErrorMessage) {
				errorMessageRef.set(((ErrorMessage) message).getPayload());
				errorMessageLetch.countDown();
			}
			return false;
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

		CountDownLatch done = SocketTestUtils.testSendLengthOverflow(scf.getPort());
		whileOpen(semaphore, added);
		assertThat(added.size()).isEqualTo(1);

		assertThat(errorMessageLetch.await(10, TimeUnit.SECONDS)).isTrue();

		assertThat(errorMessageRef.get().getMessage())
				.satisfiesAnyOf(
						s -> assertThat(s).contains("Message length 2147483647 exceeds max message length: 2048"),
						s -> assertThat(s).contains("Connection is closed"));

		assertThat(semaphore.tryAcquire(10000, TimeUnit.MILLISECONDS)).isTrue();
		assertThat(removed).hasSizeGreaterThan(0);
		scf.stop();
		done.countDown();
	}

	@Test
	public void testReadStxEtxOverflow() throws Exception {
		ByteArrayStxEtxSerializer serializer = new ByteArrayStxEtxSerializer();
		serializer.setMaxMessageSize(1024);
		final Semaphore semaphore = new Semaphore(0);
		final List<TcpConnection> added = new ArrayList<>();
		final List<TcpConnection> removed = new ArrayList<>();

		final CountDownLatch errorMessageLetch = new CountDownLatch(1);
		final AtomicReference<Throwable> errorMessageRef = new AtomicReference<>();

		AbstractServerConnectionFactory scf = getConnectionFactory(serializer, message -> {
			if (message instanceof ErrorMessage) {
				errorMessageRef.set(((ErrorMessage) message).getPayload());
				errorMessageLetch.countDown();
			}
			return false;
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

		CountDownLatch done = SocketTestUtils.testSendStxEtxOverflow(scf.getPort());
		whileOpen(semaphore, added);
		assertThat(added.size()).isEqualTo(1);

		assertThat(errorMessageLetch.await(10, TimeUnit.SECONDS)).isTrue();

		assertThat(errorMessageRef.get().getMessage())
				.satisfiesAnyOf(
						s -> assertThat(s).contains("ETX not found before max message length: 1024"),
						s -> assertThat(s).contains("Connection is closed"));

		assertThat(semaphore.tryAcquire(10000, TimeUnit.MILLISECONDS)).isTrue();
		assertThat(removed).hasSizeGreaterThan(0);
		scf.stop();
		done.countDown();
	}

	@Test
	public void testReadCrLfOverflow() throws Exception {
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		serializer.setMaxMessageSize(1024);
		final Semaphore semaphore = new Semaphore(0);
		final List<TcpConnection> added = new ArrayList<TcpConnection>();
		final List<TcpConnection> removed = new ArrayList<TcpConnection>();

		final CountDownLatch errorMessageLetch = new CountDownLatch(1);
		final AtomicReference<Throwable> errorMessageRef = new AtomicReference<Throwable>();

		AbstractServerConnectionFactory scf = getConnectionFactory(serializer, message -> {
			if (message instanceof ErrorMessage) {
				errorMessageRef.set(((ErrorMessage) message).getPayload());
				errorMessageLetch.countDown();
			}
			return false;
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

		CountDownLatch done = SocketTestUtils.testSendCrLfOverflow(scf.getPort());
		whileOpen(semaphore, added);
		assertThat(added.size()).isEqualTo(1);

		assertThat(errorMessageLetch.await(10, TimeUnit.SECONDS)).isTrue();

		assertThat(errorMessageRef.get().getMessage())
				.satisfiesAnyOf(
						s -> assertThat(s).contains("CRLF not found before max message length: 1024"),
						s -> assertThat(s).contains("Connection is closed"));

		assertThat(semaphore.tryAcquire(10000, TimeUnit.MILLISECONDS)).isTrue();
		assertThat(removed).hasSizeGreaterThan(0);
		scf.stop();
		done.countDown();
	}

	/**
	 * Tests socket closure when no data received.
	 * @throws Exception
	 */
	@Test
	public void testCloseCleanupNoData() throws Exception {
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		serializer.setMaxMessageSize(1024);
		final Semaphore semaphore = new Semaphore(0);
		final List<TcpConnection> added = new ArrayList<TcpConnection>();
		final List<TcpConnection> removed = new ArrayList<TcpConnection>();

		final CountDownLatch errorMessageLetch = new CountDownLatch(1);
		final AtomicReference<Throwable> errorMessageRef = new AtomicReference<Throwable>();

		AbstractServerConnectionFactory scf = getConnectionFactory(serializer, message -> {
			if (message instanceof ErrorMessage) {
				errorMessageRef.set(((ErrorMessage) message).getPayload());
				errorMessageLetch.countDown();
			}
			return false;
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
		Socket socket = SocketFactory.getDefault().createSocket("localhost", scf.getPort());
		socket.close();
		whileOpen(semaphore, added);
		assertThat(added.size()).isEqualTo(1);

		assertThat(errorMessageLetch.await(10, TimeUnit.SECONDS)).isTrue();

		assertThat(errorMessageRef.get().getMessage())
				.satisfiesAnyOf(
						s -> assertThat(s).contains("Stream closed after 2 of 3"),
						s -> assertThat(s).contains("Connection is closed"));

		assertThat(semaphore.tryAcquire(10000, TimeUnit.MILLISECONDS)).isTrue();
		assertThat(removed).hasSizeGreaterThan(0);
		scf.stop();
	}

	/**
	 * Tests socket closure when no data received.
	 * @throws Exception
	 */
	@Test
	public void testCloseCleanupPartialData() throws Exception {
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		serializer.setMaxMessageSize(1024);
		final Semaphore semaphore = new Semaphore(0);
		final List<TcpConnection> added = new ArrayList<TcpConnection>();
		final List<TcpConnection> removed = new ArrayList<TcpConnection>();

		final CountDownLatch errorMessageLetch = new CountDownLatch(1);
		final AtomicReference<Throwable> errorMessageRef = new AtomicReference<Throwable>();

		AbstractServerConnectionFactory scf = getConnectionFactory(serializer, message -> {
			if (message instanceof ErrorMessage) {
				errorMessageRef.set(((ErrorMessage) message).getPayload());
				errorMessageLetch.countDown();
			}
			return false;
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
		Socket socket = SocketFactory.getDefault().createSocket("localhost", scf.getPort());
		socket.getOutputStream().write("partial".getBytes());
		socket.close();
		whileOpen(semaphore, added);
		assertThat(added).hasSize(1);

		assertThat(errorMessageLetch.await(10, TimeUnit.SECONDS)).isTrue();

		assertThat(errorMessageRef.get().getMessage())
				.satisfiesAnyOf(
						s -> assertThat(s).contains("Socket closed during message assembly"),
						s -> assertThat(s).contains("Connection is closed"));

		assertThat(semaphore.tryAcquire(10000, TimeUnit.MILLISECONDS)).isTrue();
		assertThat(removed).hasSizeGreaterThan(0);
		scf.stop();
	}

	/**
	 * Tests socket closure when mid-message
	 * @throws Exception
	 */
	@Test
	public void testCloseCleanupCrLf() throws Exception {
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		testClosureMidMessageGuts(serializer, "xx");
	}

	/**
	 * Tests socket closure when mid-message
	 * @throws Exception
	 */
	@Test
	public void testCloseCleanupStxEtx() throws Exception {
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		testClosureMidMessageGuts(serializer, ByteArrayStxEtxSerializer.STX + "xx");
	}

	/**
	 * Tests socket closure when mid-message
	 * @throws Exception
	 */
	@Test
	public void testCloseCleanupLengthHeader() throws Exception {
		ByteArrayLengthHeaderSerializer serializer = new ByteArrayLengthHeaderSerializer();
		testClosureMidMessageGuts(serializer, "\u0000\u0000\u0000\u0003xx");
	}

	private void testClosureMidMessageGuts(AbstractByteArraySerializer serializer, String shortMessage)
			throws Exception {
		final Semaphore semaphore = new Semaphore(0);
		final List<TcpConnection> added = new ArrayList<TcpConnection>();
		final List<TcpConnection> removed = new ArrayList<TcpConnection>();

		final CountDownLatch errorMessageLetch = new CountDownLatch(1);
		final AtomicReference<Throwable> errorMessageRef = new AtomicReference<Throwable>();

		AbstractServerConnectionFactory scf = getConnectionFactory(serializer, message -> {
			if (message instanceof ErrorMessage) {
				errorMessageRef.set(((ErrorMessage) message).getPayload());
				errorMessageLetch.countDown();
			}
			return false;
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
		Socket socket = SocketFactory.getDefault().createSocket("localhost", scf.getPort());
		socket.getOutputStream().write(shortMessage.getBytes());
		socket.close();
		whileOpen(semaphore, added);
		assertThat(added).hasSize(1);

		assertThat(errorMessageLetch.await(10, TimeUnit.SECONDS)).isTrue();

		assertThat(errorMessageRef.get().getMessage())
				.satisfiesAnyOf(
						s -> assertThat(s).contains("Socket closed during message assembly"),
						s -> assertThat(s).contains("Stream closed after 2 of 3"),
						s -> assertThat(s).contains("Connection is closed"));

		assertThat(semaphore.tryAcquire(10000, TimeUnit.MILLISECONDS)).isTrue();
		assertThat(removed).hasSizeGreaterThan(0);
		scf.stop();
	}

	private void whileOpen(Semaphore semaphore, final List<TcpConnection> added)
			throws InterruptedException {
		assertThat(semaphore.tryAcquire(10000, TimeUnit.MILLISECONDS)).isTrue();
		with().pollInterval(Duration.ofMillis(50)).await("Failed to close socket")
				.atMost(Duration.ofSeconds(20))
				.until(() -> !added.get(0).isOpen());
	}

}
