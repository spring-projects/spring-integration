/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.ip.tcp.outbound;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import javax.net.ServerSocketFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.MessageTimeoutException;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.CachingClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.FailoverClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpConnection;
import org.springframework.integration.ip.tcp.connection.TcpNetClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.ThreadAffinityClientConnectionFactory;
import org.springframework.integration.ip.tcp.serializer.ByteArrayCrLfSerializer;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author WooSeung Kim
 *
 * @since 7.0.8
 */
public class TcpOutboundGatewayTimeoutTests implements TestApplicationContextAware {

	private static final String SLOW_REQUEST = "slow-request";

	private static final String NEXT_REQUEST = "next-request";

	private static final Expression SLOW_REQUEST_TIMEOUT =
			new SpelExpressionParser().parseExpression("payload == '" + SLOW_REQUEST + "' ? 400 : 10000");

	private final Executor executor = new SimpleAsyncTaskExecutor("tcpGatewayTimeout-");

	static Stream<Arguments> connectionFactories() {
		return Stream.of(
				Arguments.of("shared", true),
				Arguments.of("cached", true),
				Arguments.of("failover", true),
				Arguments.of("failover over cached", true),
				Arguments.of("failover over cached", false),
				Arguments.of("thread affinity", true),
				Arguments.of("thread affinity", false),
				Arguments.of("thread affinity over cached", true),
				Arguments.of("thread affinity over cached", false));
	}

	@ParameterizedTest(name = "{0}, async={1}")
	@MethodSource("connectionFactories")
	void nextRequestDoesNotReceiveTimedOutReply(String type, boolean async) throws Exception {
		ServerSocket server = startServer();
		try {
			AbstractClientConnectionFactory factory = connectionFactory(type, server.getLocalPort());
			assertThat(new String(nextReply(factory, async))).isEqualTo("reply-" + NEXT_REQUEST);
		}
		finally {
			server.close();
		}
	}

	@Test
	void timeoutClosesTheConnectionBeforeReleasingIt() throws Exception {
		ServerSocket server = startServer();
		AtomicInteger idleOnForceClose = new AtomicInteger(-1);
		try {
			CachingClientConnectionFactory caching =
					new CachingClientConnectionFactory(netClient(server.getLocalPort(), false), 1) {

						@Override
						public void forceClose(TcpConnection connection) {
							idleOnForceClose.compareAndSet(-1, getIdleCount());
							super.forceClose(connection);
						}

					};
			initialize(caching);
			assertThat(new String(nextReply(caching, true))).isEqualTo("reply-" + NEXT_REQUEST);
			assertThat(idleOnForceClose.get()).isZero();
		}
		finally {
			server.close();
		}
	}

	@Test
	void failingForceCloseStillReleasesTheConnection() throws Exception {
		ServerSocket server = startServer();
		try {
			TcpNetClientConnectionFactory factory =
					new TcpNetClientConnectionFactory("localhost", server.getLocalPort()) {

						@Override
						public void forceClose(TcpConnection connection) {
							super.forceClose(connection);
							throw new IllegalStateException("intentional");
						}

					};
			factory.setSoTimeout(15_000);
			initialize(factory);
			assertThat(new String(nextReply(factory, true))).isEqualTo("reply-" + NEXT_REQUEST);
		}
		finally {
			server.close();
		}
	}

	@Test
	void replyLosingToTheTimeoutIsALateReply() throws Exception {
		ServerSocket server = startServer();
		ThreadPoolTaskScheduler scheduler = scheduler();
		TcpOutboundGateway gateway = new TcpOutboundGateway();
		QueueChannel unsolicited = new QueueChannel();
		AtomicBoolean lateReplyRoutedBeforeTimeout = new AtomicBoolean();
		QueueChannel errors = new QueueChannel() {

			@Override
			protected boolean doSend(Message<?> message, long timeout) {
				lateReplyRoutedBeforeTimeout.set(unsolicited.getQueueSize() > 0);
				return super.doSend(message, timeout);
			}

		};
		TcpNetClientConnectionFactory factory = new TcpNetClientConnectionFactory("localhost", server.getLocalPort()) {

			@Override
			public void forceClose(TcpConnection connection) {
				gateway.onMessage(MessageBuilder.withPayload("reply-" + SLOW_REQUEST)
						.setHeader(IpHeaders.CONNECTION_ID, connection.getConnectionId())
						.build());
				super.forceClose(connection);
			}

		};
		factory.setSoTimeout(15_000);
		initialize(factory);
		try {
			gateway.setConnectionFactory(factory);
			gateway.setAsync(true);
			gateway.setRemoteTimeout(200);
			gateway.setRequiresReply(true);
			gateway.setOutputChannel(new QueueChannel());
			gateway.setUnsolicitedMessageChannel(unsolicited);
			gateway.setBeanFactory(TEST_INTEGRATION_CONTEXT);
			gateway.setTaskScheduler(scheduler);
			gateway.afterPropertiesSet();
			gateway.handleMessage(MessageBuilder.withPayload(SLOW_REQUEST).setErrorChannel(errors).build());
			assertTimedOut(errors.receive(10_000));
			assertThat(lateReplyRoutedBeforeTimeout.get()).isTrue();
			Message<?> late = unsolicited.receive(0);
			assertThat(late).isNotNull();
			assertThat(late.getPayload()).isEqualTo("reply-" + SLOW_REQUEST);
			assertThat(availablePermits(gateway)).isEqualTo(1);
		}
		finally {
			gateway.stop();
			factory.stop();
			server.close();
			scheduler.shutdown();
		}
	}

