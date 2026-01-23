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

package org.springframework.integration.ip.tcp.connection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.SocketFactory;

import org.apache.commons.logging.Log;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.ip.tcp.connection.TcpNioConnection.ChannelInputStream;
import org.springframework.integration.ip.tcp.serializer.ByteArrayStxEtxSerializer;
import org.springframework.integration.ip.tcp.serializer.MapJsonSerializer;
import org.springframework.integration.ip.tcp.serializer.SoftEndOfStreamException;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.converter.MapMessageConverter;
import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 2.2.2
 *
 */
public class TcpNetConnectionTests implements TestApplicationContextAware {

	@Test
	public void testErrorLog() throws Exception {
		Socket socket = mock(Socket.class);
		InputStream stream = mock(InputStream.class);
		when(socket.getInputStream()).thenReturn(stream);
		when(stream.read()).thenReturn((int) 'x');
		TcpNetConnection connection = new TcpNetConnection(socket, true, false, e -> {
		}, null);
		connection.setDeserializer(new ByteArrayStxEtxSerializer());
		final AtomicReference<Object> log = new AtomicReference<>();
		Log logger = mock(Log.class);
		given(logger.isErrorEnabled()).willReturn(true);
		doAnswer(invocation -> {
			log.set(invocation.getArguments()[0]);
			return null;
		}).when(logger).error(Mockito.anyString());
		DirectFieldAccessor accessor = new DirectFieldAccessor(connection);
		accessor.setPropertyValue("logger", logger);
		connection.registerListener(mock(TcpListener.class));
		connection.setMapper(new TcpMessageMapper());
		connection.run();
		assertThat(log.get()).isNotNull();
		assertThat(log.get()).isEqualTo("Read exception " +
				connection.getConnectionId() +
				" MessageMappingException:Expected STX to begin message");
	}

	@Test
	public void testBinary() throws Exception {
		SocketChannel socketChannel = mock(SocketChannel.class);
		Socket socket = mock(Socket.class);
		when(socketChannel.socket()).thenReturn(socket);
		TcpNioConnection connection = new TcpNioConnection(socketChannel, true, false, e -> {
		}, null);
		ChannelInputStream inputStream =
				TestUtils.<ChannelInputStream>getPropertyValue(connection, "channelInputStream");
		inputStream.write(ByteBuffer.wrap(new byte[] {(byte) 0x80}));
		assertThat(inputStream.read()).isEqualTo(0x80);
	}

	@Test
	public void transferHeaders() throws Exception {
		Socket inSocket = mock(Socket.class);
		PipedInputStream pipe = new PipedInputStream();
		when(inSocket.getInputStream()).thenReturn(pipe);

		TcpConnectionSupport inboundConnection = new TcpNetConnection(inSocket, true, false, e -> {
		}, null);
		inboundConnection.setDeserializer(new MapJsonSerializer());
		MapMessageConverter inConverter = new MapMessageConverter();
		MessageConvertingTcpMessageMapper inMapper = new MessageConvertingTcpMessageMapper(inConverter);
		inboundConnection.setMapper(inMapper);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Socket outSocket = mock(Socket.class);
		TcpNetConnection outboundConnection = new TcpNetConnection(outSocket, true, false, e -> {
		}, null);
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

		final AtomicReference<Message<?>> inboundMessage = new AtomicReference<>();
		TcpListener listener = message1 -> {
			if (!(message1 instanceof ErrorMessage)) {
				inboundMessage.set(message1);
			}
		};
		inboundConnection.registerListener(listener);
		inboundConnection.run();
		assertThat(inboundMessage.get()).isNotNull();
		assertThat(inboundMessage.get().getPayload()).isEqualTo("foo");
		assertThat(inboundMessage.get().getHeaders()).containsEntry("bar", "baz");
	}

	@Test
	public void socketClosedNextRead() throws InterruptedException, IOException {
		TcpNetServerConnectionFactory server = new TcpNetServerConnectionFactory(0);
		server.setTaskScheduler(new SimpleAsyncTaskScheduler());
		AtomicInteger port = new AtomicInteger();
		CountDownLatch latch = new CountDownLatch(1);
		ApplicationEventPublisher publisher = ev -> {
			if (ev instanceof TcpConnectionServerListeningEvent) {
				port.set(((TcpConnectionServerListeningEvent) ev).getPort());
				latch.countDown();
			}
		};
		server.setApplicationEventPublisher(publisher);
		server.registerListener(message -> {
		});
		server.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		server.afterPropertiesSet();
		server.start();
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port.get());
		TcpNetConnection connection = new TcpNetConnection(socket, false, false, publisher, "socketClosedNextRead");
		socket.close();
		assertThatExceptionOfType(SoftEndOfStreamException.class)
				.isThrownBy(connection::getPayload);
		server.stop();
	}

}
