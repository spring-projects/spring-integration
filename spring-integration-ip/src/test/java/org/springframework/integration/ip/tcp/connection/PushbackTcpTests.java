/*
 * Copyright 2017-2020 the original author or authors.
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
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.time.Duration;
import java.util.Collections;

import org.junit.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.serializer.Deserializer;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.tcp.TcpInboundGateway;
import org.springframework.integration.ip.tcp.TcpOutboundGateway;
import org.springframework.integration.ip.tcp.serializer.ByteArrayCrLfSerializer;
import org.springframework.integration.ip.tcp.serializer.ByteArrayStxEtxSerializer;
import org.springframework.integration.ip.tcp.serializer.SoftEndOfStreamException;
import org.springframework.integration.transformer.ObjectToStringTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Gary Russell
 * @since 5.0
 *
 */
public class PushbackTcpTests {

	private static final String PORT = "PushbackTcpTests.port";

	@Test
	public void testPushbackNet() throws Exception {
		AnnotationConfigApplicationContext server = new AnnotationConfigApplicationContext(ServerNet.class);
		TcpNetServerConnectionFactory serverCF = server.getBean(TcpNetServerConnectionFactory.class);
		int port = waitForPort(serverCF);
		System.setProperty(PORT, String.valueOf(port));
		AnnotationConfigApplicationContext client = new AnnotationConfigApplicationContext(ClientNet.class);
		MessageChannel channel1 = client.getBean("out1", MessageChannel.class); // crlf
		MessageChannel channel2 = client.getBean("out2", MessageChannel.class); // stxetx
		QueueChannel replies = new QueueChannel();
		Message<?> message = new GenericMessage<>("foo",
				Collections.singletonMap(MessageHeaders.REPLY_CHANNEL, replies));
		channel1.send(message);
		channel2.send(message);
		assertThat(replies.getQueueSize()).isEqualTo(2);
		Message<?> replyA = replies.receive(0);
		Message<?> replyB = replies.receive(0);
		assertThat((String) replyA.getPayload()).contains("ip_connectionId:pushback:");
		assertThat(replyB.getPayload()).isNotEqualTo(replyA.getPayload());
		CompositeDeserializer deserializer = server.getBean(CompositeDeserializer.class);
		assertThat(deserializer.receivedCrLf).isTrue();
		assertThat(deserializer.receivedStxEtx).isTrue();
		System.getProperties().remove(PORT);
		client.close();
		server.close();
	}

	@Test
	public void testPushbackNio() throws Exception {
		AnnotationConfigApplicationContext server = new AnnotationConfigApplicationContext(ServerNio.class);
		TcpNioServerConnectionFactory serverCF = server.getBean(TcpNioServerConnectionFactory.class);
		int port = waitForPort(serverCF);
		System.setProperty(PORT, String.valueOf(port));
		AnnotationConfigApplicationContext client = new AnnotationConfigApplicationContext(ClientNio.class);
		MessageChannel channel1 = client.getBean("out1", MessageChannel.class); // crlf
		MessageChannel channel2 = client.getBean("out2", MessageChannel.class); // stxetx
		QueueChannel replies = new QueueChannel();
		Message<?> message = new GenericMessage<>("foo",
				Collections.singletonMap(MessageHeaders.REPLY_CHANNEL, replies));
		channel1.send(message);
		channel2.send(message);
		assertThat(replies.getQueueSize()).isEqualTo(2);
		Message<?> replyA = replies.receive(0);
		Message<?> replyB = replies.receive(0);
		assertThat((String) replyA.getPayload()).contains("ip_connectionId:pushback:");
		assertThat(replyB.getPayload()).isNotEqualTo(replyA.getPayload());
		CompositeDeserializer deserializer = server.getBean(CompositeDeserializer.class);
		assertThat(deserializer.receivedCrLf).isTrue();
		assertThat(deserializer.receivedStxEtx).isTrue();
		System.getProperties().remove(PORT);
		client.close();
		server.close();
	}

	@Test
	public void testPushbackNioSSL() throws Exception {
		AnnotationConfigApplicationContext server = new AnnotationConfigApplicationContext(ServerNioSSL.class,
				SSLConfig.class);
		TcpNioServerConnectionFactory serverCF = server.getBean(TcpNioServerConnectionFactory.class);
		int port = waitForPort(serverCF);
		System.setProperty(PORT, String.valueOf(port));
		AnnotationConfigApplicationContext client = new AnnotationConfigApplicationContext(ClientNioSSL.class,
				SSLConfig.class);
		MessageChannel channel1 = client.getBean("out1", MessageChannel.class); // crlf
		MessageChannel channel2 = client.getBean("out2", MessageChannel.class); // stxetx
		QueueChannel replies = new QueueChannel();
		Message<?> message = new GenericMessage<>("foo",
				Collections.singletonMap(MessageHeaders.REPLY_CHANNEL, replies));
		channel1.send(message);
		channel2.send(message);
		assertThat(replies.getQueueSize()).isEqualTo(2);
		Message<?> replyA = replies.receive(0);
		Message<?> replyB = replies.receive(0);
		assertThat((String) replyA.getPayload()).contains("ip_connectionId:pushback:");
		assertThat(replyB.getPayload()).isNotEqualTo(replyA.getPayload());
		CompositeDeserializer deserializer = server.getBean(CompositeDeserializer.class);
		assertThat(deserializer.receivedCrLf).isTrue();
		assertThat(deserializer.receivedStxEtx).isTrue();
		System.getProperties().remove(PORT);
		client.close();
		server.close();
	}

