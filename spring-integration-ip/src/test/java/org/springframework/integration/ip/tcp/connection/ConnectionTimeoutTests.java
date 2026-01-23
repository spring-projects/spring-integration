/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.ip.tcp.connection;

import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.ip.util.TestingUtilities;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.condition.LongRunningTest;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 2.2
 */
@LongRunningTest
public class ConnectionTimeoutTests {

	@Test
	public void testDefaultTimeout() throws Exception {
		TcpNetServerConnectionFactory server = new TcpNetServerConnectionFactory(0);
		this.setupServerCallbacks(server, 0);
		server.start();
		TestingUtilities.waitListening(server, null);
		TcpNetClientConnectionFactory client = new TcpNetClientConnectionFactory("localhost", server.getPort());
		setupClientCallback(client);
		client.start();
		TcpConnection connection = client.getConnection();
		Socket socket = TestUtils.getPropertyValue(connection, "socket");
		// should default to 0 (infinite) timeout
		assertThat(socket.getSoTimeout()).isEqualTo(0);
		connection.close();
		server.stop();
		client.stop();
	}

	@Test
	public void testNetSimpleTimeout() throws Exception {
		TcpNetServerConnectionFactory server = new TcpNetServerConnectionFactory(0);
		this.setupServerCallbacks(server, 0);
		server.start();
		TestingUtilities.waitListening(server, null);
		TcpNetClientConnectionFactory client = new TcpNetClientConnectionFactory("localhost", server.getPort());
		client.registerListener(message -> {
		});
		client.setSoTimeout(1000);
		CountDownLatch clientCloseLatch = getCloseLatch(client);
		setupClientCallback(client);
		client.start();
		TcpConnection connection = client.getConnection();
		Socket socket = TestUtils.getPropertyValue(connection, "socket");
		assertThat(socket.getSoTimeout()).isEqualTo(1000);
		assertThat(clientCloseLatch.await(3, TimeUnit.SECONDS)).isTrue();
		assertThat(connection.isOpen()).isFalse();
		server.stop();
		client.stop();
	}

	/**
	 * Ensure we don't time out on the read side (client) if we sent a message within the
	 * current timeout.
	 * @throws Exception
	 */
	@Test
	public void testNetReplyNotTimeout() throws Exception {
		TcpNetServerConnectionFactory server = new TcpNetServerConnectionFactory(0);
		setupAndStartServer(server);
		TcpNetClientConnectionFactory client = new TcpNetClientConnectionFactory("localhost", server.getPort());
		this.notTimeoutGuts(server, client);
	}

	/**
	 * Ensure we don't time out on the read side (client) if we sent a message within the
	 * current timeout.
	 * @throws Exception
	 */
	@Test
	public void testNioReplyNotTimeout() throws Exception {
		TcpNetServerConnectionFactory server = new TcpNetServerConnectionFactory(0);
		setupAndStartServer(server);
		TcpNioClientConnectionFactory client = new TcpNioClientConnectionFactory("localhost", server.getPort());
		this.notTimeoutGuts(server, client);
	}

	private void notTimeoutGuts(AbstractServerConnectionFactory server, AbstractClientConnectionFactory client)
			throws Exception {
		final AtomicReference<Message<?>> reply = new AtomicReference<>();
		final CountDownLatch replyLatch = new CountDownLatch(1);
		client.registerListener(message -> {
			if (!(message instanceof ErrorMessage)) {
				reply.set(message);
				replyLatch.countDown();
			}
		});
		client.setSoTimeout(2000);
		CountDownLatch clientClosedLatch = getCloseLatch(client);
		setupClientCallback(client);
		client.start();
		TcpConnection connection = client.getConnection();
		Thread.sleep(1000);
		connection.send(MessageBuilder.withPayload("foo").build());
		assertThat(replyLatch.await(5, TimeUnit.SECONDS)).isTrue();
		assertThat(reply.get()).isNotNull();
		assertThat(clientClosedLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(connection.isOpen()).isFalse();
		server.stop();
		client.stop();
	}

	private void setupAndStartServer(AbstractServerConnectionFactory server) {
		this.setupServerCallbacks(server, 1200);
		server.start();
		TestingUtilities.waitListening(server, null);
	}

	/**
	 * Ensure we do timeout on the read side (client) if we sent a message within the
	 * first timeout but the reply takes > 2 timeouts.
	 * @throws Exception
	 */
	@Test
	public void testNetReplyTimeout() throws Exception {
		TcpNetServerConnectionFactory server = new TcpNetServerConnectionFactory(0);
		this.setupServerCallbacks(server, 4500);
		final AtomicReference<Message<?>> reply = new AtomicReference<>();
		server.start();
		TestingUtilities.waitListening(server, null);
		TcpNetClientConnectionFactory client = new TcpNetClientConnectionFactory("localhost", server.getPort());
		client.registerListener(message -> {
			if (!(message instanceof ErrorMessage)) {
				reply.set(message);
			}
		});
		client.setSoTimeout(2000);
		CountDownLatch clientCloseLatch = getCloseLatch(client);
		setupClientCallback(client);
		client.start();
		TcpConnection connection = client.getConnection();
		Socket socket = TestUtils.getPropertyValue(connection, "socket");
		assertThat(socket.getSoTimeout()).isEqualTo(2000);
		Thread.sleep(1000);
		connection.send(MessageBuilder.withPayload("foo").build());
		Thread.sleep(1400);
		assertThat(connection.isOpen()).isTrue();
		assertThat(clientCloseLatch.await(5, TimeUnit.SECONDS)).isTrue();
		assertThat(reply.get()).isNull();
		assertThat(connection.isOpen()).isFalse();
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
		TcpNetServerConnectionFactory server = new TcpNetServerConnectionFactory(0);
		this.setupServerCallbacks(server, 2100);
		final AtomicReference<Message<?>> reply = new AtomicReference<>();
		server.start();
		TestingUtilities.waitListening(server, null);
		TcpNioClientConnectionFactory client = new TcpNioClientConnectionFactory("localhost", server.getPort());
		client.registerListener(message -> {
			if (!(message instanceof ErrorMessage)) {
				reply.set(message);
			}
		});
		client.setSoTimeout(1000);
		CountDownLatch clientCloseLatch = getCloseLatch(client);
		setupClientCallback(client);
		client.start();
		TcpConnection connection = client.getConnection();
		Thread.sleep(500);
		connection.send(MessageBuilder.withPayload("foo").build());
		Thread.sleep(700);
		assertThat(connection.isOpen()).isTrue();
		assertThat(clientCloseLatch.await(2, TimeUnit.SECONDS)).isTrue();
		assertThat(reply.get()).isNull();
		assertThat(connection.isOpen()).isFalse();
		server.stop();
		client.stop();
	}

	private void setupServerCallbacks(AbstractServerConnectionFactory server, final int serverDelay) {
		server.setComponentName("serverFactory");
		final AtomicReference<TcpConnection> serverConnection = new AtomicReference<>();
		server.registerListener(message -> {
			try {
				Thread.sleep(serverDelay);
				serverConnection.get().send(message);
			}
			catch (Exception e) {
				e.printStackTrace();
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
	}

	public void setupClientCallback(AbstractClientConnectionFactory client) {
		client.setComponentName("clientFactory");
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

			@Override
			public void publishEvent(Object event) {

			}

		});
		return clientClosedLatch;
	}

}
