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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLServerSocket;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.integration.ip.tcp.serializer.ByteArrayCrLfSerializer;
import org.springframework.integration.ip.util.TestingUtilities;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.2
 */
public class SocketSupportTests {

	@Test
	public void testNetClient() throws Exception {
		TcpSocketFactorySupport factorySupport = mock(TcpSocketFactorySupport.class);
		SocketFactory factory = Mockito.mock(SocketFactory.class);
		when(factorySupport.getSocketFactory()).thenReturn(factory);
		Socket socket = mock(Socket.class);
		InputStream is = mock(InputStream.class);
		when(is.read()).thenReturn(-1);
		when(socket.getInputStream()).thenReturn(is);
		InetAddress inetAddress = InetAddress.getLocalHost();
		when(socket.getInetAddress()).thenReturn(inetAddress);
		when(factory.createSocket()).thenReturn(socket);
		TcpSocketSupport socketSupport = Mockito.mock(TcpSocketSupport.class);

		TcpNetClientConnectionFactory connectionFactory = new TcpNetClientConnectionFactory("x", 0);
		connectionFactory.setTcpSocketFactorySupport(factorySupport);
		connectionFactory.setTcpSocketSupport(socketSupport);
		connectionFactory.start();
		connectionFactory.getConnection();

		verify(socketSupport).postProcessSocket(socket);
		connectionFactory.stop();
	}

	@Test
	public void testNetClientSocketTimeout() throws Exception {
		TcpSocketFactorySupport factorySupport = mock(TcpSocketFactorySupport.class);
		SocketFactory factory = Mockito.mock(SocketFactory.class);
		when(factorySupport.getSocketFactory()).thenReturn(factory);
		Socket socket = mock(Socket.class);
		InputStream is = mock(InputStream.class);
		when(is.read()).thenReturn(-1);
		when(socket.getInputStream()).thenReturn(is);
		InetAddress inetAddress = InetAddress.getLocalHost();
		when(socket.getInetAddress()).thenReturn(inetAddress);
		when(factory.createSocket()).thenReturn(socket);
		doThrow(new SocketTimeoutException()).when(socket).connect(any(), eq(1000));
		TcpSocketSupport socketSupport = Mockito.mock(TcpSocketSupport.class);

		TcpNetClientConnectionFactory connectionFactory = new TcpNetClientConnectionFactory("x", 0);
		connectionFactory.setConnectTimeout(1);
		connectionFactory.setTcpSocketFactorySupport(factorySupport);
		connectionFactory.setTcpSocketSupport(socketSupport);
		connectionFactory.start();
		assertThatThrownBy(() -> connectionFactory.getConnection())
			.isInstanceOf(UncheckedIOException.class)
			.hasCauseInstanceOf(SocketTimeoutException.class);

		connectionFactory.stop();
	}

	@Test
	public void testNetServer() throws Exception {
		TcpSocketFactorySupport factorySupport = mock(TcpSocketFactorySupport.class);
		ServerSocketFactory factory = mock(ServerSocketFactory.class);
		when(factorySupport.getServerSocketFactory()).thenReturn(factory);
		Socket socket = mock(Socket.class);
		Socket socket1 = mock(Socket.class);
		InputStream is = mock(InputStream.class);
		when(is.read()).thenReturn(-1);
		when(socket.getInputStream()).thenReturn(is);
		when(socket1.getInputStream()).thenReturn(is);
		InetAddress inetAddress = InetAddress.getLocalHost();
		when(socket.getInetAddress()).thenReturn(inetAddress);
		when(socket1.getInetAddress()).thenReturn(inetAddress);
		ServerSocket serverSocket = mock(ServerSocket.class);
		AtomicBoolean closed = new AtomicBoolean();
		doAnswer(invoc -> {
			closed.set(true);
			return null;
		}).when(serverSocket).close();
		when(serverSocket.getInetAddress()).thenReturn(inetAddress);
		when(factory.createServerSocket(0, 5)).thenReturn(serverSocket);
		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);
		when(serverSocket.accept()).thenReturn(socket).then(invocation -> {
			if (closed.get()) {
				throw new SocketException();
			}
			latch1.countDown();
			latch2.await(10, TimeUnit.SECONDS);
			Thread.sleep(50);
			return socket1;
		});
		TcpSocketSupport socketSupport = mock(TcpSocketSupport.class);

