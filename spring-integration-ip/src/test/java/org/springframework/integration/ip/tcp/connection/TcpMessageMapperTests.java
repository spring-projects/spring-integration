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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.Map;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Gengwu Zhao
 * @author Glenn Renfro
 * @since 2.0
 *
 */
public class TcpMessageMapperTests {

	private static final String TEST_PAYLOAD = "abcdefghijkl";

	private Codec codec;

	@BeforeEach
	public void setup() {
		MessageCodec messageCodec = new MessageCodec();
		Map<Class<?>, Codec> codecs = Map.of(messageCodec.getClass(), messageCodec);
		this.codec = new CompositeCodec(codecs, new MessageCodec());
	}

	@Test
	public void testToMessage() {
		TcpMessageMapper mapper = new TcpMessageMapper();
		TcpConnection connection = creatMockTcpConcnection(TEST_PAYLOAD.getBytes(), "MyHost", "1.1.1.1", 1234);
		InetAddress local = mock(InetAddress.class);
		Socket socket = creatMockSocket(local);
		SocketInfo info = new SocketInfo(socket);
		when(connection.getSocketInfo()).thenReturn(info);
		Message<?> message = mapper.toMessage(connection);
		assertThat(new String((byte[]) message.getPayload())).isEqualTo(TEST_PAYLOAD);
		assertThat(message.getHeaders().get(IpHeaders.HOSTNAME)).isEqualTo("MyHost");
		assertThat(message.getHeaders().get(IpHeaders.IP_ADDRESS)).isEqualTo("1.1.1.1");
		assertThat(message.getHeaders().get(IpHeaders.REMOTE_PORT)).isEqualTo(1234);
		assertThat(message.getHeaders().get(IpHeaders.LOCAL_ADDRESS)).isSameAs(local);
		assertThat(message.getHeaders().get(MessageHeaders.CONTENT_TYPE)).isNull();
	}

