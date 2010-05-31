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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import javax.net.SocketFactory;

import org.junit.Test;
import org.springframework.integration.ip.util.SocketUtils;

/**
 * @author Gary Russell
 *
 */
public class NioSocketReaderTests {

	private CountDownLatch latch = new CountDownLatch(1);
	/**
	 * Test method for {@link org.springframework.integration.ip.tcp.NioSocketReader}.
	 */
	@Test
	public void testReadLength() throws Exception {
		ServerSocketChannel server = ServerSocketChannel.open();
		server.configureBlocking(false);
		int port = SocketUtils.findAvailableServerSocket();
		server.socket().bind(new InetSocketAddress(port));
		final Selector selector = Selector.open();
		server.register(selector, SelectionKey.OP_ACCEPT);
		
		// Fire up the sender.
		
		SocketUtils.testSendLength(port, latch);
		
		SocketChannel channel = accept(server, selector);
		NioSocketReader reader = new NioSocketReader(channel);
		int count = 0;
		while(selector.select(1000) > 0) {
			Set<SelectionKey> keys = selector.selectedKeys();
			Iterator<SelectionKey> iterator = keys.iterator();
			while (iterator.hasNext()) {
				SelectionKey key = iterator.next();
				iterator.remove();
				if (key.isReadable()) {
					assertEquals(channel, key.channel());
					if (reader.assembleData() == SocketReader.MESSAGE_COMPLETE) {
						assertEquals("Data", SocketUtils.TEST_STRING + SocketUtils.TEST_STRING, 
						         new String((byte[]) reader.getAssembledData()));
						count++;
					}
					latch.countDown();
				}
				else {
					fail("Unexpected key: " + key);
				}
			}
		}
		assertEquals("Did not receive data", 2, count);
		server.close();
	}

	@Test
	public void testFragmented() throws Exception {
		ServerSocketChannel server = ServerSocketChannel.open();
		server.configureBlocking(false);
		int port = SocketUtils.findAvailableServerSocket();
		server.socket().bind(new InetSocketAddress(port));
		final Selector selector = Selector.open();
		server.register(selector, SelectionKey.OP_ACCEPT);
		
		// Fire up the sender.
		SocketUtils.testSendFragmented(port, false);

		SocketChannel channel = accept(server, selector);
		NioSocketReader reader = new NioSocketReader(channel);
		boolean done = false;
		while(selector.select(1000) > 0) {
			Set<SelectionKey> keys = selector.selectedKeys();
			Iterator<SelectionKey> iterator = keys.iterator();
			while (iterator.hasNext()) {
				SelectionKey key = iterator.next();
				iterator.remove();
				if (key.isReadable()) {
					assertEquals(channel, key.channel());
					if (reader.assembleData() == SocketReader.MESSAGE_COMPLETE) {
						assertEquals("Data", "xx", 
						         new String((byte[]) reader.getAssembledData()));
						done = true;
					}
					latch.countDown();
				}
				else {
					fail("Unexpected key: " + key);
				}
			}
		}
		assertTrue("Did not receive data", done);
		server.close();
	}
	
	/**
	 * Test method for {@link org.springframework.integration.ip.tcp.NioSocketReader}.
	 */
	@Test
	public void testReadStxEtx() throws Exception {
		ServerSocketChannel server = ServerSocketChannel.open();
		server.configureBlocking(false);
		int port = SocketUtils.findAvailableServerSocket();
		server.socket().bind(new InetSocketAddress(port));
		final Selector selector = Selector.open();
		server.register(selector, SelectionKey.OP_ACCEPT);
		
		// Fire up the sender.
		SocketUtils.testSendStxEtx(port, latch);

		SocketChannel channel = accept(server, selector);
		NioSocketReader reader = new NioSocketReader(channel);
		reader.setMessageFormat(MessageFormats.FORMAT_STX_ETX);
		int count = 0;
		while(selector.select(1000) > 0) {
			Set<SelectionKey> keys = selector.selectedKeys();
			Iterator<SelectionKey> iterator = keys.iterator();
			while (iterator.hasNext()) {
				SelectionKey key = iterator.next();
				iterator.remove();
				if (key.isReadable()) {
					assertEquals(channel, key.channel());
					if (reader.assembleData() == SocketReader.MESSAGE_COMPLETE) {
						assertEquals("Data", SocketUtils.TEST_STRING + SocketUtils.TEST_STRING, 
						         new String((byte[]) reader.getAssembledData()));
						count++;
					}
					latch.countDown();
				}
				else {
					fail("Unexpected key: " + key);
				}
			}
		}
		assertEquals("Did not receive data", 2, count);
		server.close();
	}

