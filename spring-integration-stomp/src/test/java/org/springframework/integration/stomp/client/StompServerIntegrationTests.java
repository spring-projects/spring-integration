/*
 * Copyright 2015-2018 the original author or authors.
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

package org.springframework.integration.stomp.client;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.apache.activemq.broker.BrokerService;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.Lifecycle;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.event.inbound.ApplicationEventListeningMessageProducer;
import org.springframework.integration.stomp.AbstractStompSessionManager;
import org.springframework.integration.stomp.ReactorNettyTcpStompSessionManager;
import org.springframework.integration.stomp.StompSessionManager;
import org.springframework.integration.stomp.event.StompConnectionFailedEvent;
import org.springframework.integration.stomp.event.StompIntegrationEvent;
import org.springframework.integration.stomp.event.StompReceiptEvent;
import org.springframework.integration.stomp.event.StompSessionConnectedEvent;
import org.springframework.integration.stomp.inbound.StompInboundChannelAdapter;
import org.springframework.integration.stomp.outbound.StompMessageHandler;
import org.springframework.integration.support.converter.PassThruMessageConverter;
import org.springframework.integration.test.rule.Log4j2LevelAdjuster;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.simp.stomp.ReactorNettyTcpStompClient;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.SocketUtils;

/**
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.2
 */
public class StompServerIntegrationTests {

	private static BrokerService activeMQBroker;

	private static ReactorNettyTcpStompClient stompClient;

	@Rule
	public Log4j2LevelAdjuster adjuster =
			Log4j2LevelAdjuster.trace()
					.categories(true, "org.springframework",
							"org.apache.activemq.broker",
							"reactor.ipc",
							"io.netty");

	@BeforeClass
	public static void setup() throws Exception {
		int port = SocketUtils.findAvailableTcpPort(61613);
		activeMQBroker = new BrokerService();
		activeMQBroker.addConnector("stomp://127.0.0.1:" + port);
		activeMQBroker.setPersistent(false);
		activeMQBroker.setUseJmx(false);
		activeMQBroker.getSystemUsage().getMemoryUsage().setLimit(1024 * 1024 * 5);
		activeMQBroker.getSystemUsage().getTempUsage().setLimit(1024 * 1024 * 5);
		activeMQBroker.start();

		stompClient = new ReactorNettyTcpStompClient("127.0.0.1", port);
		stompClient.setMessageConverter(new PassThruMessageConverter());
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.afterPropertiesSet();
		stompClient.setTaskScheduler(taskScheduler);
		stompClient.setReceiptTimeLimit(5000);
	}

	@AfterClass
	public static void teardown() throws Exception {
		activeMQBroker.stop();
	}

