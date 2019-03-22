/*
 * Copyright 2017-2019 the original author or authors.
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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.tcp.TcpInboundGateway;
import org.springframework.integration.ip.tcp.TcpOutboundGateway;
import org.springframework.integration.transformer.ObjectToStringTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Gary Russell
 * @since 5.0
 *
 */
public class ThreadAffinityClientConnectionFactoryTests {

	private static final String PORT = "ThreadAffinityClientConnectionFactoryTests.port";

	@Test
	public void testAffinityNet() throws Exception {
		AnnotationConfigApplicationContext server = new AnnotationConfigApplicationContext(ServerNet.class);
		TcpNetServerConnectionFactory serverCF = server.getBean(TcpNetServerConnectionFactory.class);
		int port = waitForPort(serverCF);
		System.setProperty(PORT, String.valueOf(port));
		AnnotationConfigApplicationContext client = new AnnotationConfigApplicationContext(ClientNet.class);
		doTest(server, serverCF, client);
	}

	@Test
	public void testAffinityNio() throws Exception {
		AnnotationConfigApplicationContext server = new AnnotationConfigApplicationContext(ServerNio.class);
		TcpNioServerConnectionFactory serverCF = server.getBean(TcpNioServerConnectionFactory.class);
		int port = waitForPort(serverCF);
		System.setProperty(PORT, String.valueOf(port));
		AnnotationConfigApplicationContext client = new AnnotationConfigApplicationContext(ClientNio.class);
		doTest(server, serverCF, client);
	}

	private int waitForPort(AbstractServerConnectionFactory serverCF) throws InterruptedException {
		int port = serverCF.getPort();
		int n = 0;
		while (n++ < 200 && port == 0) {
			Thread.sleep(100);
			port = serverCF.getPort();
		}
		assertTrue(n < 200);
		return port;
	}

	protected void doTest(AnnotationConfigApplicationContext server, AbstractServerConnectionFactory serverCF,
			AnnotationConfigApplicationContext client) throws InterruptedException {
		MessageChannel channel = client.getBean("out", MessageChannel.class);
		QueueChannel replies = new QueueChannel();
		Message<?> message = new GenericMessage<>("foo",
				Collections.singletonMap(MessageHeaders.REPLY_CHANNEL, replies));
		channel.send(message);
		channel.send(message);
		ThreadAffinityClientConnectionFactory clientFactory = client
				.getBean(ThreadAffinityClientConnectionFactory.class);
		clientFactory.releaseConnection();
		channel.send(message);
		channel.send(message);
		clientFactory.releaseConnection();
		assertThat(replies.getQueueSize(), equalTo(4));
		Message<?> replyA = replies.receive(0);
		Message<?> replyB = replies.receive(0);
		Message<?> replyC = replies.receive(0);
		Message<?> replyD = replies.receive(0);
		assertThat((String) replyA.getPayload(), containsString("ip_connectionId"));
		assertThat(replyA.getPayload(), equalTo(replyB.getPayload()));
		assertThat(replyC.getPayload(), equalTo(replyD.getPayload()));
		assertThat(replyC.getPayload(), not(equalTo(replyA.getPayload())));
		System.getProperties().remove(PORT);
		int n = 0;
		while (n++ < 200 && serverCF.getOpenConnectionIds().size() > 0) {
			Thread.sleep(100);
		}
		assertThat(n, lessThan(200));
		client.close();
		server.close();
	}

	@Configuration
	@EnableIntegration
	public static class ServerNet {

		@Bean
		public TcpNetServerConnectionFactory sf() {
			return new TcpNetServerConnectionFactory(0);
		}

		@Bean
		public TcpInboundGateway inGate() {
			TcpInboundGateway inGate = new TcpInboundGateway();
			inGate.setConnectionFactory(sf());
			inGate.setRequestChannelName("in");
			return inGate;
		}

		@ServiceActivator(inputChannel = "in")
		public String handle(Message<?> message) {
			return IpHeaders.CONNECTION_ID + ":" + (String) message.getHeaders().get(IpHeaders.CONNECTION_ID);
		}

	}

	@Configuration
	@EnableIntegration
	public static class ClientNet {

		@Bean
		public TcpNetClientConnectionFactory cf() {
			TcpNetClientConnectionFactory cf = new TcpNetClientConnectionFactory("localhost",
					Integer.parseInt(System.getProperty(PORT)));
			cf.setSingleUse(true);
			return cf;
		}

		@Bean
		public ThreadAffinityClientConnectionFactory tacf() {
			return new ThreadAffinityClientConnectionFactory(cf());
		}

		@Bean
		@ServiceActivator(inputChannel = "out")
		public TcpOutboundGateway outGate() {
			TcpOutboundGateway outGate = new TcpOutboundGateway();
			outGate.setConnectionFactory(tacf());
			outGate.setReplyChannelName("toString");
			return outGate;
		}

		@Bean
		@Transformer(inputChannel = "toString")
		public ObjectToStringTransformer otst() {
			return new ObjectToStringTransformer();
		}

	}

	@Configuration
	@EnableIntegration
	public static class ServerNio {

		@Bean
		public TcpNioServerConnectionFactory sf() {
			return new TcpNioServerConnectionFactory(0);
		}

		@Bean
		public TcpInboundGateway inGate() {
			TcpInboundGateway inGate = new TcpInboundGateway();
			inGate.setConnectionFactory(sf());
			inGate.setRequestChannelName("in");
			return inGate;
		}

		@ServiceActivator(inputChannel = "in")
		public String handle(Message<?> message) {
			return IpHeaders.CONNECTION_ID + ":" + (String) message.getHeaders().get(IpHeaders.CONNECTION_ID);
		}

	}

	@Configuration
	@EnableIntegration
	public static class ClientNio {

		@Bean
		public TcpNioClientConnectionFactory cf() {
			TcpNioClientConnectionFactory cf = new TcpNioClientConnectionFactory("localhost",
					Integer.parseInt(System.getProperty(PORT)));
			cf.setSingleUse(true);
			return cf;
		}

		@Bean
		public ThreadAffinityClientConnectionFactory tacf() {
			return new ThreadAffinityClientConnectionFactory(cf());
		}

		@Bean
		@ServiceActivator(inputChannel = "out")
		public TcpOutboundGateway outGate() {
			TcpOutboundGateway outGate = new TcpOutboundGateway();
			outGate.setConnectionFactory(tacf());
			outGate.setReplyChannelName("toString");
			return outGate;
		}

		@Bean
		@Transformer(inputChannel = "toString")
		public ObjectToStringTransformer otst() {
			return new ObjectToStringTransformer();
		}

	}


}
