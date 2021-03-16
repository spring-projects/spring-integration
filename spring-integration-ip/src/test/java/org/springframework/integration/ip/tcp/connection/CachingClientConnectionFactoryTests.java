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
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.log.LogAccessor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.tcp.TcpOutboundGateway;
import org.springframework.integration.ip.tcp.TcpSendingMessageHandler;
import org.springframework.integration.ip.tcp.serializer.ByteArrayCrLfSerializer;
import org.springframework.integration.ip.util.TestingUtilities;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.condition.LogLevels;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.util.PoolItemNotAvailableException;
import org.springframework.integration.util.SimplePool;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.2
 *
 */
@SpringJUnitConfig
@DirtiesContext
@LogLevels(level = "trace", categories = "org.springframework.integration")
public class CachingClientConnectionFactoryTests {

	@Autowired
	SubscribableChannel outbound;

	@Autowired
	PollableChannel inbound;

	@Autowired
	AbstractServerConnectionFactory serverCf;

	@Autowired
	SubscribableChannel toGateway;

	@Autowired
	SubscribableChannel replies;

	@Autowired
	PollableChannel fromGateway;

	@Autowired
	@Qualifier("gateway.caching.ccf")
	private CachingClientConnectionFactory gatewayCF;

	@Autowired
	@Qualifier("gateway.ccf")
	private AbstractClientConnectionFactory clientGatewayCf;

	@Autowired
	@Qualifier("ccf")
	private AbstractClientConnectionFactory clientAdapterCf;

	@Test
	public void testReuse() throws Exception {
		AbstractClientConnectionFactory factory = mock(AbstractClientConnectionFactory.class);
		when(factory.isRunning()).thenReturn(true);
		TcpConnectionSupport mockConn1 = makeMockConnection("conn1");
		TcpConnectionSupport mockConn2 = makeMockConnection("conn2");
		when(factory.getConnection()).thenReturn(mockConn1).thenReturn(mockConn2);
		CachingClientConnectionFactory cachingFactory = new CachingClientConnectionFactory(factory, 2);
		cachingFactory.start();
		TcpConnection conn1 = cachingFactory.getConnection();
		// INT-3652
		TcpConnectionInterceptorSupport cachedConn1 = (TcpConnectionInterceptorSupport) conn1;
		Log logger = spy(TestUtils.getPropertyValue(cachedConn1, "logger", Log.class));
		when(logger.isDebugEnabled()).thenReturn(true);
		new DirectFieldAccessor(cachedConn1).setPropertyValue("logger", logger);
		cachedConn1.onMessage(new ErrorMessage(new RuntimeException()));
		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(logger).debug(captor.capture());
		assertThat(captor.getValue()).startsWith("Message discarded; no listener:");
		// end INT-3652
		assertThat(conn1.toString()).isEqualTo("Cached:" + mockConn1.toString());
		conn1.close();
		conn1 = cachingFactory.getConnection();
		assertThat(conn1.toString()).isEqualTo("Cached:" + mockConn1.toString());
		TcpConnection conn2 = cachingFactory.getConnection();
		assertThat(conn2.toString()).isEqualTo("Cached:" + mockConn2.toString());
		conn1.close();
		conn2.close();
	}

	@Test
	public void testReuseNoLimit() throws Exception {
		AbstractClientConnectionFactory factory = mock(AbstractClientConnectionFactory.class);
		when(factory.isRunning()).thenReturn(true);
		TcpConnectionSupport mockConn1 = makeMockConnection("conn1");
		TcpConnectionSupport mockConn2 = makeMockConnection("conn2");
		when(factory.getConnection()).thenReturn(mockConn1).thenReturn(mockConn2);
		CachingClientConnectionFactory cachingFactory = new CachingClientConnectionFactory(factory, 0);
		cachingFactory.start();
		TcpConnection conn1 = cachingFactory.getConnection();
		assertThat(conn1.toString()).isEqualTo("Cached:" + mockConn1.toString());
		conn1.close();
		conn1 = cachingFactory.getConnection();
		assertThat(conn1.toString()).isEqualTo("Cached:" + mockConn1.toString());
		TcpConnection conn2 = cachingFactory.getConnection();
		assertThat(conn2.toString()).isEqualTo("Cached:" + mockConn2.toString());
		conn1.close();
		conn2.close();
	}