	@Test
	public void testToMessageWithContentType() {
		TcpMessageMapper mapper = new TcpMessageMapper();
		mapper.setAddContentTypeHeader(true);
		TcpConnection connection = creatMockTcpConcnection(TEST_PAYLOAD.getBytes(), "MyHost", "1.1.1.1", 1234);
		InetAddress local = mock(InetAddress.class);
		Socket socket = creatMockSocket(local);
		SocketInfo info = new SocketInfo(socket);
		when(connection.getSocketInfo()).thenReturn(info);
		Message<?> message = mapper.toMessage(connection);
		assertThat(new String((byte[]) message.getPayload())).isEqualTo(TEST_PAYLOAD);
		assertThat(message.getHeaders().get(IpHeaders.HOSTNAME)).isEqualTo("MyHost");
		assertThat(message.getHeaders().get(IpHeaders.IP_ADDRESS)).isEqualTo("1.1.1.1");
		assertThat(message.getHeaders().get(IpHeaders.REMOTE_PORT)).isEqualTo(1234);
		assertThat(message.getHeaders().get(IpHeaders.LOCAL_ADDRESS)).isSameAs(local);
		assertThat(message.getHeaders().get(MessageHeaders.CONTENT_TYPE))
				.isEqualTo("application/octet-stream;charset=UTF-8");
		MimeType parseOk = MimeType.valueOf((String) message.getHeaders().get(MessageHeaders.CONTENT_TYPE));
		assertThat(parseOk.toString()).isEqualTo(message.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}

	@Test
	public void testToMessageWithCustomContentType() {
		TcpMessageMapper mapper = new TcpMessageMapper();
		mapper.setAddContentTypeHeader(true);
		mapper.setContentType("application/octet-stream;charset=ISO-8859-1");
		TcpConnection connection = creatMockTcpConcnection(TEST_PAYLOAD.getBytes(), "MyHost", "1.1.1.1", 1234);
		InetAddress local = mock(InetAddress.class);
		Socket socket = creatMockSocket(local);
		SocketInfo info = new SocketInfo(socket);
		when(connection.getSocketInfo()).thenReturn(info);
		Message<?> message = mapper.toMessage(connection);
		assertThat(new String((byte[]) message.getPayload())).isEqualTo(TEST_PAYLOAD);
		assertThat(message.getHeaders().get(IpHeaders.HOSTNAME)).isEqualTo("MyHost");
		assertThat(message.getHeaders().get(IpHeaders.IP_ADDRESS)).isEqualTo("1.1.1.1");
		assertThat(message.getHeaders().get(IpHeaders.REMOTE_PORT)).isEqualTo(1234);
		assertThat(message.getHeaders().get(IpHeaders.LOCAL_ADDRESS)).isSameAs(local);
		assertThat(message.getHeaders().get(MessageHeaders.CONTENT_TYPE))
				.isEqualTo("application/octet-stream;charset=ISO-8859-1");
		MimeType parseOk = MimeType.valueOf((String) message.getHeaders().get(MessageHeaders.CONTENT_TYPE));
		assertThat(parseOk.toString()).isEqualTo(message.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}

	@Test
	public void testToMessageWithBadContentType() {
		TcpMessageMapper mapper = new TcpMessageMapper();
		mapper.setAddContentTypeHeader(true);
		assertThatIllegalArgumentException()
				.isThrownBy(() -> mapper.setContentType(""))
				.withMessageContaining("'contentType' could not be parsed");
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
			public void send(Message<?> message) {
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
			public Object getPayload() {
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
		assertThat(new String((byte[]) message.getPayload())).isEqualTo(TEST_PAYLOAD);
		assertThat(message
				.getHeaders().get(IpHeaders.HOSTNAME)).isEqualTo("MyHost");
		assertThat(message
				.getHeaders().get(IpHeaders.IP_ADDRESS)).isEqualTo("1.1.1.1");
		assertThat(message
				.getHeaders().get(IpHeaders.REMOTE_PORT)).isEqualTo(1234);
		assertThat(new IntegrationMessageHeaderAccessor(message).getSequenceNumber()).isEqualTo(0);
		message = mapper.toMessage(connection);
		assertThat(new String((byte[]) message.getPayload())).isEqualTo(TEST_PAYLOAD);
		assertThat(message
				.getHeaders().get(IpHeaders.HOSTNAME)).isEqualTo("MyHost");
		assertThat(message
				.getHeaders().get(IpHeaders.IP_ADDRESS)).isEqualTo("1.1.1.1");
		assertThat(message
				.getHeaders().get(IpHeaders.REMOTE_PORT)).isEqualTo(1234);
		assertThat(new IntegrationMessageHeaderAccessor(message).getSequenceNumber()).isEqualTo(0);
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
			public void send(Message<?> message) {
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
			public Object getPayload() {
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
		assertThat(new String((byte[]) message.getPayload())).isEqualTo(TEST_PAYLOAD);
		assertThat(message
				.getHeaders().get(IpHeaders.HOSTNAME)).isEqualTo("MyHost");
		assertThat(message
				.getHeaders().get(IpHeaders.IP_ADDRESS)).isEqualTo("1.1.1.1");
		assertThat(message
				.getHeaders().get(IpHeaders.REMOTE_PORT)).isEqualTo(1234);
		IntegrationMessageHeaderAccessor headerAccessor = new IntegrationMessageHeaderAccessor(message);
		assertThat(headerAccessor.getSequenceNumber()).isEqualTo(1);
		assertThat(headerAccessor.getCorrelationId()).isEqualTo(message.getHeaders().get(IpHeaders.CONNECTION_ID));
		message = mapper.toMessage(connection);
		headerAccessor = new IntegrationMessageHeaderAccessor(message);
		assertThat(new String((byte[]) message.getPayload())).isEqualTo(TEST_PAYLOAD);
		assertThat(message
				.getHeaders().get(IpHeaders.HOSTNAME)).isEqualTo("MyHost");
		assertThat(message
				.getHeaders().get(IpHeaders.IP_ADDRESS)).isEqualTo("1.1.1.1");
		assertThat(message
				.getHeaders().get(IpHeaders.REMOTE_PORT)).isEqualTo(1234);
		assertThat(headerAccessor.getSequenceNumber()).isEqualTo(2);
		assertThat(headerAccessor.getCorrelationId()).isEqualTo(message.getHeaders().get(IpHeaders.CONNECTION_ID));
		assertThat(message.getHeaders().get("foo")).isNotNull();
		assertThat(message.getHeaders().get("foo")).isEqualTo("bar");

	}

	@Test
	public void testFromMessageBytes() throws Exception {
		String s = "test";
		Message<String> message = MessageBuilder.withPayload(s).build();
		TcpMessageMapper mapper = new TcpMessageMapper();
		mapper.setStringToBytes(true);
		byte[] bArray = (byte[]) mapper.fromMessage(message);
		assertThat(new String(bArray)).isEqualTo(s);

	}

	@Test
	public void testFromMessage() throws Exception {
		String s = "test";
		Message<String> message = MessageBuilder.withPayload(s).build();
		TcpMessageMapper mapper = new TcpMessageMapper();
		mapper.setStringToBytes(false);
		String out = (String) mapper.fromMessage(message);
		assertThat(out).isEqualTo(s);

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
		assertThat(new String(baos.toByteArray(), "UTF-8"))
				.isEqualTo("{\"headers\":{\"bar\":\"baz\"},\"payload\":\"foo\"}\n");
	}

	@Test
	public void testMapMessageConvertingInboundJson() throws Exception {
		String json = "{\"headers\":{\"bar\":\"baz\"},\"payload\":\"foo\"}\n";
		MapMessageConverter converter = new MapMessageConverter();
		MessageConvertingTcpMessageMapper mapper = new MessageConvertingTcpMessageMapper(converter);
		MapJsonSerializer deserializer = new MapJsonSerializer();
		Map<?, ?> map = deserializer.deserialize(new ByteArrayInputStream(json.getBytes("UTF-8")));

		TcpConnection connection = creatMockTcpConcnection(map, "someHost", "1.1.1.1", 1234);
		when(connection.getConnectionId()).thenReturn("someId");
		Message<?> message = mapper.toMessage(connection);
		assertThat(message.getPayload()).isEqualTo("foo");
		assertThat(message.getHeaders().get("bar")).isEqualTo("baz");
		assertThat(message.getHeaders().get(IpHeaders.HOSTNAME)).isEqualTo("someHost");
		assertThat(message.getHeaders().get(IpHeaders.IP_ADDRESS)).isEqualTo("1.1.1.1");
		assertThat(message.getHeaders().get(IpHeaders.REMOTE_PORT)).isEqualTo(1234);
		assertThat(message.getHeaders().get(IpHeaders.CONNECTION_ID)).isEqualTo("someId");
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
		TcpConnection connection = creatMockTcpConcnection(map, "someHost", "1.1.1.1", 1234);
		when(connection.getConnectionId()).thenReturn("someId");
		Message<?> message = mapper.toMessage(connection);
		assertThat(message.getPayload()).isEqualTo("foo");
		assertThat(message.getHeaders().get("bar")).isEqualTo("baz");
		assertThat(message.getHeaders().get(IpHeaders.HOSTNAME)).isEqualTo("someHost");
		assertThat(message.getHeaders().get(IpHeaders.IP_ADDRESS)).isEqualTo("1.1.1.1");
		assertThat(message.getHeaders().get(IpHeaders.REMOTE_PORT)).isEqualTo(1234);
		assertThat(message.getHeaders().get(IpHeaders.CONNECTION_ID)).isEqualTo("someId");
	}

	@Test
	public void testCodecMessageConvertingBothWaysJava() {
		Message<String> outMessage = MessageBuilder.withPayload("foo")
				.setHeader("bar", "baz")
				.build();
		MessageConverter converter = new CodecMessageConverter(this.codec);
		MessageConvertingTcpMessageMapper mapper = new MessageConvertingTcpMessageMapper(converter);
		byte[] bytes = (byte[]) mapper.fromMessage(outMessage);

		TcpConnection connection = creatMockTcpConcnection(bytes, "someHost", "1.1.1.1", 1234);
		when(connection.getConnectionId()).thenReturn("someId");
		Message<?> message = mapper.toMessage(connection);
		assertThat(message.getPayload()).isEqualTo("foo");
		assertThat(message.getHeaders().get("bar")).isEqualTo("baz");
		assertThat(message.getHeaders().get(IpHeaders.HOSTNAME)).isEqualTo("someHost");
		assertThat(message.getHeaders().get(IpHeaders.IP_ADDRESS)).isEqualTo("1.1.1.1");
		assertThat(message.getHeaders().get(IpHeaders.REMOTE_PORT)).isEqualTo(1234);
		assertThat(message.getHeaders().get(IpHeaders.CONNECTION_ID)).isEqualTo("someId");
	}

	@Test
	public void testWithBytesMapper() {
		Message<String> outMessage = MessageBuilder.withPayload("foo")
				.setHeader("bar", "baz")
				.build();
		TcpMessageMapper mapper = new TcpMessageMapper();
		mapper.setBytesMessageMapper(new EmbeddedJsonHeadersMessageMapper());
		byte[] bytes = (byte[]) mapper.fromMessage(outMessage);

		TcpConnection connection = creatMockTcpConcnection(bytes, "someHost", "1.1.1.1", 1234);
		when(connection.getConnectionId()).thenReturn("someId");
		Message<?> message = mapper.toMessage(connection);
		assertThat(message.getPayload()).isEqualTo("foo");
		assertThat(message.getHeaders().get("bar")).isEqualTo("baz");
		assertThat(message.getHeaders().get(IpHeaders.HOSTNAME)).isEqualTo("someHost");
		assertThat(message.getHeaders().get(IpHeaders.IP_ADDRESS)).isEqualTo("1.1.1.1");
		assertThat(message.getHeaders().get(IpHeaders.REMOTE_PORT)).isEqualTo(1234);
		assertThat(message.getHeaders().get(IpHeaders.CONNECTION_ID)).isEqualTo("someId");
	}

	private static TcpConnection creatMockTcpConcnection(Object bytes, String hostName, String ipAdress, int port) {
		TcpConnection connection = mock(TcpConnection.class);
		when(connection.getPayload()).thenReturn(bytes);
		when(connection.getHostName()).thenReturn(hostName);
		when(connection.getHostAddress()).thenReturn(ipAdress);
		when(connection.getPort()).thenReturn(port);
		return connection;
	}

	private static Socket creatMockSocket(InetAddress local) {
		Socket socket = mock(Socket.class);
		when(socket.getLocalAddress()).thenReturn(local);
		return socket;
	}

}
