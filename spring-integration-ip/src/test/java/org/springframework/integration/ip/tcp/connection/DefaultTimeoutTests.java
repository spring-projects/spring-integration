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

import static org.junit.Assert.assertEquals;

import java.net.Socket;

import org.junit.Test;
import org.springframework.integration.Message;
import org.springframework.integration.ip.util.SocketTestUtils;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author Gary Russell
 * @since 2.2
 *
 */
public class DefaultTimeoutTests {

	@Test
	public void test() throws Exception {
		int port = SocketTestUtils.findAvailableServerSocket();
		TcpNetServerConnectionFactory server = new TcpNetServerConnectionFactory(port);
		server.registerListener(new TcpListener() {
			public boolean onMessage(Message<?> message) {
				return false;
			}
		});
		TcpNetClientConnectionFactory client = new TcpNetClientConnectionFactory("localhost", port);
		client.registerSender(new TcpSender() {
			public void addNewConnection(TcpConnection connection) {
			}
			public void removeDeadConnection(TcpConnection connection) {
			}
		});
		client.registerListener(new TcpListener() {
			public boolean onMessage(Message<?> message) {
				return false;
			}
		});
		server.start();
		client.start();
		TcpConnection connection = client.getConnection();
		Socket socket = TestUtils.getPropertyValue(connection, "socket", Socket.class);
		// should default to 0 (infinite) timeout
		assertEquals(0, socket.getSoTimeout());
		connection.close();
		server.stop();
		client.stop();
	}

}
