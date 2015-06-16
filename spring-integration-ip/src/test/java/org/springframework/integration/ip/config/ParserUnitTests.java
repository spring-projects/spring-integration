/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.integration.ip.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Iterator;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.ip.tcp.TcpInboundGateway;
import org.springframework.integration.ip.tcp.TcpOutboundGateway;
import org.springframework.integration.ip.tcp.TcpReceivingChannelAdapter;
import org.springframework.integration.ip.tcp.TcpSendingMessageHandler;
import org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory;
import org.springframework.integration.ip.tcp.connection.DefaultTcpNetSSLSocketFactorySupport;
import org.springframework.integration.ip.tcp.connection.DefaultTcpNioSSLConnectionSupport;
import org.springframework.integration.ip.tcp.connection.DefaultTcpSSLContextSupport;
import org.springframework.integration.ip.tcp.connection.TcpConnectionEvent;
import org.springframework.integration.ip.tcp.connection.TcpConnectionEventListeningMessageProducer;
import org.springframework.integration.ip.tcp.connection.TcpConnectionOpenEvent;
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
import org.springframework.integration.ip.udp.UnicastReceivingChannelAdapter;
import org.springframework.integration.ip.udp.UnicastSendingMessageHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @author Oleg Zhurakousky
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ParserUnitTests {

	@Autowired
	ApplicationContext ctx;

	@Autowired
	@Qualifier(value="testInUdp")
	UnicastReceivingChannelAdapter udpIn;

	@Autowired
	@Qualifier(value="testInUdpMulticast")
	MulticastReceivingChannelAdapter udpInMulticast;

	@Autowired
	@Qualifier(value="testInTcp")
	TcpReceivingChannelAdapter tcpIn;

	@Autowired
	@Qualifier(value="testOutUdp.handler")
	UnicastSendingMessageHandler udpOut;

	@Autowired
	@Qualifier(value="testOutUdpiMulticast.handler")
	MulticastSendingMessageHandler udpOutMulticast;

	@Autowired
	@Qualifier(value="testOutTcpNio")
	AbstractEndpoint tcpOutEndpoint;

	@Autowired
	@Qualifier(value="testOutTcpNio.handler")
	TcpSendingMessageHandler tcpOut;

	@Autowired
	EventDrivenConsumer testOutTcpNio;

	@Autowired
	@Qualifier(value="inGateway1")
	TcpInboundGateway tcpInboundGateway1;

	@Autowired
	@Qualifier(value="inGateway2")
	TcpInboundGateway tcpInboundGateway2;

	@Autowired
	@Qualifier(value="outGateway.handler")
	TcpOutboundGateway tcpOutboundGateway;

	@Autowired
	@Qualifier(value="outAdviceGateway.handler")
	TcpOutboundGateway outAdviceGateway;

	// verify we can still inject by generated name
	@Autowired
	@Qualifier(value="org.springframework.integration.ip.tcp.TcpOutboundGateway#0")
	TcpOutboundGateway tcpOutboundGatewayByGeneratedName;

	@Autowired
	EventDrivenConsumer outGateway;

	@Autowired
	@Qualifier(value="externalTE")
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
	@Qualifier(value="tcpNewOut1.handler")
	TcpSendingMessageHandler tcpNewOut1;

	@Autowired
	@Qualifier(value="tcpNewOut2.handler")
	TcpSendingMessageHandler tcpNewOut2;

	@Autowired
	TcpReceivingChannelAdapter tcpNewIn1;

	@Autowired
	TcpReceivingChannelAdapter tcpNewIn2;

	@Autowired
	private MessageChannel errorChannel;

	@Autowired
	private DirectChannel udpChannel;

	@Autowired
	private DirectChannel udpAdviceChannel;

	@Autowired
	private DirectChannel tcpAdviceChannel;

	@Autowired
	private DirectChannel tcpAdviceGateChannel;

	@Autowired
	private DirectChannel tcpChannel;

	@Autowired
	TcpReceivingChannelAdapter tcpInClientMode;

	@Autowired
	TcpInboundGateway inGatewayClientMode;

	@Autowired
	TaskScheduler sched;

	@Autowired
	@Qualifier(value="tcpOutClientMode.handler")
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
	TcpSocketFactorySupport socketFactorySupport;

	@Autowired
	TcpSocketSupport socketSupport;

	@Autowired
	TcpSSLContextSupport contextSupport;

	@Autowired
	TcpMessageMapper mapper;

	@Autowired
	TcpConnectionEventListeningMessageProducer eventAdapter;

	@Autowired
	QueueChannel eventChannel;

	private static volatile int adviceCalled;

	@Test
	public void testInUdp() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(udpIn);
		assertTrue(udpIn.getPort() >= 5000);
		assertEquals(27, dfa.getPropertyValue("poolSize"));
		assertEquals(29, dfa.getPropertyValue("receiveBufferSize"));
		assertEquals(30, dfa.getPropertyValue("soReceiveBufferSize"));
		assertEquals(31, dfa.getPropertyValue("soSendBufferSize"));
		assertEquals(32, dfa.getPropertyValue("soTimeout"));
		assertEquals("testInUdp",udpIn.getComponentName());
		assertEquals("ip:udp-inbound-channel-adapter", udpIn.getComponentType());
		assertEquals("127.0.0.1", dfa.getPropertyValue("localAddress"));
		assertSame(taskExecutor, dfa.getPropertyValue("taskExecutor"));
		assertEquals(errorChannel, dfa.getPropertyValue("errorChannel"));
		DatagramPacketMessageMapper mapper = (DatagramPacketMessageMapper) dfa.getPropertyValue("mapper");
		DirectFieldAccessor mapperAccessor = new DirectFieldAccessor(mapper);
		assertFalse((Boolean)mapperAccessor.getPropertyValue("lookupHost"));
		assertFalse(TestUtils.getPropertyValue(udpIn, "autoStartup", Boolean.class));
		assertEquals(1234, dfa.getPropertyValue("phase"));
	}

	@Test
	public void testInUdpMulticast() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(udpInMulticast);
		assertTrue(udpInMulticast.getPort() >= 5100);
		assertEquals("225.6.7.8", dfa.getPropertyValue("group"));
		assertEquals(27, dfa.getPropertyValue("poolSize"));
		assertEquals(29, dfa.getPropertyValue("receiveBufferSize"));
		assertEquals(30, dfa.getPropertyValue("soReceiveBufferSize"));
		assertEquals(31, dfa.getPropertyValue("soSendBufferSize"));
		assertEquals(32, dfa.getPropertyValue("soTimeout"));
		assertEquals("127.0.0.1", dfa.getPropertyValue("localAddress"));
		assertNotSame(taskExecutor, dfa.getPropertyValue("taskExecutor"));
		assertNull(dfa.getPropertyValue("errorChannel"));
		DatagramPacketMessageMapper mapper = (DatagramPacketMessageMapper) dfa.getPropertyValue("mapper");
		DirectFieldAccessor mapperAccessor = new DirectFieldAccessor(mapper);
		assertTrue((Boolean)mapperAccessor.getPropertyValue("lookupHost"));
	}

	@Test
	public void testInTcp() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(tcpIn);
		assertSame(cfS1, dfa.getPropertyValue("serverConnectionFactory"));
		assertEquals("testInTcp",tcpIn.getComponentName());
		assertEquals("ip:tcp-inbound-channel-adapter", tcpIn.getComponentType());
		assertEquals(errorChannel, dfa.getPropertyValue("errorChannel"));
		assertFalse(cfS1.isLookupHost());
		assertFalse(tcpIn.isAutoStartup());
		assertEquals(124, tcpIn.getPhase());
		TcpMessageMapper cfS1Mapper = TestUtils.getPropertyValue(cfS1, "mapper", TcpMessageMapper.class);
		assertSame(mapper, cfS1Mapper);
		assertTrue((Boolean) TestUtils.getPropertyValue(cfS1Mapper, "applySequence"));
		Object socketSupport = TestUtils.getPropertyValue(cfS1, "tcpSocketFactorySupport");
		assertTrue(socketSupport instanceof DefaultTcpNetSSLSocketFactorySupport);
		assertNotNull(TestUtils.getPropertyValue(socketSupport, "sslContext"));

		TcpSSLContextSupport contextSupport = TestUtils.getPropertyValue(cfS1, "tcpSocketFactorySupport.sslContextSupport", TcpSSLContextSupport.class);
		assertSame(contextSupport, this.contextSupport);
		assertTrue(TestUtils.getPropertyValue(contextSupport, "keyStore") instanceof ClassPathResource);
		assertTrue(TestUtils.getPropertyValue(contextSupport, "trustStore") instanceof ClassPathResource);

		contextSupport = new DefaultTcpSSLContextSupport("http:foo", "file:bar", "", "");
		assertTrue(TestUtils.getPropertyValue(contextSupport, "keyStore") instanceof UrlResource);
		assertTrue(TestUtils.getPropertyValue(contextSupport, "trustStore") instanceof UrlResource);
	}

	@Test
	public void testInTcpNioSSLDefaultConfig() {
		assertFalse(cfS1Nio.isLookupHost());
		assertTrue((Boolean) TestUtils.getPropertyValue(cfS1Nio, "mapper.applySequence"));
		Object connectionSupport = TestUtils.getPropertyValue(cfS1Nio, "tcpNioConnectionSupport");
		assertTrue(connectionSupport instanceof DefaultTcpNioSSLConnectionSupport);
		assertNotNull(TestUtils.getPropertyValue(connectionSupport, "sslContext"));
	}

	@Test
	public void testOutUdp() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(udpOut);
		assertTrue(udpOut.getPort() >= 5400);
		assertEquals("localhost", dfa.getPropertyValue("host"));
		int ackPort = (Integer) dfa.getPropertyValue("ackPort");
		assertTrue("Expected ackPort >= 5300 was:" + ackPort, ackPort >= 5300);
		DatagramPacketMessageMapper mapper = (DatagramPacketMessageMapper) dfa
				.getPropertyValue("mapper");
		String ackAddress = (String) new DirectFieldAccessor(mapper)
				.getPropertyValue("ackAddress");
		assertEquals("somehost:" + ackPort, ackAddress);
		assertEquals(51, dfa.getPropertyValue("ackTimeout"));
		assertEquals(true, dfa.getPropertyValue("waitForAck"));
		assertEquals(52, dfa.getPropertyValue("soReceiveBufferSize"));
		assertEquals(53, dfa.getPropertyValue("soSendBufferSize"));
		assertEquals(54, dfa.getPropertyValue("soTimeout"));
		assertEquals("127.0.0.1", dfa.getPropertyValue("localAddress"));
		assertSame(taskExecutor, dfa.getPropertyValue("taskExecutor"));
		assertEquals(23, dfa.getPropertyValue("order"));
		assertEquals("testOutUdp",udpOut.getComponentName());
		assertEquals("ip:udp-outbound-channel-adapter", udpOut.getComponentType());
	}

	@Test
	public void testOutUdpMulticast() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(udpOutMulticast);
		assertTrue(udpOutMulticast.getPort() >= 5600);
		assertEquals("225.6.7.8", dfa.getPropertyValue("host"));
		int ackPort = (Integer) dfa.getPropertyValue("ackPort");
		assertTrue("Expected ackPort >= 5500 was:" + ackPort, ackPort >= 5500);
		DatagramPacketMessageMapper mapper = (DatagramPacketMessageMapper) dfa
				.getPropertyValue("mapper");
		String ackAddress = (String) new DirectFieldAccessor(mapper)
				.getPropertyValue("ackAddress");
		assertEquals("somehost:" + ackPort, ackAddress);
		assertEquals(51, dfa.getPropertyValue("ackTimeout"));
		assertEquals(true, dfa.getPropertyValue("waitForAck"));
		assertEquals(52, dfa.getPropertyValue("soReceiveBufferSize"));
		assertEquals(53, dfa.getPropertyValue("soSendBufferSize"));
		assertEquals(54, dfa.getPropertyValue("soTimeout"));
		assertEquals(55, dfa.getPropertyValue("timeToLive"));
		assertEquals(12, dfa.getPropertyValue("order"));
	}

	@Test
	public void testUdpOrder() {
		@SuppressWarnings("unchecked")
		Set<MessageHandler> handlers = (Set<MessageHandler>) TestUtils
				.getPropertyValue(
						TestUtils.getPropertyValue(this.udpChannel, "dispatcher"),
						"handlers");
		Iterator<MessageHandler> iterator = handlers.iterator();
		assertSame(this.udpOutMulticast, iterator.next());
		assertSame(this.udpOut, iterator.next());
	}

	@Test
	public void udpAdvice() {
		adviceCalled = 0;
		this.udpAdviceChannel.send(new GenericMessage<String>("foo"));
		assertEquals(1, adviceCalled);
	}

	@Test
	public void tcpAdvice() {
		adviceCalled = 0;
		this.tcpAdviceChannel.send(new GenericMessage<String>("foo"));
		assertEquals(1, adviceCalled);
	}

	@Test
	public void tcpGatewayAdvice() {
		adviceCalled = 0;
		this.tcpAdviceGateChannel.send(new GenericMessage<String>("foo"));
		assertEquals(1, adviceCalled);
	}

	@Test
	public void testOutTcp() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(tcpOut);
		assertSame(cfC1, dfa.getPropertyValue("clientConnectionFactory"));
		assertEquals("testOutTcpNio",tcpOut.getComponentName());
		assertEquals("ip:tcp-outbound-channel-adapter", tcpOut.getComponentType());
		assertFalse(cfC1.isLookupHost());
		assertEquals(35, dfa.getPropertyValue("order"));
		assertFalse(tcpOutEndpoint.isAutoStartup());
		assertEquals(125, tcpOutEndpoint.getPhase());
		assertFalse((Boolean) TestUtils.getPropertyValue(
				TestUtils.getPropertyValue(cfC1, "mapper"), "applySequence"));
		assertEquals(10000L, TestUtils.getPropertyValue(cfC1, "readDelay"));
	}

	@Test
	public void testInGateway1() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(tcpInboundGateway1);
		assertSame(cfS2, dfa.getPropertyValue("serverConnectionFactory"));
		assertEquals(456L, dfa.getPropertyValue("replyTimeout"));
		assertEquals("inGateway1",tcpInboundGateway1.getComponentName());
		assertEquals("ip:tcp-inbound-gateway", tcpInboundGateway1.getComponentType());
		assertEquals(errorChannel, dfa.getPropertyValue("errorChannel"));
		assertTrue(cfS2.isLookupHost());
		assertFalse(tcpInboundGateway1.isAutoStartup());
		assertEquals(126, tcpInboundGateway1.getPhase());
		assertFalse((Boolean) TestUtils.getPropertyValue(
				TestUtils.getPropertyValue(cfS2, "mapper"), "applySequence"));
		assertEquals(100L, TestUtils.getPropertyValue(cfS2, "readDelay"));
	}

	@Test
	public void testInGateway2() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(tcpInboundGateway2);
		assertSame(cfS3, dfa.getPropertyValue("serverConnectionFactory"));
		assertEquals(456L, dfa.getPropertyValue("replyTimeout"));
		assertEquals("inGateway2",tcpInboundGateway2.getComponentName());
		assertEquals("ip:tcp-inbound-gateway", tcpInboundGateway2.getComponentType());
		assertNull(dfa.getPropertyValue("errorChannel"));
		assertEquals(Boolean.FALSE, dfa.getPropertyValue("isClientMode"));
		assertNull(dfa.getPropertyValue("taskScheduler"));
		assertEquals(60000L, dfa.getPropertyValue("retryInterval"));
	}

	@Test
	public void testOutGateway() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(tcpOutboundGateway);
		assertSame(cfC2, dfa.getPropertyValue("connectionFactory"));
		assertEquals(234L, dfa.getPropertyValue("requestTimeout"));
		MessagingTemplate messagingTemplate = TestUtils.getPropertyValue(tcpOutboundGateway, "messagingTemplate",
				MessagingTemplate.class);
		assertEquals(Long.valueOf(567), TestUtils.getPropertyValue(messagingTemplate, "sendTimeout", Long.class));
		assertEquals("789", TestUtils.getPropertyValue(tcpOutboundGateway, "remoteTimeoutExpression.literalValue"));
		assertEquals("outGateway",tcpOutboundGateway.getComponentName());
		assertEquals("ip:tcp-outbound-gateway", tcpOutboundGateway.getComponentType());
		assertTrue(cfC2.isLookupHost());
		assertEquals(24, dfa.getPropertyValue("order"));

		assertEquals("4000", TestUtils.getPropertyValue(outAdviceGateway, "remoteTimeoutExpression.expression"));
	}

	@Test
	public void testConnClient1() {
		assertTrue(client1 instanceof TcpNioClientConnectionFactory);
		assertEquals("localhost", client1.getHost());
		assertTrue(client1.getPort() >= 6000);
		assertEquals(54, client1.getSoLinger());
		assertEquals(1234, client1.getSoReceiveBufferSize());
		assertEquals(1235, client1.getSoSendBufferSize());
		assertEquals(1236, client1.getSoTimeout());
		assertEquals(12, client1.getSoTrafficClass());
		DirectFieldAccessor dfa = new DirectFieldAccessor(client1);
		assertSame(serializer, dfa.getPropertyValue("serializer"));
		assertSame(deserializer, dfa.getPropertyValue("deserializer"));
		assertEquals(true, dfa.getPropertyValue("soTcpNoDelay"));
		assertEquals(true, dfa.getPropertyValue("singleUse"));
		assertSame(taskExecutor, dfa.getPropertyValue("taskExecutor"));
		assertEquals(true, dfa.getPropertyValue("usingDirectBuffers"));
		assertNotNull(dfa.getPropertyValue("interceptorFactoryChain"));
	}

	@Test
	public void testConnServer1() {
		assertTrue(server1 instanceof TcpNioServerConnectionFactory);
		assertEquals(client1.getPort(), server1.getPort());
		assertEquals(55, server1.getSoLinger());
		assertEquals(1234, server1.getSoReceiveBufferSize());
		assertEquals(1235, server1.getSoSendBufferSize());
		assertEquals(1236, server1.getSoTimeout());
		assertEquals(12, server1.getSoTrafficClass());
		DirectFieldAccessor dfa = new DirectFieldAccessor(server1);
		assertSame(serializer, dfa.getPropertyValue("serializer"));
		assertSame(deserializer, dfa.getPropertyValue("deserializer"));
		assertEquals(true, dfa.getPropertyValue("soTcpNoDelay"));
		assertEquals(true, dfa.getPropertyValue("singleUse"));
		assertSame(taskExecutor, dfa.getPropertyValue("taskExecutor"));
		assertEquals(123, dfa.getPropertyValue("backlog"));
		assertEquals(true, dfa.getPropertyValue("usingDirectBuffers"));
		assertNotNull(dfa.getPropertyValue("interceptorFactoryChain"));
	}

	@Test
	public void testConnClient2() {
		assertTrue(client2 instanceof TcpNetClientConnectionFactory);
		assertEquals("localhost", client1.getHost());
		assertTrue(client1.getPort() >= 6000);
		assertEquals(54, client1.getSoLinger());
		assertEquals(1234, client1.getSoReceiveBufferSize());
		assertEquals(1235, client1.getSoSendBufferSize());
		assertEquals(1236, client1.getSoTimeout());
		assertEquals(12, client1.getSoTrafficClass());
		DirectFieldAccessor dfa = new DirectFieldAccessor(client1);
		assertSame(serializer, dfa.getPropertyValue("serializer"));
		assertSame(deserializer, dfa.getPropertyValue("deserializer"));
		assertEquals(true, dfa.getPropertyValue("soTcpNoDelay"));
		assertEquals(true, dfa.getPropertyValue("singleUse"));
		assertSame(taskExecutor, dfa.getPropertyValue("taskExecutor"));
		assertNotNull(dfa.getPropertyValue("interceptorFactoryChain"));
	}

	@Test
	public void testConnServer2() {
		assertTrue(server2 instanceof TcpNetServerConnectionFactory);
		assertEquals(client1.getPort(), server1.getPort());
		assertEquals(55, server1.getSoLinger());
		assertEquals(1234, server1.getSoReceiveBufferSize());
		assertEquals(1235, server1.getSoSendBufferSize());
		assertEquals(1236, server1.getSoTimeout());
		assertEquals(12, server1.getSoTrafficClass());
		DirectFieldAccessor dfa = new DirectFieldAccessor(server1);
		assertSame(serializer, dfa.getPropertyValue("serializer"));
		assertSame(deserializer, dfa.getPropertyValue("deserializer"));
		assertEquals(true, dfa.getPropertyValue("soTcpNoDelay"));
		assertEquals(true, dfa.getPropertyValue("singleUse"));
		assertSame(taskExecutor, dfa.getPropertyValue("taskExecutor"));
		assertEquals(123, dfa.getPropertyValue("backlog"));
		assertNotNull(dfa.getPropertyValue("interceptorFactoryChain"));
	}

	@Test
	public void testNewOut1() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(tcpNewOut1);
		assertSame(client1, dfa.getPropertyValue("clientConnectionFactory"));
		assertEquals(25, dfa.getPropertyValue("order"));
		assertEquals(Boolean.FALSE, dfa.getPropertyValue("isClientMode"));
		assertNull(dfa.getPropertyValue("taskScheduler"));
		assertEquals(60000L, dfa.getPropertyValue("retryInterval"));
	}

	@Test
	public void testNewOut2() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(tcpNewOut2);
		assertSame(server1, dfa.getPropertyValue("serverConnectionFactory"));
		assertEquals(15, dfa.getPropertyValue("order"));
	}

	@Test
	public void testNewIn1() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(tcpNewIn1);
		assertSame(client1, dfa.getPropertyValue("clientConnectionFactory"));
		assertNull(dfa.getPropertyValue("errorChannel"));
		assertEquals(Boolean.FALSE, dfa.getPropertyValue("isClientMode"));
		assertNull(dfa.getPropertyValue("taskScheduler"));
		assertEquals(60000L, dfa.getPropertyValue("retryInterval"));
	}

	@Test
	public void testNewIn2() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(tcpNewIn2);
		assertSame(server1, dfa.getPropertyValue("serverConnectionFactory"));
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
		assertSame(this.tcpNewOut2, iterator.next());			//15
		assertSame(this.tcpOutboundGateway, iterator.next());	//24
		assertSame(this.tcpNewOut1, iterator.next());			//25
		assertSame(this.tcpOut, iterator.next());				//35
	}

	@Test
	public void testInClientMode() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(tcpInClientMode);
		assertSame(cfC3, dfa.getPropertyValue("clientConnectionFactory"));
		assertNull(dfa.getPropertyValue("serverConnectionFactory"));
		assertEquals(Boolean.TRUE, dfa.getPropertyValue("isClientMode"));
		assertSame(sched, dfa.getPropertyValue("taskScheduler"));
		assertEquals(123000L, dfa.getPropertyValue("retryInterval"));
	}

	@Test
	public void testOutClientMode() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(tcpOutClientMode);
		assertSame(cfC4, dfa.getPropertyValue("clientConnectionFactory"));
		assertNull(dfa.getPropertyValue("serverConnectionFactory"));
		assertEquals(Boolean.TRUE, dfa.getPropertyValue("isClientMode"));
		assertSame(sched, dfa.getPropertyValue("taskScheduler"));
		assertEquals(124000L, dfa.getPropertyValue("retryInterval"));
	}

	@Test
	public void testInGatewayClientMode() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(inGatewayClientMode);
		assertSame(cfC5, dfa.getPropertyValue("clientConnectionFactory"));
		assertNull(dfa.getPropertyValue("serverConnectionFactory"));
		assertEquals(Boolean.TRUE, dfa.getPropertyValue("isClientMode"));
		assertSame(sched, dfa.getPropertyValue("taskScheduler"));
		assertEquals(125000L, dfa.getPropertyValue("retryInterval"));
	}

	@Test
	public void testAutoTcp() {
		assertSame(tcpAutoChannel, TestUtils.getPropertyValue(tcpAutoAdapter, "outputChannel"));
	}

	@Test
	public void testAutoUdp() {
		assertSame(udpAutoChannel, TestUtils.getPropertyValue(udpAutoAdapter, "outputChannel"));
	}

	@Test
	public void testSecureServer() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(secureServer);
		assertSame(socketFactorySupport, dfa.getPropertyValue("tcpSocketFactorySupport"));
		assertSame(socketSupport, dfa.getPropertyValue("tcpSocketSupport"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testEventAdapter() {
		Set<?> eventTypes = TestUtils.getPropertyValue(this.eventAdapter, "eventTypes", Set.class);
		assertEquals(2, eventTypes.size());
		assertTrue(eventTypes.contains(EventSubclass1.class));
		assertTrue(eventTypes.contains(EventSubclass2.class));
		assertFalse(TestUtils.getPropertyValue(this.eventAdapter, "autoStartup", Boolean.class));
		assertEquals(23, TestUtils.getPropertyValue(this.eventAdapter, "phase"));
		assertEquals("eventErrors", TestUtils.getPropertyValue(this.eventAdapter, "errorChannel",
				DirectChannel.class).getComponentName());

		TcpConnectionSupport connection = mock(TcpConnectionSupport.class);
		TcpConnectionEvent event = new TcpConnectionOpenEvent(connection, "foo");
		Class<TcpConnectionEvent>[] types = (Class<TcpConnectionEvent>[]) new Class<?>[]{TcpConnectionEvent.class};
		this.eventAdapter.setEventTypes(types);
		this.eventAdapter.onApplicationEvent(event);
		assertNull(this.eventChannel.receive(0));
		this.eventAdapter.start();
		this.eventAdapter.onApplicationEvent(event);
		Message<?> eventMessage = this.eventChannel.receive(0);
		assertNotNull(eventMessage);
		assertSame(event, eventMessage.getPayload());
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
			adviceCalled++;
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
}
