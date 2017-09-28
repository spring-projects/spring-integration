/*
 * Copyright 2002-2017 the original author or authors.
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSession;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.serializer.DefaultDeserializer;
import org.springframework.core.serializer.DefaultSerializer;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.codec.Codec;
import org.springframework.integration.codec.CodecMessageConverter;
import org.springframework.integration.codec.CompositeCodec;
import org.springframework.integration.codec.kryo.MessageCodec;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.tcp.serializer.MapJsonSerializer;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.converter.MapMessageConverter;
import org.springframework.integration.support.json.EmbeddedJsonHeadersMessageMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.util.MimeType;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 *
 */
public class TcpMessageMapperTests {

	private static final String TEST_PAYLOAD = "abcdefghijkl";

	private Codec codec;

	@Before
	public void setup() {
		Map<Class<?>, Codec> codecs = new HashMap<Class<?>, Codec>();
		this.codec = new CompositeCodec(codecs, new MessageCodec());
	}

	@Test
	public void testToMessage() throws Exception {
		TcpMessageMapper mapper = new TcpMessageMapper();
		TcpConnection connection = mock(TcpConnection.class);
		Socket socket = mock(Socket.class);
		InetAddress local = mock(InetAddress.class);
		SocketInfo info = new SocketInfo(socket);
		when(socket.getLocalAddress()).thenReturn(local);
		when(connection.getPayload()).thenReturn(TEST_PAYLOAD.getBytes());
		when(connection.getHostName()).thenReturn("MyHost");
		when(connection.getHostAddress()).thenReturn("1.1.1.1");
		when(connection.getPort()).thenReturn(1234);
		when(connection.getSocketInfo()).thenReturn(info);
		Message<?> message = mapper.toMessage(connection);
		assertEquals(TEST_PAYLOAD, new String((byte[]) message.getPayload()));
		assertEquals("MyHost", message.getHeaders().get(IpHeaders.HOSTNAME));
		assertEquals("1.1.1.1", message.getHeaders().get(IpHeaders.IP_ADDRESS));
		assertEquals(1234, message.getHeaders().get(IpHeaders.REMOTE_PORT));
		assertSame(local, message.getHeaders().get(IpHeaders.LOCAL_ADDRESS));
		assertNull(message.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}

	@Test
	public void testToMessageWithContentType() throws Exception {
		TcpMessageMapper mapper = new TcpMessageMapper();
		mapper.setAddContentTypeHeader(true);
		TcpConnection connection = mock(TcpConnection.class);
		Socket socket = mock(Socket.class);
		InetAddress local = mock(InetAddress.class);
		SocketInfo info = new SocketInfo(socket);
		when(socket.getLocalAddress()).thenReturn(local);
		when(connection.getPayload()).thenReturn(TEST_PAYLOAD.getBytes());
		when(connection.getHostName()).thenReturn("MyHost");
		when(connection.getHostAddress()).thenReturn("1.1.1.1");
		when(connection.getPort()).thenReturn(1234);
		when(connection.getSocketInfo()).thenReturn(info);
		Message<?> message = mapper.toMessage(connection);
		assertEquals(TEST_PAYLOAD, new String((byte[]) message.getPayload()));
		assertEquals("MyHost", message.getHeaders().get(IpHeaders.HOSTNAME));
		assertEquals("1.1.1.1", message.getHeaders().get(IpHeaders.IP_ADDRESS));
		assertEquals(1234, message.getHeaders().get(IpHeaders.REMOTE_PORT));
		assertSame(local, message.getHeaders().get(IpHeaders.LOCAL_ADDRESS));
		assertEquals("application/octet-stream;charset=UTF-8", message.getHeaders().get(MessageHeaders.CONTENT_TYPE));
		MimeType parseOk = MimeType.valueOf((String) message.getHeaders().get(MessageHeaders.CONTENT_TYPE));
		assertEquals(message.getHeaders().get(MessageHeaders.CONTENT_TYPE), parseOk.toString());
	}

	@Test
	public void testToMessageWithCustomContentType() throws Exception {
		TcpMessageMapper mapper = new TcpMessageMapper();
		mapper.setAddContentTypeHeader(true);
		mapper.setContentType("application/octet-stream;charset=ISO-8859-1");
		TcpConnection connection = mock(TcpConnection.class);
		Socket socket = mock(Socket.class);
		InetAddress local = mock(InetAddress.class);
		SocketInfo info = new SocketInfo(socket);
		when(socket.getLocalAddress()).thenReturn(local);
		when(connection.getPayload()).thenReturn(TEST_PAYLOAD.getBytes());
		when(connection.getHostName()).thenReturn("MyHost");
		when(connection.getHostAddress()).thenReturn("1.1.1.1");
		when(connection.getPort()).thenReturn(1234);
		when(connection.getSocketInfo()).thenReturn(info);
		Message<?> message = mapper.toMessage(connection);
		assertEquals(TEST_PAYLOAD, new String((byte[]) message.getPayload()));
		assertEquals("MyHost", message.getHeaders().get(IpHeaders.HOSTNAME));
		assertEquals("1.1.1.1", message.getHeaders().get(IpHeaders.IP_ADDRESS));
		assertEquals(1234, message.getHeaders().get(IpHeaders.REMOTE_PORT));
		assertSame(local, message.getHeaders().get(IpHeaders.LOCAL_ADDRESS));
		assertEquals("application/octet-stream;charset=ISO-8859-1", message.getHeaders().get(MessageHeaders.CONTENT_TYPE));
		MimeType parseOk = MimeType.valueOf((String) message.getHeaders().get(MessageHeaders.CONTENT_TYPE));
		assertEquals(message.getHeaders().get(MessageHeaders.CONTENT_TYPE), parseOk.toString());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testToMessageWithBadContentType() throws Exception {
		TcpMessageMapper mapper = new TcpMessageMapper();
		mapper.setAddContentTypeHeader(true);
		try {
			mapper.setContentType("");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), containsString("'contentType' could not be parsed"));
			throw e;
		}
	}

