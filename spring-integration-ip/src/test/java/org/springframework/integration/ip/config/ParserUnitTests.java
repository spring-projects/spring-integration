/*
 * Copyright 2002-2010 the original author or authors.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.commons.serializer.InputStreamingConverter;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.ip.tcp.CustomNetSocketReader;
import org.springframework.integration.ip.tcp.CustomNetSocketWriter;
import org.springframework.integration.ip.tcp.CustomNioSocketReader;
import org.springframework.integration.ip.tcp.CustomNioSocketWriter;
import org.springframework.integration.ip.tcp.MessageFormats;
import org.springframework.integration.ip.tcp.SimpleTcpNetInboundGateway;
import org.springframework.integration.ip.tcp.SimpleTcpNetOutboundGateway;
import org.springframework.integration.ip.tcp.TcpNetReceivingChannelAdapter;
import org.springframework.integration.ip.tcp.TcpNetSendingMessageHandler;
import org.springframework.integration.ip.tcp.TcpNioReceivingChannelAdapter;
import org.springframework.integration.ip.tcp.TcpNioSendingMessageHandler;
import org.springframework.integration.ip.tcp.TcpReceivingChannelAdapter;
import org.springframework.integration.ip.tcp.TcpSendingMessageHandler;
import org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNioClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNioServerConnectionFactory;
import org.springframework.integration.ip.udp.DatagramPacketMessageMapper;
import org.springframework.integration.ip.udp.MulticastReceivingChannelAdapter;
import org.springframework.integration.ip.udp.MulticastSendingMessageHandler;
import org.springframework.integration.ip.udp.UnicastReceivingChannelAdapter;
import org.springframework.integration.ip.udp.UnicastSendingMessageHandler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


/**
 * @author Gary Russell
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
	@Qualifier(value="testInTcpNio")
	TcpNioReceivingChannelAdapter tcpInNio;
	
	@Autowired
	@Qualifier(value="testInTcpNioDirect")
	TcpNioReceivingChannelAdapter tcpInNioDirect;
	
	@Autowired
	@Qualifier(value="testInTcpNet")
	TcpNetReceivingChannelAdapter tcpInNet;
	
	@Autowired
	@Qualifier(value="testInTcpNetSerialized")
	TcpNetReceivingChannelAdapter tcpInNetSerialized;
	
	@Autowired
	@Qualifier(value="org.springframework.integration.ip.udp.UnicastSendingMessageHandler#0")
	UnicastSendingMessageHandler udpOut;

	@Autowired
	@Qualifier(value="org.springframework.integration.ip.udp.MulticastSendingMessageHandler#0")
	MulticastSendingMessageHandler udpOutMulticast;

	@Autowired
	@Qualifier(value="org.springframework.integration.ip.tcp.TcpNioSendingMessageHandler#0")
	TcpNioSendingMessageHandler tcpOutNio;

	@Autowired
	@Qualifier(value="org.springframework.integration.ip.tcp.TcpNioSendingMessageHandler#1")
	TcpNioSendingMessageHandler tcpOutNioDirect;

	@Autowired
	@Qualifier(value="org.springframework.integration.ip.tcp.TcpNetSendingMessageHandler#0")
	TcpNetSendingMessageHandler tcpOutNet;

	@Autowired
	@Qualifier(value="org.springframework.integration.ip.tcp.TcpNetSendingMessageHandler#1")
	TcpNetSendingMessageHandler tcpOutNetSerialized;

	@Autowired
	@Qualifier(value="simpleInGateway")
	SimpleTcpNetInboundGateway simpleTcpNetInboundGateway;

	@Autowired
	@Qualifier(value="org.springframework.integration.ip.tcp.SimpleTcpNetOutboundGateway#0")
	SimpleTcpNetOutboundGateway simpleTcpNetOutboundGateway;
	
	@Autowired
	@Qualifier(value="simpleInGatewayClose")
	SimpleTcpNetInboundGateway simpleTcpNetInboundGatewayClose;

	@Autowired
	@Qualifier(value="org.springframework.integration.ip.tcp.SimpleTcpNetOutboundGateway#1")
	SimpleTcpNetOutboundGateway simpleTcpNetOutboundGatewayClose;

	@Autowired
	@Qualifier(value="externalTE")
	TaskExecutor taskExecutor;
	
	@Autowired
	@Qualifier(value="client1")
	AbstractConnectionFactory client1;

	@Autowired
	InputStreamingConverter<byte[]> converter;
	
	@Autowired
	@Qualifier(value="server1")
	AbstractConnectionFactory server1;

	@Autowired
	@Qualifier(value="org.springframework.integration.ip.tcp.TcpSendingMessageHandler#0")
	TcpSendingMessageHandler tcpNewOut1;

	@Autowired
	@Qualifier(value="org.springframework.integration.ip.tcp.TcpSendingMessageHandler#1")
	TcpSendingMessageHandler tcpNewOut2;

	@Autowired
	@Qualifier(value="tcpNewIn1")
	TcpReceivingChannelAdapter tcpNewIn1;

	@Autowired
	@Qualifier(value="tcpNewIn2")
	TcpReceivingChannelAdapter tcpNewIn2;

	@Test
	public void testInUdp() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(udpIn);
		assertTrue(udpIn.getPort() >= 5000);
		assertEquals(27, dfa.getPropertyValue("poolSize"));
		assertEquals(29, dfa.getPropertyValue("receiveBufferSize"));
		assertEquals(30, dfa.getPropertyValue("soReceiveBufferSize"));
		assertEquals(31, dfa.getPropertyValue("soSendBufferSize"));
		assertEquals(32, dfa.getPropertyValue("soTimeout"));
		assertEquals("127.0.0.1", dfa.getPropertyValue("localAddress"));
		assertSame(taskExecutor, dfa.getPropertyValue("taskExecutor"));
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
	}
	
	@Test
	public void testInTcpNio() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(tcpInNio);
		assertTrue(tcpInNio.getPort() >= 5200);
		assertEquals(CustomNioSocketReader.class, dfa.getPropertyValue("customSocketReaderClass"));
		assertEquals(false, dfa.getPropertyValue("usingDirectBuffers"));
		assertEquals(MessageFormats.FORMAT_STX_ETX, dfa.getPropertyValue("messageFormat"));
		assertEquals(27, dfa.getPropertyValue("poolSize"));
		assertEquals(true, dfa.getPropertyValue("soKeepAlive"));
		assertEquals(29, dfa.getPropertyValue("receiveBufferSize"));
		assertEquals(30, dfa.getPropertyValue("soReceiveBufferSize"));
		assertEquals(32, dfa.getPropertyValue("soTimeout"));
		assertEquals(false, dfa.getPropertyValue("close"));
		assertEquals("127.0.0.1", dfa.getPropertyValue("localAddress"));
	}
	
	@Test
	public void testInTcpNioDirect() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(tcpInNioDirect);
		assertTrue(tcpInNioDirect.getPort() >= 5300);
		assertEquals(CustomNioSocketReader.class, dfa.getPropertyValue("customSocketReaderClass"));
		assertEquals(true, dfa.getPropertyValue("usingDirectBuffers"));
		assertEquals(MessageFormats.FORMAT_STX_ETX, dfa.getPropertyValue("messageFormat"));
		assertEquals(27, dfa.getPropertyValue("poolSize"));
		assertEquals(true, dfa.getPropertyValue("soKeepAlive"));
		assertEquals(29, dfa.getPropertyValue("receiveBufferSize"));
		assertEquals(30, dfa.getPropertyValue("soReceiveBufferSize"));
		assertEquals(32, dfa.getPropertyValue("soTimeout"));
		assertEquals(true, dfa.getPropertyValue("close"));
	}

	@Test
	public void testInTcpNet() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(tcpInNet);
		assertTrue(tcpInNet.getPort() >= 5400);
		assertEquals(CustomNetSocketReader.class, dfa.getPropertyValue("customSocketReaderClass"));
		assertEquals(MessageFormats.FORMAT_STX_ETX, dfa.getPropertyValue("messageFormat"));
		assertEquals(27, dfa.getPropertyValue("poolSize"));
		assertEquals(true, dfa.getPropertyValue("soKeepAlive"));
		assertEquals(29, dfa.getPropertyValue("receiveBufferSize"));
		assertEquals(30, dfa.getPropertyValue("soReceiveBufferSize"));
		assertEquals(32, dfa.getPropertyValue("soTimeout"));
		assertEquals(false, dfa.getPropertyValue("close"));
		assertEquals("127.0.0.1", dfa.getPropertyValue("localAddress"));
	}
	
	@Test
	public void testInTcpNetSerialized() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(tcpInNetSerialized);
		assertTrue(tcpInNetSerialized.getPort() >= 5450);
		assertEquals(MessageFormats.FORMAT_JAVA_SERIALIZED, dfa.getPropertyValue("messageFormat"));
		assertEquals(27, dfa.getPropertyValue("poolSize"));
		assertEquals(true, dfa.getPropertyValue("soKeepAlive"));
		assertEquals(29, dfa.getPropertyValue("receiveBufferSize"));
		assertEquals(30, dfa.getPropertyValue("soReceiveBufferSize"));
		assertEquals(32, dfa.getPropertyValue("soTimeout"));
		assertEquals(false, dfa.getPropertyValue("close"));
	}
	
	@Test
	public void testOutUdp() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(udpOut);
		assertTrue(udpOut.getPort() >= 6000);
		assertEquals("localhost", dfa.getPropertyValue("host"));
		int ackPort = (Integer) dfa.getPropertyValue("ackPort");
		assertTrue("Expected ackPort >= 7000 was:" + ackPort, ackPort >= 7000);
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
	}
	
	@Test
	public void testOutUdpMulticast() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(udpOutMulticast);
		assertTrue(udpOutMulticast.getPort() >= 6100);
		assertEquals("225.6.7.8", dfa.getPropertyValue("host"));
		int ackPort = (Integer) dfa.getPropertyValue("ackPort");
		assertTrue("Expected ackPort >= 7100 was:" + ackPort, ackPort >= 7100);
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
	}

	@Test
	public void testOutTcpNio() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(tcpOutNio);
		assertTrue(tcpOutNio.getPort() >= 6200);
		assertEquals(MessageFormats.FORMAT_STX_ETX, dfa.getPropertyValue("messageFormat"));
		assertEquals(CustomNioSocketWriter.class, dfa.getPropertyValue("customSocketWriterClass"));
		assertEquals(true, dfa.getPropertyValue("soKeepAlive"));
		assertEquals(3, dfa.getPropertyValue("soLinger"));
		assertEquals(true, dfa.getPropertyValue("soTcpNoDelay"));
		assertEquals(27, dfa.getPropertyValue("soTrafficClass"));
		assertEquals(53, dfa.getPropertyValue("soSendBufferSize"));
		assertEquals(54, dfa.getPropertyValue("soTimeout"));
		assertEquals(false, dfa.getPropertyValue("usingDirectBuffers"));
	}

	@Test
	public void testOutTcpNioDirect() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(tcpOutNioDirect);
		assertTrue(tcpOutNioDirect.getPort() >= 6300);
		assertEquals(MessageFormats.FORMAT_STX_ETX, dfa.getPropertyValue("messageFormat"));
		assertEquals(CustomNioSocketWriter.class, dfa.getPropertyValue("customSocketWriterClass"));
		assertEquals(true, dfa.getPropertyValue("soKeepAlive"));
		assertEquals(3, dfa.getPropertyValue("soLinger"));
		assertEquals(true, dfa.getPropertyValue("soTcpNoDelay"));
		assertEquals(27, dfa.getPropertyValue("soTrafficClass"));
		assertEquals(53, dfa.getPropertyValue("soSendBufferSize"));
		assertEquals(54, dfa.getPropertyValue("soTimeout"));
		assertEquals(true, dfa.getPropertyValue("usingDirectBuffers"));
	}

	@Test
	public void testOutTcpNet() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(tcpOutNet);
		assertTrue(tcpOutNet.getPort() >= 6400);
		assertEquals(MessageFormats.FORMAT_STX_ETX, dfa.getPropertyValue("messageFormat"));
		assertEquals(CustomNetSocketWriter.class, dfa.getPropertyValue("customSocketWriterClass"));
		assertEquals(true, dfa.getPropertyValue("soKeepAlive"));
		assertEquals(3, dfa.getPropertyValue("soLinger"));
		assertEquals(true, dfa.getPropertyValue("soTcpNoDelay"));
		assertEquals(27, dfa.getPropertyValue("soTrafficClass"));
		assertEquals(53, dfa.getPropertyValue("soSendBufferSize"));
		assertEquals(54, dfa.getPropertyValue("soTimeout"));
	}
	
	@Test
	public void testOutTcpNetSerialized() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(tcpOutNetSerialized);
		assertTrue(tcpOutNetSerialized.getPort() >= 6450);
		assertEquals(MessageFormats.FORMAT_JAVA_SERIALIZED, dfa.getPropertyValue("messageFormat"));
		assertEquals(true, dfa.getPropertyValue("soKeepAlive"));
		assertEquals(3, dfa.getPropertyValue("soLinger"));
		assertEquals(true, dfa.getPropertyValue("soTcpNoDelay"));
		assertEquals(27, dfa.getPropertyValue("soTrafficClass"));
		assertEquals(53, dfa.getPropertyValue("soSendBufferSize"));
		assertEquals(54, dfa.getPropertyValue("soTimeout"));
	}
	
	@Test
	public void testInGateway() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(simpleTcpNetInboundGateway);
		assertTrue(simpleTcpNetInboundGateway.getPort() >= 6500);
		assertEquals(MessageFormats.FORMAT_CRLF, dfa.getPropertyValue("messageFormat"));
		TcpNetReceivingChannelAdapter delegate = (TcpNetReceivingChannelAdapter) dfa
				.getPropertyValue("delegate");
		DirectFieldAccessor delegateDfa = new DirectFieldAccessor(delegate);
		assertEquals(CustomNetSocketReader.class, delegateDfa.getPropertyValue("customSocketReaderClass"));
		assertEquals(CustomNetSocketWriter.class, dfa.getPropertyValue("customSocketWriterClass"));
		assertEquals(true, dfa.getPropertyValue("soKeepAlive"));
		assertEquals(123, dfa.getPropertyValue("receiveBufferSize"));
		assertEquals(124, dfa.getPropertyValue("soReceiveBufferSize"));
		assertEquals(125, dfa.getPropertyValue("soSendBufferSize"));
		assertEquals(126, dfa.getPropertyValue("soTimeout"));
		assertEquals(23, dfa.getPropertyValue("poolSize"));
		assertEquals(false, dfa.getPropertyValue("close"));
		assertEquals("127.0.0.1", dfa.getPropertyValue("localAddress"));
		assertSame(taskExecutor, dfa.getPropertyValue("taskExecutor"));
		assertSame(taskExecutor, delegateDfa.getPropertyValue("taskExecutor"));
	}

	@Test
	public void testOutGateway() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(simpleTcpNetOutboundGateway);
		assertTrue(simpleTcpNetOutboundGateway.getPort() >= 6600);
		assertEquals(MessageFormats.FORMAT_CRLF, dfa.getPropertyValue("messageFormat"));
		TcpNetSendingMessageHandler handler = (TcpNetSendingMessageHandler) dfa
				.getPropertyValue("handler");
		DirectFieldAccessor delegateDfa = new DirectFieldAccessor(handler);
		assertEquals(CustomNetSocketReader.class, dfa.getPropertyValue("customSocketReaderClass"));
		assertEquals(CustomNetSocketWriter.class, delegateDfa.getPropertyValue("customSocketWriterClass"));
		assertEquals(true, delegateDfa.getPropertyValue("soKeepAlive"));
		assertEquals(224, dfa.getPropertyValue("soReceiveBufferSize"));
		assertEquals(225, delegateDfa.getPropertyValue("soSendBufferSize"));
		assertEquals(226, delegateDfa.getPropertyValue("soTimeout"));
		assertEquals(false, dfa.getPropertyValue("close"));
	}

	@Test
	public void testInGatewayClose() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(simpleTcpNetInboundGatewayClose);
		assertTrue(simpleTcpNetInboundGatewayClose.getPort() >= 6700);
		assertEquals(MessageFormats.FORMAT_CRLF, dfa.getPropertyValue("messageFormat"));
		TcpNetReceivingChannelAdapter delegate = (TcpNetReceivingChannelAdapter) dfa
				.getPropertyValue("delegate");
		DirectFieldAccessor delegateDfa = new DirectFieldAccessor(delegate);
		assertEquals(CustomNetSocketReader.class, delegateDfa.getPropertyValue("customSocketReaderClass"));
		assertEquals(CustomNetSocketWriter.class, dfa.getPropertyValue("customSocketWriterClass"));
		assertEquals(true, dfa.getPropertyValue("soKeepAlive"));
		assertEquals(123, dfa.getPropertyValue("receiveBufferSize"));
		assertEquals(124, dfa.getPropertyValue("soReceiveBufferSize"));
		assertEquals(125, dfa.getPropertyValue("soSendBufferSize"));
		assertEquals(126, dfa.getPropertyValue("soTimeout"));
		assertEquals(23, dfa.getPropertyValue("poolSize"));
		assertEquals(true, dfa.getPropertyValue("close"));
	}

	@Test
	public void testOutGatewayClose() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(simpleTcpNetOutboundGatewayClose);
		assertTrue(simpleTcpNetOutboundGatewayClose.getPort() >= 6800);
		assertEquals(MessageFormats.FORMAT_CRLF, dfa.getPropertyValue("messageFormat"));
		TcpNetSendingMessageHandler handler = (TcpNetSendingMessageHandler) dfa
				.getPropertyValue("handler");
		DirectFieldAccessor delegateDfa = new DirectFieldAccessor(handler);
		assertEquals(CustomNetSocketReader.class, dfa.getPropertyValue("customSocketReaderClass"));
		assertEquals(CustomNetSocketWriter.class, delegateDfa.getPropertyValue("customSocketWriterClass"));
		assertEquals(true, delegateDfa.getPropertyValue("soKeepAlive"));
		assertEquals(224, dfa.getPropertyValue("soReceiveBufferSize"));
		assertEquals(225, delegateDfa.getPropertyValue("soSendBufferSize"));
		assertEquals(226, delegateDfa.getPropertyValue("soTimeout"));
		assertEquals(true, dfa.getPropertyValue("close"));
	}

	@Test
	public void testConnClient1() {
		assertTrue(client1 instanceof TcpNioClientConnectionFactory);
		assertEquals("localhost", client1.getHost());
		assertEquals(9876, client1.getPort());
		assertEquals(54, client1.getSoLinger());
		assertEquals(1234, client1.getSoReceiveBufferSize());
		assertEquals(1235, client1.getSoSendBufferSize());
		assertEquals(1236, client1.getSoTimeout());
		assertEquals(12, client1.getSoTrafficClass());
		DirectFieldAccessor dfa = new DirectFieldAccessor(client1);
		assertSame(converter, dfa.getPropertyValue("inputConverter"));
		assertSame(converter, dfa.getPropertyValue("outputConverter"));
		assertEquals(true, dfa.getPropertyValue("soTcpNoDelay"));
		assertEquals(true, dfa.getPropertyValue("singleUse"));
		assertSame(taskExecutor, dfa.getPropertyValue("taskExecutor"));
		assertEquals(321, dfa.getPropertyValue("poolSize"));
		assertEquals(true, dfa.getPropertyValue("usingDirectBuffers"));
		assertNotNull(dfa.getPropertyValue("interceptorFactoryChain"));
	}

	@Test
	public void testConnServer1() {
		assertTrue(server1 instanceof TcpNioServerConnectionFactory);
		assertEquals(9876, server1.getPort());
		assertEquals(55, server1.getSoLinger());
		assertEquals(1234, server1.getSoReceiveBufferSize());
		assertEquals(1235, server1.getSoSendBufferSize());
		assertEquals(1236, server1.getSoTimeout());
		assertEquals(12, server1.getSoTrafficClass());
		DirectFieldAccessor dfa = new DirectFieldAccessor(server1);
		assertSame(converter, dfa.getPropertyValue("inputConverter"));
		assertSame(converter, dfa.getPropertyValue("outputConverter"));
		assertEquals(true, dfa.getPropertyValue("soTcpNoDelay"));
		assertEquals(true, dfa.getPropertyValue("singleUse"));
		assertSame(taskExecutor, dfa.getPropertyValue("taskExecutor"));
		assertEquals(123, dfa.getPropertyValue("poolSize"));
		assertEquals(true, dfa.getPropertyValue("usingDirectBuffers"));
		assertNotNull(dfa.getPropertyValue("interceptorFactoryChain"));		
	}

	@Test
	public void testNewOut1() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(tcpNewOut1);
		assertSame(client1, dfa.getPropertyValue("clientConnectionFactory"));
	}
	
	@Test
	public void testNewOut2() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(tcpNewOut2);
		assertSame(server1, dfa.getPropertyValue("serverConnectionFactory"));
	}
	
	@Test
	public void testNewIn1() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(tcpNewIn1);
		assertSame(client1, dfa.getPropertyValue("clientConnectionFactory"));
	}
	
	@Test
	public void testNewIn2() {
		DirectFieldAccessor dfa = new DirectFieldAccessor(tcpNewIn2);
		assertSame(server1, dfa.getPropertyValue("serverConnectionFactory"));
	}
	
}