		TcpNetServerConnectionFactory connectionFactory = new TcpNetServerConnectionFactory(0);
		connectionFactory.setTcpSocketFactorySupport(factorySupport);
		connectionFactory.setTcpSocketSupport(socketSupport);
		connectionFactory.registerListener(mock(TcpListener.class));
		connectionFactory.start();

		assertThat(latch1.await(10, TimeUnit.SECONDS)).isTrue();
		verify(socketSupport).postProcessServerSocket(serverSocket);
		verify(socketSupport).postProcessSocket(socket);
		latch2.countDown();
		connectionFactory.stop();
	}

	@Test
	public void testNioClientAndServer() throws Exception {
		TcpNioServerConnectionFactory serverConnectionFactory = new TcpNioServerConnectionFactory(0);
		serverConnectionFactory.registerListener(message -> false);
		final AtomicInteger ppSocketCountServer = new AtomicInteger();
		final AtomicInteger ppServerSocketCountServer = new AtomicInteger();
		final CountDownLatch latch = new CountDownLatch(1);
		TcpSocketSupport serverSocketSupport = new TcpSocketSupport() {

			@Override
			public void postProcessSocket(Socket socket) {
				ppSocketCountServer.incrementAndGet();
				latch.countDown();
			}

			@Override
			public void postProcessServerSocket(ServerSocket serverSocket) {
				ppServerSocketCountServer.incrementAndGet();
			}

		};
		serverConnectionFactory.setTcpSocketSupport(serverSocketSupport);
		serverConnectionFactory.start();
		TestingUtilities.waitListening(serverConnectionFactory, null);

		TcpNioClientConnectionFactory clientConnectionFactory = new TcpNioClientConnectionFactory("localhost",
				serverConnectionFactory.getPort());
		final AtomicInteger ppSocketCountClient = new AtomicInteger();
		final AtomicInteger ppServerSocketCountClient = new AtomicInteger();
		TcpSocketSupport clientSocketSupport = new TcpSocketSupport() {

			@Override
			public void postProcessSocket(Socket socket) {
				ppSocketCountClient.incrementAndGet();
			}

			@Override
			public void postProcessServerSocket(ServerSocket serverSocket) {
				ppServerSocketCountClient.incrementAndGet();
			}

		};
		clientConnectionFactory.setTcpSocketSupport(clientSocketSupport);
		clientConnectionFactory.start();
		clientConnectionFactory.getConnection().send(new GenericMessage<String>("Hello, world!"));
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(ppServerSocketCountClient.get()).isEqualTo(0);
		assertThat(ppSocketCountClient.get()).isEqualTo(1);

		assertThat(ppServerSocketCountServer.get()).isEqualTo(1);
		assertThat(ppSocketCountServer.get()).isEqualTo(1);

		clientConnectionFactory.stop();
		serverConnectionFactory.stop();
	}

	/*
	$ keytool -genkeypair -alias sitestcertkey -keyalg RSA -validity 36500 -keystore src/test/resources/test.ks -ext
	san=dns:localhost
	Enter keystore password: secret
	Re-enter new password: secret
	What is your first and last name?
	  [Unknown]:  Spring Integration
	What is the name of your organizational unit?
	  [Unknown]:  Spring
	What is the name of your organization?
	  [Unknown]:  Pivotal Software Inc.
	What is the name of your City or Locality?
	  [Unknown]:  San Francisco
	What is the name of your State or Province?
	  [Unknown]:  CA
	What is the two-letter country code for this unit?
	  [Unknown]:  US
	Is CN=Spring Integration, OU=Spring, O=Pivotal Software Inc., L=San Francisco, ST=CA, C=US correct?
	  [no]:  yes

	Enter key password for <sitestcertkey>
		(RETURN if same as keystore password):

	$ keytool -list -v -keystore src/test/resources/test.ks
	Enter keystore password: secret

	Keystore type: JKS
	Keystore provider: SUN

	Your keystore contains 1 entry

	Alias name: sitestcertkey
	Creation date: Aug 29, 2018
	Entry type: PrivateKeyEntry
	Certificate chain length: 1
	Certificate[1]:
	Owner: CN=Spring Integration, OU=Spring, O=Pivotal Software Inc., L=San Francisco, ST=CA, C=US
	Issuer: CN=Spring Integration, OU=Spring, O=Pivotal Software Inc., L=San Francisco, ST=CA, C=US
	Serial number: 3f2ab6ef
	Valid from: Wed Aug 29 14:58:27 EDT 2018 until: Fri Aug 05 14:58:27 EDT 2118
	Certificate fingerprints:
		 MD5:  74:14:93:3C:6E:7B:14:59:30:A3:90:C4:A2:AD:52:5E
		 SHA1: 12:BE:77:93:ED:C3:20:23:75:D7:D5:D9:FE:D9:5E:D1:D3:3E:E2:DC
		 SHA256: 6B:90:65:8D:AA:F6:F3:89:38:AE:92:8E:F0:83:26:17:DD:8A:2C:F6:7E:C5:39:F0:7E:DC:60:A3:6D:73:E1:7A
	Signature algorithm name: SHA256withRSA
	Subject Public Key Algorithm: 2048-bit RSA key
	Version: 3

	Extensions:

	#1: ObjectId: 2.5.29.17 Criticality=false
	SubjectAlternativeName [
	  DNSName: localhost
	]

	#2: ObjectId: 2.5.29.14 Criticality=false
	SubjectKeyIdentifier [
	KeyIdentifier [
	0000: 78 2D FA 48 D8 21 73 86   68 CE 77 B9 98 5A BA 0F  x-.H.!s.h.w..Z..
	0010: E2 FE CD 8C                                        ....
	]
	]



	*******************************************
	*******************************************


	$ keytool -export -alias sitestcertkey -keystore src/test/resources/test.ks -rfc -file src/test/resources/test.cer
	Enter keystore password:
	Certificate stored in file <src/test/resources/test.cer>

	$ keytool -import -alias sitestcertkey -file src/test/resources/test.cer -keystore src/test/resources/test
	.truststore.ks
	Enter keystore password: secret
	Re-enter new password: secret
	Owner: CN=Spring Integration, OU=Spring, O=Pivotal Software Inc., L=San Francisco, ST=CA, C=US
	Issuer: CN=Spring Integration, OU=Spring, O=Pivotal Software Inc., L=San Francisco, ST=CA, C=US
	Serial number: 3f2ab6ef
	Valid from: Wed Aug 29 14:58:27 EDT 2018 until: Fri Aug 05 14:58:27 EDT 2118
	Certificate fingerprints:
		 MD5:  74:14:93:3C:6E:7B:14:59:30:A3:90:C4:A2:AD:52:5E
		 SHA1: 12:BE:77:93:ED:C3:20:23:75:D7:D5:D9:FE:D9:5E:D1:D3:3E:E2:DC
		 SHA256: 6B:90:65:8D:AA:F6:F3:89:38:AE:92:8E:F0:83:26:17:DD:8A:2C:F6:7E:C5:39:F0:7E:DC:60:A3:6D:73:E1:7A
	Signature algorithm name: SHA256withRSA
	Subject Public Key Algorithm: 2048-bit RSA key
	Version: 3

	Extensions:

	#1: ObjectId: 2.5.29.17 Criticality=false
	SubjectAlternativeName [
	  DNSName: localhost
	]

	#2: ObjectId: 2.5.29.14 Criticality=false
	SubjectKeyIdentifier [
	KeyIdentifier [
	0000: 78 2D FA 48 D8 21 73 86   68 CE 77 B9 98 5A BA 0F  x-.H.!s.h.w..Z..
	0010: E2 FE CD 8C                                        ....
	]
	]

	Trust this certificate? [no]:  yes
	Certificate was added to keystore

	$ keytool -list -v -keystore src/test/resources/test.truststore.ks
	Enter keystore password: secret

	Keystore type: JKS
	Keystore provider: SUN

	Your keystore contains 1 entry

	Alias name: sitestcertkey
	Creation date: Aug 29, 2018
	Entry type: trustedCertEntry

	Owner: CN=Spring Integration, OU=Spring, O=Pivotal Software Inc., L=San Francisco, ST=CA, C=US
	Issuer: CN=Spring Integration, OU=Spring, O=Pivotal Software Inc., L=San Francisco, ST=CA, C=US
	Serial number: 3f2ab6ef
	Valid from: Wed Aug 29 14:58:27 EDT 2018 until: Fri Aug 05 14:58:27 EDT 2118
	Certificate fingerprints:
		 MD5:  74:14:93:3C:6E:7B:14:59:30:A3:90:C4:A2:AD:52:5E
		 SHA1: 12:BE:77:93:ED:C3:20:23:75:D7:D5:D9:FE:D9:5E:D1:D3:3E:E2:DC
		 SHA256: 6B:90:65:8D:AA:F6:F3:89:38:AE:92:8E:F0:83:26:17:DD:8A:2C:F6:7E:C5:39:F0:7E:DC:60:A3:6D:73:E1:7A
	Signature algorithm name: SHA256withRSA
	Subject Public Key Algorithm: 2048-bit RSA key
	Version: 3

	Extensions:

	#1: ObjectId: 2.5.29.17 Criticality=false
	SubjectAlternativeName [
	  DNSName: localhost
	]

	#2: ObjectId: 2.5.29.14 Criticality=false
	SubjectKeyIdentifier [
	KeyIdentifier [
	0000: 78 2D FA 48 D8 21 73 86   68 CE 77 B9 98 5A BA 0F  x-.H.!s.h.w..Z..
	0010: E2 FE CD 8C                                        ....
	]
	]



	*******************************************
	*******************************************
	*/
	@Test
	public void testNetClientAndServerSSL() throws Exception {
		System.setProperty("javax.net.debug", "all"); // SSL activity in the console
		TcpNetServerConnectionFactory server = new TcpNetServerConnectionFactory(0);
		TcpSSLContextSupport sslContextSupport = new DefaultTcpSSLContextSupport("test.ks",
				"test.truststore.ks", "secret", "secret");
		DefaultTcpNetSSLSocketFactorySupport tcpSocketFactorySupport =
				new DefaultTcpNetSSLSocketFactorySupport(sslContextSupport);
		server.setTcpSocketFactorySupport(tcpSocketFactorySupport);
		final List<Message<?>> messages = new ArrayList<Message<?>>();
		final CountDownLatch latch = new CountDownLatch(1);
		server.registerListener(message -> {
			messages.add(message);
			latch.countDown();
			return false;
		});
		server.setMapper(new SSLMapper());
		server.start();
		TestingUtilities.waitListening(server, null);

		TcpNetClientConnectionFactory client = new TcpNetClientConnectionFactory("localhost", server.getPort());
		client.setTcpSocketFactorySupport(tcpSocketFactorySupport);
		client.setTcpSocketSupport(new DefaultTcpSocketSupport(true));
		client.start();

		TcpConnection connection = client.getConnection();
		connection.send(new GenericMessage<String>("Hello, world!"));
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(new String((byte[]) messages.get(0).getPayload())).isEqualTo("Hello, world!");
		assertThat(messages.get(0).getHeaders().get("cipher")).isNotNull();

		client.stop();
		server.stop();
	}

	@Test
	public void testNetClientAndServerSSLDifferentContexts() throws Exception {
		testNetClientAndServerSSLDifferentContexts(false);
		assertThatExceptionOfType(MessagingException.class)
			.isThrownBy(() -> testNetClientAndServerSSLDifferentContexts(true));
	}

	private void testNetClientAndServerSSLDifferentContexts(boolean badServer) throws Exception {
		System.setProperty("javax.net.debug", "all"); // SSL activity in the console
		TcpNetServerConnectionFactory server = new TcpNetServerConnectionFactory(0);
		TcpSSLContextSupport serverSslContextSupport = new DefaultTcpSSLContextSupport(
				badServer ? "client.ks" : "server.ks",
				"server.truststore.ks", "secret", "secret");
		DefaultTcpNetSSLSocketFactorySupport serverTcpSocketFactorySupport =
				new DefaultTcpNetSSLSocketFactorySupport(serverSslContextSupport);
		server.setTcpSocketFactorySupport(serverTcpSocketFactorySupport);
		final List<Message<?>> messages = new ArrayList<Message<?>>();
		final CountDownLatch latch = new CountDownLatch(1);
		server.registerListener(message -> {
			if (!(message instanceof ErrorMessage)) {
				messages.add(message);
				latch.countDown();
			}
			return false;
		});
		server.setTcpSocketSupport(new DefaultTcpSocketSupport(false) {

			@Override
			public void postProcessServerSocket(ServerSocket serverSocket) {
				((SSLServerSocket) serverSocket).setNeedClientAuth(true);
			}

		});
		server.start();
		TestingUtilities.waitListening(server, null);

		TcpNetClientConnectionFactory client = new TcpNetClientConnectionFactory("localhost", server.getPort());
		TcpSSLContextSupport clientSslContextSupport = new DefaultTcpSSLContextSupport("client.ks",
				"client.truststore.ks", "secret", "secret");
		DefaultTcpNetSSLSocketFactorySupport clientTcpSocketFactorySupport =
				new DefaultTcpNetSSLSocketFactorySupport(clientSslContextSupport);
		client.setTcpSocketFactorySupport(clientTcpSocketFactorySupport);
		client.setTcpSocketSupport(new DefaultTcpSocketSupport(false));

		try {
			client.start();
			TcpConnection connection = client.getConnection();
			connection.send(new GenericMessage<String>("Hello, world!"));
			assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
			assertThat(new String((byte[]) messages.get(0).getPayload())).isEqualTo("Hello, world!");
		}
		finally {
			client.stop();
			server.stop();
		}
	}

	@Test
	public void testNioClientAndServerSSL() throws Exception {
		System.setProperty("javax.net.debug", "all"); // SSL activity in the console
		TcpNioServerConnectionFactory server = new TcpNioServerConnectionFactory(0);
		server.setSslHandshakeTimeout(43);
		DefaultTcpSSLContextSupport sslContextSupport = new DefaultTcpSSLContextSupport("test.ks",
				"test.truststore.ks", "secret", "secret");
		sslContextSupport.setProtocol("SSL");
		DefaultTcpNioSSLConnectionSupport tcpNioConnectionSupport =
				new DefaultTcpNioSSLConnectionSupport(sslContextSupport, false);
		server.setTcpNioConnectionSupport(tcpNioConnectionSupport);
		final List<Message<?>> messages = new ArrayList<Message<?>>();
		final CountDownLatch latch = new CountDownLatch(1);
		server.registerListener(message -> {
			messages.add(message);
			latch.countDown();
			return false;
		});
		server.setMapper(new SSLMapper());
		final AtomicReference<String> serverConnectionId = new AtomicReference<>();
		server.setApplicationEventPublisher(e -> {
			if (e instanceof TcpConnectionOpenEvent) {
				serverConnectionId.set(((TcpConnectionEvent) e).getConnectionId());
			}
		});
		server.start();
		TestingUtilities.waitListening(server, null);

		TcpNioClientConnectionFactory client = new TcpNioClientConnectionFactory("localhost", server.getPort());
		client.setSslHandshakeTimeout(34);
		client.setTcpNioConnectionSupport(tcpNioConnectionSupport);
		client.registerListener(message -> false);
		client.setApplicationEventPublisher(e -> {
		});
		client.start();

		TcpConnection connection = client.getConnection();
		assertThat(TestUtils.getPropertyValue(connection, "handshakeTimeout")).isEqualTo(34);
		connection.send(new GenericMessage<String>("Hello, world!"));
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(new String((byte[]) messages.get(0).getPayload())).isEqualTo("Hello, world!");
		assertThat(messages.get(0).getHeaders().get("cipher")).isNotNull();

		Map<?, ?> connections = TestUtils.getPropertyValue(server, "connections", Map.class);
		Object serverConnection = connections.get(serverConnectionId.get());
		assertThat(serverConnection).isNotNull();
		assertThat(TestUtils.getPropertyValue(serverConnection, "handshakeTimeout")).isEqualTo(43);

		client.stop();
		server.stop();
	}

	@Test
	public void testNioClientAndServerSSLDifferentContexts() throws Exception {
		testNioClientAndServerSSLDifferentContexts(false);
		assertThatExceptionOfType(MessagingException.class)
			.isThrownBy(() -> testNioClientAndServerSSLDifferentContexts(true))
			.withMessageMatching(".*javax.net.ssl.SSLHandshakeException.*");
	}

	private void testNioClientAndServerSSLDifferentContexts(boolean badServer) throws Exception {
		System.setProperty("javax.net.debug", "all"); // SSL activity in the console
		TcpNioServerConnectionFactory server = new TcpNioServerConnectionFactory(0);
		TcpSSLContextSupport serverSslContextSupport = new DefaultTcpSSLContextSupport(
				badServer ? "client.ks" : "server.ks",
				"server.truststore.ks", "secret", "secret");
		DefaultTcpNioSSLConnectionSupport tcpNioConnectionSupport =
				new DefaultTcpNioSSLConnectionSupport(serverSslContextSupport, false) {

					@Override
					protected void postProcessSSLEngine(SSLEngine sslEngine) {
						sslEngine.setNeedClientAuth(true);
					}

				};
		server.setTcpNioConnectionSupport(tcpNioConnectionSupport);
		final List<Message<?>> messages = new ArrayList<Message<?>>();
		final CountDownLatch latch = new CountDownLatch(1);
		server.registerListener(message -> {
			messages.add(message);
			latch.countDown();
			return false;
		});
		server.start();
		TestingUtilities.waitListening(server, null);

		TcpNioClientConnectionFactory client = new TcpNioClientConnectionFactory("localhost", server.getPort());
		TcpSSLContextSupport clientSslContextSupport = new DefaultTcpSSLContextSupport("client.ks",
				"client.truststore.ks", "secret", "secret");
		DefaultTcpNioSSLConnectionSupport clientTcpNioConnectionSupport =
				new DefaultTcpNioSSLConnectionSupport(clientSslContextSupport, false);
		client.setTcpNioConnectionSupport(clientTcpNioConnectionSupport);

		try {
			client.start();
			TcpConnection connection = client.getConnection();
			connection.send(new GenericMessage<String>("Hello, world!"));
			assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
			assertThat(new String((byte[]) messages.get(0).getPayload())).isEqualTo("Hello, world!");
		}
		finally {
			client.stop();
			server.stop();
		}
	}

	@Test
	public void testNioClientAndServerSSLDifferentContextsLargeDataWithReply() throws Exception {
		System.setProperty("javax.net.debug", "all"); // SSL activity in the console
		TcpNioServerConnectionFactory server = new TcpNioServerConnectionFactory(0);
		TcpSSLContextSupport serverSslContextSupport = new DefaultTcpSSLContextSupport("server.ks",
				"server.truststore.ks", "secret", "secret");
		DefaultTcpNioSSLConnectionSupport serverTcpNioConnectionSupport =
				new DefaultTcpNioSSLConnectionSupport(serverSslContextSupport, false);
		server.setTcpNioConnectionSupport(serverTcpNioConnectionSupport);
		final List<Message<?>> messages = new ArrayList<Message<?>>();
		final CountDownLatch latch = new CountDownLatch(2);
		final Replier replier = new Replier();
		server.registerSender(replier);
		server.registerListener(message -> {
			messages.add(message);
			try {
				replier.send(message);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
			latch.countDown();
			return false;
		});
		ByteArrayCrLfSerializer deserializer = new ByteArrayCrLfSerializer();
		deserializer.setMaxMessageSize(120000);
		server.setDeserializer(deserializer);
		final AtomicReference<String> serverConnectionId = new AtomicReference<>();
		server.setApplicationEventPublisher(e -> {
			if (e instanceof TcpConnectionOpenEvent) {
				serverConnectionId.set(((TcpConnectionEvent) e).getConnectionId());
			}
		});
		server.start();
		TestingUtilities.waitListening(server, null);

		TcpNioClientConnectionFactory client = new TcpNioClientConnectionFactory("localhost", server.getPort());
		TcpSSLContextSupport clientSslContextSupport = new DefaultTcpSSLContextSupport("client.ks",
				"client.truststore.ks", "secret", "secret");
		DefaultTcpNioSSLConnectionSupport clientTcpNioConnectionSupport =
				new DefaultTcpNioSSLConnectionSupport(clientSslContextSupport, false);
		client.setTcpNioConnectionSupport(clientTcpNioConnectionSupport);
		client.registerListener(message -> {
			messages.add(message);
			latch.countDown();
			return false;
		});
		client.setDeserializer(deserializer);
		client.setApplicationEventPublisher(e -> {
		});
		client.start();

		TcpConnection connection = client.getConnection();
		assertThat(TestUtils.getPropertyValue(connection, "handshakeTimeout")).isEqualTo(30);
		byte[] bytes = new byte[100000];
		connection.send(new GenericMessage<String>("Hello, world!" + new String(bytes)));
		assertThat(latch.await(60, TimeUnit.SECONDS)).isTrue();
		byte[] payload = (byte[]) messages.get(0).getPayload();
		assertThat(payload.length).isEqualTo(13 + bytes.length);
		assertThat(new String(payload).substring(0, 13)).isEqualTo("Hello, world!");
		payload = (byte[]) messages.get(1).getPayload();
		assertThat(payload.length).isEqualTo(13 + bytes.length);
		assertThat(new String(payload).substring(0, 13)).isEqualTo("Hello, world!");

		Map<?, ?> connections = TestUtils.getPropertyValue(server, "connections", Map.class);
		Object serverConnection = connections.get(serverConnectionId.get());
		assertThat(serverConnection).isNotNull();
		assertThat(TestUtils.getPropertyValue(serverConnection, "handshakeTimeout")).isEqualTo(30);

		client.stop();
		server.stop();
	}

	private static class Replier implements TcpSender {

		private TcpConnection connection;

		@Override
		public void addNewConnection(TcpConnection connection) {
			this.connection = connection;
		}

		@Override
		public void removeDeadConnection(TcpConnection connection) {
		}

		public void send(Message<?> message) throws Exception {
			// force a renegotiation from the server side
			SSLEngine sslEngine = TestUtils.getPropertyValue(this.connection, "sslEngine", SSLEngine.class);
			sslEngine.getSession().invalidate();
			sslEngine.beginHandshake();
			this.connection.send(message);
		}

	}

	private static class SSLMapper extends TcpMessageMapper {

		@Override
		protected Map<String, ?> supplyCustomHeaders(TcpConnection connection) {
			return Collections.singletonMap("cipher", connection.getSslSession().getCipherSuite());
		}

	}

}
