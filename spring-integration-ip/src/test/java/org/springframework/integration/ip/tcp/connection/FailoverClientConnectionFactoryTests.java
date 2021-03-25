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

package org.springframework.integration.ip.tcp.connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ServerSocketFactory;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.BridgeHandler;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.tcp.TcpInboundGateway;
import org.springframework.integration.ip.tcp.TcpOutboundGateway;
import org.springframework.integration.ip.util.TestingUtilities;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.util.SimplePool;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.2
 *
 */
public class FailoverClientConnectionFactoryTests {

	private static final ApplicationEventPublisher NULL_PUBLISHER = new ApplicationEventPublisher() {

		@Override
		public void publishEvent(ApplicationEvent event) {
		}

		@Override
		public void publishEvent(Object event) {

		}

	};

	@Test
	public void testFailoverGood() throws Exception {
		AbstractClientConnectionFactory factory1 = mock(AbstractClientConnectionFactory.class);
		AbstractClientConnectionFactory factory2 = mock(AbstractClientConnectionFactory.class);
		List<AbstractClientConnectionFactory> factories = new ArrayList<AbstractClientConnectionFactory>();
		factories.add(factory1);
		factories.add(factory2);
		TcpConnectionSupport conn1 = makeMockConnection();
		TcpConnectionSupport conn2 = makeMockConnection();
		when(factory1.getConnection()).thenReturn(conn1);
		when(factory2.getConnection()).thenReturn(conn2);
		when(factory1.isActive()).thenReturn(true);
		when(factory2.isActive()).thenReturn(true);
		doThrow(new UncheckedIOException(new IOException("fail")))
			.when(conn1).send(Mockito.any(Message.class));
		doAnswer(invocation -> null).when(conn2).send(Mockito.any(Message.class));
		FailoverClientConnectionFactory failoverFactory = new FailoverClientConnectionFactory(factories);
		failoverFactory.start();
		GenericMessage<String> message = new GenericMessage<String>("foo");
		failoverFactory.getConnection().send(message);
		Mockito.verify(conn2).send(message);
	}

	@Test
	public void testRefreshShared() throws Exception {
		testRefreshShared(false, 10_000);
	}

	@Test
	public void testRefreshSharedCloseOnRefresh() throws Exception {
		testRefreshShared(true, 10_000);
	}

	@Test
	public void testRefreshSharedInfinite() throws Exception {
		testRefreshShared(false, Long.MAX_VALUE);
	}

	private void testRefreshShared(boolean closeOnRefresh, long interval) throws Exception {
		AbstractClientConnectionFactory factory1 = mock(AbstractClientConnectionFactory.class);
		AbstractClientConnectionFactory factory2 = mock(AbstractClientConnectionFactory.class);
		List<AbstractClientConnectionFactory> factories = new ArrayList<AbstractClientConnectionFactory>();
		factories.add(factory1);
		factories.add(factory2);
		TcpConnectionSupport conn1 = makeMockConnection();
		doReturn("conn1").when(conn1).getConnectionId();
		TcpConnectionSupport conn2 = makeMockConnection();
		doReturn("conn2").when(conn2).getConnectionId();
		doThrow(new UncheckedIOException(new IOException("fail")))
			.when(factory1).getConnection();
		if (closeOnRefresh) {
			when(factory2.getConnection()).thenReturn(conn1, conn2);
		}
		else {
			when(factory2.getConnection()).thenReturn(conn1);
		}
		when(factory1.isActive()).thenReturn(true);
		when(factory2.isActive()).thenReturn(true);
		FailoverClientConnectionFactory failoverFactory = new FailoverClientConnectionFactory(factories);
		failoverFactory.setCloseOnRefresh(closeOnRefresh);
		failoverFactory.start();
		TcpConnectionSupport connection = failoverFactory.getConnection();
		assertThat(TestUtils.getPropertyValue(failoverFactory, "theConnection")).isNotNull();
		failoverFactory.setRefreshSharedInterval(interval);
		InOrder inOrder = inOrder(factory1, factory2, conn1, conn2);
		inOrder.verify(factory1).getConnection();
		inOrder.verify(factory2).getConnection();
		inOrder.verify(conn1).registerListener(any());
		inOrder.verify(conn1).isOpen();
		assertThat(failoverFactory.getConnection()).isSameAs(connection);
		inOrder.verifyNoMoreInteractions();
		failoverFactory.setRefreshSharedInterval(-1);
		assertThat(failoverFactory.getConnection()).isNotSameAs(connection);
		inOrder.verify(factory1).getConnection();
		inOrder.verify(factory2).getConnection();
		if (closeOnRefresh) {
			inOrder.verify(conn2).registerListener(any());
			inOrder.verify(conn2).isOpen();
			inOrder.verify(conn1).close();
		}
		else {
			inOrder.verify(conn1).registerListener(any());
			inOrder.verify(conn1).isOpen();
			inOrder.verify(conn1, never()).close();
		}
		inOrder.verifyNoMoreInteractions();
	}

