/*
 * Copyright 2002-2024 the original author or authors.
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

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.ConsumerEndpointFactoryBean;
import org.springframework.integration.handler.BridgeHandler;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpConnectionSupport;
import org.springframework.integration.ip.tcp.connection.TcpNetClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNetServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNioClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNioServerConnectionFactory;
import org.springframework.integration.ip.tcp.serializer.ByteArrayRawSerializer;
import org.springframework.integration.ip.util.TestingUtilities;
import org.springframework.integration.test.condition.LogLevels;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class TcpInboundGatewayTests {

	@Test
	public void testNetSingle() throws Exception {
		AbstractServerConnectionFactory scf = new TcpNetServerConnectionFactory(0);
		scf.setSingleUse(true);
		TcpInboundGateway gateway = new TcpInboundGateway();
		gateway.setConnectionFactory(scf);
		gateway.setBeanFactory(mock(BeanFactory.class));
		scf.start();
		TestingUtilities.waitListening(scf, 20000L);
		int port = scf.getPort();
		final QueueChannel channel = new QueueChannel();
		gateway.setRequestChannel(channel);
		ServiceActivatingHandler handler = new ServiceActivatingHandler(new Service());
		handler.setChannelResolver(channelName -> channel);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		Socket socket1 = SocketFactory.getDefault().createSocket("localhost", port);
		socket1.getOutputStream().write("Test1\r\n".getBytes());
		Socket socket2 = SocketFactory.getDefault().createSocket("localhost", port);
		socket2.getOutputStream().write("Test2\r\n".getBytes());
		handler.handleMessage(channel.receive(10000));
		handler.handleMessage(channel.receive(10000));
		byte[] bytes = new byte[12];
		readFully(socket1.getInputStream(), bytes);
		assertThat(new String(bytes)).isEqualTo("Echo:Test1\r\n");
		readFully(socket2.getInputStream(), bytes);
		assertThat(new String(bytes)).isEqualTo("Echo:Test2\r\n");
		gateway.stop();
		scf.stop();
	}

	@Test
	public void testNetNotSingle() throws Exception {
		AbstractServerConnectionFactory scf = new TcpNetServerConnectionFactory(0);
		scf.setSingleUse(false);
		TcpInboundGateway gateway = new TcpInboundGateway();
		gateway.setConnectionFactory(scf);
		scf.start();
		TestingUtilities.waitListening(scf, 20000L);
		int port = scf.getPort();
		final QueueChannel channel = new QueueChannel();
		gateway.setRequestChannel(channel);
		gateway.setBeanFactory(mock(BeanFactory.class));
		ServiceActivatingHandler handler = new ServiceActivatingHandler(new Service());
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.getOutputStream().write("Test1\r\n".getBytes());
		socket.getOutputStream().write("Test2\r\n".getBytes());
		handler.handleMessage(channel.receive(10000));
		handler.handleMessage(channel.receive(10000));
		byte[] bytes = new byte[12];
		readFully(socket.getInputStream(), bytes);
		assertThat(new String(bytes)).isEqualTo("Echo:Test1\r\n");
		readFully(socket.getInputStream(), bytes);
		assertThat(new String(bytes)).isEqualTo("Echo:Test2\r\n");
		gateway.stop();
		scf.stop();
	}

	@Test
	public void testNetClientMode() throws Exception {
		final AtomicInteger port = new AtomicInteger();
		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);
		final CountDownLatch latch3 = new CountDownLatch(1);
		final AtomicBoolean done = new AtomicBoolean();

		ExecutorService executorService = Executors.newSingleThreadExecutor();
		executorService
				.execute(() -> {
					try {
						ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0, 10);
						port.set(server.getLocalPort());
						latch1.countDown();
						Socket socket = server.accept();
						socket.getOutputStream().write("Test1\r\nTest2\r\n".getBytes());
						byte[] bytes = new byte[12];
						readFully(socket.getInputStream(), bytes);
						assertThat(new String(bytes)).isEqualTo("Echo:Test1\r\n");
						readFully(socket.getInputStream(), bytes);
						assertThat(new String(bytes)).isEqualTo("Echo:Test2\r\n");
						latch2.await();
						socket.close();
						server.close();
						done.set(true);
						latch3.countDown();
					}
					catch (Exception e) {
						if (!done.get()) {
							e.printStackTrace();
						}
					}
				});
		assertThat(latch1.await(10, TimeUnit.SECONDS)).isTrue();
		AbstractClientConnectionFactory ccf = new TcpNetClientConnectionFactory("localhost", port.get());
		ccf.setSingleUse(false);
		TcpInboundGateway gateway = new TcpInboundGateway();
		gateway.setConnectionFactory(ccf);
		final QueueChannel channel = new QueueChannel();
		gateway.setRequestChannel(channel);
		gateway.setClientMode(true);
		gateway.setRetryInterval(10000);
		gateway.setBeanFactory(mock(BeanFactory.class));
		gateway.afterPropertiesSet();
		ServiceActivatingHandler handler = new ServiceActivatingHandler(new Service());
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.setPoolSize(1);
		taskScheduler.initialize();
		gateway.setTaskScheduler(taskScheduler);
		gateway.start();
		Message<?> message = channel.receive(10000);
		assertThat(message).isNotNull();
		handler.handleMessage(message);
		message = channel.receive(10000);
		assertThat(message).isNotNull();
		handler.handleMessage(message);
		latch2.countDown();
		assertThat(latch3.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(done.get()).isTrue();
		gateway.stop();
		executorService.shutdown();
		taskScheduler.destroy();
	}

	@Test
	public void testNioSingle() throws Exception {
		AbstractServerConnectionFactory scf = new TcpNioServerConnectionFactory(0);
		scf.setSingleUse(true);
		TcpInboundGateway gateway = new TcpInboundGateway();
		gateway.setConnectionFactory(scf);
		scf.start();
		TestingUtilities.waitListening(scf, 20000L);
		int port = scf.getPort();
		final QueueChannel channel = new QueueChannel();
		gateway.setRequestChannel(channel);
		gateway.setBeanFactory(mock(BeanFactory.class));
		ServiceActivatingHandler handler = new ServiceActivatingHandler(new Service());
		handler.setChannelResolver(channelName -> channel);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		Socket socket1 = SocketFactory.getDefault().createSocket("localhost", port);
		socket1.getOutputStream().write("Test1\r\n".getBytes());
		Socket socket2 = SocketFactory.getDefault().createSocket("localhost", port);
		socket2.getOutputStream().write("Test2\r\n".getBytes());
		handler.handleMessage(channel.receive(10000));
		handler.handleMessage(channel.receive(10000));
		byte[] bytes = new byte[12];
		readFully(socket1.getInputStream(), bytes);
		assertThat(new String(bytes)).isEqualTo("Echo:Test1\r\n");
		readFully(socket2.getInputStream(), bytes);
		assertThat(new String(bytes)).isEqualTo("Echo:Test2\r\n");
		gateway.stop();
		scf.stop();
	}

	@Test
	public void testNioNotSingle() throws Exception {
		AbstractServerConnectionFactory scf = new TcpNioServerConnectionFactory(0);
		scf.setSingleUse(false);
		TcpInboundGateway gateway = new TcpInboundGateway();
		gateway.setConnectionFactory(scf);
		scf.start();
		TestingUtilities.waitListening(scf, 20000L);
		int port = scf.getPort();
		final QueueChannel channel = new QueueChannel();
		gateway.setRequestChannel(channel);
		gateway.setBeanFactory(mock(BeanFactory.class));
		ServiceActivatingHandler handler = new ServiceActivatingHandler(new Service());
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.getOutputStream().write("Test1\r\n".getBytes());
		socket.getOutputStream().write("Test2\r\n".getBytes());
		handler.handleMessage(channel.receive(10000));
		handler.handleMessage(channel.receive(10000));
		Set<String> results = new HashSet<String>();
		byte[] bytes = new byte[12];
		readFully(socket.getInputStream(), bytes);
		results.add(new String(bytes));
		readFully(socket.getInputStream(), bytes);
		results.add(new String(bytes));
		assertThat(results.remove("Echo:Test1\r\n")).isTrue();
		assertThat(results.remove("Echo:Test2\r\n")).isTrue();
		gateway.stop();
		scf.stop();
	}

	@Test
	public void testErrorFlow() throws Exception {
		AbstractServerConnectionFactory scf = new TcpNetServerConnectionFactory(0);
		scf.setSingleUse(true);
		TcpInboundGateway gateway = new TcpInboundGateway();
		gateway.setConnectionFactory(scf);
		SubscribableChannel errorChannel = new DirectChannel();
		final String errorMessage = "An error occurred";
		errorChannel.subscribe(message -> {
			MessageChannel replyChannel = (MessageChannel) message.getHeaders().getReplyChannel();
			replyChannel.send(new GenericMessage<>(errorMessage));
		});
		gateway.setErrorChannel(errorChannel);
		scf.start();
		TestingUtilities.waitListening(scf, 20000L);
		int port = scf.getPort();
		final SubscribableChannel channel = new DirectChannel();
		gateway.setRequestChannel(channel);
		gateway.setBeanFactory(mock(BeanFactory.class));
		ServiceActivatingHandler handler = new ServiceActivatingHandler(new FailingService());
		channel.subscribe(handler);
		Socket socket1 = SocketFactory.getDefault().createSocket("localhost", port);
		socket1.getOutputStream().write("Test1\r\n".getBytes());
		Socket socket2 = SocketFactory.getDefault().createSocket("localhost", port);
		socket2.getOutputStream().write("Test2\r\n".getBytes());
		byte[] bytes = new byte[errorMessage.length() + 2];
		readFully(socket1.getInputStream(), bytes);
		assertThat(new String(bytes)).isEqualTo(errorMessage + "\r\n");
		readFully(socket2.getInputStream(), bytes);
		assertThat(new String(bytes)).isEqualTo(errorMessage + "\r\n");
		gateway.stop();
		scf.stop();
	}

	@Test
	@LogLevels(categories = "org.springframework.integration.ip", level = "DEBUG")
	public void testNetCloseStream() throws InterruptedException, IOException {
		testCloseStream(new TcpNetServerConnectionFactory(0),
				port -> new TcpNetClientConnectionFactory("localhost", port));
	}

	@Test
	public void testNioCloseStream() throws InterruptedException, IOException {
		testCloseStream(new TcpNioServerConnectionFactory(0),
				port -> new TcpNioClientConnectionFactory("localhost", port));
	}

	private void testCloseStream(AbstractServerConnectionFactory scf,
			Function<Integer, AbstractClientConnectionFactory> ccf) throws InterruptedException, IOException {

		scf.setSingleUse(true);
		scf.setDeserializer(new ByteArrayRawSerializer());
		TcpInboundGateway gateway = new TcpInboundGateway();
		gateway.setConnectionFactory(scf);
		BeanFactory bf = mock(ConfigurableBeanFactory.class);
		gateway.setBeanFactory(bf);
		gateway.start();
		TestingUtilities.waitListening(scf, 20000L);
		int port = scf.getPort();
		final DirectChannel channel = new DirectChannel();
		gateway.setRequestChannel(channel);
		BridgeHandler bridge = new BridgeHandler();
		bridge.setBeanFactory(bf);
		bridge.afterPropertiesSet();
		ConsumerEndpointFactoryBean consumer = new ConsumerEndpointFactoryBean();
		consumer.setInputChannel(channel);
		consumer.setBeanFactory(bf);
		consumer.setHandler(bridge);
		consumer.afterPropertiesSet();
		consumer.start();
		AbstractClientConnectionFactory client = ccf.apply(port);
		CountDownLatch latch = new CountDownLatch(1);
		client.registerListener(message -> {
			latch.countDown();
			return false;
		});
		client.afterPropertiesSet();
		client.start();
		TcpConnectionSupport connection = client.getConnection();
		connection.send(new GenericMessage<>("foo"));
		connection.shutdownOutput(); // signal EOF to server
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		gateway.stop();
		client.stop();
	}

	private void readFully(InputStream is, byte[] buff) throws IOException {
		for (int i = 0; i < buff.length; i++) {
			buff[i] = (byte) is.read();
		}
	}

	private class Service {

		@SuppressWarnings("unused")
		public String serviceMethod(byte[] bytes) {
			return "Echo:" + new String(bytes);
		}

	}

	private class FailingService {

		@SuppressWarnings("unused")
		public String serviceMethod(byte[] bytes) {
			throw new RuntimeException("Planned Failure For Tests");
		}

	}

}
