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

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import javax.net.ServerSocketFactory;

/**
 * TCP/IP Test utilities.
 * 
 * @author Gary Russell
 *
 */
public class Utils {

	public static final String TEST_STRING = "TestMessage";

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
						System.out.println(i + " Wrote first part");
						if (latch != null) {
							latch.await();
						}
						Thread.sleep(500);
						// send the second chunk
						socket.getOutputStream().write(TEST_STRING.getBytes());
						System.out.println(i + " Wrote second part");
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
	 * Test for reassembly of completely fragmented message; sends
	 * 6 bytes 500ms apart.
	 * @param os
	 * @param b
	 * @throws Exception
	 */
	public static void testSendFragmented(final int port) {
		Thread thread = new Thread(new Runnable() {
			public void run() {
				try {
					Socket socket = new Socket(InetAddress.getByName("localhost"), port);
					OutputStream os = socket.getOutputStream();
					writeByte(os, 0);
					writeByte(os, 0);
					writeByte(os, 0);
					writeByte(os, 2);
					writeByte(os, 'x');
					writeByte(os, 'x');
					Thread.sleep(1000000000L); // wait forever, but we're a daemon
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		thread.setDaemon(true);
		thread.start();
	}

	private static void writeByte(OutputStream os, int b) throws Exception {
		os.write(b);
		System.out.printf("Wrote 0x%x\n", b);
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
						writeByte(outputStream, 0x02);
						outputStream.write(TEST_STRING.getBytes());
						System.out.println(i + " Wrote first part");
						if (latch != null) {
							latch.await();
						}
						Thread.sleep(500);
						// send the second chunk
						outputStream.write(TEST_STRING.getBytes());
						System.out.println(i + " Wrote second part");
						writeByte(outputStream, 0x03);
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
						System.out.println(i + " Wrote first part");
						if (latch != null) {
							latch.await();
						}
						Thread.sleep(500);
						// send the second chunk
						outputStream.write(TEST_STRING.getBytes());
						System.out.println(i + " Wrote second part");
						writeByte(outputStream, '\r');
						writeByte(outputStream, '\n');
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
	
	public static int findAvailableServerSocket() {
		for (int i = 5678; i < 5878; i++) {
			try {
				ServerSocket sock = ServerSocketFactory.getDefault().createServerSocket(i);
				sock.close();
				return i;
			} catch (Exception e) { }
		}
		throw new RuntimeException("Cannot find a free server socket");
	}
}
