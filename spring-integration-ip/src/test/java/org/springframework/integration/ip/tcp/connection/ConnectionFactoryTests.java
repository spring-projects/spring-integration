/*
 * Copyright 2002-2018 the original author or authors.
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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.ip.config.TcpConnectionFactoryFactoryBean;
import org.springframework.integration.ip.event.IpIntegrationEvent;
import org.springframework.integration.ip.tcp.TcpReceivingChannelAdapter;
import org.springframework.integration.test.rule.Log4j2LevelAdjuster;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 3.0
 *
 */
public class ConnectionFactoryTests {

	@Rule
	public Log4j2LevelAdjuster adjuster = Log4j2LevelAdjuster.trace();

	@Test
	public void factoryBeanTests() {
		TcpConnectionFactoryFactoryBean fb = new TcpConnectionFactoryFactoryBean("client");
		assertEquals(AbstractClientConnectionFactory.class, fb.getObjectType());
		fb = new TcpConnectionFactoryFactoryBean("server");
		assertEquals(AbstractServerConnectionFactory.class, fb.getObjectType());
		fb = new TcpConnectionFactoryFactoryBean();
		assertEquals(AbstractConnectionFactory.class, fb.getObjectType());
	}

	@Test
	public void testObtainConnectionIdsNet() throws Exception {
		TcpNetServerConnectionFactory serverFactory = new TcpNetServerConnectionFactory(0);
		testObtainConnectionIds(serverFactory);
	}

	@Test
	public void testObtainConnectionIdsNio() throws Exception {
		TcpNioServerConnectionFactory serverFactory = new TcpNioServerConnectionFactory(0);
		testObtainConnectionIds(serverFactory);
	}

	public void testObtainConnectionIds(AbstractServerConnectionFactory serverFactory) throws Exception {
		final List<IpIntegrationEvent> events =
				Collections.synchronizedList(new ArrayList<IpIntegrationEvent>());
		int expectedEvents = serverFactory instanceof TcpNetServerConnectionFactory
				? 7  // Listening, + OPEN, CLOSE, EXCEPTION for each side
				: 5; // Listening, + OPEN, CLOSE (but we *might* get exceptions, depending on timing).
		final CountDownLatch serverListeningLatch = new CountDownLatch(1);
		final CountDownLatch eventLatch = new CountDownLatch(expectedEvents);
		ApplicationEventPublisher publisher = new ApplicationEventPublisher() {

			@Override
			public void publishEvent(ApplicationEvent event) {
				LogFactory.getLog(this.getClass()).trace("Received: " + event);
				events.add((IpIntegrationEvent) event);
				if (event instanceof TcpConnectionServerListeningEvent) {
					serverListeningLatch.countDown();
				}
				eventLatch.countDown();
			}

			@Override
			public void publishEvent(Object event) {

			}

		};
		serverFactory.setBeanName("serverFactory");
		serverFactory.setApplicationEventPublisher(publisher);
		serverFactory = spy(serverFactory);
		final CountDownLatch serverConnectionInitLatch = new CountDownLatch(1);
		doAnswer(invocation -> {
			Object result = invocation.callRealMethod();
			serverConnectionInitLatch.countDown();
			return result;
		}).when(serverFactory).wrapConnection(any(TcpConnectionSupport.class));
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(10);
		scheduler.afterPropertiesSet();
		BeanFactory bf = mock(BeanFactory.class);
		when(bf.containsBean(IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME)).thenReturn(true);
		when(bf.getBean(IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME, TaskScheduler.class)).thenReturn(scheduler);
		serverFactory.setBeanFactory(bf);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setOutputChannel(new NullChannel());
		adapter.setConnectionFactory(serverFactory);
		adapter.start();
		assertTrue("Listening event not received", serverListeningLatch.await(10, TimeUnit.SECONDS));
		assertThat(events.get(0), instanceOf(TcpConnectionServerListeningEvent.class));
		assertThat(((TcpConnectionServerListeningEvent) events.get(0)).getPort(), equalTo(serverFactory.getPort()));
		int port = serverFactory.getPort();
		TcpNetClientConnectionFactory clientFactory = new TcpNetClientConnectionFactory("localhost", port);
		clientFactory.registerListener(message -> false);
		clientFactory.setBeanName("clientFactory");
		clientFactory.setApplicationEventPublisher(publisher);
		clientFactory.start();
		TcpConnectionSupport client = clientFactory.getConnection();
		List<String> clients = clientFactory.getOpenConnectionIds();
		assertEquals(1, clients.size());
		assertTrue(clients.contains(client.getConnectionId()));
		assertTrue("Server connection failed to register", serverConnectionInitLatch.await(10, TimeUnit.SECONDS));
		List<String> servers = serverFactory.getOpenConnectionIds();
		assertEquals(1, servers.size());
		assertTrue(serverFactory.closeConnection(servers.get(0)));
		servers = serverFactory.getOpenConnectionIds();
		assertEquals(0, servers.size());
		int n = 0;
		clients = clientFactory.getOpenConnectionIds();
		while (n++ < 100 && clients.size() > 0) {
			Thread.sleep(100);
			clients = clientFactory.getOpenConnectionIds();
		}
		assertEquals(0, clients.size());
		assertTrue(eventLatch.await(10, TimeUnit.SECONDS));
		assertThat("Expected at least " + expectedEvents + " events; got: " + events.size() + " : " + events,
				events.size(), greaterThanOrEqualTo(expectedEvents));

		FooEvent event = new FooEvent(client, "foo");
		client.publishEvent(event);
		assertThat("Expected at least " + expectedEvents + " events; got: " + events.size() + " : " + events,
				events.size(), greaterThanOrEqualTo(expectedEvents + 1));

		try {
			event = new FooEvent(mock(TcpConnectionSupport.class), "foo");
			client.publishEvent(event);
			fail("Expected exception");
		}
		catch (IllegalArgumentException e) {
			assertTrue("Can only publish events with this as the source".equals(e.getMessage()));
		}

		SocketAddress address = serverFactory.getServerSocketAddress();
		if (address instanceof InetSocketAddress) {
			InetSocketAddress inetAddress = (InetSocketAddress) address;
			assertEquals(port, inetAddress.getPort());
		}
		serverFactory.stop();
		scheduler.shutdown();
	}