	@Test
	public void testStompAdapters() throws Exception {
		ConfigurableApplicationContext context1 = new AnnotationConfigApplicationContext(ContextConfiguration.class);
		ConfigurableApplicationContext context2 = new AnnotationConfigApplicationContext(ContextConfiguration.class);

		PollableChannel stompEvents1 = context1.getBean("stompEvents", PollableChannel.class);
		PollableChannel stompEvents2 = context2.getBean("stompEvents", PollableChannel.class);

		PollableChannel stompInputChannel1 = context1.getBean("stompInputChannel", PollableChannel.class);
		PollableChannel stompInputChannel2 = context2.getBean("stompInputChannel", PollableChannel.class);

		MessageChannel stompOutputChannel1 = context1.getBean("stompOutputChannel", MessageChannel.class);
		MessageChannel stompOutputChannel2 = context2.getBean("stompOutputChannel", MessageChannel.class);

		Message<?> eventMessage;
		do {
			eventMessage = stompEvents1.receive(10000);
		}
		while (eventMessage != null && !(eventMessage.getPayload() instanceof StompSessionConnectedEvent));

		assertNotNull(eventMessage);

		eventMessage = stompEvents1.receive(10000);
		assertNotNull(eventMessage);
		assertThat(eventMessage.getPayload(), instanceOf(StompReceiptEvent.class));
		StompReceiptEvent stompReceiptEvent = (StompReceiptEvent) eventMessage.getPayload();
		assertEquals(StompCommand.SUBSCRIBE, stompReceiptEvent.getStompCommand());
		assertEquals("/topic/myTopic", stompReceiptEvent.getDestination());

		eventMessage = stompEvents2.receive(10000);
		assertNotNull(eventMessage);
		assertThat(eventMessage.getPayload(), instanceOf(StompSessionConnectedEvent.class));

		eventMessage = stompEvents2.receive(10000);
		assertNotNull(eventMessage);
		assertThat(eventMessage.getPayload(), instanceOf(StompReceiptEvent.class));
		stompReceiptEvent = (StompReceiptEvent) eventMessage.getPayload();
		assertEquals(StompCommand.SUBSCRIBE, stompReceiptEvent.getStompCommand());
		assertEquals("/topic/myTopic", stompReceiptEvent.getDestination());

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

		eventMessage = stompEvents2.receive(10000);
		assertNotNull(eventMessage);
		assertThat(eventMessage.getPayload(), instanceOf(StompReceiptEvent.class));
		stompReceiptEvent = (StompReceiptEvent) eventMessage.getPayload();
		assertEquals(StompCommand.SEND, stompReceiptEvent.getStompCommand());
		assertEquals("/topic/myTopic", stompReceiptEvent.getDestination());
		assertArrayEquals("Hello, Client#1!".getBytes(), (byte[]) stompReceiptEvent.getMessage().getPayload());

		Lifecycle stompInboundChannelAdapter2 = context2.getBean("stompInboundChannelAdapter", Lifecycle.class);
		stompInboundChannelAdapter2.stop();

		stompOutputChannel1.send(new GenericMessage<byte[]>("How do you do?".getBytes()));

		Message<?> receive13 = stompInputChannel1.receive(10000);
		assertNotNull(receive13);

		Message<?> receive23 = stompInputChannel2.receive(100);
		assertNull(receive23);

		stompInboundChannelAdapter2.start();

		eventMessage = stompEvents2.receive(10000);
		assertNotNull(eventMessage);
		assertThat(eventMessage.getPayload(), instanceOf(StompReceiptEvent.class));
		stompReceiptEvent = (StompReceiptEvent) eventMessage.getPayload();
		assertEquals(StompCommand.SUBSCRIBE, stompReceiptEvent.getStompCommand());
		assertEquals("/topic/myTopic", stompReceiptEvent.getDestination());

		stompOutputChannel1.send(new GenericMessage<byte[]>("???".getBytes()));

		Message<?> receive24 = stompInputChannel2.receive(10000);
		assertNotNull(receive24);
		assertArrayEquals("???".getBytes(), (byte[]) receive24.getPayload());

		activeMQBroker.stop();

		do {
			eventMessage = stompEvents1.receive(10000);
			assertNotNull(eventMessage);
		}
		while (!(eventMessage.getPayload() instanceof StompConnectionFailedEvent));

		try {
			stompOutputChannel1.send(new GenericMessage<byte[]>("foo".getBytes()));
			fail("MessageDeliveryException is expected");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(MessageDeliveryException.class));
			assertThat(e.getMessage(), containsString("could not deliver message"));
		}

		activeMQBroker.start(false);

		do {
			eventMessage = stompEvents1.receive(20000);
			assertNotNull(eventMessage);
		}
		while (!(eventMessage.getPayload() instanceof StompReceiptEvent));

		do {
			eventMessage = stompEvents2.receive(10000);
			assertNotNull(eventMessage);
		}
		while (!(eventMessage.getPayload() instanceof StompReceiptEvent));

		stompOutputChannel1.send(new GenericMessage<byte[]>("foo".getBytes()));
		Message<?> receive25 = stompInputChannel2.receive(10000);
		assertNotNull(receive25);
		assertArrayEquals("foo".getBytes(), (byte[]) receive25.getPayload());

		context1.close();
		context2.close();
	}

	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {

		@Bean
		public StompSessionManager stompSessionManager() {
			AbstractStompSessionManager stompSessionManager = new ReactorNettyTcpStompSessionManager(stompClient);
			stompSessionManager.setAutoReceipt(true);
			stompSessionManager.setRecoveryInterval(500);
			return stompSessionManager;
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
			handler.setConnectTimeout(1000);
			return handler;
		}

		@Bean
		public PollableChannel stompEvents() {
			return new QueueChannel();
		}

		@Bean
		@SuppressWarnings("unchecked")
		public ApplicationListener<ApplicationEvent> stompEventListener() {
			ApplicationEventListeningMessageProducer producer = new ApplicationEventListeningMessageProducer();
			producer.setEventTypes(StompIntegrationEvent.class);
			producer.setOutputChannel(stompEvents());
			return producer;
		}

	}

}