	@Test
	public void testReuseClosed() throws Exception {
		AbstractClientConnectionFactory factory = mock(AbstractClientConnectionFactory.class);
		when(factory.isRunning()).thenReturn(true);
		TcpConnectionSupport mockConn1 = makeMockConnection("conn1");
		TcpConnectionSupport mockConn2 = makeMockConnection("conn2");
		doAnswer(invocation -> null).when(mockConn1).close();
		when(factory.getConnection()).thenReturn(mockConn1)
				.thenReturn(mockConn2).thenReturn(mockConn1)
				.thenReturn(mockConn2);
		CachingClientConnectionFactory cachingFactory = new CachingClientConnectionFactory(factory, 2);
		cachingFactory.start();
		TcpConnection conn1 = cachingFactory.getConnection();
		assertThat(conn1.toString()).isEqualTo("Cached:" + mockConn1.toString());
		conn1.close();
		conn1 = cachingFactory.getConnection();
		assertThat(conn1.toString()).isEqualTo("Cached:" + mockConn1.toString());
		TcpConnection conn2 = cachingFactory.getConnection();
		assertThat(conn2.toString()).isEqualTo("Cached:" + mockConn2.toString());
		conn1.close();
		conn2.close();
		when(mockConn1.isOpen()).thenReturn(false);
		TcpConnection conn2a = cachingFactory.getConnection();
		assertThat(conn2a.toString()).isEqualTo("Cached:" + mockConn2.toString());
		assertThat(TestUtils.getPropertyValue(conn2a, "theConnection"))
				.isSameAs(TestUtils.getPropertyValue(conn2, "theConnection"));
		conn2a.close();
	}

	@Test
	public void testLimit() throws Exception {
		AbstractClientConnectionFactory factory = mock(AbstractClientConnectionFactory.class);
		when(factory.isRunning()).thenReturn(true);
		TcpConnectionSupport mockConn1 = makeMockConnection("conn1");
		TcpConnectionSupport mockConn2 = makeMockConnection("conn2");
		when(factory.getConnection()).thenReturn(mockConn1).thenReturn(mockConn2);
		CachingClientConnectionFactory cachingFactory = new CachingClientConnectionFactory(factory, 2);
		cachingFactory.setConnectionWaitTimeout(10);
		cachingFactory.start();
		TcpConnection conn1 = cachingFactory.getConnection();
		assertThat(conn1.toString()).isEqualTo("Cached:" + mockConn1.toString());
		conn1.close();
		conn1 = cachingFactory.getConnection();
		assertThat(conn1.toString()).isEqualTo("Cached:" + mockConn1.toString());
		TcpConnection conn2 = cachingFactory.getConnection();
		assertThat(conn2.toString()).isEqualTo("Cached:" + mockConn2.toString());
		assertThatExceptionOfType(PoolItemNotAvailableException.class)
				.isThrownBy(cachingFactory::getConnection);
	}

	@Test
	public void testStop() throws Exception {
		AbstractClientConnectionFactory factory = mock(AbstractClientConnectionFactory.class);
		when(factory.isRunning()).thenReturn(true);
		TcpConnectionSupport mockConn1 = makeMockConnection("conn1");
		TcpConnectionSupport mockConn2 = makeMockConnection("conn2");
		int i = 3;
		when(factory.getConnection()).thenReturn(mockConn1)
				.thenReturn(mockConn2)
				.thenReturn(makeMockConnection("conn" + (i++)));
		CachingClientConnectionFactory cachingFactory = new CachingClientConnectionFactory(factory, 2);
		cachingFactory.start();
		TcpConnection conn1 = cachingFactory.getConnection();
		assertThat(conn1.toString()).isEqualTo("Cached:" + mockConn1.toString());
		conn1.close();
		conn1 = cachingFactory.getConnection();
		assertThat(conn1.toString()).isEqualTo("Cached:" + mockConn1.toString());
		TcpConnection conn2 = cachingFactory.getConnection();
		assertThat(conn2.toString()).isEqualTo("Cached:" + mockConn2.toString());
		cachingFactory.stop();
		Answer<Object> answer = invocation -> null;
		doAnswer(answer).when(mockConn1).close();
		doAnswer(answer).when(mockConn2).close();
		when(factory.isRunning()).thenReturn(false);
		conn1.close();
		conn2.close();
		verify(mockConn1).close();
		verify(mockConn2).close();
		when(mockConn1.isOpen()).thenReturn(false);
		when(mockConn2.isOpen()).thenReturn(false);
		when(factory.isRunning()).thenReturn(true);
		TcpConnection conn3 = cachingFactory.getConnection();
		assertThat(TestUtils.getPropertyValue(conn3, "theConnection"))
				.isNotSameAs(TestUtils.getPropertyValue(conn1, "theConnection"));
		assertThat(TestUtils.getPropertyValue(conn3, "theConnection"))
				.isNotSameAs(TestUtils.getPropertyValue(conn2, "theConnection"));
	}

