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
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.serializer.DefaultDeserializer;
import org.springframework.core.serializer.DefaultSerializer;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.ConsumerEndpointFactoryBean;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.HelloWorldInterceptor;
import org.springframework.integration.ip.tcp.connection.TcpConnection;
import org.springframework.integration.ip.tcp.connection.TcpConnectionCloseEvent;
import org.springframework.integration.ip.tcp.connection.TcpConnectionInterceptorFactory;
import org.springframework.integration.ip.tcp.connection.TcpConnectionInterceptorFactoryChain;
import org.springframework.integration.ip.tcp.connection.TcpConnectionOpenEvent;
import org.springframework.integration.ip.tcp.connection.TcpNetClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNetServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNioClientConnectionFactory;
import org.springframework.integration.ip.tcp.serializer.ByteArrayCrLfSerializer;
import org.springframework.integration.ip.tcp.serializer.ByteArrayLengthHeaderSerializer;
import org.springframework.integration.ip.tcp.serializer.ByteArrayStxEtxSerializer;
import org.springframework.integration.ip.util.TestingUtilities;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;


/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Mário Dias
 *
 * @since 2.0
 */
public class TcpSendingMessageHandlerTests extends AbstractTcpChannelAdapterTests {

	private static final Log logger = LogFactory.getLog(TcpSendingMessageHandlerTests.class);

	private final AsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("TcpSendingMessageHandlerTests-");

	private void readFully(InputStream is, byte[] buff) throws IOException {
		for (int i = 0; i < buff.length; i++) {
			buff[i] = (byte) is.read();
		}
	}

