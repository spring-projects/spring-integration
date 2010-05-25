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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.ip.tcp.TcpNetReceivingChannelAdapter;
import org.springframework.integration.ip.tcp.TcpNetSendingMessageHandler;
import org.springframework.integration.ip.tcp.TcpNioReceivingChannelAdapter;
import org.springframework.integration.ip.tcp.TcpNioSendingMessageHandler;
import org.springframework.integration.ip.udp.UnicastReceivingChannelAdapter;
import org.springframework.integration.ip.util.SocketUtils;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


/**
 * @author Gary Russell
 *
 */
@ContextConfiguration(locations={"inboundAdapters.xml"
								,"outboundAdapters.xml"
								})
@RunWith(SpringJUnit4ClassRunner.class)
public class IpChannelAdapterParserTests 
//				implements ApplicationContextAware 
										{

	@Autowired
	QueueChannel channel;
	
	@Autowired
	@Qualifier(value="tcp1")
	TcpNioReceivingChannelAdapter tcp1;
	
	@Autowired
	@Qualifier(value="tcp2")
	TcpNioReceivingChannelAdapter tcp2;
	
	@Autowired
	@Qualifier(value="tcp3")
	TcpNetReceivingChannelAdapter tcp3;
	
	@Autowired
	@Qualifier(value="tcp4")
	TcpNetReceivingChannelAdapter tcp4;

	@Autowired
	@Qualifier(value="tcp5")
	TcpNetReceivingChannelAdapter tcp5;

	@Autowired
	@Qualifier(value="tcp6")
	TcpNetReceivingChannelAdapter tcp6;

	@Autowired
	@Qualifier(value="udp1")
	UnicastReceivingChannelAdapter udp1;
	
	@Autowired
	@Qualifier(value="org.springframework.integration.ip.tcp.TcpNioSendingMessageHandler#0")
	TcpNioSendingMessageHandler tcpOut1;
	
	@Autowired
	@Qualifier(value="org.springframework.integration.ip.tcp.TcpNioSendingMessageHandler#1")
	TcpNioSendingMessageHandler tcpOut2;
	
	@Autowired
	@Qualifier(value="org.springframework.integration.ip.tcp.TcpNetSendingMessageHandler#0")
	TcpNetSendingMessageHandler tcpOut3;
	
	@Autowired
	@Qualifier(value="org.springframework.integration.ip.tcp.TcpNetSendingMessageHandler#1")
	TcpNetSendingMessageHandler tcpOut4;
	
	@Autowired
	@Qualifier(value="org.springframework.integration.ip.tcp.TcpNetSendingMessageHandler#2")
	TcpNetSendingMessageHandler tcpOut5;
	
	@Autowired
	@Qualifier(value="org.springframework.integration.ip.tcp.TcpNetSendingMessageHandler#3")
	TcpNetSendingMessageHandler tcpOut6;
	
	@SuppressWarnings("unchecked")
	@Test
	public void testTcpInbound1() {
		SocketUtils.testSendFragmented(tcp1.getPort(), true);
		Message<byte[]> message = (Message<byte[]>) channel.receive(10000);
		assertNotNull(message);
		assertEquals("xx", new String(message.getPayload()));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testTcpInbound2() {
		SocketUtils.testSendFragmented(tcp2.getPort(), true);
		Message<byte[]> message = (Message<byte[]>) channel.receive(10000);
		assertNotNull(message);
		assertEquals("xx", new String(message.getPayload()));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testTcpInbound3() {
		SocketUtils.testSendFragmented(tcp3.getPort(), true);
		Message<byte[]> message = (Message<byte[]>) channel.receive(10000);
		assertNotNull(message);
		assertEquals("xx", new String(message.getPayload()));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testTcpInbound4() {
		SocketUtils.testSendStxEtx(tcp4.getPort(), null);
		Message<byte[]> message = (Message<byte[]>) channel.receive(10000);
		assertNotNull(message);
		assertEquals(SocketUtils.TEST_STRING + SocketUtils.TEST_STRING, new String(message.getPayload()));
		message = (Message<byte[]>) channel.receive(10000);
		assertNotNull(message);
		assertEquals(SocketUtils.TEST_STRING + SocketUtils.TEST_STRING, new String(message.getPayload()));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testTcpInbound5() {
		SocketUtils.testSendCrLf(tcp5.getPort(), null);
		Message<byte[]> message = (Message<byte[]>) channel.receive(10000);
		assertNotNull(message);
		assertEquals(SocketUtils.TEST_STRING + SocketUtils.TEST_STRING, new String(message.getPayload()));
		message = (Message<byte[]>) channel.receive(10000);
		assertNotNull(message);
		assertEquals(SocketUtils.TEST_STRING + SocketUtils.TEST_STRING, new String(message.getPayload()));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testTcpInbound6() {
		SocketUtils.testSendStxEtx(tcp6.getPort(), null);
		Message<byte[]> message = (Message<byte[]>) channel.receive(10000);
		assertNotNull(message);
		assertEquals("\u0002" + SocketUtils.TEST_STRING + SocketUtils.TEST_STRING + "\u0003", 
				new String(message.getPayload()));
		message = (Message<byte[]>) channel.receive(10000);
		assertNotNull(message);
		assertEquals("\u0002" + SocketUtils.TEST_STRING + SocketUtils.TEST_STRING + "\u0003", 
				new String(message.getPayload()));
	}

	@Test 
	public void testUdpInbound1() {
		assertNotNull(udp1);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testTcpOutbound1() {
		Message<String> message = MessageBuilder.withPayload("TESTING").build();
		tcpOut1.handleMessage(message);
		Message<byte[]> mOut = (Message<byte[]>) channel.receive(10000);
		assertNotNull(mOut);
		assertEquals("TESTING", new String(mOut.getPayload()));

	}

	@SuppressWarnings("unchecked")
	@Test
	public void testTcpOutbound2() {
		Message<String> message = MessageBuilder.withPayload("TESTING").build();
		tcpOut2.handleMessage(message);
		Message<byte[]> mOut = (Message<byte[]>) channel.receive(10000);
		assertNotNull(mOut);
		assertEquals("TESTING", new String(mOut.getPayload()));

	}

	@SuppressWarnings("unchecked")
	@Test
	public void testTcpOutbound3() {
		Message<String> message = MessageBuilder.withPayload("TESTING").build();
		tcpOut3.handleMessage(message);
		Message<byte[]> mOut = (Message<byte[]>) channel.receive(10000);
		assertNotNull(mOut);
		assertEquals("TESTING", new String(mOut.getPayload()));

	}

	@SuppressWarnings("unchecked")
	@Test
	public void testTcpOutbound4() {
		Message<String> message = MessageBuilder.withPayload("TESTING").build();
		tcpOut4.handleMessage(message);
		Message<byte[]> mOut = (Message<byte[]>) channel.receive(10000);
		assertNotNull(mOut);
		assertEquals("TESTING", new String(mOut.getPayload()));

	}

	@SuppressWarnings("unchecked")
	@Test
	public void testTcpOutbound5() {
		Message<String> message = MessageBuilder.withPayload("TESTING").build();
		tcpOut5.handleMessage(message);
		Message<byte[]> mOut = (Message<byte[]>) channel.receive(10000);
		assertNotNull(mOut);
		assertEquals("TESTING", new String(mOut.getPayload()));

	}

	@SuppressWarnings("unchecked")
	@Test
	public void testTcpOutbound6() {
		Message<String> message = MessageBuilder.withPayload("TESTING").build();
		tcpOut6.handleMessage(message);
		Message<byte[]> mOut = (Message<byte[]>) channel.receive(10000);
		assertNotNull(mOut);
		// custom format pads to 24 bytes
		assertEquals("TESTING                 ", new String(mOut.getPayload()));

	}

	@SuppressWarnings("unchecked")
	@Test
	public void testTcpOutbound6a() {
		Message<String> message = MessageBuilder.withPayload(
				"abcdefghijklmnopqrdtuvwxyz").build();
		tcpOut6.handleMessage(message);
		Message<byte[]> mOut = (Message<byte[]>) channel.receive(10000);
		assertNotNull(mOut);
		// custom format truncates to 24 bytes
		assertEquals("abcdefghijklmnopqrdtuvwx", new String(mOut.getPayload()));

	}

//	/* (non-Javadoc)
//	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
//	 */
//	@Override
//	public void setApplicationContext(ApplicationContext applicationContext)
//			throws BeansException {
//		String[] names = 
//		applicationContext.getBeanNamesForType(TcpNetSendingMessageHandler.class);
//		for (String n : names) {
//			System.out.println(n);
//		}
//	}

}