	@Test
	void errorFlowCanSendThroughTheGatewayAfterTimeout() throws Exception {
		ServerSocket server = startServer();
		ThreadPoolTaskScheduler scheduler = scheduler();
		TcpNetClientConnectionFactory factory = netClient(server.getLocalPort(), false);
		TcpOutboundGateway gateway = new TcpOutboundGateway();
		QueueChannel replies = new QueueChannel();
		DirectChannel errors = new DirectChannel();
		errors.subscribe(message -> gateway.handleMessage(MessageBuilder.withPayload(NEXT_REQUEST).build()));
		try {
			gateway.setConnectionFactory(factory);
			gateway.setAsync(true);
			gateway.setRequiresReply(true);
			gateway.setRequestTimeout(1_000);
			gateway.setRemoteTimeoutExpression(SLOW_REQUEST_TIMEOUT);
			gateway.setOutputChannel(replies);
			gateway.setBeanFactory(TEST_INTEGRATION_CONTEXT);
			gateway.setTaskScheduler(scheduler);
			gateway.afterPropertiesSet();
			gateway.handleMessage(MessageBuilder.withPayload(SLOW_REQUEST).setErrorChannel(errors).build());
			Message<?> reply = replies.receive(10_000);
			assertThat(reply).isNotNull();
			assertThat(new String((byte[]) reply.getPayload())).isEqualTo("reply-" + NEXT_REQUEST);
		}
		finally {
			gateway.stop();
			factory.stop();
			server.close();
			scheduler.shutdown();
		}
	}

	@Test
	void sendFailureDoesNotReleaseTheSemaphoreAgainOnTimeout() throws Exception {
		ServerSocket server = startServer();
		ThreadPoolTaskScheduler scheduler = scheduler();
		TcpNetClientConnectionFactory factory = new TcpNetClientConnectionFactory("localhost", server.getLocalPort());
		factory.setSerializer(new ByteArrayCrLfSerializer() {

			@Override
			public void serialize(byte[] bytes, OutputStream outputStream) throws IOException {
				throw new IOException("intentional");
			}

		});
		initialize(factory);
		TcpOutboundGateway gateway = new TcpOutboundGateway();
		try {
			gateway.setConnectionFactory(factory);
			gateway.setAsync(true);
			gateway.setRemoteTimeout(100);
			gateway.setRequiresReply(true);
			gateway.setOutputChannel(new QueueChannel());
			gateway.setBeanFactory(TEST_INTEGRATION_CONTEXT);
			gateway.setTaskScheduler(scheduler);
			gateway.afterPropertiesSet();
			assertThatExceptionOfType(MessagingException.class)
					.isThrownBy(() -> gateway.handleMessage(MessageBuilder.withPayload(SLOW_REQUEST).build()));
			assertThat(scheduler.getScheduledThreadPoolExecutor().getTaskCount()).isZero();
			assertThat(availablePermits(gateway)).isEqualTo(1);
		}
		finally {
			gateway.stop();
			factory.stop();
			server.close();
			scheduler.shutdown();
		}
	}

