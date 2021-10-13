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

package org.springframework.integration.ip.tcp.connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.net.SocketFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.log.LogAccessor;
import org.springframework.core.log.LogMessage;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.ip.config.TcpConnectionFactoryFactoryBean;
import org.springframework.integration.ip.event.IpIntegrationEvent;
import org.springframework.integration.ip.tcp.TcpOutboundGateway;
import org.springframework.integration.ip.tcp.TcpReceivingChannelAdapter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 3.0
 *
 */
public class ConnectionFactoryTests {

	@Test
	void netOpenEventOnReadThread() throws InterruptedException, IOException {
		TcpNetServerConnectionFactory server = new TcpNetServerConnectionFactory(0);
		AtomicReference<Thread> readThread = new AtomicReference<>();
		AtomicReference<Thread> openEventThread = new AtomicReference<>();
		CountDownLatch latch1 = new CountDownLatch(1);
		CountDownLatch latch2 = new CountDownLatch(1);
		server.registerListener(msg -> {
			readThread.set(Thread.currentThread());
			latch2.countDown();
			return false;
		});
		server.setApplicationEventPublisher(event -> {
			if (event instanceof TcpConnectionServerListeningEvent) {
				latch1.countDown();
			}
			if (event instanceof TcpConnectionOpenEvent) {
				openEventThread.set(Thread.currentThread());
			}
		});
		server.afterPropertiesSet();
		server.start();
		assertThat(latch1.await(10, TimeUnit.SECONDS)).isTrue();
		Socket socket = SocketFactory.getDefault().createSocket("localhost", server.getPort());
		socket.getOutputStream().write("test\r\n".getBytes());
		socket.close();
		assertThat(latch2.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(readThread.get()).isSameAs(openEventThread.get());
	}

	@Test
	public void factoryBeanTests() {
		TcpConnectionFactoryFactoryBean fb = new TcpConnectionFactoryFactoryBean("client");
		assertThat(fb.getObjectType()).isEqualTo(AbstractClientConnectionFactory.class);
		fb = new TcpConnectionFactoryFactoryBean("server");
		assertThat(fb.getObjectType()).isEqualTo(AbstractServerConnectionFactory.class);
		fb = new TcpConnectionFactoryFactoryBean();
		assertThat(fb.getObjectType()).isEqualTo(AbstractConnectionFactory.class);
	}

	@Test
	public void testObtainConnectionIdsNet() throws Exception {
		TcpNetServerConnectionFactory serverFactory = new TcpNetServerConnectionFactory(0);
		testObtainConnectionIds(serverFactory);
	}

	@Test
	public void testObtainConnectionIdsNio() throws Exception {
		TcpNioServerConnectionFactory serverFactory = new TcpNioServerConnectionFactory(0);
		testObtainConnectionIds(serverFactory);
	}

	public void testObtainConnectionIds(AbstractServerConnectionFactory serverFactory) throws Exception {
		final List<IpIntegrationEvent> events =
				Collections.synchronizedList(new ArrayList<IpIntegrationEvent>());
		int expectedEvents = serverFactory instanceof TcpNetServerConnectionFactory
				? 7  // Listening, + OPEN, CLOSE, EXCEPTION for each side
				: 5; // Listening, + OPEN, CLOSE (but we *might* get exceptions, depending on timing).
		final CountDownLatch serverListeningLatch = new CountDownLatch(1);
		final CountDownLatch eventLatch = new CountDownLatch(expectedEvents);
		ApplicationEventPublisher publisher = event -> {
			LogFactory.getLog(this.getClass()).trace("Received: " + event);
			events.add((IpIntegrationEvent) event);
			if (event instanceof TcpConnectionServerListeningEvent) {
				serverListeningLatch.countDown();
			}
			eventLatch.countDown();
		};
		serverFactory.setBeanName("serverFactory");
		serverFactory.setApplicationEventPublisher(publisher);
		serverFactory = spy(serverFactory);
		final CountDownLatch serverConnectionInitLatch = new CountDownLatch(1);
		doAnswer(invocation -> {
			Object result = invocation.callRealMethod();
			serverConnectionInitLatch.countDown();
			return result;
		}).when(serverFactory).wrapConnection(any(TcpConnectionSupport.class));
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(10);
		scheduler.afterPropertiesSet();
		BeanFactory bf = mock(BeanFactory.class);
		when(bf.containsBean(IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME)).thenReturn(true);
		when(bf.getBean(IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME, TaskScheduler.class)).thenReturn(scheduler);
		serverFactory.setBeanFactory(bf);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setOutputChannel(new NullChannel());
		adapter.setConnectionFactory(serverFactory);
		adapter.start();
		assertThat(serverListeningLatch.await(10, TimeUnit.SECONDS)).as("Listening event not received").isTrue();
		assertThat(events.get(0)).isInstanceOf(TcpConnectionServerListeningEvent.class);
		assertThat(((TcpConnectionServerListeningEvent) events.get(0)).getPort()).isEqualTo(serverFactory.getPort());
		int port = serverFactory.getPort();
		TcpNetClientConnectionFactory clientFactory = new TcpNetClientConnectionFactory("localhost", port);
		clientFactory.registerListener(message -> false);
		clientFactory.setBeanName("clientFactory");
		clientFactory.setApplicationEventPublisher(publisher);
		clientFactory.start();
		TcpConnectionSupport client = clientFactory.getConnection();
		List<String> clients = clientFactory.getOpenConnectionIds();
		assertThat(clients.size()).isEqualTo(1);
		assertThat(clients.contains(client.getConnectionId())).isTrue();
		assertThat(serverConnectionInitLatch.await(10, TimeUnit.SECONDS)).as("Server connection failed to register")
				.isTrue();
		List<String> servers = serverFactory.getOpenConnectionIds();
		assertThat(servers.size()).isEqualTo(1);
		assertThat(serverFactory.closeConnection(servers.get(0))).isTrue();
		servers = serverFactory.getOpenConnectionIds();
		assertThat(servers.size()).isEqualTo(0);
		await().atMost(Duration.ofSeconds(10)).until(() -> clientFactory.getOpenConnectionIds().size() == 0);
		clients = clientFactory.getOpenConnectionIds();
		assertThat(clients.size()).isEqualTo(0);
		assertThat(eventLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(events.size())
				.as("Expected at least " + expectedEvents + " events; got: " + events.size() + " : " + events)
				.isGreaterThanOrEqualTo(expectedEvents);

		FooEvent event = new FooEvent(client, "foo");
		client.publishEvent(event);
		assertThat(events.size())
				.as("Expected at least " + expectedEvents + " events; got: " + events.size() + " : " + events)
				.isGreaterThanOrEqualTo(expectedEvents + 1);

		try {
			event = new FooEvent(mock(TcpConnectionSupport.class), "foo");
			client.publishEvent(event);
			fail("Expected exception");
		}
		catch (IllegalArgumentException e) {
			assertThat("Can only publish events with this as the source".equals(e.getMessage())).isTrue();
		}

		SocketAddress address = serverFactory.getServerSocketAddress();
		if (address instanceof InetSocketAddress) {
			InetSocketAddress inetAddress = (InetSocketAddress) address;
			assertThat(inetAddress.getPort()).isEqualTo(port);
		}
		serverFactory.stop();
		scheduler.shutdown();
	}

	@Test
	public void testEarlyCloseNet() throws Exception {
		AbstractServerConnectionFactory factory = new TcpNetServerConnectionFactory(0);
		testEarlyClose(factory, "serverSocket", " stopped before accept");
	}

	@Test
	public void testEarlyCloseNio() throws Exception {
		AbstractServerConnectionFactory factory = new TcpNioServerConnectionFactory(0);
		testEarlyClose(factory, "serverChannel", " stopped before registering the server channel");
	}

	private void testEarlyClose(final AbstractServerConnectionFactory factory, String property,
			String message) throws Exception {

		factory.setApplicationEventPublisher(mock(ApplicationEventPublisher.class));
		factory.setBeanName("foo");
		factory.registerListener(mock(TcpListener.class));
		factory.afterPropertiesSet();
		LogAccessor logAccessor = TestUtils.getPropertyValue(factory, "logger", LogAccessor.class);
		Log logger = spy(logAccessor.getLog());
		new DirectFieldAccessor(logAccessor).setPropertyValue("log", logger);
		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);
		final CountDownLatch latch3 = new CountDownLatch(1);
		when(logger.isInfoEnabled()).thenReturn(true);
		when(logger.isDebugEnabled()).thenReturn(true);
		doAnswer(invocation -> {
			latch1.countDown();
			// wait until the stop nulls the channel
			latch2.await(10, TimeUnit.SECONDS);
			return null;
		}).when(logger).info(argThat(logMessage -> logMessage.toString().contains("Listening")));
		doAnswer(invocation -> {
			latch3.countDown();
			return null;
		}).when(logger).debug(argThat(logMessage -> logMessage.toString().contains(message)));
		factory.start();
		assertThat(latch1.await(10, TimeUnit.SECONDS)).as("missing info log").isTrue();
		// stop on a different thread because it waits for the executor
		new SimpleAsyncTaskExecutor("testEarlyClose-")
				.execute(factory::stop);
		int n = 0;
		DirectFieldAccessor accessor = new DirectFieldAccessor(factory);
		await("Stop was not invoked in time").atMost(Duration.ofSeconds(20))
				.until(() -> accessor.getPropertyValue(property) == null);
		latch2.countDown();
		assertThat(latch3.await(10, TimeUnit.SECONDS)).as("missing debug log").isTrue();
		String expected = "bean 'foo', port=" + factory.getPort() + message;
		ArgumentCaptor<LogMessage> captor = ArgumentCaptor.forClass(LogMessage.class);
		verify(logger, atLeast(1)).debug(captor.capture());
		assertThat(captor.getAllValues().stream().map(Object::toString).collect(Collectors.toList())).contains(expected);
		factory.stop();
	}

	@Test
	void healthCheckSuccessNet() throws InterruptedException {
		healthCheckSuccess(new TcpNetServerConnectionFactory(0), false);
	}

	@Test
	void healthCheckSuccessNio() throws InterruptedException {
		healthCheckSuccess(new TcpNioServerConnectionFactory(0), false);
	}

	@Test
	void healthCheckFailureNet() throws InterruptedException {
		healthCheckSuccess(new TcpNetServerConnectionFactory(0), true);
	}

	@Test
	void healthCheckFailureNio() throws InterruptedException {
		healthCheckSuccess(new TcpNioServerConnectionFactory(0), true);
	}

	private void healthCheckSuccess(AbstractServerConnectionFactory server, boolean fail) throws InterruptedException {
		CountDownLatch serverUp = new CountDownLatch(1);
		server.setApplicationEventPublisher(event -> {
			if (event instanceof TcpConnectionServerListeningEvent) {
				serverUp.countDown();
			}
		});
		server.setBeanFactory(mock(BeanFactory.class));
		AtomicReference<TcpConnection> connection = new AtomicReference<>();
		server.registerSender(conn -> {
			connection.set(conn);
		});
		AtomicInteger tested = new AtomicInteger();
		server.registerListener(msg -> {
			if (!(msg instanceof ErrorMessage)) {
				String payload = new String((byte[]) msg.getPayload());
				if (payload.equals("PING")) {
					tested.incrementAndGet();
					connection.get().send(new GenericMessage<>(fail ? "PANG" : "PONG"));
				}
				else {
					connection.get().send(msg);
				}
			}
			return false;
		});
		server.start();
		assertThat(serverUp.await(10, TimeUnit.SECONDS)).isTrue();
		TcpNetClientConnectionFactory clientFactory = new TcpNetClientConnectionFactory("localhost", server.getPort());
		clientFactory.setApplicationEventPublisher(event -> { });
		clientFactory.setConnectionTest(conn -> {
			CountDownLatch latch = new CountDownLatch(1);
			AtomicBoolean result = new AtomicBoolean();
			conn.registerTestListener(msg -> {
				if (Arrays.equals("PONG".getBytes(), (byte[]) msg.getPayload())) {
					result.set(true);
				}
				latch.countDown();
				return false;
			});
			conn.send(new GenericMessage<>("PING"));
			try {
				latch.await(10, TimeUnit.SECONDS);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			return result.get();
		});
		TcpOutboundGateway gateway = new TcpOutboundGateway();
		gateway.setRemoteTimeout(60000);
		gateway.setConnectionFactory(clientFactory);
		QueueChannel outputChannel = new QueueChannel();
		gateway.setOutputChannel(outputChannel);
		gateway.start();
		if (fail) {
			assertThatExceptionOfType(MessagingException.class).isThrownBy(() ->
					gateway.handleMessage(new GenericMessage<>("test1")))
					.withMessageContaining("Connection test failed for");
		}
		else {
			gateway.handleMessage(new GenericMessage<>("test1"));
			Message<?> received = outputChannel.receive(0);
			assertThat(received).isNotNull();
			assertThat(received.getPayload()).isEqualTo("test1".getBytes());
			gateway.handleMessage(new GenericMessage<>("test2"));
			received = outputChannel.receive(0);
			assertThat(received).isNotNull();
			assertThat(received.getPayload()).isEqualTo("test2".getBytes());
			assertThat(tested.get()).isEqualTo(1);
		}
		gateway.stop();
		server.stop();
	}

	@SuppressWarnings("serial")
	private class FooEvent extends TcpConnectionOpenEvent {

		FooEvent(TcpConnectionSupport connection, String connectionFactoryName) {
			super(connection, connectionFactoryName);
		}

	}

}
