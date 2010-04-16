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

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;

import javax.net.ServerSocketFactory;

import org.junit.Test;
import org.springframework.integration.ip.util.SocketUtils;

/**
 * @author Gary Russell
 * 
 */
public class NioSocketWriterTests {

	@Test
	public void testBuffersNoWait() throws Exception {
		NioSocketWriter writer = new NioSocketWriter(null, 2, 2048);
		ByteBuffer b1 = writer.getBuffer();
		ByteBuffer b2 = writer.getBuffer();
		writer.returnBuffer(b2);
		ByteBuffer b3 = writer.getBuffer();
		assertEquals(b2, b3);
		writer.returnBuffer(b3);
		writer.returnBuffer(b1);
		b3 = writer.getBuffer();
		assertEquals(b2, b3);
		writer.returnBuffer(b3);
		b3 = writer.getBuffer();
		assertEquals(b1, b3);
		writer.returnBuffer(b3);
	}
	
	@Test
	public void testBuffersWait() throws Exception {
		final NioSocketWriter writer = new NioSocketWriter(null, 2, 2048);
		ByteBuffer b1 = writer.getBuffer();
		ByteBuffer b2 = writer.getBuffer();
		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);
		final ByteBuffer b2a = b2;
		new Thread(new Runnable(){
			public void run() {
				latch1.countDown();
				try {
					ByteBuffer b = writer.getBuffer();
					assertEquals(b2a, b);
					writer.returnBuffer(b);
					latch2.countDown();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
		}).start();
		latch1.await();
		Thread.sleep(2000);
		writer.returnBuffer(b2);
		latch2.await();
		ByteBuffer b3 = writer.getBuffer();
		assertEquals(b2, b3);
		writer.returnBuffer(b3);
		writer.returnBuffer(b1);
		b3 = writer.getBuffer();
		assertEquals(b2, b3);
		writer.returnBuffer(b3);
		b3 = writer.getBuffer();
		assertEquals(b1, b3);
		writer.returnBuffer(b3);
	}
	
	@Test
	public void testWriteLengthHeader() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		final String testString = "abcdef";
		ServerSocket server = ServerSocketFactory.getDefault()
				.createServerSocket(port);
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					ByteBuffer buffer = ByteBuffer
							.allocate(testString.length());
					buffer.put(testString.getBytes());
					SocketChannel channel = SocketChannel
							.open(new InetSocketAddress("localhost", port));
					NioSocketWriter writer = new NioSocketWriter(channel, 2,
							2048);
					writer
							.setMessageFormat(MessageFormats.FORMAT_LENGTH_HEADER);
					writer.write(buffer.array());
					Thread.sleep(1000000000L);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		t.setDaemon(true);
		t.start();
		Socket socket = server.accept();
		InputStream is = socket.getInputStream();
		byte[] buff = new byte[testString.length() + 4];
		readFully(is, buff);
		ByteBuffer buffer = ByteBuffer.wrap(buff);
		assertEquals(testString.length(), buffer.getInt());
		assertEquals(testString, new String(buff, 4, testString.length()));
		server.close();
	}

