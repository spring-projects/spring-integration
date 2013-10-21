/*
 * Copyright 2002-2012 the original author or authors.
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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.SSLEngine;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.messaging.Message;
import org.springframework.integration.ip.tcp.serializer.ByteArrayCrLfSerializer;
import org.springframework.integration.ip.util.TestingUtilities;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.test.util.SocketUtils;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author Gary Russell
 * @since 2.2
 *
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
		when(factory.createSocket("x", 0)).thenReturn(socket);
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
	public void testNetServer() throws Exception {
		TcpSocketFactorySupport factorySupport = mock(TcpSocketFactorySupport.class);
		ServerSocketFactory factory = mock(ServerSocketFactory.class);
		when(factorySupport.getServerSocketFactory()).thenReturn(factory);
		Socket socket = mock(Socket.class);
		InputStream is = mock(InputStream.class);
		when(is.read()).thenReturn(-1);
		when(socket.getInputStream()).thenReturn(is);
		InetAddress inetAddress = InetAddress.getLocalHost();
		when(socket.getInetAddress()).thenReturn(inetAddress);
		ServerSocket serverSocket = mock(ServerSocket.class);
		when(serverSocket.getInetAddress()).thenReturn(inetAddress);
		when(factory.createServerSocket(0, 5)).thenReturn(serverSocket);
		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);
		when(serverSocket.accept()).thenReturn(socket).then(new Answer<Socket> (){
			public Socket answer(InvocationOnMock invocation) throws Throwable {
				latch1.countDown();
				latch2.await(10, TimeUnit.SECONDS);
				return null;
			}});
		TcpSocketSupport socketSupport = mock(TcpSocketSupport.class);

		TcpNetServerConnectionFactory connectionFactory = new TcpNetServerConnectionFactory(0);
		connectionFactory.setTcpSocketFactorySupport(factorySupport);
		connectionFactory.setTcpSocketSupport(socketSupport);
		connectionFactory.registerListener(mock(TcpListener.class));
		connectionFactory.start();

		assertTrue(latch1.await(10, TimeUnit.SECONDS));
		verify(socketSupport).postProcessServerSocket(serverSocket);
		verify(socketSupport).postProcessSocket(socket);
		latch2.countDown();
		connectionFactory.stop();
	}

	@Test
	public void testNioClientAndServer() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		TcpNioClientConnectionFactory clientConnectionFactory = new TcpNioClientConnectionFactory("localhost", port);
		final AtomicInteger ppSocketCountClient = new AtomicInteger();
		final AtomicInteger ppServerSocketCountClient = new AtomicInteger();
		TcpSocketSupport clientSocketSupport = new TcpSocketSupport() {
			public void postProcessSocket(Socket socket) {
				ppSocketCountClient.incrementAndGet();
			}
			public void postProcessServerSocket(ServerSocket serverSocket) {
				ppServerSocketCountClient.incrementAndGet();
			}
		};
		clientConnectionFactory.setTcpSocketSupport(clientSocketSupport);
		clientConnectionFactory.start();
		TcpNioServerConnectionFactory serverConnectionFactory = new TcpNioServerConnectionFactory(port);
		serverConnectionFactory.registerListener(new TcpListener() {
			public boolean onMessage(Message<?> message) {
				return false;
			}
		});
		final AtomicInteger ppSocketCountServer = new AtomicInteger();
		final AtomicInteger ppServerSocketCountServer = new AtomicInteger();
		final CountDownLatch latch = new CountDownLatch(1);
		TcpSocketSupport serverSocketSupport = new TcpSocketSupport() {
			public void postProcessSocket(Socket socket) {
				ppSocketCountServer.incrementAndGet();
				latch.countDown();
			}
			public void postProcessServerSocket(ServerSocket serverSocket) {
				ppServerSocketCountServer.incrementAndGet();
			}
		};
		serverConnectionFactory.setTcpSocketSupport(serverSocketSupport);
		serverConnectionFactory.start();
		TestingUtilities.waitListening(serverConnectionFactory, null);
		clientConnectionFactory.getConnection().send(new GenericMessage<String>("Hello, world!"));
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		assertEquals(0, ppServerSocketCountClient.get());
		assertEquals(1, ppSocketCountClient.get());

		assertEquals(1, ppServerSocketCountServer.get());
		assertEquals(1, ppSocketCountServer.get());
	}

	/*
$ keytool -genkeypair -alias sitestcertkey -keyalg RSA -validity 36500 -keystore src/test/resources/test.ks
Enter keystore password: secret
Re-enter new password: secret
What is your first and last name?
  [Unknown]:  Spring Integration
What is the name of your organizational unit?
  [Unknown]:  SpringSource
What is the name of your organization?
  [Unknown]:  VMware
What is the name of your City or Locality?
  [Unknown]:  Palo Alto
What is the name of your State or Province?
  [Unknown]:  CA
What is the two-letter country code for this unit?
  [Unknown]:  US
Is CN=Spring Integration, OU=SpringSource, O=VMware, L=Palo Alto, ST=CA, C=US correct?
  [no]:  yes

Enter key password for <certificatekey>
	(RETURN if same as keystore password):

$ keytool -list -v -keystore src/test/resources/test.ks
Enter keystore password: secret

Keystore type: JKS
Keystore provider: SUN

Your keystore contains 1 entry

Alias name: sitestcertkey
Creation date: Feb 25, 2012
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Spring Integration, OU=SpringSource, O=VMware, L=Palo Alto, ST=CA, C=US
Issuer: CN=Spring Integration, OU=SpringSource, O=VMware, L=Palo Alto, ST=CA, C=US
Serial number: 4f491902
Valid from: Sat Feb 25 12:23:14 EST 2012 until: Mon Feb 01 12:23:14 EST 2112
Certificate fingerprints:
	 MD5:  4F:A9:76:0E:A9:C0:A8:B7:26:E7:7E:C7:E8:22:1F:8B
	 SHA1: 88:AC:9E:4D:29:0D:3A:59:3B:73:95:4A:E1:BB:D0:22:89:37:64:4C
	 Signature algorithm name: SHA1withRSA
	 Version: 3


*******************************************
*******************************************

$ keytool -export -alias sitestcertkey -keystore src/test/resources/test.ks -rfc -file src/test/resources/test.cer
Enter keystore password:
Certificate stored in file <src/test/resources/test.cer>

$ keytool -import -alias sitestcertkey -file src/test/resources/test.cer -keystore src/test/resources/test.truststore.ks
Enter keystore password: secret
Re-enter new password: secret
Owner: CN=Spring Integration, OU=SpringSource, O=VMware, L=Palo Alto, ST=CA, C=US
Issuer: CN=Spring Integration, OU=SpringSource, O=VMware, L=Palo Alto, ST=CA, C=US
Serial number: 4f491902
Valid from: Sat Feb 25 12:23:14 EST 2012 until: Mon Feb 01 12:23:14 EST 2112
Certificate fingerprints:
	 MD5:  4F:A9:76:0E:A9:C0:A8:B7:26:E7:7E:C7:E8:22:1F:8B
	 SHA1: 88:AC:9E:4D:29:0D:3A:59:3B:73:95:4A:E1:BB:D0:22:89:37:64:4C
	 Signature algorithm name: SHA1withRSA
	 Version: 3
Trust this certificate? [no]:  yes
Certificate was added to keystore

$ keytool -list -v -keystore src/test/resources/test.truststore.ks
Enter keystore password: secret

Keystore type: JKS
Keystore provider: SUN

Your keystore contains 1 entry

Alias name: sitestcertkey
Creation date: Feb 25, 2012
Entry type: trustedCertEntry

Owner: CN=Spring Integration, OU=SpringSource, O=VMware, L=Palo Alto, ST=CA, C=US
Issuer: CN=Spring Integration, OU=SpringSource, O=VMware, L=Palo Alto, ST=CA, C=US
Serial number: 4f491902
Valid from: Sat Feb 25 12:23:14 EST 2012 until: Mon Feb 01 12:23:14 EST 2112
Certificate fingerprints:
	 MD5:  4F:A9:76:0E:A9:C0:A8:B7:26:E7:7E:C7:E8:22:1F:8B
	 SHA1: 88:AC:9E:4D:29:0D:3A:59:3B:73:95:4A:E1:BB:D0:22:89:37:64:4C
	 Signature algorithm name: SHA1withRSA
	 Version: 3


*******************************************
*******************************************

	 */
	@Test
	public void testNetClientAndServerSSL() throws Exception {
		System.setProperty("javax.net.debug", "all"); // SSL activity in the console
		int port = SocketUtils.findAvailableServerSocket();
		TcpNetServerConnectionFactory server = new TcpNetServerConnectionFactory(port);
		TcpSSLContextSupport sslContextSupport = new DefaultTcpSSLContextSupport("test.ks",
				"test.truststore.ks", "secret", "secret");
		DefaultTcpNetSSLSocketFactorySupport tcpSocketFactorySupport = new DefaultTcpNetSSLSocketFactorySupport(sslContextSupport);
		tcpSocketFactorySupport.afterPropertiesSet();
		server.setTcpSocketFactorySupport(tcpSocketFactorySupport);
		final List<Message<?>> messages = new ArrayList<Message<?>>();
		final CountDownLatch latch = new CountDownLatch(1);
		server.registerListener(new TcpListener() {
			public boolean onMessage(Message<?> message) {
				messages.add(message);
				latch.countDown();
				return false;
			}
		});
		server.start();
		TestingUtilities.waitListening(server, null);

		TcpNetClientConnectionFactory client = new TcpNetClientConnectionFactory("localhost", port);
		client.setTcpSocketFactorySupport(tcpSocketFactorySupport);
		client.start();

		TcpConnection connection = client.getConnection();
		connection.send(new GenericMessage<String>("Hello, world!"));
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		assertEquals("Hello, world!", new String((byte[]) messages.get(0).getPayload()));
	}

	@Test
	public void testNetClientAndServerSSLDifferentContexts() throws Exception {
		System.setProperty("javax.net.debug", "all"); // SSL activity in the console
		int port = SocketUtils.findAvailableServerSocket();
		TcpNetServerConnectionFactory server = new TcpNetServerConnectionFactory(port);
		TcpSSLContextSupport serverSslContextSupport = new DefaultTcpSSLContextSupport("server.ks",
				"server.truststore.ks", "secret", "secret");
		DefaultTcpNetSSLSocketFactorySupport serverTcpSocketFactorySupport = new DefaultTcpNetSSLSocketFactorySupport(serverSslContextSupport);
		serverTcpSocketFactorySupport.afterPropertiesSet();
		server.setTcpSocketFactorySupport(serverTcpSocketFactorySupport);
		final List<Message<?>> messages = new ArrayList<Message<?>>();
		final CountDownLatch latch = new CountDownLatch(1);
		server.registerListener(new TcpListener() {
			public boolean onMessage(Message<?> message) {
				messages.add(message);
				latch.countDown();
				return false;
			}
		});
		server.start();
		TestingUtilities.waitListening(server, null);

		TcpNetClientConnectionFactory client = new TcpNetClientConnectionFactory("localhost", port);
		TcpSSLContextSupport clientSslContextSupport = new DefaultTcpSSLContextSupport("client.ks", "client.truststore.ks",
				"secret", "secret");
		DefaultTcpNetSSLSocketFactorySupport clientTcpSocketFactorySupport = new DefaultTcpNetSSLSocketFactorySupport(clientSslContextSupport);
		clientTcpSocketFactorySupport.afterPropertiesSet();
		client.setTcpSocketFactorySupport(clientTcpSocketFactorySupport);
		client.start();

		TcpConnection connection = client.getConnection();
		connection.send(new GenericMessage<String>("Hello, world!"));
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		assertEquals("Hello, world!", new String((byte[]) messages.get(0).getPayload()));
	}

	@Test
	public void testNioClientAndServerSSL() throws Exception {
		System.setProperty("javax.net.debug", "all"); // SSL activity in the console
		int port = SocketUtils.findAvailableServerSocket();
		TcpNioServerConnectionFactory server = new TcpNioServerConnectionFactory(port);
		DefaultTcpSSLContextSupport sslContextSupport = new DefaultTcpSSLContextSupport("test.ks",
				"test.truststore.ks", "secret", "secret");
		sslContextSupport.setProtocol("SSL");
		DefaultTcpNioSSLConnectionSupport tcpNioConnectionSupport = new DefaultTcpNioSSLConnectionSupport(sslContextSupport);
		tcpNioConnectionSupport.afterPropertiesSet();
		server.setTcpNioConnectionSupport(tcpNioConnectionSupport);
		final List<Message<?>> messages = new ArrayList<Message<?>>();
		final CountDownLatch latch = new CountDownLatch(1);
		server.registerListener(new TcpListener() {
			public boolean onMessage(Message<?> message) {
				System.out.println("Server" + message);
				messages.add(message);
				latch.countDown();
				return false;
			}
		});
		server.start();
		TestingUtilities.waitListening(server, null);

		TcpNioClientConnectionFactory client = new TcpNioClientConnectionFactory("localhost", port);
		client.setTcpNioConnectionSupport(tcpNioConnectionSupport);
		client.registerListener(new TcpListener() {
			public boolean onMessage(Message<?> message) {
				System.out.println("Client" + message);
				return false;
			}
		});
		client.start();

		TcpConnection connection = client.getConnection();
		connection.send(new GenericMessage<String>("Hello, world!"));
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		assertEquals("Hello, world!", new String((byte[]) messages.get(0).getPayload()));
	}

	@Test
	public void testNioClientAndServerSSLDifferentContextsLargeDataWithReply() throws Exception {
		System.setProperty("javax.net.debug", "all"); // SSL activity in the console
		int port = SocketUtils.findAvailableServerSocket();
		TcpNioServerConnectionFactory server = new TcpNioServerConnectionFactory(port);
		TcpSSLContextSupport serverSslContextSupport = new DefaultTcpSSLContextSupport("server.ks",
				"server.truststore.ks", "secret", "secret");
		DefaultTcpNioSSLConnectionSupport serverTcpNioConnectionSupport = new DefaultTcpNioSSLConnectionSupport(serverSslContextSupport);
		serverTcpNioConnectionSupport.afterPropertiesSet();
		server.setTcpNioConnectionSupport(serverTcpNioConnectionSupport);
		final List<Message<?>> messages = new ArrayList<Message<?>>();
		final CountDownLatch latch = new CountDownLatch(2);
		final Replier replier = new Replier();
		server.registerSender(replier);
		server.registerListener(new TcpListener() {
			public boolean onMessage(Message<?> message) {
				System.out.println("Server:" + message);
				messages.add(message);
				try {
					replier.send(message);
				} catch (Exception e) {
					e.printStackTrace();
				}
				latch.countDown();
				return false;
			}
		});
		ByteArrayCrLfSerializer deserializer = new ByteArrayCrLfSerializer();
		deserializer.setMaxMessageSize(120000);
		server.setDeserializer(deserializer);
		server.start();
		TestingUtilities.waitListening(server, null);

		TcpNioClientConnectionFactory client = new TcpNioClientConnectionFactory("localhost", port);
		TcpSSLContextSupport clientSslContextSupport = new DefaultTcpSSLContextSupport("client.ks",
				"client.truststore.ks", "secret", "secret");
		DefaultTcpNioSSLConnectionSupport clientTcpNioConnectionSupport = new DefaultTcpNioSSLConnectionSupport(clientSslContextSupport);
		clientTcpNioConnectionSupport.afterPropertiesSet();
		client.setTcpNioConnectionSupport(clientTcpNioConnectionSupport);
		client.registerListener(new TcpListener() {
			public boolean onMessage(Message<?> message) {
				System.out.println("Client:" + message);
				messages.add(message);
				latch.countDown();
				return false;
			}
		});
		client.setDeserializer(deserializer);
		client.start();

		TcpConnection connection = client.getConnection();
		byte[] bytes = new byte[100000];
		connection.send(new GenericMessage<String>("Hello, world!" + new String(bytes)));
		assertTrue(latch.await(60, TimeUnit.SECONDS));
		byte[] payload = (byte[]) messages.get(0).getPayload();
		assertEquals(13 + bytes.length, payload.length);
		assertEquals("Hello, world!", new String(payload).substring(0, 13));
		payload = (byte[]) messages.get(1).getPayload();
		assertEquals(13 + bytes.length, payload.length);
		assertEquals("Hello, world!", new String(payload).substring(0, 13));
	}

	private class Replier implements TcpSender {

		private TcpConnection connection;

		public void addNewConnection(TcpConnection connection) {
			this.connection = connection;
		}

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
}
