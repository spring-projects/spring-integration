/*
 * Copyright 2022 the original author or authors.
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

/**
 * @author Gary Russell
 *
 * @since 5.3.10
 *
 */
public class TcpSenderTests {

	@Test
	void senderCalledForDeadConnectionClientNet() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		TcpNetServerConnectionFactory server = new TcpNetServerConnectionFactory(0);
		server.registerListener(msg -> false);
		server.afterPropertiesSet();
		server.setApplicationEventPublisher(event -> {
			if (event instanceof TcpConnectionServerListeningEvent) {
				latch.countDown();
			}
		});
		server.start();
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		TcpNetClientConnectionFactory client = new TcpNetClientConnectionFactory("localhost", server.getPort());
		senderCalledForDeadConnectionClient(client);
		server.stop();
	}

	@Test
	void senderCalledForDeadConnectionClientNio() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		TcpNetServerConnectionFactory server = new TcpNetServerConnectionFactory(0);
		server.registerListener(msg -> false);
		server.afterPropertiesSet();
		server.setApplicationEventPublisher(event -> {
			if (event instanceof TcpConnectionServerListeningEvent) {
				latch.countDown();
			}
		});
		server.start();
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		TcpNioClientConnectionFactory client = new TcpNioClientConnectionFactory("localhost", server.getPort());
		senderCalledForDeadConnectionClient(client);
		server.stop();
	}

	private void senderCalledForDeadConnectionClient(AbstractClientConnectionFactory client) throws InterruptedException {
		CountDownLatch adds = new CountDownLatch(2);
		CountDownLatch removes = new CountDownLatch(2);
		TcpConnectionInterceptorFactoryChain chain = new TcpConnectionInterceptorFactoryChain();
		chain.setInterceptors(new TcpConnectionInterceptorFactory[] {
				new HelloWorldInterceptorFactory() {

					@Override
					public TcpConnectionInterceptorSupport getInterceptor() {
						return new TcpConnectionInterceptorSupport() {

						};
					}

				}
		});
		client.setInterceptorFactoryChain(chain);
		client.registerSender(new TcpSender() {

			@Override
			public void addNewConnection(TcpConnection connection) {
				adds.countDown();
			}

			@Override
			public void removeDeadConnection(TcpConnection connection) {
				removes.countDown();
			}

		});
		client.setSingleUse(true);
		client.afterPropertiesSet();
		client.start();
		TcpConnectionSupport conn = client.getConnection();
		conn.close();
		conn = client.getConnection();
		assertThat(adds.await(10, TimeUnit.SECONDS)).isTrue();
		conn.close();
		client.stop();
		assertThat(removes.await(10, TimeUnit.SECONDS)).isTrue();
	}

}
