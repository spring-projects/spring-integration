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
import org.springframework.integration.ip.tcp.TcpNioReceivingChannelAdapter;
import org.springframework.integration.ip.tcp.Utils;
import org.springframework.integration.ip.udp.UnicastReceivingChannelAdapter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


/**
 * @author Gary Russell
 *
 */
@ContextConfiguration(locations="inboundAdapters.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class IpChannelAdapterParserTests {

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
	
	@SuppressWarnings("unchecked")
	@Test
	public void testTcpInbound1() {
		Utils.testSendFragmented(tcp1.getPort());
		Message<byte[]> message = (Message<byte[]>) channel.receive();
		assertNotNull(message);
		assertEquals("xx", new String(message.getPayload()));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testTcpInbound2() {
		Utils.testSendFragmented(tcp2.getPort());
		Message<byte[]> message = (Message<byte[]>) channel.receive();
		assertNotNull(message);
		assertEquals("xx", new String(message.getPayload()));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testTcpInbound3() {
		Utils.testSendFragmented(tcp3.getPort());
		Message<byte[]> message = (Message<byte[]>) channel.receive();
		assertNotNull(message);
		assertEquals("xx", new String(message.getPayload()));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testTcpInbound4() {
		Utils.testSendStxEtx(tcp4.getPort(), null);
		Message<byte[]> message = (Message<byte[]>) channel.receive();
		assertNotNull(message);
		assertEquals(Utils.TEST_STRING + Utils.TEST_STRING, new String(message.getPayload()));
		message = (Message<byte[]>) channel.receive();
		assertNotNull(message);
		assertEquals(Utils.TEST_STRING + Utils.TEST_STRING, new String(message.getPayload()));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testTcpInbound5() {
		Utils.testSendCrLf(tcp5.getPort(), null);
		Message<byte[]> message = (Message<byte[]>) channel.receive();
		assertNotNull(message);
		assertEquals(Utils.TEST_STRING + Utils.TEST_STRING, new String(message.getPayload()));
		message = (Message<byte[]>) channel.receive();
		assertNotNull(message);
		assertEquals(Utils.TEST_STRING + Utils.TEST_STRING, new String(message.getPayload()));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testTcpInbound6() {
		Utils.testSendStxEtx(tcp6.getPort(), null);
		Message<byte[]> message = (Message<byte[]>) channel.receive();
		assertNotNull(message);
		assertEquals("\u0002" + Utils.TEST_STRING + Utils.TEST_STRING + "\u0003", 
				new String(message.getPayload()));
		message = (Message<byte[]>) channel.receive();
		assertNotNull(message);
		assertEquals("\u0002" + Utils.TEST_STRING + Utils.TEST_STRING + "\u0003", 
				new String(message.getPayload()));
	}

	@Test 
	public void testUdpInbound1() {
		assertNotNull(udp1);
	}
}
