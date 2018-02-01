/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.ip.dsl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNetClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNetServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNioClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNioServerConnectionFactory;
import org.springframework.integration.ip.util.TestingUtilities;
import org.springframework.integration.transformer.ObjectToStringTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Gary Russell
 * @author Tim Ysewyn
 * @since 5.0
 *
 */
public class ConnectionFacforyTests {

	@Test
	public void test() throws Exception {
		ApplicationEventPublisher publisher = e -> { };
		AbstractServerConnectionFactory server = Tcp.netServer(0).backlog(2).soTimeout(5000).get();
		final AtomicReference<Message<?>> received = new AtomicReference<>();
		final CountDownLatch latch = new CountDownLatch(1);
		server.registerListener(m -> {
			received.set(new ObjectToStringTransformer().transform(m));
			latch.countDown();
			return false;
		});
		server.setApplicationEventPublisher(publisher);
		server.afterPropertiesSet();
		server.start();
		TestingUtilities.waitListening(server, null);
		AbstractClientConnectionFactory client = Tcp.netClient("localhost", server.getPort()).get();
		client.setApplicationEventPublisher(publisher);
		client.afterPropertiesSet();
		client.start();
		client.getConnection().send(new GenericMessage<>("foo"));
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		assertEquals("foo", received.get().getPayload());
		client.stop();
		server.stop();
	}

	@Test
	public void shouldReturnNioFlavor() throws Exception {
		AbstractServerConnectionFactory server = Tcp.nioServer(0).get();
		assertTrue(server instanceof TcpNioServerConnectionFactory);

		AbstractClientConnectionFactory client = Tcp.nioClient("localhost", server.getPort()).get();
		assertTrue(client instanceof TcpNioClientConnectionFactory);
	}

	@Test
	public void shouldReturnNetFlavor() throws Exception {
		AbstractServerConnectionFactory server = Tcp.netServer(0).get();
		assertTrue(server instanceof TcpNetServerConnectionFactory);

		AbstractClientConnectionFactory client = Tcp.netClient("localhost", server.getPort()).get();
		assertTrue(client instanceof TcpNetClientConnectionFactory);
	}

}
