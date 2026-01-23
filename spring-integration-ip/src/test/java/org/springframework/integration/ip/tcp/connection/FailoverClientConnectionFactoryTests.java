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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.BridgeHandler;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.tcp.inbound.TcpInboundGateway;
import org.springframework.integration.ip.tcp.outbound.TcpOutboundGateway;
import org.springframework.integration.ip.util.TestingUtilities;
import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.util.SimplePool;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

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

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Gengwu Zhao
 * @author Glenn Renfro
 *
 * @since 2.2
 *
 */
public class FailoverClientConnectionFactoryTests implements TestApplicationContextAware {

	@Test
	public void testFailoverGood() throws Exception {
		TcpConnectionSupport conn1 = makeMockConnection();
		TcpConnectionSupport conn2 = makeMockConnection();
		AbstractClientConnectionFactory factory1 = createFactoryWithMockConnection(conn1);
		AbstractClientConnectionFactory factory2 = createFactoryWithMockConnection(conn2);
		List<AbstractClientConnectionFactory> factories = new ArrayList<>();
		factories.add(factory1);
		factories.add(factory2);
		doThrow(new UncheckedIOException(new IOException("fail")))
				.when(conn1).send(Mockito.any(Message.class));
		doAnswer(invocation -> null).when(conn2).send(Mockito.any(Message.class));
		FailoverClientConnectionFactory failoverFactory = new FailoverClientConnectionFactory(factories);
		failoverFactory.start();
		GenericMessage<String> message = new GenericMessage<>("foo");
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
		List<AbstractClientConnectionFactory> factories = new ArrayList<>();
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
		assertThat(TestUtils.<Object>getPropertyValue(failoverFactory, "theConnection")).isNotNull();
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
		TcpConnectionSupport conn1 = makeMockConnection();
		TcpConnectionSupport conn2 = makeMockConnection();
		AbstractClientConnectionFactory factory1 = createFactoryWithMockConnection(conn1);
		AbstractClientConnectionFactory factory2 = createFactoryWithMockConnection(conn2);
		List<AbstractClientConnectionFactory> factories = new ArrayList<>();
		factories.add(factory1);
		factories.add(factory2);
		doThrow(new UncheckedIOException(new IOException("fail")))
				.when(conn1).send(Mockito.any(Message.class));
		doThrow(new UncheckedIOException(new IOException("fail")))
				.when(conn2).send(Mockito.any(Message.class));
		FailoverClientConnectionFactory failoverFactory = new FailoverClientConnectionFactory(factories);
		failoverFactory.start();
		GenericMessage<String> message = new GenericMessage<>("foo");
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
		CountDownLatch latch = new CountDownLatch(1);
		cf1.setApplicationEventPublisher(event -> {
			if (event instanceof TcpConnectionCloseEvent) {
				latch.countDown();
			}
		});
		cf1.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		cf1.afterPropertiesSet();
		cf2.setApplicationEventPublisher(event -> {
		});
		FailoverClientConnectionFactory fccf = new FailoverClientConnectionFactory(List.of(cf1, cf2));

		CompletableFuture<Message<?>> messageCompletableFuture = new CompletableFuture<>();
		fccf.registerListener(value -> messageCompletableFuture.complete(value));

		fccf.start();
		fccf.getConnection().send(new GenericMessage<>("test"));
		assertThat(messageCompletableFuture)
				.succeedsWithin(10, TimeUnit.SECONDS)
				.extracting(Message::getPayload)
				.isEqualTo("ok".getBytes());
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() ->
				fccf.getConnection().send(new GenericMessage<>("test")));
	}

	@Test
	public void testFailoverAllDeadButOriginalOkAgain() throws Exception {
		TcpConnectionSupport conn1 = makeMockConnection();
		TcpConnectionSupport conn2 = makeMockConnection();
		AbstractClientConnectionFactory factory1 = createFactoryWithMockConnection(conn1);
		AbstractClientConnectionFactory factory2 = createFactoryWithMockConnection(conn2);
		List<AbstractClientConnectionFactory> factories = new ArrayList<>();
		factories.add(factory1);
		factories.add(factory2);
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
		GenericMessage<String> message = new GenericMessage<>("foo");
		failoverFactory.getConnection().send(message);
		Mockito.verify(conn2).send(message);
		Mockito.verify(conn1, times(2)).send(message);
	}

	@Test
	public void testFailoverConnectNone() throws Exception {
		AbstractClientConnectionFactory factory1 = mock(AbstractClientConnectionFactory.class);
		AbstractClientConnectionFactory factory2 = mock(AbstractClientConnectionFactory.class);
		List<AbstractClientConnectionFactory> factories = new ArrayList<>();
		factories.add(factory1);
		factories.add(factory2);
		when(factory1.getConnection()).thenThrow(new UncheckedIOException(new IOException("fail")));
		when(factory2.getConnection()).thenThrow(new UncheckedIOException(new IOException("fail")));
		when(factory1.isActive()).thenReturn(true);
		when(factory2.isActive()).thenReturn(true);
		FailoverClientConnectionFactory failoverFactory = new FailoverClientConnectionFactory(factories);
		failoverFactory.start();
		GenericMessage<String> message = new GenericMessage<>("foo");
		assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() ->
				failoverFactory.getConnection().send(message));
	}

	@Test
	public void testFailoverConnectToFirstAfterTriedAll() throws Exception {
		AbstractClientConnectionFactory factory1 = mock(AbstractClientConnectionFactory.class);
		AbstractClientConnectionFactory factory2 = mock(AbstractClientConnectionFactory.class);
		List<AbstractClientConnectionFactory> factories = new ArrayList<>();
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
		TcpConnectionSupport conn1 = makeMockConnection();
		TcpConnectionSupport conn2 = makeMockConnection();
		AbstractClientConnectionFactory factory1 = createFactoryWithMockConnection(conn1);
		AbstractClientConnectionFactory factory2 = createFactoryWithMockConnection(conn2);
		List<AbstractClientConnectionFactory> factories = new ArrayList<>();
		factories.add(factory1);
		factories.add(factory2);
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
		GenericMessage<String> message = new GenericMessage<>("foo");
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
		AbstractServerConnectionFactory server1 = getTcpNetServerConnectionFactory(0);
		AbstractServerConnectionFactory server2 = getTcpNetServerConnectionFactory(0);

		Holder holder = setupAndStartServers(server1, server2);

		AbstractClientConnectionFactory client1 = getTcpNetServerConnectionFactory("localhost", server1.getPort());
		AbstractClientConnectionFactory client2 = getTcpNetServerConnectionFactory("localhost", server2.getPort());
		testRealGuts(client1, client2, holder);
	}

	@Test
	public void testRealNio() throws Exception {

		AbstractServerConnectionFactory server1 = getTcpNetServerConnectionFactory(0);
		AbstractServerConnectionFactory server2 = getTcpNetServerConnectionFactory(0);

		Holder holder = setupAndStartServers(server1, server2);

		AbstractClientConnectionFactory client1 = getTcpNetServerConnectionFactory("localhost", server1.getPort());
		AbstractClientConnectionFactory client2 = getTcpNetServerConnectionFactory("localhost", server2.getPort());
		testRealGuts(client1, client2, holder);
	}

	@Test
	public void testRealNetSingleUse() throws Exception {

		AbstractServerConnectionFactory server1 = getTcpNetServerConnectionFactory(0);
		AbstractServerConnectionFactory server2 = getTcpNetServerConnectionFactory(0);

		Holder holder = setupAndStartServers(server1, server2);

		AbstractClientConnectionFactory client1 = getTcpNetServerConnectionFactory("localhost", server1.getPort());
		AbstractClientConnectionFactory client2 = getTcpNetServerConnectionFactory("localhost", server2.getPort());
		client1.setSingleUse(true);
		client2.setSingleUse(true);
		testRealGuts(client1, client2, holder);
	}

	@Test
	public void testRealNioSingleUse() throws Exception {

		AbstractServerConnectionFactory server1 = getTcpNetServerConnectionFactory(0);
		AbstractServerConnectionFactory server2 = getTcpNetServerConnectionFactory(0);

		Holder holder = setupAndStartServers(server1, server2);

		AbstractClientConnectionFactory client1 = getTcpNetServerConnectionFactory("localhost", server1.getPort());
		AbstractClientConnectionFactory client2 = getTcpNetServerConnectionFactory("localhost", server2.getPort());
		client1.setSingleUse(true);
		client2.setSingleUse(true);
		testRealGuts(client1, client2, holder);
	}

	@Test
	public void testFailoverCachedRealClose() throws Exception {
		TcpNetServerConnectionFactory server1 = new TcpNetServerConnectionFactory(0);
		server1.setBeanName("server1");
		server1.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		server1.setApplicationEventPublisher(TEST_INTEGRATION_CONTEXT);
		final CountDownLatch latch1 = new CountDownLatch(3);
		server1.registerListener(message -> latch1.countDown());
		server1.afterPropertiesSet();
		server1.start();
		TestingUtilities.waitListening(server1, 10000L);
		int port1 = server1.getPort();
		TcpNetServerConnectionFactory server2 = new TcpNetServerConnectionFactory(0);
		server2.setBeanName("server2");
		server2.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		server2.setApplicationEventPublisher(TEST_INTEGRATION_CONTEXT);
		final CountDownLatch latch2 = new CountDownLatch(2);
		server2.registerListener(message -> latch2.countDown());
		server2.afterPropertiesSet();
		server2.start();
		TestingUtilities.waitListening(server2, 10000L);
		int port2 = server2.getPort();
		AbstractClientConnectionFactory factory1 = new TcpNetClientConnectionFactory("localhost", port1);
		factory1.setBeanName("client1");
		factory1.registerListener(message -> {
		});
		AbstractClientConnectionFactory factory2 = new TcpNetClientConnectionFactory("localhost", port2);
		factory2.setBeanName("client2");
		factory2.registerListener(message -> {
		});
		// Cache
		CachingClientConnectionFactory cachingFactory1 = new CachingClientConnectionFactory(factory1, 2);
		cachingFactory1.setBeanName("cache1");
		CachingClientConnectionFactory cachingFactory2 = new CachingClientConnectionFactory(factory2, 2);
		cachingFactory2.setBeanName("cache2");

		// Failover
		List<AbstractClientConnectionFactory> factories = new ArrayList<>();
		factories.add(cachingFactory1);
		factories.add(cachingFactory2);
		FailoverClientConnectionFactory failoverFactory = new FailoverClientConnectionFactory(factories);

		failoverFactory.start();
		TcpConnection conn1 = failoverFactory.getConnection();
		conn1.send(new GenericMessage<>("foo1"));
		conn1.close();
		TcpConnection conn2 = failoverFactory.getConnection();
		assertThat((TestUtils.<TcpConnectionInterceptorSupport>getPropertyValue(conn2, "delegate"))
				.getTheConnection())
				.isSameAs((TestUtils.<TcpConnectionInterceptorSupport>getPropertyValue(conn1, "delegate"))
						.getTheConnection());
		conn2.send(new GenericMessage<>("foo2"));
		conn1 = failoverFactory.getConnection();
		assertThat((TestUtils.<TcpConnectionInterceptorSupport>getPropertyValue(conn2, "delegate"))
				.getTheConnection())
				.isNotSameAs((TestUtils.<TcpConnectionInterceptorSupport>getPropertyValue(conn1, "delegate"))
						.getTheConnection());
		conn1.send(new GenericMessage<>("foo3"));
		conn1.close();
		conn2.close();
		assertThat(latch1.await(10, TimeUnit.SECONDS)).isTrue();
		server1.stop();
		TestingUtilities.waitStopListening(server1, 10000L);
		TestingUtilities.waitUntilFactoryHasThisNumberOfConnections(factory1, 0);
		conn1 = failoverFactory.getConnection();
		conn2 = failoverFactory.getConnection();
		conn1.send(new GenericMessage<>("foo4"));
		conn2.send(new GenericMessage<>("foo5"));
		conn1.close();
		conn2.close();
		assertThat(latch2.await(10, TimeUnit.SECONDS)).isTrue();
		SimplePool<?> pool = TestUtils.getPropertyValue(cachingFactory2, "pool");
		assertThat(pool.getIdleCount()).isEqualTo(2);
		server2.stop();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testFailoverCachedWithGateway() {
		final TcpNetServerConnectionFactory server = new TcpNetServerConnectionFactory(0);
		server.setBeanName("server");
		server.setApplicationEventPublisher(TEST_INTEGRATION_CONTEXT);
		server.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		server.afterPropertiesSet();
		DirectChannel inChannel = new DirectChannel();
		inChannel.setBeanName("inChannel");
		TcpInboundGateway inbound = new TcpInboundGateway();
		inbound.setConnectionFactory(server);
		inbound.setRequestChannel(inChannel);
		inbound.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		inbound.afterPropertiesSet();
		inChannel.subscribe(new BridgeHandler());
		inbound.start();
		TestingUtilities.waitListening(server, 10000L);
		int port = server.getPort();
		AbstractClientConnectionFactory client = new TcpNetClientConnectionFactory("localhost", port);
		client.setBeanName("client");
		client.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		client.setApplicationEventPublisher(TEST_INTEGRATION_CONTEXT);
		client.afterPropertiesSet();

		// Cache
		CachingClientConnectionFactory cachingClient = new CachingClientConnectionFactory(client, 2);
		cachingClient.setBeanName("cache");
		cachingClient.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		cachingClient.afterPropertiesSet();

		// Failover
		List<AbstractClientConnectionFactory> clientFactories = new ArrayList<>();
		clientFactories.add(cachingClient);
		FailoverClientConnectionFactory failoverClient = new FailoverClientConnectionFactory(clientFactories);
		failoverClient.setSingleUse(true);
		failoverClient.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		failoverClient.afterPropertiesSet();

		TcpOutboundGateway outbound = new TcpOutboundGateway();
		outbound.setConnectionFactory(failoverClient);
		QueueChannel replyChannel = new QueueChannel();
		replyChannel.setBeanName("replyChannel");
		outbound.setReplyChannel(replyChannel);
		outbound.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		outbound.afterPropertiesSet();
		outbound.start();

		outbound.handleMessage(new GenericMessage<>("foo"));
		Message<byte[]> result = (Message<byte[]>) replyChannel.receive(10000);
		assertThat(result).isNotNull();
		assertThat(new String(result.getPayload())).isEqualTo("foo");

		// INT-4024 - second reply had bad connection id
		outbound.handleMessage(new GenericMessage<>("foo"));
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
		server1.setApplicationEventPublisher(TEST_INTEGRATION_CONTEXT);
		server1.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		final CountDownLatch latch1 = new CountDownLatch(3);
		server1.registerListener(message -> latch1.countDown());
		server1.afterPropertiesSet();
		server1.start();
		TestingUtilities.waitListening(server1, 10000L);
		int port1 = server1.getPort();

		TcpNetServerConnectionFactory server2 = new TcpNetServerConnectionFactory(0);
		server2.setBeanName("server2");
		server2.setApplicationEventPublisher(TEST_INTEGRATION_CONTEXT);
		server2.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		final CountDownLatch latch2 = new CountDownLatch(2);
		server2.registerListener(message -> latch2.countDown());
		server2.afterPropertiesSet();
		server2.start();
		TestingUtilities.waitListening(server2, 10000L);
		int port2 = server2.getPort();

		AbstractClientConnectionFactory factory1 = new TcpNetClientConnectionFactory("junkjunk", port1);
		factory1.setBeanName("client1");
		factory1.registerListener(message -> {
		});
		factory1.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		factory1.setApplicationEventPublisher(TEST_INTEGRATION_CONTEXT);
		factory1.afterPropertiesSet();

		AbstractClientConnectionFactory factory2 = new TcpNetClientConnectionFactory("localhost", port2);
		factory2.setBeanName("client2");
		factory2.registerListener(message -> {
		});
		factory2.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		factory2.setApplicationEventPublisher(TEST_INTEGRATION_CONTEXT);
		factory2.afterPropertiesSet();

		// Cache
		CachingClientConnectionFactory cachingFactory1 = new CachingClientConnectionFactory(factory1, 2);
		cachingFactory1.setBeanName("cache1");
		CachingClientConnectionFactory cachingFactory2 = new CachingClientConnectionFactory(factory2, 2);
		cachingFactory2.setBeanName("cache2");

		// Failover
		List<AbstractClientConnectionFactory> factories = new ArrayList<>();
		factories.add(cachingFactory1);
		factories.add(cachingFactory2);
		FailoverClientConnectionFactory failoverFactory = new FailoverClientConnectionFactory(factories);
		failoverFactory.start();
		TcpConnection conn1 = failoverFactory.getConnection();
		GenericMessage<String> message = new GenericMessage<>("foo");
		conn1.send(message);
		conn1.close();
		TcpConnection conn2 = failoverFactory.getConnection();
		assertThat((TestUtils.<TcpConnectionInterceptorSupport>getPropertyValue(conn2, "delegate"))
				.getTheConnection())
				.isSameAs((TestUtils.<TcpConnectionInterceptorSupport>getPropertyValue(conn1, "delegate"))
						.getTheConnection());
		conn2.send(message);
		conn1 = failoverFactory.getConnection();
		assertThat((TestUtils.<TcpConnectionInterceptorSupport>getPropertyValue(conn2, "delegate"))
				.getTheConnection())
				.isNotSameAs((TestUtils.<TcpConnectionInterceptorSupport>getPropertyValue(conn1, "delegate"))
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
		client1.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		client2.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		client1.setApplicationEventPublisher(event -> {
		});
		client2.setApplicationEventPublisher(event -> {
		});
		client1.afterPropertiesSet();
		client2.afterPropertiesSet();
		List<AbstractClientConnectionFactory> factories = new ArrayList<>();
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
		Message<String> message = new GenericMessage<>("foo");
		outGateway.setRemoteTimeout(120000);
		outGateway.handleMessage(message);
		Socket socket;
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
		server1.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		server2.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		server1.setApplicationEventPublisher(TEST_INTEGRATION_CONTEXT);
		server2.setApplicationEventPublisher(TEST_INTEGRATION_CONTEXT);
		server1.afterPropertiesSet();
		server2.afterPropertiesSet();
		TcpInboundGateway gateway1 = new TcpInboundGateway();
		gateway1.setConnectionFactory(server1);
		SubscribableChannel channel = new DirectChannel();
		final AtomicReference<String> connectionId = new AtomicReference<>();
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
			return TestUtils.<Socket>getPropertyValue(client.getConnection(), "socket");
		}
		else {
			return TestUtils.<SocketChannel>getPropertyValue(client.getConnection(), "socketChannel").socket();
		}
	}

	private static class Holder {

		private AtomicReference<String> connectionId;

		private Executor exec;

		private TcpInboundGateway gateway2;

		private AbstractServerConnectionFactory server1;

	}

	private static AbstractClientConnectionFactory createFactoryWithMockConnection(TcpConnectionSupport mockConn)
			throws Exception {

		AbstractClientConnectionFactory factory = mock(AbstractClientConnectionFactory.class);
		when(factory.getConnection()).thenReturn(mockConn);
		when(factory.isActive()).thenReturn(true);
		return factory;
	}

	private static TcpNetServerConnectionFactory getTcpNetServerConnectionFactory(int port) {
		TcpNetServerConnectionFactory result = new TcpNetServerConnectionFactory(port);
		result.setTaskScheduler(new SimpleAsyncTaskScheduler());

		return result;
	}

	private static TcpNetClientConnectionFactory getTcpNetServerConnectionFactory(String host, int port) {
		TcpNetClientConnectionFactory result = new TcpNetClientConnectionFactory(host, port);
		result.setTaskScheduler(new SimpleAsyncTaskScheduler());

		return result;
	}

}