	@Test
	public void testEarlyCloseNet() throws Exception {
		AbstractServerConnectionFactory factory = new TcpNetServerConnectionFactory(0);
		testEarlyClose(factory, "serverSocket", " stopped before accept");
	}

	@Test
	public void testEarlyCloseNio() throws Exception {
		AbstractServerConnectionFactory factory = new TcpNioServerConnectionFactory(0);
		testEarlyClose(factory, "serverChannel", " stopped before registering the server channel");
	}

	private void testEarlyClose(final AbstractServerConnectionFactory factory, String property,
			String message) throws Exception {
		factory.setApplicationEventPublisher(mock(ApplicationEventPublisher.class));
		factory.setBeanName("foo");
		factory.registerListener(mock(TcpListener.class));
		factory.afterPropertiesSet();
		Log logger = spy(TestUtils.getPropertyValue(factory, "logger", Log.class));
		new DirectFieldAccessor(factory).setPropertyValue("logger", logger);
		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);
		final CountDownLatch latch3 = new CountDownLatch(1);
		when(logger.isInfoEnabled()).thenReturn(true);
		when(logger.isDebugEnabled()).thenReturn(true);
		doAnswer(invocation -> {
			latch1.countDown();
			// wait until the stop nulls the channel
			latch2.await(10, TimeUnit.SECONDS);
			return null;
		}).when(logger).info(contains("Listening"));
		doAnswer(invocation -> {
			latch3.countDown();
			return null;
		}).when(logger).debug(contains(message));
		factory.start();
		assertTrue("missing info log", latch1.await(10, TimeUnit.SECONDS));
		// stop on a different thread because it waits for the executor
		new SimpleAsyncTaskExecutor()
				.execute(factory::stop);
		int n = 0;
		DirectFieldAccessor accessor = new DirectFieldAccessor(factory);
		while (n++ < 200 && accessor.getPropertyValue(property) != null) {
			Thread.sleep(100);
		}
		assertTrue("Stop was not invoked in time", n < 200);
		latch2.countDown();
		assertTrue("missing debug log", latch3.await(10, TimeUnit.SECONDS));
		String expected = "foo, port=" + factory.getPort() + message;
		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(logger, atLeast(1)).debug(captor.capture());
		assertThat(captor.getAllValues(), hasItem(expected));
		factory.stop();
	}

	@SuppressWarnings("serial")
	private class FooEvent extends TcpConnectionOpenEvent {

		FooEvent(TcpConnectionSupport connection, String connectionFactoryName) {
			super(connection, connectionFactoryName);
		}

	}

}
