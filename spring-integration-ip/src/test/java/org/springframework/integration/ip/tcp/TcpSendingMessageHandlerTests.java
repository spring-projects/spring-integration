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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ServerSocketFactory;

import org.junit.Test;
import org.springframework.commons.serializer.java.JavaStreamingConverter;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNetClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNioClientConnectionFactory;
import org.springframework.integration.ip.tcp.converter.ByteArrayCrLfConverter;
import org.springframework.integration.ip.tcp.converter.ByteArrayLengthHeaderConverter;
import org.springframework.integration.ip.tcp.converter.ByteArrayStxEtxConverter;
import org.springframework.integration.ip.util.SocketUtils;
import org.springframework.integration.message.MessageBuilder;


/**
 * @author Gary Russell
 * @since 2.0
 *
 */
public class TcpSendingMessageHandlerTests {

	@Test
	public void testNet() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		final String testString = "abcdef";
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		server.setSoTimeout(10000);
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					TcpNetSendingMessageHandler handler = new TcpNetSendingMessageHandler("localhost", port);
					handler.setMessageFormat(MessageFormats.FORMAT_STX_ETX);
					Message<String> message = MessageBuilder.withPayload(testString).build();
					handler.handleMessage(message);
					Thread.sleep(1000000000L);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		t.setDaemon(true);
		t.start();
		Socket socket = server.accept();
		socket.setSoTimeout(5000);
		InputStream is = socket.getInputStream();
		byte[] buff = new byte[testString.length() + 2];
		readFully(is, buff);
		assertEquals(MessageFormats.STX, buff[0]);
		assertEquals(testString, new String(buff, 1, testString.length()));
		assertEquals(MessageFormats.ETX, buff[testString.length() + 1]);
		server.close();
	}

	@Test
	public void testNetCustom() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		final String testString = "abcdef";
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		server.setSoTimeout(10000);
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					TcpNetSendingMessageHandler handler = new TcpNetSendingMessageHandler("localhost", port);
					handler.setMessageFormat(MessageFormats.FORMAT_CUSTOM);
					handler.setCustomSocketWriterClassName("org.springframework.integration.ip.tcp.CustomNetSocketWriter");
					Message<String> message = MessageBuilder.withPayload(testString).build();
					handler.handleMessage(message);
					Thread.sleep(1000000000L);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		t.setDaemon(true);
		t.start();
		Socket socket = server.accept();
		socket.setSoTimeout(5000);
		InputStream is = socket.getInputStream();
		byte[] buff = new byte[24];
		readFully(is, buff);
		assertEquals((testString + "                        ").substring(0, 24), 
				new String(buff));
		server.close();
	}

	@Test
	public void testNio() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		final String testString = "abcdef";
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		server.setSoTimeout(10000);
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					TcpNioSendingMessageHandler handler = new TcpNioSendingMessageHandler("localhost", port);
					handler.setMessageFormat(MessageFormats.FORMAT_STX_ETX);
					Message<String> message = MessageBuilder.withPayload(testString).build();
					handler.handleMessage(message);
					Thread.sleep(1000000000L);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		t.setDaemon(true);
		t.start();
		Socket socket = server.accept();
		socket.setSoTimeout(5000);
		InputStream is = socket.getInputStream();
		byte[] buff = new byte[testString.length() + 2];
		readFully(is, buff);
		assertEquals(MessageFormats.STX, buff[0]);
		assertEquals(testString, new String(buff, 1, testString.length()));
		assertEquals(MessageFormats.ETX, buff[testString.length() + 1]);
		server.close();
	}
	
	@Test
	public void testNioDirect() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		final String testString = "abcdef";
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		server.setSoTimeout(10000);
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					TcpNioSendingMessageHandler handler = new TcpNioSendingMessageHandler("localhost", port);
					handler.setMessageFormat(MessageFormats.FORMAT_STX_ETX);
					handler.setUsingDirectBuffers(true);
					Message<String> message = MessageBuilder.withPayload(testString).build();
					handler.handleMessage(message);
					Thread.sleep(1000000000L);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		t.setDaemon(true);
		t.start();
		Socket socket = server.accept();
		socket.setSoTimeout(5000);
		InputStream is = socket.getInputStream();
		byte[] buff = new byte[testString.length() + 2];
		readFully(is, buff);
		assertEquals(MessageFormats.STX, buff[0]);
		assertEquals(testString, new String(buff, 1, testString.length()));
		assertEquals(MessageFormats.ETX, buff[testString.length() + 1]);
		server.close();
	}
	
	@Test
	public void testNioCustom() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		final String testString = "abcdef";
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		server.setSoTimeout(10000);
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					TcpNioSendingMessageHandler handler = new TcpNioSendingMessageHandler("localhost", port);
					handler.setMessageFormat(MessageFormats.FORMAT_CUSTOM);
					handler.setCustomSocketWriterClassName("org.springframework.integration.ip.tcp.CustomNioSocketWriter");
					Message<String> message = MessageBuilder.withPayload(testString).build();
					handler.handleMessage(message);
					Thread.sleep(1000000000L);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		t.setDaemon(true);
		t.start();
		Socket socket = server.accept();
		socket.setSoTimeout(5000);
		InputStream is = socket.getInputStream();
		byte[] buff = new byte[24];
		readFully(is, buff);
		assertEquals((testString + "                        ").substring(0, 24), 
				new String(buff));
		server.close();
	}
	
	private void readFully(InputStream is, byte[] buff) throws IOException {
		for (int i = 0; i < buff.length; i++) {
			buff[i] = (byte) is.read();
		}
	}
	
	@Test
	public void newTestNetCrLf() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean done = new AtomicBoolean();
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			public void run() {
				try {
					ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
					latch.countDown();
					Socket socket = server.accept();
					int i = 0;
					while (true) {
						byte[] b = new byte[6];
						readFully(socket.getInputStream(), b);
						b = ("Reply" + (++i) + "\r\n").getBytes();
						socket.getOutputStream().write(b);
					}
				} catch (Exception e) {
					if (!done.get()) {
						e.printStackTrace();
					}
				}
			}
		});
		AbstractConnectionFactory ccf = new TcpNetClientConnectionFactory("localhost", port);
		ByteArrayCrLfConverter converter = new ByteArrayCrLfConverter();
		ccf.setInputConverter(converter);
		ccf.setOutputConverter(converter);
		ccf.setSoTimeout(10000);		
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(ccf);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(ccf);
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		assertTrue(latch.await(10, TimeUnit.SECONDS));		
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		Message<?> mOut = channel.receive(10000);
		assertNotNull(mOut);
		assertEquals("Reply1", new String((byte[]) mOut.getPayload()));
		mOut = channel.receive(10000);
		assertNotNull(mOut);
		assertEquals("Reply2", new String((byte[]) mOut.getPayload()));
		done.set(true);
	}

	@Test
	public void newTestNio() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean done = new AtomicBoolean();
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			public void run() {
				try {
					ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
					latch.countDown();
					Socket socket = server.accept();
					int i = 0;
					while (true) {
						byte[] b = new byte[6];
						readFully(socket.getInputStream(), b);
						b = ("Reply" + (++i) + "\r\n").getBytes();
						socket.getOutputStream().write(b);
					}
				} catch (Exception e) {
					if (!done.get()) {
						e.printStackTrace();
					}
				}
			}
		});
		AbstractConnectionFactory ccf = new TcpNioClientConnectionFactory("localhost", port);
		ByteArrayCrLfConverter converter = new ByteArrayCrLfConverter();
		ccf.setInputConverter(converter);
		ccf.setOutputConverter(converter);
