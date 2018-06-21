/*
 * Copyright 2016-2018 the original author or authors.
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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.aopalliance.intercept.MethodInterceptor;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.ip.tcp.TcpOutboundGateway;
import org.springframework.integration.ip.tcp.TcpReceivingChannelAdapter;
import org.springframework.integration.ip.tcp.TcpSendingMessageHandler;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.serializer.TcpCodecs;
import org.springframework.integration.ip.udp.MulticastSendingMessageHandler;
import org.springframework.integration.ip.udp.UdpServerListeningEvent;
import org.springframework.integration.ip.udp.UnicastReceivingChannelAdapter;
import org.springframework.integration.ip.util.TestingUtilities;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
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
	@Qualifier("clientTcpFlow.input")
	private MessageChannel clientTcpFlowInput;

	@Autowired
	private UnicastReceivingChannelAdapter udpInbound;

	@Autowired
	private QueueChannel udpIn;

	@Autowired
	private Config config;

	@Autowired
	private AtomicBoolean adviceCalled;

	@Test
	public void testTcpAdapters() {
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
		assertEquals("foo", Transformers.objectToString().transform(receivedMessage).getPayload());
		client.stop();
		server.stop();
	}

	@Test
	public void testTcpGateways() {
		TestingUtilities.waitListening(this.server1, null);
		this.client1.stop();
		this.client1.setPort(this.server1.getPort());
		this.client1.start();

		MessagingTemplate messagingTemplate = new MessagingTemplate(this.clientTcpFlowInput);

		assertThat(messagingTemplate.convertSendAndReceive("foo", String.class), equalTo("FOO"));

		assertTrue(this.adviceCalled.get());
	}

	@Test
	public void testUdp() throws Exception {
		assertTrue(this.config.listeningLatch.await(10, TimeUnit.SECONDS));
		assertEquals(this.udpInbound.getPort(), this.config.serverPort);
		Message<String> outMessage = MessageBuilder.withPayload("foo")
				.setHeader("udp_dest", "udp://localhost:" + this.udpInbound.getPort())
				.build();
		this.udpOut.send(outMessage);
		Message<?> received = this.udpIn.receive(10000);
		assertNotNull(received);
		assertEquals("foo", Transformers.objectToString().transform(received).getPayload());
	}

	@Test
	public void testUdpInheritance() {
		UdpMulticastOutboundChannelAdapterSpec udpMulticastOutboundChannelAdapterSpec =
				Udp.outboundMulticastAdapter("headers['udp_dest']");

		UdpMulticastOutboundChannelAdapterSpec udpMulticastOutboundChannelAdapterSpec1 =
				udpMulticastOutboundChannelAdapterSpec.lengthCheck(true);

		UdpMulticastOutboundChannelAdapterSpec udpMulticastOutboundChannelAdapterSpec2 =
				udpMulticastOutboundChannelAdapterSpec1.timeToLive(10);

		assertThat(udpMulticastOutboundChannelAdapterSpec2.get(), instanceOf(MulticastSendingMessageHandler.class));
	}

	@Configuration
	@EnableIntegration
	public static class Config {

		private final CountDownLatch listeningLatch = new CountDownLatch(1);

		private volatile int serverPort;

		@Bean
		public AbstractServerConnectionFactory server1() {
			return Tcp.netServer(0)
					.serializer(TcpCodecs.lengthHeader1())
					.deserializer(TcpCodecs.crlf())
					.get();
		}

		@Bean
		public IntegrationFlow inTcpGateway() {
			return IntegrationFlows.from(Tcp.inboundGateway(server1()))
					.transform(Transformers.objectToString())
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
			return f -> f.handle(Udp.outboundAdapter(m -> m.getHeaders().get("udp_dest")));
		}

		@Bean
		public ApplicationListener<UdpServerListeningEvent> events() {
			return (ApplicationListener<UdpServerListeningEvent>) event -> {
				this.serverPort = event.getPort();
				this.listeningLatch.countDown();
			};
		}

		@Bean
		public AbstractClientConnectionFactory client1() {
			return Tcp.netClient("localhost", server1().getPort())
					.serializer(TcpCodecs.crlf())
					.deserializer(TcpCodecs.lengthHeader1())
					.get();
		}

		@Bean
		public TcpOutboundGateway tcpOut() {
			return Tcp.outboundGateway(client1())
					.remoteTimeout(m -> 5000)
					.get();
		}

		@Bean
		public AtomicBoolean adviceCalled() {
			return new AtomicBoolean();
		}

		@Bean
		public MethodInterceptor testAdvice() {
			return invocation -> {
				adviceCalled().set(true);
				return invocation.proceed();
			};
		}

		@Bean
		public IntegrationFlow clientTcpFlow() {
			return f -> f
					.handle(tcpOut(), e -> e.advice(testAdvice()))
					.transform(Transformers.objectToString());
		}

	}

}
