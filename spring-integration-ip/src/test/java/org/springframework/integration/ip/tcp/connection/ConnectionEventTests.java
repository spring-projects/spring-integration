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

package org.springframework.integration.ip.tcp.connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ServerSocketFactory;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.log.LogAccessor;
import org.springframework.core.serializer.Serializer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.tcp.TcpInboundGateway;
import org.springframework.integration.ip.tcp.TcpOutboundGateway;
import org.springframework.integration.ip.tcp.TcpSendingMessageHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 3.0
 *
 */
public class ConnectionEventTests {

	@Test
	public void testConnectionEvents() throws Exception {
		Socket socket = mock(Socket.class);
		final List<TcpConnectionEvent> theEvent = new ArrayList<>();
		TcpNetConnection conn = new TcpNetConnection(socket, false, false, new ApplicationEventPublisher() {

			@Override
			public void publishEvent(ApplicationEvent event) {
				theEvent.add((TcpConnectionEvent) event);
			}

			@Override
			public void publishEvent(Object event) {

			}

		}, "foo");
		/*
		 *  Open is not published by the connection itself; the factory publishes it after initialization.
		 *  See ConnectionToConnectionTests.
		 */
		@SuppressWarnings("unchecked")
		Serializer<Object> serializer = mock(Serializer.class);
		RuntimeException toBeThrown = new RuntimeException("foo");
		doThrow(toBeThrown).when(serializer).serialize(Mockito.any(Object.class), Mockito.any(OutputStream.class));
		conn.setMapper(new TcpMessageMapper());
		conn.setSerializer(serializer);
		try {
			conn.send(new GenericMessage<>("bar"));
			fail("Expected exception");
		}
		catch (Exception e) {
		}
		assertThat(theEvent.size() > 0).isTrue();
		assertThat(theEvent.get(0)).isNotNull();
		assertThat(theEvent.get(0) instanceof TcpConnectionExceptionEvent).isTrue();
		assertThat(theEvent.get(0).toString().endsWith("[factory=foo, connectionId=" + conn.getConnectionId() + "]"))
				.isTrue();
		assertThat(theEvent.get(0).toString())
				.contains("RuntimeException: foo, failedMessage=GenericMessage [payload=bar");
		TcpConnectionExceptionEvent event = (TcpConnectionExceptionEvent) theEvent.get(0);
		assertThat(event.getCause()).isNotNull();
		assertThat(event.getCause().getCause()).isSameAs(toBeThrown);
		assertThat(theEvent.size() > 1).isTrue();
		assertThat(theEvent.get(1)).isNotNull();
		assertThat(theEvent.get(1).toString()
				.endsWith("[factory=foo, connectionId=" + conn.getConnectionId() + "] **CLOSED**")).isTrue();
	}

	@Test
	public void testNetServerExceptionEvent() throws Exception {
		AbstractServerConnectionFactory factory = new TcpNetServerConnectionFactory(0);
		testServerExceptionGuts(factory);
	}

	@Test
	public void testNioServerExceptionEvent() throws Exception {
		AbstractServerConnectionFactory factory = new TcpNioServerConnectionFactory(0);
		testServerExceptionGuts(factory);
	}

	@Test
	public void testOutboundChannelAdapterNoConnectionEvents() {
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		AbstractServerConnectionFactory scf = new AbstractServerConnectionFactory(0) {

			@Override
			public void run() {
			}

		};
		final AtomicReference<ApplicationEvent> theEvent = new AtomicReference<>();
		scf.setApplicationEventPublisher(new ApplicationEventPublisher() {

			@Override
			public void publishEvent(Object event) {
			}

			@Override
			public void publishEvent(ApplicationEvent event) {
				theEvent.set(event);
			}

		});
		handler.setConnectionFactory(scf);
		handler.start();
		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader(IpHeaders.CONNECTION_ID, "bar")
				.build();
		try {
			handler.handleMessage(message);
			fail("expected exception");
		}
		catch (MessageHandlingException e) {
			assertThat(e.getMessage()).contains("Unable to find outbound socket");
		}
		assertThat(theEvent.get()).isNotNull();
		TcpConnectionFailedCorrelationEvent event = (TcpConnectionFailedCorrelationEvent) theEvent.get();
		assertThat(event.getConnectionId()).isEqualTo("bar");
		assertThat(((MessagingException) event.getCause()).getFailedMessage()).isSameAs(message);
	}

	@Test
	public void testInboundGatewayNoConnectionEvents() {
		TcpInboundGateway gw = new TcpInboundGateway();
		AbstractServerConnectionFactory scf = new AbstractServerConnectionFactory(0) {

			@Override
			public void run() {
			}
		};
		final AtomicReference<ApplicationEvent> theEvent = new AtomicReference<>();
		scf.setApplicationEventPublisher(new ApplicationEventPublisher() {

			@Override
			public void publishEvent(Object event) {
			}

			@Override
			public void publishEvent(ApplicationEvent event) {
				theEvent.set(event);
			}

		});
		gw.setConnectionFactory(scf);
		DirectChannel requestChannel = new DirectChannel();
		requestChannel.subscribe(message -> ((MessageChannel) message.getHeaders().getReplyChannel()).send(message));
		gw.setRequestChannel(requestChannel);
		gw.start();
		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader(IpHeaders.CONNECTION_ID, "bar")
				.build();
		gw.onMessage(message);
		assertThat(theEvent.get()).isNotNull();
		TcpConnectionFailedCorrelationEvent event = (TcpConnectionFailedCorrelationEvent) theEvent.get();
		assertThat(event.getConnectionId()).isEqualTo("bar");
		assertThat(((MessagingException) event.getCause()).getFailedMessage()).isSameAs(message);
		gw.stop();
		scf.stop();
	}