//		ccf.setSoTimeout(10000);
		ccf.start();
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(ccf);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(ccf);
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		Message<?> mOut = channel.receive(10000);
		assertNotNull(mOut);
		assertEquals("Reply1", new String((byte[]) mOut.getPayload()));
		mOut = channel.receive(10000);
		assertNotNull(mOut);
		assertEquals("Reply2", new String((byte[]) mOut.getPayload()));
		done.set(true);
	}
	
	@Test
	public void newTestNetStxEtx() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean done = new AtomicBoolean();
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			public void run() {
				try {
					ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
					latch.countDown();
					Socket socket = server.accept();
					int i = 0;
					while (true) {
						byte[] b = new byte[6];
						readFully(socket.getInputStream(), b);
						b = ("\u0002Reply" + (++i) + "\u0003").getBytes();
						socket.getOutputStream().write(b);
					}
				} catch (Exception e) {
					if (!done.get()) {
						e.printStackTrace();
					}
				}
			}
		});
		AbstractConnectionFactory ccf = new TcpNetClientConnectionFactory("localhost", port);
		ByteArrayStxEtxConverter converter = new ByteArrayStxEtxConverter();
		ccf.setInputConverter(converter);
		ccf.setOutputConverter(converter);
		ccf.setSoTimeout(10000);
		ccf.start();
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(ccf);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(ccf);
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		assertTrue(latch.await(10, TimeUnit.SECONDS));		
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		Message<?> mOut = channel.receive(10000);
		assertNotNull(mOut);
		assertEquals("Reply1", new String((byte[]) mOut.getPayload()));
		mOut = channel.receive(10000);
		assertNotNull(mOut);
		assertEquals("Reply2", new String((byte[]) mOut.getPayload()));
		done.set(true);
	}

	@Test
	public void newTestNioStxEtx() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean done = new AtomicBoolean();
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			public void run() {
				try {
					ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
					latch.countDown();
					Socket socket = server.accept();
					int i = 0;
					while (true) {
						byte[] b = new byte[6];
						readFully(socket.getInputStream(), b);
						b = ("\u0002Reply" + (++i) + "\u0003").getBytes();
						socket.getOutputStream().write(b);
					}
				} catch (Exception e) {
					if (!done.get()) {
						e.printStackTrace();
					}
				}
			}
		});
		AbstractConnectionFactory ccf = new TcpNioClientConnectionFactory("localhost", port);
		ByteArrayStxEtxConverter converter = new ByteArrayStxEtxConverter();
		ccf.setInputConverter(converter);
		ccf.setOutputConverter(converter);
		ccf.setSoTimeout(10000);
		ccf.start();
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(ccf);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(ccf);
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		Message<?> mOut = channel.receive(10000);
		assertNotNull(mOut);
		assertEquals("Reply1", new String((byte[]) mOut.getPayload()));
		mOut = channel.receive(10000);
		assertNotNull(mOut);
		assertEquals("Reply2", new String((byte[]) mOut.getPayload()));
		done.set(true);
	}

	@Test
	public void newTestNetLength() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean done = new AtomicBoolean();
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			public void run() {
				try {
					ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
					latch.countDown();
					Socket socket = server.accept();
					int i = 0;
					while (true) {
						byte[] b = new byte[8];
						readFully(socket.getInputStream(), b);
						if (!"\u0000\u0000\u0000\u0004Test".equals(new String(b))) {
							throw new RuntimeException("Bad Data");
						}
						b = ("\u0000\u0000\u0000\u0006Reply" + (++i)).getBytes();
						socket.getOutputStream().write(b);
					}
				} catch (Exception e) {
					if (!done.get()) {
						e.printStackTrace();
					}
				}
			}
		});
		AbstractConnectionFactory ccf = new TcpNetClientConnectionFactory("localhost", port);
		ByteArrayLengthHeaderConverter converter = new ByteArrayLengthHeaderConverter();
		ccf.setInputConverter(converter);
		ccf.setOutputConverter(converter);
		ccf.setSoTimeout(10000);
		ccf.start();
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(ccf);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(ccf);
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		assertTrue(latch.await(10, TimeUnit.SECONDS));		
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		Message<?> mOut = channel.receive(10000);
		assertNotNull(mOut);
		assertEquals("Reply1", new String((byte[]) mOut.getPayload()));
		mOut = channel.receive(10000);
		assertNotNull(mOut);
		assertEquals("Reply2", new String((byte[]) mOut.getPayload()));
		done.set(true);
	}

	@Test
	public void newTestNioLength() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean done = new AtomicBoolean();
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			public void run() {
				try {
					ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
					latch.countDown();
					Socket socket = server.accept();
					int i = 0;
					while (true) {
						byte[] b = new byte[8];
						readFully(socket.getInputStream(), b);
						if (!"\u0000\u0000\u0000\u0004Test".equals(new String(b))) {
							throw new RuntimeException("Bad Data");
						}
						b = ("\u0000\u0000\u0000\u0006Reply" + (++i)).getBytes();
						socket.getOutputStream().write(b);
					}
				} catch (Exception e) {
					if (!done.get()) {
						e.printStackTrace();
					}
				}
			}
		});
		AbstractConnectionFactory ccf = new TcpNioClientConnectionFactory("localhost", port);
		ByteArrayLengthHeaderConverter converter = new ByteArrayLengthHeaderConverter();
		ccf.setInputConverter(converter);
		ccf.setOutputConverter(converter);
		ccf.setSoTimeout(10000);
		ccf.start();
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(ccf);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(ccf);
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		Message<?> mOut = channel.receive(10000);
		assertNotNull(mOut);
		assertEquals("Reply1", new String((byte[]) mOut.getPayload()));
		mOut = channel.receive(10000);
		assertNotNull(mOut);
		assertEquals("Reply2", new String((byte[]) mOut.getPayload()));
		done.set(true);
	}

	@Test
	public void newTestNetSerial() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean done = new AtomicBoolean();
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			public void run() {
				try {
					ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
					latch.countDown();
					Socket socket = server.accept();
					int i = 0;
					while (true) {
						ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
						ois.readObject();
						ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
						oos.writeObject("Reply" + (++i));
					}
				} catch (Exception e) {
					if (!done.get()) {
						e.printStackTrace();
					}
				}
			}
		});
		AbstractConnectionFactory ccf = new TcpNetClientConnectionFactory("localhost", port);
		JavaStreamingConverter converter = new JavaStreamingConverter();
		ccf.setInputConverter(converter);
		ccf.setOutputConverter(converter);
		ccf.setSoTimeout(10000);
		ccf.start();
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(ccf);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(ccf);
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		assertTrue(latch.await(10, TimeUnit.SECONDS));		
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		Message<?> mOut = channel.receive(10000);
		assertNotNull(mOut);
		assertEquals("Reply1", mOut.getPayload());
		mOut = channel.receive(10000);
		assertNotNull(mOut);
		assertEquals("Reply2", mOut.getPayload());
		done.set(true);
	}

	@Test
	public void newTestNioSerial() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean done = new AtomicBoolean();
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			public void run() {
				try {
					ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
					latch.countDown();
					Socket socket = server.accept();
					int i = 0;
					while (true) {
						ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
						ois.readObject();
						ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
						oos.writeObject("Reply" + (++i));
					}
				} catch (Exception e) {
					if (!done.get()) {
						e.printStackTrace();
					}
				}
			}
		});
		AbstractConnectionFactory ccf = new TcpNioClientConnectionFactory("localhost", port);
		JavaStreamingConverter converter = new JavaStreamingConverter();
		ccf.setInputConverter(converter);
		ccf.setOutputConverter(converter);
		ccf.setSoTimeout(10000);
		ccf.start();
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(ccf);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(ccf);
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		Message<?> mOut = channel.receive(10000);
		assertNotNull(mOut);
		assertEquals("Reply1", mOut.getPayload());
		mOut = channel.receive(10000);
		assertNotNull(mOut);
		assertEquals("Reply2", mOut.getPayload());
		done.set(true);
	}

	@Test
	public void newTestNetSingleUseNoInbound() throws Exception  {
		final int port = SocketUtils.findAvailableServerSocket();
		final CountDownLatch latch = new CountDownLatch(1);
		final Semaphore semaphore = new Semaphore(0);
		final AtomicBoolean done = new AtomicBoolean();
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			public void run() {
				try {
					ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
					latch.countDown();
					while (true) {
						Socket socket = server.accept();
						semaphore.release();
						byte[] b = new byte[6];
						readFully(socket.getInputStream(), b);
						semaphore.release();
					}
				} catch (Exception e) {
					if (!done.get()) {
						e.printStackTrace();
					}
				}
			}
		});
		AbstractConnectionFactory ccf = new TcpNetClientConnectionFactory("localhost", port);
		ByteArrayCrLfConverter converter = new ByteArrayCrLfConverter();
		ccf.setInputConverter(converter);
		ccf.setOutputConverter(converter);
		ccf.setSoTimeout(10000);
		ccf.start();
		ccf.setSingleUse(true);
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(ccf);
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		assertTrue(semaphore.tryAcquire(4, 10000, TimeUnit.MILLISECONDS));
		done.set(true);
	}

	@Test
	public void newTestNioSingleUseNoInbound() throws Exception  {
		final int port = SocketUtils.findAvailableServerSocket();
		final CountDownLatch latch = new CountDownLatch(1);
		final Semaphore semaphore = new Semaphore(0);
		final AtomicBoolean done = new AtomicBoolean();
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			public void run() {
				try {
					ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
					latch.countDown();
					while (true) {
						Socket socket = server.accept();
						semaphore.release();
						byte[] b = new byte[6];
						readFully(socket.getInputStream(), b);
						semaphore.release();
					}
				} catch (Exception e) {
					if (!done.get()) {
						e.printStackTrace();
					}
				}
			}
		});
		AbstractConnectionFactory ccf = new TcpNioClientConnectionFactory("localhost", port);
		ByteArrayCrLfConverter converter = new ByteArrayCrLfConverter();
		ccf.setInputConverter(converter);
		ccf.setOutputConverter(converter);
		ccf.setSoTimeout(10000);
		ccf.start();
		ccf.setSingleUse(true);
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(ccf);
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		assertTrue(semaphore.tryAcquire(4, 10000, TimeUnit.MILLISECONDS));
		done.set(true);
	}

	@Test
	public void newTestNetSingleUseWithInbound() throws Exception  {
		final int port = SocketUtils.findAvailableServerSocket();
		final CountDownLatch latch = new CountDownLatch(1);
		final Semaphore semaphore = new Semaphore(0);
		final AtomicBoolean done = new AtomicBoolean();
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			public void run() {
				try {
					ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
					latch.countDown();
					int i = 0;
					while (true) {
						Socket socket = server.accept();
						semaphore.release();
						byte[] b = new byte[6];
						readFully(socket.getInputStream(), b);
						b = ("Reply" + (++i) + "\r\n").getBytes();
						socket.getOutputStream().write(b);
					}
				} catch (Exception e) {
					if (!done.get()) {
						e.printStackTrace();
					}
				}
			}
		});
		AbstractConnectionFactory ccf = new TcpNetClientConnectionFactory("localhost", port);
		ByteArrayCrLfConverter converter = new ByteArrayCrLfConverter();
		ccf.setInputConverter(converter);
		ccf.setOutputConverter(converter);
		ccf.setSoTimeout(10000);
		ccf.start();
		ccf.setSingleUse(true);
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(ccf);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(ccf);
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		assertTrue(semaphore.tryAcquire(2, 10000, TimeUnit.MILLISECONDS));		
		Set<String> replies = new HashSet<String>();
		for (int i = 0; i < 2; i++) {
			Message<?> mOut = channel.receive(10000);
			assertNotNull(mOut);
			replies.add(new String((byte[])mOut.getPayload()));
		}
		assertTrue(replies.remove("Reply1"));
		assertTrue(replies.remove("Reply2"));
		done.set(true);
	}

	@Test
	public void newTestNioSingleUseWithInbound() throws Exception  {
		final int port = SocketUtils.findAvailableServerSocket();
		final CountDownLatch latch = new CountDownLatch(1);
		final Semaphore semaphore = new Semaphore(0);
		final AtomicBoolean done = new AtomicBoolean();
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			public void run() {
				try {
					ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
					latch.countDown();
					int i = 0;
					while (true) {
						Socket socket = server.accept();
						semaphore.release();
						byte[] b = new byte[6];
						readFully(socket.getInputStream(), b);
						b = ("Reply" + (++i) + "\r\n").getBytes();
						socket.getOutputStream().write(b);
					}
				} catch (Exception e) {
					if (!done.get()) {
						e.printStackTrace();
					}
				}
			}
		});
		AbstractConnectionFactory ccf = new TcpNioClientConnectionFactory("localhost", port);
		ByteArrayCrLfConverter converter = new ByteArrayCrLfConverter();
		ccf.setInputConverter(converter);
		ccf.setOutputConverter(converter);
		ccf.setSoTimeout(10000);
		ccf.start();
		ccf.setSingleUse(true);
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(ccf);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(ccf);
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		assertTrue(semaphore.tryAcquire(2, 10000, TimeUnit.MILLISECONDS));		
		Set<String> replies = new HashSet<String>();
		for (int i = 0; i < 2; i++) {
			Message<?> mOut = channel.receive(10000);
			assertNotNull(mOut);
			replies.add(new String((byte[])mOut.getPayload()));
		}
		assertTrue(replies.remove("Reply1"));
		assertTrue(replies.remove("Reply2"));
		done.set(true);
	}

	@Test
	public void newTestNioSingleUseWithInboundMany() throws Exception  {
		final int port = SocketUtils.findAvailableServerSocket();
		final CountDownLatch latch = new CountDownLatch(1);
		final Semaphore semaphore = new Semaphore(0);
		final AtomicBoolean done = new AtomicBoolean();
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			public void run() {
				try {
					ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
					latch.countDown();
					int i = 0;
					while (true) {
						Socket socket = server.accept();
						semaphore.release();
						byte[] b = new byte[8];
						readFully(socket.getInputStream(), b);
						b = ("Reply" + (i++) + "\r\n").getBytes();
						socket.getOutputStream().write(b);
					}
				} catch (Exception e) {
					if (!done.get()) {
						e.printStackTrace();
					}
				}
			}
		});
		AbstractConnectionFactory ccf = new TcpNioClientConnectionFactory("localhost", port);
		ByteArrayCrLfConverter converter = new ByteArrayCrLfConverter();
		ccf.setInputConverter(converter);
		ccf.setOutputConverter(converter);
		ccf.setSoTimeout(10000);
		ccf.start();
		ccf.setSingleUse(true);
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(ccf);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(ccf);
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		for (int i = 100; i < 200; i++) {
			handler.handleMessage(MessageBuilder.withPayload("Test" + i).build());
		}
		assertTrue(semaphore.tryAcquire(10, 10000, TimeUnit.MILLISECONDS));
		Set<String> replies = new HashSet<String>();
		for (int i = 100; i < 200; i++) {
			Message<?> mOut = channel.receive(10000);
			assertNotNull(mOut);
			replies.add(new String((byte[])mOut.getPayload()));
		}
		for (int i = 0; i < 100; i++) {
			assertTrue("Reply" + i + " missing", replies.remove("Reply" + i));
		}
		done.set(true);
	}
}
