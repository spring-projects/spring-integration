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

package org.springframework.integration.syslog.inbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;

import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.log.LogAccessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNioServerConnectionFactory;
import org.springframework.integration.ip.udp.UnicastReceivingChannelAdapter;
import org.springframework.integration.ip.util.TestingUtilities;
import org.springframework.integration.syslog.DefaultMessageConverter;
import org.springframework.integration.syslog.RFC5424MessageConverter;
import org.springframework.integration.syslog.config.SyslogReceivingChannelAdapterFactoryBean;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;

/**
 * @author Gary Russell
 * @author David Liu
 * @author Artem Bilan
 *
 * @since 3.0
 *
 */
public class SyslogReceivingChannelAdapterTests {

	@Test
	public void testUdp() throws Exception {
		SyslogReceivingChannelAdapterFactoryBean factory = new SyslogReceivingChannelAdapterFactoryBean(
				SyslogReceivingChannelAdapterFactoryBean.Protocol.udp);
		PollableChannel outputChannel = new QueueChannel();
		factory.setPort(0);
		factory.setOutputChannel(outputChannel);
		factory.setBeanFactory(mock(BeanFactory.class));
		factory.afterPropertiesSet();
		factory.start();
		UnicastReceivingChannelAdapter server = TestUtils.getPropertyValue(factory, "syslogAdapter.udpAdapter",
				UnicastReceivingChannelAdapter.class);
		TestingUtilities.waitListening(server, null);
		UdpSyslogReceivingChannelAdapter adapter = (UdpSyslogReceivingChannelAdapter) factory.getObject();
		byte[] buf = "<157>JUL 26 22:08:35 WEBERN TESTING[70729]: TEST SYSLOG MESSAGE".getBytes(StandardCharsets.UTF_8);
		DatagramPacket packet = new DatagramPacket(buf, buf.length, new InetSocketAddress("localhost",
				server.getPort()));
		DatagramSocket socket = new DatagramSocket();
		socket.send(packet);
		socket.close();
		Message<?> message = outputChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(message.getHeaders().get("syslog_HOST")).isEqualTo("WEBERN");
		assertThat(message.getHeaders().get(IpHeaders.IP_ADDRESS)).isNotNull();
		adapter.stop();
	}

