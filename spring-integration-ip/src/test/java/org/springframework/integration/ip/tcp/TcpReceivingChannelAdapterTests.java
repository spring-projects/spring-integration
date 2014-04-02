/*
 * Copyright 2002-2014 the original author or authors.
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
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.serializer.DefaultDeserializer;
import org.springframework.core.serializer.DefaultSerializer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpConnectionInterceptorFactory;
import org.springframework.integration.ip.tcp.connection.TcpConnectionInterceptorFactoryChain;
import org.springframework.integration.ip.tcp.connection.TcpNetClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNetServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNioServerConnectionFactory;
import org.springframework.integration.ip.tcp.serializer.ByteArrayCrLfSerializer;
import org.springframework.integration.ip.util.TestingUtilities;
import org.springframework.integration.test.util.SocketUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @author Gary Russell
 */
public class TcpReceivingChannelAdapterTests extends AbstractTcpChannelAdapterTests {

	@Test
	public void testNet() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		AbstractServerConnectionFactory scf = new TcpNetServerConnectionFactory(port);
		noopPublisher(scf);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		scf.setSerializer(serializer);
		scf.setDeserializer(serializer);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(scf);
		scf.start();
		TestingUtilities.waitListening(scf, null);
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		adapter.setBeanFactory(mock(BeanFactory.class));
		adapter.afterPropertiesSet();
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.getOutputStream().write("Test1\r\n".getBytes());
		socket.getOutputStream().write("Test2\r\n".getBytes());
		Message<?> message = channel.receive(10000);
		assertNotNull(message);
		assertEquals("Test1", new String((byte[]) message.getPayload()));
		message = channel.receive(10000);
		assertNotNull(message);
		assertEquals("Test2", new String((byte[]) message.getPayload()));
		scf.stop();
	}

	@Test
	public void testNetClientMode() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);
		final AtomicBoolean done = new AtomicBoolean();
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			@Override
			public void run() {
				try {
					ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port, 10);
					latch1.countDown();
					Socket socket = server.accept();
					socket.getOutputStream().write("Test1\r\nTest2\r\n".getBytes());
					latch2.await();
					socket.close();
					server.close();
				}
				catch (Exception e) {
					if (!done.get()) {
						e.printStackTrace();
					}
				}
			}
		});
		AbstractClientConnectionFactory ccf = new TcpNetClientConnectionFactory("localhost", port);
		noopPublisher(ccf);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		ccf.setSerializer(serializer);
		ccf.setDeserializer(serializer);
		ccf.setSoTimeout(Integer.MAX_VALUE);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(ccf);
		adapter.setClientMode(true);
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		adapter.setBeanFactory(mock(BeanFactory.class));
		adapter.afterPropertiesSet();
		assertTrue(latch1.await(10, TimeUnit.SECONDS));
		adapter.setRetryInterval(10000);
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.setPoolSize(1);
		taskScheduler.initialize();
		adapter.setTaskScheduler(taskScheduler);
		adapter.start();
		Message<?> message = channel.receive(10000);
		assertNotNull(message);
		assertEquals("Test1", new String((byte[]) message.getPayload()));
		message = channel.receive(10000);
		assertNotNull(message);
		assertEquals("Test2", new String((byte[]) message.getPayload()));
		adapter.stop();
		adapter.start();
		adapter.stop();
		latch2.countDown();
		ccf.stop();
	}

	@Test
	public void testNio() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		TcpNioServerConnectionFactory scf = new TcpNioServerConnectionFactory(port);
		noopPublisher(scf);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		scf.setSerializer(serializer);
		scf.setDeserializer(serializer);
		scf.setSoTimeout(5000);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(scf);
		scf.start();
		TestingUtilities.waitListening(scf, null);
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		for (int i = 0; i < 1000; i++) {
			socket.getOutputStream().write(("Test" + i + "\r\n").getBytes());
		}
		Set<String> results = new HashSet<String>();
		for (int i = 0; i < 1000; i++) {
			Message<?> message = channel.receive(10000);
			assertNotNull(message);
			results.add(new String((byte[]) message.getPayload()));
		}
		for (int i = 0; i < 1000; i++) {
			assertTrue(results.remove("Test" + i));
		}
		scf.stop();
	}

	@Test
	public void testNetShared() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		AbstractServerConnectionFactory scf = new TcpNetServerConnectionFactory(port);
		noopPublisher(scf);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		scf.setSerializer(serializer);
		scf.setDeserializer(serializer);
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(scf);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(scf);
		scf.start();
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		TestingUtilities.waitListening(scf, null);
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.setSoTimeout(2000);
		socket.getOutputStream().write("Test\r\n".getBytes());
		socket.getOutputStream().write("Test\r\n".getBytes());
		Message<?> message = channel.receive(10000);
		assertNotNull(message);
		handler.handleMessage(message);
		message = channel.receive(10000);
		assertNotNull(message);
		handler.handleMessage(message);
		byte[] b = new byte[6];
		readFully(socket.getInputStream(), b);
		assertEquals("Test\r\n", new String(b));
		readFully(socket.getInputStream(), b);
		assertEquals("Test\r\n", new String(b));
		scf.stop();
	}

	@Test
	public void testNioShared() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		TcpNioServerConnectionFactory scf = new TcpNioServerConnectionFactory(port);
		noopPublisher(scf);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		scf.setSerializer(serializer);
		scf.setDeserializer(serializer);
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(scf);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(scf);
		scf.start();
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		TestingUtilities.waitListening(scf, null);
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.setSoTimeout(2000);
		socket.getOutputStream().write("Test\r\n".getBytes());
		socket.getOutputStream().write("Test\r\n".getBytes());
		Message<?> message = channel.receive(10000);
		assertNotNull(message);
		handler.handleMessage(message);
		message = channel.receive(10000);
		assertNotNull(message);
		handler.handleMessage(message);
		byte[] b = new byte[6];
		readFully(socket.getInputStream(), b);
		assertEquals("Test\r\n", new String(b));
		readFully(socket.getInputStream(), b);
		assertEquals("Test\r\n", new String(b));
		scf.stop();
	}

	@Test
	public void testNetSingleNoOutbound() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		AbstractServerConnectionFactory scf = new TcpNetServerConnectionFactory(port);
		noopPublisher(scf);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		scf.setSerializer(serializer);
		scf.setDeserializer(serializer);
		scf.setSingleUse(true);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(scf);
		scf.start();
		TestingUtilities.waitListening(scf, null);
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.getOutputStream().write("Test1\r\n".getBytes());
		socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.getOutputStream().write("Test2\r\n".getBytes());
		Message<?> message = channel.receive(10000);
		assertNotNull(message);
		// with single use, results may come back in a different order
		Set<String> results = new HashSet<String>();
		results.add(new String((byte[]) message.getPayload()));
		message = channel.receive(10000);
		assertNotNull(message);
		results.add(new String((byte[]) message.getPayload()));
		assertTrue(results.contains("Test1"));
		assertTrue(results.contains("Test2"));
		scf.stop();
	}

	@Test
	public void testNioSingleNoOutbound() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		TcpNioServerConnectionFactory scf = new TcpNioServerConnectionFactory(port);
		noopPublisher(scf);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		scf.setSerializer(serializer);
		scf.setDeserializer(serializer);
		scf.setSingleUse(true);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(scf);
		scf.start();
		TestingUtilities.waitListening(scf, null);
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.getOutputStream().write("Test1\r\n".getBytes());
		socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.getOutputStream().write("Test2\r\n".getBytes());
		Message<?> message = channel.receive(60000);
		assertNotNull(message);
		// with single use, results may come back in a different order
		Set<String> results = new HashSet<String>();
		results.add(new String((byte[]) message.getPayload()));
		message = channel.receive(10000);
		assertNotNull(message);
		results.add(new String((byte[]) message.getPayload()));
		assertTrue(results.contains("Test1"));
		assertTrue(results.contains("Test2"));
		scf.stop();
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

	@Test
	public void testNetSingleShared() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		AbstractServerConnectionFactory scf = new TcpNetServerConnectionFactory(port);
		noopPublisher(scf);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		scf.setSerializer(serializer);
		scf.setDeserializer(serializer);
		scf.setSingleUse(true);
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(scf);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(scf);
		scf.start();
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		TestingUtilities.waitListening(scf, null);
		Socket socket1 = SocketFactory.getDefault().createSocket("localhost", port);
		socket1.setSoTimeout(2000);
		socket1.getOutputStream().write("Test1\r\n".getBytes());
		Socket socket2 = SocketFactory.getDefault().createSocket("localhost", port);
		socket2.setSoTimeout(2000);
		socket2.getOutputStream().write("Test2\r\n".getBytes());
		Message<?> message = channel.receive(10000);
		assertNotNull(message);
		handler.handleMessage(message);
		message = channel.receive(10000);
		assertNotNull(message);
		handler.handleMessage(message);
		byte[] b = new byte[7];
		readFully(socket1.getInputStream(), b);
		assertEquals("Test1\r\n", new String(b));
		readFully(socket2.getInputStream(), b);
		assertEquals("Test2\r\n", new String(b));
		scf.stop();
	}

	@Test
	public void testNioSingleShared() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		TcpNioServerConnectionFactory scf = new TcpNioServerConnectionFactory(port);
		noopPublisher(scf);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		scf.setSerializer(serializer);
		scf.setDeserializer(serializer);
		scf.setSingleUse(true);
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(scf);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(scf);
		scf.start();
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		TestingUtilities.waitListening(scf, null);
		Socket socket1 = SocketFactory.getDefault().createSocket("localhost", port);
		socket1.setSoTimeout(2000);
		socket1.getOutputStream().write("Test1\r\n".getBytes());
		Socket socket2 = SocketFactory.getDefault().createSocket("localhost", port);
		socket2.setSoTimeout(2000);
		socket2.getOutputStream().write("Test2\r\n".getBytes());
		Message<?> message = channel.receive(10000);
		assertNotNull(message);
		handler.handleMessage(message);
		message = channel.receive(10000);
		assertNotNull(message);
		handler.handleMessage(message);
		byte[] b = new byte[7];
		readFully(socket1.getInputStream(), b);
		assertEquals("Test1\r\n", new String(b));
		readFully(socket2.getInputStream(), b);
		assertEquals("Test2\r\n", new String(b));
		scf.stop();
	}

	@Test
	public void testNioSingleSharedMany() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		TcpNioServerConnectionFactory scf = new TcpNioServerConnectionFactory(port);
		noopPublisher(scf);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		scf.setSerializer(serializer);
		scf.setDeserializer(serializer);
		scf.setSingleUse(true);
		scf.setBacklog(100);
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(scf);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(scf);
		Executor te = Executors.newCachedThreadPool();
		scf.setTaskExecutor(te);
		scf.start();
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		TestingUtilities.waitListening(scf, null);
		List<Socket> sockets = new LinkedList<Socket>();
		for (int i = 100; i < 200; i++) {
			Socket socket1 = SocketFactory.getDefault().createSocket("localhost", port);
			socket1.setSoTimeout(2000);
			socket1.getOutputStream().write(("Test" + i + "\r\n").getBytes());
			sockets.add(socket1);
		}
		for (int i = 100; i < 200; i++) {
			Message<?> message = channel.receive(60000);
			assertNotNull(message);
			handler.handleMessage(message);
		}
		byte[] b = new byte[9];
		for (int i = 100; i < 200; i++) {
			readFully(sockets.remove(0).getInputStream(), b);
			assertEquals("Test" + i + "\r\n", new String(b));
		}
		scf.stop();
	}

	@Test
	public void testNetInterceptors() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		AbstractServerConnectionFactory scf = new TcpNetServerConnectionFactory(port);
		noopPublisher(scf);
		interceptorsGuts(port, scf);
		scf.stop();
	}

	@Test
	public void testNetSingleNoOutboundInterceptors() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		AbstractServerConnectionFactory scf = new TcpNetServerConnectionFactory(port);
		noopPublisher(scf);
		singleNoOutboundInterceptorsGuts(port, scf);
		scf.stop();
	}

	@Test
	public void testNetSingleSharedInterceptors() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		AbstractServerConnectionFactory scf = new TcpNetServerConnectionFactory(port);
		noopPublisher(scf);
		singleSharedInterceptorsGuts(port, scf);
		scf.stop();
	}

	@Test
	public void testNioInterceptors() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		AbstractServerConnectionFactory scf = new TcpNioServerConnectionFactory(port);
		noopPublisher(scf);
		interceptorsGuts(port, scf);
		scf.stop();
	}

	@Test
	public void testNioSingleNoOutboundInterceptors() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		AbstractServerConnectionFactory scf = new TcpNioServerConnectionFactory(port);
		noopPublisher(scf);
		singleNoOutboundInterceptorsGuts(port, scf);
		scf.stop();
	}

	@Test
	public void testNioSingleSharedInterceptors() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		AbstractServerConnectionFactory scf = new TcpNioServerConnectionFactory(port);
		noopPublisher(scf);
		singleSharedInterceptorsGuts(port, scf);
		scf.stop();
	}

	private void interceptorsGuts(final int port, AbstractServerConnectionFactory scf) throws Exception {
		scf.setSerializer(new DefaultSerializer());
		scf.setDeserializer(new DefaultDeserializer());
		scf.setSingleUse(false);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(scf);
		TcpConnectionInterceptorFactoryChain fc = new TcpConnectionInterceptorFactoryChain();
		fc.setInterceptors(new TcpConnectionInterceptorFactory[] {
				newInterceptorFactory(),
				newInterceptorFactory()
		});
		scf.setInterceptorFactoryChain(fc);
		scf.setSoTimeout(10000);
		scf.start();
		TestingUtilities.waitListening(scf, null);
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.setSoTimeout(10000);
		new ObjectOutputStream(socket.getOutputStream()).writeObject("Hello");
		assertEquals("world!", new ObjectInputStream(socket.getInputStream()).readObject());
		new ObjectOutputStream(socket.getOutputStream()).writeObject("Hello");
		assertEquals("world!", new ObjectInputStream(socket.getInputStream()).readObject());
		new ObjectOutputStream(socket.getOutputStream()).writeObject("Test1");
		new ObjectOutputStream(socket.getOutputStream()).writeObject("Test2");
		Set<String> results = new HashSet<String>();
		Message<?> message = channel.receive(10000);
		assertNotNull(message);
		results.add((String) message.getPayload());
		message = channel.receive(10000);
		assertNotNull(message);
		results.add((String) message.getPayload());
		assertTrue(results.contains("Test1"));
		assertTrue(results.contains("Test2"));
	}

	private void singleNoOutboundInterceptorsGuts(final int port, AbstractServerConnectionFactory scf) throws Exception {
		scf.setSerializer(new DefaultSerializer());
		scf.setDeserializer(new DefaultDeserializer());
		scf.setSingleUse(true);
		scf.setSoTimeout(10000);
		TcpConnectionInterceptorFactoryChain fc = new TcpConnectionInterceptorFactoryChain();
		fc.setInterceptors(new TcpConnectionInterceptorFactory[] {
				newInterceptorFactory(),
				newInterceptorFactory()
		});
		scf.setInterceptorFactoryChain(fc);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(scf);
		scf.start();
		TestingUtilities.waitListening(scf, null);
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.setSoTimeout(10000);
		new ObjectOutputStream(socket.getOutputStream()).writeObject("Hello");
		assertEquals("world!", new ObjectInputStream(socket.getInputStream()).readObject());
		new ObjectOutputStream(socket.getOutputStream()).writeObject("Hello");
		assertEquals("world!", new ObjectInputStream(socket.getInputStream()).readObject());
		new ObjectOutputStream(socket.getOutputStream()).writeObject("Test1");

		socket = SocketFactory.getDefault().createSocket("localhost", port);
		new ObjectOutputStream(socket.getOutputStream()).writeObject("Hello");
		assertEquals("world!", new ObjectInputStream(socket.getInputStream()).readObject());
		new ObjectOutputStream(socket.getOutputStream()).writeObject("Hello");
		assertEquals("world!", new ObjectInputStream(socket.getInputStream()).readObject());
		new ObjectOutputStream(socket.getOutputStream()).writeObject("Test2");
		Message<?> message = channel.receive(10000);
		assertNotNull(message);
		// with single use, results may come back in a different order
		Set<Object> results = new HashSet<Object>();
		results.add(message.getPayload());
		message = channel.receive(10000);
		assertNotNull(message);
		results.add(message.getPayload());
		assertTrue(results.contains("Test1"));
		assertTrue(results.contains("Test2"));
	}

	private void singleSharedInterceptorsGuts(final int port, AbstractServerConnectionFactory scf) throws Exception {
		scf.setSerializer(new DefaultSerializer());
		scf.setDeserializer(new DefaultDeserializer());
		scf.setSingleUse(true);
		scf.setSoTimeout(60000);
		TcpConnectionInterceptorFactoryChain fc = new TcpConnectionInterceptorFactoryChain();
		fc.setInterceptors(new TcpConnectionInterceptorFactory[] {
				newInterceptorFactory(),
				newInterceptorFactory()
		});
		scf.setInterceptorFactoryChain(fc);
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(scf);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(scf);
		scf.start();
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		TestingUtilities.waitListening(scf, null);
		Socket socket1 = SocketFactory.getDefault().createSocket("localhost", port);
		socket1.setSoTimeout(60000);
		new ObjectOutputStream(socket1.getOutputStream()).writeObject("Hello");
		assertEquals("world!", new ObjectInputStream(socket1.getInputStream()).readObject());
		new ObjectOutputStream(socket1.getOutputStream()).writeObject("Hello");
		assertEquals("world!", new ObjectInputStream(socket1.getInputStream()).readObject());
		new ObjectOutputStream(socket1.getOutputStream()).writeObject("Test1");

		Socket socket2 = SocketFactory.getDefault().createSocket("localhost", port);
		socket2.setSoTimeout(60000);
		new ObjectOutputStream(socket2.getOutputStream()).writeObject("Hello");
		assertEquals("world!", new ObjectInputStream(socket2.getInputStream()).readObject());
		new ObjectOutputStream(socket2.getOutputStream()).writeObject("Hello");
		assertEquals("world!", new ObjectInputStream(socket2.getInputStream()).readObject());
		new ObjectOutputStream(socket2.getOutputStream()).writeObject("Test2");

		Message<?> message = channel.receive(10000);
		assertNotNull(message);
		handler.handleMessage(message);
		message = channel.receive(10000);
		assertNotNull(message);
		handler.handleMessage(message);

		assertEquals("Test1", new ObjectInputStream(socket1.getInputStream()).readObject());
		assertEquals("Test2", new ObjectInputStream(socket2.getInputStream()).readObject());
	}

	@Test
	public void testException() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		AbstractServerConnectionFactory scf = new TcpNetServerConnectionFactory(port);
		noopPublisher(scf);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		scf.setSerializer(serializer);
		scf.setDeserializer(serializer);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(scf);
		scf.start();
		TestingUtilities.waitListening(scf, null);
		SubscribableChannel channel = new DirectChannel();
		adapter.setOutputChannel(channel);
		ServiceActivatingHandler handler = new ServiceActivatingHandler(new FailingService());
		channel.subscribe(handler);
		QueueChannel errorChannel = new QueueChannel();
		adapter.setErrorChannel(errorChannel);
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.getOutputStream().write("Test1\r\n".getBytes());
		socket.getOutputStream().write("Test2\r\n".getBytes());
		Message<?> message = errorChannel.receive(10000);
		assertNotNull(message);
		assertEquals("Failed", ((Exception) message.getPayload()).getCause().getMessage());
		message = errorChannel.receive(10000);
		assertNotNull(message);
		assertEquals("Failed", ((Exception) message.getPayload()).getCause().getMessage());
		scf.stop();
	}

	private class FailingService {
		@SuppressWarnings("unused")
		public String serviceMethod(byte[] bytes) {
			throw new RuntimeException("Failed");
		}
	}


}
