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

package org.springframework.integration.ip.util;

import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Enumeration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.ip.AbstractInternetProtocolReceivingChannelAdapter;
import org.springframework.lang.Nullable;

/**
 * TCP/IP Test utilities.
 *
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 */
public class SocketTestUtils {

	public static final String TEST_STRING = "TestMessage";

	private static final Log logger = LogFactory.getLog(SocketTestUtils.class);

	private SocketTestUtils() {
		super();
	}

	/**
	 * Sends a message in two chunks with a preceding length. Two such messages are sent.
	 * @param latch If not null, await until counted down before sending second chunk.
	 */
	public static CountDownLatch testSendLength(final int port, final CountDownLatch latch) {
		final CountDownLatch testCompleteLatch = new CountDownLatch(1);
		Thread thread = new Thread(() -> {
			try (Socket socket = new Socket(InetAddress.getByName("localhost"), port)) {
				for (int i = 0; i < 2; i++) {
					byte[] len = new byte[4];
					ByteBuffer.wrap(len).putInt(TEST_STRING.length() * 2);
					socket.getOutputStream().write(len);
					socket.getOutputStream().write(TEST_STRING.getBytes());
					logger.debug(i + " Wrote first part");
					if (latch != null) {
						latch.await();
					}
					Thread.sleep(500);
					// send the second chunk
					socket.getOutputStream().write(TEST_STRING.getBytes());
					logger.debug(i + " Wrote second part");
				}
				testCompleteLatch.await(10, TimeUnit.SECONDS);
			}
			catch (Exception e1) {
				logger.error(e1);
			}
		});
		thread.setDaemon(true);
		thread.start();
		return testCompleteLatch;
	}

	/**
	 * Sends a message with a bad length part, causing an overflow on the receiver.
	 */
	public static CountDownLatch testSendLengthOverflow(final int port) {
		final CountDownLatch testCompleteLatch = new CountDownLatch(1);
		Thread thread = new Thread(() -> {
			try (Socket socket = new Socket(InetAddress.getByName("localhost"), port)) {
				byte[] len = new byte[4];
				ByteBuffer.wrap(len).putInt(Integer.MAX_VALUE);
				socket.getOutputStream().write(len);
				socket.getOutputStream().write(TEST_STRING.getBytes());
				testCompleteLatch.await(10, TimeUnit.SECONDS);
			}
			catch (Exception e1) {
				logger.error(e1);
			}
		});
		thread.setDaemon(true);
		thread.start();
		return testCompleteLatch;
	}

	/**
	 * Test for reassembly of completely fragmented message; sends
	 * 6 bytes 500ms apart.
	 */
	public static CountDownLatch testSendFragmented(final int port, final int howMany, final boolean noDelay) {
		final CountDownLatch testCompleteLatch = new CountDownLatch(1);
		Thread thread = new Thread(() -> {
			Socket socket = null;
			try {
				logger.debug("Connecting to " + port);
				socket = new Socket(InetAddress.getByName("localhost"), port);
				OutputStream os = socket.getOutputStream();
				for (int i = 0; i < howMany; i++) {
					writeByte(os, 0, noDelay);
					writeByte(os, 0, noDelay);
					writeByte(os, 0, noDelay);
					writeByte(os, 2, noDelay);
					writeByte(os, 'x', noDelay);
					writeByte(os, 'x', noDelay);
				}
				testCompleteLatch.await(10, TimeUnit.SECONDS);
			}
			catch (Exception e1) {
				logger.error(e1);
			}
			finally {
				if (socket != null) {
					try {
						socket.close();
					}
					catch (IOException e2) {

					}
				}
			}
		});
		thread.setDaemon(true);
		thread.start();
		return testCompleteLatch;
	}

	private static void writeByte(OutputStream os, int b, boolean noDelay) throws Exception {
		os.write(b);
		logger.trace("Wrote 0x" + Integer.toHexString(b));
		if (noDelay) {
			return;
		}
		Thread.sleep(500);
	}

	/**
	 * Sends a STX/ETX message in two chunks. Two such messages are sent.
	 * @param latch If not null, await until counted down before sending second chunk.
	 */
	public static CountDownLatch testSendStxEtx(final int port, final CountDownLatch latch) {
		final CountDownLatch testCompleteLatch = new CountDownLatch(1);
		Thread thread = new Thread(() -> {
			try (Socket socket = new Socket(InetAddress.getByName("localhost"), port)) {
				OutputStream outputStream = socket.getOutputStream();
				for (int i = 0; i < 2; i++) {
					writeByte(outputStream, 0x02, true);
					outputStream.write(TEST_STRING.getBytes());
					logger.debug(i + " Wrote first part");
					if (latch != null) {
						latch.await();
					}
					Thread.sleep(500);
					// send the second chunk
					outputStream.write(TEST_STRING.getBytes());
					logger.debug(i + " Wrote second part");
					writeByte(outputStream, 0x03, true);
				}
				testCompleteLatch.await(10, TimeUnit.SECONDS);
			}
			catch (Exception e1) {
				logger.error(e1);
			}
		});
		thread.setDaemon(true);
		thread.start();
		return testCompleteLatch;
	}

	/**
	 * Sends a large STX/ETX message with no ETX
	 */
	public static CountDownLatch testSendStxEtxOverflow(final int port) {
		final CountDownLatch testCompleteLatch = new CountDownLatch(1);
		Thread thread = new Thread(() -> {
			try (Socket socket = new Socket(InetAddress.getByName("localhost"), port)) {
				OutputStream outputStream = socket.getOutputStream();
				writeByte(outputStream, 0x02, true);
				for (int i = 0; i < 1500; i++) {
					writeByte(outputStream, 'x', true);
				}
				testCompleteLatch.await(10, TimeUnit.SECONDS);
			}
			catch (Exception e1) {
				logger.debug("write failed", e1);
			}
		});
		thread.setDaemon(true);
		thread.start();
		return testCompleteLatch;
	}

