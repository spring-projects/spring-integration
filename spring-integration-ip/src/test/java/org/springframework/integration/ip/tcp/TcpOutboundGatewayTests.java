/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.ip.tcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ServerSocketFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.serializer.DefaultDeserializer;
import org.springframework.core.serializer.DefaultSerializer;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.MessageTimeoutException;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.CachingClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.FailoverClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpConnectionSupport;
import org.springframework.integration.ip.tcp.connection.TcpNetClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNioClientConnectionFactory;
import org.springframework.integration.ip.tcp.serializer.ByteArrayCrLfSerializer;
import org.springframework.integration.ip.tcp.serializer.SoftEndOfStreamException;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.condition.LongRunningTest;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
@LongRunningTest
public class TcpOutboundGatewayTests {

	private static final Log logger = LogFactory.getLog(TcpOutboundGatewayTests.class);

	private final AsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("TcpOutboundGatewayTests-");

	@Test
	void testGoodNetSingle() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean done = new AtomicBoolean();
		final AtomicReference<ServerSocket> serverSocket = new AtomicReference<>();
		this.executor.execute(() -> {
			try {
				ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0, 100);
				serverSocket.set(server);
				latch.countDown();
				List<Socket> sockets = new ArrayList<>();
				int i = 0;
				while (true) {
					Socket socket = server.accept();
					ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
					ois.readObject();
					ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
					oos.writeObject("Reply" + (i++));
					sockets.add(socket);
				}
			}
			catch (Exception e) {
				if (!done.get()) {
					e.printStackTrace();
				}
			}
		});
		assertThat(latch.await(10000, TimeUnit.MILLISECONDS)).isTrue();
		AbstractClientConnectionFactory ccf = new TcpNetClientConnectionFactory("localhost",
				serverSocket.get().getLocalPort());
		ccf.setSerializer(new DefaultSerializer());
		ccf.setDeserializer(new DefaultDeserializer());
		ccf.setSoTimeout(10000);
		ccf.setSingleUse(true);
		ccf.start();
		TcpOutboundGateway gateway = new TcpOutboundGateway();
		gateway.setConnectionFactory(ccf);
		QueueChannel replyChannel = new QueueChannel();
		gateway.setRequiresReply(true);
		gateway.setOutputChannel(replyChannel);
		// check the default remote timeout
		assertThat(TestUtils.getPropertyValue(gateway, "remoteTimeoutExpression.value")).isEqualTo(10000L);
		gateway.setSendTimeout(123);
		gateway.setRemoteTimeout(60000);
		gateway.setSendTimeout(61000);
		// ensure this did NOT change the remote timeout
		assertThat(TestUtils.getPropertyValue(gateway, "remoteTimeoutExpression.value")).isEqualTo(60000L);
		gateway.setRequestTimeout(60000);
		for (int i = 100; i < 200; i++) {
			gateway.handleMessage(MessageBuilder.withPayload("Test" + i).build());
		}
		Set<String> replies = new HashSet<>();
		for (int i = 100; i < 200; i++) {
			Message<?> m = replyChannel.receive(10000);
			assertThat(m).isNotNull();
			replies.add((String) m.getPayload());
		}
		for (int i = 0; i < 100; i++) {
			assertThat(replies.remove("Reply" + i)).isTrue();
		}
		done.set(true);
		ccf.stop();
		serverSocket.get().close();
	}

	@Test
	void testGoodNetMultiplex() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean done = new AtomicBoolean();
		final AtomicReference<ServerSocket> serverSocket = new AtomicReference<>();
		this.executor.execute(() -> {
			try {
				ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0, 10);
				serverSocket.set(server);
				latch.countDown();
				int i = 0;
				Socket socket = server.accept();
				while (true) {
					ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
					ois.readObject();
					ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
					oos.writeObject("Reply" + (i++));
				}
			}
			catch (Exception e) {
				if (!done.get()) {
					e.printStackTrace();
				}
			}
		});
		assertThat(latch.await(10000, TimeUnit.MILLISECONDS)).isTrue();
		AbstractClientConnectionFactory ccf = new TcpNetClientConnectionFactory("localhost",
				serverSocket.get().getLocalPort());
		ccf.setSerializer(new DefaultSerializer());
		ccf.setDeserializer(new DefaultDeserializer());
		ccf.setSoTimeout(10000);
		ccf.setSingleUse(false);
		ccf.start();
		TcpOutboundGateway gateway = new TcpOutboundGateway();
		gateway.setConnectionFactory(ccf);
		QueueChannel replyChannel = new QueueChannel();
		gateway.setRequiresReply(true);
		gateway.setOutputChannel(replyChannel);
		for (int i = 100; i < 110; i++) {
			gateway.handleMessage(MessageBuilder.withPayload("Test" + i).build());
		}
		Set<String> replies = new HashSet<>();
		for (int i = 100; i < 110; i++) {
			Message<?> m = replyChannel.receive(10000);
			assertThat(m).isNotNull();
			replies.add((String) m.getPayload());
		}
		for (int i = 0; i < 10; i++) {
			assertThat(replies.remove("Reply" + i)).isTrue();
		}
		done.set(true);
		gateway.stop();
		ccf.stop();
		serverSocket.get().close();
	}

	@Test
	void testGoodNetTimeout() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean done = new AtomicBoolean();
		final AtomicReference<ServerSocket> serverSocket = new AtomicReference<>();
		this.executor.execute(() -> {
			try {
				ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0);
				serverSocket.set(server);
				latch.countDown();
				int i = 0;
				Socket socket = server.accept();
				while (true) {
					ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
					ois.readObject();
					ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
					Thread.sleep(1000);
					oos.writeObject("Reply" + (i++));
				}
			}
			catch (Exception e) {
				if (!done.get()) {
					e.printStackTrace();
				}
			}
		});
		assertThat(latch.await(10000, TimeUnit.MILLISECONDS)).isTrue();
		AbstractClientConnectionFactory ccf = new TcpNetClientConnectionFactory("localhost",
				serverSocket.get().getLocalPort());
		ccf.setSerializer(new DefaultSerializer());
		ccf.setDeserializer(new DefaultDeserializer());
		ccf.setSoTimeout(10000);
		ccf.setSingleUse(false);
		ccf.start();
		final TcpOutboundGateway gateway = new TcpOutboundGateway();
		gateway.setConnectionFactory(ccf);
		gateway.setRequestTimeout(1);
		QueueChannel replyChannel = new QueueChannel();
		gateway.setRequiresReply(true);
		gateway.setOutputChannel(replyChannel);
		@SuppressWarnings("unchecked")
		Future<Integer>[] results = (Future<Integer>[]) new Future<?>[2];
		for (int i = 0; i < 2; i++) {
			final int j = i;
			results[j] = (this.executor.submit(() -> {
				gateway.handleMessage(MessageBuilder.withPayload("Test" + j).build());
				return 0;
			}));
		}
		Set<String> replies = new HashSet<>();
		int timeouts = 0;
		for (int i = 0; i < 2; i++) {
			try {
				results[i].get();
			}
			catch (ExecutionException e) {
				if (timeouts > 0) {
					fail("Unexpected " + e.getMessage());
				}
				else {
					assertThat(e.getCause()).isInstanceOf(MessageTimeoutException.class);
				}
				timeouts++;
				continue;
			}
			Message<?> m = replyChannel.receive(10000);
			assertThat(m).isNotNull();
			replies.add((String) m.getPayload());
		}
		if (timeouts < 1) {
			fail("Expected ExecutionException");
		}
		for (int i = 0; i < 1; i++) {
			assertThat(replies.remove("Reply" + i)).isTrue();
		}
		done.set(true);
		gateway.stop();
		ccf.stop();
		serverSocket.get().close();
	}

	@Test
	void testGoodNetGWTimeout() throws Exception {
		ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket(0);
		final int port = serverSocket.getLocalPort();
		AbstractClientConnectionFactory ccf = buildCF(port);
		ccf.start();
		testGoodNetGWTimeoutGuts(ccf, serverSocket);
		serverSocket.close();
	}

	@Test
	void testGoodNetGWTimeoutCached() throws Exception {
		ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket(0);
		final int port = serverSocket.getLocalPort();
		AbstractClientConnectionFactory ccf = buildCF(port);
		CachingClientConnectionFactory cccf = new CachingClientConnectionFactory(ccf, 1);
		cccf.start();
		testGoodNetGWTimeoutGuts(cccf, serverSocket);
		serverSocket.close();
	}

	private AbstractClientConnectionFactory buildCF(final int port) {
		AbstractClientConnectionFactory ccf = new TcpNetClientConnectionFactory("localhost", port);
		ccf.setSerializer(new DefaultSerializer());
		ccf.setDeserializer(new DefaultDeserializer());
		ccf.setSoTimeout(10000);
		ccf.setSingleUse(false);
		return ccf;
	}

	/**
	 * Sends 2 concurrent messages on a shared connection. The GW single threads
	 * these requests. The first will timeout; the second should receive its
	 * own response, not that for the first.
	 * @throws Exception
	 */
	private void testGoodNetGWTimeoutGuts(AbstractClientConnectionFactory ccf,
			final ServerSocket server) throws InterruptedException {

		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean done = new AtomicBoolean();
		/*
		 * The payload of the last message received by the remote side;
		 * used to verify the correct response is received.
		 */
		final AtomicReference<String> lastReceived = new AtomicReference<>();
		final CountDownLatch serverLatch = new CountDownLatch(2);

		this.executor.execute(() -> {
			try {
				latch.countDown();
				int i = 0;
				while (!done.get()) {
					Socket socket = server.accept();
					i++;
					while (!socket.isClosed()) {
						try {
							ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
							String request = (String) ois.readObject();
							logger.debug("Read " + request);
							ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
							if (i < 2) {
								Thread.sleep(2000);
							}
							oos.writeObject(request.replace("Test", "Reply"));
							logger.debug("Replied to " + request);
							lastReceived.set(request);
						}
						catch (IOException e1) {
							logger.debug("error on write " + e1.getMessage());
							socket.close();
						}
						finally {
							serverLatch.countDown();
						}
					}
				}
			}
			catch (Exception e2) {
				if (!done.get()) {
					e2.printStackTrace();
				}
			}
		});
		assertThat(latch.await(10000, TimeUnit.MILLISECONDS)).isTrue();
		final TcpOutboundGateway gateway = new TcpOutboundGateway();
		gateway.setConnectionFactory(ccf);
		gateway.setRequestTimeout(Integer.MAX_VALUE);
		QueueChannel replyChannel = new QueueChannel();
		gateway.setRequiresReply(true);
		gateway.setOutputChannel(replyChannel);

		Expression remoteTimeoutExpression = Mockito.mock(Expression.class);

		when(remoteTimeoutExpression.getValue(Mockito.any(EvaluationContext.class), Mockito.any(Message.class),
				Mockito.eq(Long.class))).thenReturn(50L, 60000L);

		gateway.setRemoteTimeoutExpression(remoteTimeoutExpression);

		@SuppressWarnings("unchecked")
		Future<Integer>[] results = (Future<Integer>[]) new Future<?>[2];

		for (int i = 0; i < 2; i++) {
			final int j = i;
			results[j] = (this.executor.submit(() -> {
				gateway.handleMessage(MessageBuilder.withPayload("Test" + j).build());
				return j;
			}));

		}
		// wait until the server side has processed both requests
		assertThat(serverLatch.await(30, TimeUnit.SECONDS)).isTrue();
		List<String> replies = new ArrayList<String>();
		int timeouts = 0;
		for (int i = 0; i < 2; i++) {
			try {
				int result = results[i].get();
				String reply = (String) replyChannel.receive(1000).getPayload();
				logger.debug(i + " got " + result + " " + reply);
				replies.add(reply);
			}
			catch (ExecutionException e) {
				if (timeouts >= 2) {
					fail("Unexpected " + e.getMessage());
				}
				else {
					assertThat(e.getCause()).isNotNull();
					assertThat(e.getCause()).isInstanceOf(MessageTimeoutException.class);
				}
				timeouts++;
				continue;
			}
		}
		assertThat(timeouts).as("Expected exactly one ExecutionException").isEqualTo(1);
		assertThat(replies.size()).isEqualTo(1);
		assertThat(replies.get(0)).isEqualTo(lastReceived.get().replace("Test", "Reply"));
		done.set(true);
		assertThat(TestUtils.getPropertyValue(gateway, "pendingReplies", Map.class).size()).isEqualTo(0);
		gateway.stop();
		ccf.stop();
	}

	@Test
	void testCachingFailover() throws Exception {
		final AtomicReference<ServerSocket> serverSocket = new AtomicReference<>();
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean done = new AtomicBoolean();
		final CountDownLatch serverLatch = new CountDownLatch(1);

		this.executor.execute(() -> {
			try {
				ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0);
				serverSocket.set(server);
				latch.countDown();
				while (!done.get()) {
					Socket socket = server.accept();
					while (!socket.isClosed()) {
						try {
							ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
							String request = (String) ois.readObject();
							logger.debug("Read " + request);
							ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
							oos.writeObject("bar");
							logger.debug("Replied to " + request);
							serverLatch.countDown();
						}
						catch (IOException e1) {
							logger.debug("error on write " + e1.getClass().getSimpleName());
							socket.close();
						}
					}
				}
			}
			catch (Exception e2) {
				if (!done.get()) {
					e2.printStackTrace();
				}
			}
		});
		assertThat(latch.await(10000, TimeUnit.MILLISECONDS)).isTrue();

		// Failover
		AbstractClientConnectionFactory factory1 = mock(AbstractClientConnectionFactory.class);
		TcpConnectionSupport mockConn1 = makeMockConnection();
		when(factory1.getConnection()).thenReturn(mockConn1);
		doThrow(new UncheckedIOException(new IOException("fail")))
				.when(mockConn1).send(Mockito.any(Message.class));

		AbstractClientConnectionFactory factory2 = new TcpNetClientConnectionFactory("localhost",
				serverSocket.get().getLocalPort());
		factory2.setSerializer(new DefaultSerializer());
		factory2.setDeserializer(new DefaultDeserializer());
		factory2.setSoTimeout(10000);
		factory2.setSingleUse(false);

		List<AbstractClientConnectionFactory> factories = new ArrayList<>();
		factories.add(factory1);
		factories.add(factory2);
		FailoverClientConnectionFactory failoverFactory = new FailoverClientConnectionFactory(factories);
		failoverFactory.start();

		// Cache
		CachingClientConnectionFactory cachingFactory = new CachingClientConnectionFactory(failoverFactory, 2);
		cachingFactory.start();
		TcpOutboundGateway gateway = new TcpOutboundGateway();
		gateway.setConnectionFactory(cachingFactory);
		PollableChannel outputChannel = new QueueChannel();
		gateway.setOutputChannel(outputChannel);
		gateway.setBeanFactory(mock(BeanFactory.class));
		gateway.afterPropertiesSet();
		gateway.start();

		GenericMessage<String> message = new GenericMessage<>("foo");
		gateway.handleMessage(message);
		Message<?> reply = outputChannel.receive(0);
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isEqualTo("bar");
		done.set(true);
		gateway.stop();
		verify(mockConn1).send(Mockito.any(Message.class));
		factory2.stop();
		serverSocket.get().close();
	}

	@Test
	void testFailoverCached() throws Exception {
		final AtomicReference<ServerSocket> serverSocket = new AtomicReference<>();
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean done = new AtomicBoolean();
		final CountDownLatch serverLatch = new CountDownLatch(1);

		this.executor.execute(() -> {
			try {
				ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0);
				serverSocket.set(server);
				latch.countDown();
				while (!done.get()) {
					Socket socket = server.accept();
					while (!socket.isClosed()) {
						try {
							ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
							String request = (String) ois.readObject();
							logger.debug("Read " + request);
							ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
							oos.writeObject("bar");
							logger.debug("Replied to " + request);
							serverLatch.countDown();
						}
						catch (IOException e1) {
							logger.debug("error on write " + e1.getClass().getSimpleName());
							socket.close();
						}
					}
				}
			}
			catch (Exception e2) {
				if (!done.get()) {
					e2.printStackTrace();
				}
			}
		});
		assertThat(latch.await(10000, TimeUnit.MILLISECONDS)).isTrue();

		// Cache
		AbstractClientConnectionFactory factory1 = mock(AbstractClientConnectionFactory.class);
		TcpConnectionSupport mockConn1 = makeMockConnection();
		when(factory1.getConnection()).thenReturn(mockConn1);
		when(factory1.isSingleUse()).thenReturn(true);
		doThrow(new UncheckedIOException(new IOException("fail")))
				.when(mockConn1).send(Mockito.any(Message.class));
		CachingClientConnectionFactory cachingFactory1 = new CachingClientConnectionFactory(factory1, 1);

		AbstractClientConnectionFactory factory2 = new TcpNetClientConnectionFactory("localhost",
				serverSocket.get().getLocalPort());
		factory2.setSerializer(new DefaultSerializer());
		factory2.setDeserializer(new DefaultDeserializer());
		factory2.setSoTimeout(10000);
		factory2.setSingleUse(true);
		CachingClientConnectionFactory cachingFactory2 = new CachingClientConnectionFactory(factory2, 1);

		// Failover
		List<AbstractClientConnectionFactory> factories = new ArrayList<>();
		factories.add(cachingFactory1);
		factories.add(cachingFactory2);
		FailoverClientConnectionFactory failoverFactory = new FailoverClientConnectionFactory(factories);
		failoverFactory.setSingleUse(true);
		failoverFactory.afterPropertiesSet();
		failoverFactory.start();

		TcpOutboundGateway gateway = new TcpOutboundGateway();
		gateway.setConnectionFactory(failoverFactory);
		PollableChannel outputChannel = new QueueChannel();
		gateway.setOutputChannel(outputChannel);
		gateway.setBeanFactory(mock(BeanFactory.class));
		gateway.afterPropertiesSet();
		gateway.start();

		GenericMessage<String> message = new GenericMessage<>("foo");
		gateway.handleMessage(message);
		Message<?> reply = outputChannel.receive(0);
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isEqualTo("bar");
		done.set(true);
		gateway.stop();
		verify(mockConn1).send(Mockito.any(Message.class));
		factory2.stop();
		serverSocket.get().close();
	}

	public TcpConnectionSupport makeMockConnection() {
		TcpConnectionSupport connection = mock(TcpConnectionSupport.class);
		when(connection.isOpen()).thenReturn(true);
		return connection;
	}

	@Test
	void testNetGWPropagatesSocketClose() throws Exception {
		ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket(0);
		final int port = serverSocket.getLocalPort();
		AbstractClientConnectionFactory ccf = new TcpNetClientConnectionFactory("localhost", port);
		ccf.setSerializer(new DefaultSerializer());
		ccf.setDeserializer(new DefaultDeserializer());
		ccf.setSoTimeout(10000);
		ccf.setSingleUse(false);
		ccf.start();
		testGWPropagatesSocketCloseGuts(ccf, serverSocket);
		serverSocket.close();
	}

	@Test
	void testNioGWPropagatesSocketClose() throws Exception {
		ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket(0);
		final int port = serverSocket.getLocalPort();
		AbstractClientConnectionFactory ccf = new TcpNioClientConnectionFactory("localhost", port);
		ccf.setSerializer(new DefaultSerializer());
		ccf.setDeserializer(new DefaultDeserializer());
		ccf.setSoTimeout(10000);
		ccf.setSingleUse(false);
		ccf.start();
		testGWPropagatesSocketCloseGuts(ccf, serverSocket);
		serverSocket.close();
	}

	@Test
	void testCachedGWPropagatesSocketClose() throws Exception {
		ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket(0);
		final int port = serverSocket.getLocalPort();
		AbstractClientConnectionFactory ccf = new TcpNetClientConnectionFactory("localhost", port);
		ccf.setSerializer(new DefaultSerializer());
		ccf.setDeserializer(new DefaultDeserializer());
		ccf.setSoTimeout(10000);
		ccf.setSingleUse(false);
		CachingClientConnectionFactory cccf = new CachingClientConnectionFactory(ccf, 1);
		cccf.start();
		testGWPropagatesSocketCloseGuts(cccf, serverSocket);
		serverSocket.close();
	}

	@Test
	void testFailoverGWPropagatesSocketClose() throws Exception {
		ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket(0);
		final int port = serverSocket.getLocalPort();
		AbstractClientConnectionFactory ccf = new TcpNetClientConnectionFactory("localhost", port);
		ccf.setSerializer(new DefaultSerializer());
		ccf.setDeserializer(new DefaultDeserializer());
		ccf.setSoTimeout(10000);
		ccf.setSingleUse(false);
		FailoverClientConnectionFactory focf = new FailoverClientConnectionFactory(
				Collections.singletonList(ccf));
		focf.start();
		testGWPropagatesSocketCloseGuts(focf, serverSocket);
		serverSocket.close();
	}

	private void testGWPropagatesSocketCloseGuts(AbstractClientConnectionFactory ccf,
			final ServerSocket server) throws Exception {

		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean done = new AtomicBoolean();
		final AtomicReference<String> lastReceived = new AtomicReference<>();
		final CountDownLatch serverLatch = new CountDownLatch(1);

		this.executor.execute(() -> {
			List<Socket> sockets = new ArrayList<>();
			try {
				latch.countDown();
				while (!done.get()) {
					Socket socket1 = server.accept();
					sockets.add(socket1);
					while (!socket1.isClosed()) {
						try {
							ObjectInputStream ois = new ObjectInputStream(socket1.getInputStream());
							String request = (String) ois.readObject();
							logger.debug("Read " + request + " closing socket");
							socket1.close();
							lastReceived.set(request);
							serverLatch.countDown();
						}
						catch (IOException e1) {
							logger.debug("error on write " + e1.getMessage());
							socket1.close();
						}
					}
				}
			}
			catch (Exception e2) {
				if (!done.get()) {
					e2.printStackTrace();
				}
			}
			for (Socket socket2 : sockets) {
				try {
					socket2.close();
				}
				catch (@SuppressWarnings("unused") IOException e3) {
				}
			}
		});
		assertThat(latch.await(10000, TimeUnit.MILLISECONDS)).isTrue();
		final TcpOutboundGateway gateway = new TcpOutboundGateway();
		gateway.setConnectionFactory(ccf);
		gateway.setRequestTimeout(Integer.MAX_VALUE);
		QueueChannel replyChannel = new QueueChannel();
		gateway.setRequiresReply(true);
		gateway.setOutputChannel(replyChannel);
		gateway.setRemoteTimeoutExpression(new SpelExpressionParser().parseExpression("5000"));
		gateway.setBeanFactory(mock(BeanFactory.class));
		gateway.afterPropertiesSet();
		gateway.start();
		Throwable thrown = catchThrowable(() -> gateway.handleMessage(MessageBuilder.withPayload("Test").build()));
		assertThat(thrown).isInstanceOf(MessageHandlingException.class);
		assertThat(thrown.getCause()).isInstanceOf(MessagingException.class);
		assertThat(thrown.getCause().getCause()).isInstanceOf(EOFException.class);
		assertThat(TestUtils.getPropertyValue(gateway, "pendingReplies", Map.class).size()).isEqualTo(0);
		Message<?> reply = replyChannel.receive(0);
		assertThat(reply).isNull();
		done.set(true);
		ccf.getConnection();
		gateway.stop();
		ccf.stop();
	}

	@Test
	void testNetGWPropagatesSocketTimeout() throws Exception {
		ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket(0);
		final int port = serverSocket.getLocalPort();
		AbstractClientConnectionFactory ccf = new TcpNetClientConnectionFactory("localhost", port);
		ccf.setSerializer(new DefaultSerializer());
		ccf.setDeserializer(new DefaultDeserializer());
		ccf.setSoTimeout(100);
		ccf.setSingleUse(false);
		ccf.start();
		testGWPropagatesSocketTimeoutGuts(ccf, serverSocket);
		serverSocket.close();
	}

	@Test
	void testNioGWPropagatesSocketTimeout() throws Exception {
		ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket(0);
		final int port = serverSocket.getLocalPort();
		AbstractClientConnectionFactory ccf = new TcpNioClientConnectionFactory("localhost", port);
		ccf.setSerializer(new DefaultSerializer());
		ccf.setDeserializer(new DefaultDeserializer());
		ccf.setSoTimeout(100);
		ccf.setSingleUse(false);
		ccf.start();
		testGWPropagatesSocketTimeoutGuts(ccf, serverSocket);
		serverSocket.close();
	}

	@Test
	void testNetGWPropagatesSocketTimeoutSingleUse() throws Exception {
		ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket(0);
		final int port = serverSocket.getLocalPort();
		AbstractClientConnectionFactory ccf = new TcpNetClientConnectionFactory("localhost", port);
		ccf.setSerializer(new DefaultSerializer());
		ccf.setDeserializer(new DefaultDeserializer());
		ccf.setSoTimeout(100);
		ccf.setSingleUse(true);
		ccf.start();
		testGWPropagatesSocketTimeoutGuts(ccf, serverSocket);
		serverSocket.close();
	}

	@Test
	void testNioGWPropagatesSocketTimeoutSingleUse() throws Exception {
		ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket(0);
		final int port = serverSocket.getLocalPort();
		AbstractClientConnectionFactory ccf = new TcpNioClientConnectionFactory("localhost", port);
		ccf.setSerializer(new DefaultSerializer());
		ccf.setDeserializer(new DefaultDeserializer());
		ccf.setSoTimeout(100);
		ccf.setSingleUse(true);
		ccf.start();
		testGWPropagatesSocketTimeoutGuts(ccf, serverSocket);
		serverSocket.close();
	}

	private void testGWPropagatesSocketTimeoutGuts(AbstractClientConnectionFactory ccf,
			final ServerSocket server) throws Exception {

		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean done = new AtomicBoolean();

		this.executor.execute(() -> {
			List<Socket> sockets = new ArrayList<>();
			try {
				latch.countDown();
				while (!done.get()) {
					sockets.add(server.accept());
				}
			}
			catch (Exception e1) {
				if (!done.get()) {
					e1.printStackTrace();
				}
			}
			for (Socket socket : sockets) {
				try {
					socket.close();
				}
				catch (@SuppressWarnings("unused") IOException e2) {
				}
			}
		});
		assertThat(latch.await(10000, TimeUnit.MILLISECONDS)).isTrue();
		final TcpOutboundGateway gateway = new TcpOutboundGateway();
		gateway.setConnectionFactory(ccf);
		gateway.setRequestTimeout(Integer.MAX_VALUE);
		QueueChannel replyChannel = new QueueChannel();
		gateway.setRequiresReply(true);
		gateway.setOutputChannel(replyChannel);
		gateway.setRemoteTimeoutExpression(new SpelExpressionParser().parseExpression("5000"));
		gateway.setBeanFactory(mock(BeanFactory.class));
		gateway.afterPropertiesSet();
		gateway.start();
		Throwable thrown = catchThrowable(() -> gateway.handleMessage(MessageBuilder.withPayload("Test").build()));
		assertThat(thrown).isInstanceOf(MessageHandlingException.class);
		assertThat(thrown.getCause()).isInstanceOf(MessagingException.class);
		assertThat(thrown.getCause().getCause()).isInstanceOf(SocketTimeoutException.class);
		assertThat(TestUtils.getPropertyValue(gateway, "pendingReplies", Map.class).size()).isEqualTo(0);
		Message<?> reply = replyChannel.receive(0);
		assertThat(reply).isNull();
		done.set(true);
		ccf.getConnection();
		gateway.stop();
		ccf.stop();
	}

	@Test
	void testNioSecondChance() throws Exception {
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0);
		final int port = server.getLocalPort();
		TcpOutboundGateway gateway = new TcpOutboundGateway();
		TcpNioClientConnectionFactory cf = new TcpNioClientConnectionFactory("localhost", port);
		cf.setApplicationEventPublisher(e -> { });
		gateway.setConnectionFactory(cf);
		final AtomicBoolean done = new AtomicBoolean();

		this.executor.execute(() -> {
			List<Socket> sockets = new ArrayList<>();
			try {
				while (!done.get()) {
					sockets.add(server.accept());
				}
			}
			catch (Exception e1) {
				if (!done.get()) {
					e1.printStackTrace();
				}
			}
			for (Socket socket : sockets) {
				try {
					socket.close();
				}
				catch (@SuppressWarnings("unused") IOException e2) {
				}
			}
		});
		QueueChannel replies = new QueueChannel();
		gateway.setReplyChannel(replies);
		gateway.setBeanFactory(mock(BeanFactory.class));
		gateway.setRemoteTimeout(60_000);
		gateway.afterPropertiesSet();
		gateway.start();
		this.executor.execute(() -> gateway.handleMessage(new GenericMessage<>("foo")));
		int n = 0;
		@SuppressWarnings("unchecked")
		Map<String, ?> pending = TestUtils.getPropertyValue(gateway, "pendingReplies", Map.class);
		await().atMost(Duration.ofSeconds(10)).until(() -> pending.size() > 0);
		String connectionId = pending.keySet().iterator().next();
		this.executor.execute(() -> gateway.onMessage(new ErrorMessage(new RuntimeException(),
				Collections.singletonMap(IpHeaders.CONNECTION_ID, connectionId))));
		GenericMessage<String> message = new GenericMessage<>("FOO",
				Collections.singletonMap(IpHeaders.CONNECTION_ID, connectionId));
		gateway.onMessage(message);
		assertThat(replies.receive(10000)).isEqualTo(message);
		gateway.stop();
		done.set(true);
		server.close();
	}

	@Test
	void testAsyncSingle() throws Exception {
		testAsync(true);
	}

	@Test
	void testAsyncShared() throws Exception {
		testAsync(false);
	}

	private void testAsync(boolean singleUse) throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean done = new AtomicBoolean();
		final AtomicReference<ServerSocket> serverSocket = new AtomicReference<>();
		ThreadPoolTaskScheduler sched = new ThreadPoolTaskScheduler();
		sched.initialize();
		TcpOutboundGateway gateway = null;
		try {
			this.executor.execute(() -> {
				try {
					ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0, 100);
					serverSocket.set(server);
					latch.countDown();
					int i = 0;
					while (true) {
						Socket socket = server.accept();
						InputStream is = socket.getInputStream();
						OutputStream os = socket.getOutputStream();
						ByteArrayCrLfSerializer deser = new ByteArrayCrLfSerializer();
						try {
							deser.deserialize(is);
						}
						catch (SoftEndOfStreamException e) {
							continue;
						}
						deser.serialize(("reply" + ++i).getBytes(), os);
						if (!singleUse) {
							deser.deserialize(is);
							deser.serialize(("reply" + ++i).getBytes(), os);
						}
						socket.close();
					}
				}
				catch (Exception e) {
					if (!done.get()) {
						e.printStackTrace();
					}
				}
			});
			assertThat(latch.await(10000, TimeUnit.MILLISECONDS)).isTrue();
			AbstractClientConnectionFactory ccf = new TcpNetClientConnectionFactory("localhost",
					serverSocket.get().getLocalPort());
			ccf.setSoTimeout(10000);
			ccf.setSingleUse(singleUse);
			ccf.start();
			gateway = new TcpOutboundGateway();
			gateway.setConnectionFactory(ccf);
			gateway.setAsync(true);
			QueueChannel replyChannel = new QueueChannel();
			AtomicReference<Thread> thread = new AtomicReference<>();
			replyChannel.addInterceptor(new ChannelInterceptor() {

				@Override
				public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
					thread.set(Thread.currentThread());
				}

			});
			gateway.setRequiresReply(true);
			gateway.setOutputChannel(replyChannel);
			gateway.setBeanFactory(mock(BeanFactory.class));
			gateway.setTaskScheduler(sched);
			gateway.afterPropertiesSet();
			gateway.handleMessage(MessageBuilder.withPayload("Test1").build());
			gateway.handleMessage(MessageBuilder.withPayload("Test2").build());
			Message<?> reply1 = replyChannel.receive(10000);
			assertThat(reply1).isNotNull();
			if (singleUse) {
				assertThat(reply1.getPayload()).satisfiesAnyOf(
						payload -> assertThat(payload).isEqualTo("reply1".getBytes()),
						payload -> assertThat(payload).isEqualTo("reply2".getBytes()));
			}
			else {
				assertThat(reply1.getPayload()).isEqualTo("reply1".getBytes());
			}
			Message<?> reply2 = replyChannel.receive(10000);
			assertThat(reply2).isNotNull();
			if (singleUse) {
				assertThat(reply1.getPayload()).satisfiesAnyOf(
						payload -> assertThat(payload).isEqualTo("reply1".getBytes()),
						payload -> assertThat(payload).isEqualTo("reply2".getBytes()));
				assertThat(reply1.getPayload()).isNotEqualTo(reply2.getPayload());
			}
			else {
				assertThat(reply2.getPayload()).isEqualTo("reply2".getBytes());
			}
			assertThat(thread.get()).isNotSameAs(Thread.currentThread());
		}
		finally {
			if (gateway != null) {
				gateway.stop();
			}
			done.set(true);
			if (serverSocket.get() != null) {
				serverSocket.get().close();
			}
			sched.shutdown();
		}
	}

	@Test
	void testAsyncTimeout() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		final CountDownLatch doneLatch = new CountDownLatch(1);
		final AtomicBoolean done = new AtomicBoolean();
		final AtomicReference<ServerSocket> serverSocket = new AtomicReference<>();
		AbstractClientConnectionFactory ccf = null;
		ThreadPoolTaskScheduler sched = new ThreadPoolTaskScheduler();
		sched.initialize();
		try {
			this.executor.execute(() -> {
				try {
					ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0, 100);
					serverSocket.set(server);
					latch.countDown();
					int i = 0;
					while (true) {
						Socket socket = server.accept();
						doneLatch.await(10, TimeUnit.SECONDS);
						socket.close();
					}
				}
				catch (Exception e) {
					if (!done.get()) {
						e.printStackTrace();
					}
				}
			});
			assertThat(latch.await(10000, TimeUnit.MILLISECONDS)).isTrue();
			ccf = new TcpNetClientConnectionFactory("localhost",
					serverSocket.get().getLocalPort());
			ccf.setSoTimeout(10000);
			ccf.start();
			TcpOutboundGateway gateway = new TcpOutboundGateway();
			gateway.setConnectionFactory(ccf);
			gateway.setAsync(true);
			gateway.setRemoteTimeout(10);
			QueueChannel replyChannel = new QueueChannel();
			gateway.setRequiresReply(true);
			gateway.setOutputChannel(replyChannel);
			gateway.setBeanFactory(mock(BeanFactory.class));
			gateway.setTaskScheduler(sched);
			gateway.afterPropertiesSet();
			QueueChannel errorChannel = new QueueChannel();
			gateway.handleMessage(MessageBuilder.withPayload("Test1")
					.setErrorChannel(errorChannel)
					.build());
			Message<?> reply = errorChannel.receive(10000);
			assertThat(reply).isInstanceOf(ErrorMessage.class);
			assertThat(reply.getPayload()).isInstanceOf(MessageTimeoutException.class);
			doneLatch.countDown();
			gateway.stop();
		}
		finally {
			done.set(true);
			if (ccf != null) {
				ccf.stop();
			}
			if (serverSocket.get() != null) {
				serverSocket.get().close();
			}
			sched.shutdown();
		}
	}

}