	private int waitForPort(AbstractServerConnectionFactory serverCF) throws InterruptedException {
		await().atMost(Duration.ofSeconds(20)).until(() -> serverCF.getPort() > 0);
		return serverCF.getPort();
	}

	@Configuration
	@EnableIntegration
	public static class ServerNet {

		@Bean
		public TcpNetServerConnectionFactory sf() {
			TcpNetServerConnectionFactory server = new TcpNetServerConnectionFactory(0);
			server.setDeserializer(deserializer());
			DefaultTcpNetConnectionSupport connectionSupport = new DefaultTcpNetConnectionSupport();
			connectionSupport.setPushbackCapable(true);
			server.setTcpNetConnectionSupport(connectionSupport);
			return server;
		}

		@Bean
		public CompositeDeserializer deserializer() {
			return new CompositeDeserializer();
		}

		@Bean
		public TcpInboundGateway inGate() {
			TcpInboundGateway inGate = new TcpInboundGateway();
			inGate.setConnectionFactory(sf());
			inGate.setRequestChannelName("in");
			return inGate;
		}

		@ServiceActivator(inputChannel = "in")
		public String handle(Message<?> message) {
			return IpHeaders.CONNECTION_ID + ":" + (String) message.getHeaders().get(IpHeaders.CONNECTION_ID);
		}

	}

	@Configuration
	@EnableIntegration
	public static class ClientNet {

		@Bean
		public TcpNetClientConnectionFactory cf1() {
			TcpNetClientConnectionFactory cf = new TcpNetClientConnectionFactory("localhost",
					Integer.parseInt(System.getProperty(PORT)));
			cf.setSingleUse(true);
			return cf;
		}

		@Bean
		@ServiceActivator(inputChannel = "out1")
		public TcpOutboundGateway outGate1() {
			TcpOutboundGateway outGate = new TcpOutboundGateway();
			outGate.setConnectionFactory(cf1());
			outGate.setReplyChannelName("toString");
			return outGate;
		}

		@Bean
		public TcpNetClientConnectionFactory cf2() {
			TcpNetClientConnectionFactory cf = new TcpNetClientConnectionFactory("localhost",
					Integer.parseInt(System.getProperty(PORT)));
			cf.setSerializer(new ByteArrayStxEtxSerializer());
			return cf;
		}

		@Bean
		@ServiceActivator(inputChannel = "out2")
		public TcpOutboundGateway outGate2() {
			TcpOutboundGateway outGate = new TcpOutboundGateway();
			outGate.setConnectionFactory(cf2());
			outGate.setReplyChannelName("toString");
			return outGate;
		}

		@Bean
		@Transformer(inputChannel = "toString")
		public ObjectToStringTransformer otst() {
			return new ObjectToStringTransformer();
		}

	}

	@Configuration
	@EnableIntegration
	public static class ServerNio {

		@Bean
		public TcpNioServerConnectionFactory sf() {
			TcpNioServerConnectionFactory server = new TcpNioServerConnectionFactory(0);
			server.setDeserializer(deserializer());
			DefaultTcpNioConnectionSupport connectionSupport = new DefaultTcpNioConnectionSupport();
			connectionSupport.setPushbackCapable(true);
			server.setTcpNioConnectionSupport(connectionSupport);
			return server;
		}

		@Bean
		public CompositeDeserializer deserializer() {
			return new CompositeDeserializer();
		}

		@Bean
		public TcpInboundGateway inGate() {
			TcpInboundGateway inGate = new TcpInboundGateway();
			inGate.setConnectionFactory(sf());
			inGate.setRequestChannelName("in");
			return inGate;
		}

		@ServiceActivator(inputChannel = "in")
		public String handle(Message<?> message) {
			return IpHeaders.CONNECTION_ID + ":" + (String) message.getHeaders().get(IpHeaders.CONNECTION_ID);
		}

	}

	@Configuration
	@EnableIntegration
	public static class ClientNio {

		@Bean
		public TcpNioClientConnectionFactory cf1() {
			TcpNioClientConnectionFactory cf = new TcpNioClientConnectionFactory("localhost",
					Integer.parseInt(System.getProperty(PORT)));
			cf.setSingleUse(true);
			return cf;
		}

		@Bean
		@ServiceActivator(inputChannel = "out1")
		public TcpOutboundGateway outGate1() {
			TcpOutboundGateway outGate = new TcpOutboundGateway();
			outGate.setConnectionFactory(cf1());
			outGate.setRemoteTimeout(50000);
			outGate.setReplyChannelName("toString");
			return outGate;
		}

		@Bean
		public TcpNioClientConnectionFactory cf2() {
			TcpNioClientConnectionFactory cf = new TcpNioClientConnectionFactory("localhost",
					Integer.parseInt(System.getProperty(PORT)));
			cf.setSerializer(new ByteArrayStxEtxSerializer());
			return cf;
		}