	@Test
	public void testToMessageSequence() throws Exception {
		TcpMessageMapper mapper = new TcpMessageMapper();
		Socket socket = SocketFactory.getDefault().createSocket();
		TcpConnection connection = new TcpConnectionSupport(socket, false, false, null, null) {

			@Override
			public void run() {
			}

			@Override
			public void send(Message<?> message) throws Exception {
			}

			@Override
			public boolean isOpen() {
				return false;
			}

			@Override
			public int getPort() {
				return 1234;
			}

			@Override
			public Object getPayload() throws Exception {
				return TEST_PAYLOAD.getBytes();
			}

			@Override
			public String getHostName() {
				return "MyHost";
			}

			@Override
			public String getHostAddress() {
				return "1.1.1.1";
			}

			@Override
			public String getConnectionId() {
				return "anId";
			}

			@Override
			public Object getDeserializerStateKey() {
				return null;
			}

			@Override
			public SSLSession getSslSession() {
				return null;
			}

		};
		Message<?> message = mapper.toMessage(connection);
		assertEquals(TEST_PAYLOAD, new String((byte[]) message.getPayload()));
		assertEquals("MyHost", message
				.getHeaders().get(IpHeaders.HOSTNAME));
		assertEquals("1.1.1.1", message
				.getHeaders().get(IpHeaders.IP_ADDRESS));
		assertEquals(1234, message
				.getHeaders().get(IpHeaders.REMOTE_PORT));
		assertEquals(0, new IntegrationMessageHeaderAccessor(message).getSequenceNumber());
		message = mapper.toMessage(connection);
		assertEquals(TEST_PAYLOAD, new String((byte[]) message.getPayload()));
		assertEquals("MyHost", message
				.getHeaders().get(IpHeaders.HOSTNAME));
		assertEquals("1.1.1.1", message
				.getHeaders().get(IpHeaders.IP_ADDRESS));
		assertEquals(1234, message
				.getHeaders().get(IpHeaders.REMOTE_PORT));
		assertEquals(0, new IntegrationMessageHeaderAccessor(message).getSequenceNumber());
	}