	@Test
	public void testWriteStxEtx() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		final String testString = "abcdef";
		ServerSocket server = ServerSocketFactory.getDefault()
				.createServerSocket(port);
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					ByteBuffer buffer = ByteBuffer
							.allocate(testString.length());
					buffer.put(testString.getBytes());
					SocketChannel channel = SocketChannel
							.open(new InetSocketAddress("localhost", port));
					NioSocketWriter writer = new NioSocketWriter(channel, 2,
							2048);
					writer.setMessageFormat(MessageFormats.FORMAT_STX_ETX);
					writer.write(buffer.array());
					Thread.sleep(1000000000L);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		t.setDaemon(true);
		t.start();
		Socket socket = server.accept();
		InputStream is = socket.getInputStream();
		byte[] buff = new byte[testString.length() + 2];
		readFully(is, buff);
		assertEquals(MessageFormats.STX, buff[0]);
		assertEquals(testString, new String(buff, 1, testString.length()));
		assertEquals(MessageFormats.ETX, buff[testString.length() + 1]);
		server.close();
	}

	@Test
	public void testWriteCrLf() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		final String testString = "abcdef";
		ServerSocket server = ServerSocketFactory.getDefault()
				.createServerSocket(port);
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					ByteBuffer buffer = ByteBuffer
							.allocate(testString.length());
					buffer.put(testString.getBytes());
					SocketChannel channel = SocketChannel
							.open(new InetSocketAddress("localhost", port));
					NioSocketWriter writer = new NioSocketWriter(channel, 2,
							2048);
					writer.setMessageFormat(MessageFormats.FORMAT_CRLF);
					writer.write(buffer.array());
					Thread.sleep(1000000000L);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		t.setDaemon(true);
		t.start();
		Socket socket = server.accept();
		InputStream is = socket.getInputStream();
		byte[] buff = new byte[testString.length() + 2];
		readFully(is, buff);
		assertEquals(testString, new String(buff, 0, testString.length()));
		assertEquals('\r', buff[testString.length()]);
		assertEquals('\n', buff[testString.length() + 1]);
		server.close();
	}

	@Test
	public void testWriteLengthHeaderDirect() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		final String testString = "abcdef";
		ServerSocket server = ServerSocketFactory.getDefault()
				.createServerSocket(port);
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					ByteBuffer buffer = ByteBuffer
							.allocate(testString.length());
					buffer.put(testString.getBytes());
					SocketChannel channel = SocketChannel
							.open(new InetSocketAddress("localhost", port));
					NioSocketWriter writer = new NioSocketWriter(channel, 2,
							2048);
					writer
							.setMessageFormat(MessageFormats.FORMAT_LENGTH_HEADER);
					writer.setUsingDirectBuffers(true);
					writer.write(buffer.array());
					Thread.sleep(1000000000L);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		t.setDaemon(true);
		t.start();
		Socket socket = server.accept();
		InputStream is = socket.getInputStream();
		byte[] buff = new byte[testString.length() + 4];
		readFully(is, buff);
		ByteBuffer buffer = ByteBuffer.wrap(buff);
		assertEquals(testString.length(), buffer.getInt());
		assertEquals(testString, new String(buff, 4, testString.length()));
		server.close();
	}

	@Test
	public void testWriteStxEtxDirect() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		final String testString = "abcdef";
		ServerSocket server = ServerSocketFactory.getDefault()
				.createServerSocket(port);
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					ByteBuffer buffer = ByteBuffer
							.allocate(testString.length());
					buffer.put(testString.getBytes());
					SocketChannel channel = SocketChannel
							.open(new InetSocketAddress("localhost", port));
					NioSocketWriter writer = new NioSocketWriter(channel, 2,
							2048);
					writer.setMessageFormat(MessageFormats.FORMAT_STX_ETX);
					writer.setUsingDirectBuffers(true);
					writer.write(buffer.array());
					Thread.sleep(1000000000L);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		t.setDaemon(true);
		t.start();
		Socket socket = server.accept();
		InputStream is = socket.getInputStream();
		byte[] buff = new byte[testString.length() + 2];
		readFully(is, buff);
		assertEquals(MessageFormats.STX, buff[0]);
		assertEquals(testString, new String(buff, 1, testString.length()));
		assertEquals(MessageFormats.ETX, buff[testString.length() + 1]);
		server.close();
	}

	@Test
	public void testWriteCrLfDirect() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		final String testString = "abcdef";
		ServerSocket server = ServerSocketFactory.getDefault()
				.createServerSocket(port);
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					ByteBuffer buffer = ByteBuffer
							.allocate(testString.length());
					buffer.put(testString.getBytes());
					SocketChannel channel = SocketChannel
							.open(new InetSocketAddress("localhost", port));
					NioSocketWriter writer = new NioSocketWriter(channel, 2,
							2048);
					writer.setMessageFormat(MessageFormats.FORMAT_CRLF);
					writer.setUsingDirectBuffers(true);
					writer.write(buffer.array());
					Thread.sleep(1000000000L);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		t.setDaemon(true);
		t.start();
		Socket socket = server.accept();
		InputStream is = socket.getInputStream();
		byte[] buff = new byte[testString.length() + 2];
		readFully(is, buff);
		assertEquals(testString, new String(buff, 0, testString.length()));
		assertEquals('\r', buff[testString.length()]);
		assertEquals('\n', buff[testString.length() + 1]);
		server.close();
	}

	/**
	 * @param is
	 * @param buff
	 */
	private void readFully(InputStream is, byte[] buff) throws IOException {
		for (int i = 0; i < buff.length; i++) {
			buff[i] = (byte) is.read();
		}
	}

}