	/**
	 * Test method for {@link org.springframework.integration.ip.tcp.NioSocketReader}.
	 */
	@Test
	public void testReadCrLf() throws Exception {
		ServerSocketChannel server = ServerSocketChannel.open();
		server.configureBlocking(false);
		int port = SocketUtils.findAvailableServerSocket();
		server.socket().bind(new InetSocketAddress(port));
		final Selector selector = Selector.open();
		server.register(selector, SelectionKey.OP_ACCEPT);
		
		// Fire up the sender.
		SocketUtils.testSendCrLf(port, latch);

		SocketChannel channel = accept(server, selector);
		NioSocketReader reader = new NioSocketReader(channel);
		reader.setMessageFormat(MessageFormats.FORMAT_CRLF);
		int count = 0;
		while(selector.select(1000) > 0) {
			Set<SelectionKey> keys = selector.selectedKeys();
			Iterator<SelectionKey> iterator = keys.iterator();
			while (iterator.hasNext()) {
				SelectionKey key = iterator.next();
				iterator.remove();
				if (key.isReadable()) {
					assertEquals(channel, key.channel());
					if (reader.assembleData() == SocketReader.MESSAGE_COMPLETE) {
						assertEquals("Data", SocketUtils.TEST_STRING + SocketUtils.TEST_STRING, 
						         new String((byte[]) reader.getAssembledData()));
						count++;
					}
					latch.countDown();
				}
				else {
					fail("Unexpected key: " + key);
				}
			}
		}
		assertEquals("Did not receive data", 2, count);
		server.close();
	}

	/**
	 * Test method for {@link org.springframework.integration.ip.tcp.NioSocketReader}.
	 */
	@Test
	public void testReadLengthOverflow() throws Exception {
		ServerSocketChannel server = ServerSocketChannel.open();
		server.configureBlocking(false);
		int port = SocketUtils.findAvailableServerSocket();
		server.socket().bind(new InetSocketAddress(port));
		final Selector selector = Selector.open();
		server.register(selector, SelectionKey.OP_ACCEPT);
		
		// Fire up the sender.
		SocketUtils.testSendLengthOverflow(port);

		SocketChannel channel = accept(server, selector);
		NioSocketReader reader = new NioSocketReader(channel);
		int count = 0;
		while(selector.select(1000) > 0) {
			Set<SelectionKey> keys = selector.selectedKeys();
			Iterator<SelectionKey> iterator = keys.iterator();
			while (iterator.hasNext()) {
				SelectionKey key = iterator.next();
				iterator.remove();
				if (key.isReadable()) {
					assertEquals(channel, key.channel());
					try {
					    if (reader.assembleData() == SocketReader.MESSAGE_COMPLETE) {
					    	fail("Expected message length exceeded exception");
					    }
					} catch (IOException e) {
						if (!e.getMessage().startsWith("Message length")) {
							e.printStackTrace();
							fail("Unexpected IO Error:" + e.getMessage());
						}
						count++;
						break;
					}
				}
				else {
					fail("Unexpected key: " + key);
				}
			}
			if (count > 0) {
				break;
			}
		}
		server.close();
	}

