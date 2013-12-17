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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.Socket;
import java.util.Collections;
import java.util.Map;

import javax.net.SocketFactory;

import org.junit.Test;

import org.springframework.core.serializer.DefaultDeserializer;
import org.springframework.core.serializer.DefaultSerializer;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.tcp.serializer.MapJsonSerializer;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.converter.MapMessageConverter;
import org.springframework.messaging.Message;

/**
 * @author Gary Russell
 * @since 2.0
 *
 */
public class TcpMessageMapperTests {

	private static final String TEST_PAYLOAD = "abcdefghijkl";

	@Test
	public void testToMessage() throws Exception {

		TcpMessageMapper mapper = new TcpMessageMapper();
		TcpConnection connection = mock(TcpConnection.class);
		when(connection.getPayload()).thenReturn(TEST_PAYLOAD.getBytes());
		when(connection.getHostName()).thenReturn("MyHost");
		when(connection.getHostAddress()).thenReturn("1.1.1.1");
		when(connection.getPort()).thenReturn(1234);
		Message<?> message = mapper.toMessage(connection);
		assertEquals(TEST_PAYLOAD, new String((byte[]) message.getPayload()));
		assertEquals("MyHost", message
				.getHeaders().get(IpHeaders.HOSTNAME));
		assertEquals("1.1.1.1", message
				.getHeaders().get(IpHeaders.IP_ADDRESS));
		assertEquals(1234, message
				.getHeaders().get(IpHeaders.REMOTE_PORT));
	}

	@Test
	public void testToMessageSequence() throws Exception {

		TcpMessageMapper mapper = new TcpMessageMapper();
		Socket socket = SocketFactory.getDefault().createSocket();
		TcpConnection connection = new TcpConnectionSupport(socket, false, false, null, null) {
			public void run() {
			}
			public void send(Message<?> message) throws Exception {
			}
			public boolean isOpen() {
				return false;
			}
			public int getPort() {
				return 1234;
			}
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
			public Object getDeserializerStateKey() {
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
		assertEquals(Integer.valueOf(0), new IntegrationMessageHeaderAccessor(message).getSequenceNumber());
		message = mapper.toMessage(connection);
		assertEquals(TEST_PAYLOAD, new String((byte[]) message.getPayload()));
		assertEquals("MyHost", message
				.getHeaders().get(IpHeaders.HOSTNAME));
		assertEquals("1.1.1.1", message
				.getHeaders().get(IpHeaders.IP_ADDRESS));
		assertEquals(1234, message
				.getHeaders().get(IpHeaders.REMOTE_PORT));
		assertEquals(Integer.valueOf(0), new IntegrationMessageHeaderAccessor(message).getSequenceNumber());
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
			public void run() {
			}
			public void send(Message<?> message) throws Exception {
			}
			public boolean isOpen() {
				return false;
			}
			public int getPort() {
				return 1234;
			}
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
			public Object getDeserializerStateKey() {
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
		assertEquals(Integer.valueOf(1), headerAccessor.getSequenceNumber());
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
		assertEquals(Integer.valueOf(2), headerAccessor.getSequenceNumber());
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
}
