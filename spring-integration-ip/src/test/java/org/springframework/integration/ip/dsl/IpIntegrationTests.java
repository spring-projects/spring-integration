/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.ip.dsl;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.dsl.context.IntegrationFlowRegistration;
import org.springframework.integration.ip.tcp.TcpReceivingChannelAdapter;
import org.springframework.integration.ip.tcp.TcpSendingMessageHandler;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.serializer.TcpCodecs;
import org.springframework.integration.ip.udp.UnicastReceivingChannelAdapter;
import org.springframework.integration.ip.util.TestingUtilities;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transformer.ObjectToStringTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Gary Russell
 * @since 5.0
 *
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class IpIntegrationTests {

	@Autowired
	private AbstractServerConnectionFactory server1;

	@Autowired
	private AbstractClientConnectionFactory client1;

	@Autowired
	private IntegrationFlowContext flowContext;

	@Autowired
	@Qualifier("outUdpAdapter.input")
	private MessageChannel udpOut;

	@Autowired
	private UnicastReceivingChannelAdapter udpInbound;

	@Autowired
	private QueueChannel udpIn;

	@Test
	public void testTcpAdapters() throws Exception {
		ApplicationEventPublisher publisher = e -> { };
		AbstractServerConnectionFactory server = Tcp.netServer(0).backlog(2).soTimeout(5000).id("server").get();
		assertEquals("server", server.getComponentName());
		server.setApplicationEventPublisher(publisher);
		server.afterPropertiesSet();
		TcpReceivingChannelAdapter inbound = Tcp.inboundAdapter(server).get();
		QueueChannel received = new QueueChannel();
		inbound.setOutputChannel(received);
		inbound.afterPropertiesSet();
		inbound.start();
		TestingUtilities.waitListening(server, null);
		AbstractClientConnectionFactory client = Tcp.netClient("localhost", server.getPort()).id("client").get();
		assertEquals("client", client.getComponentName());
		client.setApplicationEventPublisher(publisher);
		client.afterPropertiesSet();
		TcpSendingMessageHandler handler = Tcp.outboundAdapter(client).get();
		handler.start();
		handler.handleMessage(new GenericMessage<>("foo"));
		Message<?> receivedMessage = received.receive(10000);
		assertNotNull(receivedMessage);
		assertEquals("foo", new ObjectToStringTransformer().transform(receivedMessage).getPayload());
		client.stop();
		server.stop();
	}

	@Test
	public void testTcpGateways() {
		TestingUtilities.waitListening(this.server1, null);
		this.client1.setPort(this.server1.getPort());
		IntegrationFlow flow = f -> f
				.handle(Tcp.outboundGateway(this.client1))
				.transform(new ObjectToStringTransformer());
		IntegrationFlowRegistration theFlow = this.flowContext.registration(flow).register();
		assertThat(theFlow.getMessagingTemplate().convertSendAndReceive("foo", String.class), equalTo("FOO"));
	}

	@Test
	public void testUdp() {
		TestingUtilities.waitListening(this.udpInbound, null);
		Message<String> outMessage = MessageBuilder.withPayload("foo")
				.setHeader("udp_dest", "udp://localhost:" + this.udpInbound.getPort())
				.build();
		this.udpOut.send(outMessage);
		Message<?> received = this.udpIn.receive(10000);
		assertNotNull(received);
		assertEquals("foo", new ObjectToStringTransformer().transform(received).getPayload());
	}

	@Configuration
	@EnableIntegration
	public static class Config {

		@Bean
		public AbstractServerConnectionFactory server1() {
			return Tcp.netServer(0)
					.serializer(TcpCodecs.lengthHeader1())
					.deserializer(TcpCodecs.crlf())
					.get();
		}

		@Bean
		public AbstractClientConnectionFactory client1() {
			return Tcp.netClient("localhost", 0)
					.serializer(TcpCodecs.crlf())
					.deserializer(TcpCodecs.lengthHeader1())
					.get();
		}

		@Bean
		public IntegrationFlow inTcpGateway() {
			return IntegrationFlows.from(Tcp.inboundGateway(server1()))
					.transform(new ObjectToStringTransformer())
					.<String, String>transform(String::toUpperCase)
					.get();
		}

		@Bean
		public IntegrationFlow inUdpAdapter() {
			return IntegrationFlows.from(Udp.inboundAdapter(0))
					.channel(udpIn())
					.get();
		}

		@Bean
		public QueueChannel udpIn() {
			return new QueueChannel();
		}

		@Bean
		public IntegrationFlow outUdpAdapter() {
			return f -> f.handle(Udp.outboundAdapter("headers['udp_dest']"));
		}

	}

}
