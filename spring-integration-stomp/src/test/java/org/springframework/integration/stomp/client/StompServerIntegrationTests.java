/*
 * Copyright 2015 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.integration.stomp.client;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.apache.activemq.broker.BrokerService;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.Lifecycle;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.stomp.Reactor2TcpStompSessionManager;
import org.springframework.integration.stomp.StompSessionManager;
import org.springframework.integration.stomp.inbound.StompInboundChannelAdapter;
import org.springframework.integration.stomp.outbound.StompMessageHandler;
import org.springframework.integration.support.converter.PassThruMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.simp.stomp.Reactor2TcpStompClient;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.SocketUtils;

/**
 * @author Artem Bilan
 * @since 4.2
 */
public class StompServerIntegrationTests {

	private static BrokerService activeMQBroker;

	private static Reactor2TcpStompClient stompClient;

	@BeforeClass
	public static void setup() throws Exception {
		int port = SocketUtils.findAvailableTcpPort(61613);
		activeMQBroker = new BrokerService();
		activeMQBroker.addConnector("stomp://127.0.0.1:" + port);
		activeMQBroker.setStartAsync(false);
		activeMQBroker.setPersistent(false);
		activeMQBroker.setUseJmx(false);
		activeMQBroker.getSystemUsage().getMemoryUsage().setLimit(1024 * 1024 * 5);
		activeMQBroker.getSystemUsage().getTempUsage().setLimit(1024 * 1024 * 5);
		activeMQBroker.start();
		stompClient = new Reactor2TcpStompClient("127.0.0.1", port);
		stompClient.setMessageConverter(new PassThruMessageConverter());
	}

	@AfterClass
	public static void teardown() throws Exception {
		activeMQBroker.stop();
	}

	@Test
	public void testStompAdapters() {
		ConfigurableApplicationContext context1 = new AnnotationConfigApplicationContext(ContextConfiguration.class);
		ConfigurableApplicationContext context2 = new AnnotationConfigApplicationContext(ContextConfiguration.class);

		PollableChannel stompInputChannel1 = context1.getBean("stompInputChannel", PollableChannel.class);
		PollableChannel stompInputChannel2 = context2.getBean("stompInputChannel", PollableChannel.class);

		MessageChannel stompOutputChannel1 = context1.getBean("stompOutputChannel", MessageChannel.class);
		MessageChannel stompOutputChannel2 = context2.getBean("stompOutputChannel", MessageChannel.class);

		stompOutputChannel1.send(new GenericMessage<byte[]>("Hello, Client#2!".getBytes()));

		Message<?> receive11 = stompInputChannel1.receive(10000);
		Message<?> receive21 = stompInputChannel2.receive(10000);

		assertNotNull(receive11);
		assertNotNull(receive21);

		assertArrayEquals("Hello, Client#2!".getBytes(), (byte[]) receive11.getPayload());
		assertArrayEquals("Hello, Client#2!".getBytes(), (byte[]) receive21.getPayload());

		stompOutputChannel2.send(new GenericMessage<byte[]>("Hello, Client#1!".getBytes()));

		Message<?> receive12 = stompInputChannel1.receive(10000);
		Message<?> receive22 = stompInputChannel2.receive(10000);

		assertNotNull(receive12);
		assertNotNull(receive22);

		assertArrayEquals("Hello, Client#1!".getBytes(), (byte[]) receive12.getPayload());
		assertArrayEquals("Hello, Client#1!".getBytes(), (byte[]) receive22.getPayload());

		Lifecycle stompInboundChannelAdapter2 = context2.getBean("stompInboundChannelAdapter", Lifecycle.class);
		stompInboundChannelAdapter2.stop();

		stompOutputChannel1.send(new GenericMessage<byte[]>("How do you do?".getBytes()));

		Message<?> receive13 = stompInputChannel1.receive(10000);
		assertNotNull(receive13);

		Message<?> receive23 = stompInputChannel2.receive(100);
		assertNull(receive23);

		stompInboundChannelAdapter2.start();

		stompOutputChannel1.send(new GenericMessage<byte[]>("???".getBytes()));

		Message<?> receive24 = stompInputChannel2.receive(10000);
		assertNotNull(receive24);
		assertArrayEquals("???".getBytes(), (byte[]) receive24.getPayload());

		context1.close();
		context2.close();
	}

	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {

		@Bean
		public StompSessionManager stompSessionManager() {
			return new Reactor2TcpStompSessionManager(stompClient);
		}

		@Bean
		public PollableChannel stompInputChannel() {
			return new QueueChannel();
		}

		@Bean
		public StompInboundChannelAdapter stompInboundChannelAdapter() {
			StompInboundChannelAdapter adapter =
					new StompInboundChannelAdapter(stompSessionManager(), "/topic/myTopic");
			adapter.setOutputChannel(stompInputChannel());
			return adapter;
		}

		@Bean
		@ServiceActivator(inputChannel = "stompOutputChannel")
		public MessageHandler stompMessageHandler() {
			StompMessageHandler handler = new StompMessageHandler(stompSessionManager());
			handler.setDestination("/topic/myTopic");
			return handler;
		}

	}

}
