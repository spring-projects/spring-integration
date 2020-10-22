/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.ip.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.time.Duration;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.ip.tcp.TcpInboundGateway;
import org.springframework.integration.ip.tcp.TcpOutboundGateway;
import org.springframework.integration.ip.tcp.TcpReceivingChannelAdapter;
import org.springframework.integration.ip.tcp.TcpSendingMessageHandler;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory;
import org.springframework.integration.ip.tcp.connection.DefaultTcpNetConnectionSupport;
import org.springframework.integration.ip.tcp.connection.DefaultTcpNetSSLSocketFactorySupport;
import org.springframework.integration.ip.tcp.connection.DefaultTcpNioSSLConnectionSupport;
import org.springframework.integration.ip.tcp.connection.DefaultTcpSSLContextSupport;
import org.springframework.integration.ip.tcp.connection.TcpConnectionEvent;
import org.springframework.integration.ip.tcp.connection.TcpConnectionSupport;
import org.springframework.integration.ip.tcp.connection.TcpMessageMapper;
import org.springframework.integration.ip.tcp.connection.TcpNetClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNetServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNioClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNioServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpSSLContextSupport;
import org.springframework.integration.ip.tcp.connection.TcpSocketFactorySupport;
import org.springframework.integration.ip.tcp.connection.TcpSocketSupport;
import org.springframework.integration.ip.udp.DatagramPacketMessageMapper;
import org.springframework.integration.ip.udp.MulticastReceivingChannelAdapter;
import org.springframework.integration.ip.udp.MulticastSendingMessageHandler;
import org.springframework.integration.ip.udp.SocketCustomizer;
import org.springframework.integration.ip.udp.UnicastReceivingChannelAdapter;
import org.springframework.integration.ip.udp.UnicastSendingMessageHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Gary Russell
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 *
 * @since 2.0
 */
@SpringJUnitConfig
@DirtiesContext
public class ParserUnitTests {

	@Autowired
	ApplicationContext ctx;

	@Autowired
	@Qualifier("testInUdp")
	UnicastReceivingChannelAdapter udpIn;

	@Autowired
	@Qualifier("testInUdpMulticast")
	MulticastReceivingChannelAdapter udpInMulticast;

	@Autowired
	@Qualifier("testInTcp")
	TcpReceivingChannelAdapter tcpIn;

	@Autowired
	@Qualifier("testOutUdp.handler")
	UnicastSendingMessageHandler udpOut;

	@Autowired
	@Qualifier("testOutUdpiMulticast.handler")
	MulticastSendingMessageHandler udpOutMulticast;

	@Autowired
	@Qualifier("testOutTcpNio")
	AbstractEndpoint tcpOutEndpoint;

	@Autowired
	@Qualifier("testOutTcpNio.handler")
	TcpSendingMessageHandler tcpOut;

	@Autowired
	EventDrivenConsumer testOutTcpNio;

	@Autowired
	@Qualifier("inGateway1")
	TcpInboundGateway tcpInboundGateway1;

	@Autowired
	@Qualifier("inGateway2")
	TcpInboundGateway tcpInboundGateway2;

	@Autowired
	@Qualifier("outGateway.handler")
	TcpOutboundGateway tcpOutboundGateway;

	@Autowired
	@Qualifier("outAdviceGateway.handler")
	TcpOutboundGateway outAdviceGateway;

	// verify we can still inject by generated name
	@Autowired
	@Qualifier("org.springframework.integration.ip.tcp.TcpOutboundGateway#0")
	TcpOutboundGateway tcpOutboundGatewayByGeneratedName;

	@Autowired
	EventDrivenConsumer outGateway;

	@Autowired
	@Qualifier("externalTE")
	TaskExecutor taskExecutor;

	@Autowired
	AbstractConnectionFactory client1;

	@Autowired
	AbstractConnectionFactory client2;

	@Autowired
	AbstractConnectionFactory cfC1;

	@Autowired
	AbstractConnectionFactory cfC2;

