/*
 * Copyright 2002-2018 the original author or authors.
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
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.integration.ip.tcp.connection.TcpNioConnection.ChannelInputStream;
import org.springframework.integration.ip.tcp.serializer.ByteArrayCrLfSerializer;
import org.springframework.integration.ip.tcp.serializer.MapJsonSerializer;
import org.springframework.integration.ip.util.TestingUtilities;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.converter.MapMessageConverter;
import org.springframework.integration.test.rule.Log4j2LevelAdjuster;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.util.CompositeExecutor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StopWatch;


/**
 * @author Gary Russell
 * @author John Anderson
 * @author Artem Bilan
 *
 * @since 2.0
 *
 */
public class TcpNioConnectionTests {

	private final static Log logger = LogFactory.getLog(TcpNioConnectionTests.class);

	@Rule
	public Log4j2LevelAdjuster adjuster =
			Log4j2LevelAdjuster.trace()
					.categories("org.springframework.integration.ip.tcp");

	@Rule
	public TestName testName = new TestName();

	private final ApplicationEventPublisher nullPublisher = mock(ApplicationEventPublisher.class);

	private final AsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();

	@Test
	public void testWriteTimeout() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		final CountDownLatch done = new CountDownLatch(1);
		final AtomicReference<ServerSocket> serverSocket = new AtomicReference<>();
		this.executor.execute(() -> {
			try {
				ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0);
				logger.debug(testName.getMethodName() + " starting server for " + server.getLocalPort());
				serverSocket.set(server);
				latch.countDown();
				Socket s = server.accept();
				// block so we fill the buffer
				done.await(10, TimeUnit.SECONDS);
				s.close();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		});
		assertTrue(latch.await(10000, TimeUnit.MILLISECONDS));
		TcpNioClientConnectionFactory factory = new TcpNioClientConnectionFactory("localhost",
				serverSocket.get().getLocalPort());
		factory.setApplicationEventPublisher(nullPublisher);
		factory.setSoTimeout(100);
		factory.start();
		try {
			TcpConnection connection = factory.getConnection();
			connection.send(MessageBuilder.withPayload(new byte[1000000]).build());
		}
		catch (Exception e) {
			assertTrue("Expected SocketTimeoutException, got " + e.getClass().getSimpleName() +
					":" + e.getMessage(), e instanceof SocketTimeoutException);
		}
		done.countDown();
		factory.stop();
		serverSocket.get().close();
	}

	@Test
	public void testReadTimeout() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		final CountDownLatch done = new CountDownLatch(1);
		final AtomicReference<ServerSocket> serverSocket = new AtomicReference<>();
		this.executor.execute(() -> {
			try {
				ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0);
				logger.debug(testName.getMethodName() + " starting server for " + server.getLocalPort());
				serverSocket.set(server);
				latch.countDown();
				Socket socket = server.accept();
				byte[] b = new byte[6];
				readFully(socket.getInputStream(), b);
				// block to cause timeout on read.
				done.await(10, TimeUnit.SECONDS);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		});
		assertTrue(latch.await(10000, TimeUnit.MILLISECONDS));
		TcpNioClientConnectionFactory factory = new TcpNioClientConnectionFactory("localhost",
				serverSocket.get().getLocalPort());
		factory.setApplicationEventPublisher(nullPublisher);
		factory.setSoTimeout(100);
		factory.start();
		try {
			TcpConnection connection = factory.getConnection();
			connection.send(MessageBuilder.withPayload("Test").build());
			int n = 0;
			while (connection.isOpen()) {
				Thread.sleep(10);
				if (n++ > 200) {
					break;
				}
			}
			assertTrue(!connection.isOpen());
		}
		catch (Exception e) {
			fail("Unexpected exception " + e);
		}
		done.countDown();
		factory.stop();
		serverSocket.get().close();
	}

	@Test
	public void testMemoryLeak() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicReference<ServerSocket> serverSocket = new AtomicReference<>();
		this.executor.execute(() -> {
			try {
				ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0);
				logger.debug(testName.getMethodName() + " starting server for " + server.getLocalPort());
				serverSocket.set(server);
				latch.countDown();
				Socket socket = server.accept();
				byte[] b = new byte[6];
				readFully(socket.getInputStream(), b);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		});
		assertTrue(latch.await(10000, TimeUnit.MILLISECONDS));
		TcpNioClientConnectionFactory factory = new TcpNioClientConnectionFactory("localhost",
				serverSocket.get().getLocalPort());
		factory.setApplicationEventPublisher(nullPublisher);
		factory.setNioHarvestInterval(100);
		factory.start();
		try {
			TcpConnection connection = factory.getConnection();
			Map<SocketChannel, TcpNioConnection> connections = factory.getConnections();
			assertEquals(1, connections.size());
			connection.close();
			assertTrue(!connection.isOpen());
			TestUtils.getPropertyValue(factory, "selector", Selector.class).wakeup();
			int n = 0;
			while (connections.size() > 0) {
				Thread.sleep(100);
				if (n++ > 100) {
					break;
				}
			}
			assertEquals(0, connections.size());
		}
		catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception " + e);
		}
		factory.stop();
		serverSocket.get().close();
	}

	@Test
	public void testCleanup() throws Exception {
		TcpNioClientConnectionFactory factory = new TcpNioClientConnectionFactory("localhost", 0);
		factory.setApplicationEventPublisher(nullPublisher);
		factory.setNioHarvestInterval(100);
		Map<SocketChannel, TcpNioConnection> connections = new HashMap<>();
		SocketChannel chan1 = mock(SocketChannel.class);
		SocketChannel chan2 = mock(SocketChannel.class);
		SocketChannel chan3 = mock(SocketChannel.class);
		TcpNioConnection conn1 = mock(TcpNioConnection.class);
		TcpNioConnection conn2 = mock(TcpNioConnection.class);
		TcpNioConnection conn3 = mock(TcpNioConnection.class);
		connections.put(chan1, conn1);
		connections.put(chan2, conn2);
		connections.put(chan3, conn3);
		final List<Field> fields = new ArrayList<Field>();
		ReflectionUtils.doWithFields(SocketChannel.class, field -> {
			field.setAccessible(true);
			fields.add(field);
		}, field -> field.getName().equals("open"));
		Field field = fields.get(0);
		// Can't use Mockito because isOpen() is final
		ReflectionUtils.setField(field, chan1, true);
		ReflectionUtils.setField(field, chan2, true);
		ReflectionUtils.setField(field, chan3, true);
		Selector selector = mock(Selector.class);
		HashSet<SelectionKey> keys = new HashSet<SelectionKey>();
		when(selector.selectedKeys()).thenReturn(keys);
		factory.processNioSelections(1, selector, null, connections);
		assertEquals(3, connections.size()); // all open

		ReflectionUtils.setField(field, chan1, false);
		factory.processNioSelections(1, selector, null, connections);
		assertEquals(3, connections.size()); // interval didn't pass
		Thread.sleep(110);
		factory.processNioSelections(1, selector, null, connections);
		assertEquals(2, connections.size()); // first is closed

		ReflectionUtils.setField(field, chan2, false);
		factory.processNioSelections(1, selector, null, connections);
		assertEquals(2, connections.size()); // interval didn't pass
		Thread.sleep(110);
		factory.processNioSelections(1, selector, null, connections);
		assertEquals(1, connections.size()); // second is closed

		ReflectionUtils.setField(field, chan3, false);
		factory.processNioSelections(1, selector, null, connections);
		assertEquals(1, connections.size()); // interval didn't pass
		Thread.sleep(110);
		factory.processNioSelections(1, selector, null, connections);
		assertEquals(0, connections.size()); // third is closed

		assertEquals(0, TestUtils.getPropertyValue(factory, "connections", Map.class).size());
	}

	@Test
	public void testInsufficientThreads() throws Exception {
		final ExecutorService exec = Executors.newFixedThreadPool(2);
		Future<Object> future = exec.submit(() -> {
			SocketChannel channel = mock(SocketChannel.class);
			Socket socket = mock(Socket.class);
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
			// Nobody reading, should timeout on 6th write.
			try {
				for (int i = 0; i < 6; i++) {
					method.invoke(connection);
				}
			}
			catch (Exception e) {
				e.printStackTrace();
				throw (Exception) e.getCause();
			}
			return null;
		});
		try {
			Object o = future.get(10, TimeUnit.SECONDS);
			fail("Expected exception, got " + o);
		}
		catch (ExecutionException e) {
			assertEquals("Timed out waiting for buffer space", e.getCause().getMessage());
		}
		finally {
			exec.shutdownNow();
		}
	}

	@Test
	public void testSufficientThreads() throws Exception {
		final ExecutorService exec = Executors.newFixedThreadPool(3);
		final CountDownLatch messageLatch = new CountDownLatch(1);
		Future<Object> future = exec.submit(() -> {
			SocketChannel channel = mock(SocketChannel.class);
			Socket socket = mock(Socket.class);
			Mockito.when(channel.socket()).thenReturn(socket);
			doAnswer(invocation -> {
				ByteBuffer buffer = invocation.getArgument(0);
				buffer.position(1025);
				buffer.put((byte) '\r');
				buffer.put((byte) '\n');
				return 1027;
			}).when(channel).read(Mockito.any(ByteBuffer.class));
			final TcpNioConnection connection = new TcpNioConnection(channel, false, false,
					null, null);
			connection.setTaskExecutor(exec);
			connection.registerListener(message -> {
				messageLatch.countDown();
				return false;
			});
			connection.setMapper(new TcpMessageMapper());
			connection.setDeserializer(new ByteArrayCrLfSerializer());
			Method method = TcpNioConnection.class.getDeclaredMethod("doRead");
			method.setAccessible(true);
			try {
				for (int i = 0; i < 20; i++) {
					method.invoke(connection);
				}
			}
			catch (Exception e) {
				e.printStackTrace();
				throw (Exception) e.getCause();
			}
			return null;
		});
		future.get(60, TimeUnit.SECONDS);
		assertTrue(messageLatch.await(10, TimeUnit.SECONDS));

		exec.shutdownNow();
	}

	@Test
	public void testByteArrayRead() throws Exception {
		SocketChannel socketChannel = mock(SocketChannel.class);
		Socket socket = mock(Socket.class);
		when(socketChannel.socket()).thenReturn(socket);
		TcpNioConnection connection = new TcpNioConnection(socketChannel, false, false, null, null);
		TcpNioConnection.ChannelInputStream stream = (ChannelInputStream) new DirectFieldAccessor(connection)
				.getPropertyValue("channelInputStream");
		stream.write(ByteBuffer.wrap("foo".getBytes()));
		byte[] out = new byte[2];
		int n = stream.read(out);
		assertEquals(2, n);
		assertEquals("fo", new String(out));
		out = new byte[2];
		n = stream.read(out);
		assertEquals(1, n);
		assertEquals("o\u0000", new String(out));
	}

	@Test
	public void testByteArrayReadMulti() throws Exception {
		SocketChannel socketChannel = mock(SocketChannel.class);
		Socket socket = mock(Socket.class);
		when(socketChannel.socket()).thenReturn(socket);
		TcpNioConnection connection = new TcpNioConnection(socketChannel, false, false, null, null);
		TcpNioConnection.ChannelInputStream stream = (ChannelInputStream) new DirectFieldAccessor(connection)
				.getPropertyValue("channelInputStream");
		stream.write(ByteBuffer.wrap("foo".getBytes()));
		stream.write(ByteBuffer.wrap("bar".getBytes()));
		byte[] out = new byte[6];
		int n = stream.read(out);
		assertEquals(6, n);
		assertEquals("foobar", new String(out));
	}

	@Test
	public void testByteArrayReadWithOffset() throws Exception {
		SocketChannel socketChannel = mock(SocketChannel.class);
		Socket socket = mock(Socket.class);
		when(socketChannel.socket()).thenReturn(socket);
		TcpNioConnection connection = new TcpNioConnection(socketChannel, false, false, null, null);
		TcpNioConnection.ChannelInputStream stream = (ChannelInputStream) new DirectFieldAccessor(connection)
				.getPropertyValue("channelInputStream");
		stream.write(ByteBuffer.wrap("foo".getBytes()));
		byte[] out = new byte[5];
		int n = stream.read(out, 1, 4);
		assertEquals(3, n);
		assertEquals("\u0000foo\u0000", new String(out));
	}

	@Test
	public void testByteArrayReadWithBadArgs() throws Exception {
		SocketChannel socketChannel = mock(SocketChannel.class);
		Socket socket = mock(Socket.class);
		when(socketChannel.socket()).thenReturn(socket);
		TcpNioConnection connection = new TcpNioConnection(socketChannel, false, false, null, null);
		TcpNioConnection.ChannelInputStream stream = (ChannelInputStream) new DirectFieldAccessor(connection)
				.getPropertyValue("channelInputStream");
		stream.write(ByteBuffer.wrap("foo".getBytes()));
		byte[] out = new byte[5];
		try {
			stream.read(out, 1, 5);
			fail("Expected IndexOutOfBoundsException");
		}
		catch (IndexOutOfBoundsException e) {
		}
		try {
			stream.read(null, 1, 5);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
		}
		assertEquals(0, stream.read(out, 0, 0));
		assertEquals(3, stream.read(out));
	}

	@Test
	public void testByteArrayBlocksForZeroRead() throws Exception {
		SocketChannel socketChannel = mock(SocketChannel.class);
		Socket socket = mock(Socket.class);
		when(socketChannel.socket()).thenReturn(socket);
		TcpNioConnection connection = new TcpNioConnection(socketChannel, false, false, null, null);
		final TcpNioConnection.ChannelInputStream stream = (ChannelInputStream) new DirectFieldAccessor(connection)
				.getPropertyValue("channelInputStream");
		final CountDownLatch latch = new CountDownLatch(1);
		final byte[] out = new byte[4];
		this.executor.execute(() -> {
			try {
				stream.read(out);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			latch.countDown();
		});
		Thread.sleep(1000);
		assertEquals(0x00, out[0]);
		stream.write(ByteBuffer.wrap("foo".getBytes()));
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		assertEquals("foo\u0000", new String(out));
	}

	@Test
	public void transferHeaders() throws Exception {
		Socket inSocket = mock(Socket.class);
		SocketChannel inChannel = mock(SocketChannel.class);
		when(inChannel.socket()).thenReturn(inSocket);

		TcpNioConnection inboundConnection = new TcpNioConnection(inChannel, true, false, nullPublisher, null);
		inboundConnection.setDeserializer(new MapJsonSerializer());
		MapMessageConverter inConverter = new MapMessageConverter();
		MessageConvertingTcpMessageMapper inMapper = new MessageConvertingTcpMessageMapper(inConverter);
		inboundConnection.setMapper(inMapper);
		final ByteArrayOutputStream written = new ByteArrayOutputStream();
		doAnswer(new Answer<Integer>() {

			@Override
			public Integer answer(InvocationOnMock invocation) throws Throwable {
				ByteBuffer buff = invocation.getArgument(0);
				byte[] bytes = written.toByteArray();
				buff.put(bytes);
				return bytes.length;
			}
		}).when(inChannel).read(any(ByteBuffer.class));

		Socket outSocket = mock(Socket.class);
		SocketChannel outChannel = mock(SocketChannel.class);
		when(outChannel.socket()).thenReturn(outSocket);
		TcpNioConnection outboundConnection = new TcpNioConnection(outChannel, true, false, nullPublisher, null);
		doAnswer(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				ByteBuffer buff = invocation.getArgument(0);
				byte[] bytes = new byte[buff.limit()];
				buff.get(bytes);
				written.write(bytes);
				return null;
			}
		}).when(outChannel).write(any(ByteBuffer.class));

		MapMessageConverter outConverter = new MapMessageConverter();
		outConverter.setHeaderNames("bar");
		MessageConvertingTcpMessageMapper outMapper = new MessageConvertingTcpMessageMapper(outConverter);
		outboundConnection.setMapper(outMapper);
		outboundConnection.setSerializer(new MapJsonSerializer());

		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader("bar", "baz")
				.build();
		outboundConnection.send(message);

		final AtomicReference<Message<?>> inboundMessage = new AtomicReference<Message<?>>();
		final CountDownLatch latch = new CountDownLatch(1);
		TcpListener listener = new TcpListener() {

			@Override
			public boolean onMessage(Message<?> message) {
				inboundMessage.set(message);
				latch.countDown();
				return false;
			}
		};
		inboundConnection.registerListener(listener);
		inboundConnection.readPacket();
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		assertNotNull(inboundMessage.get());
		assertEquals("foo", inboundMessage.get().getPayload());
		assertEquals("baz", inboundMessage.get().getHeaders().get("bar"));
	}

	@Test
	public void testAssemblerUsesSecondaryExecutor() throws Exception {
		TcpNioServerConnectionFactory factory = new TcpNioServerConnectionFactory(0);
		factory.setApplicationEventPublisher(nullPublisher);

		CompositeExecutor compositeExec = compositeExecutor();

		factory.setSoTimeout(1000);
		factory.setTaskExecutor(compositeExec);
		final AtomicReference<String> threadName = new AtomicReference<String>();
		final CountDownLatch latch = new CountDownLatch(1);
		factory.registerListener(new TcpListener() {

			@Override
			public boolean onMessage(Message<?> message) {
				if (!(message instanceof ErrorMessage)) {
					threadName.set(Thread.currentThread().getName());
					latch.countDown();
				}
				return false;
			}

		});
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
		assertTrue("Could not open socket to localhost:" + port, n < 100);
		socket.getOutputStream().write("foo\r\n".getBytes());
		socket.close();

		assertTrue(latch.await(10, TimeUnit.SECONDS));
		assertThat(threadName.get(), containsString("assembler"));

		factory.stop();

		cleanupCompositeExecutor(compositeExec);
	}

	private void cleanupCompositeExecutor(CompositeExecutor compositeExec) throws Exception {
		TestUtils.getPropertyValue(compositeExec, "primaryTaskExecutor", DisposableBean.class).destroy();
		TestUtils.getPropertyValue(compositeExec, "secondaryTaskExecutor", DisposableBean.class).destroy();
	}

	@Test
	public void testAllMessagesDelivered() throws Exception {
		final int numberOfSockets = 10;
		TcpNioServerConnectionFactory factory = new TcpNioServerConnectionFactory(0);
		factory.setApplicationEventPublisher(nullPublisher);

		CompositeExecutor compositeExec = compositeExecutor();

		factory.setTaskExecutor(compositeExec);
		final CountDownLatch latch = new CountDownLatch(numberOfSockets * 4);
		factory.registerListener(message -> {
			if (!(message instanceof ErrorMessage)) {
				latch.countDown();
			}
			return false;
		});
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
			assertTrue("Could not open socket to localhost:" + port, n < 100);
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

		assertTrue("latch is still " + latch.getCount(), latch.await(60, TimeUnit.SECONDS));

		factory.stop();

		cleanupCompositeExecutor(compositeExec);
	}

	private CompositeExecutor compositeExecutor() {
		ThreadPoolTaskExecutor ioExec = new ThreadPoolTaskExecutor();
		ioExec.setCorePoolSize(2);
		ioExec.setMaxPoolSize(4);
		ioExec.setQueueCapacity(0);
		ioExec.setThreadNamePrefix("io-");
		ioExec.setRejectedExecutionHandler(new AbortPolicy());
		ioExec.initialize();
		ThreadPoolTaskExecutor assemblerExec = new ThreadPoolTaskExecutor();
		assemblerExec.setCorePoolSize(2);
		assemblerExec.setMaxPoolSize(5);
		assemblerExec.setQueueCapacity(0);
		assemblerExec.setThreadNamePrefix("assembler-");
		assemblerExec.setRejectedExecutionHandler(new AbortPolicy());
		assemblerExec.initialize();
		return new CompositeExecutor(ioExec, assemblerExec);
	}

	@Test
	public void int3453RaceTest() throws Exception {
		TcpNioServerConnectionFactory factory = new TcpNioServerConnectionFactory(0);
		final CountDownLatch connectionLatch = new CountDownLatch(1);
		factory.setApplicationEventPublisher(new ApplicationEventPublisher() {

			@Override
			public void publishEvent(ApplicationEvent event) {
				if (event instanceof TcpConnectionOpenEvent) {
					connectionLatch.countDown();
				}
			}

			@Override
			public void publishEvent(Object event) {

			}

		});
		final CountDownLatch assemblerLatch = new CountDownLatch(1);
		final AtomicReference<Thread> assembler = new AtomicReference<Thread>();
		factory.registerListener(new TcpListener() {

			@Override
			public boolean onMessage(Message<?> message) {
				if (!(message instanceof ErrorMessage)) {
					assembler.set(Thread.currentThread());
					assemblerLatch.countDown();
				}
				return false;
			}

		});
		ThreadPoolTaskExecutor te = new ThreadPoolTaskExecutor();
		te.setCorePoolSize(3); // selector, reader, assembler
		te.setMaxPoolSize(3);
		te.setQueueCapacity(0);
		te.initialize();
		factory.setTaskExecutor(te);
		factory.start();
		TestingUtilities.waitListening(factory, 10000L);
		int port = factory.getPort();
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		assertTrue(connectionLatch.await(10, TimeUnit.SECONDS));

		TcpNioConnection connection = (TcpNioConnection) TestUtils.getPropertyValue(factory, "connections", Map.class)
				.values().iterator().next();
		Log logger = spy(TestUtils.getPropertyValue(connection, "logger", Log.class));
		DirectFieldAccessor dfa = new DirectFieldAccessor(connection);
		dfa.setPropertyValue("logger", logger);

		ChannelInputStream cis = spy(TestUtils.getPropertyValue(connection, "channelInputStream", ChannelInputStream.class));
		dfa.setPropertyValue("channelInputStream", cis);

		final CountDownLatch readerLatch = new CountDownLatch(4); // 3 dataAvailable, 1 continuing
		final CountDownLatch readerFinishedLatch = new CountDownLatch(1);
		doAnswer(new Answer<Void>() {

			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				invocation.callRealMethod();
				// delay the reader thread resetting writingToPipe
				readerLatch.await(10, TimeUnit.SECONDS);
				Thread.sleep(100);
				readerFinishedLatch.countDown();
				return null;
			}
		}).when(cis).write(any(ByteBuffer.class));

		doReturn(true).when(logger).isTraceEnabled();
		doAnswer(new Answer<Void>() {

			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				invocation.callRealMethod();
				readerLatch.countDown();
				return null;
			}
		}).when(logger).trace(contains("checking data avail"));

		doAnswer(new Answer<Void>() {

			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				invocation.callRealMethod();
				readerLatch.countDown();
				return null;
			}
		}).when(logger).trace(contains("Nio assembler continuing"));

		socket.getOutputStream().write("foo\r\n".getBytes());

		assertTrue(assemblerLatch.await(10, TimeUnit.SECONDS));
		assertTrue(readerFinishedLatch.await(10, TimeUnit.SECONDS));

		StackTraceElement[] stackTrace = assembler.get().getStackTrace();
		assertThat(Arrays.asList(stackTrace).toString(), not(containsString("ChannelInputStream.getNextBuffer")));
		socket.close();
		factory.stop();

		te.shutdown();
	}

	@Test
	@Ignore // Timing is too short for CI/Travis
	public void testNoDelayOnClose() throws Exception {
		TcpNioServerConnectionFactory cf = new TcpNioServerConnectionFactory(0);
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
		cf.registerListener(m -> false);
		final CountDownLatch listening = new CountDownLatch(1);
		cf.setApplicationEventPublisher(e -> {
			listening.countDown();
		});
		cf.afterPropertiesSet();
		cf.start();
		assertTrue(listening.await(10, TimeUnit.SECONDS));
		Socket socket = SocketFactory.getDefault().createSocket("localhost", cf.getPort());
		socket.getOutputStream().write("x".getBytes());
		assertTrue(reading.await(10, TimeUnit.SECONDS));
		socket.close();
		cf.stop();
		assertThat(watch.getLastTaskTimeMillis(), lessThan(950L));
	}

	private void readFully(InputStream is, byte[] buff) throws IOException {
		for (int i = 0; i < buff.length; i++) {
			buff[i] = (byte) is.read();
		}
	}

}