	private byte[] nextReply(AbstractClientConnectionFactory factory, boolean async) {
		ThreadPoolTaskScheduler scheduler = scheduler();
		TcpOutboundGateway gateway = new TcpOutboundGateway();
		QueueChannel replies = new QueueChannel();
		QueueChannel errors = new QueueChannel();
		try {
			gateway.setConnectionFactory(factory);
			gateway.setAsync(async);
			gateway.setRequiresReply(true);
			gateway.setRemoteTimeoutExpression(SLOW_REQUEST_TIMEOUT);
			gateway.setOutputChannel(replies);
			gateway.setBeanFactory(TEST_INTEGRATION_CONTEXT);
			gateway.setTaskScheduler(scheduler);
			gateway.afterPropertiesSet();
			Message<String> slowRequest = MessageBuilder.withPayload(SLOW_REQUEST).setErrorChannel(errors).build();
			if (async) {
				gateway.handleMessage(slowRequest);
				assertTimedOut(errors.receive(10_000));
			}
			else {
				assertThatExceptionOfType(MessageTimeoutException.class)
						.isThrownBy(() -> gateway.handleMessage(slowRequest));
			}
			gateway.handleMessage(MessageBuilder.withPayload(NEXT_REQUEST).setErrorChannel(errors).build());
			Message<?> reply = replies.receive(10_000);
			assertThat(reply).isNotNull();
			return (byte[]) reply.getPayload();
		}
		finally {
			gateway.stop();
			factory.stop();
			scheduler.shutdown();
		}
	}

	private AbstractClientConnectionFactory connectionFactory(String type, int port) {
		AbstractClientConnectionFactory factory = switch (type) {
			case "shared" -> netClient(port, false);
			case "cached" -> new CachingClientConnectionFactory(netClient(port, false), 1);
			case "failover" -> new FailoverClientConnectionFactory(List.of(netClient(port, false)));
			case "failover over cached" -> {
				FailoverClientConnectionFactory failover =
						new FailoverClientConnectionFactory(List.of(cached(port)));
				failover.setSingleUse(true);
				yield failover;
			}
			case "thread affinity" -> new ThreadAffinityClientConnectionFactory(netClient(port, true));
			case "thread affinity over cached" -> new ThreadAffinityClientConnectionFactory(cached(port));
			default -> throw new IllegalArgumentException(type);
		};
		if (!(factory instanceof TcpNetClientConnectionFactory)) {
			initialize(factory);
		}
		return factory;
	}

	private CachingClientConnectionFactory cached(int port) {
		CachingClientConnectionFactory caching = new CachingClientConnectionFactory(netClient(port, false), 1);
		initialize(caching);
		return caching;
	}

	private static void initialize(AbstractClientConnectionFactory factory) {
		factory.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		factory.setApplicationEventPublisher(TEST_INTEGRATION_CONTEXT);
		factory.afterPropertiesSet();
		factory.start();
	}

	private static TcpNetClientConnectionFactory netClient(int port, boolean singleUse) {
		TcpNetClientConnectionFactory factory = new TcpNetClientConnectionFactory("localhost", port);
		factory.setSingleUse(singleUse);
		factory.setSoTimeout(15_000);
		initialize(factory);
		return factory;
	}

	private static ThreadPoolTaskScheduler scheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(2);
		scheduler.initialize();
		return scheduler;
	}

	private static void assertTimedOut(Message<?> error) {
		assertThat(error).isInstanceOf(ErrorMessage.class)
				.extracting(Message::getPayload)
				.isInstanceOf(MessageTimeoutException.class);
	}

	private static int availablePermits(TcpOutboundGateway gateway) {
		return ((Semaphore) TestUtils.getPropertyValue(gateway, "semaphore")).availablePermits();
	}

	private ServerSocket startServer() throws Exception {
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0, 50);
		this.executor.execute(() -> {
			try {
				while (!server.isClosed()) {
					Socket socket = server.accept();
					this.executor.execute(() -> serve(socket));
				}
			}
			catch (Exception ex) {
				// server closed
			}
		});
		return server;
	}

	/**
	 * Hold the reply to the slow request until the next request arrives on the same socket.
	 */
	private void serve(Socket socket) {
		try (socket) {
			InputStream input = socket.getInputStream();
			OutputStream output = socket.getOutputStream();
			ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
			byte[] heldReply = null;
			while (true) {
				String request = new String(serializer.deserialize(input));
				byte[] reply = ("reply-" + request).getBytes();
				if (SLOW_REQUEST.equals(request)) {
					heldReply = reply;
					continue;
				}
				if (heldReply != null) {
					serializer.serialize(heldReply, output);
					heldReply = null;
				}
				serializer.serialize(reply, output);
				output.flush();
			}
		}
		catch (Exception ex) {
			// connection closed
		}
	}

}
