/*
 * Copyright 2002-2013 the original author or authors.
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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.util.TestingUtilities;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 2.2
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD)
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

	@Test(expected=MessagingException.class)
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
		Answer<Object> answer = new Answer<Object> () {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return null;
			}};
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
	public void gatewayIntegrationTest() throws Exception {
		final List<String> connectionIds = new ArrayList<String>();
		final AtomicBoolean okToRun = new AtomicBoolean(true);
		Executors.newSingleThreadExecutor().execute(new Runnable() {
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

		// wait a short time to allow the connection to be returned to the pool
		Thread.sleep(1000);

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

	private void testCloseOnTimeoutGuts(AbstractClientConnectionFactory cf) throws Exception, InterruptedException {
		TestingUtilities.waitListening(serverCf, null);
		cf.setSoTimeout(100);
		CachingClientConnectionFactory cccf = new CachingClientConnectionFactory(cf, 1);
		cccf.start();
		TcpConnection connection = cccf.getConnection();
		Thread.sleep(200);
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

	public TcpConnectionSupport makeMockConnection() {
		TcpConnectionSupport connection = mock(TcpConnectionSupport.class);
		when(connection.isOpen()).thenReturn(true);
		return connection;
	}

}