	@Autowired
	AbstractConnectionFactory cfC3;

	@Autowired
	AbstractConnectionFactory cfC4;

	@Autowired
	AbstractConnectionFactory cfC5;

	@Autowired
	Serializer<?> serializer;

	@Autowired
	Deserializer<?> deserializer;

	@Autowired
	AbstractConnectionFactory server1;

	@Autowired
	AbstractConnectionFactory server2;

	@Autowired
	AbstractConnectionFactory cfS1;

	@Autowired
	AbstractConnectionFactory cfS1Nio;

	@Autowired
	AbstractConnectionFactory cfS2;

	@Autowired
	AbstractConnectionFactory cfS3;

	@Autowired
	@Qualifier("tcpNewOut1.handler")
	TcpSendingMessageHandler tcpNewOut1;

	@Autowired
	@Qualifier("tcpNewOut2.handler")
	TcpSendingMessageHandler tcpNewOut2;

	@Autowired
	TcpReceivingChannelAdapter tcpNewIn1;

	@Autowired
	TcpReceivingChannelAdapter tcpNewIn2;

	@Autowired
	private MessageChannel errorChannel;

	@Autowired
	private MessageChannel udpChannel;

	@Autowired
	private MessageChannel udpAdviceChannel;

	@Autowired
	private MessageChannel tcpAdviceChannel;

	@Autowired
	private MessageChannel tcpAdviceGateChannel;

	@Autowired
	private MessageChannel tcpChannel;

	@Autowired
	TcpReceivingChannelAdapter tcpInClientMode;

	@Autowired
	TcpInboundGateway inGatewayClientMode;

	@Autowired
	TaskScheduler sched;

	@Autowired
	@Qualifier("tcpOutClientMode.handler")
	TcpSendingMessageHandler tcpOutClientMode;

	@Autowired
	MessageChannel tcpAutoChannel;

	@Autowired
	MessageChannel udpAutoChannel;

	@Autowired @Qualifier("tcpAutoChannel.adapter")
	TcpReceivingChannelAdapter tcpAutoAdapter;

	@Autowired @Qualifier("udpAutoChannel.adapter")
	UnicastReceivingChannelAdapter udpAutoAdapter;

	@Autowired
	TcpNetServerConnectionFactory secureServer;

	@Autowired
	TcpNioServerConnectionFactory secureServerNio;

	@Autowired
	TcpSocketFactorySupport socketFactorySupport;

	@Autowired
	TcpSocketSupport socketSupport;

	@Autowired
	DefaultTcpNetConnectionSupport netConnectionSupport;

	@Autowired
	TcpSSLContextSupport contextSupport;

	@Autowired
	TcpMessageMapper mapper;

	@Autowired
	SocketCust socketCustomizer;

	private static CountDownLatch adviceCalled = new CountDownLatch(1);