	@Test
	public void testEnlargePool() throws Exception {
		AbstractClientConnectionFactory factory = mock(AbstractClientConnectionFactory.class);
		when(factory.isRunning()).thenReturn(true);
		TcpConnectionSupport mockConn1 = makeMockConnection("conn1");
		TcpConnectionSupport mockConn2 = makeMockConnection("conn2");
		TcpConnectionSupport mockConn3 = makeMockConnection("conn3");
		TcpConnectionSupport mockConn4 = makeMockConnection("conn4");
		when(factory.getConnection()).thenReturn(mockConn1, mockConn2, mockConn3, mockConn4);
		CachingClientConnectionFactory cachingFactory = new CachingClientConnectionFactory(factory, 2);
		cachingFactory.start();
		TcpConnection conn1 = cachingFactory.getConnection();
		TcpConnection conn2 = cachingFactory.getConnection();
		assertThat(conn2).isNotSameAs(conn1);
		Semaphore semaphore = TestUtils.getPropertyValue(
				TestUtils.getPropertyValue(cachingFactory, "pool"), "permits", Semaphore.class);
		assertThat(semaphore.availablePermits()).isEqualTo(0);
		cachingFactory.setPoolSize(4);
		TcpConnection conn3 = cachingFactory.getConnection();
		TcpConnection conn4 = cachingFactory.getConnection();
		assertThat(semaphore.availablePermits()).isEqualTo(0);
		conn1.close();
		conn1.close();
		conn2.close();
		conn3.close();
		conn4.close();
		assertThat(semaphore.availablePermits()).isEqualTo(4);
	}

	@Test
	public void testReducePool() throws Exception {
		AbstractClientConnectionFactory factory = mock(AbstractClientConnectionFactory.class);
		when(factory.isRunning()).thenReturn(true);
		TcpConnectionSupport mockConn1 = makeMockConnection("conn1", true);
		TcpConnectionSupport mockConn2 = makeMockConnection("conn2", true);
		TcpConnectionSupport mockConn3 = makeMockConnection("conn3", true);
		TcpConnectionSupport mockConn4 = makeMockConnection("conn4", true);
		when(factory.getConnection()).thenReturn(mockConn1)
				.thenReturn(mockConn2).thenReturn(mockConn3)
				.thenReturn(mockConn4);
		CachingClientConnectionFactory cachingFactory = new CachingClientConnectionFactory(factory, 4);
		cachingFactory.start();
		TcpConnection conn1 = cachingFactory.getConnection();
		TcpConnection conn2 = cachingFactory.getConnection();
		TcpConnection conn3 = cachingFactory.getConnection();
		TcpConnection conn4 = cachingFactory.getConnection();
		Semaphore semaphore = TestUtils.getPropertyValue(
				TestUtils.getPropertyValue(cachingFactory, "pool"), "permits", Semaphore.class);
		assertThat(semaphore.availablePermits()).isEqualTo(0);
		conn1.close();
		assertThat(semaphore.availablePermits()).isEqualTo(1);
		cachingFactory.setPoolSize(2);
		assertThat(semaphore.availablePermits()).isEqualTo(0);
		assertThat(cachingFactory.getActiveCount()).isEqualTo(3);
		conn2.close();
		assertThat(semaphore.availablePermits()).isEqualTo(0);
		assertThat(cachingFactory.getActiveCount()).isEqualTo(2);
		conn3.close();
		assertThat(cachingFactory.getActiveCount()).isEqualTo(1);
		assertThat(cachingFactory.getIdleCount()).isEqualTo(1);
		conn4.close();
		assertThat(semaphore.availablePermits()).isEqualTo(2);
		assertThat(cachingFactory.getActiveCount()).isEqualTo(0);
		assertThat(cachingFactory.getIdleCount()).isEqualTo(2);
		verify(mockConn1).close();
		verify(mockConn2).close();
	}

