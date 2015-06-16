/*
 * Copyright 2002-2015 the original author or authors.
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

import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
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
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.tcp.TcpOutboundGateway;
import org.springframework.integration.ip.tcp.TcpSendingMessageHandler;
import org.springframework.integration.ip.tcp.serializer.ByteArrayCrLfSerializer;
import org.springframework.integration.ip.util.TestingUtilities;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.SocketUtils;

/**
 * @author Gary Russell
 * @since 2.2
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
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
	CachingClientConnectionFactory gatewayCF;

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
		assertThat(captor.getValue(), startsWith("Message discarded; no listener:"));
		// end INT-3652
		assertEquals("Cached:" + mockConn1.toString(), conn1.toString());
		conn1.close();
		conn1 = cachingFactory.getConnection();
		assertEquals("Cached:" + mockConn1.toString(), conn1.toString());
		TcpConnection conn2 = cachingFactory.getConnection();
		assertEquals("Cached:" + mockConn2.toString(), conn2.toString());
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
		assertEquals("Cached:" + mockConn1.toString(), conn1.toString());
		conn1.close();
		conn1 = cachingFactory.getConnection();
		assertEquals("Cached:" + mockConn1.toString(), conn1.toString());
		TcpConnection conn2 = cachingFactory.getConnection();
		assertEquals("Cached:" + mockConn2.toString(), conn2.toString());
		conn1.close();
		conn2.close();
	}

	@Test
	public void testReuseClosed() throws Exception {
		AbstractClientConnectionFactory factory = mock(AbstractClientConnectionFactory.class);
		when(factory.isRunning()).thenReturn(true);
		TcpConnectionSupport mockConn1 = makeMockConnection("conn1");
		TcpConnectionSupport mockConn2 = makeMockConnection("conn2");
		doAnswer(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return null;
			}

		}).when(mockConn1).close();
		when(factory.getConnection()).thenReturn(mockConn1)
				.thenReturn(mockConn2).thenReturn(mockConn1)
				.thenReturn(mockConn2);
		CachingClientConnectionFactory cachingFactory = new CachingClientConnectionFactory(factory, 2);
		cachingFactory.start();
		TcpConnection conn1 = cachingFactory.getConnection();
		assertEquals("Cached:" + mockConn1.toString(), conn1.toString());
		conn1.close();
		conn1 = cachingFactory.getConnection();
		assertEquals("Cached:" + mockConn1.toString(), conn1.toString());
		TcpConnection conn2 = cachingFactory.getConnection();
		assertEquals("Cached:" + mockConn2.toString(), conn2.toString());
		conn1.close();
		conn2.close();
		when(mockConn1.isOpen()).thenReturn(false);
		TcpConnection conn2a = cachingFactory.getConnection();
		assertEquals("Cached:" + mockConn2.toString(), conn2a.toString());
		assertSame(TestUtils.getPropertyValue(conn2, "theConnection"),
				TestUtils.getPropertyValue(conn2a, "theConnection"));
		conn2a.close();
	}

	@Test(expected = MessagingException.class)
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
		assertEquals("Cached:" + mockConn1.toString(), conn1.toString());
		conn1.close();
		conn1 = cachingFactory.getConnection();
		assertEquals("Cached:" + mockConn1.toString(), conn1.toString());
		TcpConnection conn2 = cachingFactory.getConnection();
		assertEquals("Cached:" + mockConn2.toString(), conn2.toString());
		cachingFactory.getConnection();
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
		assertEquals("Cached:" + mockConn1.toString(), conn1.toString());
		conn1.close();
		conn1 = cachingFactory.getConnection();
		assertEquals("Cached:" + mockConn1.toString(), conn1.toString());
		TcpConnection conn2 = cachingFactory.getConnection();
		assertEquals("Cached:" + mockConn2.toString(), conn2.toString());
		cachingFactory.stop();
		Answer<Object> answer = new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return null;
			}

		};
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
		assertNotSame(TestUtils.getPropertyValue(conn1, "theConnection"),
				TestUtils.getPropertyValue(conn3, "theConnection"));
		assertNotSame(TestUtils.getPropertyValue(conn2, "theConnection"),
				TestUtils.getPropertyValue(conn3, "theConnection"));
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
		assertNotSame(conn1, conn2);
		Semaphore semaphore = TestUtils.getPropertyValue(
				TestUtils.getPropertyValue(cachingFactory, "pool"), "permits", Semaphore.class);
		assertEquals(0, semaphore.availablePermits());
		cachingFactory.setPoolSize(4);
		TcpConnection conn3 = cachingFactory.getConnection();
		TcpConnection conn4 = cachingFactory.getConnection();
		assertEquals(0, semaphore.availablePermits());
		conn1.close();
		conn1.close();
		conn2.close();
		conn3.close();
		conn4.close();
		assertEquals(4, semaphore.availablePermits());
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
		assertEquals(0, semaphore.availablePermits());
		conn1.close();
		assertEquals(1, semaphore.availablePermits());
		cachingFactory.setPoolSize(2);
		assertEquals(0, semaphore.availablePermits());
		assertEquals(3, cachingFactory.getActiveCount());
		conn2.close();
		assertEquals(0, semaphore.availablePermits());
		assertEquals(2, cachingFactory.getActiveCount());
		conn3.close();
		assertEquals(1, cachingFactory.getActiveCount());
		assertEquals(1, cachingFactory.getIdleCount());
		conn4.close();
		assertEquals(2, semaphore.availablePermits());
		assertEquals(0, cachingFactory.getActiveCount());
		assertEquals(2, cachingFactory.getIdleCount());
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
		try {
			cached1.send(new GenericMessage<String>("foo"));
			fail("Expected IOException");
		}
		catch (IOException e) {
			assertEquals("Foo", e.getMessage());
		}
		// Before INT-3163 this failed with a timeout - connection not returned to pool after failure on send()
		TcpConnection cached2 = cccf.getConnection();
		assertTrue(cached1.getConnectionId().contains(conn1.getConnectionId()));
		assertTrue(cached2.getConnectionId().contains(conn2.getConnectionId()));
	}

	private CachingClientConnectionFactory createCCCFWith2Connections(TcpConnectionSupport conn1, TcpConnectionSupport conn2)
			throws Exception {
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
		TcpNetConnection conn = new TcpNetConnection(socket, false, false, new ApplicationEventPublisher() {

			@Override
			public void publishEvent(ApplicationEvent event) {
			}

			@Override
			public void publishEvent(Object event) {

			}

		}, "foo");
		conn.setMapper(new TcpMessageMapper());
		conn.setSerializer(new ByteArrayCrLfSerializer());
		return conn;
	}

	private TcpConnectionSupport mockedTcpNioConnection() throws Exception {
		SocketChannel socketChannel = mock(SocketChannel.class);
		new DirectFieldAccessor(socketChannel).setPropertyValue("open", false);
		doThrow(new IOException("Foo")).when(socketChannel).write(Mockito.any(ByteBuffer.class));
		when(socketChannel.socket()).thenReturn(mock(Socket.class));
		TcpNioConnection conn = new TcpNioConnection(socketChannel, false, false, new ApplicationEventPublisher() {

			@Override
			public void publishEvent(ApplicationEvent event) {
			}

			@Override
			public void publishEvent(Object event) {

			}

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
	public void integrationTest() throws Exception {
		TestingUtilities.waitListening(serverCf, null);
		outbound.send(new GenericMessage<String>("Hello, world!"));
		Message<?> m = inbound.receive(1000);
		assertNotNull(m);
		String connectionId = m.getHeaders().get(IpHeaders.CONNECTION_ID, String.class);

		// assert we use the same connection from the pool
		outbound.send(new GenericMessage<String>("Hello, world!"));
		m = inbound.receive(1000);
		assertNotNull(m);
		assertEquals(connectionId, m.getHeaders().get(IpHeaders.CONNECTION_ID, String.class));
	}

	@Test
//	@Repeat(1000) // INT-3722
	public void gatewayIntegrationTest() throws Exception {
		final List<String> connectionIds = new ArrayList<String>();
		final AtomicBoolean okToRun = new AtomicBoolean(true);
		Executors.newSingleThreadExecutor().execute(new Runnable() {

			@Override
			public void run() {
				while (okToRun.get()) {
					Message<?> m = inbound.receive(1000);
					if (m != null) {
						connectionIds.add((String) m.getHeaders().get(IpHeaders.CONNECTION_ID));
						replies.send(MessageBuilder.withPayload("foo:" + new String((byte[]) m.getPayload()))
								.copyHeaders(m.getHeaders())
								.build());
					}
				}
			}

		});
		TestingUtilities.waitListening(serverCf, null);
		toGateway.send(new GenericMessage<String>("Hello, world!"));
		Message<?> m = fromGateway.receive(1000);
		assertNotNull(m);
		assertEquals("foo:" + "Hello, world!", new String((byte[]) m.getPayload()));

		BlockingQueue<?> connections = TestUtils
				.getPropertyValue(this.gatewayCF, "pool.available", BlockingQueue.class);
		// wait until the connection is returned to the pool
		int n = 0;
		while (n++ < 100 && connections.size() == 0) {
			Thread.sleep(100);
		}

		// assert we use the same connection from the pool
		toGateway.send(new GenericMessage<String>("Hello, world2!"));
		m = fromGateway.receive(1000);
		assertNotNull(m);
		assertEquals("foo:" + "Hello, world2!", new String((byte[]) m.getPayload()));

		assertEquals(2, connectionIds.size());
		assertEquals(connectionIds.get(0), connectionIds.get(1));

		okToRun.set(false);
	}

	@Test
	public void testCloseOnTimeoutNet() throws Exception {
		TcpNetClientConnectionFactory cf = new TcpNetClientConnectionFactory("localhost", serverCf.getPort());
		testCloseOnTimeoutGuts(cf);
	}

	@Test
	public void testCloseOnTimeoutNio() throws Exception {
		TcpNioClientConnectionFactory cf = new TcpNioClientConnectionFactory("localhost", serverCf.getPort());
		testCloseOnTimeoutGuts(cf);
	}

	private void testCloseOnTimeoutGuts(AbstractClientConnectionFactory cf) throws Exception {
		TestingUtilities.waitListening(serverCf, null);
		cf.setSoTimeout(100);
		CachingClientConnectionFactory cccf = new CachingClientConnectionFactory(cf, 1);
		cccf.start();
		TcpConnection connection = cccf.getConnection();
		int n = 0;
		while (n++ < 100 && connection.isOpen()) {
			Thread.sleep(100);
		}
		assertFalse(connection.isOpen());
		cccf.stop();
	}

	@Test
	public void testCachedFailover() throws Exception {
		// Failover
		AbstractClientConnectionFactory factory1 = mock(AbstractClientConnectionFactory.class);
		AbstractClientConnectionFactory factory2 = mock(AbstractClientConnectionFactory.class);
		List<AbstractClientConnectionFactory> factories = new ArrayList<AbstractClientConnectionFactory>();
		factories.add(factory1);
		factories.add(factory2);
		TcpConnectionSupport mockConn1 = makeMockConnection();
		TcpConnectionSupport mockConn2 = makeMockConnection();
		when(factory1.getConnection()).thenReturn(mockConn1);
		when(factory2.getConnection()).thenReturn(mockConn2);
		when(factory1.isActive()).thenReturn(true);
		when(factory2.isActive()).thenReturn(true);
		doThrow(new IOException("fail")).when(mockConn1).send(Mockito.any(Message.class));
		doAnswer(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return null;
			}

		}).when(mockConn2).send(Mockito.any(Message.class));
		FailoverClientConnectionFactory failoverFactory = new FailoverClientConnectionFactory(factories);
		failoverFactory.start();

		// Cache
		CachingClientConnectionFactory cachingFactory = new CachingClientConnectionFactory(failoverFactory, 2);
		cachingFactory.start();
		TcpConnection conn1 = cachingFactory.getConnection();
		GenericMessage<String> message = new GenericMessage<String>("foo");
		conn1 = cachingFactory.getConnection();
		conn1.send(message);
		Mockito.verify(mockConn2).send(message);
	}

	@Test //INT-3650
	public void testRealConnection() throws Exception {
		int port = SocketUtils.findAvailableTcpPort();
		TcpNetServerConnectionFactory in = new TcpNetServerConnectionFactory(port);
		final CountDownLatch latch1 = new CountDownLatch(2);
		final CountDownLatch latch2 = new CountDownLatch(102);
		final List<String> connectionIds = new ArrayList<String>();
		in.registerListener(new TcpListener() {

			@Override
			public boolean onMessage(Message<?> message) {
				connectionIds.add((String) message.getHeaders().get(IpHeaders.CONNECTION_ID));
				latch1.countDown();
				latch2.countDown();
				return false;
			}

		});
		in.start();
		int n = 0;
		while (n++ < 100 && !in.isListening()) {
			Thread.sleep(100);
		}
		assertTrue(in.isListening());
		TcpNetClientConnectionFactory out = new TcpNetClientConnectionFactory("localhost", port);
		CachingClientConnectionFactory cache = new CachingClientConnectionFactory(out, 1);
		cache.setSingleUse(false);
		cache.setConnectionWaitTimeout(100);
		cache.start();
		TcpConnectionSupport connection1 = cache.getConnection();
		connection1.send(new GenericMessage<String>("foo"));
		connection1.close();
		TcpConnectionSupport connection2 = cache.getConnection();
		connection2.send(new GenericMessage<String>("foo"));
		connection2.close();
		assertTrue(latch1.await(10, TimeUnit.SECONDS));
		assertSame(connectionIds.get(0), connectionIds.get(1));
		for (int i = 0; i < 100; i++) {
			TcpConnectionSupport connection = cache.getConnection();
			connection.send(new GenericMessage<String>("foo"));
			connection.close();
		}
		assertTrue(latch2.await(10, TimeUnit.SECONDS));
		assertSame(connectionIds.get(0), connectionIds.get(101));
		in.stop();
		cache.stop();
	}

	@SuppressWarnings("unchecked")
	@Test //INT-3722
	public void testGatewayRelease() throws Exception {
		int port = SocketUtils.findAvailableTcpPort();
		TcpNetServerConnectionFactory in = new TcpNetServerConnectionFactory(port);
		in.setApplicationEventPublisher(mock(ApplicationEventPublisher.class));
		final TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(in);
		final AtomicInteger count = new AtomicInteger(2);
		in.registerListener(new TcpListener() {

			@Override
			public boolean onMessage(Message<?> message) {
				if (!(message instanceof ErrorMessage)) {
					if (count.decrementAndGet() < 1) {
						try {
							Thread.sleep(1000);
						}
						catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
					}
					handler.handleMessage(message);
				}
				return false;
			}

		});
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		handler.start();
		int n = 0;
		while (n++ < 100 && !in.isListening()) {
			Thread.sleep(100);
		}
		assertTrue(in.isListening());
		TcpNetClientConnectionFactory out = new TcpNetClientConnectionFactory("localhost", port);
		out.setApplicationEventPublisher(mock(ApplicationEventPublisher.class));
		CachingClientConnectionFactory cache = new CachingClientConnectionFactory(out, 2);
		final TcpOutboundGateway gate = new TcpOutboundGateway();
		gate.setConnectionFactory(cache);
		QueueChannel outputChannel = new QueueChannel();
		gate.setOutputChannel(outputChannel);
		gate.setBeanFactory(mock(BeanFactory.class));
		gate.afterPropertiesSet();
		Log logger = spy(TestUtils.getPropertyValue(gate, "logger", Log.class));
		new DirectFieldAccessor(gate).setPropertyValue("logger", logger);
		when(logger.isDebugEnabled()).thenReturn(true);
		doAnswer(new Answer<Void>() {

			private final CountDownLatch latch = new CountDownLatch(2);

			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				invocation.callRealMethod();
				String log = (String) invocation.getArguments()[0];
				if (log.startsWith("Response")) {
					Executors.newSingleThreadScheduledExecutor().execute(new Runnable() {

						@Override
						public void run() {
							gate.handleMessage(new GenericMessage<String>("bar"));
						}
					});
					// hold up the first thread until the second has added its pending reply
					latch.await(10, TimeUnit.SECONDS);
				}
				else if (log.startsWith("Added")) {
					latch.countDown();
				}
				return null;
			}
		}).when(logger).debug(anyString());
		gate.start();
		gate.handleMessage(new GenericMessage<String>("foo"));
		Message<byte[]> result = (Message<byte[]>) outputChannel.receive(10000);
		assertNotNull(result);
		assertEquals("foo", new String(result.getPayload()));
		result = (Message<byte[]>) outputChannel.receive(10000);
		assertNotNull(result);
		assertEquals("bar", new String(result.getPayload()));
		handler.stop();
		gate.stop();
		verify(logger, never()).error(anyString());
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
		final AtomicReference<Message<?>> received = new AtomicReference<Message<?>>();
		cachingFactory.registerListener(new TcpListener() {

			@Override
			public boolean onMessage(Message<?> message) {
				if (!(message instanceof ErrorMessage)) {
					received.set(message);
					latch.countDown();
				}
				return false;
			}
		});
		cachingFactory.start();

		cachingFactory.getConnection();
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		assertNotNull(received.get());
		assertNotNull(received.get().getHeaders().get(IpHeaders.ACTUAL_CONNECTION_ID));
		cachingFactory.stop();
	}

	private TcpConnectionSupport makeMockConnection() {
		TcpConnectionSupport connection = mock(TcpConnectionSupport.class);
		when(connection.isOpen()).thenReturn(true);
		return connection;
	}

}
