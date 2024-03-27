/*
 * Copyright 2022-2024 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Kazuki Shimizu
 * @author Artem Bilan
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
		CountDownLatch interceptorAddCalled = new CountDownLatch(6);
		CountDownLatch interceptorRemCalled = new CountDownLatch(6);
		TcpConnectionInterceptorFactoryChain chain = new TcpConnectionInterceptorFactoryChain();
		AtomicInteger instances = new AtomicInteger();
		List<Integer> addOrder = Collections.synchronizedList(new ArrayList<>());
		List<Integer> remOrder = Collections.synchronizedList(new ArrayList<>());
		Map<Integer, TcpConnection> interceptorsPerInstance = new HashMap<>();
		List<TcpConnection> passedConnectionsToSenderViaAddNewConnection = new ArrayList<>();
		class InterceptorFactory extends HelloWorldInterceptorFactory {

			@Override
			public TcpConnectionInterceptorSupport getInterceptor() {

				TcpConnectionInterceptorSupport interceptor = new TcpConnectionInterceptorSupport() {

					private final int instance = instances.incrementAndGet();

					@Override
					public void addNewConnection(TcpConnection connection) {
						addOrder.add(this.instance);
						interceptorAddCalled.countDown();
						super.addNewConnection(connection);
					}

					@Override
					public synchronized void removeDeadConnection(TcpConnection connection) {
						super.removeDeadConnection(connection);
						// can be called multiple times on different threads.
						if (!remOrder.contains(this.instance)) {
							remOrder.add(this.instance);
							interceptorRemCalled.countDown();
						}
					}

				};
				interceptorsPerInstance.put(instances.get(), interceptor);
				return interceptor;
			}

		}

		chain.setInterceptor(new InterceptorFactory(), new InterceptorFactory(), new InterceptorFactory());
		client.setInterceptorFactoryChain(chain);
		CountDownLatch firstClosed = new CountDownLatch(1);
		client.registerSender(new TcpSender() {

			@Override
			public void addNewConnection(TcpConnection connection) {
				addOrder.add(99);
				passedConnectionsToSenderViaAddNewConnection.add(connection);
				adds.countDown();
			}

			@Override
			public synchronized void removeDeadConnection(TcpConnection connection) {
				remOrder.add(99);
				removes.countDown();
				firstClosed.countDown();
			}

		});
		client.setSingleUse(true);
		client.afterPropertiesSet();
		client.start();
		TcpConnectionSupport conn = client.getConnection();
		assertThat(((TcpConnectionInterceptorSupport) conn).hasRealSender()).isTrue();
		conn.close();
		conn = client.getConnection();
		assertThat(adds.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(addOrder).containsExactly(1, 2, 3, 99, 4, 5, 6, 99);
		assertThat(firstClosed.await(10, TimeUnit.SECONDS)).isTrue();
		conn.close();
		client.stop();
		assertThat(removes.await(10, TimeUnit.SECONDS)).isTrue();
		// 9x before 3, 6 due to ordering in overridden interceptor method
		assertThat(remOrder).containsExactly(1, 2, 99, 3, 4, 5, 99, 6);
		assertThat(interceptorAddCalled.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(interceptorRemCalled.await(10, TimeUnit.SECONDS)).isTrue();
		// should be passed the last interceptor to the real sender via addNewConnection method
		assertThat(passedConnectionsToSenderViaAddNewConnection.get(0)).isSameAs(interceptorsPerInstance.get(3));
		assertThat(passedConnectionsToSenderViaAddNewConnection.get(1)).isSameAs(interceptorsPerInstance.get(6));
	}

}
