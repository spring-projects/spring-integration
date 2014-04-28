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

package org.springframework.integration.ip.tcp.connection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.ip.util.TestingUtilities;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.support.LongRunningIntegrationTest;
import org.springframework.integration.test.util.SocketUtils;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;

/**
 * @author Gary Russell
 * @since 2.2
 */
public class ConnectionTimeoutTests {

	@Rule
	public LongRunningIntegrationTest longTests = new LongRunningIntegrationTest();

	@Test
	public void testDefaultTimeout() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		TcpNetServerConnectionFactory server = new TcpNetServerConnectionFactory(port);
		TcpNetClientConnectionFactory client = new TcpNetClientConnectionFactory("localhost", port);
		this.setupCallbacks(server, client, 0);
		server.start();
		TestingUtilities.waitListening(server, null);
		client.start();
		TcpConnection connection = client.getConnection();
		Socket socket = TestUtils.getPropertyValue(connection, "socket", Socket.class);
		// should default to 0 (infinite) timeout
		assertEquals(0, socket.getSoTimeout());
		connection.close();
		server.stop();
		client.stop();
	}

	@Test
	public void testNetSimpleTimeout() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		TcpNetServerConnectionFactory server = new TcpNetServerConnectionFactory(port);
		TcpNetClientConnectionFactory client = new TcpNetClientConnectionFactory("localhost", port);
		this.setupCallbacks(server, client, 0);
		client.registerListener(new TcpListener() {
			@Override
			public boolean onMessage(Message<?> message) {
				return false;
			}
		});
		client.setSoTimeout(1000);
		server.start();
		TestingUtilities.waitListening(server, null);
		CountDownLatch clientCloseLatch = getCloseLatch(client);
		client.start();
		TcpConnection connection = client.getConnection();
		Socket socket = TestUtils.getPropertyValue(connection, "socket", Socket.class);
		assertEquals(1000, socket.getSoTimeout());
		assertTrue(clientCloseLatch.await(3, TimeUnit.SECONDS));
		assertFalse(connection.isOpen());
		server.stop();
		client.stop();
	}

	/**
	 * Ensure we don't timeout on the read side (client) if we sent a message within the
	 * current timeout.
	 * @throws Exception
	 */
	@Test
	public void testNetReplyNotTimeout() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		TcpNetServerConnectionFactory server = new TcpNetServerConnectionFactory(port);
		TcpNetClientConnectionFactory client = new TcpNetClientConnectionFactory("localhost", port);
		this.notTimeoutGuts(server, client);
	}

	/**
	 * Ensure we don't timeout on the read side (client) if we sent a message within the
	 * current timeout.
	 * @throws Exception
	 */
	@Test
	public void testNioReplyNotTimeout() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		TcpNetServerConnectionFactory server = new TcpNetServerConnectionFactory(port);
		TcpNioClientConnectionFactory client = new TcpNioClientConnectionFactory("localhost", port);
		this.notTimeoutGuts(server, client);
	}

	private void notTimeoutGuts(AbstractServerConnectionFactory server, AbstractClientConnectionFactory client)
			throws Exception, InterruptedException {
		this.setupCallbacks(server, client, 1200);
		final AtomicReference<Message<?>> reply = new AtomicReference<Message<?>>();
		final CountDownLatch replyLatch = new CountDownLatch(1);
		client.registerListener(new TcpListener() {
			@Override
			public boolean onMessage(Message<?> message) {
				if (!(message instanceof ErrorMessage)) {
					reply.set(message);
					replyLatch.countDown();
				}
				return false;
			}
		});
		client.setSoTimeout(2000);
		server.start();
		TestingUtilities.waitListening(server, null);
		CountDownLatch clientClosedLatch = getCloseLatch(client);
		client.start();
		TcpConnection connection = client.getConnection();
		Thread.sleep(1000);
		connection.send(MessageBuilder.withPayload("foo").build());
		assertTrue(replyLatch.await(5, TimeUnit.SECONDS));
		assertNotNull(reply.get());
		assertTrue(clientClosedLatch.await(10, TimeUnit.SECONDS));
		assertFalse(connection.isOpen());
		server.stop();
		client.stop();
	}

	/**
	 * Ensure we do timeout on the read side (client) if we sent a message within the
	 * first timeout but the reply takes > 2 timeouts.
	 * @throws Exception
	 */
	@Test
	public void testNetReplyTimeout() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		TcpNetServerConnectionFactory server = new TcpNetServerConnectionFactory(port);
		TcpNetClientConnectionFactory client = new TcpNetClientConnectionFactory("localhost", port);
		this.setupCallbacks(server, client, 4500);
		final AtomicReference<Message<?>> reply = new AtomicReference<Message<?>>();
		client.registerListener(new TcpListener() {
			@Override
			public boolean onMessage(Message<?> message) {
				if (!(message instanceof ErrorMessage)) {
					reply.set(message);
				}
				return false;
			}
		});
		client.setSoTimeout(2000);
		server.start();
		TestingUtilities.waitListening(server, null);
		CountDownLatch clientCloseLatch = getCloseLatch(client);
		client.start();
		TcpConnection connection = client.getConnection();
		Socket socket = TestUtils.getPropertyValue(connection, "socket", Socket.class);
		assertEquals(2000, socket.getSoTimeout());
		Thread.sleep(1000);
		connection.send(MessageBuilder.withPayload("foo").build());
		Thread.sleep(1400);
		assertTrue(connection.isOpen());
		assertTrue(clientCloseLatch.await(2000, TimeUnit.SECONDS));
		assertNull(reply.get());
		assertFalse(connection.isOpen());
		server.stop();
		client.stop();
	}

	/**
	 * Ensure we do timeout on the read side (client) if we sent a message within the
	 * first timeout but the reply takes > 2 timeouts.
	 * @throws Exception
	 */
	@Test
	public void testNioReplyTimeout() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		TcpNetServerConnectionFactory server = new TcpNetServerConnectionFactory(port);
		TcpNioClientConnectionFactory client = new TcpNioClientConnectionFactory("localhost", port);
		this.setupCallbacks(server, client, 2100);
		final AtomicReference<Message<?>> reply = new AtomicReference<Message<?>>();
		client.registerListener(new TcpListener() {
			@Override
			public boolean onMessage(Message<?> message) {
				if (!(message instanceof ErrorMessage)) {
					reply.set(message);
				}
				return false;
			}
		});
		client.setSoTimeout(1000);
		server.start();
		TestingUtilities.waitListening(server, null);
		CountDownLatch clientCloseLatch = getCloseLatch(client);
		client.start();
		TcpConnection connection = client.getConnection();
		Thread.sleep(500);
		connection.send(MessageBuilder.withPayload("foo").build());
		Thread.sleep(700);
		assertTrue(connection.isOpen());
		assertTrue(clientCloseLatch.await(2, TimeUnit.SECONDS));
		assertNull(reply.get());
		assertFalse(connection.isOpen());
		server.stop();
		client.stop();
	}

	private void setupCallbacks(AbstractServerConnectionFactory server, AbstractClientConnectionFactory client,
			final int serverDelay) {
		client.setComponentName("clientFactory");
		server.setComponentName("serverFactory");
		final AtomicReference<TcpConnection> serverConnection = new AtomicReference<TcpConnection>();
		server.registerListener(new TcpListener() {
			@Override
			public boolean onMessage(Message<?> message) {
				try {
					Thread.sleep(serverDelay);
					serverConnection.get().send(message);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				return false;
			}
		});
		server.registerSender(new TcpSender() {
			@Override
			public void addNewConnection(TcpConnection connection) {
				serverConnection.set(connection);
			}
			@Override
			public void removeDeadConnection(TcpConnection connection) {
			}
		});
		client.registerSender(new TcpSender() {
			@Override
			public void addNewConnection(TcpConnection connection) {
			}
			@Override
			public void removeDeadConnection(TcpConnection connection) {
			}
		});
	}

	private CountDownLatch getCloseLatch(AbstractClientConnectionFactory client) {
		final CountDownLatch clientClosedLatch;
		clientClosedLatch = new CountDownLatch(1);
		client.setApplicationEventPublisher(new ApplicationEventPublisher() {
			@Override
			public void publishEvent(ApplicationEvent event) {
				if (event instanceof TcpConnectionCloseEvent) {
					clientClosedLatch.countDown();
				}
			}
		});
		return clientClosedLatch;
	}

}
