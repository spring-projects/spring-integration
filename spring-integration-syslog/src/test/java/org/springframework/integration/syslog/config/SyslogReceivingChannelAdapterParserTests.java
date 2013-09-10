/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.integration.syslog.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;

import javax.net.SocketFactory;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.syslog.MessageConverter;
import org.springframework.integration.syslog.inbound.TcpSyslogReceivingChannelAdapter;
import org.springframework.integration.syslog.inbound.UdpSyslogReceivingChannelAdapter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class SyslogReceivingChannelAdapterParserTests {

	@Autowired @Qualifier("foo.adapter")
	private UdpSyslogReceivingChannelAdapter adapter1;

	@Autowired
	private UdpSyslogReceivingChannelAdapter foobar;

	@Autowired
	private PollableChannel foo;

	@Autowired @Qualifier("explicitUdp.adapter")
	private UdpSyslogReceivingChannelAdapter explicitUdpAdapter;

	@Autowired
	private PollableChannel explicitUdp;

	@Autowired
	private PollableChannel errors;

	@Autowired
	private UdpSyslogReceivingChannelAdapter fullBoatUdp;

	@Autowired
	private PassThruConverter converter;

	@Autowired @Qualifier("bar.adapter")
	private TcpSyslogReceivingChannelAdapter adapter2;

	@Autowired
	private PollableChannel bar;

	@Autowired
	private TcpSyslogReceivingChannelAdapter fullBoatTcp;

	@Autowired
	private AbstractServerConnectionFactory cf;

	@Test
	public void testSimplestUdp() throws Exception {
		int port = TestUtils.getPropertyValue(adapter1, "udpAdapter.port", Integer.class);
		byte[] buf = "<157>JUL 26 22:08:35 WEBERN TESTING[70729]: TEST SYSLOG MESSAGE".getBytes("UTF-8");
		DatagramPacket packet = new DatagramPacket(buf, buf.length, new InetSocketAddress("localhost", port));
		DatagramSocket socket = new DatagramSocket();
		Thread.sleep(1000);
		socket.send(packet);
		socket.close();
		Message<?> message = foo.receive(10000);
		assertNotNull(message);
		adapter1.stop();
	}

	@Test
	public void testExplicitChannelUdp() throws Exception {
		assertEquals(1514, TestUtils.getPropertyValue(foobar, "udpAdapter.port"));
		assertSame(foo, TestUtils.getPropertyValue(foobar, "outputChannel"));
	}

	@Test
	public void testExplicitUdp() throws Exception {
		assertSame(explicitUdp, TestUtils.getPropertyValue(explicitUdpAdapter, "outputChannel"));
	}

	@Test
	public void testFullBoatUdp() {
		assertSame(foo, TestUtils.getPropertyValue(fullBoatUdp, "outputChannel"));
		assertFalse(fullBoatUdp.isAutoStartup());
		assertEquals(123, fullBoatUdp.getPhase());
		assertEquals(456L, TestUtils.getPropertyValue(fullBoatUdp, "messagingTemplate.sendTimeout"));
		assertSame(converter, TestUtils.getPropertyValue(fullBoatUdp, "converter"));
		assertSame(errors, TestUtils.getPropertyValue(fullBoatUdp, "errorChannel"));
		assertFalse(TestUtils.getPropertyValue(fullBoatUdp, "udpAdapter.mapper.lookupHost", Boolean.class));
	}

	@Test
	public void testSimplestTcp() throws Exception {
		AbstractServerConnectionFactory connectionFactory = TestUtils.getPropertyValue(adapter2, "connectionFactory",
				AbstractServerConnectionFactory.class);
		int port = connectionFactory.getPort();
		waitListening(connectionFactory, 10000L);
		byte[] buf = "<157>JUL 26 22:08:35 WEBERN TESTING[70729]: TEST SYSLOG MESSAGE\n".getBytes("UTF-8");
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		Thread.sleep(1000);
		socket.getOutputStream().write(buf);
		socket.close();
		Message<?> message = bar.receive(10000);
		assertNotNull(message);
		adapter2.stop();
		assertNotNull(TestUtils.getPropertyValue(adapter2, "connectionFactory.applicationEventPublisher"));
	}

	@Test
	public void testFullBoatTcp() {
		assertSame(bar, TestUtils.getPropertyValue(fullBoatTcp, "outputChannel"));
		assertFalse(fullBoatTcp.isAutoStartup());
		assertEquals(123, fullBoatTcp.getPhase());
		assertEquals(456L, TestUtils.getPropertyValue(fullBoatUdp, "messagingTemplate.sendTimeout"));
		assertSame(converter, TestUtils.getPropertyValue(fullBoatTcp, "converter"));
		assertSame(errors, TestUtils.getPropertyValue(fullBoatTcp, "errorChannel"));
		assertSame(cf, TestUtils.getPropertyValue(fullBoatTcp, "connectionFactory"));
	}

	@Test
	public void testPortOnUdpChild() {
		try {
			new ClassPathXmlApplicationContext(this.getClass().getSimpleName() + "-fail1-context.xml", this.getClass());
			fail("Expected exception");
		}
		catch (BeanDefinitionParsingException e) {
			assertTrue(e.getMessage().startsWith(
					"Configuration problem: When child element 'udp-attributes' is present, 'port' must be defined there"));
		}
	}

	@Test
	public void testPortWithTCPFactory() {
		try {
			new ClassPathXmlApplicationContext(this.getClass().getSimpleName() + "-fail2-context.xml", this.getClass());
			fail("Expected exception");
		}
		catch (BeanCreationException e) {
			assertEquals("Cannot specify both 'port' and 'connectionFactory'", e.getCause().getMessage());
		}
	}

	@Test
	public void testUdpChildWithTcp() {
		try {
			new ClassPathXmlApplicationContext(this.getClass().getSimpleName() + "-fail3-context.xml", this.getClass());
			fail("Expected exception");
		}
		catch (BeanCreationException e) {
			e.printStackTrace();

			assertEquals("Cannot specifiy 'udp-attributes' when the protocol is 'tcp'", e.getCause().getMessage());
		}
	}

	@Test
	public void testUDPWithTCPFactory() {
		try {
			new ClassPathXmlApplicationContext(this.getClass().getSimpleName() + "-fail4-context.xml", this.getClass());
			fail("Expected exception");
		}
		catch (BeanCreationException e) {
			assertEquals("Cannot specifiy 'connection-factory' unless the protocol is 'tcp'", e.getCause().getMessage());
		}
	}

	public static class PassThruConverter implements MessageConverter {

		@Override
		public Message<?> fromSyslog(Message<?> syslog) throws Exception {
			return syslog;
		}

	}

	/**
	 * Wait for a server connection factory to actually start listening before
	 * starting a test. Waits for up to 10 seconds by default.
	 * @param serverConnectionFactory The server connection factory.
	 * @param delay How long to wait in milliseconds; default 10000 (10 seconds) if null.
	 * @throws IllegalStateException
	 */
	private void waitListening(AbstractServerConnectionFactory serverConnectionFactory, Long delay)
		throws IllegalStateException {
		if (delay == null) {
			delay = 100L;
		}
		else {
			delay = delay / 100;
		}
		int n = 0;
		while (!serverConnectionFactory.isListening()) {
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException e1) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(e1);
			}

			if (n++ > delay) {
				throw new IllegalStateException("Server didn't start listening.");
			}
		}
	}

}
