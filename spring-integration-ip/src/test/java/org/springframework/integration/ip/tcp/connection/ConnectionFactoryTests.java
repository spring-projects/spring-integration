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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.Message;
import org.springframework.integration.ip.tcp.TcpReceivingChannelAdapter;
import org.springframework.integration.ip.util.TestingUtilities;
import org.springframework.integration.test.util.SocketUtils;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @since 3.0
 *
 */
public class ConnectionFactoryTests {

	@Test
	public void testObtainConnectionIds() throws Exception {
		final List<TcpConnectionEvent> events =
				Collections.synchronizedList(new ArrayList<TcpConnectionEvent>());
		ApplicationEventPublisher publisher = new ApplicationEventPublisher() {
			public void publishEvent(ApplicationEvent event) {
				events.add((TcpConnectionEvent) event);
			}
		};
		int port = SocketUtils.findAvailableServerSocket();
		TcpNetServerConnectionFactory serverFactory = new TcpNetServerConnectionFactory(port);
		serverFactory.setBeanName("serverFactory");
		serverFactory.setApplicationEventPublisher(publisher);
		serverFactory = spy(serverFactory);
		final CountDownLatch serverConnectionInitLatch = new CountDownLatch(1);
		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Object result = invocation.callRealMethod();
				serverConnectionInitLatch.countDown();
				return result;
			}
		}).when(serverFactory).wrapConnection(any(TcpConnectionSupport.class));
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(serverFactory);
		adapter.start();
		TestingUtilities.waitListening(serverFactory, null);
		TcpNetClientConnectionFactory clientFactory = new TcpNetClientConnectionFactory("localhost", port);
		clientFactory.registerListener(new TcpListener() {
			public boolean onMessage(Message<?> message) {
				return false;
			}
		});
		clientFactory.setBeanName("clientFactory");
		clientFactory.setApplicationEventPublisher(publisher);
		clientFactory.start();
		TcpConnectionSupport client = clientFactory.getConnection();
		List<String> clients = clientFactory.getOpenConnectionIds();
		assertEquals(1, clients.size());
		assertTrue(clients.contains(client.getConnectionId()));
		assertTrue("Server connection failed to register", serverConnectionInitLatch.await(1, TimeUnit.SECONDS));
		List<String> servers = serverFactory.getOpenConnectionIds();
		assertEquals(1, servers.size());
		assertTrue(serverFactory.closeConnection(servers.get(0)));
		servers = serverFactory.getOpenConnectionIds();
		assertEquals(0, servers.size());
		Thread.sleep(1000);
		clients = clientFactory.getOpenConnectionIds();
		assertEquals(0, clients.size());
		assertEquals(6, events.size()); // OPEN, CLOSE, EXCEPTION for each side

		FooEvent event = new FooEvent(client, "foo");
		client.publishEvent(event);
		assertEquals(7, events.size());

		try {
			event = new FooEvent(mock(TcpConnectionSupport.class), "foo");
			client.publishEvent(event);
			fail("Expected exception");
		}
		catch (IllegalArgumentException e) {
			assertTrue("Can only publish events with this as the source".equals(e.getMessage()));
		}
	}

	@SuppressWarnings("serial")
	private class FooEvent extends TcpConnectionOpenEvent {

		public FooEvent(TcpConnectionSupport connection, String connectionFactoryName) {
			super(connection, connectionFactoryName);
		}

	}

}
