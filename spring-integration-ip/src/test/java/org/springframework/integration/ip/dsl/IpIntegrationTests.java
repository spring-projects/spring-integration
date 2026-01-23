/*
 * Copyright 2016-present the original author or authors.
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

package org.springframework.integration.ip.dsl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.integration.MessageTimeoutException;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.dsl.context.IntegrationFlowContext.IntegrationFlowRegistration;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.CachingClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpConnectionOpenEvent;
import org.springframework.integration.ip.tcp.connection.TcpConnectionServerListeningEvent;
import org.springframework.integration.ip.tcp.connection.TcpNetClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNetServerConnectionFactory;
import org.springframework.integration.ip.tcp.inbound.TcpReceivingChannelAdapter;
import org.springframework.integration.ip.tcp.outbound.TcpOutboundGateway;
import org.springframework.integration.ip.tcp.outbound.TcpSendingMessageHandler;
import org.springframework.integration.ip.tcp.serializer.ByteArrayRawSerializer;
import org.springframework.integration.ip.tcp.serializer.TcpCodecs;
import org.springframework.integration.ip.udp.UdpServerListeningEvent;
import org.springframework.integration.ip.udp.inbound.UnicastReceivingChannelAdapter;
import org.springframework.integration.ip.udp.outbound.MulticastSendingMessageHandler;
import org.springframework.integration.ip.util.TestingUtilities;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 5.0
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class IpIntegrationTests implements TestApplicationContextAware {

	@Autowired
	private ConfigurableApplicationContext applicationContext;

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
	private TcpOutboundGateway tcpOutAsync;

	@Autowired
	private Config config;

	@Autowired
	private AtomicBoolean adviceCalled;

	@Autowired
	@Qualifier("unsolicitedServerSide.input")
	private MessageChannel unsolicitedServerSide;

	@Autowired
	private QueueChannel unsolicited;

	@Test
	void testTcpAdapters() {
		ApplicationEventPublisher publisher = e -> {
		};
		AbstractServerConnectionFactory server = Tcp.netServer(0).backlog(2).soTimeout(5000).id("server").getObject();
		assertThat(server.getComponentName()).isEqualTo("server");
		server.setApplicationEventPublisher(publisher);
		server.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		server.afterPropertiesSet();
		TcpReceivingChannelAdapter inbound = Tcp.inboundAdapter(server).getObject();
		QueueChannel received = new QueueChannel();
		inbound.setOutputChannel(received);
		inbound.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		inbound.afterPropertiesSet();
		inbound.start();
		TestingUtilities.waitListening(server, null);
		AbstractClientConnectionFactory client = Tcp.netClient("localhost", server.getPort()).id("client").getObject();
		assertThat(client.getComponentName()).isEqualTo("client");
		client.setApplicationEventPublisher(publisher);
		client.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		client.afterPropertiesSet();
		TcpSendingMessageHandler handler = Tcp.outboundAdapter(client).getObject();
		handler.start();
		handler.handleMessage(new GenericMessage<>("test"));
		Message<?> receivedMessage = received.receive(10000);
		assertThat(receivedMessage).isNotNull();
		assertThat(Transformers.objectToString().transform(receivedMessage).getPayload()).isEqualTo("test");
		client.stop();
		server.stop();
	}

	@Test
	void testTcpGateways() {
		TestingUtilities.waitListening(this.server1, null);
		this.client1.stop();
		this.client1.setPort(this.server1.getPort());
		this.client1.start();

		MessagingTemplate messagingTemplate = new MessagingTemplate(this.clientTcpFlowInput);
		messagingTemplate.setBeanFactory(TEST_INTEGRATION_CONTEXT);

		assertThat(messagingTemplate.convertSendAndReceive("test", String.class)).isEqualTo("TEST");
		assertThat(messagingTemplate.convertSendAndReceive("junk", String.class)).isEqualTo("error:non-convertible");
		assertThat(this.adviceCalled.get()).isTrue();

		GenericMessage<String> unsol = new GenericMessage<>("test",
				Collections.singletonMap(IpHeaders.CONNECTION_ID, this.config.connectionId));
		this.unsolicitedServerSide.send(unsol);
		assertThat(this.unsolicited.receive(10_000).getPayload()).isEqualTo("test".getBytes());
	}

	@Test
	void testUdp() throws Exception {
		assertThat(this.config.listeningLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(this.config.serverPort).isEqualTo(this.udpInbound.getPort());
		Message<String> outMessage = MessageBuilder.withPayload("test")
				.setHeader("udp_dest", "udp://localhost:" + this.udpInbound.getPort())
				.build();
		this.udpOut.send(outMessage);
		this.udpIn.setTaskScheduler(new SimpleAsyncTaskScheduler());
		Message<?> received = this.udpIn.receive(10000);
		assertThat(received).isNotNull();
		assertThat(Transformers.objectToString().transform(received).getPayload()).isEqualTo("test");
	}

	@Test
	void testUdpInheritance() {
		UdpMulticastOutboundChannelAdapterSpec udpMulticastOutboundChannelAdapterSpec =
				Udp.outboundMulticastAdapter("headers['udp_dest']");

		UdpMulticastOutboundChannelAdapterSpec udpMulticastOutboundChannelAdapterSpec1 =
				udpMulticastOutboundChannelAdapterSpec.lengthCheck(true);

		UdpMulticastOutboundChannelAdapterSpec udpMulticastOutboundChannelAdapterSpec2 =
				udpMulticastOutboundChannelAdapterSpec1.timeToLive(10);

		assertThat(udpMulticastOutboundChannelAdapterSpec2.getObject())
				.isInstanceOf(MulticastSendingMessageHandler.class);
	}

	@Test
	void testCloseStream() throws InterruptedException {
		IntegrationFlow server = IntegrationFlow.from(Tcp.inboundGateway(Tcp.netServer(0)
						.deserializer(new ByteArrayRawSerializer())))
				.<byte[], String>transform(p -> "reply:" + new String(p).toUpperCase())
				.get();
		CountDownLatch latch = new CountDownLatch(1);
		AtomicInteger port = new AtomicInteger();
		this.applicationContext.addApplicationListener(
				(ApplicationListener<TcpConnectionServerListeningEvent>) event -> {
					port.set(event.getPort());
					latch.countDown();
				});
		this.flowContext.registration(server)
				.id("streamCloseServer")
				.register();
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		IntegrationFlow client = IntegrationFlow.from(MessageChannels.direct())
				.handle(Tcp.outboundGateway(Tcp.netClient("localhost", port.get())
								.singleUseConnections(true)
								.serializer(new ByteArrayRawSerializer()))
						.remoteTimeout(20_000)
						.closeStreamAfterSend(true))
				.transform(Transformers.objectToString())
				.get();
		IntegrationFlowRegistration clientRegistration = this.flowContext.registration(client)
				.id("streamCloseClient")
				.register();
		assertThat(clientRegistration.getMessagingTemplate()
				.convertSendAndReceive("test", String.class)).isEqualTo("reply:TEST");
	}

	@Test
	void async() {
		assertThat(TestUtils.<Boolean>getPropertyValue(this.tcpOutAsync, "async")).isTrue();
	}

	@Autowired
	private AbstractServerConnectionFactory server2;

	@Autowired
	private TcpNetClientConnectionFactory client3;

	@Autowired
	@Qualifier("outboundFlow.input")
	MessageChannel outboundFlowInput;

	@Autowired
	PollableChannel cachingRepliesChannel;

	@Test
	void allRepliesAreReceivedViaLimitedCachingConnectionFactory() {
		this.client3.stop();
		TestingUtilities.waitListening(this.server2, null);
		this.client3.setPort(this.server2.getPort());
		this.client3.start();

		List<String> expected =
				IntStream.rangeClosed('a', 'z')
						.mapToObj((characterCode) -> (char) characterCode)
						.map((character) -> "" + character)
						.parallel()
						.peek((character) -> this.outboundFlowInput.send(new GenericMessage<>(character)))
						.map(String::toUpperCase)
						.toList();

		List<String> replies = new ArrayList<>();

		for (int i = 0; i < expected.size(); i++) {
			Message<?> replyMessage = this.cachingRepliesChannel.receive(10_000);
			assertThat(replyMessage).isNotNull();
			replies.add(replyMessage.getPayload().toString());
		}

		assertThat(replies).containsAll(expected);

		assertThat(config.openEvents).hasSizeLessThanOrEqualTo(5);
	}

	@Configuration
	@EnableIntegration
	public static class Config {

		private final CountDownLatch listeningLatch = new CountDownLatch(1);

		private volatile int serverPort;

		private volatile String connectionId;

		@Bean
		public TcpNetServerConnectionFactorySpec server1() {
			return Tcp.netServer(0)
					.serializer(TcpCodecs.lengthHeader1())
					.deserializer(TcpCodecs.crlf());
		}

		@Bean
		public IntegrationFlow inTcpGateway(TcpNetServerConnectionFactory server1) {
			return IntegrationFlow.from(
							Tcp.inboundGateway(server1)
									.replyTimeout(1)
									.errorOnTimeout(true)
									.errorChannel("inTcpGatewayErrorFlow.input"))
					.handle(this, "captureId")
					.transform(Transformers.objectToString())
					.<String>filter((payload) -> !"junk".equals(payload))
					.<String, String>transform(String::toUpperCase)
					.get();
		}

		public Message<?> captureId(Message<?> msg) {
			this.connectionId = msg.getHeaders().get(IpHeaders.CONNECTION_ID, String.class);
			return msg;
		}

		@Bean
		public IntegrationFlow unsolicitedServerSide(TcpNetServerConnectionFactory server1) {
			return f -> f.handle(Tcp.outboundAdapter(server1));
		}

		@Bean
		public IntegrationFlow inTcpGatewayErrorFlow() {
			return (flow) -> flow
					.<Exception>handle((payload, headers) -> {
						if (payload instanceof MessageTimeoutException) {
							return "error:non-convertible";
						}
						else {
							ReflectionUtils.rethrowRuntimeException(payload);
							return null;
						}
					});
		}

		@Bean
		public IntegrationFlow inUdpAdapter() {
			return IntegrationFlow.from(Udp.inboundAdapter(0))
					.channel(udpIn())
					.get();
		}

		@Bean
		public QueueChannel udpIn() {
			return new QueueChannel();
		}

		@Bean
		public IntegrationFlow outUdpAdapter() {
			return f -> f.handle(Udp.outboundAdapter(m -> m.getHeaders().get("udp_dest"))
					.configureSocket(socket -> socket.setTrafficClass(0x10)));
		}

		@Bean
		public ApplicationListener<UdpServerListeningEvent> events() {
			return event -> {
				this.serverPort = event.getPort();
				this.listeningLatch.countDown();
			};
		}

		@Bean
		public TcpNetClientConnectionFactorySpec client1() {
			// The port from server is assigned
			return Tcp.netClient("localhost", 0)
					.serializer(TcpCodecs.crlf())
					.deserializer(TcpCodecs.lengthHeader1());
		}

		@Bean
		public TcpOutboundGatewaySpec tcpOut(TcpNetClientConnectionFactory client1) {
			return Tcp.outboundGateway(client1)
					.remoteTimeout(m -> 5000)
					.unsolicitedMessageChannelName("unsolicited");
		}

		@Bean
		public QueueChannel unsolicited() {
			return new QueueChannel();
		}

		@Bean
		public TcpNetClientConnectionFactorySpec client2() {
			// The port from server is assigned
			return Tcp.netClient("localhost", 0)
					.serializer(TcpCodecs.crlf())
					.deserializer(TcpCodecs.lengthHeader1());
		}

		@Bean
		public TcpOutboundGatewaySpec tcpOutAsync(TcpNetClientConnectionFactory client2) {
			return Tcp.outboundGateway(client2)
					.async(true)
					.remoteTimeout(m -> 5000);
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
		public IntegrationFlow clientTcpFlow(TcpOutboundGateway tcpOut) {
			return f -> f
					.handle(tcpOut, e -> e.advice(testAdvice()))
					.transform(Transformers.objectToString());
		}

		@Bean
		public TcpNetServerConnectionFactorySpec server2() {
			return Tcp.netServer(0);
		}

		@Bean
		public IntegrationFlow server2Flow(TcpNetServerConnectionFactory server2) {
			return IntegrationFlow.from(Tcp.inboundGateway(server2))
					.transform(Transformers.objectToString())
					.<String, String>transform(String::toUpperCase)
					.get();
		}

		@Bean
		public TcpNetClientConnectionFactorySpec client3() {
			// The port from server is assigned
			return Tcp.netClient("localhost", 0);
		}

		final List<TcpConnectionOpenEvent> openEvents = new ArrayList<>();

		@EventListener
		void connectionOpened(TcpConnectionOpenEvent tcpConnectionOpenEvent) {
			if ("client3".equals(tcpConnectionOpenEvent.getConnectionFactoryName())) {
				this.openEvents.add(tcpConnectionOpenEvent);
			}
		}

		@Bean
		CachingClientConnectionFactory cachingClient(TcpNetClientConnectionFactory client3) {
			var cachingClientConnectionFactory = new CachingClientConnectionFactory(client3, 5);
			cachingClientConnectionFactory.setConnectionWaitTimeout(10_000);
			return cachingClientConnectionFactory;
		}

		@Bean
		IntegrationFlow outboundFlow(CachingClientConnectionFactory cachingClient) {
			return (flow) -> flow.handle(Tcp.outboundAdapter(cachingClient));
		}

		@Bean
		IntegrationFlow inboundFlow(CachingClientConnectionFactory cachingClient) {
			return IntegrationFlow.from(Tcp.inboundAdapter(cachingClient))
					.transform(Transformers.objectToString())
					.channel((channels) -> channels.queue("cachingRepliesChannel"))
					.get();
		}

	}

}