	@Test
	public void testNetCrLf() throws Exception {
		final AtomicReference<ServerSocket> serverSocket = new AtomicReference<ServerSocket>();
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean done = new AtomicBoolean();
		this.executor.execute(() -> {
			try {
				ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0);
				serverSocket.set(server);
				latch.countDown();
				Socket socket = server.accept();
				int i = 0;
				while (true) {
					byte[] b = new byte[6];
					readFully(socket.getInputStream(), b);
					b = ("Reply" + (++i) + "\r\n").getBytes();
					socket.getOutputStream().write(b);
				}
			}
			catch (Exception e) {
				if (!done.get()) {
					e.printStackTrace();
				}
			}
		});
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		AbstractConnectionFactory ccf = new TcpNetClientConnectionFactory("localhost",
				serverSocket.get().getLocalPort());
		noopPublisher(ccf);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		ccf.setSerializer(serializer);
		ccf.setDeserializer(serializer);
		ccf.setSoTimeout(10000);
		ccf.start();
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(ccf);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(ccf);
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		Message<?> mOut = channel.receive(10000);
		assertThat(mOut).isNotNull();
		assertThat(new String((byte[]) mOut.getPayload())).isEqualTo("Reply1");
		mOut = channel.receive(10000);
		assertThat(mOut).isNotNull();
		assertThat(new String((byte[]) mOut.getPayload())).isEqualTo("Reply2");
		done.set(true);
		ccf.stop();
		serverSocket.get().close();
	}

	@Test
	public void testNetCrLfClientMode() throws Exception {
		final AtomicReference<ServerSocket> serverSocket = new AtomicReference<ServerSocket>();
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean done = new AtomicBoolean();
		this.executor.execute(() -> {
			try {
				ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0);
				serverSocket.set(server);
				latch.countDown();
				Socket socket = server.accept();
				int i = 0;
				while (true) {
					byte[] b = new byte[6];
					readFully(socket.getInputStream(), b);
					b = ("Reply" + (++i) + "\r\n").getBytes();
					socket.getOutputStream().write(b);
				}
			}
			catch (Exception e) {
				if (!done.get()) {
					e.printStackTrace();
				}
			}
		});
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		AbstractConnectionFactory ccf = new TcpNetClientConnectionFactory("localhost",
				serverSocket.get().getLocalPort());
		noopPublisher(ccf);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		ccf.setSerializer(serializer);
		ccf.setDeserializer(serializer);
		ccf.setSoTimeout(Integer.MAX_VALUE);
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(ccf);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(ccf);
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		handler.setClientMode(true);
		handler.setRetryInterval(10000);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.setPoolSize(1);
		taskScheduler.initialize();
		handler.setTaskScheduler(taskScheduler);
		handler.start();
		adapter.start();
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		Message<?> mOut = channel.receive(10000);
		assertThat(mOut).isNotNull();
		assertThat(new String((byte[]) mOut.getPayload())).isEqualTo("Reply1");
		mOut = channel.receive(10000);
		assertThat(mOut).isNotNull();
		assertThat(new String((byte[]) mOut.getPayload())).isEqualTo("Reply2");
		done.set(true);
		handler.stop();
		handler.start();
		handler.stop();
		adapter.stop();
		ccf.stop();
		serverSocket.get().close();
	}

	@Test
	public void testNioCrLf() throws Exception {
		final AtomicReference<ServerSocket> serverSocket = new AtomicReference<ServerSocket>();
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean done = new AtomicBoolean();
		this.executor.execute(() -> {
			try {
				ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0);
				serverSocket.set(server);
				latch.countDown();
				Socket socket = server.accept();
				int i = 0;
				while (true) {
					byte[] b = new byte[6];
					readFully(socket.getInputStream(), b);
					b = ("Reply" + (++i) + "\r\n").getBytes();
					socket.getOutputStream().write(b);
				}
			}
			catch (Exception e) {
				if (!done.get()) {
					e.printStackTrace();
				}
			}
		});
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		AbstractConnectionFactory ccf = new TcpNioClientConnectionFactory("localhost",
				serverSocket.get().getLocalPort());
		noopPublisher(ccf);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		ccf.setSerializer(serializer);
		ccf.setDeserializer(serializer);
		ccf.setSoTimeout(10000);
		ccf.start();
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(ccf);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(ccf);
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		Set<String> results = new HashSet<String>();
		Message<?> mOut = channel.receive(10000);
		assertThat(mOut).isNotNull();
		results.add(new String((byte[]) mOut.getPayload()));
		mOut = channel.receive(10000);
		assertThat(mOut).isNotNull();
		results.add(new String((byte[]) mOut.getPayload()));
		assertThat(results.remove("Reply1")).isTrue();
		assertThat(results.remove("Reply2")).isTrue();
		done.set(true);
		ccf.stop();
		serverSocket.get().close();
	}

	@Test
	public void testNetStxEtx() throws Exception {
		final AtomicReference<ServerSocket> serverSocket = new AtomicReference<ServerSocket>();
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean done = new AtomicBoolean();
		this.executor.execute(() -> {
			try {
				ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0);
				serverSocket.set(server);
				latch.countDown();
				Socket socket = server.accept();
				int i = 0;
				while (true) {
					byte[] b = new byte[6];
					readFully(socket.getInputStream(), b);
					b = ("\u0002Reply" + (++i) + "\u0003").getBytes();
					socket.getOutputStream().write(b);
				}
			}
			catch (Exception e) {
				if (!done.get()) {
					e.printStackTrace();
				}
			}
		});
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		AbstractConnectionFactory ccf = new TcpNetClientConnectionFactory("localhost",
				serverSocket.get().getLocalPort());
		noopPublisher(ccf);
		ByteArrayStxEtxSerializer serializer = new ByteArrayStxEtxSerializer();
		ccf.setSerializer(serializer);
		ccf.setDeserializer(serializer);
		ccf.setSoTimeout(10000);
		ccf.start();
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(ccf);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(ccf);
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		Message<?> mOut = channel.receive(10000);
		assertThat(mOut).isNotNull();
		assertThat(new String((byte[]) mOut.getPayload())).isEqualTo("Reply1");
		mOut = channel.receive(10000);
		assertThat(mOut).isNotNull();
		assertThat(new String((byte[]) mOut.getPayload())).isEqualTo("Reply2");
		done.set(true);
		ccf.stop();
		serverSocket.get().close();
	}

	@Test
	public void testNioStxEtx() throws Exception {
		final AtomicReference<ServerSocket> serverSocket = new AtomicReference<ServerSocket>();
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean done = new AtomicBoolean();
		this.executor.execute(() -> {
			try {
				ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0);
				serverSocket.set(server);
				latch.countDown();
				Socket socket = server.accept();
				int i = 0;
				while (true) {
					byte[] b = new byte[6];
					readFully(socket.getInputStream(), b);
					b = ("\u0002Reply" + (++i) + "\u0003").getBytes();
					socket.getOutputStream().write(b);
				}
			}
			catch (Exception e) {
				if (!done.get()) {
					e.printStackTrace();
				}
			}
		});
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		AbstractConnectionFactory ccf = new TcpNioClientConnectionFactory("localhost",
				serverSocket.get().getLocalPort());
		noopPublisher(ccf);
		ByteArrayStxEtxSerializer serializer = new ByteArrayStxEtxSerializer();
		ccf.setSerializer(serializer);
		ccf.setDeserializer(serializer);
		ccf.setSoTimeout(10000);
		ccf.start();
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(ccf);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(ccf);
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		Set<String> results = new HashSet<String>();
		Message<?> mOut = channel.receive(10000);
		assertThat(mOut).isNotNull();
		results.add(new String((byte[]) mOut.getPayload()));
		mOut = channel.receive(10000);
		assertThat(mOut).isNotNull();
		results.add(new String((byte[]) mOut.getPayload()));
		assertThat(results.remove("Reply1")).isTrue();
		assertThat(results.remove("Reply2")).isTrue();
		done.set(true);
		ccf.stop();
		serverSocket.get().close();
	}

	@Test
	public void testNetLength() throws Exception {
		final AtomicReference<ServerSocket> serverSocket = new AtomicReference<>();
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean done = new AtomicBoolean();
		this.executor.execute(() -> {
			try {
				ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0);
				serverSocket.set(server);
				latch.countDown();
				Socket socket = server.accept();
				int i = 0;
				while (true) {
					byte[] b = new byte[8];
					readFully(socket.getInputStream(), b);
					if (!"\u0000\u0000\u0000\u0004Test".equals(new String(b))) {
						throw new RuntimeException("Bad Data");
					}
					b = ("\u0000\u0000\u0000\u0006Reply" + (++i)).getBytes();
					socket.getOutputStream().write(b);
				}
			}
			catch (Exception e) {
				if (!done.get()) {
					e.printStackTrace();
				}
			}
		});
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		AbstractConnectionFactory ccf = new TcpNetClientConnectionFactory("localhost",
				serverSocket.get().getLocalPort());
		noopPublisher(ccf);
		ByteArrayLengthHeaderSerializer serializer = new ByteArrayLengthHeaderSerializer();
		ccf.setSerializer(serializer);
		ccf.setDeserializer(serializer);
		ccf.setSoTimeout(10000);
		ccf.start();
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(ccf);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(ccf);
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		Message<?> mOut = channel.receive(10000);
		assertThat(mOut).isNotNull();
		assertThat(new String((byte[]) mOut.getPayload())).isEqualTo("Reply1");
		mOut = channel.receive(10000);
		assertThat(mOut).isNotNull();
		assertThat(new String((byte[]) mOut.getPayload())).isEqualTo("Reply2");
		done.set(true);
		ccf.stop();
		serverSocket.get().close();
	}

	@Test
	public void testNioLength() throws Exception {
		final AtomicReference<ServerSocket> serverSocket = new AtomicReference<ServerSocket>();
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean done = new AtomicBoolean();
		this.executor.execute(() -> {
			try {
				ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0);
				serverSocket.set(server);
				latch.countDown();
				Socket socket = server.accept();
				int i = 0;
				while (true) {
					byte[] b = new byte[8];
					readFully(socket.getInputStream(), b);
					if (!"\u0000\u0000\u0000\u0004Test".equals(new String(b))) {
						throw new RuntimeException("Bad Data");
					}
					b = ("\u0000\u0000\u0000\u0006Reply" + (++i)).getBytes();
					socket.getOutputStream().write(b);
				}
			}
			catch (Exception e) {
				if (!done.get()) {
					e.printStackTrace();
				}
			}
		});
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		AbstractConnectionFactory ccf = new TcpNioClientConnectionFactory("localhost",
				serverSocket.get().getLocalPort());
		noopPublisher(ccf);
		ByteArrayLengthHeaderSerializer serializer = new ByteArrayLengthHeaderSerializer();
		ccf.setSerializer(serializer);
		ccf.setDeserializer(serializer);
		ccf.setSoTimeout(10000);
		ccf.start();
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(ccf);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(ccf);
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		Set<String> results = new HashSet<String>();
		Message<?> mOut = channel.receive(10000);
		assertThat(mOut).isNotNull();
		results.add(new String((byte[]) mOut.getPayload()));
		mOut = channel.receive(10000);
		assertThat(mOut).isNotNull();
		results.add(new String((byte[]) mOut.getPayload()));
		assertThat(results.remove("Reply1")).isTrue();
		assertThat(results.remove("Reply2")).isTrue();
		done.set(true);
		ccf.stop();
		serverSocket.get().close();
	}

	@Test
	public void testNetSerial() throws Exception {
		final AtomicReference<ServerSocket> serverSocket = new AtomicReference<ServerSocket>();
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean done = new AtomicBoolean();
		this.executor.execute(() -> {
			try {
				ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0);
				serverSocket.set(server);
				latch.countDown();
				Socket socket = server.accept();
				int i = 0;
				while (true) {
					ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
					ois.readObject();
					ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
					oos.writeObject("Reply" + (++i));
				}
			}
			catch (Exception e) {
				if (!done.get()) {
					e.printStackTrace();
				}
			}
		});
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		AbstractConnectionFactory ccf = new TcpNetClientConnectionFactory("localhost",
				serverSocket.get().getLocalPort());
		noopPublisher(ccf);
		ccf.setSerializer(new DefaultSerializer());
		ccf.setDeserializer(new DefaultDeserializer());
		ccf.setSoTimeout(10000);
		ccf.start();
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(ccf);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(ccf);
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		Message<?> mOut = channel.receive(10000);
		assertThat(mOut).isNotNull();
		assertThat(mOut.getPayload()).isEqualTo("Reply1");
		mOut = channel.receive(10000);
		assertThat(mOut).isNotNull();
		assertThat(mOut.getPayload()).isEqualTo("Reply2");
		done.set(true);
		ccf.stop();
		serverSocket.get().close();
	}

	@Test
	public void testNioSerial() throws Exception {
		final AtomicReference<ServerSocket> serverSocket = new AtomicReference<>();
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean done = new AtomicBoolean();
		this.executor.execute(() -> {
			try {
				ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0);
				serverSocket.set(server);
				latch.countDown();
				Socket socket = server.accept();
				int i = 0;
				while (true) {
					ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
					ois.readObject();
					ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
					oos.writeObject("Reply" + (++i));
				}
			}
			catch (Exception e) {
				if (!done.get()) {
					e.printStackTrace();
				}
			}
		});
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		AbstractConnectionFactory ccf = new TcpNioClientConnectionFactory("localhost",
				serverSocket.get().getLocalPort());
		noopPublisher(ccf);
		ccf.setSerializer(new DefaultSerializer());
		ccf.setDeserializer(new DefaultDeserializer());
		ccf.setSoTimeout(10000);
		ccf.start();
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(ccf);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(ccf);
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		Set<String> results = new HashSet<String>();
		Message<?> mOut = channel.receive(10000);
		assertThat(mOut).isNotNull();
		results.add((String) mOut.getPayload());
		mOut = channel.receive(10000);
		assertThat(mOut).isNotNull();
		results.add((String) mOut.getPayload());
		assertThat(results.remove("Reply1")).isTrue();
		assertThat(results.remove("Reply2")).isTrue();
		done.set(true);
		ccf.stop();
		serverSocket.get().close();
	}

	@Test
	public void testNetSingleUseNoInbound() throws Exception {
		final AtomicReference<ServerSocket> serverSocket = new AtomicReference<ServerSocket>();
		final CountDownLatch latch = new CountDownLatch(1);
		final Semaphore semaphore = new Semaphore(0);
		final AtomicBoolean done = new AtomicBoolean();
		this.executor.execute(() -> {
			try {
				ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0);
				serverSocket.set(server);
				latch.countDown();
				for (int i = 0; i < 2; i++) {
					Socket socket = server.accept();
					semaphore.release();
					byte[] b = new byte[6];
					readFully(socket.getInputStream(), b);
					semaphore.release();
					socket.close();
				}
				server.close();
			}
			catch (Exception e) {
				if (!done.get()) {
					e.printStackTrace();
				}
			}
		});
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		AbstractConnectionFactory ccf = new TcpNetClientConnectionFactory("localhost",
				serverSocket.get().getLocalPort());
		noopPublisher(ccf);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		ccf.setSerializer(serializer);
		ccf.setDeserializer(serializer);
		ccf.setSoTimeout(10000);
		ccf.start();
		ccf.setSingleUse(true);
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(ccf);
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		assertThat(semaphore.tryAcquire(4, 10000, TimeUnit.MILLISECONDS)).isTrue();
		done.set(true);
		ccf.stop();
		serverSocket.get().close();
	}

	@Test
	public void testNioSingleUseNoInbound() throws Exception {
		final AtomicReference<ServerSocket> serverSocket = new AtomicReference<ServerSocket>();
		final CountDownLatch latch = new CountDownLatch(1);
		final Semaphore semaphore = new Semaphore(0);
		final AtomicBoolean done = new AtomicBoolean();
		this.executor.execute(() -> {
			try {
				ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0);
				serverSocket.set(server);
				latch.countDown();
				for (int i = 0; i < 2; i++) {
					Socket socket = server.accept();
					semaphore.release();
					byte[] b = new byte[8];
					readFully(socket.getInputStream(), b);
					semaphore.release();
					socket.close();
				}
				server.close();
			}
			catch (Exception e) {
				if (!done.get()) {
					e.printStackTrace();
				}
			}
		});
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		AbstractConnectionFactory ccf = new TcpNioClientConnectionFactory("localhost",
				serverSocket.get().getLocalPort());
		noopPublisher(ccf);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		ccf.setSerializer(serializer);
		ccf.setDeserializer(serializer);
		ccf.setSoTimeout(5000);
		ccf.start();
		ccf.setSingleUse(true);
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(ccf);
		handler.handleMessage(MessageBuilder.withPayload("Test.1").build());
		handler.handleMessage(MessageBuilder.withPayload("Test.2").build());
		assertThat(semaphore.tryAcquire(4, 10000, TimeUnit.MILLISECONDS)).isTrue();
		done.set(true);
		ccf.stop();
		serverSocket.get().close();
	}

	@Test
	public void testNetSingleUseWithInbound() throws Exception {
		final AtomicReference<ServerSocket> serverSocket = new AtomicReference<ServerSocket>();
		final CountDownLatch latch = new CountDownLatch(1);
		final Semaphore semaphore = new Semaphore(0);
		final AtomicBoolean done = new AtomicBoolean();
		this.executor.execute(() -> {
			try {
				ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0);
				serverSocket.set(server);
				latch.countDown();
				for (int i = 1; i < 3; i++) {
					Socket socket = server.accept();
					semaphore.release();
					byte[] b = new byte[6];
					readFully(socket.getInputStream(), b);
					b = ("Reply" + i + "\r\n").getBytes();
					socket.getOutputStream().write(b);
					socket.close();
				}
				server.close();
			}
			catch (Exception e) {
				if (!done.get()) {
					e.printStackTrace();
				}
			}
		});
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		AbstractConnectionFactory ccf = new TcpNetClientConnectionFactory("localhost",
				serverSocket.get().getLocalPort());
		noopPublisher(ccf);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		ccf.setSerializer(serializer);
		ccf.setDeserializer(serializer);
		ccf.setSoTimeout(10000);
		ccf.start();
		ccf.setSingleUse(true);
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(ccf);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(ccf);
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		assertThat(semaphore.tryAcquire(2, 10000, TimeUnit.MILLISECONDS)).isTrue();
		Set<String> replies = new HashSet<String>();
		for (int i = 0; i < 2; i++) {
			Message<?> mOut = channel.receive(10000);
			assertThat(mOut).isNotNull();
			replies.add(new String((byte[]) mOut.getPayload()));
		}
		assertThat(replies.remove("Reply1")).isTrue();
		assertThat(replies.remove("Reply2")).isTrue();
		done.set(true);
		ccf.stop();
		serverSocket.get().close();
	}

	@Test
	public void testNioSingleUseWithInbound() throws Exception {
		final AtomicReference<ServerSocket> serverSocket = new AtomicReference<ServerSocket>();
		final CountDownLatch latch = new CountDownLatch(1);
		final Semaphore semaphore = new Semaphore(0);
		final AtomicBoolean done = new AtomicBoolean();
		this.executor.execute(() -> {
			try {
				ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0);
				serverSocket.set(server);
				latch.countDown();
				for (int i = 1; i < 3; i++) {
					Socket socket = server.accept();
					semaphore.release();
					byte[] b = new byte[6];
					readFully(socket.getInputStream(), b);
					b = ("Reply" + i + "\r\n").getBytes();
					socket.getOutputStream().write(b);
					socket.close();
				}
				server.close();
			}
			catch (Exception e) {
				if (!done.get()) {
					e.printStackTrace();
				}
			}
		});
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		AbstractConnectionFactory ccf = new TcpNioClientConnectionFactory("localhost",
				serverSocket.get().getLocalPort());
		noopPublisher(ccf);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		ccf.setSerializer(serializer);
		ccf.setDeserializer(serializer);
		ccf.setSoTimeout(10000);
		ccf.start();
		ccf.setSingleUse(true);
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(ccf);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(ccf);
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		assertThat(semaphore.tryAcquire(2, 10000, TimeUnit.MILLISECONDS)).isTrue();
		Set<String> replies = new HashSet<String>();
		for (int i = 0; i < 2; i++) {
			Message<?> mOut = channel.receive(10000);
			assertThat(mOut).isNotNull();
			replies.add(new String((byte[]) mOut.getPayload()));
		}
		assertThat(replies.remove("Reply1")).isTrue();
		assertThat(replies.remove("Reply2")).isTrue();
		done.set(true);
		ccf.stop();
		serverSocket.get().close();
	}

	@Test
	public void testNioSingleUseWithInboundMany() throws Exception {
		final AtomicReference<ServerSocket> serverSocket = new AtomicReference<ServerSocket>();
		final CountDownLatch latch = new CountDownLatch(1);
		final Semaphore semaphore = new Semaphore(0);
		final AtomicBoolean done = new AtomicBoolean();
		final List<Socket> serverSockets = new ArrayList<Socket>();
		this.executor.execute(() -> {
			try {
				ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0, 100);
				serverSocket.set(server);
				latch.countDown();
				for (int i = 0; i < 100; i++) {
					final Socket socket = server.accept();
					serverSockets.add(socket);
					final int j = i;
					this.executor.execute(() -> {
						semaphore.release();
						byte[] b = new byte[9];
						try {
							readFully(socket.getInputStream(), b);
							b = ("Reply" + j + "\r\n").getBytes();
							socket.getOutputStream().write(b);
						}
						catch (IOException e1) {
							e1.printStackTrace();
						}
						finally {
							try {
								socket.close();
							}
							catch (IOException e2) {
							}
						}
					});
				}
				server.close();
			}
			catch (Exception e) {
				if (!done.get()) {
					e.printStackTrace();
				}
			}
		});
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		AbstractConnectionFactory ccf = new TcpNioClientConnectionFactory("localhost",
				serverSocket.get().getLocalPort());
		noopPublisher(ccf);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		ccf.setSerializer(serializer);
		ccf.setDeserializer(serializer);
		ccf.setSoTimeout(10000);
		ccf.setSingleUse(true);
		ccf.setTaskExecutor(this.executor);
		ccf.start();
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(ccf);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(ccf);
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		int i = 0;
		try {
			for (i = 100; i < 200; i++) {
				handler.handleMessage(MessageBuilder.withPayload("Test" + i).build());
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			fail("Exception at " + i);
		}
		assertThat(semaphore.tryAcquire(100, 20000, TimeUnit.MILLISECONDS)).isTrue();
		Set<String> replies = new HashSet<String>();
		for (i = 100; i < 200; i++) {
			Message<?> mOut = channel.receive(20000);
			assertThat(mOut).isNotNull();
			replies.add(new String((byte[]) mOut.getPayload()));
		}
		for (i = 0; i < 100; i++) {
			assertThat(replies.remove("Reply" + i)).as("Reply" + i + " missing").isTrue();
		}
		done.set(true);
		ccf.stop();
		serverSocket.get().close();
	}

	@Test
	public void testNetNegotiate() throws Exception {
		final AtomicReference<ServerSocket> serverSocket = new AtomicReference<ServerSocket>();
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean done = new AtomicBoolean();
		this.executor.execute(() -> {
			try {
				ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0);
				serverSocket.set(server);
				latch.countDown();
				Socket socket = server.accept();
				int i = 0;
				while (true) {
					ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
					Object in = null;
					ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
					if (i == 0) {
						in = ois.readObject();
						logger.debug("read object: " + in);
						oos.writeObject("world!");
						ois = new ObjectInputStream(socket.getInputStream());
						oos = new ObjectOutputStream(socket.getOutputStream());
						in = ois.readObject();
						logger.debug("read object: " + in);
						oos.writeObject("world!");
						ois = new ObjectInputStream(socket.getInputStream());
						oos = new ObjectOutputStream(socket.getOutputStream());
					}
					in = ois.readObject();
					oos.writeObject("Reply" + (++i));
				}
			}
			catch (Exception e) {
				if (!done.get()) {
					e.printStackTrace();
				}
			}
		});
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		AbstractConnectionFactory ccf = new TcpNetClientConnectionFactory("localhost",
				serverSocket.get().getLocalPort());
		noopPublisher(ccf);
		ccf.setSerializer(new DefaultSerializer());
		ccf.setDeserializer(new DefaultDeserializer());
		ccf.setSoTimeout(10000);
		TcpConnectionInterceptorFactoryChain fc = new TcpConnectionInterceptorFactoryChain();
		fc.setInterceptors(new TcpConnectionInterceptorFactory[] {
				newInterceptorFactory(),
				newInterceptorFactory()
		});
		ccf.setInterceptorFactoryChain(fc);
		ccf.start();
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(ccf);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(ccf);
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		Message<?> mOut = channel.receive(10000);
		assertThat(mOut).isNotNull();
		assertThat(mOut.getPayload()).isEqualTo("Reply1");
		mOut = channel.receive(10000);
		assertThat(mOut).isNotNull();
		assertThat(mOut.getPayload()).isEqualTo("Reply2");
		done.set(true);
		ccf.stop();
		serverSocket.get().close();
	}

	@Test
	public void testNioNegotiate() throws Exception {
		final AtomicReference<ServerSocket> serverSocket = new AtomicReference<ServerSocket>();
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean done = new AtomicBoolean();
		this.executor.execute(() -> {
			try {
				ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0);
				serverSocket.set(server);
				latch.countDown();
				Socket socket = server.accept();
				int i = 100;
				while (true) {
					ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
					Object in;
					ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
					if (i == 100) {
						in = ois.readObject();
						logger.debug("read object: " + in);
						oos.writeObject("world!");
						ois = new ObjectInputStream(socket.getInputStream());
						oos = new ObjectOutputStream(socket.getOutputStream());
						Thread.sleep(500);
					}
					in = ois.readObject();
					oos.writeObject("Reply" + (i++));
				}
			}
			catch (Exception e) {
				if (!done.get()) {
					e.printStackTrace();
				}
			}
		});
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		AbstractConnectionFactory ccf = new TcpNioClientConnectionFactory("localhost",
				serverSocket.get().getLocalPort());
		noopPublisher(ccf);
		ccf.setSerializer(new DefaultSerializer());
		ccf.setDeserializer(new DefaultDeserializer());
		ccf.setSoTimeout(10000);
		TcpConnectionInterceptorFactoryChain fc = new TcpConnectionInterceptorFactoryChain();
		fc.setInterceptors(new TcpConnectionInterceptorFactory[] { newInterceptorFactory() });
		ccf.setInterceptorFactoryChain(fc);
		ccf.start();
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(ccf);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(ccf);
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		for (int i = 0; i < 1000; i++) {
			handler.handleMessage(MessageBuilder.withPayload("Test").build());
		}
		Set<String> results = new TreeSet<String>();
		for (int i = 0; i < 1000; i++) {
			Message<?> mOut = channel.receive(10000);
			assertThat(mOut).isNotNull();
			results.add((String) mOut.getPayload());
		}
		logger.debug("results: " + results);
		for (int i = 100; i < 1100; i++) {
			assertThat(results.remove("Reply" + i)).as("Missing Reply" + i).isTrue();
		}
		done.set(true);
		ccf.stop();
		serverSocket.get().close();
	}

	@Test
	public void testNetNegotiateSingleNoListen() throws Exception {
		final AtomicReference<ServerSocket> serverSocket = new AtomicReference<ServerSocket>();
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean done = new AtomicBoolean();
		this.executor.execute(() -> {
			try {
				ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0);
				serverSocket.set(server);
				latch.countDown();
				Socket socket = server.accept();
				ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
				ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
				Object in = ois.readObject();
				logger.debug("read object: " + in);
				oos.writeObject("world!");
				ois = new ObjectInputStream(socket.getInputStream());
				oos = new ObjectOutputStream(socket.getOutputStream());
				in = ois.readObject();
				logger.debug("read object: " + in);
				oos.writeObject("world!");
				ois = new ObjectInputStream(socket.getInputStream());
				oos = new ObjectOutputStream(socket.getOutputStream());
				in = ois.readObject();
				oos.writeObject("Reply");
				socket.close();
				server.close();
			}
			catch (Exception e) {
				if (!done.get()) {
					e.printStackTrace();
				}
			}
		});
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		AbstractConnectionFactory ccf = new TcpNetClientConnectionFactory("localhost",
				serverSocket.get().getLocalPort());
		noopPublisher(ccf);
		ccf.setSerializer(new DefaultSerializer());
		ccf.setDeserializer(new DefaultDeserializer());
		ccf.setSoTimeout(10000);
		TcpConnectionInterceptorFactoryChain fc = new TcpConnectionInterceptorFactoryChain();
		fc.setInterceptors(new TcpConnectionInterceptorFactory[] {
				newInterceptorFactory(),
				newInterceptorFactory()
		});
		ccf.setInterceptorFactoryChain(fc);
		ccf.setSingleUse(true);
		ccf.start();
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(ccf);
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		done.set(true);
		ccf.stop();
		serverSocket.get().close();
	}

	@Test
	public void testNioNegotiateSingleNoListen() throws Exception {
		final AtomicReference<ServerSocket> serverSocket = new AtomicReference<ServerSocket>();
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean done = new AtomicBoolean();
		this.executor.execute(() -> {
			int i = 0;
			try {
				ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0);
				serverSocket.set(server);
				latch.countDown();
				Socket socket = server.accept();
				ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
				ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
				Object in = ois.readObject();
				logger.debug("read object: " + in);
				oos.writeObject("world!");
				ois = new ObjectInputStream(socket.getInputStream());
				oos = new ObjectOutputStream(socket.getOutputStream());
				in = ois.readObject();
				logger.debug("read object: " + in);
				oos.writeObject("world!");
				ois = new ObjectInputStream(socket.getInputStream());
				oos = new ObjectOutputStream(socket.getOutputStream());
				oos.writeObject("Reply" + (++i));
				socket.close();
				server.close();
			}
			catch (Exception e) {
				if (i == 0) {
					e.printStackTrace();
				}
			}
		});
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		AbstractConnectionFactory ccf = new TcpNioClientConnectionFactory("localhost",
				serverSocket.get().getLocalPort());
		noopPublisher(ccf);
		ccf.setSerializer(new DefaultSerializer());
		ccf.setDeserializer(new DefaultDeserializer());
		ccf.setSoTimeout(10000);
		TcpConnectionInterceptorFactoryChain fc = new TcpConnectionInterceptorFactoryChain();
		fc.setInterceptors(new TcpConnectionInterceptorFactory[] {
				newInterceptorFactory(),
				newInterceptorFactory()
		});
		ccf.setInterceptorFactoryChain(fc);
		ccf.setSingleUse(true);
		ccf.start();
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(ccf);
		handler.handleMessage(MessageBuilder.withPayload("Test").build());
		done.set(true);
		ccf.stop();
		serverSocket.get().close();
	}

	@Test
	public void testOutboundChannelAdapterWithinChain() throws Exception {
		AbstractApplicationContext ctx = new ClassPathXmlApplicationContext(
				"TcpOutboundChannelAdapterWithinChainTests-context.xml", this.getClass());
		AbstractServerConnectionFactory scf = ctx.getBean(AbstractServerConnectionFactory.class);
		TestingUtilities.waitListening(scf, null);
		ctx.getBean(AbstractClientConnectionFactory.class).setPort(scf.getPort());
		ctx.getBeansOfType(ConsumerEndpointFactoryBean.class).values().forEach(c -> c.start());
		MessageChannel channelAdapterWithinChain = ctx.getBean("tcpOutboundChannelAdapterWithinChain",
				MessageChannel.class);
		PollableChannel inbound = ctx.getBean("inbound", PollableChannel.class);
		String testPayload = "Hello, world!";
		channelAdapterWithinChain.send(new GenericMessage<String>(testPayload));
		Message<?> m = inbound.receive(1000);
		assertThat(m).isNotNull();
		assertThat(new String((byte[]) m.getPayload())).isEqualTo(testPayload);
		ctx.close();
	}

	@Test
	public void testConnectionException() throws Exception {
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		AbstractConnectionFactory mockCcf = mock(AbstractClientConnectionFactory.class);
		Mockito.doAnswer(invocation -> {
			throw new SocketException("Failed to connect");
		}).when(mockCcf).getConnection();
		handler.setConnectionFactory(mockCcf);
		try {
			handler.handleMessage(new GenericMessage<String>("foo"));
			fail("Expected exception");
		}
		catch (Exception e) {
			assertThat(e instanceof MessagingException).isTrue();
			assertThat(e.getCause() != null).isTrue();
			assertThat(e.getCause() instanceof SocketException).isTrue();
			assertThat(e.getCause().getMessage()).isEqualTo("Failed to connect");
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testInterceptedConnection() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		AbstractServerConnectionFactory scf = new TcpNetServerConnectionFactory(0);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		scf.setSerializer(serializer);
		scf.setDeserializer(serializer);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(scf);
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(scf);
		final AtomicReference<TcpConnection> connection = new AtomicReference<>();
		scf.setApplicationEventPublisher(event -> {
			if (event instanceof TcpConnectionOpenEvent) {
				connection.set(handler.getConnections()
						.get(((TcpConnectionOpenEvent) event).getConnectionId()));
				latch.countDown();
			}
		});
		TcpConnectionInterceptorFactoryChain fc = new TcpConnectionInterceptorFactoryChain();
		fc.setInterceptor(newInterceptorFactory(scf.getApplicationEventPublisher()));
		scf.setInterceptorFactoryChain(fc);
		scf.start();
		TestingUtilities.waitListening(scf, null);
		int port = scf.getPort();
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.close();
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(connection.get()).isInstanceOf(HelloWorldInterceptor.class);
		await().untilAsserted(() -> handler.getConnections().isEmpty());
		scf.stop();
	}

	@Test
	public void testInterceptedCleanup() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		AbstractServerConnectionFactory scf = new TcpNetServerConnectionFactory(0);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		scf.setSerializer(serializer);
		scf.setDeserializer(serializer);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(scf);
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(scf);
		scf.setApplicationEventPublisher(event -> {
			if (event instanceof TcpConnectionCloseEvent) {
				latch.countDown();
			}
		});
		TcpConnectionInterceptorFactoryChain fc = new TcpConnectionInterceptorFactoryChain();
		fc.setInterceptor(newInterceptorFactory(scf.getApplicationEventPublisher()));
		scf.setInterceptorFactoryChain(fc);
		scf.start();
		TestingUtilities.waitListening(scf, null);
		int port = scf.getPort();
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.close();
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		await().untilAsserted(() -> handler.getConnections().isEmpty());
		scf.stop();
	}

}