		@Bean
		@ServiceActivator(inputChannel = "out2")
		public TcpOutboundGateway outGate2() {
			TcpOutboundGateway outGate = new TcpOutboundGateway();
			outGate.setConnectionFactory(cf2());
			outGate.setReplyChannelName("toString");
			return outGate;
		}

		@Bean
		@Transformer(inputChannel = "toString")
		public ObjectToStringTransformer otst() {
			return new ObjectToStringTransformer();
		}

	}

	@Configuration
	@EnableIntegration
	public static class ServerNioSSL {

		@Bean
		public TcpNioServerConnectionFactory sf(DefaultTcpNioSSLConnectionSupport sslNioSupport) {
			TcpNioServerConnectionFactory server = new TcpNioServerConnectionFactory(0);
			server.setDeserializer(deserializer());
			server.setTcpNioConnectionSupport(sslNioSupport);
			return server;
		}

		@Bean
		public CompositeDeserializer deserializer() {
			return new CompositeDeserializer();
		}

		@Bean
		public TcpInboundGateway inGate(TcpNioServerConnectionFactory sf) {
			TcpInboundGateway inGate = new TcpInboundGateway();
			inGate.setConnectionFactory(sf);
			inGate.setRequestChannelName("in");
			return inGate;
		}

		@ServiceActivator(inputChannel = "in")
		public String handle(Message<?> message) {
			return IpHeaders.CONNECTION_ID + ":" + (String) message.getHeaders().get(IpHeaders.CONNECTION_ID);
		}

	}

	@Configuration
	@EnableIntegration
	public static class ClientNioSSL {

		@Bean
		public TcpNioClientConnectionFactory cf1(DefaultTcpNioSSLConnectionSupport sslNioSupport) {
			TcpNioClientConnectionFactory cf = new TcpNioClientConnectionFactory("localhost",
					Integer.parseInt(System.getProperty(PORT)));
			cf.setTcpNioConnectionSupport(sslNioSupport);
			return cf;
		}

		@Bean
		@ServiceActivator(inputChannel = "out1")
		public TcpOutboundGateway outGate1(DefaultTcpNioSSLConnectionSupport sslNioSupport) {
			TcpOutboundGateway outGate = new TcpOutboundGateway();
			outGate.setConnectionFactory(cf1(sslNioSupport));
			outGate.setReplyChannelName("toString");
			return outGate;
		}

		@Bean
		public TcpNioClientConnectionFactory cf2(DefaultTcpNioSSLConnectionSupport sslNioSupport) {
			TcpNioClientConnectionFactory cf = new TcpNioClientConnectionFactory("localhost",
					Integer.parseInt(System.getProperty(PORT)));
			cf.setSerializer(new ByteArrayStxEtxSerializer());
			cf.setTcpNioConnectionSupport(sslNioSupport);
			return cf;
		}

		@Bean
		@ServiceActivator(inputChannel = "out2")
		public TcpOutboundGateway outGate2(DefaultTcpNioSSLConnectionSupport sslNioSupport) {
			TcpOutboundGateway outGate = new TcpOutboundGateway();
			outGate.setConnectionFactory(cf2(sslNioSupport));
			outGate.setReplyChannelName("toString");
			return outGate;
		}

		@Bean
		@Transformer(inputChannel = "toString")
		public ObjectToStringTransformer otst() {
			return new ObjectToStringTransformer();
		}

	}

	@Configuration
	public static class SSLConfig {

		@Bean
		public DefaultTcpNioSSLConnectionSupport connectionSupport() {
			DefaultTcpSSLContextSupport sslContextSupport = new DefaultTcpSSLContextSupport("test.ks",
					"test.truststore.ks", "secret", "secret");
			sslContextSupport.setProtocol("SSL");
			DefaultTcpNioSSLConnectionSupport tcpNioConnectionSupport =
					new DefaultTcpNioSSLConnectionSupport(sslContextSupport, false);
			tcpNioConnectionSupport.setPushbackCapable(true);
			return tcpNioConnectionSupport;
		}

	}

	private static class CompositeDeserializer implements Deserializer<byte[]> {

		private final ByteArrayStxEtxSerializer stxEtx = new ByteArrayStxEtxSerializer();

		private final ByteArrayCrLfSerializer crlf = new ByteArrayCrLfSerializer();

		private volatile boolean receivedStxEtx;

		private volatile boolean receivedCrLf;

		CompositeDeserializer() {
			super();
		}

		@Override
		public byte[] deserialize(InputStream inputStream) throws IOException {
			PushbackInputStream pbis = (PushbackInputStream) inputStream;
			int first = pbis.read();
			if (first < 0) {
				throw new SoftEndOfStreamException();
			}
			pbis.unread(first);
			if (first == ByteArrayStxEtxSerializer.STX) {
				this.receivedStxEtx = true;
				return this.stxEtx.deserialize(pbis);
			}
			else {
				this.receivedCrLf = true;
				return this.crlf.deserialize(pbis);
			}
		}

	}

}
