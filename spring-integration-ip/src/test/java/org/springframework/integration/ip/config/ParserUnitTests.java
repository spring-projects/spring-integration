/*
 * Copyright 2002-2018 the original author or authors.
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

import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
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
import org.springframework.integration.ip.udp.UnicastReceivingChannelAdapter;
import org.springframework.integration.ip.udp.UnicastSendingMessageHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 *
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
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

	private static CountDownLatch adviceCalled = new CountDownLatch(1);

	@Test
	public void testInUdp() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(udpIn);
		assertEquals(27, dfa.getPropertyValue("poolSize"));
		assertEquals(29, dfa.getPropertyValue("receiveBufferSize"));
		assertEquals(30, dfa.getPropertyValue("soReceiveBufferSize"));
		assertEquals(31, dfa.getPropertyValue("soSendBufferSize"));
		assertEquals(32, dfa.getPropertyValue("soTimeout"));
		assertEquals("testInUdp", udpIn.getComponentName());
		assertEquals("ip:udp-inbound-channel-adapter", udpIn.getComponentType());
		assertEquals("127.0.0.1", dfa.getPropertyValue("localAddress"));
		assertSame(taskExecutor, dfa.getPropertyValue("taskExecutor"));
		assertEquals(errorChannel, dfa.getPropertyValue("errorChannel"));
		DatagramPacketMessageMapper mapper = (DatagramPacketMessageMapper) dfa.getPropertyValue("mapper");
		DirectFieldAccessor mapperAccessor = new DirectFieldAccessor(mapper);
		assertFalse((Boolean) mapperAccessor.getPropertyValue("lookupHost"));
		assertFalse(TestUtils.getPropertyValue(udpIn, "autoStartup", Boolean.class));
		assertEquals(1234, dfa.getPropertyValue("phase"));
	}

	@Test
	public void testInUdpMulticast() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(udpInMulticast);
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
		assertTrue((Boolean) mapperAccessor.getPropertyValue("lookupHost"));
	}

	@Test
	public void testInTcp() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(tcpIn);
		assertSame(cfS1, dfa.getPropertyValue("serverConnectionFactory"));
		assertEquals("testInTcp", tcpIn.getComponentName());
		assertEquals("ip:tcp-inbound-channel-adapter", tcpIn.getComponentType());
		assertEquals(errorChannel, dfa.getPropertyValue("errorChannel"));
		assertFalse(cfS1.isLookupHost());
		assertFalse(tcpIn.isAutoStartup());
		assertEquals(124, tcpIn.getPhase());
		TcpMessageMapper cfS1Mapper = TestUtils.getPropertyValue(cfS1, "mapper", TcpMessageMapper.class);
		assertSame(mapper, cfS1Mapper);
		assertTrue(TestUtils.getPropertyValue(cfS1Mapper, "applySequence", Boolean.class));
		Object socketSupport = TestUtils.getPropertyValue(cfS1, "tcpSocketFactorySupport");
		assertTrue(socketSupport instanceof DefaultTcpNetSSLSocketFactorySupport);
		assertNotNull(TestUtils.getPropertyValue(socketSupport, "sslContext"));

		TcpSSLContextSupport tcpSSLContextSupport = new DefaultTcpSSLContextSupport("http:foo", "file:bar", "", "");
		assertTrue(TestUtils.getPropertyValue(tcpSSLContextSupport, "keyStore") instanceof UrlResource);
		assertTrue(TestUtils.getPropertyValue(tcpSSLContextSupport, "trustStore") instanceof UrlResource);
	}

	@Test
	public void testInTcpNioSSLDefaultConfig() {
		assertFalse(cfS1Nio.isLookupHost());
		assertTrue(TestUtils.getPropertyValue(cfS1Nio, "mapper.applySequence", Boolean.class));
		Object connectionSupport = TestUtils.getPropertyValue(cfS1Nio, "tcpNioConnectionSupport");
		assertTrue(connectionSupport instanceof DefaultTcpNioSSLConnectionSupport);
		assertNotNull(TestUtils.getPropertyValue(connectionSupport, "sslContext"));
		assertEquals(43, TestUtils.getPropertyValue(this.cfS1Nio, "sslHandshakeTimeout"));
		assertSame(this.ctx.getBean(DefaultTcpNioSSLConnectionSupport.class),
				TestUtils.getPropertyValue(this.cfS1Nio, "tcpNioConnectionSupport"));
	}

	@Test
	public void testOutUdp() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(udpOut);
		assertEquals("localhost", dfa.getPropertyValue("host"));
		DatagramPacketMessageMapper mapper = (DatagramPacketMessageMapper) dfa
				.getPropertyValue("mapper");
		String ackAddress = (String) new DirectFieldAccessor(mapper)
				.getPropertyValue("ackAddress");
		assertThat(ackAddress, startsWith("somehost:"));
		assertEquals(51, dfa.getPropertyValue("ackTimeout"));
		assertEquals(true, dfa.getPropertyValue("waitForAck"));
		assertEquals(52, dfa.getPropertyValue("soReceiveBufferSize"));
		assertEquals(53, dfa.getPropertyValue("soSendBufferSize"));
		assertEquals(54, dfa.getPropertyValue("soTimeout"));
		assertEquals("127.0.0.1", dfa.getPropertyValue("localAddress"));
		assertSame(taskExecutor, dfa.getPropertyValue("taskExecutor"));
		assertEquals(23, dfa.getPropertyValue("order"));
		assertEquals("testOutUdp", udpOut.getComponentName());
		assertEquals("ip:udp-outbound-channel-adapter", udpOut.getComponentType());
	}

	@Test
	public void testOutUdpMulticast() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(udpOutMulticast);
		assertEquals("225.6.7.8", dfa.getPropertyValue("host"));
		DatagramPacketMessageMapper mapper = (DatagramPacketMessageMapper) dfa
				.getPropertyValue("mapper");
		String ackAddress = (String) new DirectFieldAccessor(mapper)
				.getPropertyValue("ackAddress");
		assertThat(ackAddress, startsWith("somehost:"));
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
	public void udpAdvice() throws InterruptedException {
		adviceCalled = new CountDownLatch(1);
		this.udpAdviceChannel.send(new GenericMessage<String>("foo"));
		assertTrue(adviceCalled.await(10, TimeUnit.SECONDS));
	}

	@Test
	public void tcpAdvice() throws InterruptedException {
		adviceCalled = new CountDownLatch(1);
		this.tcpAdviceChannel.send(new GenericMessage<String>("foo"));
		assertTrue(adviceCalled.await(10, TimeUnit.SECONDS));
	}

	@Test
	public void tcpGatewayAdvice() throws InterruptedException {
		adviceCalled = new CountDownLatch(1);
		this.tcpAdviceGateChannel.send(new GenericMessage<String>("foo"));
		assertTrue(adviceCalled.await(10, TimeUnit.SECONDS));
	}

	@Test
	public void testOutTcp() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(tcpOut);
		assertSame(cfC1, dfa.getPropertyValue("clientConnectionFactory"));
		assertEquals("testOutTcpNio", tcpOut.getComponentName());
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
		assertEquals("inGateway1", tcpInboundGateway1.getComponentName());
		assertEquals("ip:tcp-inbound-gateway", tcpInboundGateway1.getComponentType());
		assertEquals(errorChannel, tcpInboundGateway1.getErrorChannel());
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
		assertEquals("inGateway2", tcpInboundGateway2.getComponentName());
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
		assertEquals("outGateway", tcpOutboundGateway.getComponentName());
		assertEquals("ip:tcp-outbound-gateway", tcpOutboundGateway.getComponentType());
		assertTrue(cfC2.isLookupHost());
		assertEquals(24, dfa.getPropertyValue("order"));

		assertEquals("4000", TestUtils.getPropertyValue(outAdviceGateway, "remoteTimeoutExpression.expression"));
	}

	@Test
	public void testConnClient1() {
		assertTrue(client1 instanceof TcpNioClientConnectionFactory);
		assertEquals("localhost", client1.getHost());
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
		assertEquals(34, TestUtils.getPropertyValue(this.secureServerNio, "sslHandshakeTimeout"));
		assertSame(this.netConnectionSupport, dfa.getPropertyValue("tcpNetConnectionSupport"));
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
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
}
