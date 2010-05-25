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
package org.springframework.integration.ip.util;

import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import javax.net.ServerSocketFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * TCP/IP Test utilities.
 * 
 * @author Gary Russell
 *
 */
public class SocketUtils {

	public static final String TEST_STRING = "TestMessage";

	private static final Log logger = LogFactory.getLog(SocketUtils.class);
	
	/**
	 * Sends a message in two chunks with a preceding length. Two such messages are sent.
	 * @param latch If not null, await until counted down before sending second chunk.
	 */
	public static void testSendLength(final int port, final CountDownLatch latch) {
		Thread thread = new Thread(new Runnable() {
			public void run() {
				try {
					Socket socket = new Socket(InetAddress.getByName("localhost"), port);
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
					Thread.sleep(1000000000L); // wait forever, but we're a daemon
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		thread.setDaemon(true);
		thread.start();
	}

	/**
	 * Sends a message with a bad length part, causing an overflow on the receiver.
	 */
	public static void testSendLengthOverflow(final int port) {
		Thread thread = new Thread(new Runnable() {
			public void run() {
				try {
					Socket socket = new Socket(InetAddress.getByName("localhost"), port);
					byte[] len = new byte[4];
					ByteBuffer.wrap(len).putInt(Integer.MAX_VALUE);
					socket.getOutputStream().write(len);
					socket.getOutputStream().write(TEST_STRING.getBytes());
					Thread.sleep(1000000000L); // wait forever, but we're a daemon
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		thread.setDaemon(true);
		thread.start();
	}

	/**
	 * Test for reassembly of completely fragmented message; sends
	 * 6 bytes 500ms apart.
	 * @param os
	 * @param b
	 * @throws Exception
	 */
	public static void testSendFragmented(final int port, final boolean noDelay) {
		Thread thread = new Thread(new Runnable() {
			public void run() {
				try {
					logger.debug("Connecting to " + port);
					Socket socket = new Socket(InetAddress.getByName("localhost"), port);
					OutputStream os = socket.getOutputStream();
					writeByte(os, 0, noDelay);
					writeByte(os, 0, noDelay);
					writeByte(os, 0, noDelay);
					writeByte(os, 2, noDelay);
					writeByte(os, 'x', noDelay);
					writeByte(os, 'x', noDelay);
					Thread.sleep(1000000000L); // wait forever, but we're a daemon
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		thread.setDaemon(true);
		thread.start();
	}

	private static void writeByte(OutputStream os, int b, boolean noDelay) throws Exception {
		os.write(b);
		logger.debug("Wrote 0x" + Integer.toHexString(b));
		if (noDelay) {
			return;
		}
		Thread.sleep(500);
	}
	
	/**
	 * Sends a STX/ETX message in two chunks. Two such messages are sent.
	 * @param latch If not null, await until counted down before sending second chunk.
	 */
	public static void testSendStxEtx(final int port, final CountDownLatch latch) {
		Thread thread = new Thread(new Runnable() {
			public void run() {
				try {
					Socket socket = new Socket(InetAddress.getByName("localhost"), port);
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
					Thread.sleep(1000000000L); // wait forever, but we're a daemon
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		thread.setDaemon(true);
		thread.start();
	}
	
	/**
	 * Sends a large STX/ETX message with no ETX
	 */
	public static void testSendStxEtxOverflow(final int port) {
		Thread thread = new Thread(new Runnable() {
			public void run() {
				try {
					Socket socket = new Socket(InetAddress.getByName("localhost"), port);
					OutputStream outputStream = socket.getOutputStream();
					writeByte(outputStream, 0x02, true);
					for (int i = 0; i < 1500; i++) {
						writeByte(outputStream, 'x', true);
					}
					Thread.sleep(1000000000L); // wait forever, but we're a daemon
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		thread.setDaemon(true);
		thread.start();
	}
	
	/**
	 * Sends a message +CRLF in two chunks. Two such messages are sent.
	 * @param latch If not null, await until counted down before sending second chunk.
	 */
	public static void testSendCrLf(final int port, final CountDownLatch latch) {
		Thread thread = new Thread(new Runnable() {
			public void run() {
				try {
					Socket socket = new Socket(InetAddress.getByName("localhost"), port);
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
					Thread.sleep(1000000000L); // wait forever, but we're a daemon
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		thread.setDaemon(true);
		thread.start();
	}
	
	/**
	 * Sends a large CRLF message with no CRLF.
	 */
	public static void testSendCrLfOverflow(final int port) {
		Thread thread = new Thread(new Runnable() {
			public void run() {
				try {
					Socket socket = new Socket(InetAddress.getByName("localhost"), port);
					OutputStream outputStream = socket.getOutputStream();
					for (int i = 0; i < 1500; i++) {
						writeByte(outputStream, 'x', true);
					}
					Thread.sleep(1000000000L); // wait forever, but we're a daemon
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		thread.setDaemon(true);
		thread.start();
	}
	
	public static int findAvailableServerSocket(int seed) {
		for (int i = seed; i < seed+200; i++) {
			try {
				ServerSocket sock = ServerSocketFactory.getDefault().createServerSocket(i);
				sock.close();
				return i;
			} catch (Exception e) { }
		}
		throw new RuntimeException("Cannot find a free server socket");
	}
	
	public static int findAvailableServerSocket() {
		return findAvailableServerSocket(5678);
	}

	public static int findAvailableUdpSocket(int seed) {
		for (int i = seed; i < seed+200; i++) {
			try {
				DatagramSocket sock = new DatagramSocket(i);
				sock.close();
				Thread.sleep(100);
				return i;
			} catch (Exception e) { }
		}
		throw new RuntimeException("Cannot find a free server socket");
	}
	
	public static int findAvailableUdpSocket() {
		return findAvailableUdpSocket(9876);
	}

}