	@Test
	public void testToMessageSequenceNewWithCustomHeader() throws Exception {
		TcpMessageMapper mapper = new TcpMessageMapper() {

			@Override
			protected Map<String, ?> supplyCustomHeaders(TcpConnection connection) {
				return Collections.singletonMap("foo", "bar");
			}

		};
		mapper.setApplySequence(true);
		Socket socket = SocketFactory.getDefault().createSocket();
		TcpConnection connection = new TcpConnectionSupport(socket, false, false, null, null) {

			@Override
			public void run() {
			}

			@Override
			public void send(Message<?> message) throws Exception {
			}

			@Override
			public boolean isOpen() {
				return false;
			}

			@Override
			public int getPort() {
				return 1234;
			}

			@Override
			public Object getPayload() throws Exception {
				return TEST_PAYLOAD.getBytes();
			}

			@Override
			public String getHostName() {
				return "MyHost";
			}

			@Override
			public String getHostAddress() {
				return "1.1.1.1";
			}

			@Override
			public String getConnectionId() {
				return "anId";
			}

			@Override
			public Object getDeserializerStateKey() {
				return null;
			}

			@Override
			public SSLSession getSslSession() {
				return null;
			}

		};
		Message<?> message = mapper.toMessage(connection);
		assertEquals(TEST_PAYLOAD, new String((byte[]) message.getPayload()));
		assertEquals("MyHost", message
				.getHeaders().get(IpHeaders.HOSTNAME));
		assertEquals("1.1.1.1", message
				.getHeaders().get(IpHeaders.IP_ADDRESS));
		assertEquals(1234, message
				.getHeaders().get(IpHeaders.REMOTE_PORT));
		IntegrationMessageHeaderAccessor headerAccessor = new IntegrationMessageHeaderAccessor(message);
		assertEquals(1, headerAccessor.getSequenceNumber());
		assertEquals(message.getHeaders().get(IpHeaders.CONNECTION_ID), headerAccessor.getCorrelationId());
		message = mapper.toMessage(connection);
		headerAccessor = new IntegrationMessageHeaderAccessor(message);
		assertEquals(TEST_PAYLOAD, new String((byte[]) message.getPayload()));
		assertEquals("MyHost", message
				.getHeaders().get(IpHeaders.HOSTNAME));
		assertEquals("1.1.1.1", message
				.getHeaders().get(IpHeaders.IP_ADDRESS));
		assertEquals(1234, message
				.getHeaders().get(IpHeaders.REMOTE_PORT));
		assertEquals(2, headerAccessor.getSequenceNumber());
		assertEquals(message.getHeaders().get(IpHeaders.CONNECTION_ID), headerAccessor.getCorrelationId());
		assertNotNull(message.getHeaders().get("foo"));
		assertEquals("bar", message.getHeaders().get("foo"));

	}

	@Test
	public void testFromMessageBytes() throws Exception {
		String s = "test";
		Message<String> message = MessageBuilder.withPayload(s).build();
		TcpMessageMapper mapper = new TcpMessageMapper();
		mapper.setStringToBytes(true);
		byte[] bArray = (byte[]) mapper.fromMessage(message);
		assertEquals(s, new String(bArray));

	}

	@Test
	public void testFromMessage() throws Exception {
		String s = "test";
		Message<String> message = MessageBuilder.withPayload(s).build();
		TcpMessageMapper mapper = new TcpMessageMapper();
		mapper.setStringToBytes(false);
		String out = (String) mapper.fromMessage(message);
		assertEquals(s, out);

	}

	@Test
	public void testMapMessageConvertingOutboundJson() throws Exception {
		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader("bar", "baz")
				.build();
		MapMessageConverter converter = new MapMessageConverter();
		converter.setHeaderNames("bar");
		MessageConvertingTcpMessageMapper mapper = new MessageConvertingTcpMessageMapper(converter);
		Map<?, ?> map = (Map<?, ?>) mapper.fromMessage(message);
		MapJsonSerializer serializer = new MapJsonSerializer();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		serializer.serialize(map, baos);
		assertEquals("{\"headers\":{\"bar\":\"baz\"},\"payload\":\"foo\"}\n", new String(baos.toByteArray(), "UTF-8"));
	}

	@Test
	public void testMapMessageConvertingInboundJson() throws Exception {
		String json = "{\"headers\":{\"bar\":\"baz\"},\"payload\":\"foo\"}\n";
		MapMessageConverter converter = new MapMessageConverter();
		MessageConvertingTcpMessageMapper mapper = new MessageConvertingTcpMessageMapper(converter);
		MapJsonSerializer deserializer = new MapJsonSerializer();
		Map<?, ?> map = deserializer.deserialize(new ByteArrayInputStream(json.getBytes("UTF-8")));

		TcpConnection connection = mock(TcpConnection.class);
		when(connection.getPayload()).thenReturn(map);
		when(connection.getHostName()).thenReturn("someHost");
		when(connection.getHostAddress()).thenReturn("1.1.1.1");
		when(connection.getPort()).thenReturn(1234);
		when(connection.getConnectionId()).thenReturn("someId");
		Message<?> message = mapper.toMessage(connection);
		assertEquals("foo", message.getPayload());
		assertEquals("baz", message.getHeaders().get("bar"));
		assertEquals("someHost", message.getHeaders().get(IpHeaders.HOSTNAME));
		assertEquals("1.1.1.1", message.getHeaders().get(IpHeaders.IP_ADDRESS));
		assertEquals(1234, message.getHeaders().get(IpHeaders.REMOTE_PORT));
		assertEquals("someId", message.getHeaders().get(IpHeaders.CONNECTION_ID));
	}

