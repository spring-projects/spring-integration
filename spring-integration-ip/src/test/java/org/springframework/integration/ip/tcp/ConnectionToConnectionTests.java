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

package org.springframework.integration.ip.tcp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpConnection;
import org.springframework.integration.ip.tcp.connection.TcpConnectionCloseEvent;
import org.springframework.integration.ip.tcp.connection.TcpConnectionEvent;
import org.springframework.integration.ip.tcp.connection.TcpConnectionExceptionEvent;
import org.springframework.integration.ip.tcp.connection.TcpConnectionOpenEvent;
import org.springframework.integration.ip.tcp.serializer.ByteArrayRawSerializer;
import org.springframework.integration.ip.util.TestingUtilities;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
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
@DirtiesContext
public class ConnectionToConnectionTests {

	@Autowired
	AbstractApplicationContext ctx;

	@Autowired
	private AbstractClientConnectionFactory clientNet;

	@Autowired
	private AbstractServerConnectionFactory serverNet;

	@Autowired
	private AbstractClientConnectionFactory clientNio;

	@Autowired
	private AbstractServerConnectionFactory serverNio;

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

	@Test
	public void testConnectNet() throws Exception {
		testConnectGuts(this.clientNet, this.serverNet, "gwNet", true);
	}

	@Test
	public void testConnectNio() throws Exception {
		testConnectGuts(this.clientNio, this.serverNio, "gwNio", false);
	}

	@SuppressWarnings("unchecked")
	private void testConnectGuts(AbstractClientConnectionFactory client, AbstractServerConnectionFactory server,
			String gatewayName, boolean expectExceptionOnClose) throws Exception {

		TestingUtilities.waitListening(server, null);
		client.setPort(server.getPort());
		client.start();
		for (int i = 0; i < 100; i++) {
			TcpConnection connection = client.getConnection();
			connection.send(MessageBuilder.withPayload("Test").build());
			Message<?> message = serverSideChannel.receive(10000);
			assertThat(message).isNotNull();
			MessageHistory history = MessageHistory.read(message);
			//org.springframework.integration.test.util.TestUtils
			Properties componentHistoryRecord = TestUtils.locateComponentInHistory(history, gatewayName, 0);
			assertThat(componentHistoryRecord).isNotNull();
			assertThat(componentHistoryRecord.get("type").equals("ip:tcp-inbound-gateway")).isTrue();
			assertThat(message).isNotNull();
			assertThat(new String((byte[]) message.getPayload())).isEqualTo("Test");
		}
		int clientOpens = 0;
		int clientCloses = 0;
		int serverOpens = 0;
		int serverCloses = 0;
		int clientExceptions = 0;
		Message<TcpConnectionEvent> eventMessage;
		int i = 0;
		while (i++ < (expectExceptionOnClose ? 600 : 400)
				&& (eventMessage = (Message<TcpConnectionEvent>) events.receive(10000)) != null) {
			TcpConnectionEvent event = eventMessage.getPayload();
			if (event.getConnectionFactoryName().startsWith("client")) {
				if (event instanceof TcpConnectionOpenEvent) {
					clientOpens++;
				}
				else if (event instanceof TcpConnectionCloseEvent) {
					clientCloses++;
				}
				else if (event instanceof TcpConnectionExceptionEvent) {
					clientExceptions++;
				}
			}
			else if (event.getConnectionFactoryName().startsWith("server")) {
				if (event instanceof TcpConnectionOpenEvent) {
					serverOpens++;
				}
				else if (event instanceof TcpConnectionCloseEvent) {
					serverCloses++;
				}
			}
		}
		assertThat(clientOpens).isEqualTo(100);
		assertThat(clientCloses).isEqualTo(100);
		if (expectExceptionOnClose) {
			assertThat(clientExceptions).isEqualTo(100);
		}
		assertThat(serverOpens).isEqualTo(100);
		assertThat(serverCloses).isEqualTo(100);
	}

	@Test
	public void testConnectRaw() throws Exception {
		ByteArrayRawSerializer serializer = new ByteArrayRawSerializer();
		clientNet.setSerializer(serializer);
		serverNet.setDeserializer(serializer);
		clientNet.start();
		TcpConnection connection = clientNet.getConnection();
		connection.send(MessageBuilder.withPayload("Test").build());
		connection.close();
		Message<?> message = serverSideChannel.receive(10000);
		assertThat(message).isNotNull();
		MessageHistory history = MessageHistory.read(message);
		//org.springframework.integration.test.util.TestUtils
		Properties componentHistoryRecord = TestUtils.locateComponentInHistory(history, "gwNet", 0);
		assertThat(componentHistoryRecord).isNotNull();
		assertThat(componentHistoryRecord.get("type").equals("ip:tcp-inbound-gateway")).isTrue();
		assertThat(message).isNotNull();
		assertThat(new String((byte[]) message.getPayload())).isEqualTo("Test");
	}

	@Test
	public void testLookup() throws Exception {
		clientNet.start();
		TcpConnection connection = clientNet.getConnection();
		assertThat(connection.getConnectionId().contains("localhost")).isFalse();
		connection.close();
		clientNet.setLookupHost(true);
		connection = clientNet.getConnection();
		assertThat(connection.getConnectionId().contains("localhost")).isTrue();
		connection.close();
		clientNet.setLookupHost(false);
		connection = clientNet.getConnection();
		assertThat(connection.getConnectionId().contains("localhost")).isFalse();
		connection.close();
	}

}
