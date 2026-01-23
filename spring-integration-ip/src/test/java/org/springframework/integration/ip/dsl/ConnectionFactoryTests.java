/*
 * Copyright 2016-present the original author or authors.
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

package org.springframework.integration.ip.dsl;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNetClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNetConnectionSupport;
import org.springframework.integration.ip.tcp.connection.TcpNetServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNioClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNioConnectionSupport;
import org.springframework.integration.ip.tcp.connection.TcpNioServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpSocketFactorySupport;
import org.springframework.integration.ip.tcp.connection.TcpSocketSupport;
import org.springframework.integration.ip.util.TestingUtilities;
import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.transformer.ObjectToStringTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Gary Russell
 * @author Tim Ysewyn
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 5.0
 *
 */
public class ConnectionFactoryTests implements TestApplicationContextAware {

	@Test
	public void test() throws Exception {
		ApplicationEventPublisher publisher = e -> {
		};
		AbstractServerConnectionFactory server = Tcp.netServer(0).backlog(2).soTimeout(5000).getObject();
		server.setTaskScheduler(new SimpleAsyncTaskScheduler());
		final AtomicReference<Message<?>> received = new AtomicReference<>();
		final CountDownLatch latch = new CountDownLatch(1);
		server.registerListener(m -> {
			received.set(new ObjectToStringTransformer().transform(m));
			latch.countDown();
		});
		server.setApplicationEventPublisher(publisher);
		server.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		server.afterPropertiesSet();
		server.start();
		TestingUtilities.waitListening(server, null);
		AbstractClientConnectionFactory client = Tcp.netClient("localhost", server.getPort()).getObject();
		client.setApplicationEventPublisher(publisher);
		client.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		client.afterPropertiesSet();
		client.start();
		client.getConnection().send(new GenericMessage<>("foo"));
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(received.get().getPayload()).isEqualTo("foo");
		client.stop();
		server.stop();
	}

	@Test
	public void shouldReturnNioFlavor() {
		AbstractServerConnectionFactory server = Tcp.nioServer(0).getObject();
		assertThat(server instanceof TcpNioServerConnectionFactory).isTrue();

		AbstractClientConnectionFactory client = Tcp.nioClient("localhost", server.getPort()).getObject();
		assertThat(client instanceof TcpNioClientConnectionFactory).isTrue();
	}

	@Test
	public void shouldReturnNetFlavor() {
		AbstractServerConnectionFactory server = Tcp.netServer(0).getObject();
		assertThat(server instanceof TcpNetServerConnectionFactory).isTrue();

		AbstractClientConnectionFactory client = Tcp.netClient("localhost", server.getPort()).getObject();
		assertThat(client instanceof TcpNetClientConnectionFactory).isTrue();
	}

	@Test
	void netCustomServer() {
		TcpSocketSupport sockSupp = mock(TcpSocketSupport.class);
		TcpNetConnectionSupport conSupp = mock(TcpNetConnectionSupport.class);
		TcpSocketFactorySupport factSupp = mock(TcpSocketFactorySupport.class);
		TcpNetServerConnectionFactory server = Tcp.netServer(0)
				.socketSupport(sockSupp)
				.connectionSupport(conSupp)
				.socketFactorySupport(factSupp)
				.getObject();
		assertThat(TestUtils.<Object>getPropertyValue(server, "tcpSocketSupport")).isSameAs(sockSupp);
		assertThat(TestUtils.<Object>getPropertyValue(server, "tcpNetConnectionSupport")).isSameAs(conSupp);
		assertThat(TestUtils.<Object>getPropertyValue(server, "tcpSocketFactorySupport")).isSameAs(factSupp);
	}

	@Test
	void nioCustomServer() {
		TcpSocketSupport sockSupp = mock(TcpSocketSupport.class);
		TcpNioConnectionSupport conSupp = mock(TcpNioConnectionSupport.class);
		TcpNioServerConnectionFactory server = Tcp.nioServer(0)
				.socketSupport(sockSupp)
				.directBuffers(true)
				.connectionSupport(conSupp)
				.getObject();
		assertThat(TestUtils.<Object>getPropertyValue(server, "tcpSocketSupport")).isSameAs(sockSupp);
		assertThat(TestUtils.<Boolean>getPropertyValue(server, "usingDirectBuffers")).isTrue();
		assertThat(TestUtils.<Object>getPropertyValue(server, "tcpNioConnectionSupport")).isSameAs(conSupp);
	}

	@Test
	void netCustomClient() {
		TcpSocketSupport sockSupp = mock(TcpSocketSupport.class);
		TcpNetConnectionSupport conSupp = mock(TcpNetConnectionSupport.class);
		TcpSocketFactorySupport factSupp = mock(TcpSocketFactorySupport.class);
		TcpNetClientConnectionFactory client = Tcp.netClient("localhost", 0)
				.socketSupport(sockSupp)
				.connectionSupport(conSupp)
				.socketFactorySupport(factSupp)
				.getObject();
		assertThat(TestUtils.<Object>getPropertyValue(client, "tcpSocketSupport")).isSameAs(sockSupp);
		assertThat(TestUtils.<Object>getPropertyValue(client, "tcpNetConnectionSupport")).isSameAs(conSupp);
		assertThat(TestUtils.<Object>getPropertyValue(client, "tcpSocketFactorySupport")).isSameAs(factSupp);
	}

	@Test
	void nioCustomClient() {
		TcpSocketSupport sockSupp = mock(TcpSocketSupport.class);
		TcpNioConnectionSupport conSupp = mock(TcpNioConnectionSupport.class);
		TcpNioClientConnectionFactory client = Tcp.nioClient("localhost", 0)
				.socketSupport(sockSupp)
				.directBuffers(true)
				.connectionSupport(conSupp)
				.getObject();
		assertThat(TestUtils.<Object>getPropertyValue(client, "tcpSocketSupport")).isSameAs(sockSupp);
		assertThat(TestUtils.<Boolean>getPropertyValue(client, "usingDirectBuffers")).isTrue();
		assertThat(TestUtils.<Object>getPropertyValue(client, "tcpNioConnectionSupport")).isSameAs(conSupp);
	}

}