	@Test
	public void testExceptionOnSendNet() throws Exception {
		TcpConnectionSupport conn1 = mockedTcpNetConnection();
		TcpConnectionSupport conn2 = mockedTcpNetConnection();

		CachingClientConnectionFactory cccf = createCCCFWith2Connections(conn1, conn2);
		doTestCloseOnSendError(conn1, conn2, cccf);
	}

	@Test
	public void testExceptionOnSendNio() throws Exception {
		TcpConnectionSupport conn1 = mockedTcpNioConnection();
		TcpConnectionSupport conn2 = mockedTcpNioConnection();

		CachingClientConnectionFactory cccf = createCCCFWith2Connections(conn1, conn2);
		doTestCloseOnSendError(conn1, conn2, cccf);
	}

	private void doTestCloseOnSendError(TcpConnection conn1, TcpConnection conn2,
			CachingClientConnectionFactory cccf) throws Exception {
		TcpConnection cached1 = cccf.getConnection();
		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> cached1.send(new GenericMessage<>("foo")));
		// Before INT-3163 this failed with a timeout - connection not returned to pool after failure on send()
		TcpConnection cached2 = cccf.getConnection();
		assertThat(cached1.getConnectionId().contains(conn1.getConnectionId())).isTrue();
		assertThat(cached2.getConnectionId().contains(conn2.getConnectionId())).isTrue();
	}

	private CachingClientConnectionFactory createCCCFWith2Connections(TcpConnectionSupport conn1,
			TcpConnectionSupport conn2) throws Exception {

		AbstractClientConnectionFactory factory = mock(AbstractClientConnectionFactory.class);
		when(factory.isRunning()).thenReturn(true);
		when(factory.getConnection()).thenReturn(conn1, conn2);
		CachingClientConnectionFactory cccf = new CachingClientConnectionFactory(factory, 1);
		cccf.setConnectionWaitTimeout(1);
		cccf.start();
		return cccf;
	}

	private TcpConnectionSupport mockedTcpNetConnection() throws IOException {
		Socket socket = mock(Socket.class);
		when(socket.isClosed()).thenReturn(true); // closed when next retrieved
		OutputStream stream = mock(OutputStream.class);
		doThrow(new IOException("Foo")).when(stream).write(any(byte[].class), anyInt(), anyInt());
		when(socket.getOutputStream()).thenReturn(stream);
		TcpNetConnection conn = new TcpNetConnection(socket, false, false, event -> {
		}, "foo");
		conn.setMapper(new TcpMessageMapper());
		conn.setSerializer(new ByteArrayCrLfSerializer());
		return conn;
	}

	private TcpConnectionSupport mockedTcpNioConnection() throws Exception {
		SocketChannel socketChannel = mock(SocketChannel.class);
		if (System.getProperty("java.version").startsWith("1.8")) {
			new DirectFieldAccessor(socketChannel).setPropertyValue("open", false);
		}
		else {
			new DirectFieldAccessor(socketChannel).setPropertyValue("closed", true);
		}
		doThrow(new IOException("Foo")).when(socketChannel).write(Mockito.any(ByteBuffer.class));
		when(socketChannel.socket()).thenReturn(mock(Socket.class));
		TcpNioConnection conn = new TcpNioConnection(socketChannel, false, false, event -> {
		}, "foo");
		conn.setMapper(new TcpMessageMapper());
		conn.setSerializer(new ByteArrayCrLfSerializer());
		return conn;
	}

	private TcpConnectionSupport makeMockConnection(String name) {
		return makeMockConnection(name, false);
	}

	private TcpConnectionSupport makeMockConnection(String name, boolean closeOk) {
		TcpConnectionSupport mockConn1 = mock(TcpConnectionSupport.class);
		when(mockConn1.getConnectionId()).thenReturn(name);
		when(mockConn1.toString()).thenReturn(name);
		when(mockConn1.isOpen()).thenReturn(true);
		if (!closeOk) {
			doThrow(new RuntimeException("close() not expected")).when(mockConn1).close();
		}
		return mockConn1;
	}

	@Test
	public void integrationTest() {
		TestingUtilities.waitListening(serverCf, null);
		new DirectFieldAccessor(this.clientAdapterCf).setPropertyValue("port", this.serverCf.getPort());

		this.outbound.send(new GenericMessage<>("Hello, world!"));
		Message<?> m = inbound.receive(20_000);
		assertThat(m).isNotNull();
		String connectionId = m.getHeaders().get(IpHeaders.CONNECTION_ID, String.class);

		// assert we use the same connection from the pool
		outbound.send(new GenericMessage<String>("Hello, world!"));
		m = inbound.receive(20_000);
		assertThat(m).isNotNull();
		assertThat(m.getHeaders().get(IpHeaders.CONNECTION_ID, String.class)).isEqualTo(connectionId);
	}

	@Test
	//	@Repeat(1000) // INT-3722
	public void gatewayIntegrationTest() throws Exception {
		final List<String> connectionIds = new ArrayList<>();
		final AtomicBoolean okToRun = new AtomicBoolean(true);
		ExecutorService exec = Executors.newSingleThreadExecutor();
		exec.execute(() -> {
			while (okToRun.get()) {
				Message<?> m = inbound.receive(1000);
				if (m != null) {
					connectionIds.add((String) m.getHeaders().get(IpHeaders.CONNECTION_ID));
					replies.send(MessageBuilder.withPayload("foo:" + new String((byte[]) m.getPayload()))
							.copyHeaders(m.getHeaders())
							.build());
				}
			}
		});
		TestingUtilities.waitListening(serverCf, null);
		new DirectFieldAccessor(this.clientGatewayCf).setPropertyValue("port", this.serverCf.getPort());

		this.toGateway.send(new GenericMessage<>("Hello, world!"));
		Message<?> m = fromGateway.receive(1000);
		assertThat(m).isNotNull();
		assertThat(new String((byte[]) m.getPayload())).isEqualTo("foo:" + "Hello, world!");

		BlockingQueue<?> connections = TestUtils
				.getPropertyValue(this.gatewayCF, "pool.available", BlockingQueue.class);
		// wait until the connection is returned to the pool
		await().atMost(Duration.ofSeconds(10)).until(() -> connections.size() > 0);

		// assert we use the same connection from the pool
		toGateway.send(new GenericMessage<>("Hello, world2!"));
		m = fromGateway.receive(1000);
		assertThat(m).isNotNull();
		assertThat(new String((byte[]) m.getPayload())).isEqualTo("foo:" + "Hello, world2!");

		assertThat(connectionIds.size()).isEqualTo(2);
		assertThat(connectionIds.get(1)).isEqualTo(connectionIds.get(0));

		okToRun.set(false);
		exec.shutdownNow();
		assertThat(exec.awaitTermination(20, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	public void testCloseOnTimeoutNet() throws Exception {
		TestingUtilities.waitListening(serverCf, null);
		testCloseOnTimeoutGuts(new TcpNetClientConnectionFactory("localhost", serverCf.getPort()));
	}

	@Test
	public void testCloseOnTimeoutNio() throws Exception {
		TestingUtilities.waitListening(serverCf, null);
		testCloseOnTimeoutGuts(new TcpNioClientConnectionFactory("localhost", serverCf.getPort()));
	}

	private void testCloseOnTimeoutGuts(AbstractClientConnectionFactory cf) throws Exception {
		cf.setSoTimeout(100);
		CachingClientConnectionFactory cccf = new CachingClientConnectionFactory(cf, 1);
		cccf.start();
		TcpConnection connection = cccf.getConnection();
		await().atMost(Duration.ofSeconds(10)).until(() -> !connection.isOpen());
		cccf.stop();
		cccf.destroy();
	}

	@Test
	public void testCachedFailover() throws Exception {
		// Failover
		AbstractClientConnectionFactory factory1 = mock(AbstractClientConnectionFactory.class);
		AbstractClientConnectionFactory factory2 = mock(AbstractClientConnectionFactory.class);
		List<AbstractClientConnectionFactory> factories = new ArrayList<>();
		factories.add(factory1);
		factories.add(factory2);
		TcpConnectionSupport mockConn1 = makeMockConnection();
		TcpConnectionSupport mockConn2 = makeMockConnection();
		when(factory1.getConnection()).thenReturn(mockConn1);
		when(factory2.getConnection()).thenReturn(mockConn2);
		when(factory1.isActive()).thenReturn(true);
		when(factory2.isActive()).thenReturn(true);
		doThrow(new UncheckedIOException(new IOException("fail"))).when(mockConn1).send(Mockito.any(Message.class));
		doAnswer(invocation -> null).when(mockConn2).send(Mockito.any(Message.class));
		FailoverClientConnectionFactory failoverFactory = new FailoverClientConnectionFactory(factories);
		failoverFactory.start();

		// Cache
		CachingClientConnectionFactory cachingFactory = new CachingClientConnectionFactory(failoverFactory, 2);
		cachingFactory.start();
		TcpConnection conn1 = cachingFactory.getConnection();
		GenericMessage<String> message = new GenericMessage<>("foo");
		conn1 = cachingFactory.getConnection();
		conn1.send(message);
		Mockito.verify(mockConn2).send(message);
		conn1.close();
		cachingFactory.stop();
		cachingFactory.destroy();
	}

	@Test
	public void testCachedFailoverRealClose() throws Exception {
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
		server1.setBeanName("server2");
		final CountDownLatch latch2 = new CountDownLatch(2);
		server2.registerListener(message -> {
			latch2.countDown();
			return false;
		});
		server2.start();
		TestingUtilities.waitListening(server2, 10000L);
		int port2 = server2.getPort();
		// Failover
		AbstractClientConnectionFactory factory1 = new TcpNetClientConnectionFactory("localhost", port1);
		factory1.setBeanName("client1");
		factory1.registerListener(message -> false);
		AbstractClientConnectionFactory factory2 = new TcpNetClientConnectionFactory("localhost", port2);
		factory2.setBeanName("client2");
		factory2.registerListener(message -> false);
		List<AbstractClientConnectionFactory> factories = new ArrayList<>();
		factories.add(factory1);
		factories.add(factory2);
		FailoverClientConnectionFactory failoverFactory = new FailoverClientConnectionFactory(factories);

		// Cache
		CachingClientConnectionFactory cachingFactory = new CachingClientConnectionFactory(failoverFactory, 2);
		cachingFactory.start();
		TcpConnection conn1 = cachingFactory.getConnection();
		GenericMessage<String> message = new GenericMessage<>("foo");
		conn1.send(message);
		conn1.close();
		TcpConnection conn2 = cachingFactory.getConnection();
		assertThat(((TcpConnectionInterceptorSupport) conn2).getTheConnection())
				.isSameAs(((TcpConnectionInterceptorSupport) conn1).getTheConnection());
		conn2.send(message);
		conn1 = cachingFactory.getConnection();
		assertThat(((TcpConnectionInterceptorSupport) conn2).getTheConnection())
				.isNotSameAs(((TcpConnectionInterceptorSupport) conn1).getTheConnection());
		conn1.send(message);
		conn1.close();
		conn2.close();
		assertThat(latch1.await(10, TimeUnit.SECONDS)).isTrue();
		server1.stop();
		TestingUtilities.waitStopListening(server1, 10000L);
		TestingUtilities.waitUntilFactoryHasThisNumberOfConnections(factory1, 0);
		conn1 = cachingFactory.getConnection();
		conn2 = cachingFactory.getConnection();
		conn1.send(message);
		conn2.send(message);
		conn1.close();
		conn2.close();
		assertThat(latch2.await(10, TimeUnit.SECONDS)).isTrue();
		SimplePool<?> pool = TestUtils.getPropertyValue(cachingFactory, "pool", SimplePool.class);
		assertThat(pool.getIdleCount()).isEqualTo(2);
		cachingFactory.stop();
		cachingFactory.destroy();
		server2.stop();
	}

	@Test
	public void testCachedFailoverRealBadHost() throws Exception {
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
		server1.setBeanName("server2");
		final CountDownLatch latch2 = new CountDownLatch(2);
		server2.registerListener(message -> {
			latch2.countDown();
			return false;
		});
		server2.start();
		TestingUtilities.waitListening(server2, 10000L);
		int port2 = server2.getPort();
		// Failover
		AbstractClientConnectionFactory factory1 = new TcpNetClientConnectionFactory("junkjunk", port1);
		factory1.setBeanName("client1");
		factory1.registerListener(message -> false);
		AbstractClientConnectionFactory factory2 = new TcpNetClientConnectionFactory("localhost", port2);
		factory2.setBeanName("client2");
		factory2.registerListener(message -> false);
		List<AbstractClientConnectionFactory> factories = new ArrayList<>();
		factories.add(factory1);
		factories.add(factory2);
		FailoverClientConnectionFactory failoverFactory = new FailoverClientConnectionFactory(factories);

		// Cache
		CachingClientConnectionFactory cachingFactory = new CachingClientConnectionFactory(failoverFactory, 2);
		cachingFactory.start();
		TcpConnection conn1 = cachingFactory.getConnection();
		GenericMessage<String> message = new GenericMessage<>("foo");
		conn1.send(message);
		conn1.close();
		TcpConnection conn2 = cachingFactory.getConnection();
		assertThat(((TcpConnectionInterceptorSupport) conn2).getTheConnection())
				.isSameAs(((TcpConnectionInterceptorSupport) conn1).getTheConnection());
		conn2.send(message);
		conn1 = cachingFactory.getConnection();
		assertThat(((TcpConnectionInterceptorSupport) conn2).getTheConnection())
				.isNotSameAs(((TcpConnectionInterceptorSupport) conn1).getTheConnection());
		conn1.send(message);
		conn1.close();
		conn2.close();
		assertThat(latch2.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(latch1.getCount()).isEqualTo(3);
		server1.stop();
		server2.stop();
		cachingFactory.stop();
		cachingFactory.destroy();
	}

	@Test //INT-3650
	public void testRealConnection() throws Exception {
		TcpNetServerConnectionFactory in = new TcpNetServerConnectionFactory(0);
		final CountDownLatch latch1 = new CountDownLatch(2);
		final CountDownLatch latch2 = new CountDownLatch(102);
		final List<String> connectionIds = new ArrayList<>();
		in.registerListener(message -> {
			connectionIds.add((String) message.getHeaders().get(IpHeaders.CONNECTION_ID));
			latch1.countDown();
			latch2.countDown();
			return false;
		});
		in.start();
		TestingUtilities.waitListening(in, null);
		int port = in.getPort();
		TcpNetClientConnectionFactory out = new TcpNetClientConnectionFactory("localhost", port);
		CachingClientConnectionFactory cache = new CachingClientConnectionFactory(out, 1);
		cache.setSingleUse(false);
		cache.setConnectionWaitTimeout(100);
		cache.start();
		TcpConnectionSupport connection1 = cache.getConnection();
		connection1.send(new GenericMessage<>("foo"));
		connection1.close();
		TcpConnectionSupport connection2 = cache.getConnection();
		connection2.send(new GenericMessage<>("foo"));
		connection2.close();
		assertThat(latch1.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(connectionIds.get(1)).isSameAs(connectionIds.get(0));
		for (int i = 0; i < 100; i++) {
			TcpConnectionSupport connection = cache.getConnection();
			connection.send(new GenericMessage<>("foo"));
			connection.close();
		}
		assertThat(latch2.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(connectionIds.get(101)).isSameAs(connectionIds.get(0));
		in.stop();
		cache.stop();
		cache.destroy();
	}

	@SuppressWarnings("unchecked")
	@Test //INT-3722
	public void testGatewayRelease() {
		TcpNetServerConnectionFactory in = new TcpNetServerConnectionFactory(0);
		in.setApplicationEventPublisher(mock(ApplicationEventPublisher.class));
		final TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(in);
		final AtomicInteger count = new AtomicInteger(2);
		in.registerListener(message -> {
			if (!(message instanceof ErrorMessage)) {
				if (count.decrementAndGet() < 1) {
					try {
						Thread.sleep(100);
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
				handler.handleMessage(message);
			}
			return false;
		});
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		handler.start();
		TestingUtilities.waitListening(in, null);
		int port = in.getPort();
		TcpNetClientConnectionFactory out = new TcpNetClientConnectionFactory("localhost", port);
		out.setApplicationEventPublisher(mock(ApplicationEventPublisher.class));
		CachingClientConnectionFactory cache = new CachingClientConnectionFactory(out, 2);
		TcpOutboundGateway gate = new TcpOutboundGateway();
		gate.setConnectionFactory(cache);
		QueueChannel outputChannel = new QueueChannel();
		gate.setOutputChannel(outputChannel);
		gate.setBeanFactory(mock(BeanFactory.class));
		gate.setRemoteTimeout(20_000);
		gate.afterPropertiesSet();
		LogAccessor logger = spy(TestUtils.getPropertyValue(gate, "logger", LogAccessor.class));
		new DirectFieldAccessor(gate).setPropertyValue("logger", logger);
		when(logger.isDebugEnabled()).thenReturn(true);
		doAnswer(new Answer<Void>() {

			private final CountDownLatch latch = new CountDownLatch(2);

			private final AtomicBoolean first = new AtomicBoolean(true);

			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				invocation.callRealMethod();
				String log = ((Supplier<String>) invocation.getArgument(0)).get();
				if (log.startsWith("Response") && this.first.getAndSet(false)) {
					new SimpleAsyncTaskExecutor("testGatewayRelease-")
							.execute(() -> gate.handleMessage(new GenericMessage<>("bar")));
					// hold up the first thread until the second has added its pending reply
					this.latch.await(20, TimeUnit.SECONDS);
				}
				else if (log.startsWith("Added")) {
					this.latch.countDown();
				}
				return null;
			}
		}).when(logger).debug(any(Supplier.class));
		gate.start();
		gate.handleMessage(new GenericMessage<>("foo"));
		Message<byte[]> result = (Message<byte[]>) outputChannel.receive(10000);
		assertThat(result).isNotNull();
		assertThat(new String(result.getPayload())).isEqualTo("foo");
		result = (Message<byte[]>) outputChannel.receive(10000);
		assertThat(result).isNotNull();
		assertThat(new String(result.getPayload())).isEqualTo("bar");
		handler.stop();
		gate.stop();
		verify(logger, never()).error(anyString());
		cache.stop();
		in.stop();
	}

	@Test // INT-3728
	public void testEarlyReceive() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		final AbstractClientConnectionFactory factory = new TcpNetClientConnectionFactory("", 0) {

			@Override
			protected Socket createSocket(String host, int port) throws IOException {
				Socket mock = mock(Socket.class);
				when(mock.getInputStream()).thenReturn(new ByteArrayInputStream("foo\r\n".getBytes()));
				return mock;
			}

			@Override
			public boolean isActive() {
				return true;
			}

		};
		factory.setApplicationEventPublisher(mock(ApplicationEventPublisher.class));
		final CachingClientConnectionFactory cachingFactory = new CachingClientConnectionFactory(factory, 1);
		final AtomicReference<Message<?>> received = new AtomicReference<>();
		cachingFactory.registerListener(message -> {
			if (!(message instanceof ErrorMessage)) {
				received.set(message);
				latch.countDown();
			}
			return false;
		});
		cachingFactory.start();

		cachingFactory.getConnection();
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(received.get()).isNotNull();
		assertThat(received.get().getHeaders().get(IpHeaders.ACTUAL_CONNECTION_ID)).isNotNull();
		cachingFactory.stop();
	}

	private TcpConnectionSupport makeMockConnection() {
		TcpConnectionSupport connection = mock(TcpConnectionSupport.class);
		when(connection.isOpen()).thenReturn(true);
		return connection;
	}

}
