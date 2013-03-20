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

package org.springframework.integration.ip.tcp;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.Properties;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpConnection;
import org.springframework.integration.ip.tcp.connection.TcpConnectionEvent;
import org.springframework.integration.ip.tcp.connection.TcpConnectionEvent.TcpConnectionEventType;
import org.springframework.integration.ip.tcp.serializer.ByteArrayRawSerializer;
import org.springframework.integration.ip.util.TestingUtilities;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


/**
 * @author Gary Russell
 * @author Gunnar Hillert
 * @since 2.0
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ConnectionToConnectionTests {

	@Autowired
	AbstractApplicationContext ctx;

	@Autowired
	private AbstractClientConnectionFactory client;

	@Autowired
	private AbstractServerConnectionFactory server;

	@Autowired
	private QueueChannel serverSideChannel;

	@Autowired
	private QueueChannel events;

	// Test jvm shutdown
	public static void main(String[] args) {
		ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext(
				ConnectionToConnectionTests.class.getPackage().getName()
						.replaceAll("\\.", "/")
						+ "/common-context.xml");
		ctx.close();
		ctx = new ClassPathXmlApplicationContext(
				ConnectionToConnectionTests.class.getPackage().getName()
						.replaceAll("\\.", "/")
						+ "/ConnectionToConnectionTests-context.xml");
		ctx.close();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testConnect() throws Exception {
		TestingUtilities.waitListening(server, null);
		client.start();
		for (int i = 0; i < 100; i++) {
			TcpConnection connection = client.getConnection();
			connection.send(MessageBuilder.withPayload("Test").build());
			Message<?> message = serverSideChannel.receive(10000);
			assertNotNull(message);
			MessageHistory history = MessageHistory.read(message);
			//org.springframework.integration.test.util.TestUtils
			Properties componentHistoryRecord = TestUtils.locateComponentInHistory(history, "looper", 0);
			assertNotNull(componentHistoryRecord);
			assertTrue(componentHistoryRecord.get("type").equals("ip:tcp-inbound-gateway"));
			assertNotNull(message);
			assertEquals("Test", new String((byte[]) message.getPayload()));
		}
		int clientOpens = 0;
		int clientCloses = 0;
		int serverOpens = 0;
		int serverCloses = 0;
		int clientExceptions = 0;
		Message<TcpConnectionEvent> eventMessage;
		while ((eventMessage = (Message<TcpConnectionEvent>) events.receive(1000)) != null) {
			TcpConnectionEvent event = eventMessage.getPayload();
			if ("client".equals(event.getConnectionFactoryName())) {
				if (TcpConnectionEventType.OPEN == event.getType()) {
					clientOpens++;
				}
				else if (TcpConnectionEventType.CLOSE == event.getType()) {
					clientCloses++;
				}
				else if (TcpConnectionEventType.EXCEPTION == event.getType()) {
					clientExceptions++;
				}
			}
			else if ("server".equals(event.getConnectionFactoryName())) {
				if (TcpConnectionEventType.OPEN == event.getType()) {
					serverOpens++;
				}
				else if (TcpConnectionEventType.CLOSE == event.getType()) {
					serverCloses++;
				}
			}
		}
		assertEquals(100, clientOpens);
		assertEquals(100, clientCloses);
		assertEquals(100, clientExceptions);
		assertEquals(100, serverOpens);
		assertEquals(100, serverCloses);
	}

	@Test
	public void testConnectRaw() throws Exception {
		ByteArrayRawSerializer serializer = new ByteArrayRawSerializer();
		client.setSerializer(serializer);
		server.setDeserializer(serializer);
		client.start();
		TcpConnection connection = client.getConnection();
		connection.send(MessageBuilder.withPayload("Test").build());
		Message<?> message = serverSideChannel.receive(10000);
		assertNotNull(message);
		MessageHistory history = MessageHistory.read(message);
		//org.springframework.integration.test.util.TestUtils
		Properties componentHistoryRecord = TestUtils.locateComponentInHistory(history, "looper", 0);
		assertNotNull(componentHistoryRecord);
		assertTrue(componentHistoryRecord.get("type").equals("ip:tcp-inbound-gateway"));
		assertNotNull(message);
		assertEquals("Test", new String((byte[]) message.getPayload()));
	}

	@Test
	public void testLookup() throws Exception {
		client.start();
		TcpConnection connection = client.getConnection();
		assertFalse(connection.getConnectionId().contains("localhost"));
		connection.close();
		client.setLookupHost(true);
		connection = client.getConnection();
		assertTrue(connection.getConnectionId().contains("localhost"));
		connection.close();
		client.setLookupHost(false);
		connection = client.getConnection();
		assertFalse(connection.getConnectionId().contains("localhost"));
		connection.close();
	}

}
