/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.syslog.config;

import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import javax.net.SocketFactory;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.syslog.MessageConverter;
import org.springframework.integration.syslog.RFC5424MessageConverter;
import org.springframework.integration.syslog.inbound.TcpSyslogReceivingChannelAdapter;
import org.springframework.integration.syslog.inbound.UdpSyslogReceivingChannelAdapter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 3.0
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class SyslogReceivingChannelAdapterParserTests {

	@Autowired
	@Qualifier("outputChannel.adapter")
	private UdpSyslogReceivingChannelAdapter adapter1;

	@Autowired
	private UdpSyslogReceivingChannelAdapter udpSyslogReceivingChannelAdapter;

	@Autowired
	private PollableChannel outputChannel;

	@Autowired
	@Qualifier("explicitUdp.adapter")
	private UdpSyslogReceivingChannelAdapter explicitUdpAdapter;

	@Autowired
	private PollableChannel explicitUdp;

	@Autowired
	private PollableChannel errors;

	@Autowired
	private UdpSyslogReceivingChannelAdapter fullBoatUdp;

	@Autowired
	private PassThruConverter converter;

	@Autowired
	private RFC5424MessageConverter rfc5424;

	@Autowired
	@Qualifier("outputChannelAlternate.adapter")
	private TcpSyslogReceivingChannelAdapter adapter2;

	@Autowired
	private PollableChannel outputChannelAlternate;

	@Autowired
	private TcpSyslogReceivingChannelAdapter fullBoatTcp;

	@Autowired
	private AbstractServerConnectionFactory cf;

	@Test
	public void testSimplestUdp() throws Exception {
		Method getPort = ReflectionUtils.findMethod(UdpSyslogReceivingChannelAdapter.class, "getPort");
		ReflectionUtils.makeAccessible(getPort);
		int port = (int) ReflectionUtils.invokeMethod(getPort, this.adapter1);
		byte[] buf = "<157>JUL 26 22:08:35 WEBERN TESTING[70729]: TEST SYSLOG MESSAGE".getBytes(StandardCharsets.UTF_8);
		DatagramPacket packet = new DatagramPacket(buf, buf.length, new InetSocketAddress("localhost", port));
		DatagramSocket socket = new DatagramSocket();
		Thread.sleep(1000);
		socket.send(packet);
		socket.close();
		Message<?> message = outputChannel.receive(10000);
		assertThat(message).isNotNull();
		adapter1.stop();
	}

	@Test
	public void testExplicitChannelUdp() {
		assertThat(TestUtils.<Integer>getPropertyValue(udpSyslogReceivingChannelAdapter, "udpAdapter.port"))
				.isEqualTo(1514);
		assertThat(TestUtils.<Object>getPropertyValue(udpSyslogReceivingChannelAdapter, "outputChannel"))
				.isSameAs(outputChannel);
	}

	@Test
	public void testExplicitUdp() {
		assertThat(TestUtils.<Object>getPropertyValue(explicitUdpAdapter, "outputChannel")).isSameAs(explicitUdp);
	}

	@Test
	public void testFullBoatUdp() {
		assertThat(TestUtils.<Object>getPropertyValue(fullBoatUdp, "outputChannel")).isSameAs(outputChannel);
		assertThat(fullBoatUdp.isAutoStartup()).isFalse();
		assertThat(fullBoatUdp.getPhase()).isEqualTo(123);
		assertThat(TestUtils.<Long>getPropertyValue(fullBoatUdp, "messagingTemplate.sendTimeout")).isEqualTo(456L);
		assertThat(TestUtils.<Object>getPropertyValue(fullBoatUdp, "converter")).isSameAs(converter);
		assertThat(TestUtils.<Object>getPropertyValue(fullBoatUdp, "errorChannel")).isSameAs(errors);
		assertThat(TestUtils.<Boolean>getPropertyValue(fullBoatUdp, "udpAdapter.mapper.lookupHost")).isFalse();
	}

	@Test
	public void testSimplestTcp() throws Exception {
		AbstractServerConnectionFactory connectionFactory = TestUtils.getPropertyValue(adapter2, "connectionFactory");
		waitListening(connectionFactory, 10000L);
		byte[] buf = "<157>JUL 26 22:08:35 WEBERN TESTING[70729]: TEST SYSLOG MESSAGE\n"
				.getBytes(StandardCharsets.UTF_8);
		int port = connectionFactory.getPort();
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		Thread.sleep(1000);
		socket.getOutputStream().write(buf);
		socket.close();
		Message<?> message = outputChannelAlternate.receive(10000);
		assertThat(message).isNotNull();
		adapter2.stop();
		assertThat(TestUtils.<Object>getPropertyValue(adapter2, "connectionFactory.applicationEventPublisher"))
				.isNotNull();
	}

	@Test
	public void testFullBoatTcp() {
		assertThat(TestUtils.<Object>getPropertyValue(fullBoatTcp, "outputChannel")).isSameAs(outputChannelAlternate);
		assertThat(fullBoatTcp.isAutoStartup()).isFalse();
		assertThat(fullBoatTcp.getPhase()).isEqualTo(123);
		assertThat(TestUtils.<Long>getPropertyValue(fullBoatUdp, "messagingTemplate.sendTimeout")).isEqualTo(456L);
		assertThat(TestUtils.<Object>getPropertyValue(fullBoatTcp, "converter")).isSameAs(rfc5424);
		assertThat(TestUtils.<Object>getPropertyValue(fullBoatTcp, "errorChannel")).isSameAs(errors);
		assertThat(TestUtils.<Object>getPropertyValue(fullBoatTcp, "connectionFactory")).isSameAs(cf);
	}

	@Test
	public void testPortOnUdpChild() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-fail1-context.xml",
								getClass()))
				.withMessageStartingWith(
						"Configuration problem: " +
								"When child element 'udp-attributes' is present, 'port' must be defined there");
	}

	@Test
	public void testPortWithTCPFactory() {
		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-fail2-context.xml",
								getClass()))
				.withStackTraceContaining("Cannot specify both 'port' and 'connectionFactory'");
	}

	@Test
	public void testUdpChildWithTcp() {
		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-fail3-context.xml",
								getClass()))
				.withStackTraceContaining("Cannot specify 'udp-attributes' when the protocol is 'tcp'");
	}

	@Test
	public void testUDPWithTCPFactory() {
		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext(this.getClass().getSimpleName() + "-fail4-context.xml",
								getClass()))
				.withStackTraceContaining("Cannot specify 'connection-factory' unless the protocol is 'tcp'");
	}

	public static class PassThruConverter implements MessageConverter {

		@Override
		public Message<?> fromSyslog(Message<?> syslog) {
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