	@Test
	public void testInUdp() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(udpIn);
		assertThat(dfa.getPropertyValue("poolSize")).isEqualTo(27);
		assertThat(dfa.getPropertyValue("receiveBufferSize")).isEqualTo(29);
		assertThat(dfa.getPropertyValue("soReceiveBufferSize")).isEqualTo(30);
		assertThat(dfa.getPropertyValue("soSendBufferSize")).isEqualTo(31);
		assertThat(dfa.getPropertyValue("soTimeout")).isEqualTo(32);
		assertThat(udpIn.getComponentName()).isEqualTo("testInUdp");
		assertThat(udpIn.getComponentType()).isEqualTo("ip:udp-inbound-channel-adapter");
		assertThat(dfa.getPropertyValue("localAddress")).isEqualTo("127.0.0.1");
		assertThat(dfa.getPropertyValue("taskExecutor")).isSameAs(taskExecutor);
		assertThat(dfa.getPropertyValue("errorChannel")).isEqualTo(errorChannel);
		DatagramPacketMessageMapper mapper = (DatagramPacketMessageMapper) dfa.getPropertyValue("mapper");
		DirectFieldAccessor mapperAccessor = new DirectFieldAccessor(mapper);
		assertThat((Boolean) mapperAccessor.getPropertyValue("lookupHost")).isFalse();
		assertThat(TestUtils.getPropertyValue(udpIn, "autoStartup", Boolean.class)).isFalse();
		assertThat(dfa.getPropertyValue("phase")).isEqualTo(1234);
		assertThat(dfa.getPropertyValue("socketCustomizer")).isSameAs(this.socketCustomizer);
	}

	@Test
	public void testInUdpMulticast() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(udpInMulticast);
		assertThat(dfa.getPropertyValue("group")).isEqualTo("225.6.7.8");
		assertThat(dfa.getPropertyValue("poolSize")).isEqualTo(27);
		assertThat(dfa.getPropertyValue("receiveBufferSize")).isEqualTo(29);
		assertThat(dfa.getPropertyValue("soReceiveBufferSize")).isEqualTo(30);
		assertThat(dfa.getPropertyValue("soSendBufferSize")).isEqualTo(31);
		assertThat(dfa.getPropertyValue("soTimeout")).isEqualTo(32);
		assertThat(dfa.getPropertyValue("localAddress")).isEqualTo("127.0.0.1");
		assertThat(dfa.getPropertyValue("taskExecutor")).isNotSameAs(taskExecutor);
		assertThat(dfa.getPropertyValue("errorChannel")).isNull();
		DatagramPacketMessageMapper mapper = (DatagramPacketMessageMapper) dfa.getPropertyValue("mapper");
		DirectFieldAccessor mapperAccessor = new DirectFieldAccessor(mapper);
		assertThat((Boolean) mapperAccessor.getPropertyValue("lookupHost")).isTrue();
	}

	@Test
	public void testInTcp() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(tcpIn);
		assertThat(dfa.getPropertyValue("serverConnectionFactory")).isSameAs(cfS1);
		assertThat(tcpIn.getComponentName()).isEqualTo("testInTcp");
		assertThat(tcpIn.getComponentType()).isEqualTo("ip:tcp-inbound-channel-adapter");
		assertThat(dfa.getPropertyValue("errorChannel")).isEqualTo(errorChannel);
		assertThat(cfS1.isLookupHost()).isFalse();
		assertThat(tcpIn.isAutoStartup()).isFalse();
		assertThat(tcpIn.getPhase()).isEqualTo(124);
		TcpMessageMapper cfS1Mapper = TestUtils.getPropertyValue(cfS1, "mapper", TcpMessageMapper.class);
		assertThat(cfS1Mapper).isSameAs(mapper);
		assertThat(TestUtils.getPropertyValue(cfS1Mapper, "applySequence", Boolean.class)).isTrue();
		Object socketSupport = TestUtils.getPropertyValue(cfS1, "tcpSocketFactorySupport");
		assertThat(socketSupport instanceof DefaultTcpNetSSLSocketFactorySupport).isTrue();
		assertThat(TestUtils.getPropertyValue(socketSupport, "sslContext")).isNotNull();

		TcpSSLContextSupport tcpSSLContextSupport = new DefaultTcpSSLContextSupport("http:foo", "file:bar", "", "");
		assertThat(TestUtils.getPropertyValue(tcpSSLContextSupport, "keyStore") instanceof UrlResource).isTrue();
		assertThat(TestUtils.getPropertyValue(tcpSSLContextSupport, "trustStore") instanceof UrlResource).isTrue();
	}

	@Test
	public void testInTcpNioSSLDefaultConfig() {
		assertThat(cfS1Nio.isLookupHost()).isFalse();
		assertThat(TestUtils.getPropertyValue(cfS1Nio, "mapper.applySequence", Boolean.class)).isTrue();
		Object connectionSupport = TestUtils.getPropertyValue(cfS1Nio, "tcpNioConnectionSupport");
		assertThat(connectionSupport instanceof DefaultTcpNioSSLConnectionSupport).isTrue();
		assertThat(TestUtils.getPropertyValue(connectionSupport, "sslContext")).isNotNull();
		assertThat(TestUtils.getPropertyValue(this.cfS1Nio, "sslHandshakeTimeout")).isEqualTo(43);
		assertThat(TestUtils.getPropertyValue(this.cfS1Nio, "tcpNioConnectionSupport"))
				.isSameAs(this.ctx.getBean(DefaultTcpNioSSLConnectionSupport.class));
	}

	@Test
	public void testOutUdp() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(udpOut);
		assertThat(dfa.getPropertyValue("host")).isEqualTo("localhost");
		DatagramPacketMessageMapper mapper = (DatagramPacketMessageMapper) dfa
				.getPropertyValue("mapper");
		String ackAddress = (String) new DirectFieldAccessor(mapper)
				.getPropertyValue("ackAddress");
		assertThat(ackAddress).startsWith("somehost:");
		assertThat(dfa.getPropertyValue("ackTimeout")).isEqualTo(51);
		assertThat(dfa.getPropertyValue("waitForAck")).isEqualTo(true);
		assertThat(dfa.getPropertyValue("soReceiveBufferSize")).isEqualTo(52);
		assertThat(dfa.getPropertyValue("soSendBufferSize")).isEqualTo(53);
		assertThat(dfa.getPropertyValue("soTimeout")).isEqualTo(54);
		assertThat(dfa.getPropertyValue("localAddress")).isEqualTo("127.0.0.1");
		assertThat(dfa.getPropertyValue("taskExecutor")).isSameAs(taskExecutor);
		assertThat(dfa.getPropertyValue("order")).isEqualTo(23);
		assertThat(udpOut.getComponentName()).isEqualTo("testOutUdp");
		assertThat(udpOut.getComponentType()).isEqualTo("ip:udp-outbound-channel-adapter");
		assertThat(dfa.getPropertyValue("socketCustomizer")).isSameAs(this.socketCustomizer);
	}

	@Test
	public void testOutUdpMulticast() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(udpOutMulticast);
		assertThat(dfa.getPropertyValue("host")).isEqualTo("225.6.7.8");
		DatagramPacketMessageMapper mapper = (DatagramPacketMessageMapper) dfa
				.getPropertyValue("mapper");
		String ackAddress = (String) new DirectFieldAccessor(mapper)
				.getPropertyValue("ackAddress");
		assertThat(ackAddress).startsWith("somehost:");
		assertThat(dfa.getPropertyValue("ackTimeout")).isEqualTo(51);
		assertThat(dfa.getPropertyValue("waitForAck")).isEqualTo(true);
		assertThat(dfa.getPropertyValue("soReceiveBufferSize")).isEqualTo(52);
		assertThat(dfa.getPropertyValue("soSendBufferSize")).isEqualTo(53);
		assertThat(dfa.getPropertyValue("soTimeout")).isEqualTo(54);
		assertThat(dfa.getPropertyValue("timeToLive")).isEqualTo(55);
		assertThat(dfa.getPropertyValue("order")).isEqualTo(12);
	}

	@Test
	public void testUdpOrder() {
		@SuppressWarnings("unchecked")
		Set<MessageHandler> handlers = (Set<MessageHandler>) TestUtils
				.getPropertyValue(
						TestUtils.getPropertyValue(this.udpChannel, "dispatcher"),
						"handlers");
		Iterator<MessageHandler> iterator = handlers.iterator();
		assertThat(iterator.next()).isSameAs(this.udpOutMulticast);
		assertThat(iterator.next()).isSameAs(this.udpOut);
	}

	@Test
	public void udpAdvice() throws InterruptedException {
		adviceCalled = new CountDownLatch(1);
		this.udpAdviceChannel.send(new GenericMessage<String>("foo"));
		assertThat(adviceCalled.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	public void tcpAdvice() throws InterruptedException {
		adviceCalled = new CountDownLatch(1);
		this.tcpAdviceChannel.send(new GenericMessage<String>("foo"));
		assertThat(adviceCalled.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	public void tcpGatewayAdvice() throws InterruptedException {
		adviceCalled = new CountDownLatch(1);
		this.tcpAdviceGateChannel.send(new GenericMessage<String>("foo"));
		assertThat(adviceCalled.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	public void testOutTcp() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(tcpOut);
		assertThat(dfa.getPropertyValue("clientConnectionFactory")).isSameAs(cfC1);
		assertThat(tcpOut.getComponentName()).isEqualTo("testOutTcpNio");
		assertThat(tcpOut.getComponentType()).isEqualTo("ip:tcp-outbound-channel-adapter");
		assertThat(cfC1.isLookupHost()).isFalse();
		assertThat(dfa.getPropertyValue("order")).isEqualTo(35);
		assertThat(tcpOutEndpoint.isAutoStartup()).isFalse();
		assertThat(tcpOutEndpoint.getPhase()).isEqualTo(125);
		assertThat((Boolean) TestUtils.getPropertyValue(
				TestUtils.getPropertyValue(cfC1, "mapper"), "applySequence")).isFalse();
		assertThat(TestUtils.getPropertyValue(cfC1, "readDelay")).isEqualTo(10000L);
		assertThat(TestUtils.getPropertyValue(cfC1, "connectTimeout")).isEqualTo(Duration.ofSeconds(70));
	}

	@Test
	public void testInGateway1() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(tcpInboundGateway1);
		assertThat(dfa.getPropertyValue("serverConnectionFactory")).isSameAs(cfS2);
		assertThat(dfa.getPropertyValue("replyTimeout")).isEqualTo(456L);
		assertThat(tcpInboundGateway1.getComponentName()).isEqualTo("inGateway1");
		assertThat(tcpInboundGateway1.getComponentType()).isEqualTo("ip:tcp-inbound-gateway");
		assertThat(tcpInboundGateway1.getErrorChannel()).isEqualTo(errorChannel);
		assertThat(cfS2.isLookupHost()).isTrue();
		assertThat(tcpInboundGateway1.isAutoStartup()).isFalse();
		assertThat(tcpInboundGateway1.getPhase()).isEqualTo(126);
		assertThat((Boolean) TestUtils.getPropertyValue(
				TestUtils.getPropertyValue(cfS2, "mapper"), "applySequence")).isFalse();
		assertThat(TestUtils.getPropertyValue(cfS2, "readDelay")).isEqualTo(100L);
	}

	@Test
	public void testInGateway2() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(tcpInboundGateway2);
		assertThat(dfa.getPropertyValue("serverConnectionFactory")).isSameAs(cfS3);
		assertThat(dfa.getPropertyValue("replyTimeout")).isEqualTo(456L);
		assertThat(tcpInboundGateway2.getComponentName()).isEqualTo("inGateway2");
		assertThat(tcpInboundGateway2.getComponentType()).isEqualTo("ip:tcp-inbound-gateway");
		assertThat(dfa.getPropertyValue("errorChannel")).isNull();
		assertThat(dfa.getPropertyValue("isClientMode")).isEqualTo(Boolean.FALSE);
		assertThat(dfa.getPropertyValue("taskScheduler")).isNull();
		assertThat(dfa.getPropertyValue("retryInterval")).isEqualTo(60000L);
	}

	@Test
	public void testOutGateway() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(tcpOutboundGateway);
		assertThat(dfa.getPropertyValue("connectionFactory")).isSameAs(cfC2);
		assertThat(dfa.getPropertyValue("requestTimeout")).isEqualTo(234L);
		MessagingTemplate messagingTemplate = TestUtils.getPropertyValue(tcpOutboundGateway, "messagingTemplate",
				MessagingTemplate.class);
		assertThat(TestUtils.getPropertyValue(messagingTemplate, "sendTimeout", Long.class))
				.isEqualTo(Long.valueOf(567));
		assertThat(TestUtils.getPropertyValue(tcpOutboundGateway, "remoteTimeoutExpression.literalValue"))
				.isEqualTo("789");
		assertThat(tcpOutboundGateway.getComponentName()).isEqualTo("outGateway");
		assertThat(tcpOutboundGateway.getComponentType()).isEqualTo("ip:tcp-outbound-gateway");
		assertThat(cfC2.isLookupHost()).isTrue();
		assertThat(dfa.getPropertyValue("order")).isEqualTo(24);
		assertThat(dfa.getPropertyValue("async")).isEqualTo(Boolean.TRUE);

		assertThat(TestUtils.getPropertyValue(outAdviceGateway, "remoteTimeoutExpression.expression"))
				.isEqualTo("4000");
		assertThat(TestUtils.getPropertyValue(outAdviceGateway, "closeStreamAfterSend")).isEqualTo(Boolean.TRUE);
		assertThat(TestUtils.getPropertyValue(outAdviceGateway, "async")).isEqualTo(Boolean.FALSE);
	}

	@Test
	public void testConnClient1() {
		assertThat(client1 instanceof TcpNioClientConnectionFactory).isTrue();
		assertThat(client1.getHost()).isEqualTo("localhost");
		assertThat(client1.getSoLinger()).isEqualTo(54);
		assertThat(client1.getSoReceiveBufferSize()).isEqualTo(1234);
		assertThat(client1.getSoSendBufferSize()).isEqualTo(1235);
		assertThat(client1.getSoTimeout()).isEqualTo(1236);
		assertThat(client1.getSoTrafficClass()).isEqualTo(12);
		DirectFieldAccessor dfa = new DirectFieldAccessor(client1);
		assertThat(dfa.getPropertyValue("serializer")).isSameAs(serializer);
		assertThat(dfa.getPropertyValue("deserializer")).isSameAs(deserializer);
		assertThat(dfa.getPropertyValue("soTcpNoDelay")).isEqualTo(true);
		assertThat(dfa.getPropertyValue("singleUse")).isEqualTo(true);
		assertThat(dfa.getPropertyValue("taskExecutor")).isSameAs(taskExecutor);
		assertThat(dfa.getPropertyValue("usingDirectBuffers")).isEqualTo(true);
		assertThat(dfa.getPropertyValue("interceptorFactoryChain")).isNotNull();
	}

	@Test
	public void testConnServer1() {
		assertThat(server1 instanceof TcpNioServerConnectionFactory).isTrue();
		assertThat(server1.getSoLinger()).isEqualTo(55);
		assertThat(server1.getSoReceiveBufferSize()).isEqualTo(1234);
		assertThat(server1.getSoSendBufferSize()).isEqualTo(1235);
		assertThat(server1.getSoTimeout()).isEqualTo(1236);
		assertThat(server1.getSoTrafficClass()).isEqualTo(12);
		DirectFieldAccessor dfa = new DirectFieldAccessor(server1);
		assertThat(dfa.getPropertyValue("serializer")).isSameAs(serializer);
		assertThat(dfa.getPropertyValue("deserializer")).isSameAs(deserializer);
		assertThat(dfa.getPropertyValue("soTcpNoDelay")).isEqualTo(true);
		assertThat(dfa.getPropertyValue("singleUse")).isEqualTo(true);
		assertThat(dfa.getPropertyValue("taskExecutor")).isSameAs(taskExecutor);
		assertThat(dfa.getPropertyValue("backlog")).isEqualTo(123);
		assertThat(dfa.getPropertyValue("usingDirectBuffers")).isEqualTo(true);
		assertThat(dfa.getPropertyValue("interceptorFactoryChain")).isNotNull();
	}

	@Test
	public void testConnClient2() {
		assertThat(client2 instanceof TcpNetClientConnectionFactory).isTrue();
		assertThat(client1.getHost()).isEqualTo("localhost");
		assertThat(client1.getSoLinger()).isEqualTo(54);
		assertThat(client1.getSoReceiveBufferSize()).isEqualTo(1234);
		assertThat(client1.getSoSendBufferSize()).isEqualTo(1235);
		assertThat(client1.getSoTimeout()).isEqualTo(1236);
		assertThat(client1.getSoTrafficClass()).isEqualTo(12);
		DirectFieldAccessor dfa = new DirectFieldAccessor(client1);
		assertThat(dfa.getPropertyValue("serializer")).isSameAs(serializer);
		assertThat(dfa.getPropertyValue("deserializer")).isSameAs(deserializer);
		assertThat(dfa.getPropertyValue("soTcpNoDelay")).isEqualTo(true);
		assertThat(dfa.getPropertyValue("singleUse")).isEqualTo(true);
		assertThat(dfa.getPropertyValue("taskExecutor")).isSameAs(taskExecutor);
		assertThat(dfa.getPropertyValue("interceptorFactoryChain")).isNotNull();
	}

	@Test
	public void testConnServer2() {
		assertThat(server2 instanceof TcpNetServerConnectionFactory).isTrue();
		assertThat(server1.getSoLinger()).isEqualTo(55);
		assertThat(server1.getSoReceiveBufferSize()).isEqualTo(1234);
		assertThat(server1.getSoSendBufferSize()).isEqualTo(1235);
		assertThat(server1.getSoTimeout()).isEqualTo(1236);
		assertThat(server1.getSoTrafficClass()).isEqualTo(12);
		DirectFieldAccessor dfa = new DirectFieldAccessor(server1);
		assertThat(dfa.getPropertyValue("serializer")).isSameAs(serializer);
		assertThat(dfa.getPropertyValue("deserializer")).isSameAs(deserializer);
		assertThat(dfa.getPropertyValue("soTcpNoDelay")).isEqualTo(true);
		assertThat(dfa.getPropertyValue("singleUse")).isEqualTo(true);
		assertThat(dfa.getPropertyValue("taskExecutor")).isSameAs(taskExecutor);
		assertThat(dfa.getPropertyValue("backlog")).isEqualTo(123);
		assertThat(dfa.getPropertyValue("interceptorFactoryChain")).isNotNull();
	}

	@Test
	public void testNewOut1() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(tcpNewOut1);
		assertThat(dfa.getPropertyValue("clientConnectionFactory")).isSameAs(client1);
		assertThat(dfa.getPropertyValue("order")).isEqualTo(25);
		assertThat(dfa.getPropertyValue("isClientMode")).isEqualTo(Boolean.FALSE);
		assertThat(dfa.getPropertyValue("taskScheduler")).isNull();
		assertThat(dfa.getPropertyValue("retryInterval")).isEqualTo(60000L);
	}

	@Test
	public void testNewOut2() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(tcpNewOut2);
		assertThat(dfa.getPropertyValue("serverConnectionFactory")).isSameAs(server1);
		assertThat(dfa.getPropertyValue("order")).isEqualTo(15);
	}

	@Test
	public void testNewIn1() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(tcpNewIn1);
		assertThat(dfa.getPropertyValue("clientConnectionFactory")).isSameAs(client1);
		assertThat(dfa.getPropertyValue("errorChannel")).isNull();
		assertThat(dfa.getPropertyValue("isClientMode")).isEqualTo(Boolean.FALSE);
		assertThat(dfa.getPropertyValue("taskScheduler")).isNull();
		assertThat(dfa.getPropertyValue("retryInterval")).isEqualTo(60000L);
	}

	@Test
	public void testNewIn2() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(tcpNewIn2);
		assertThat(dfa.getPropertyValue("serverConnectionFactory")).isSameAs(server1);
	}

	@Test
	public void testtCPOrder() {
		this.outGateway.start();
		this.testOutTcpNio.start();
		@SuppressWarnings("unchecked")
		Set<MessageHandler> handlers = (Set<MessageHandler>) TestUtils
				.getPropertyValue(
						TestUtils.getPropertyValue(this.tcpChannel, "dispatcher"),
						"handlers");
		Iterator<MessageHandler> iterator = handlers.iterator();
		assertThat(iterator.next()).isSameAs(this.tcpNewOut2);			//15
		assertThat(iterator.next()).isSameAs(this.tcpOutboundGateway);	//24
		assertThat(iterator.next()).isSameAs(this.tcpNewOut1);			//25
		assertThat(iterator.next()).isSameAs(this.tcpOut);				//35
	}

	@Test
	public void testInClientMode() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(tcpInClientMode);
		assertThat(dfa.getPropertyValue("clientConnectionFactory")).isSameAs(cfC3);
		assertThat(dfa.getPropertyValue("serverConnectionFactory")).isNull();
		assertThat(dfa.getPropertyValue("isClientMode")).isEqualTo(Boolean.TRUE);
		assertThat(dfa.getPropertyValue("taskScheduler")).isSameAs(sched);
		assertThat(dfa.getPropertyValue("retryInterval")).isEqualTo(123000L);
	}

	@Test
	public void testOutClientMode() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(tcpOutClientMode);
		assertThat(dfa.getPropertyValue("clientConnectionFactory")).isSameAs(cfC4);
		assertThat(dfa.getPropertyValue("serverConnectionFactory")).isNull();
		assertThat(dfa.getPropertyValue("isClientMode")).isEqualTo(Boolean.TRUE);
		assertThat(dfa.getPropertyValue("taskScheduler")).isSameAs(sched);
		assertThat(dfa.getPropertyValue("retryInterval")).isEqualTo(124000L);
	}

	@Test
	public void testInGatewayClientMode() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(inGatewayClientMode);
		assertThat(dfa.getPropertyValue("clientConnectionFactory")).isSameAs(cfC5);
		assertThat(dfa.getPropertyValue("serverConnectionFactory")).isNull();
		assertThat(dfa.getPropertyValue("isClientMode")).isEqualTo(Boolean.TRUE);
		assertThat(dfa.getPropertyValue("taskScheduler")).isSameAs(sched);
		assertThat(dfa.getPropertyValue("retryInterval")).isEqualTo(125000L);
	}

	@Test
	public void testAutoTcp() {
		assertThat(TestUtils.getPropertyValue(tcpAutoAdapter, "outputChannel")).isSameAs(tcpAutoChannel);
	}

	@Test
	public void testAutoUdp() {
		assertThat(TestUtils.getPropertyValue(udpAutoAdapter, "outputChannel")).isSameAs(udpAutoChannel);
	}

	@Test
	public void testSecureServer() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(secureServer);
		assertThat(dfa.getPropertyValue("tcpSocketFactorySupport")).isSameAs(socketFactorySupport);
		assertThat(dfa.getPropertyValue("tcpSocketSupport")).isSameAs(socketSupport);
		assertThat(TestUtils.getPropertyValue(this.secureServerNio, "sslHandshakeTimeout")).isEqualTo(34);
		assertThat(dfa.getPropertyValue("tcpNetConnectionSupport")).isSameAs(this.netConnectionSupport);
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
			adviceCalled.countDown();
			return null;
		}

	}

	@SuppressWarnings("serial")
	public static class EventSubclass1 extends TcpConnectionEvent {

		public EventSubclass1(TcpConnectionSupport connection, String connectionFactoryName) {
			super(connection, connectionFactoryName);
		}
	}

	@SuppressWarnings("serial")
	public static class EventSubclass2 extends TcpConnectionEvent {

		public EventSubclass2(TcpConnectionSupport connection, String connectionFactoryName) {
			super(connection, connectionFactoryName);
		}
	}

	@Configuration
	@ImportResource("org/springframework/integration/ip/config/ParserTests-context.xml")
	public static class Config {

		@Bean
		AbstractClientConnectionFactory mockClientCf() {
			AbstractClientConnectionFactory mock = mock(AbstractClientConnectionFactory.class);
			given(mock.isSingleUse()).willReturn(true);
			return mock;
		}

	}

	public static class SocketCust implements SocketCustomizer {

		@Override
		public void configure(DatagramSocket socket) throws SocketException {
		}

	}

}
