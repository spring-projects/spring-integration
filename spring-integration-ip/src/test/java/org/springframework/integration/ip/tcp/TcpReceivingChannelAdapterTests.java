/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.ip.tcp;

import static org.assertj.core.api.Assertions.assertThat;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.serializer.DefaultDeserializer;
import org.springframework.core.serializer.DefaultSerializer;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
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
import org.springframework.messaging.Message;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @author Gary Russell
 * @author Artem Bilan
 */
public class TcpReceivingChannelAdapterTests extends AbstractTcpChannelAdapterTests {

	@Test
	public void testNet() throws Exception {
		AbstractServerConnectionFactory scf = new TcpNetServerConnectionFactory(0);
		noopPublisher(scf);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		scf.setSerializer(serializer);
		scf.setDeserializer(serializer);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(scf);
		scf.start();
		TestingUtilities.waitListening(scf, null);
		int port = scf.getPort();
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		adapter.setBeanFactory(mock(BeanFactory.class));
		adapter.afterPropertiesSet();
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.getOutputStream().write("Test1\r\n".getBytes());
		socket.getOutputStream().write("Test2\r\n".getBytes());
		Message<?> message = channel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(new String((byte[]) message.getPayload())).isEqualTo("Test1");
		message = channel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(new String((byte[]) message.getPayload())).isEqualTo("Test2");
		scf.stop();
	}