	@Test
	public void testTcp() throws Exception {
		SyslogReceivingChannelAdapterFactoryBean factory = new SyslogReceivingChannelAdapterFactoryBean(
				SyslogReceivingChannelAdapterFactoryBean.Protocol.tcp);
		factory.setPort(0);
		PollableChannel outputChannel = new QueueChannel();
		factory.setOutputChannel(outputChannel);
		ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
		final CountDownLatch latch = new CountDownLatch(2);
		doAnswer(invocation -> {
			latch.countDown();
			return null;
		}).when(publisher).publishEvent(any(ApplicationEvent.class));
		factory.setApplicationEventPublisher(publisher);
		factory.setBeanFactory(mock(BeanFactory.class));
		factory.afterPropertiesSet();
		factory.start();
		AbstractServerConnectionFactory server = TestUtils.getPropertyValue(factory, "syslogAdapter.connectionFactory",
				AbstractServerConnectionFactory.class);
		TestingUtilities.waitListening(server, null);
		TcpSyslogReceivingChannelAdapter adapter = (TcpSyslogReceivingChannelAdapter) factory.getObject();
		LogAccessor logger = spy(TestUtils.getPropertyValue(adapter, "logger", LogAccessor.class));
		doReturn(true).when(logger).isDebugEnabled();
		final CountDownLatch sawLog = new CountDownLatch(1);
		doAnswer(invocation -> {
			if (((String) invocation.getArgument(0)).contains("Error on syslog socket")) {
				sawLog.countDown();
			}
			invocation.callRealMethod();
			return null;
		}).when(logger).debug(anyString());
		new DirectFieldAccessor(adapter).setPropertyValue("logger", logger);
		byte[] buf = "<157>JUL 26 22:08:35 WEBERN TESTING[70729]: TEST SYSLOG MESSAGE\n".getBytes(StandardCharsets.UTF_8);
		Socket socket = SocketFactory.getDefault().createSocket("localhost", server.getPort());
		socket.getOutputStream().write(buf);
		socket.close();
		assertThat(sawLog.await(10, TimeUnit.SECONDS)).isTrue();
		Message<?> message = outputChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(message.getHeaders().get("syslog_HOST")).isEqualTo("WEBERN");
		assertThat(message.getHeaders().get(IpHeaders.IP_ADDRESS)).isNotNull();
		adapter.stop();
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	public void testAsMapFalse() throws Exception {
		SyslogReceivingChannelAdapterFactoryBean factory = new SyslogReceivingChannelAdapterFactoryBean(
				SyslogReceivingChannelAdapterFactoryBean.Protocol.udp);
		factory.setPort(0);
		PollableChannel outputChannel = new QueueChannel();
		factory.setOutputChannel(outputChannel);
		factory.setBeanFactory(mock(BeanFactory.class));
		factory.afterPropertiesSet();
		factory.start();
		UnicastReceivingChannelAdapter server = TestUtils.getPropertyValue(factory, "syslogAdapter.udpAdapter",
				UnicastReceivingChannelAdapter.class);
		TestingUtilities.waitListening(server, null);
		UdpSyslogReceivingChannelAdapter adapter = (UdpSyslogReceivingChannelAdapter) factory.getObject();
		DefaultMessageConverter defaultMessageConverter = new DefaultMessageConverter();
		defaultMessageConverter.setAsMap(false);
		adapter.setConverter(defaultMessageConverter);
		byte[] buf = "<157>JUL 26 22:08:35 WEBERN TESTING[70729]: TEST SYSLOG MESSAGE".getBytes(StandardCharsets.UTF_8);
		DatagramPacket packet = new DatagramPacket(buf, buf.length, new InetSocketAddress("localhost",
				adapter.getPort()));
		DatagramSocket socket = new DatagramSocket();
		socket.send(packet);
		socket.close();
		Message<?> message = outputChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(message.getHeaders().get("syslog_HOST")).isEqualTo("WEBERN");
		assertThat(new String((byte[]) message.getPayload(), StandardCharsets.UTF_8))
				.isEqualTo("<157>JUL 26 22:08:35 WEBERN TESTING[70729]: TEST SYSLOG MESSAGE");
		assertThat(message.getHeaders().get(IpHeaders.IP_ADDRESS)).isNotNull();
		adapter.stop();
	}

	@Test
	public void testTcpRFC5424() throws Exception {
		SyslogReceivingChannelAdapterFactoryBean factory = new SyslogReceivingChannelAdapterFactoryBean(
				SyslogReceivingChannelAdapterFactoryBean.Protocol.tcp);
		PollableChannel outputChannel = new QueueChannel();
		factory.setOutputChannel(outputChannel);
		ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
		final CountDownLatch latch = new CountDownLatch(2);
		doAnswer(invocation -> {
			latch.countDown();
			return null;
		}).when(publisher).publishEvent(any(ApplicationEvent.class));
		factory.setBeanFactory(mock(BeanFactory.class));
		AbstractServerConnectionFactory connectionFactory = new TcpNioServerConnectionFactory(0);
		connectionFactory.setDeserializer(new RFC6587SyslogDeserializer());
		connectionFactory.setApplicationEventPublisher(publisher);
		factory.setConnectionFactory(connectionFactory);
		factory.setConverter(new RFC5424MessageConverter());
		factory.afterPropertiesSet();
		factory.start();
		TestingUtilities.waitListening(connectionFactory, null);
		TcpSyslogReceivingChannelAdapter adapter = (TcpSyslogReceivingChannelAdapter) factory.getObject();
		LogAccessor logger = spy(TestUtils.getPropertyValue(adapter, "logger", LogAccessor.class));
		doReturn(true).when(logger).isDebugEnabled();
		final CountDownLatch sawLog = new CountDownLatch(1);
		doAnswer(invocation -> {
			if (((String) invocation.getArgument(0)).contains("Error on syslog socket")) {
				sawLog.countDown();
			}
			invocation.callRealMethod();
			return null;
		}).when(logger).debug(anyString());
		new DirectFieldAccessor(adapter).setPropertyValue("logger", logger);
		byte[] buf = ("253 <14>1 2014-06-20T09:14:07+00:00 loggregator d0602076-b14a-4c55-852a-981e7afeed38 DEA - " +
				"[exampleSDID@32473 iut=\\\"3\\\" eventSource=\\\"Application\\\" eventID=\\\"1011\\\"]" +
				"[exampleSDID@32473 iut=\\\"3\\\" eventSource=\\\"Application\\\" eventID=\\\"1011\\\"] Removing instance")
				.getBytes(StandardCharsets.UTF_8);
		Socket socket = SocketFactory.getDefault().createSocket("localhost", connectionFactory.getPort());
		socket.getOutputStream().write(buf);
		socket.close();
		assertThat(sawLog.await(10, TimeUnit.SECONDS)).isTrue();
		@SuppressWarnings("unchecked")
		Message<Map<String, ?>> message = (Message<Map<String, ?>>) outputChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload().get("syslog_HOST")).isEqualTo("loggregator");
		assertThat(message.getHeaders().get(IpHeaders.IP_ADDRESS)).isNotNull();
		adapter.stop();
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	public void testUdpRFC5424() throws Exception {
		SyslogReceivingChannelAdapterFactoryBean factory = new SyslogReceivingChannelAdapterFactoryBean(
				SyslogReceivingChannelAdapterFactoryBean.Protocol.udp);
		factory.setPort(0);
		PollableChannel outputChannel = new QueueChannel();
		factory.setOutputChannel(outputChannel);
		factory.setBeanFactory(mock(BeanFactory.class));
		factory.setConverter(new RFC5424MessageConverter());
		factory.afterPropertiesSet();
		factory.start();
		UnicastReceivingChannelAdapter server = TestUtils.getPropertyValue(factory, "syslogAdapter.udpAdapter",
				UnicastReceivingChannelAdapter.class);
		TestingUtilities.waitListening(server, null);
		UdpSyslogReceivingChannelAdapter adapter = (UdpSyslogReceivingChannelAdapter) factory.getObject();
		byte[] buf = ("<14>1 2014-06-20T09:14:07+00:00 loggregator d0602076-b14a-4c55-852a-981e7afeed38 DEA - " +
				"[exampleSDID@32473 iut=\\\"3\\\" eventSource=\\\"Application\\\" eventID=\\\"1011\\\"]" +
				"[exampleSDID@32473 iut=\\\"3\\\" eventSource=\\\"Application\\\" eventID=\\\"1011\\\"] Removing instance")
				.getBytes(StandardCharsets.UTF_8);
		DatagramPacket packet = new DatagramPacket(buf, buf.length, new InetSocketAddress("localhost",
				adapter.getPort()));
		DatagramSocket socket = new DatagramSocket();
		socket.send(packet);
		socket.close();
		@SuppressWarnings("unchecked")
		Message<Map<String, ?>> message = (Message<Map<String, ?>>) outputChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload().get("syslog_HOST")).isEqualTo("loggregator");
		assertThat(message.getHeaders().get(IpHeaders.IP_ADDRESS)).isNotNull();
		adapter.stop();
	}

}
