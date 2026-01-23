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
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.integration.ip.event.IpIntegrationEvent;
import org.springframework.integration.ip.tcp.connection.TcpNioConnection.ChannelInputStream;
import org.springframework.integration.ip.tcp.serializer.ByteArrayCrLfSerializer;
import org.springframework.integration.ip.tcp.serializer.MapJsonSerializer;
import org.springframework.integration.ip.util.TestingUtilities;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.converter.MapMessageConverter;
import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.util.CompositeExecutor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StopWatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.with;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author Gary Russell
 * @author John Anderson
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 2.0
 *
 */
public class TcpNioConnectionTests implements TestApplicationContextAware {

	private static final Log logger = LogFactory.getLog(TcpNioConnectionTests.class);

	private final ApplicationEventPublisher nullPublisher = mock();

	private final AsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("TcpNioConnectionTests-");

	@Test
	public void testWriteTimeout(TestInfo testInfo) throws Exception {
		CountDownLatch latch = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(1);
		AtomicReference<ServerSocket> serverSocket = new AtomicReference<>();
		this.executor.execute(() -> {
			try {
				ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0);
				logger.debug(testInfo.getTestMethod().get().getName() +
						" starting server for " + server.getLocalPort());
				serverSocket.set(server);
				latch.countDown();
				Socket s = server.accept();
				// block so we fill the buffer
				done.await(10, TimeUnit.SECONDS);
				s.close();
			}
			catch (Exception e) {
				ReflectionUtils.rethrowRuntimeException(e);
			}
		});
		assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
		TcpNioClientConnectionFactory factory =
				new TcpNioClientConnectionFactory("localhost", serverSocket.get().getLocalPort());
		factory.setLookupHost(true);
		AtomicReference<String> connectionId = new AtomicReference<>();
		factory.setApplicationEventPublisher(event -> {
			if (event instanceof TcpConnectionOpenEvent tcpConnectionOpenEvent) {
				connectionId.set(tcpConnectionOpenEvent.getConnectionId());
			}
		});
		factory.setSoTimeout(100);
		factory.start();
		try {
			TcpConnection connection = factory.getConnection();
			connection.send(MessageBuilder.withPayload(new byte[1000000]).build());
		}
		catch (MessagingException e) {
			assertThat(e).hasCauseInstanceOf(SocketTimeoutException.class);
		}
		done.countDown();
		factory.stop();
		serverSocket.get().close();
		assertThat(connectionId.get()).startsWith("localhost");
	}

	@Test
	public void testReadTimeout(TestInfo testInfo) throws Exception {
		CountDownLatch latch = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(1);
		AtomicReference<ServerSocket> serverSocket = new AtomicReference<>();
		this.executor.execute(() -> {
			try {
				ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0);
				logger.debug(testInfo.getTestMethod().get().getName()
						+ " starting server for " + server.getLocalPort());
				serverSocket.set(server);
				latch.countDown();
				Socket socket = server.accept();
				byte[] b = new byte[6];
				readFully(socket.getInputStream(), b);
				// block to cause timeout on read.
				done.await(10, TimeUnit.SECONDS);
			}
			catch (Exception e) {
				ReflectionUtils.rethrowRuntimeException(e);
			}
		});
		assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
		TcpNioClientConnectionFactory factory =
				new TcpNioClientConnectionFactory("localhost", serverSocket.get().getLocalPort());
		factory.setApplicationEventPublisher(nullPublisher);
		factory.setSoTimeout(100);
		factory.start();
		TcpConnection connection = factory.getConnection();
		connection.send(new GenericMessage<>("Test"));
		with().pollInterval(Duration.ofMillis(10))
				.await()
				.atMost(Duration.ofSeconds(10))
				.until(() -> !connection.isOpen());

		done.countDown();
		factory.stop();
		serverSocket.get().close();
	}

	@Test
	public void testMemoryLeak(TestInfo testInfo) throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicReference<ServerSocket> serverSocket = new AtomicReference<>();
		this.executor.execute(() -> {
			try {
				ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0);
				logger.debug(testInfo.getTestMethod().get().getName()
						+ " starting server for " + server.getLocalPort());
				serverSocket.set(server);
				latch.countDown();
				Socket socket = server.accept();
				byte[] b = new byte[6];
				readFully(socket.getInputStream(), b);
			}
			catch (Exception e) {
				ReflectionUtils.rethrowRuntimeException(e);
			}
		});
		assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
		TcpNioClientConnectionFactory factory =
				new TcpNioClientConnectionFactory("localhost", serverSocket.get().getLocalPort());
		factory.setApplicationEventPublisher(nullPublisher);
		factory.setNioHarvestInterval(100);
		factory.start();
		TcpConnection connection = factory.getConnection();
		Map<SocketChannel, TcpNioConnection> connections = factory.getConnections();
		assertThat(connections).hasSize(1);
		connection.close();
		assertThat(!connection.isOpen()).isTrue();
		TestUtils.<Selector>getPropertyValue(factory, "selector").wakeup();
		await().atMost(Duration.ofSeconds(10)).until(connections::isEmpty);
		factory.stop();
		serverSocket.get().close();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testCleanup() {
		TcpNioClientConnectionFactory factory = new TcpNioClientConnectionFactory("localhost", 0);
		factory.setApplicationEventPublisher(nullPublisher);
		Map<SocketChannel, TcpNioConnection> connections = new HashMap<>();
		SocketChannel chan1 = mock();
		SocketChannel chan2 = mock();
		SocketChannel chan3 = mock();
		TcpNioConnection conn1 = mock();
		TcpNioConnection conn2 = mock();
		TcpNioConnection conn3 = mock();
		connections.put(chan1, conn1);
		connections.put(chan2, conn2);
		connections.put(chan3, conn3);
		willReturn(true).given(chan1).isOpen();
		willReturn(true).given(chan2).isOpen();
		willReturn(true).given(chan3).isOpen();
		Selector selector = mock();
		HashSet<SelectionKey> keys = new HashSet<>();
		when(selector.selectedKeys()).thenReturn(keys);
		factory.processNioSelections(1, selector, null, connections);
		assertThat(connections).hasSize(3); // all open

		DirectFieldAccessor factoryFieldAccessor = new DirectFieldAccessor(factory);

		willReturn(false).given(chan1).isOpen();
		factory.processNioSelections(1, selector, null, connections);
		assertThat(connections).hasSize(3); // interval didn't pass

		factoryFieldAccessor.setPropertyValue("nextCheckForClosedNioConnections", System.currentTimeMillis() - 10);

		factory.processNioSelections(1, selector, null, connections);
		assertThat(connections).hasSize(2); // first is closed

		willReturn(false).given(chan2).isOpen();
		factory.processNioSelections(1, selector, null, connections);
		assertThat(connections).hasSize(2); // interval didn't pass

		factoryFieldAccessor.setPropertyValue("nextCheckForClosedNioConnections", System.currentTimeMillis() - 10);

		factory.processNioSelections(1, selector, null, connections);
		assertThat(connections).hasSize(1); // second is closed

		willReturn(false).given(chan3).isOpen();
		factory.processNioSelections(1, selector, null, connections);
		assertThat(connections).hasSize(1); // interval didn't pass

		factoryFieldAccessor.setPropertyValue("nextCheckForClosedNioConnections", System.currentTimeMillis() - 10);

		factory.processNioSelections(1, selector, null, connections);
		assertThat(connections).isEmpty(); // third is closed

		assertThat(TestUtils.<Map<SocketChannel, TcpNioConnection>>getPropertyValue(factory, "connections"))
				.isEmpty();
	}

	@Test
	public void testInsufficientThreads() throws Exception {
		final ExecutorService exec = Executors.newFixedThreadPool(2);
		SocketChannel channel = mock();
		Socket socket = mock();
		Mockito.when(channel.socket()).thenReturn(socket);
		doAnswer(invocation -> {
			ByteBuffer buffer = invocation.getArgument(0);
			buffer.position(1);
			return 1;
		}).when(channel).read(Mockito.any(ByteBuffer.class));
		when(socket.getReceiveBufferSize()).thenReturn(1024);
		final TcpNioConnection connection = new TcpNioConnection(channel, false, false, nullPublisher, null);
		connection.setTaskExecutor(exec);
		connection.setPipeTimeout(200);
		Method method = TcpNioConnection.class.getDeclaredMethod("doRead");
		method.setAccessible(true);
		Future<@Nullable Object> future = exec.submit(() -> {
			// Nobody reading, should time out on 6th write.
			try {
				for (int i = 0; i < 6; i++) {
					method.invoke(connection);
				}
			}
			catch (Exception e) {
				logger.debug("Expected timeout", e);
				throw (Exception) e.getCause();
			}
			finally {
				connection.setPipeTimeout(15);
				connection.close();
			}
			return null;
		});

		assertThatExceptionOfType(ExecutionException.class)
				.isThrownBy(() -> future.get(10, TimeUnit.SECONDS))
				.withStackTraceContaining("Timed out waiting for buffer space");

		exec.shutdownNow();
	}

	@Test
	public void testSufficientThreads() throws Exception {
		ExecutorService exec = Executors.newFixedThreadPool(3);
		CountDownLatch messageLatch = new CountDownLatch(1);
		SocketChannel channel = mock();
		Socket socket = mock();
		Mockito.when(channel.socket()).thenReturn(socket);
		doAnswer(invocation -> {
			ByteBuffer buffer = invocation.getArgument(0);
			buffer.position(1025);
			buffer.put((byte) '\r');
			buffer.put((byte) '\n');
			return 1027;
		})
				.when(channel)
				.read(Mockito.any(ByteBuffer.class));
		TcpNioConnection connection = new TcpNioConnection(channel, false, false, null, null);
		connection.setTaskExecutor(exec);
		connection.registerListener(message -> messageLatch.countDown());
		TcpMessageMapper mapper = new TcpMessageMapper();
		mapper.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		connection.setMapper(mapper);
		connection.setDeserializer(new ByteArrayCrLfSerializer());
		Method method = TcpNioConnection.class.getDeclaredMethod("doRead");
		method.setAccessible(true);
		Future<@Nullable Object> future = exec.submit(() -> {
			for (int i = 0; i < 20; i++) {
				method.invoke(connection);
			}
			return null;
		});
		future.get(60, TimeUnit.SECONDS);
		assertThat(messageLatch.await(10, TimeUnit.SECONDS)).isTrue();

		exec.shutdownNow();
	}

	@Test
	public void testByteArrayRead() throws Exception {
		SocketChannel socketChannel = mock();
		Socket socket = mock();
		when(socketChannel.socket()).thenReturn(socket);
		TcpNioConnection connection = new TcpNioConnection(socketChannel, false, false, null, null);
		TcpNioConnection.ChannelInputStream stream =
				TestUtils.<TcpNioConnection.ChannelInputStream>getPropertyValue(connection, "channelInputStream");
		stream.write(ByteBuffer.wrap("foo".getBytes()));
		byte[] out = new byte[2];
		int n = stream.read(out);
		assertThat(n).isEqualTo(2);
		assertThat(new String(out)).isEqualTo("fo");
		out = new byte[2];
		n = stream.read(out);
		assertThat(n).isEqualTo(1);
		assertThat(new String(out)).isEqualTo("o\u0000");
	}

	@Test
	public void testByteArrayReadMulti() throws Exception {
		SocketChannel socketChannel = mock();
		Socket socket = mock();
		when(socketChannel.socket()).thenReturn(socket);
		TcpNioConnection connection = new TcpNioConnection(socketChannel, false, false, null, null);
		TcpNioConnection.ChannelInputStream stream =
				TestUtils.<TcpNioConnection.ChannelInputStream>getPropertyValue(connection, "channelInputStream");
		stream.write(ByteBuffer.wrap("foo".getBytes()));
		stream.write(ByteBuffer.wrap("bar".getBytes()));
		byte[] out = new byte[6];
		int n = stream.read(out);
		assertThat(n).isEqualTo(6);
		assertThat(new String(out)).isEqualTo("foobar");
	}

	@Test
	public void testByteArrayReadWithOffset() throws Exception {
		SocketChannel socketChannel = mock();
		Socket socket = mock();
		when(socketChannel.socket()).thenReturn(socket);
		TcpNioConnection connection = new TcpNioConnection(socketChannel, false, false, null, null);
		TcpNioConnection.ChannelInputStream stream =
				TestUtils.<TcpNioConnection.ChannelInputStream>getPropertyValue(connection, "channelInputStream");
		stream.write(ByteBuffer.wrap("foo".getBytes()));
		byte[] out = new byte[5];
		int n = stream.read(out, 1, 4);
		assertThat(n).isEqualTo(3);
		assertThat(new String(out)).isEqualTo("\u0000foo\u0000");
	}

	@Test
	public void testByteArrayReadWithBadArgs() throws Exception {
		SocketChannel socketChannel = mock();
		Socket socket = mock();
		when(socketChannel.socket()).thenReturn(socket);
		TcpNioConnection connection = new TcpNioConnection(socketChannel, false, false, null, null);
		TcpNioConnection.ChannelInputStream stream =
				TestUtils.<TcpNioConnection.ChannelInputStream>getPropertyValue(connection, "channelInputStream");
		stream.write(ByteBuffer.wrap("foo".getBytes()));
		byte[] out = new byte[5];

		assertThatExceptionOfType(IndexOutOfBoundsException.class)
				.isThrownBy(() -> stream.read(out, 1, 5));

		assertThatIllegalArgumentException()
				.isThrownBy(() -> stream.read(null, 1, 5));

		assertThat(stream.read(out, 0, 0)).isEqualTo(0);
		assertThat(stream.read(out)).isEqualTo(3);
	}

	@Test
	public void testByteArrayBlocksForZeroRead() throws Exception {
		SocketChannel socketChannel = mock();
		Socket socket = mock();
		when(socketChannel.socket()).thenReturn(socket);
		TcpNioConnection connection = new TcpNioConnection(socketChannel, false, false, null, null);
		final TcpNioConnection.ChannelInputStream stream =
				TestUtils.<TcpNioConnection.ChannelInputStream>getPropertyValue(connection, "channelInputStream");
		final CountDownLatch latch = new CountDownLatch(1);
		final byte[] out = new byte[4];
		this.executor.execute(() -> {
			try {
				stream.read(out);
			}
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			latch.countDown();
		});
		Thread.sleep(1000);
		assertThat(out[0]).isEqualTo((byte) 0x00);
		stream.write(ByteBuffer.wrap("foo".getBytes()));
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(new String(out)).isEqualTo("foo\u0000");
	}

	@Test
	public void transferHeaders() throws Exception {
		Socket inSocket = mock();
		SocketChannel inChannel = mock();
		when(inChannel.socket()).thenReturn(inSocket);

		TcpNioConnection inboundConnection = new TcpNioConnection(inChannel, true, false, nullPublisher, null);
		inboundConnection.setDeserializer(new MapJsonSerializer());
		MapMessageConverter inConverter = new MapMessageConverter();
		MessageConvertingTcpMessageMapper inMapper = new MessageConvertingTcpMessageMapper(inConverter);
		inboundConnection.setMapper(inMapper);
		final ByteArrayOutputStream written = new ByteArrayOutputStream();
		doAnswer((Answer<Integer>) invocation -> {
			ByteBuffer buff = invocation.getArgument(0);
			byte[] bytes = written.toByteArray();
			buff.put(bytes);
			return bytes.length;
		}).when(inChannel).read(any(ByteBuffer.class));

		Socket outSocket = mock();
		SocketChannel outChannel = mock();
		when(outChannel.socket()).thenReturn(outSocket);
		TcpNioConnection outboundConnection = new TcpNioConnection(outChannel, true, false, nullPublisher, null);
		doAnswer(invocation -> {
			ByteBuffer buff = invocation.getArgument(0);
			byte[] bytes = new byte[buff.limit()];
			buff.get(bytes);
			written.write(bytes);
			return null;
		}).when(outChannel).write(any(ByteBuffer.class));

		MapMessageConverter outConverter = new MapMessageConverter();
		outConverter.setHeaderNames("bar");
		MessageConvertingTcpMessageMapper outMapper = new MessageConvertingTcpMessageMapper(outConverter);
		outboundConnection.setMapper(outMapper);
		outboundConnection.setSerializer(new MapJsonSerializer());

		Message<String> message =
				MessageBuilder.withPayload("foo")
						.setHeader("bar", "baz")
						.build();
		outboundConnection.send(message);

		AtomicReference<Message<?>> inboundMessage = new AtomicReference<>();
		CountDownLatch latch = new CountDownLatch(1);
		TcpListener listener = message1 -> {
			inboundMessage.set(message1);
			latch.countDown();
		};
		inboundConnection.registerListener(listener);
		inboundConnection.readPacket();
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(inboundMessage.get()).isNotNull();
		assertThat(inboundMessage.get().getPayload()).isEqualTo("foo");
		assertThat(inboundMessage.get().getHeaders().get("bar")).isEqualTo("baz");
	}

	@Test
	public void testAssemblerUsesSecondaryExecutor() throws Exception {
		TcpNioServerConnectionFactory factory = newTcpNioServerConnectionFactory();
		factory.setApplicationEventPublisher(nullPublisher);
		CompositeExecutor compositeExec = compositeExecutor();

		factory.setSoTimeout(1000);
		factory.setTaskExecutor(compositeExec);
		final AtomicReference<String> threadName = new AtomicReference<>();
		final CountDownLatch latch = new CountDownLatch(1);
		factory.registerListener(message -> {
			if (!(message instanceof ErrorMessage)) {
				threadName.set(Thread.currentThread().getName());
				latch.countDown();
			}
		});
		factory.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		factory.afterPropertiesSet();
		factory.start();
		TestingUtilities.waitListening(factory, null);
		int port = factory.getPort();

		Socket socket = null;
		int n = 0;
		while (n++ < 100) {
			try {
				socket = SocketFactory.getDefault().createSocket("localhost", port);
				break;
			}
			catch (ConnectException e) {
			}
			Thread.sleep(100);
		}
		assertThat(n).as("Could not open socket to localhost:" + port).isLessThan(100);
		socket.getOutputStream().write("foo\r\n".getBytes());
		socket.close();

		assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
		assertThat(threadName.get()).contains("assembler");

		factory.stop();

		cleanupCompositeExecutor(compositeExec);
	}

	private static TcpNioServerConnectionFactory newTcpNioServerConnectionFactory() {
		TcpNioServerConnectionFactory tcpNioServerConnectionFactory = new TcpNioServerConnectionFactory(0);
		tcpNioServerConnectionFactory.setTaskScheduler(new SimpleAsyncTaskScheduler());
		return tcpNioServerConnectionFactory;
	}

	private static void cleanupCompositeExecutor(CompositeExecutor compositeExec) throws Exception {
		TestUtils.<DisposableBean>getPropertyValue(compositeExec, "primaryTaskExecutor").destroy();
		TestUtils.<DisposableBean>getPropertyValue(compositeExec, "secondaryTaskExecutor").destroy();
	}

	@Test
	public void testAllMessagesDelivered() throws Exception {
		final int numberOfSockets = 10;
		TcpNioServerConnectionFactory factory = new TcpNioServerConnectionFactory(0);
		factory.setTaskScheduler(mock());
		factory.setApplicationEventPublisher(nullPublisher);

		CompositeExecutor compositeExec = compositeExecutor();

		factory.setTaskExecutor(compositeExec);
		final CountDownLatch latch = new CountDownLatch(numberOfSockets * 4);
		factory.registerListener(message -> {
			if (!(message instanceof ErrorMessage)) {
				latch.countDown();
			}
		});
		factory.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		factory.afterPropertiesSet();
		factory.start();
		TestingUtilities.waitListening(factory, null);
		int port = factory.getPort();

		Socket[] sockets = new Socket[numberOfSockets];
		for (int i = 0; i < numberOfSockets; i++) {
			Socket socket = null;
			int n = 0;
			while (n++ < 100) {
				try {
					socket = SocketFactory.getDefault().createSocket("localhost", port);
					break;
				}
				catch (ConnectException e) {
				}
				Thread.sleep(1);
			}
			assertThat(n).as("Could not open socket to localhost:" + port).isLessThan(100);
			sockets[i] = socket;
		}
		for (int i = 0; i < numberOfSockets; i++) {
			sockets[i].getOutputStream().write("foo1 and...".getBytes());
			sockets[i].getOutputStream().flush();
		}
		Thread.sleep(1);
		for (int i = 0; i < numberOfSockets; i++) {
			sockets[i].getOutputStream().write(("...foo2\r\nbar1 and...").getBytes());
			sockets[i].getOutputStream().flush();
		}
		for (int i = 0; i < numberOfSockets; i++) {
			sockets[i].getOutputStream().write(("...bar2\r\n").getBytes());
			sockets[i].getOutputStream().flush();
		}
		for (int i = 0; i < numberOfSockets; i++) {
			sockets[i].getOutputStream().write("foo3 and...".getBytes());
			sockets[i].getOutputStream().flush();
		}
		Thread.sleep(1);
		for (int i = 0; i < numberOfSockets; i++) {
			sockets[i].getOutputStream().write(("...foo4\r\nbar3 and...").getBytes());
			sockets[i].getOutputStream().flush();
		}
		for (int i = 0; i < numberOfSockets; i++) {
			sockets[i].getOutputStream().write(("...bar4\r\n").getBytes());
			sockets[i].close();
		}

		assertThat(latch.await(60, TimeUnit.SECONDS)).as("latch is still " + latch.getCount()).isTrue();

		factory.stop();

		cleanupCompositeExecutor(compositeExec);
	}

	private static CompositeExecutor compositeExecutor() {
		ThreadPoolTaskExecutor ioExec = new ThreadPoolTaskExecutor();
		ioExec.setCorePoolSize(2);
		ioExec.setMaxPoolSize(4);
		ioExec.setQueueCapacity(0);
		ioExec.setThreadNamePrefix("io-");
		ioExec.setRejectedExecutionHandler(new AbortPolicy());
		ioExec.initialize();
		ThreadPoolTaskExecutor assemblerExec = new ThreadPoolTaskExecutor();
		assemblerExec.setCorePoolSize(2);
		assemblerExec.setMaxPoolSize(10);
		assemblerExec.setQueueCapacity(1000);
		assemblerExec.setThreadNamePrefix("assembler-");
		assemblerExec.setRejectedExecutionHandler(new AbortPolicy());
		assemblerExec.initialize();
		return new CompositeExecutor(ioExec, assemblerExec);
	}

	@Test
	public void nioAssemblerThreadIsReleased() throws Exception {
		TcpNioServerConnectionFactory factory = new TcpNioServerConnectionFactory(0);
		factory.setTaskScheduler(mock());
		final CountDownLatch connectionLatch = new CountDownLatch(1);
		factory.setApplicationEventPublisher(event -> {
			if (event instanceof TcpConnectionOpenEvent) {
				connectionLatch.countDown();
			}
		});
		final CountDownLatch assemblerLatch = new CountDownLatch(1);
		final AtomicReference<Thread> assembler = new AtomicReference<>();
		factory.registerListener(message -> {
			if (!(message instanceof ErrorMessage)) {
				assembler.set(Thread.currentThread());
				assemblerLatch.countDown();
			}
		});
		ThreadPoolTaskExecutor te = new ThreadPoolTaskExecutor();
		te.setCorePoolSize(3); // selector, reader, assembler
		te.setMaxPoolSize(3);
		te.setQueueCapacity(0);
		te.initialize();
		factory.setTaskExecutor(te);
		factory.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		factory.afterPropertiesSet();
		factory.start();
		TestingUtilities.waitListening(factory, 10000L);
		int port = factory.getPort();
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		assertThat(connectionLatch.await(10, TimeUnit.SECONDS)).isTrue();

		final CountDownLatch readerLatch = new CountDownLatch(4); // 3 dataAvailable, 1 continuing

		TcpNioConnection connection =
				TestUtils.<Map<SocketChannel, TcpNioConnection>>getPropertyValue(factory, "connections")
				.values().iterator().next();
		Log logger = spy(TestUtils.<Log>getPropertyValue(connection, "logger"));
		doReturn(true).when(logger).isTraceEnabled();
		doAnswer(invocation -> {
			invocation.callRealMethod();
			readerLatch.countDown();
			return null;
		}).when(logger).trace(contains("checking data avail"));

		doAnswer(invocation -> {
			invocation.callRealMethod();
			readerLatch.countDown();
			return null;
		}).when(logger).trace(contains("Nio assembler continuing"));
		DirectFieldAccessor dfa = new DirectFieldAccessor(connection);
		dfa.setPropertyValue("logger", logger);

		final CountDownLatch readerFinishedLatch = new CountDownLatch(1);
		ChannelInputStream cis =
				spy(TestUtils.<ChannelInputStream>getPropertyValue(connection, "channelInputStream"));
		doAnswer(invocation -> {
			invocation.callRealMethod();
			// delay the reader thread resetting writingToPipe
			readerLatch.await(10, TimeUnit.SECONDS);
			Thread.sleep(100);
			readerFinishedLatch.countDown();
			return null;
		}).when(cis).write(any(ByteBuffer.class));
		dfa.setPropertyValue("channelInputStream", cis);

		socket.getOutputStream().write("foo\r\n".getBytes());

		assertThat(assemblerLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(readerFinishedLatch.await(10, TimeUnit.SECONDS)).isTrue();

		StackTraceElement[] stackTrace = assembler.get().getStackTrace();
		assertThat(Arrays.asList(stackTrace).toString()).doesNotContain("ChannelInputStream.getNextBuffer");
		socket.close();
		factory.stop();

		te.shutdown();
	}

	@Test
	public void testNoDelayOnClose() throws Exception {
		TcpNioServerConnectionFactory cf = newTcpNioServerConnectionFactory();
		cf.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		final CountDownLatch reading = new CountDownLatch(1);
		final StopWatch watch = new StopWatch();
		cf.setDeserializer(is -> {
			reading.countDown();
			watch.start();
			is.read();
			is.read();
			watch.stop();
			return null;
		});
		cf.registerListener(m -> {
		});
		final CountDownLatch listening = new CountDownLatch(1);
		cf.setApplicationEventPublisher(e -> listening.countDown());
		cf.afterPropertiesSet();
		cf.start();
		assertThat(listening.await(10, TimeUnit.SECONDS)).isTrue();
		Socket socket = SocketFactory.getDefault().createSocket("localhost", cf.getPort());
		socket.getOutputStream().write("x".getBytes());
		assertThat(reading.await(10, TimeUnit.SECONDS)).isTrue();
		socket.close();
		cf.stop();
		assertThat(watch.lastTaskInfo().getTimeMillis()).isLessThan(950L);
	}

	@Test
	public void testMultiAccept() throws InterruptedException, IOException {
		testMulti(true);
	}

	@Test
	public void testNoMultiAccept() throws InterruptedException, IOException {
		testMulti(false);
	}

	@Test
	public void testWritingLatchClearedAfterRead() throws Exception {
		SocketChannel channel = mock();
		Socket socket = mock();
		when(channel.socket()).thenReturn(socket);
		when(socket.getReceiveBufferSize()).thenReturn(1024);

		doAnswer(invocation -> {
			ByteBuffer buffer = invocation.getArgument(0);
			buffer.put("foo".getBytes());
			return 3;
		}).when(channel).read(Mockito.any(ByteBuffer.class));

		TcpNioConnection connection = new TcpNioConnection(channel, false, false, this.nullPublisher, null);

		CompositeExecutor compositeExec = compositeExecutor();
		connection.setTaskExecutor(compositeExec);

		DirectFieldAccessor dfa = new DirectFieldAccessor(connection);

		ChannelInputStream originalStream =
				TestUtils.<TcpNioConnection.ChannelInputStream>getPropertyValue(connection, "channelInputStream");
		assertThat(originalStream).isNotNull();

		ChannelInputStream streamSpy = spy(originalStream);
		dfa.setPropertyValue("channelInputStream", streamSpy);

		AtomicReference<CountDownLatch> latchSeenDuringWrite = new AtomicReference<>();

		doAnswer(invocation -> {
			CountDownLatch currentLatch = (CountDownLatch) dfa.getPropertyValue("writingLatch");
			if (currentLatch != null) {
				latchSeenDuringWrite.compareAndSet(null, currentLatch);
			}
			return invocation.callRealMethod();
		}).when(streamSpy).write(any(ByteBuffer.class));

		Method doRead = TcpNioConnection.class.getDeclaredMethod("doRead");
		doRead.setAccessible(true);

		doRead.invoke(connection);

		assertThat(latchSeenDuringWrite.get())
				.as("writingLatch should be non-null while data is written to the pipe")
				.isNotNull();

		CountDownLatch writingLatchAfterRead = (CountDownLatch) dfa.getPropertyValue("writingLatch");
		assertThat(writingLatchAfterRead)
				.as("writingLatch must be null after a completed read cycle")
				.isNull();

		connection.close();

		cleanupCompositeExecutor(compositeExec);
	}

	@Test
	public void delayedReadsDueToRejectedExecutionException() throws InterruptedException {
		CountDownLatch delayReadLatch = new CountDownLatch(1);
		TcpNioClientConnectionFactory factory = new TcpNioClientConnectionFactory("localhost", 0) {

			@Override
			protected void delayRead(Selector selector, long now, SelectionKey key) {
				// Means RejectedExecutionException was propagated from the TcpNioConnection.readPacket()
				delayReadLatch.countDown();
			}

		};

		ThreadPoolExecutor threadPoolExecutor =
				new ThreadPoolExecutor(1, 2, 0, TimeUnit.MILLISECONDS, new SynchronousQueue<>());
		factory.setTaskExecutor(threadPoolExecutor);
		factory.start();
		SocketChannel chan1 = mock();
		willReturn(true).given(chan1).isOpen();
		when(chan1.socket()).thenReturn(mock());
		TcpNioConnection conn1 = new TcpNioConnection(chan1, false, false, null, null);
		conn1.setTaskExecutor(threadPoolExecutor);
		conn1 = spy(conn1);
		AtomicReference<RejectedExecutionException> reeReference = new AtomicReference<>();
		doAnswer(invocation -> {
			try {
				return invocation.callRealMethod();
			}
			catch (RejectedExecutionException ree) {
				reeReference.set(ree);
				throw ree;
			}
		})
				.when(conn1)
				.readPacket();

		Map<SocketChannel, TcpNioConnection> connections = Map.of(chan1, conn1);
		SelectionKey key1 = mock();
		willReturn(true).given(key1).isReadable();
		willReturn(true).given(key1).isValid();
		willReturn(conn1).given(key1).attachment();
		Set<SelectionKey> keys = new HashSet<>();
		keys.add(key1);
		Selector selector = mock();
		when(selector.selectedKeys()).thenReturn(keys);
		factory.processNioSelections(1, selector, null, connections);
		assertThat(delayReadLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(reeReference.get()).isNotNull();
		threadPoolExecutor.shutdown();
	}

	private static void testMulti(boolean multiAccept) throws InterruptedException, IOException {
		CountDownLatch serverReadyLatch = new CountDownLatch(1);
		CountDownLatch latch = new CountDownLatch(21);
		List<Socket> sockets = new ArrayList<>();
		TcpNioServerConnectionFactory server = newTcpNioServerConnectionFactory();

		try {
			List<IpIntegrationEvent> events = Collections.synchronizedList(new ArrayList<>());
			List<Message<?>> messages = Collections.synchronizedList(new ArrayList<>());
			server.setMultiAccept(multiAccept);
			server.setApplicationEventPublisher(e -> {
				if (e instanceof TcpConnectionServerListeningEvent) {
					serverReadyLatch.countDown();
				}
				events.add((IpIntegrationEvent) e);
				latch.countDown();
			});
			server.registerListener(m -> {
				messages.add(m);
				latch.countDown();
			});
			server.setBeanFactory(TEST_INTEGRATION_CONTEXT);
			server.afterPropertiesSet();
			server.start();
			assertThat(serverReadyLatch.await(10, TimeUnit.SECONDS)).isTrue();
			InetAddress localHost = InetAddress.getLocalHost();
			for (int i = 0; i < 10; i++) {
				Socket socket = SocketFactory.getDefault().createSocket(localHost, server.getPort());
				socket.getOutputStream().write("foo\r\n".getBytes());
				sockets.add(socket);
			}
			assertThat(latch.await(10, TimeUnit.SECONDS));
			assertThat(events).hasSize(11); // server ready + 10 opens
			assertThat(messages).hasSize(10);
		}
		finally {
			for (Socket socket : sockets) {
				socket.close();
			}
			server.stop();
		}
	}

	private static void readFully(InputStream is, byte[] buff) throws IOException {
		for (int i = 0; i < buff.length; i++) {
			buff[i] = (byte) is.read();
		}
	}

}