	/**
	 * Test method for {@link org.springframework.integration.ip.tcp.NioSocketReader}.
	 */
	@Test
	public void testReadStxEtxOverflow() throws Exception {
		ServerSocketChannel server = ServerSocketChannel.open();
		server.configureBlocking(false);
		int port = SocketUtils.findAvailableServerSocket();
		server.socket().bind(new InetSocketAddress(port));
		final Selector selector = Selector.open();
		server.register(selector, SelectionKey.OP_ACCEPT);
		
		// Fire up the sender.
		SocketUtils.testSendStxEtxOverflow(port);

		SocketChannel channel = accept(server, selector);
		NioSocketReader reader = new NioSocketReader(channel);
		reader.setMessageFormat(MessageFormats.FORMAT_STX_ETX);
		reader.setMaxMessageSize(1024);
		int count = 0;
		while(selector.select(1000) > 0) {
			Set<SelectionKey> keys = selector.selectedKeys();
			Iterator<SelectionKey> iterator = keys.iterator();
			while (iterator.hasNext()) {
				SelectionKey key = iterator.next();
				iterator.remove();
				if (key.isReadable()) {
					assertEquals(channel, key.channel());
					try {
					    if (reader.assembleData() == SocketReader.MESSAGE_COMPLETE) {
					    	fail("Expected message length exceeded exception");
					    }
					} catch (IOException e) {
						if (!e.getMessage().startsWith("ETX not found")) {
							e.printStackTrace();
							fail("Unexpected IO Error:" + e.getMessage());
						}
						count++;
						break;
					}
				}
				else {
					fail("Unexpected key: " + key);
				}
			}
			if (count > 0) {
				break;
			}
		}
		server.close();
	}

	/**
	 * Test method for {@link org.springframework.integration.ip.tcp.NioSocketReader}.
	 */
	@Test
	public void testReadCrLfOverflow() throws Exception {
		ServerSocketChannel server = ServerSocketChannel.open();
		server.configureBlocking(false);
		int port = SocketUtils.findAvailableServerSocket();
		server.socket().bind(new InetSocketAddress(port));
		final Selector selector = Selector.open();
		server.register(selector, SelectionKey.OP_ACCEPT);
		
		// Fire up the sender.
		SocketUtils.testSendCrLfOverflow(port);

		SocketChannel channel = accept(server, selector);
		NioSocketReader reader = new NioSocketReader(channel);
		reader.setMessageFormat(MessageFormats.FORMAT_CRLF);
		reader.setMaxMessageSize(1024);
		int count = 0;
		while(selector.select(1000) > 0) {
			Set<SelectionKey> keys = selector.selectedKeys();
			Iterator<SelectionKey> iterator = keys.iterator();
			while (iterator.hasNext()) {
				SelectionKey key = iterator.next();
				iterator.remove();
				if (key.isReadable()) {
					assertEquals(channel, key.channel());
					try {
					    if (reader.assembleData() == SocketReader.MESSAGE_COMPLETE) {
					    	fail("Expected message length exceeded exception");
					    }
					} catch (IOException e) {
						if (!e.getMessage().startsWith("CRLF not found")) {
							e.printStackTrace();
							fail("Unexpected IO Error:" + e.getMessage());
						}
						count++;
						break;
					}
				}
				else {
					fail("Unexpected key: " + key);
				}
			}
			if (count > 0) {
				break;
			}
		}
		server.close();
	}

