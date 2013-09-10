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
package org.springframework.integration.ip.tcp.connection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.ip.tcp.connection.TcpNioConnection.ChannelInputStream;
import org.springframework.integration.ip.tcp.serializer.ByteArrayStxEtxSerializer;
import org.springframework.integration.ip.tcp.serializer.MapJsonSerializer;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.converter.MapMessageConverter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;

/**
 * @author Gary Russell
 * @since 2.2.2
 *
 */
public class TcpNetConnectionTests {

	private final ApplicationEventPublisher nullPublisher = new ApplicationEventPublisher() {
		public void publishEvent(ApplicationEvent event) {
		}
	};

	@Test
	public void testErrorLog() throws Exception {
		Socket socket = mock(Socket.class);
		InputStream stream = mock(InputStream.class);
		when(socket.getInputStream()).thenReturn(stream);
		when(stream.read()).thenReturn((int) 'x');
		TcpNetConnection connection = new TcpNetConnection(socket, true, false, nullPublisher, null);
		connection.setDeserializer(new ByteArrayStxEtxSerializer());
		final AtomicReference<Object> log = new AtomicReference<Object>();
		Log logger = mock(Log.class);
		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				log.set(invocation.getArguments()[0]);
				return null;
			}
		}).when(logger).error(Mockito.anyString());
		DirectFieldAccessor accessor = new DirectFieldAccessor(connection);
		accessor.setPropertyValue("logger", logger);
		connection.registerListener(mock(TcpListener.class));
		connection.setMapper(new TcpMessageMapper());
		connection.run();
		assertNotNull(log.get());
		assertEquals("Read exception " +
				connection.getConnectionId() +
				" MessageMappingException:Expected STX to begin message",
				log.get());
	}

	@Test
	public void testBinary() throws Exception {
		SocketChannel socketChannel = mock(SocketChannel.class);
		Socket socket = mock(Socket.class);
		when(socketChannel.socket()).thenReturn(socket);
		TcpNioConnection connection = new TcpNioConnection(socketChannel, true, false, nullPublisher, null);
		ChannelInputStream inputStream = TestUtils.getPropertyValue(connection, "channelInputStream", ChannelInputStream.class);
		inputStream.write(new byte[] {(byte) 0x80}, 1);
		assertEquals(0x80, inputStream.read());
	}

	@Test
	public void transferHeaders() throws Exception {
		Socket inSocket = mock(Socket.class);
		PipedInputStream pipe = new PipedInputStream();
		when(inSocket.getInputStream()).thenReturn(pipe);

		TcpConnectionSupport inboundConnection = new TcpNetConnection(inSocket, true, false, nullPublisher, null);
		inboundConnection.setDeserializer(new MapJsonSerializer());
		MapMessageConverter inConverter = new MapMessageConverter();
		MessageConvertingTcpMessageMapper inMapper = new MessageConvertingTcpMessageMapper(inConverter);
		inboundConnection.setMapper(inMapper);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Socket outSocket = mock(Socket.class);
		TcpNetConnection outboundConnection = new TcpNetConnection(outSocket, true, false, nullPublisher, null);
		when(outSocket.getOutputStream()).thenReturn(baos);

		MapMessageConverter outConverter = new MapMessageConverter();
		outConverter.setHeaderNames("bar");
		MessageConvertingTcpMessageMapper outMapper = new MessageConvertingTcpMessageMapper(outConverter);
		outboundConnection.setMapper(outMapper);
		outboundConnection.setSerializer(new MapJsonSerializer());

		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader("bar", "baz")
				.build();
		outboundConnection.send(message);
		PipedOutputStream out = new PipedOutputStream(pipe);
		out.write(baos.toByteArray());
		out.close();

		final AtomicReference<Message<?>> inboundMessage = new AtomicReference<Message<?>>();
		TcpListener listener = new TcpListener() {

			public boolean onMessage(Message<?> message) {
				if (!(message instanceof ErrorMessage)) {
					inboundMessage.set(message);
				}
				return false;
			}
		};
		inboundConnection.registerListener(listener);
		inboundConnection.run();
		assertNotNull(inboundMessage.get());
		assertEquals("foo", inboundMessage.get().getPayload());
		assertEquals("baz", inboundMessage.get().getHeaders().get("bar"));
	}
}
