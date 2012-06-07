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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessagingException;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.tcp.TcpInboundGateway;
import org.springframework.integration.ip.tcp.TcpOutboundGateway;
import org.springframework.integration.ip.util.SocketTestUtils;
import org.springframework.integration.ip.util.TestingUtilities;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author Gary Russell
 * @since 2.2
 *
 */
public class FailoverClientConnectionFactoryTests {

	@Test
	public void testFailoverGood() throws Exception {
		AbstractClientConnectionFactory factory1 = mock(AbstractClientConnectionFactory.class);
		AbstractClientConnectionFactory factory2 = mock(AbstractClientConnectionFactory.class);
		List<AbstractClientConnectionFactory> factories = new ArrayList<AbstractClientConnectionFactory>();
		factories.add(factory1);
		factories.add(factory2);
		TcpConnection conn1 = makeMockConnection();
		TcpConnection conn2 = makeMockConnection();
		when(factory1.getConnection()).thenReturn(conn1);
		when(factory2.getConnection()).thenReturn(conn2);
		when(factory1.isActive()).thenReturn(true);
		when(factory2.isActive()).thenReturn(true);
		doThrow(new IOException("fail")).when(conn1).send(Mockito.any(Message.class));
		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return null;
			}
		}).when(conn2).send(Mockito.any(Message.class));
		FailoverClientConnectionFactory failoverFactory = new FailoverClientConnectionFactory(factories);
		failoverFactory.start();
		GenericMessage<String> message = new GenericMessage<String>("foo");
		failoverFactory.getConnection().send(message);
		Mockito.verify(conn2).send(message);
	}

	@Test(expected=IOException.class)
	public void testFailoverAllDead() throws Exception {
		AbstractClientConnectionFactory factory1 = mock(AbstractClientConnectionFactory.class);
		AbstractClientConnectionFactory factory2 = mock(AbstractClientConnectionFactory.class);
		List<AbstractClientConnectionFactory> factories = new ArrayList<AbstractClientConnectionFactory>();
		factories.add(factory1);
		factories.add(factory2);
		TcpConnection conn1 = makeMockConnection();
		TcpConnection conn2 = makeMockConnection();
		when(factory1.getConnection()).thenReturn(conn1);
		when(factory2.getConnection()).thenReturn(conn2);
		when(factory1.isActive()).thenReturn(true);
		when(factory2.isActive()).thenReturn(true);
		doThrow(new IOException("fail")).when(conn1).send(Mockito.any(Message.class));
		doThrow(new IOException("fail")).when(conn2).send(Mockito.any(Message.class));
		FailoverClientConnectionFactory failoverFactory = new FailoverClientConnectionFactory(factories);
		failoverFactory.start();
		GenericMessage<String> message = new GenericMessage<String>("foo");
		failoverFactory.getConnection().send(message);
		Mockito.verify(conn2).send(message);
	}

	@Test
	public void testFailoverAllDeadButOriginalOkAgain() throws Exception {
		AbstractClientConnectionFactory factory1 = mock(AbstractClientConnectionFactory.class);
		AbstractClientConnectionFactory factory2 = mock(AbstractClientConnectionFactory.class);
		List<AbstractClientConnectionFactory> factories = new ArrayList<AbstractClientConnectionFactory>();
		factories.add(factory1);
		factories.add(factory2);
		TcpConnection conn1 = makeMockConnection();
		TcpConnection conn2 = makeMockConnection();
		when(factory1.getConnection()).thenReturn(conn1);
		when(factory2.getConnection()).thenReturn(conn2);
		when(factory1.isActive()).thenReturn(true);
		when(factory2.isActive()).thenReturn(true);
		final AtomicBoolean failedOnce = new AtomicBoolean();
		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				if (!failedOnce.get()) {
					failedOnce.set(true);
					throw new IOException("fail");
				}
				return null;
			}
		}).when(conn1).send(Mockito.any(Message.class));
		doThrow(new IOException("fail")).when(conn2).send(Mockito.any(Message.class));
		FailoverClientConnectionFactory failoverFactory = new FailoverClientConnectionFactory(factories);
		failoverFactory.start();
		GenericMessage<String> message = new GenericMessage<String>("foo");
		failoverFactory.getConnection().send(message);
		Mockito.verify(conn2).send(message);
		Mockito.verify(conn1, times(2)).send(message);
	}

	@Test(expected=IOException.class)
	public void testFailoverConnectNone() throws Exception {
		AbstractClientConnectionFactory factory1 = mock(AbstractClientConnectionFactory.class);
		AbstractClientConnectionFactory factory2 = mock(AbstractClientConnectionFactory.class);
		List<AbstractClientConnectionFactory> factories = new ArrayList<AbstractClientConnectionFactory>();
		factories.add(factory1);
		factories.add(factory2);
		when(factory1.getConnection()).thenThrow(new IOException("fail"));
		when(factory2.getConnection()).thenThrow(new IOException("fail"));
		when(factory1.isActive()).thenReturn(true);
		when(factory2.isActive()).thenReturn(true);
		FailoverClientConnectionFactory failoverFactory = new FailoverClientConnectionFactory(factories);
		failoverFactory.start();
		GenericMessage<String> message = new GenericMessage<String>("foo");
		failoverFactory.getConnection().send(message);
	}

	@Test
	public void testFailoverConnectToFirstAfterTriedAll() throws Exception {
		AbstractClientConnectionFactory factory1 = mock(AbstractClientConnectionFactory.class);
		AbstractClientConnectionFactory factory2 = mock(AbstractClientConnectionFactory.class);
		List<AbstractClientConnectionFactory> factories = new ArrayList<AbstractClientConnectionFactory>();
		factories.add(factory1);
		factories.add(factory2);
		TcpConnection conn1 = makeMockConnection();
		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return null;
			}
		}).when(conn1).send(Mockito.any(Message.class));
		when(factory1.getConnection()).thenThrow(new IOException("fail")).thenReturn(conn1);
		when(factory2.getConnection()).thenThrow(new IOException("fail"));
		when(factory1.isActive()).thenReturn(true);
		when(factory2.isActive()).thenReturn(true);
		FailoverClientConnectionFactory failoverFactory = new FailoverClientConnectionFactory(factories);
		failoverFactory.start();
		GenericMessage<String> message = new GenericMessage<String>("foo");
		failoverFactory.getConnection().send(message);
		Mockito.verify(conn1).send(message);
	}

	@Test
	public void testOkAgainAfterCompleteFailure() throws Exception {
		AbstractClientConnectionFactory factory1 = mock(AbstractClientConnectionFactory.class);
		AbstractClientConnectionFactory factory2 = mock(AbstractClientConnectionFactory.class);
		List<AbstractClientConnectionFactory> factories = new ArrayList<AbstractClientConnectionFactory>();
		factories.add(factory1);
		factories.add(factory2);
		TcpConnection conn1 = makeMockConnection();
		TcpConnection conn2 = makeMockConnection();
		when(factory1.getConnection()).thenReturn(conn1);
		when(factory2.getConnection()).thenReturn(conn2);
		when(factory1.isActive()).thenReturn(true);
		when(factory2.isActive()).thenReturn(true);
		final AtomicInteger failCount = new AtomicInteger();
		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				if (failCount.incrementAndGet() < 3) {
					throw new IOException("fail");
				}
				return null;
			}
		}).when(conn1).send(Mockito.any(Message.class));
		doThrow(new IOException("fail")).when(conn2).send(Mockito.any(Message.class));
		FailoverClientConnectionFactory failoverFactory = new FailoverClientConnectionFactory(factories);
		failoverFactory.start();
		GenericMessage<String> message = new GenericMessage<String>("foo");
		try {
			failoverFactory.getConnection().send(message);
			fail("ExpectedFailure");
		}
		catch (IOException e) {}
		failoverFactory.getConnection().send(message);
		Mockito.verify(conn2).send(message);
		Mockito.verify(conn1, times(3)).send(message);
	}

	public TcpConnection makeMockConnection() {
		TcpConnection connection = mock(TcpConnection.class);
		when(connection.isOpen()).thenReturn(true);
		return connection;
	}

	@Test
	public void testRealNet() throws Exception {
		int port1 = SocketTestUtils.findAvailableServerSocket();
		int port2 = SocketTestUtils.findAvailableServerSocket(port1 + 1);
		AbstractClientConnectionFactory client1 = new TcpNetClientConnectionFactory("localhost", port1);
		AbstractClientConnectionFactory client2 = new TcpNetClientConnectionFactory("localhost", port2);
		AbstractServerConnectionFactory server1 = new TcpNetServerConnectionFactory(port1);
		AbstractServerConnectionFactory server2 = new TcpNetServerConnectionFactory(port2);
		testRealGuts(client1, client2, server1, server2);
	}

	@Test
	public void testRealNio() throws Exception {
		int port1 = SocketTestUtils.findAvailableServerSocket();
		int port2 = SocketTestUtils.findAvailableServerSocket(port1 + 1);
		AbstractClientConnectionFactory client1 = new TcpNioClientConnectionFactory("localhost", port1);
		AbstractClientConnectionFactory client2 = new TcpNioClientConnectionFactory("localhost", port2);
		AbstractServerConnectionFactory server1 = new TcpNioServerConnectionFactory(port1);
		AbstractServerConnectionFactory server2 = new TcpNioServerConnectionFactory(port2);
		testRealGuts(client1, client2, server1, server2);
	}

	@Test
	public void testRealNetSingleUse() throws Exception {
		int port1 = SocketTestUtils.findAvailableServerSocket();
		int port2 = SocketTestUtils.findAvailableServerSocket(port1 + 1);
		AbstractClientConnectionFactory client1 = new TcpNetClientConnectionFactory("localhost", port1);
		AbstractClientConnectionFactory client2 = new TcpNetClientConnectionFactory("localhost", port2);
		AbstractServerConnectionFactory server1 = new TcpNetServerConnectionFactory(port1);
		AbstractServerConnectionFactory server2 = new TcpNetServerConnectionFactory(port2);
		client1.setSingleUse(true);
		client2.setSingleUse(true);
		testRealGuts(client1, client2, server1, server2);
	}

	@Test
	public void testRealNioSingleUse() throws Exception {
		int port1 = SocketTestUtils.findAvailableServerSocket();
		int port2 = SocketTestUtils.findAvailableServerSocket(port1 + 1);
		AbstractClientConnectionFactory client1 = new TcpNioClientConnectionFactory("localhost", port1);
		AbstractClientConnectionFactory client2 = new TcpNioClientConnectionFactory("localhost", port2);
		AbstractServerConnectionFactory server1 = new TcpNioServerConnectionFactory(port1);
		AbstractServerConnectionFactory server2 = new TcpNioServerConnectionFactory(port2);
		client1.setSingleUse(true);
		client2.setSingleUse(true);
		testRealGuts(client1, client2, server1, server2);
	}

	private void testRealGuts(AbstractClientConnectionFactory client1, AbstractClientConnectionFactory client2,
			AbstractServerConnectionFactory server1, AbstractServerConnectionFactory server2) throws Exception {
		int port1;
		int port2;
		Executor exec = Executors.newCachedThreadPool();
		client1.setTaskExecutor(exec);
		client2.setTaskExecutor(exec);
		server1.setTaskExecutor(exec);
		server2.setTaskExecutor(exec);
		TcpInboundGateway gateway1 = new TcpInboundGateway();
		gateway1.setConnectionFactory(server1);
		SubscribableChannel channel = new DirectChannel();
		final AtomicReference<String> connectionId = new AtomicReference<String>();
		channel.subscribe(new MessageHandler() {
			public void handleMessage(Message<?> message) throws MessagingException {
				connectionId.set((String) message.getHeaders().get(IpHeaders.CONNECTION_ID));
				((MessageChannel) message.getHeaders().getReplyChannel()).send(message);
			}
		});
		gateway1.setRequestChannel(channel);
		gateway1.start();
		TcpInboundGateway gateway2 = new TcpInboundGateway();
		gateway2.setConnectionFactory(server2);
		gateway2.setRequestChannel(channel);
		gateway2.start();
		TestingUtilities.waitListening(server1, null);
		TestingUtilities.waitListening(server2, null);
		List<AbstractClientConnectionFactory> factories = new ArrayList<AbstractClientConnectionFactory>();
		factories.add(client1);
		factories.add(client2);
		FailoverClientConnectionFactory failFactory = new FailoverClientConnectionFactory(factories);
		boolean singleUse = client1.isSingleUse();
		failFactory.setSingleUse(singleUse);
		failFactory.afterPropertiesSet();
		TcpOutboundGateway outGateway = new TcpOutboundGateway();
		outGateway.setConnectionFactory(failFactory);
		outGateway.start();
		QueueChannel replyChannel = new QueueChannel();
		outGateway.setReplyChannel(replyChannel);
		Message<String> message = new GenericMessage<String>("foo");
		outGateway.setRemoteTimeout(120000);
		outGateway.handleMessage(message);
		Socket socket = getSocket(client1);
		port1 = socket.getLocalPort();
		assertTrue(singleUse | connectionId.get().contains(Integer.toString(port1)));
		Message<?> replyMessage = replyChannel.receive(10000);
		assertNotNull(replyMessage);
		server1.stop();
		TestingUtilities.waitStopListening(server1, null);
		outGateway.handleMessage(message);
		socket = getSocket(client2);
		port2 = socket.getLocalPort();
		assertTrue(singleUse | connectionId.get().contains(Integer.toString(port2)));
		replyMessage = replyChannel.receive(10000);
		assertNotNull(replyMessage);
		gateway2.stop();
	}

	private Socket getSocket(AbstractClientConnectionFactory client) throws Exception {
		if (client instanceof TcpNetClientConnectionFactory) {
			return TestUtils.getPropertyValue(client.getConnection(), "socket", Socket.class);
		}
		else {
			return TestUtils.getPropertyValue(client.getConnection(), "socketChannel", SocketChannel.class).socket();
		}

	}

}

