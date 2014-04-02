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
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNetClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNetServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNioServerConnectionFactory;
import org.springframework.integration.ip.util.TestingUtilities;
import org.springframework.integration.test.util.SocketUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

public class TcpInboundGatewayTests {

	@Test
	public void testNetSingle() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		AbstractServerConnectionFactory scf = new TcpNetServerConnectionFactory(port);
		scf.setSingleUse(true);
		TcpInboundGateway gateway = new TcpInboundGateway();
		gateway.setConnectionFactory(scf);
		gateway.setBeanFactory(mock(BeanFactory.class));
		scf.start();
		TestingUtilities.waitListening(scf, 20000L);
		final QueueChannel channel = new QueueChannel();
		gateway.setRequestChannel(channel);
		ServiceActivatingHandler handler = new ServiceActivatingHandler(new Service());
		handler.setChannelResolver(new DestinationResolver<MessageChannel>() {
			@Override
			public MessageChannel resolveDestination(String channelName) {
				return channel;
			}
		});
		Socket socket1 = SocketFactory.getDefault().createSocket("localhost", port);
		socket1.getOutputStream().write("Test1\r\n".getBytes());
		Socket socket2 = SocketFactory.getDefault().createSocket("localhost", port);
		socket2.getOutputStream().write("Test2\r\n".getBytes());
		handler.handleMessage(channel.receive(1000));
		handler.handleMessage(channel.receive(1000));
		byte[] bytes = new byte[12];
		readFully(socket1.getInputStream(), bytes);
		assertEquals("Echo:Test1\r\n", new String(bytes));
		readFully(socket2.getInputStream(), bytes);
		assertEquals("Echo:Test2\r\n", new String(bytes));
	}

	@Test
	public void testNetNotSingle() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		AbstractServerConnectionFactory scf = new TcpNetServerConnectionFactory(port);
		scf.setSingleUse(false);
		TcpInboundGateway gateway = new TcpInboundGateway();
		gateway.setConnectionFactory(scf);
		scf.start();
		TestingUtilities.waitListening(scf, 20000L);
		final QueueChannel channel = new QueueChannel();
		gateway.setRequestChannel(channel);
		gateway.setBeanFactory(mock(BeanFactory.class));
		ServiceActivatingHandler handler = new ServiceActivatingHandler(new Service());
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.getOutputStream().write("Test1\r\n".getBytes());
		socket.getOutputStream().write("Test2\r\n".getBytes());
		handler.handleMessage(channel.receive(10000));
		handler.handleMessage(channel.receive(10000));
		byte[] bytes = new byte[12];
		readFully(socket.getInputStream(), bytes);
		assertEquals("Echo:Test1\r\n", new String(bytes));
		readFully(socket.getInputStream(), bytes);
		assertEquals("Echo:Test2\r\n", new String(bytes));
	}

	@Test
	public void testNetClientMode() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		AbstractClientConnectionFactory ccf = new TcpNetClientConnectionFactory("localhost", port);
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
		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);
		final CountDownLatch latch3 = new CountDownLatch(1);
		final AtomicBoolean done = new AtomicBoolean();
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			@Override
			public void run() {
				try {
					ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port, 10);
					latch1.countDown();
					Socket socket = server.accept();
					socket.getOutputStream().write("Test1\r\nTest2\r\n".getBytes());
					byte[] bytes = new byte[12];
					readFully(socket.getInputStream(), bytes);
					assertEquals("Echo:Test1\r\n", new String(bytes));
					readFully(socket.getInputStream(), bytes);
					assertEquals("Echo:Test2\r\n", new String(bytes));
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
			}
		});
		assertTrue(latch1.await(10, TimeUnit.SECONDS));
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.setPoolSize(1);
		taskScheduler.initialize();
		gateway.setTaskScheduler(taskScheduler);
		gateway.start();
		Message<?> message = channel.receive(10000);
		assertNotNull(message);
		handler.handleMessage(message);
		message = channel.receive(10000);
		assertNotNull(message);
		handler.handleMessage(message);
		latch2.countDown();
		assertTrue(latch3.await(10, TimeUnit.SECONDS));
		assertTrue(done.get());
		gateway.stop();
	}

	@Test
	public void testNioSingle() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		AbstractServerConnectionFactory scf = new TcpNioServerConnectionFactory(port);
		scf.setSingleUse(true);
		TcpInboundGateway gateway = new TcpInboundGateway();
		gateway.setConnectionFactory(scf);
		scf.start();
		TestingUtilities.waitListening(scf, 20000L);
		final QueueChannel channel = new QueueChannel();
		gateway.setRequestChannel(channel);
		gateway.setBeanFactory(mock(BeanFactory.class));
		ServiceActivatingHandler handler = new ServiceActivatingHandler(new Service());
		handler.setChannelResolver(new DestinationResolver<MessageChannel>() {
			@Override
			public MessageChannel resolveDestination(String channelName) {
				return channel;
			}
		});
		Socket socket1 = SocketFactory.getDefault().createSocket("localhost", port);
		socket1.getOutputStream().write("Test1\r\n".getBytes());
		Socket socket2 = SocketFactory.getDefault().createSocket("localhost", port);
		socket2.getOutputStream().write("Test2\r\n".getBytes());
		handler.handleMessage(channel.receive(10000));
		handler.handleMessage(channel.receive(10000));
		byte[] bytes = new byte[12];
		readFully(socket1.getInputStream(), bytes);
		assertEquals("Echo:Test1\r\n", new String(bytes));
		readFully(socket2.getInputStream(), bytes);
		assertEquals("Echo:Test2\r\n", new String(bytes));
	}

	@Test
	public void testNioNotSingle() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		AbstractServerConnectionFactory scf = new TcpNioServerConnectionFactory(port);
		scf.setSingleUse(false);
		TcpInboundGateway gateway = new TcpInboundGateway();
		gateway.setConnectionFactory(scf);
		scf.start();
		TestingUtilities.waitListening(scf, 20000L);
		final QueueChannel channel = new QueueChannel();
		gateway.setRequestChannel(channel);
		gateway.setBeanFactory(mock(BeanFactory.class));
		ServiceActivatingHandler handler = new ServiceActivatingHandler(new Service());
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
		assertTrue(results.remove("Echo:Test1\r\n"));
		assertTrue(results.remove("Echo:Test2\r\n"));
	}

	@Test
	public void testErrorFlow() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		AbstractServerConnectionFactory scf = new TcpNetServerConnectionFactory(port);
		scf.setSingleUse(true);
		TcpInboundGateway gateway = new TcpInboundGateway();
		gateway.setConnectionFactory(scf);
		SubscribableChannel errorChannel = new DirectChannel();
		final String errorMessage = "An error occurred";
		errorChannel.subscribe(new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				MessageChannel replyChannel = (MessageChannel) message.getHeaders().getReplyChannel();
				replyChannel.send(new GenericMessage<String>(errorMessage));
			}
		});
		gateway.setErrorChannel(errorChannel);
		scf.start();
		TestingUtilities.waitListening(scf, 20000L);
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
		assertEquals(errorMessage + "\r\n", new String(bytes));
		readFully(socket2.getInputStream(), bytes);
		assertEquals(errorMessage + "\r\n", new String(bytes));
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

	private void readFully(InputStream is, byte[] buff) throws IOException {
		for (int i = 0; i < buff.length; i++) {
			buff[i] = (byte) is.read();
		}
	}

}
