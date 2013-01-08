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

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.Message;
import org.springframework.integration.ip.tcp.TcpReceivingChannelAdapter;
import org.springframework.integration.ip.util.TestingUtilities;
import org.springframework.integration.test.util.SocketUtils;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class ConnectionFactoryTests {

	@Test
	public void testObtainConnectionIds() throws Exception {
		final List<TcpConnectionEvent> events = new ArrayList<TcpConnectionEvent>();
		ApplicationEventPublisher publisher = new ApplicationEventPublisher() {
			public void publishEvent(ApplicationEvent event) {
				synchronized(events) {
					events.add((TcpConnectionEvent) event);
				}
			}
		};
		int port = SocketUtils.findAvailableServerSocket();
		TcpNetServerConnectionFactory serverFactory = new TcpNetServerConnectionFactory(port);
		serverFactory.setBeanName("serverFactory");
		serverFactory.setApplicationEventPublisher(publisher);
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
		TcpConnection client = clientFactory.getConnection();
		List<String> clients = clientFactory.getOpenConnectionIds();
		assertEquals(1, clients.size());
		assertTrue(clients.contains(client.getConnectionId()));
		List<String> servers = serverFactory.getOpenConnectionIds();
		assertEquals(1, servers.size());
		assertTrue(serverFactory.closeConnection(servers.get(0)));
		servers = serverFactory.getOpenConnectionIds();
		assertEquals(0, servers.size());
		Thread.sleep(1000);
		clients = clientFactory.getOpenConnectionIds();
		assertEquals(0, clients.size());
		assertEquals(6, events.size()); // OPEN, CLOSE, EXCEPTION for each side
	}

}