	@Test
	public void testMapMessageConvertingBothWaysJava() throws Exception {
		Message<String> outMessage = MessageBuilder.withPayload("foo")
				.setHeader("bar", "baz")
				.build();
		MapMessageConverter converter = new MapMessageConverter();
		converter.setHeaderNames("bar");
		MessageConvertingTcpMessageMapper mapper = new MessageConvertingTcpMessageMapper(converter);
		Map<?, ?> map = (Map<?, ?>) mapper.fromMessage(outMessage);
		DefaultSerializer serializer = new DefaultSerializer();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		serializer.serialize(map, baos);

		DefaultDeserializer deserializer = new DefaultDeserializer();
		map = (Map<?, ?>) deserializer.deserialize(new ByteArrayInputStream(baos.toByteArray()));
		TcpConnection connection = mock(TcpConnection.class);
		when(connection.getPayload()).thenReturn(map);
		when(connection.getHostName()).thenReturn("someHost");
		when(connection.getHostAddress()).thenReturn("1.1.1.1");
		when(connection.getPort()).thenReturn(1234);
		when(connection.getConnectionId()).thenReturn("someId");
		Message<?> message = mapper.toMessage(connection);
		assertEquals("foo", message.getPayload());
		assertEquals("baz", message.getHeaders().get("bar"));
		assertEquals("someHost", message.getHeaders().get(IpHeaders.HOSTNAME));
		assertEquals("1.1.1.1", message.getHeaders().get(IpHeaders.IP_ADDRESS));
		assertEquals(1234, message.getHeaders().get(IpHeaders.REMOTE_PORT));
		assertEquals("someId", message.getHeaders().get(IpHeaders.CONNECTION_ID));
	}

	@Test
	public void testCodecMessageConvertingBothWaysJava() throws Exception {
		Message<String> outMessage = MessageBuilder.withPayload("foo")
				.setHeader("bar", "baz")
				.build();
		MessageConverter converter = new CodecMessageConverter(this.codec);
		MessageConvertingTcpMessageMapper mapper = new MessageConvertingTcpMessageMapper(converter);
		byte[] bytes = (byte[]) mapper.fromMessage(outMessage);

		TcpConnection connection = mock(TcpConnection.class);
		when(connection.getPayload()).thenReturn(bytes);
		when(connection.getHostName()).thenReturn("someHost");
		when(connection.getHostAddress()).thenReturn("1.1.1.1");
		when(connection.getPort()).thenReturn(1234);
		when(connection.getConnectionId()).thenReturn("someId");
		Message<?> message = mapper.toMessage(connection);
		assertEquals("foo", message.getPayload());
		assertEquals("baz", message.getHeaders().get("bar"));
		assertEquals("someHost", message.getHeaders().get(IpHeaders.HOSTNAME));
		assertEquals("1.1.1.1", message.getHeaders().get(IpHeaders.IP_ADDRESS));
		assertEquals(1234, message.getHeaders().get(IpHeaders.REMOTE_PORT));
		assertEquals("someId", message.getHeaders().get(IpHeaders.CONNECTION_ID));
	}

	@Test
	public void testWithBytesMapper() throws Exception {
		Message<String> outMessage = MessageBuilder.withPayload("foo")
				.setHeader("bar", "baz")
				.build();
		TcpMessageMapper mapper = new TcpMessageMapper();
		mapper.setBytesMessageMapper(new EmbeddedJsonHeadersMessageMapper());
		byte[] bytes = (byte[]) mapper.fromMessage(outMessage);

		TcpConnection connection = mock(TcpConnection.class);
		when(connection.getPayload()).thenReturn(bytes);
		when(connection.getHostName()).thenReturn("someHost");
		when(connection.getHostAddress()).thenReturn("1.1.1.1");
		when(connection.getPort()).thenReturn(1234);
		when(connection.getConnectionId()).thenReturn("someId");
		Message<?> message = mapper.toMessage(connection);
		assertEquals("foo", message.getPayload());
		assertEquals("baz", message.getHeaders().get("bar"));
		assertEquals("someHost", message.getHeaders().get(IpHeaders.HOSTNAME));
		assertEquals("1.1.1.1", message.getHeaders().get(IpHeaders.IP_ADDRESS));
		assertEquals(1234, message.getHeaders().get(IpHeaders.REMOTE_PORT));
		assertEquals("someId", message.getHeaders().get(IpHeaders.CONNECTION_ID));
	}

}
