/*
 * Copyright 2002-2012 the original author or authors.
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
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.springframework.integration.Message;
import org.springframework.integration.ip.util.TestingUtilities;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.SocketUtils;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author Gary Russell
 * @since 2.2
 *
 */
public class ConnectionTimeoutTests {

	@Test
	public void testDefaultTimeout() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		TcpNetServerConnectionFactory server = new TcpNetServerConnectionFactory(port);
		server.registerListener(new TcpListener() {
			public boolean onMessage(Message<?> message) {
				return false;
			}
		});
		TcpNetClientConnectionFactory client = new TcpNetClientConnectionFactory("localhost", port);
		client.registerSender(new TcpSender() {
			public void addNewConnection(TcpConnection connection) {
			}
			public void removeDeadConnection(TcpConnection connection) {
			}
		});
		client.registerListener(new TcpListener() {
			public boolean onMessage(Message<?> message) {
				return false;
			}
		});
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
		server.registerListener(new TcpListener() {
			public boolean onMessage(Message<?> message) {
				return false;
			}
		});
		TcpNetClientConnectionFactory client = new TcpNetClientConnectionFactory("localhost", port);
		client.registerSender(new TcpSender() {
			public void addNewConnection(TcpConnection connection) {
			}
			public void removeDeadConnection(TcpConnection connection) {
			}
		});
		client.registerListener(new TcpListener() {
			public boolean onMessage(Message<?> message) {
				return false;
			}
		});
		client.setSoTimeout(1000);
		server.start();
		TestingUtilities.waitListening(server, null);
		client.start();
		TcpConnection connection = client.getConnection();
		Socket socket = TestUtils.getPropertyValue(connection, "socket", Socket.class);
		assertEquals(1000, socket.getSoTimeout());
		Thread.sleep(1100);
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
		final AtomicReference<TcpConnection> serverConnection = new AtomicReference<TcpConnection>();
		server.registerListener(new TcpListener() {
			public boolean onMessage(Message<?> message) {
				try {
					Thread.sleep(1200);
					serverConnection.get().send(message);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				return false;
			}
		});
		server.registerSender(new TcpSender() {
			public void addNewConnection(TcpConnection connection) {
				serverConnection.set(connection);
			}
			public void removeDeadConnection(TcpConnection connection) {
			}
		});
		TcpNetClientConnectionFactory client = new TcpNetClientConnectionFactory("localhost", port);
		client.registerSender(new TcpSender() {
			public void addNewConnection(TcpConnection connection) {
			}
			public void removeDeadConnection(TcpConnection connection) {
			}
		});
		final AtomicReference<Message<?>> reply = new AtomicReference<Message<?>>();
		client.registerListener(new TcpListener() {
			public boolean onMessage(Message<?> message) {
				reply.set(message);
				return false;
			}
		});
		client.setSoTimeout(2000);
		server.start();
		TestingUtilities.waitListening(server, null);
		client.start();
		TcpConnection connection = client.getConnection();
		Socket socket = TestUtils.getPropertyValue(connection, "socket", Socket.class);
		assertEquals(2000, socket.getSoTimeout());
		Thread.sleep(1000);
		connection.send(MessageBuilder.withPayload("foo").build());
		Thread.sleep(1400);
		assertNotNull(reply.get());
		Thread.sleep(2200);
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
	public void testNioReplyNotTimeout() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		TcpNetServerConnectionFactory server = new TcpNetServerConnectionFactory(port);
		final AtomicReference<TcpConnection> serverConnection = new AtomicReference<TcpConnection>();
		server.registerListener(new TcpListener() {
			public boolean onMessage(Message<?> message) {
				try {
					Thread.sleep(1200);
					serverConnection.get().send(message);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				return false;
			}
		});
		server.registerSender(new TcpSender() {
			public void addNewConnection(TcpConnection connection) {
				serverConnection.set(connection);
			}
			public void removeDeadConnection(TcpConnection connection) {
			}
		});
		TcpNioClientConnectionFactory client = new TcpNioClientConnectionFactory("localhost", port);
		client.registerSender(new TcpSender() {
			public void addNewConnection(TcpConnection connection) {
			}
			public void removeDeadConnection(TcpConnection connection) {
			}
		});
		final AtomicReference<Message<?>> reply = new AtomicReference<Message<?>>();
		client.registerListener(new TcpListener() {
			public boolean onMessage(Message<?> message) {
				reply.set(message);
				return false;
			}
		});
		client.setSoTimeout(2000);
		server.start();
		TestingUtilities.waitListening(server, null);
		client.start();
		TcpConnection connection = client.getConnection();
		Thread.sleep(1000);
		connection.send(MessageBuilder.withPayload("foo").build());
		Thread.sleep(1400);
		assertNotNull(reply.get());
		Thread.sleep(4200);
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
		final AtomicReference<TcpConnection> serverConnection = new AtomicReference<TcpConnection>();
		server.registerListener(new TcpListener() {
			public boolean onMessage(Message<?> message) {
				try {
					Thread.sleep(4200);
					serverConnection.get().send(message);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				return false;
			}
		});
		server.registerSender(new TcpSender() {
			public void addNewConnection(TcpConnection connection) {
				serverConnection.set(connection);
			}
			public void removeDeadConnection(TcpConnection connection) {
			}
		});
		TcpNetClientConnectionFactory client = new TcpNetClientConnectionFactory("localhost", port);
		client.registerSender(new TcpSender() {
			public void addNewConnection(TcpConnection connection) {
			}
			public void removeDeadConnection(TcpConnection connection) {
			}
		});
		final AtomicReference<Message<?>> reply = new AtomicReference<Message<?>>();
		client.registerListener(new TcpListener() {
			public boolean onMessage(Message<?> message) {
				reply.set(message);
				return false;
			}
		});
		client.setSoTimeout(2000);
		server.start();
		TestingUtilities.waitListening(server, null);
		client.start();
		TcpConnection connection = client.getConnection();
		Socket socket = TestUtils.getPropertyValue(connection, "socket", Socket.class);
		assertEquals(2000, socket.getSoTimeout());
		Thread.sleep(1000);
		connection.send(MessageBuilder.withPayload("foo").build());
		Thread.sleep(1400);
		assertTrue(connection.isOpen());
		Thread.sleep(2000);
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
		final AtomicReference<TcpConnection> serverConnection = new AtomicReference<TcpConnection>();
		server.registerListener(new TcpListener() {
			public boolean onMessage(Message<?> message) {
				try {
					Thread.sleep(2100);
					serverConnection.get().send(message);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				return false;
			}
		});
		server.registerSender(new TcpSender() {
			public void addNewConnection(TcpConnection connection) {
				serverConnection.set(connection);
			}
			public void removeDeadConnection(TcpConnection connection) {
			}
		});
		TcpNioClientConnectionFactory client = new TcpNioClientConnectionFactory("localhost", port);
		client.registerSender(new TcpSender() {
			public void addNewConnection(TcpConnection connection) {
			}
			public void removeDeadConnection(TcpConnection connection) {
			}
		});
		final AtomicReference<Message<?>> reply = new AtomicReference<Message<?>>();
		client.registerListener(new TcpListener() {
			public boolean onMessage(Message<?> message) {
				reply.set(message);
				return false;
			}
		});
		client.setSoTimeout(1000);
		server.start();
		TestingUtilities.waitListening(server, null);
		client.start();
		TcpConnection connection = client.getConnection();
		Thread.sleep(500);
		connection.send(MessageBuilder.withPayload("foo").build());
		Thread.sleep(700);
		assertTrue(connection.isOpen());
		Thread.sleep(1000);
		assertNull(reply.get());
		assertFalse(connection.isOpen());
		server.stop();
		client.stop();
	}
}
