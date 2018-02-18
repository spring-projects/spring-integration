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

package org.springframework.integration.ip.tcp.connection;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.OutputStream;
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

import org.apache.commons.logging.Log;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
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
		final List<TcpConnectionEvent> theEvent = new ArrayList<TcpConnectionEvent>();
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
			conn.send(new GenericMessage<String>("bar"));
			fail("Expected exception");
		}
		catch (Exception e) {
		}
		assertTrue(theEvent.size() > 0);
		assertNotNull(theEvent.get(0));
		assertTrue(theEvent.get(0) instanceof TcpConnectionExceptionEvent);
		assertTrue(theEvent.get(0).toString().endsWith("[factory=foo, connectionId=" + conn.getConnectionId() + "]"));
		assertThat(theEvent.get(0).toString(),
				containsString("RuntimeException: foo, failedMessage=GenericMessage [payload=bar"));
		TcpConnectionExceptionEvent event = (TcpConnectionExceptionEvent) theEvent.get(0);
		assertNotNull(event.getCause());
		assertSame(toBeThrown, event.getCause().getCause());
		assertTrue(theEvent.size() > 1);
		assertNotNull(theEvent.get(1));
		assertTrue(theEvent.get(1).toString()
				.endsWith("[factory=foo, connectionId=" + conn.getConnectionId() + "] **CLOSED**"));
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
		final AtomicReference<ApplicationEvent> theEvent = new AtomicReference<ApplicationEvent>();
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
			assertThat(e.getMessage(), Matchers.containsString("Unable to find outbound socket"));
		}
		assertNotNull(theEvent.get());
		TcpConnectionFailedCorrelationEvent event = (TcpConnectionFailedCorrelationEvent) theEvent.get();
		assertEquals("bar", event.getConnectionId());
		assertSame(message, ((MessagingException) event.getCause()).getFailedMessage());
	}

	@Test
	public void testInboundGatewayNoConnectionEvents() {
		TcpInboundGateway gw = new TcpInboundGateway();
		AbstractServerConnectionFactory scf = new AbstractServerConnectionFactory(0) {

			@Override
			public void run() {
			}
		};
		final AtomicReference<ApplicationEvent> theEvent = new AtomicReference<ApplicationEvent>();
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
		assertNotNull(theEvent.get());
		TcpConnectionFailedCorrelationEvent event = (TcpConnectionFailedCorrelationEvent) theEvent.get();
		assertEquals("bar", event.getConnectionId());
		assertSame(message, ((MessagingException) event.getCause()).getFailedMessage());
		gw.stop();
		scf.stop();
	}

	@Test
	public void testOutboundGatewayNoConnectionEvents() {
		TcpOutboundGateway gw = new TcpOutboundGateway();
		AbstractClientConnectionFactory ccf = new AbstractClientConnectionFactory("localhost", 0) {

		};
		final AtomicReference<ApplicationEvent> theEvent = new AtomicReference<ApplicationEvent>();
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
		assertNotNull(theEvent.get());
		TcpConnectionFailedCorrelationEvent event = (TcpConnectionFailedCorrelationEvent) theEvent.get();
		assertEquals("bar", event.getConnectionId());
		MessagingException messagingException = (MessagingException) event.getCause();
		assertSame(message, messagingException.getFailedMessage());
		assertEquals("Cannot correlate response - no pending reply for bar", messagingException.getMessage());

		message = new GenericMessage<String>("foo");
		gw.onMessage(message);
		assertNotNull(theEvent.get());
		event = (TcpConnectionFailedCorrelationEvent) theEvent.get();
		assertNull(event.getConnectionId());
		messagingException = (MessagingException) event.getCause();
		assertSame(message, messagingException.getFailedMessage());
		assertEquals("Cannot correlate response - no connection id", messagingException.getMessage());
		gw.stop();
		ccf.stop();
	}

	private void testServerExceptionGuts(AbstractServerConnectionFactory factory) throws Exception {
		ServerSocket ss = null;
		try {
			ss = ServerSocketFactory.getDefault().createServerSocket(0);
		}
		catch (Exception e) {
			fail("Failed to get a server socket");
		}
		factory.setPort(ss.getLocalPort());
		final AtomicReference<TcpConnectionServerExceptionEvent> theEvent =
				new AtomicReference<TcpConnectionServerExceptionEvent>();
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
		Log logger = spy(TestUtils.getPropertyValue(factory, "logger", Log.class));
		doNothing().when(logger).error(anyString(), any(Throwable.class));
		new DirectFieldAccessor(factory).setPropertyValue("logger", logger);

		factory.start();
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		String actual = theEvent.toString();
		assertThat(actual, containsString("cause=java.net.BindException"));
		assertThat(actual, containsString("source="
				+ "sf, port=" + factory.getPort()));

		ArgumentCaptor<String> reasonCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
		verify(logger).error(reasonCaptor.capture(), throwableCaptor.capture());
		assertThat(reasonCaptor.getValue(), startsWith("Error on Server"));
		assertThat(reasonCaptor.getValue(), endsWith("; port = " + factory.getPort()));
		assertThat(throwableCaptor.getValue(), instanceOf(BindException.class));
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
			protected TcpConnectionSupport buildNewConnection() throws Exception {
				throw new UnknownHostException("Mocking for test ");
			}

		};

		final AtomicReference<ApplicationEvent> failEvent = new AtomicReference<ApplicationEvent>();
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
			assertThat(e, instanceOf(UnknownHostException.class));
			TcpConnectionFailedEvent event = (TcpConnectionFailedEvent) failEvent.get();
			assertSame(e, event.getCause());
		}

		ccf.stop();
	}

}