	/**
	 * Sends a message +CRLF in two chunks. Two such messages are sent.
	 * @param latch If not null, await until counted down before sending second chunk.
	 */
	public static CountDownLatch testSendCrLf(final int port, final CountDownLatch latch) {
		final CountDownLatch testCompleteLatch = new CountDownLatch(1);
		Thread thread = new Thread(() -> {
			try (Socket socket = new Socket(InetAddress.getByName("localhost"), port)) {
				OutputStream outputStream = socket.getOutputStream();
				for (int i = 0; i < 2; i++) {
					outputStream.write(TEST_STRING.getBytes());
					logger.debug(i + " Wrote first part");
					if (latch != null) {
						latch.await();
					}
					Thread.sleep(500);
					// send the second chunk
					outputStream.write(TEST_STRING.getBytes());
					logger.debug(i + " Wrote second part");
					writeByte(outputStream, '\r', true);
					writeByte(outputStream, '\n', true);
				}
				testCompleteLatch.await(10, TimeUnit.SECONDS);
			}
			catch (Exception e1) {
				logger.error(e1);
			}
		});
		thread.setDaemon(true);
		thread.start();
		return testCompleteLatch;
	}

	/**
	 * Sends a single message +CRLF.
	 * @param latch Waits for latch to count down before closing the socket.
	 */
	public static void testSendCrLfSingle(final int port, final CountDownLatch latch) {
		Thread thread = new Thread(() -> {
			try (Socket socket = new Socket(InetAddress.getByName("localhost"), port)) {
				OutputStream outputStream = socket.getOutputStream();
				outputStream.write(TEST_STRING.getBytes());
				outputStream.write(TEST_STRING.getBytes());
				writeByte(outputStream, '\r', true);
				writeByte(outputStream, '\n', true);
				if (latch != null) {
					latch.await();
				}
			}
			catch (Exception ex) {
				logger.error(ex);
			}
		});
		thread.setDaemon(true);
		thread.start();
	}

	/**
	 * Sends a single message in two chunks and then closes the socket.
	 */
	public static void testSendRaw(final int port) {
		Thread thread = new Thread(() -> {
			try (Socket socket = new Socket(InetAddress.getByName("localhost"), port)) {
				OutputStream outputStream = socket.getOutputStream();
				outputStream.write(TEST_STRING.getBytes());
				outputStream.write(TEST_STRING.getBytes());
			}
			catch (Exception ex) {
				logger.error(ex);
			}
		});
		thread.setDaemon(true);
		thread.start();
	}

	/**
	 * Sends two serialized objects over the same socket.
	 * @param port the port for socket
	 */
	public static CountDownLatch testSendSerialized(final int port) {
		final CountDownLatch testCompleteLatch = new CountDownLatch(1);
		Thread thread = new Thread(() -> {
			try (Socket socket = new Socket(InetAddress.getByName("localhost"), port)) {
				OutputStream outputStream = socket.getOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(outputStream);
				oos.writeObject(TEST_STRING);
				oos.flush();
				oos = new ObjectOutputStream(outputStream);
				oos.writeObject(TEST_STRING);
				oos.flush();
				testCompleteLatch.await(10, TimeUnit.SECONDS);
			}
			catch (Exception e1) {
				logger.error(e1);
			}
		});
		thread.setDaemon(true);
		thread.start();
		return testCompleteLatch;
	}

	/**
	 * Sends a large CRLF message with no CRLF.
	 */
	public static CountDownLatch testSendCrLfOverflow(final int port) {
		final CountDownLatch testCompleteLatch = new CountDownLatch(1);
		Thread thread = new Thread(() -> {
			try (Socket socket = new Socket(InetAddress.getByName("localhost"), port)) {
				OutputStream outputStream = socket.getOutputStream();
				for (int i = 0; i < 1500; i++) {
					writeByte(outputStream, 'x', true);
				}
				testCompleteLatch.await(10, TimeUnit.SECONDS);
			}
			catch (Exception e) {

			}
		});
		thread.setDaemon(true);
		thread.start();
		return testCompleteLatch;
	}

	public static void setLocalNicIfPossible(
			AbstractInternetProtocolReceivingChannelAdapter adapter)
			throws UnknownHostException {
		InetAddress[] nics = InetAddress.getAllByName(null);
		if (nics.length > 0) {
			// just listen on the loopback interface
			String loopBack = nics[0].getHostAddress();
			adapter.setLocalAddress(loopBack);
		}
	}

	@Nullable
	public static NetworkInterface chooseANic(boolean multicast) throws Exception {
		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		while (interfaces.hasMoreElements()) {
			NetworkInterface networkInterface = interfaces.nextElement();
			if ((!multicast || networkInterface.supportsMulticast())
					&& !networkInterface.getName().contains("vboxnet")) {

				Enumeration<InetAddress> addressesFromNetworkInterface = networkInterface.getInetAddresses();
				while (addressesFromNetworkInterface.hasMoreElements()) {
					InetAddress inetAddress = addressesFromNetworkInterface.nextElement();
					if (inetAddress.isSiteLocalAddress()
							&& !inetAddress.isAnyLocalAddress()
							&& !inetAddress.isLinkLocalAddress()
							&& !inetAddress.isLoopbackAddress()) {

						return networkInterface;
					}
				}
			}
		}
		return null;
	}

	public static void waitListening(AbstractInternetProtocolReceivingChannelAdapter adapter) {
		await("Adapter not listening").atMost(Duration.ofSeconds(10)).until(adapter::isListening);
	}

}
