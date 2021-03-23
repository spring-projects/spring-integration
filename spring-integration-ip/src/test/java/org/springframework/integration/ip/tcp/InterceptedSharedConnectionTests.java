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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.ConsumerEndpointFactoryBean;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.HelloWorldInterceptor;
import org.springframework.integration.ip.tcp.connection.TcpConnectionOpenEvent;
import org.springframework.integration.ip.tcp.connection.TcpConnectionSupport;
import org.springframework.integration.ip.util.TestingUtilities;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Gary Russell
 * @since 2.0
 */
@SpringJUnitConfig
@DirtiesContext
public class InterceptedSharedConnectionTests {

	@Autowired
	AbstractApplicationContext ctx;

	@Autowired
	AbstractServerConnectionFactory server;

	@Autowired
	AbstractClientConnectionFactory client;

	@Autowired
	AbstractServerConnectionFactory netServer;

	@Autowired
	AbstractClientConnectionFactory netClient;

	@Autowired
	Listener listener;

	/**
	 * Tests a loopback. The client-side outbound adapter sends a message over
	 * a connection from the client connection factory; the server side
	 * receives the message, puts in on a channel which is the input channel
	 * for the outbound adapter that's sharing the connections. The response
	 * comes back to an inbound adapter that is sharing the client's
	 * connection and we verify we get the echo back as expected.
	 *
	 * @throws Exception
	 */
	@Test
	void test1() throws Exception {
		TestingUtilities.waitListening(this.server, null);
		this.client.setPort(this.server.getPort());
		this.ctx.getBeansOfType(ConsumerEndpointFactoryBean.class).values().forEach(c -> c.start());
		for (int i = 0; i < 5; i++) {
			MessageChannel input = ctx.getBean("input", MessageChannel.class);
			input.send(MessageBuilder.withPayload("Test").build());
			QueueChannel replies = ctx.getBean("replies", QueueChannel.class);
			Message<?> message = replies.receive(10000);
			assertThat(message).isNotNull();
			assertThat(message.getPayload()).isEqualTo("Test");
		}
		assertThat(this.listener.clientOpenEvent).isNotNull();
		assertThat(this.listener.clientOpenEvent.getConnectionFactoryName()).isEqualTo("client");
		assertThat(this.listener.serverOpenEvent).isNotNull();
		assertThat(this.listener.serverOpenEvent.getConnectionFactoryName()).isEqualTo("server");
	}

	@Test
	void correctOpenNetEventPublished() throws InterruptedException {
		TestingUtilities.waitListening(this.netServer, null);
		this.listener.clientOpenEvent = null;
		this.listener.serverOpenEvent = null;
		this.netClient.setPort(this.netServer.getPort());
		this.netClient.start();
		TcpConnectionSupport conn = this.netClient.getConnection();
		conn.send(new GenericMessage<>("foo"));
		conn.close();
		assertThat(this.listener.latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(this.listener.clientOpenEvent).isNotNull();
		assertThat(this.listener.clientOpenEvent.getConnectionFactoryName()).isEqualTo("netClient");
		assertThat(this.listener.serverOpenEvent).isNotNull();
		assertThat(this.listener.serverOpenEvent.getConnectionFactoryName()).isEqualTo("netServer");
	}

	public static class Listener implements ApplicationListener<TcpConnectionOpenEvent> {

		final CountDownLatch latch = new CountDownLatch(2);

		volatile TcpConnectionOpenEvent clientOpenEvent;

		volatile TcpConnectionOpenEvent serverOpenEvent;

		@Override
		public void onApplicationEvent(TcpConnectionOpenEvent event) {
			if (event.getSource() instanceof HelloWorldInterceptor) {
				if (event.getConnectionFactoryName().startsWith("net")) {
					this.latch.countDown();
				}
				if (event.getConnectionFactoryName().contains("lient")) {
					this.clientOpenEvent = event;
				}
				else {
					this.serverOpenEvent = event;
				}
			}
		}

	}

}