	@Test
	public void testOutboundGatewayNoConnectionEvents() {
		TcpOutboundGateway gw = new TcpOutboundGateway();
		AbstractClientConnectionFactory ccf = new AbstractClientConnectionFactory("localhost", 0) {

		};
		final AtomicReference<ApplicationEvent> theEvent = new AtomicReference<>();
		ccf.setApplicationEventPublisher(new ApplicationEventPublisher() {

			@Override
			public void publishEvent(Object event) {
			}

			@Override
			public void publishEvent(ApplicationEvent event) {
				theEvent.set(event);
			}

		});
		gw.setConnectionFactory(ccf);
		DirectChannel requestChannel = new DirectChannel();
		requestChannel.subscribe(message -> ((MessageChannel) message.getHeaders().getReplyChannel()).send(message));
		gw.start();
		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader(IpHeaders.CONNECTION_ID, "bar")
				.build();
		gw.onMessage(message);
		assertThat(theEvent.get()).isNotNull();
		TcpConnectionFailedCorrelationEvent event = (TcpConnectionFailedCorrelationEvent) theEvent.get();
		assertThat(event.getConnectionId()).isEqualTo("bar");
		MessagingException messagingException = (MessagingException) event.getCause();
		assertThat(messagingException.getFailedMessage()).isSameAs(message);
		assertThat(messagingException.getMessage()).isEqualTo("Cannot correlate response - no pending reply for bar");

		message = new GenericMessage<>("foo");
		gw.onMessage(message);
		assertThat(theEvent.get()).isNotNull();
		event = (TcpConnectionFailedCorrelationEvent) theEvent.get();
		assertThat(event.getConnectionId()).isNull();
		messagingException = (MessagingException) event.getCause();
		assertThat(messagingException.getFailedMessage()).isSameAs(message);
		assertThat(messagingException.getMessage()).isEqualTo("Cannot correlate response - no connection id");
		gw.stop();
		ccf.stop();
	}

	private void testServerExceptionGuts(AbstractServerConnectionFactory factory) throws Exception {
		ServerSocket ss = ServerSocketFactory.getDefault().createServerSocket(0);
		factory.setPort(ss.getLocalPort());
		final AtomicReference<TcpConnectionServerExceptionEvent> theEvent = new AtomicReference<>();
		final CountDownLatch latch = new CountDownLatch(1);
		factory.setApplicationEventPublisher(new ApplicationEventPublisher() {

			@Override
			public void publishEvent(ApplicationEvent event) {
				theEvent.set((TcpConnectionServerExceptionEvent) event);
				latch.countDown();
			}

			@Override
			public void publishEvent(Object event) {

			}

		});
		factory.setBeanName("sf");
		factory.registerListener(message -> false);
		LogAccessor logger = spy(TestUtils.getPropertyValue(factory, "logger", LogAccessor.class));
		doNothing().when(logger).error(any(Throwable.class), anyString());
		new DirectFieldAccessor(factory).setPropertyValue("logger", logger);

		factory.start();
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		String actual = theEvent.toString();
		assertThat(actual).contains("cause=java.net.BindException");
		assertThat(actual).contains("source="
				+ "bean 'sf', port=" + factory.getPort());

		ArgumentCaptor<String> reasonCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
		verify(logger).error(throwableCaptor.capture(), reasonCaptor.capture());
		assertThat(reasonCaptor.getValue()).startsWith("Error on Server");
		assertThat(reasonCaptor.getValue()).endsWith("; port = " + factory.getPort());
		assertThat(throwableCaptor.getValue()).isInstanceOf(BindException.class);
		ss.close();
	}

	@Test
	public void testFailConnect() {
		AbstractClientConnectionFactory ccf = new AbstractClientConnectionFactory("junkjunk", 1234) {

			@Override
			protected boolean isActive() {
				return true;
			}

			@Override
			protected TcpConnectionSupport buildNewConnection() {
				throw new UncheckedIOException(new UnknownHostException("Mocking for test "));
			}

		};

		final AtomicReference<ApplicationEvent> failEvent = new AtomicReference<>();
		ccf.setApplicationEventPublisher(new ApplicationEventPublisher() {

			@Override
			public void publishEvent(Object event) {
			}

			@Override
			public void publishEvent(ApplicationEvent event) {
				failEvent.set(event);
			}

		});
		ccf.start();
		try {
			ccf.getConnection();
			fail("expected exception");
		}
		catch (Exception e) {
			assertThat(e.getCause()).isInstanceOf(UnknownHostException.class);
			TcpConnectionFailedEvent event = (TcpConnectionFailedEvent) failEvent.get();
			assertThat(event.getCause()).isSameAs(e);
		}

		ccf.stop();
	}

}