	@Test
	public void testNetClientMode() throws Exception {
		final AtomicReference<ServerSocket> serverSocket = new AtomicReference<>();
		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);
		final AtomicBoolean done = new AtomicBoolean();
		new SimpleAsyncTaskExecutor("testNetClientMode-").execute(() -> {
			try {
				ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0, 10);
				serverSocket.set(server);
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
		});
		assertThat(latch1.await(10, TimeUnit.SECONDS)).isTrue();
		AbstractClientConnectionFactory ccf = new TcpNetClientConnectionFactory("localhost",
				serverSocket.get().getLocalPort());
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
		adapter.setRetryInterval(10000);
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.setPoolSize(1);
		taskScheduler.initialize();
		adapter.setTaskScheduler(taskScheduler);
		adapter.start();
		Message<?> message = channel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(new String((byte[]) message.getPayload())).isEqualTo("Test1");
		message = channel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(new String((byte[]) message.getPayload())).isEqualTo("Test2");
		adapter.stop();
		adapter.start();
		adapter.stop();
		latch2.countDown();
		ccf.stop();
		serverSocket.get().close();
	}

	@Test
	public void testNio() throws Exception {
		TcpNioServerConnectionFactory scf = new TcpNioServerConnectionFactory(0);
		noopPublisher(scf);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		scf.setSerializer(serializer);
		scf.setDeserializer(serializer);
		scf.setSoTimeout(5000);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(scf);
		scf.start();
		TestingUtilities.waitListening(scf, null);
		int port = scf.getPort();
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		for (int i = 0; i < 1000; i++) {
			socket.getOutputStream().write(("Test" + i + "\r\n").getBytes());
		}
		Set<String> results = new HashSet<String>();
		for (int i = 0; i < 1000; i++) {
			Message<?> message = channel.receive(10000);
			assertThat(message).isNotNull();
			results.add(new String((byte[]) message.getPayload()));
		}
		for (int i = 0; i < 1000; i++) {
			assertThat(results.remove("Test" + i)).isTrue();
		}
		scf.stop();
	}

	@Test
	public void testNetShared() throws Exception {
		AbstractServerConnectionFactory scf = new TcpNetServerConnectionFactory(0);
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
		int port = scf.getPort();
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.setSoTimeout(2000);
		socket.getOutputStream().write("Test\r\n".getBytes());
		socket.getOutputStream().write("Test\r\n".getBytes());
		Message<?> message = channel.receive(10000);
		assertThat(message).isNotNull();
		handler.handleMessage(message);
		message = channel.receive(10000);
		assertThat(message).isNotNull();
		handler.handleMessage(message);
		byte[] b = new byte[6];
		readFully(socket.getInputStream(), b);
		assertThat(new String(b)).isEqualTo("Test\r\n");
		readFully(socket.getInputStream(), b);
		assertThat(new String(b)).isEqualTo("Test\r\n");
		scf.stop();
	}

	@Test
	public void testNioShared() throws Exception {
		TcpNioServerConnectionFactory scf = new TcpNioServerConnectionFactory(0);
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
		int port = scf.getPort();
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.setSoTimeout(2000);
		socket.getOutputStream().write("Test\r\n".getBytes());
		socket.getOutputStream().write("Test\r\n".getBytes());
		Message<?> message = channel.receive(10000);
		assertThat(message).isNotNull();
		handler.handleMessage(message);
		message = channel.receive(10000);
		assertThat(message).isNotNull();
		handler.handleMessage(message);
		byte[] b = new byte[6];
		readFully(socket.getInputStream(), b);
		assertThat(new String(b)).isEqualTo("Test\r\n");
		readFully(socket.getInputStream(), b);
		assertThat(new String(b)).isEqualTo("Test\r\n");
		scf.stop();
	}

	@Test
	public void testNetSingleNoOutbound() throws Exception {
		AbstractServerConnectionFactory scf = new TcpNetServerConnectionFactory(0);
		noopPublisher(scf);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		scf.setSerializer(serializer);
		scf.setDeserializer(serializer);
		scf.setSingleUse(true);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(scf);
		scf.start();
		TestingUtilities.waitListening(scf, null);
		int port = scf.getPort();
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.getOutputStream().write("Test1\r\n".getBytes());
		socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.getOutputStream().write("Test2\r\n".getBytes());
		Message<?> message = channel.receive(10000);
		assertThat(message).isNotNull();
		// with single use, results may come back in a different order
		Set<String> results = new HashSet<String>();
		results.add(new String((byte[]) message.getPayload()));
		message = channel.receive(10000);
		assertThat(message).isNotNull();
		results.add(new String((byte[]) message.getPayload()));
		assertThat(results.contains("Test1")).isTrue();
		assertThat(results.contains("Test2")).isTrue();
		scf.stop();
	}

	@Test
	public void testNioSingleNoOutbound() throws Exception {
		TcpNioServerConnectionFactory scf = new TcpNioServerConnectionFactory(0);
		noopPublisher(scf);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		scf.setSerializer(serializer);
		scf.setDeserializer(serializer);
		scf.setSingleUse(true);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(scf);
		scf.start();
		TestingUtilities.waitListening(scf, null);
		int port = scf.getPort();
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.getOutputStream().write("Test1\r\n".getBytes());
		socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.getOutputStream().write("Test2\r\n".getBytes());
		Message<?> message = channel.receive(60000);
		assertThat(message).isNotNull();
		// with single use, results may come back in a different order
		Set<String> results = new HashSet<String>();
		results.add(new String((byte[]) message.getPayload()));
		message = channel.receive(10000);
		assertThat(message).isNotNull();
		results.add(new String((byte[]) message.getPayload()));
		assertThat(results.contains("Test1")).isTrue();
		assertThat(results.contains("Test2")).isTrue();
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
		AbstractServerConnectionFactory scf = new TcpNetServerConnectionFactory(0);
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
		int port = scf.getPort();
		Socket socket1 = SocketFactory.getDefault().createSocket("localhost", port);
		socket1.setSoTimeout(2000);
		socket1.getOutputStream().write("Test1\r\n".getBytes());
		Socket socket2 = SocketFactory.getDefault().createSocket("localhost", port);
		socket2.setSoTimeout(2000);
		socket2.getOutputStream().write("Test2\r\n".getBytes());
		Message<?> message = channel.receive(10000);
		assertThat(message).isNotNull();
		handler.handleMessage(message);
		message = channel.receive(10000);
		assertThat(message).isNotNull();
		handler.handleMessage(message);
		byte[] b = new byte[7];
		readFully(socket1.getInputStream(), b);
		assertThat(new String(b)).isEqualTo("Test1\r\n");
		readFully(socket2.getInputStream(), b);
		assertThat(new String(b)).isEqualTo("Test2\r\n");
		scf.stop();
	}

	@Test
	public void testNioSingleShared() throws Exception {
		TcpNioServerConnectionFactory scf = new TcpNioServerConnectionFactory(0);
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
		int port = scf.getPort();
		Socket socket1 = SocketFactory.getDefault().createSocket("localhost", port);
		socket1.setSoTimeout(2000);
		socket1.getOutputStream().write("Test1\r\n".getBytes());
		Socket socket2 = SocketFactory.getDefault().createSocket("localhost", port);
		socket2.setSoTimeout(2000);
		socket2.getOutputStream().write("Test2\r\n".getBytes());
		Message<?> message = channel.receive(10000);
		assertThat(message).isNotNull();
		handler.handleMessage(message);
		message = channel.receive(10000);
		assertThat(message).isNotNull();
		handler.handleMessage(message);
		byte[] b = new byte[7];
		readFully(socket1.getInputStream(), b);
		assertThat(new String(b)).isEqualTo("Test1\r\n");
		readFully(socket2.getInputStream(), b);
		assertThat(new String(b)).isEqualTo("Test2\r\n");
		scf.stop();
	}

	@Test
	public void testNioSingleSharedMany() throws Exception {
		TcpNioServerConnectionFactory scf = new TcpNioServerConnectionFactory(0);
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
		Executor te = new SimpleAsyncTaskExecutor("testNioSingleSharedMany-");
		scf.setTaskExecutor(te);
		scf.start();
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		TestingUtilities.waitListening(scf, null);
		int port = scf.getPort();
		List<Socket> sockets = new LinkedList<Socket>();
		for (int i = 100; i < 200; i++) {
			Socket socket1 = SocketFactory.getDefault().createSocket("localhost", port);
			socket1.setSoTimeout(2000);
			socket1.getOutputStream().write(("Test" + i + "\r\n").getBytes());
			sockets.add(socket1);
		}
		for (int i = 100; i < 200; i++) {
			Message<?> message = channel.receive(60000);
			assertThat(message).isNotNull();
			handler.handleMessage(message);
		}
		byte[] b = new byte[9];
		for (int i = 100; i < 200; i++) {
			readFully(sockets.remove(0).getInputStream(), b);
			assertThat(new String(b)).isEqualTo("Test" + i + "\r\n");
		}
		scf.stop();
	}

	@Test
	public void testNetInterceptors() throws Exception {
		AbstractServerConnectionFactory scf = new TcpNetServerConnectionFactory(0);
		noopPublisher(scf);
		interceptorsGuts(scf);
		scf.stop();
	}

	@Test
	public void testNetSingleNoOutboundInterceptors() throws Exception {
		AbstractServerConnectionFactory scf = new TcpNetServerConnectionFactory(0);
		noopPublisher(scf);
		singleNoOutboundInterceptorsGuts(scf);
		scf.stop();
	}

	@Test
	public void testNetSingleSharedInterceptors() throws Exception {
		AbstractServerConnectionFactory scf = new TcpNetServerConnectionFactory(0);
		noopPublisher(scf);
		singleSharedInterceptorsGuts(scf);
		scf.stop();
	}

	@Test
	public void testNioInterceptors() throws Exception {
		AbstractServerConnectionFactory scf = new TcpNioServerConnectionFactory(0);
		noopPublisher(scf);
		interceptorsGuts(scf);
		scf.stop();
	}

	@Test
	public void testNioSingleNoOutboundInterceptors() throws Exception {
		AbstractServerConnectionFactory scf = new TcpNioServerConnectionFactory(0);
		noopPublisher(scf);
		singleNoOutboundInterceptorsGuts(scf);
		scf.stop();
	}

	@Test
	public void testNioSingleSharedInterceptors() throws Exception {
		AbstractServerConnectionFactory scf = new TcpNioServerConnectionFactory(0);
		noopPublisher(scf);
		singleSharedInterceptorsGuts(scf);
		scf.stop();
	}

	private void interceptorsGuts(AbstractServerConnectionFactory scf) throws Exception {
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
		int port = scf.getPort();
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.setSoTimeout(10000);
		new ObjectOutputStream(socket.getOutputStream()).writeObject("Hello");
		assertThat(new ObjectInputStream(socket.getInputStream()).readObject()).isEqualTo("world!");
		new ObjectOutputStream(socket.getOutputStream()).writeObject("Hello");
		assertThat(new ObjectInputStream(socket.getInputStream()).readObject()).isEqualTo("world!");
		new ObjectOutputStream(socket.getOutputStream()).writeObject("Test1");
		new ObjectOutputStream(socket.getOutputStream()).writeObject("Test2");
		Set<String> results = new HashSet<String>();
		Message<?> message = channel.receive(10000);
		assertThat(message).isNotNull();
		results.add((String) message.getPayload());
		message = channel.receive(10000);
		assertThat(message).isNotNull();
		results.add((String) message.getPayload());
		assertThat(results.contains("Test1")).isTrue();
		assertThat(results.contains("Test2")).isTrue();
	}

	private void singleNoOutboundInterceptorsGuts(AbstractServerConnectionFactory scf) throws Exception {
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
		int port = scf.getPort();
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.setSoTimeout(10000);
		new ObjectOutputStream(socket.getOutputStream()).writeObject("Hello");
		assertThat(new ObjectInputStream(socket.getInputStream()).readObject()).isEqualTo("world!");
		new ObjectOutputStream(socket.getOutputStream()).writeObject("Hello");
		assertThat(new ObjectInputStream(socket.getInputStream()).readObject()).isEqualTo("world!");
		new ObjectOutputStream(socket.getOutputStream()).writeObject("Test1");

		socket = SocketFactory.getDefault().createSocket("localhost", port);
		new ObjectOutputStream(socket.getOutputStream()).writeObject("Hello");
		assertThat(new ObjectInputStream(socket.getInputStream()).readObject()).isEqualTo("world!");
		new ObjectOutputStream(socket.getOutputStream()).writeObject("Hello");
		assertThat(new ObjectInputStream(socket.getInputStream()).readObject()).isEqualTo("world!");
		new ObjectOutputStream(socket.getOutputStream()).writeObject("Test2");
		Message<?> message = channel.receive(10000);
		assertThat(message).isNotNull();
		// with single use, results may come back in a different order
		Set<Object> results = new HashSet<Object>();
		results.add(message.getPayload());
		message = channel.receive(10000);
		assertThat(message).isNotNull();
		results.add(message.getPayload());
		assertThat(results.contains("Test1")).isTrue();
		assertThat(results.contains("Test2")).isTrue();
	}

	private void singleSharedInterceptorsGuts(AbstractServerConnectionFactory scf) throws Exception {
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
		int port = scf.getPort();
		Socket socket1 = SocketFactory.getDefault().createSocket("localhost", port);
		socket1.setSoTimeout(60000);
		new ObjectOutputStream(socket1.getOutputStream()).writeObject("Hello");
		assertThat(new ObjectInputStream(socket1.getInputStream()).readObject()).isEqualTo("world!");
		new ObjectOutputStream(socket1.getOutputStream()).writeObject("Hello");
		assertThat(new ObjectInputStream(socket1.getInputStream()).readObject()).isEqualTo("world!");
		new ObjectOutputStream(socket1.getOutputStream()).writeObject("Test1");

		Socket socket2 = SocketFactory.getDefault().createSocket("localhost", port);
		socket2.setSoTimeout(60000);
		new ObjectOutputStream(socket2.getOutputStream()).writeObject("Hello");
		assertThat(new ObjectInputStream(socket2.getInputStream()).readObject()).isEqualTo("world!");
		new ObjectOutputStream(socket2.getOutputStream()).writeObject("Hello");
		assertThat(new ObjectInputStream(socket2.getInputStream()).readObject()).isEqualTo("world!");
		new ObjectOutputStream(socket2.getOutputStream()).writeObject("Test2");

		Message<?> message = channel.receive(10000);
		assertThat(message).isNotNull();
		handler.handleMessage(message);
		message = channel.receive(10000);
		assertThat(message).isNotNull();
		handler.handleMessage(message);

		assertThat(new ObjectInputStream(socket1.getInputStream()).readObject()).isEqualTo("Test1");
		assertThat(new ObjectInputStream(socket2.getInputStream()).readObject()).isEqualTo("Test2");
	}

	@Test
	public void testException() throws Exception {
		AbstractServerConnectionFactory scf = new TcpNetServerConnectionFactory(0);
		noopPublisher(scf);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		scf.setSerializer(serializer);
		scf.setDeserializer(serializer);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(scf);
		scf.start();
		TestingUtilities.waitListening(scf, null);
		int port = scf.getPort();
		SubscribableChannel channel = new DirectChannel();
		adapter.setOutputChannel(channel);
		ServiceActivatingHandler handler = new ServiceActivatingHandler(new FailingService());
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		channel.subscribe(handler);
		QueueChannel errorChannel = new QueueChannel();
		adapter.setErrorChannel(errorChannel);
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.getOutputStream().write("Test1\r\n".getBytes());
		socket.getOutputStream().write("Test2\r\n".getBytes());
		Message<?> message = errorChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(((Exception) message.getPayload()).getCause().getMessage()).isEqualTo("Failed");
		message = errorChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(((Exception) message.getPayload()).getCause().getMessage()).isEqualTo("Failed");
		scf.stop();
	}

	private class FailingService {

		@SuppressWarnings("unused")
		public String serviceMethod(byte[] bytes) {
			throw new RuntimeException("Failed");
		}
	}


}