	@Test
	public void testFailoverAllDead() throws Exception {
		AbstractClientConnectionFactory factory1 = mock(AbstractClientConnectionFactory.class);
		AbstractClientConnectionFactory factory2 = mock(AbstractClientConnectionFactory.class);
		List<AbstractClientConnectionFactory> factories = new ArrayList<AbstractClientConnectionFactory>();
		factories.add(factory1);
		factories.add(factory2);
		TcpConnectionSupport conn1 = makeMockConnection();
		TcpConnectionSupport conn2 = makeMockConnection();
		when(factory1.getConnection()).thenReturn(conn1);
		when(factory2.getConnection()).thenReturn(conn2);
		when(factory1.isActive()).thenReturn(true);
		when(factory2.isActive()).thenReturn(true);
		doThrow(new UncheckedIOException(new IOException("fail")))
			.when(conn1).send(Mockito.any(Message.class));
		doThrow(new UncheckedIOException(new IOException("fail")))
			.when(conn2).send(Mockito.any(Message.class));
		FailoverClientConnectionFactory failoverFactory = new FailoverClientConnectionFactory(factories);
		failoverFactory.start();
		GenericMessage<String> message = new GenericMessage<String>("foo");
		assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() ->
			failoverFactory.getConnection().send(message));
		Mockito.verify(conn2).send(message);
	}

	@Test
	void failoverAllDeadAfterSuccess() throws Exception {
		ServerSocket ss1 = ServerSocketFactory.getDefault().createServerSocket(0);
		ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
		exec.initialize();
		exec.submit(() -> {
			Socket accepted = ss1.accept();
			BufferedReader br = new BufferedReader(new InputStreamReader(accepted.getInputStream()));
			br.readLine();
			accepted.getOutputStream().write("ok\r\n".getBytes());
			accepted.close();
			ss1.close();
			return true;
		});
		TcpNetClientConnectionFactory cf1 = new TcpNetClientConnectionFactory("localhost", ss1.getLocalPort());
		AbstractClientConnectionFactory cf2 = mock(AbstractClientConnectionFactory.class);
		doThrow(new UncheckedIOException(new IOException("fail"))).when(cf2).getConnection();
		CountDownLatch latch = new CountDownLatch(2);
		cf1.setApplicationEventPublisher(event -> {
			if (event instanceof TcpConnectionCloseEvent) {
				latch.countDown();
			}
		});
		cf2.setApplicationEventPublisher(event -> { });
		FailoverClientConnectionFactory fccf = new FailoverClientConnectionFactory(List.of(cf1, cf2));
		fccf.registerListener(msf -> {
			latch.countDown();
			return false;
		});
		fccf.start();
		fccf.getConnection().send(new GenericMessage<>("test"));
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() ->
				fccf.getConnection().send(new GenericMessage<>("test")));
	}

	@Test
	public void testFailoverAllDeadButOriginalOkAgain() throws Exception {
		AbstractClientConnectionFactory factory1 = mock(AbstractClientConnectionFactory.class);
		AbstractClientConnectionFactory factory2 = mock(AbstractClientConnectionFactory.class);
		List<AbstractClientConnectionFactory> factories = new ArrayList<AbstractClientConnectionFactory>();
		factories.add(factory1);
		factories.add(factory2);
		TcpConnectionSupport conn1 = makeMockConnection();
		TcpConnectionSupport conn2 = makeMockConnection();
		when(factory1.getConnection()).thenReturn(conn1);
		when(factory2.getConnection()).thenReturn(conn2);
		when(factory1.isActive()).thenReturn(true);
		when(factory2.isActive()).thenReturn(true);
		final AtomicBoolean failedOnce = new AtomicBoolean();
		doAnswer(invocation -> {
			if (!failedOnce.get()) {
				failedOnce.set(true);
				throw new UncheckedIOException(new IOException("fail"));
			}
			return null;
		}).when(conn1).send(Mockito.any(Message.class));
		doThrow(new UncheckedIOException(new IOException("fail")))
			.when(conn2).send(Mockito.any(Message.class));
		FailoverClientConnectionFactory failoverFactory = new FailoverClientConnectionFactory(factories);
		failoverFactory.start();
		GenericMessage<String> message = new GenericMessage<String>("foo");
		failoverFactory.getConnection().send(message);
		Mockito.verify(conn2).send(message);
		Mockito.verify(conn1, times(2)).send(message);
	}

	@Test
	public void testFailoverConnectNone() throws Exception {
		AbstractClientConnectionFactory factory1 = mock(AbstractClientConnectionFactory.class);
		AbstractClientConnectionFactory factory2 = mock(AbstractClientConnectionFactory.class);
		List<AbstractClientConnectionFactory> factories = new ArrayList<AbstractClientConnectionFactory>();
		factories.add(factory1);
		factories.add(factory2);
		when(factory1.getConnection()).thenThrow(new UncheckedIOException(new IOException("fail")));
		when(factory2.getConnection()).thenThrow(new UncheckedIOException(new IOException("fail")));
		when(factory1.isActive()).thenReturn(true);
		when(factory2.isActive()).thenReturn(true);
		FailoverClientConnectionFactory failoverFactory = new FailoverClientConnectionFactory(factories);
		failoverFactory.start();
		GenericMessage<String> message = new GenericMessage<String>("foo");
		assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() ->
			failoverFactory.getConnection().send(message));
	}

	@Test
	public void testFailoverConnectToFirstAfterTriedAll() throws Exception {
		AbstractClientConnectionFactory factory1 = mock(AbstractClientConnectionFactory.class);
		AbstractClientConnectionFactory factory2 = mock(AbstractClientConnectionFactory.class);
		List<AbstractClientConnectionFactory> factories = new ArrayList<AbstractClientConnectionFactory>();
		factories.add(factory1);
		factories.add(factory2);
		TcpConnectionSupport conn1 = makeMockConnection();
		doAnswer(invocation -> null).when(conn1).send(Mockito.any(Message.class));
		when(factory1.getConnection())
			.thenThrow(new UncheckedIOException(new IOException("fail")))
			.thenReturn(conn1);
		when(factory2.getConnection())
			.thenThrow(new UncheckedIOException(new IOException("fail")));
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
		TcpConnectionSupport conn1 = makeMockConnection();
		TcpConnectionSupport conn2 = makeMockConnection();
		when(factory1.getConnection()).thenReturn(conn1);
		when(factory2.getConnection()).thenReturn(conn2);
		when(factory1.isActive()).thenReturn(true);
		when(factory2.isActive()).thenReturn(true);
		final AtomicInteger failCount = new AtomicInteger();
		doAnswer(invocation -> {
			if (failCount.incrementAndGet() < 3) {
				throw new UncheckedIOException(new IOException("fail"));
			}
			return null;
		}).when(conn1).send(Mockito.any(Message.class));
		doThrow(new UncheckedIOException(new IOException("fail")))
			.when(conn2).send(Mockito.any(Message.class));
		FailoverClientConnectionFactory failoverFactory = new FailoverClientConnectionFactory(factories);
		failoverFactory.start();
		GenericMessage<String> message = new GenericMessage<String>("foo");
		assertThatExceptionOfType(UncheckedIOException.class)
			.isThrownBy(() -> failoverFactory.getConnection().send(message));
		failoverFactory.getConnection().send(message);
		Mockito.verify(conn2).send(message);
		Mockito.verify(conn1, times(3)).send(message);
	}

	public TcpConnectionSupport makeMockConnection() {
		TcpConnectionSupport connection = mock(TcpConnectionSupport.class);
		when(connection.isOpen()).thenReturn(true);
		return connection;
	}

	@Test
	public void testRealNet() throws Exception {
		AbstractServerConnectionFactory server1 = new TcpNetServerConnectionFactory(0);
		AbstractServerConnectionFactory server2 = new TcpNetServerConnectionFactory(0);

		Holder holder = setupAndStartServers(server1, server2);

		AbstractClientConnectionFactory client1 = new TcpNetClientConnectionFactory("localhost", server1.getPort());
		AbstractClientConnectionFactory client2 = new TcpNetClientConnectionFactory("localhost", server2.getPort());
		testRealGuts(client1, client2, holder);
	}

	@Test
	public void testRealNio() throws Exception {

		AbstractServerConnectionFactory server1 = new TcpNioServerConnectionFactory(0);
		AbstractServerConnectionFactory server2 = new TcpNioServerConnectionFactory(0);

		Holder holder = setupAndStartServers(server1, server2);

		AbstractClientConnectionFactory client1 = new TcpNioClientConnectionFactory("localhost", server1.getPort());
		AbstractClientConnectionFactory client2 = new TcpNioClientConnectionFactory("localhost", server2.getPort());
		testRealGuts(client1, client2, holder);
	}

	@Test
	public void testRealNetSingleUse() throws Exception {

		AbstractServerConnectionFactory server1 = new TcpNetServerConnectionFactory(0);
		AbstractServerConnectionFactory server2 = new TcpNetServerConnectionFactory(0);

		Holder holder = setupAndStartServers(server1, server2);

		AbstractClientConnectionFactory client1 = new TcpNetClientConnectionFactory("localhost", server1.getPort());
		AbstractClientConnectionFactory client2 = new TcpNetClientConnectionFactory("localhost", server2.getPort());
		client1.setSingleUse(true);
		client2.setSingleUse(true);
		testRealGuts(client1, client2, holder);
	}

	@Test
	public void testRealNioSingleUse() throws Exception {

		AbstractServerConnectionFactory server1 = new TcpNioServerConnectionFactory(0);
		AbstractServerConnectionFactory server2 = new TcpNioServerConnectionFactory(0);

		Holder holder = setupAndStartServers(server1, server2);

		AbstractClientConnectionFactory client1 = new TcpNioClientConnectionFactory("localhost", server1.getPort());
		AbstractClientConnectionFactory client2 = new TcpNioClientConnectionFactory("localhost", server2.getPort());
		client1.setSingleUse(true);
		client2.setSingleUse(true);
		testRealGuts(client1, client2, holder);
	}

	@Test
	public void testFailoverCachedRealClose() throws Exception {
		TcpNetServerConnectionFactory server1 = new TcpNetServerConnectionFactory(0);
		server1.setBeanName("server1");
		final CountDownLatch latch1 = new CountDownLatch(3);
		server1.registerListener(message -> {
			latch1.countDown();
			return false;
		});
		server1.start();
		TestingUtilities.waitListening(server1, 10000L);
		int port1 = server1.getPort();
		TcpNetServerConnectionFactory server2 = new TcpNetServerConnectionFactory(0);
		server2.setBeanName("server2");
		final CountDownLatch latch2 = new CountDownLatch(2);
		server2.registerListener(message -> {
			latch2.countDown();
			return false;
		});
		server2.start();
		TestingUtilities.waitListening(server2, 10000L);
		int port2 = server2.getPort();
		AbstractClientConnectionFactory factory1 = new TcpNetClientConnectionFactory("localhost", port1);
		factory1.setBeanName("client1");
		factory1.registerListener(message -> false);
		AbstractClientConnectionFactory factory2 = new TcpNetClientConnectionFactory("localhost", port2);
		factory2.setBeanName("client2");
		factory2.registerListener(message -> false);
		// Cache
		CachingClientConnectionFactory cachingFactory1 = new CachingClientConnectionFactory(factory1, 2);
		cachingFactory1.setBeanName("cache1");
		CachingClientConnectionFactory cachingFactory2 = new CachingClientConnectionFactory(factory2, 2);
		cachingFactory2.setBeanName("cache2");

		// Failover
		List<AbstractClientConnectionFactory> factories = new ArrayList<AbstractClientConnectionFactory>();
		factories.add(cachingFactory1);
		factories.add(cachingFactory2);
		FailoverClientConnectionFactory failoverFactory = new FailoverClientConnectionFactory(factories);

		failoverFactory.start();
		TcpConnection conn1 = failoverFactory.getConnection();
		conn1.send(new GenericMessage<String>("foo1"));
		conn1.close();
		TcpConnection conn2 = failoverFactory.getConnection();
		assertThat((TestUtils.getPropertyValue(conn2, "delegate", TcpConnectionInterceptorSupport.class))
				.getTheConnection())
				.isSameAs((TestUtils.getPropertyValue(conn1, "delegate", TcpConnectionInterceptorSupport.class))
						.getTheConnection());
		conn2.send(new GenericMessage<String>("foo2"));
		conn1 = failoverFactory.getConnection();
		assertThat((TestUtils.getPropertyValue(conn2, "delegate", TcpConnectionInterceptorSupport.class))
				.getTheConnection())
				.isNotSameAs((TestUtils.getPropertyValue(conn1, "delegate", TcpConnectionInterceptorSupport.class))
						.getTheConnection());
		conn1.send(new GenericMessage<String>("foo3"));
		conn1.close();
		conn2.close();
		assertThat(latch1.await(10, TimeUnit.SECONDS)).isTrue();
		server1.stop();
		TestingUtilities.waitStopListening(server1, 10000L);
		TestingUtilities.waitUntilFactoryHasThisNumberOfConnections(factory1, 0);
		conn1 = failoverFactory.getConnection();
		conn2 = failoverFactory.getConnection();
		conn1.send(new GenericMessage<String>("foo4"));
		conn2.send(new GenericMessage<String>("foo5"));
		conn1.close();
		conn2.close();
		assertThat(latch2.await(10, TimeUnit.SECONDS)).isTrue();
		SimplePool<?> pool = TestUtils.getPropertyValue(cachingFactory2, "pool", SimplePool.class);
		assertThat(pool.getIdleCount()).isEqualTo(2);
		server2.stop();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testFailoverCachedWithGateway() throws Exception {
		final TcpNetServerConnectionFactory server = new TcpNetServerConnectionFactory(0);
		server.setBeanName("server");
		server.afterPropertiesSet();
		DirectChannel inChannel = new DirectChannel();
		inChannel.setBeanName("inChannel");
		TcpInboundGateway inbound = new TcpInboundGateway();
		inbound.setConnectionFactory(server);
		inbound.setRequestChannel(inChannel);
		inbound.afterPropertiesSet();
		inChannel.subscribe(new BridgeHandler());
		inbound.start();
		TestingUtilities.waitListening(server, 10000L);
		int port = server.getPort();
		AbstractClientConnectionFactory client = new TcpNetClientConnectionFactory("localhost", port);
		client.setBeanName("client");

		// Cache
		CachingClientConnectionFactory cachingClient = new CachingClientConnectionFactory(client, 2);
		cachingClient.setBeanName("cache");
		cachingClient.afterPropertiesSet();

		// Failover
		List<AbstractClientConnectionFactory> clientFactories = new ArrayList<AbstractClientConnectionFactory>();
		clientFactories.add(cachingClient);
		FailoverClientConnectionFactory failoverClient = new FailoverClientConnectionFactory(clientFactories);
		failoverClient.setSingleUse(true);
		failoverClient.afterPropertiesSet();

		TcpOutboundGateway outbound = new TcpOutboundGateway();
		outbound.setConnectionFactory(failoverClient);
		QueueChannel replyChannel = new QueueChannel();
		replyChannel.setBeanName("replyChannel");
		outbound.setReplyChannel(replyChannel);
		outbound.setBeanFactory(mock(BeanFactory.class));
		outbound.afterPropertiesSet();
		outbound.start();

		outbound.handleMessage(new GenericMessage<String>("foo"));
		Message<byte[]> result = (Message<byte[]>) replyChannel.receive(10000);
		assertThat(result).isNotNull();
		assertThat(new String(result.getPayload())).isEqualTo("foo");

		// INT-4024 - second reply had bad connection id
		outbound.handleMessage(new GenericMessage<String>("foo"));
		result = (Message<byte[]>) replyChannel.receive(10000);
		assertThat(result).isNotNull();
		assertThat(new String(result.getPayload())).isEqualTo("foo");

		inbound.stop();
		outbound.stop();
	}

	@Test
	public void testFailoverCachedRealBadHost() throws Exception {
		TcpNetServerConnectionFactory server1 = new TcpNetServerConnectionFactory(0);
		server1.setBeanName("server1");
		final CountDownLatch latch1 = new CountDownLatch(3);
		server1.registerListener(message -> {
			latch1.countDown();
			return false;
		});
		server1.start();
		TestingUtilities.waitListening(server1, 10000L);
		int port1 = server1.getPort();
		TcpNetServerConnectionFactory server2 = new TcpNetServerConnectionFactory(0);
		server2.setBeanName("server2");
		final CountDownLatch latch2 = new CountDownLatch(2);
		server2.registerListener(message -> {
			latch2.countDown();
			return false;
		});
		server2.start();
		TestingUtilities.waitListening(server2, 10000L);
		int port2 = server2.getPort();

		AbstractClientConnectionFactory factory1 = new TcpNetClientConnectionFactory("junkjunk", port1);
		factory1.setBeanName("client1");
		factory1.registerListener(message -> false);
		AbstractClientConnectionFactory factory2 = new TcpNetClientConnectionFactory("localhost", port2);
		factory2.setBeanName("client2");
		factory2.registerListener(message -> false);

		// Cache
		CachingClientConnectionFactory cachingFactory1 = new CachingClientConnectionFactory(factory1, 2);
		cachingFactory1.setBeanName("cache1");
		CachingClientConnectionFactory cachingFactory2 = new CachingClientConnectionFactory(factory2, 2);
		cachingFactory2.setBeanName("cache2");

		// Failover
		List<AbstractClientConnectionFactory> factories = new ArrayList<AbstractClientConnectionFactory>();
		factories.add(cachingFactory1);
		factories.add(cachingFactory2);
		FailoverClientConnectionFactory failoverFactory = new FailoverClientConnectionFactory(factories);
		failoverFactory.start();
		TcpConnection conn1 = failoverFactory.getConnection();
		GenericMessage<String> message = new GenericMessage<String>("foo");
		conn1.send(message);
		conn1.close();
		TcpConnection conn2 = failoverFactory.getConnection();
		assertThat((TestUtils.getPropertyValue(conn2, "delegate", TcpConnectionInterceptorSupport.class))
				.getTheConnection())
				.isSameAs((TestUtils.getPropertyValue(conn1, "delegate", TcpConnectionInterceptorSupport.class))
						.getTheConnection());
		conn2.send(message);
		conn1 = failoverFactory.getConnection();
		assertThat((TestUtils.getPropertyValue(conn2, "delegate", TcpConnectionInterceptorSupport.class))
				.getTheConnection())
				.isNotSameAs((TestUtils.getPropertyValue(conn1, "delegate", TcpConnectionInterceptorSupport.class))
						.getTheConnection());
		conn1.send(message);
		conn1.close();
		conn2.close();
		assertThat(latch2.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(latch1.getCount()).isEqualTo(3);
		server1.stop();
		server2.stop();
	}

	private void testRealGuts(AbstractClientConnectionFactory client1, AbstractClientConnectionFactory client2,
			Holder holder) throws Exception {
		int port1 = 0;
		int port2 = 0;

		client1.setTaskExecutor(holder.exec);
		client2.setTaskExecutor(holder.exec);
		client1.setBeanName("client1");
		client2.setBeanName("client2");
		client1.setApplicationEventPublisher(NULL_PUBLISHER);
		client2.setApplicationEventPublisher(NULL_PUBLISHER);
		List<AbstractClientConnectionFactory> factories = new ArrayList<AbstractClientConnectionFactory>();
		factories.add(client1);
		factories.add(client2);
		FailoverClientConnectionFactory failFactory = new FailoverClientConnectionFactory(factories);
		boolean singleUse = client1.isSingleUse();
		failFactory.setSingleUse(singleUse);
		failFactory.setBeanFactory(mock(BeanFactory.class));
		failFactory.afterPropertiesSet();
		TcpOutboundGateway outGateway = new TcpOutboundGateway();
		outGateway.setConnectionFactory(failFactory);
		outGateway.start();
		QueueChannel replyChannel = new QueueChannel();
		outGateway.setReplyChannel(replyChannel);
		Message<String> message = new GenericMessage<String>("foo");
		outGateway.setRemoteTimeout(120000);
		outGateway.handleMessage(message);
		Socket socket = null;
		if (!singleUse) {
			socket = getSocket(client1);
			port1 = socket.getLocalPort();
		}
		assertThat(singleUse | holder.connectionId.get().contains(Integer.toString(port1))).isTrue();
		Message<?> replyMessage = replyChannel.receive(10000);
		assertThat(replyMessage).isNotNull();
		holder.server1.stop();
		TestingUtilities.waitStopListening(holder.server1, 10000L);
		TestingUtilities.waitUntilFactoryHasThisNumberOfConnections(client1, 0);
		outGateway.handleMessage(message);
		if (!singleUse) {
			socket = getSocket(client2);
			port2 = socket.getLocalPort();
		}
		assertThat(singleUse | holder.connectionId.get().contains(Integer.toString(port2))).isTrue();
		replyMessage = replyChannel.receive(10000);
		assertThat(replyMessage).isNotNull();
		holder.gateway2.stop();
		outGateway.stop();
	}

	private Holder setupAndStartServers(AbstractServerConnectionFactory server1,
			AbstractServerConnectionFactory server2) {

		Executor exec = new SimpleAsyncTaskExecutor("FailoverClientConnectionFactoryTests-");
		server1.setTaskExecutor(exec);
		server2.setTaskExecutor(exec);
		server1.setBeanName("server1");
		server2.setBeanName("server2");
		server1.setApplicationEventPublisher(NULL_PUBLISHER);
		server2.setApplicationEventPublisher(NULL_PUBLISHER);
		TcpInboundGateway gateway1 = new TcpInboundGateway();
		gateway1.setConnectionFactory(server1);
		SubscribableChannel channel = new DirectChannel();
		final AtomicReference<String> connectionId = new AtomicReference<String>();
		channel.subscribe(message -> {
			connectionId.set((String) message.getHeaders().get(IpHeaders.CONNECTION_ID));
			((MessageChannel) message.getHeaders().getReplyChannel()).send(message);
		});
		gateway1.setRequestChannel(channel);
		gateway1.setBeanFactory(mock(BeanFactory.class));
		gateway1.afterPropertiesSet();
		gateway1.start();
		TcpInboundGateway gateway2 = new TcpInboundGateway();
		gateway2.setConnectionFactory(server2);
		gateway2.setRequestChannel(channel);
		gateway2.setBeanFactory(mock(BeanFactory.class));
		gateway2.afterPropertiesSet();
		gateway2.start();
		TestingUtilities.waitListening(server1, null);
		TestingUtilities.waitListening(server2, null);
		Holder holder = new Holder();
		holder.exec = exec;
		holder.connectionId = connectionId;
		holder.server1 = server1;
		holder.gateway2 = gateway2;
		return holder;
	}

	private Socket getSocket(AbstractClientConnectionFactory client) throws Exception {
		if (client instanceof TcpNetClientConnectionFactory) {
			return TestUtils.getPropertyValue(client.getConnection(), "socket", Socket.class);
		}
		else {
			return TestUtils.getPropertyValue(client.getConnection(), "socketChannel", SocketChannel.class).socket();
		}
	}

	private static class Holder {

		private AtomicReference<String> connectionId;

		private Executor exec;

		private TcpInboundGateway gateway2;

		private AbstractServerConnectionFactory server1;

	}

}