	/**
	 * Tests socket closure when no data received.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCloseCleanupNoData() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		final Semaphore semaphore = new Semaphore(0);
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			public void run() {
				try {
					semaphore.acquire();
					while (true) {
						Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
						semaphore.acquire();
						socket.close();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		try {
			ServerSocketChannel server = ServerSocketChannel.open();
			server.configureBlocking(false);
			server.socket().bind(new InetSocketAddress(port));
			final Selector selector = Selector.open();
			server.register(selector, SelectionKey.OP_ACCEPT);
			
			semaphore.release();

			SocketChannel channel = accept(server, selector);

			NioSocketReader reader = new NioSocketReader(channel);
			semaphore.release();
			assertTrue(assembleData(reader) < 0);
			assertTrue(reader.getSocket().isClosed());
			
			channel = accept(server, selector);
			reader = new NioSocketReader(channel);
			reader.setMessageFormat(MessageFormats.FORMAT_CRLF);
			semaphore.release();
			assertTrue(assembleData(reader) < 0);
			assertTrue(reader.getSocket().isClosed());			
			
			channel = accept(server, selector);
			reader = new NioSocketReader(channel);
			reader.setMessageFormat(MessageFormats.FORMAT_STX_ETX);
			semaphore.release();
			assertTrue(assembleData(reader) < 0);
			assertTrue(reader.getSocket().isClosed());			
			
			channel = accept(server, selector);
			reader = new CustomNioSocketReader(channel);
			reader.setMessageFormat(MessageFormats.FORMAT_CUSTOM);
			semaphore.release();
			assertTrue(assembleData(reader) < 0);
			assertTrue(reader.getSocket().isClosed());		
			
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	/**
	 * Tests socket closure when mid-message
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCloseCleanup() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		final Semaphore semaphore = new Semaphore(0);
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			public void run() {
				try {
					semaphore.acquire();
					
					Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
					byte[] header = {0, 0, 0, 10};
					socket.getOutputStream().write(header);
					socket.getOutputStream().write("xx".getBytes());
					semaphore.acquire();
					socket.close();
					
					socket = SocketFactory.getDefault().createSocket("localhost", port);
					socket.getOutputStream().write("xx".getBytes());
					semaphore.acquire();
					socket.close();

					socket = SocketFactory.getDefault().createSocket("localhost", port);
					socket.getOutputStream().write(MessageFormats.STX);
					socket.getOutputStream().write("xx".getBytes());
					semaphore.acquire();
					socket.close();
					
					socket = SocketFactory.getDefault().createSocket("localhost", port);
					socket.getOutputStream().write(MessageFormats.STX);
					socket.getOutputStream().write("xx".getBytes());
					semaphore.acquire();
					socket.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		try {
			ServerSocketChannel server = ServerSocketChannel.open();
			server.configureBlocking(false);
			server.socket().bind(new InetSocketAddress(port));
			final Selector selector = Selector.open();
			server.register(selector, SelectionKey.OP_ACCEPT);
			
			semaphore.release();

			SocketChannel channel = accept(server, selector);

			NioSocketReader reader = new NioSocketReader(channel);
			semaphore.release();
			try {
				assembleData(reader);
				fail("Exception expected");
			} catch (IOException e) { }
			assertTrue(reader.getSocket().isClosed());
			
			channel = accept(server, selector);
			reader = new NioSocketReader(channel);
			reader.setMessageFormat(MessageFormats.FORMAT_CRLF);
			semaphore.release();
			try {
				assembleData(reader);
				fail("Exception expected");
			} catch (IOException e) { }
			assertTrue(reader.getSocket().isClosed());			
			
			channel = accept(server, selector);
			reader = new NioSocketReader(channel);
			reader.setMessageFormat(MessageFormats.FORMAT_STX_ETX);
			semaphore.release();
			try {
				assembleData(reader);
				fail("Exception expected");
			} catch (IOException e) { }
			assertTrue(reader.getSocket().isClosed());			
			
			channel = accept(server, selector);
			reader = new CustomNioSocketReader(channel);
			reader.setMessageFormat(MessageFormats.FORMAT_CUSTOM);
			semaphore.release();
			try {
				assembleData(reader);
				fail("Exception expected");
			} catch (IOException e) { }
			assertTrue(reader.getSocket().isClosed());		
			
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}



	/** Poor man's nio reader 
	 * 
	 * @param reader
	 * @return
	 * @throws IOException
	 */
	private int assembleData(NioSocketReader reader) throws Exception {
		int m = 0;
		while (true) {
			int n = reader.assembleData();
			if (n < 0) {
				return n;
			}
			Thread.sleep(10);
			if (m++ > 1000)
				throw new Exception("No close detected");
		}
	}

	private SocketChannel accept(ServerSocketChannel server,
			final Selector selector) throws IOException, ClosedChannelException {
		SocketChannel channel = null;
		
		if(selector.select(10000) <= 0) {
			fail("Socket failed to connect");
		}
		Set<SelectionKey> keys = selector.selectedKeys();
		Iterator<SelectionKey> iterator = keys.iterator();
		while (iterator.hasNext()) {
			SelectionKey key = iterator.next();
			iterator.remove();
			if (key.isAcceptable()) {
				channel = server.accept();
				channel.configureBlocking(false);
				channel.register(selector, SelectionKey.OP_READ);
			}
			else {
				fail("Unexpected key: " + key);
			}
		}
		return channel;
	}

}
