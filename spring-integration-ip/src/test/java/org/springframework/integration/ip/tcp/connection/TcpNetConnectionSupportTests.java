/*
 * Copyright 2018-2022 the original author or authors.
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

import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.SocketFactory;

import org.junit.Test;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @since 5.0.3
 *
 */
public class TcpNetConnectionSupportTests {

	@Test
	public void testBadCode() throws Exception {
		TcpNetServerConnectionFactory server = new TcpNetServerConnectionFactory(0);
		AtomicReference<Message<?>> message = new AtomicReference<>();
		CountDownLatch latch1 = new CountDownLatch(1);
		server.registerListener(m -> {
			message.set(m);
			latch1.countDown();
			return false;
		});
		AtomicBoolean firstTime = new AtomicBoolean(true);
		server.setTcpNetConnectionSupport(new DefaultTcpNetConnectionSupport() {

			@Override
			public TcpNetConnection createNewConnection(Socket socket, boolean isServer, boolean lookupHost,
					ApplicationEventPublisher applicationEventPublisher, String connectionFactoryName) {

				if (firstTime.getAndSet(false)) {
					throw new RuntimeException("intended");
				}
				return super.createNewConnection(socket, isServer, lookupHost, applicationEventPublisher, connectionFactoryName);
			}

		});
		CountDownLatch latch2 = new CountDownLatch(1);
		server.setApplicationEventPublisher(e -> {
			if (e instanceof TcpConnectionServerListeningEvent) {
				latch2.countDown();
			}
		});
		server.afterPropertiesSet();
		server.start();
		assertThat(latch2.await(10, TimeUnit.SECONDS)).isTrue();
		Socket socket = SocketFactory.getDefault().createSocket("localhost", server.getPort());
		socket.close();
		socket = SocketFactory.getDefault().createSocket("localhost", server.getPort());
		socket.getOutputStream().write("foo\r\n".getBytes());
		socket.close();
		assertThat(latch1.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(message.get()).isNotNull();
		server.stop();
	}

}
