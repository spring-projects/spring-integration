/*
 * Copyright 2015-2024 the original author or authors.
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

package org.springframework.integration.stomp.client;

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.simp.stomp.ReactorNettyTcpStompClient;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.2
 */
public class StompServerIntegrationTests {

	private static final EmbeddedActiveMQ broker = new EmbeddedActiveMQ();

	private static final ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();

	private static ReactorNettyTcpStompClient stompClient;

	@BeforeAll
	public static void setup() throws Exception {
		ConfigurationImpl configuration =
				new ConfigurationImpl()
						.setName("embedded-server")
						.setPersistenceEnabled(false)
						.setSecurityEnabled(false)
						.setJMXManagementEnabled(false)
						.setJournalDatasync(false)
						.addAcceptorConfiguration("stomp", "tcp://127.0.0.1:" + TransportConstants.DEFAULT_STOMP_PORT)
						.addAddressSetting("#",
								new AddressSettings()
										.setDeadLetterAddress(SimpleString.of("dla"))
										.setExpiryAddress(SimpleString.of("expiry")));
		broker.setConfiguration(configuration).start();

		stompClient = new ReactorNettyTcpStompClient("127.0.0.1", TransportConstants.DEFAULT_STOMP_PORT);
		stompClient.setMessageConverter(new PassThruMessageConverter());
		taskScheduler.afterPropertiesSet();
		stompClient.setTaskScheduler(taskScheduler);
		stompClient.setReceiptTimeLimit(5000);
	}

	@AfterAll
	public static void teardown() throws Exception {
		stompClient.shutdown();
		broker.stop();
		taskScheduler.destroy();
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

		int n = 0;
		Message<?> eventMessage;
		do {
			eventMessage = stompEvents1.receive(10000);
		}
		while (eventMessage != null && !(eventMessage.getPayload() instanceof StompSessionConnectedEvent) && n++ < 100);

		assertThat(eventMessage).isNotNull();

		eventMessage = stompEvents1.receive(10000);
		assertThat(eventMessage).isNotNull();
		assertThat(eventMessage.getPayload()).isInstanceOf(StompReceiptEvent.class);
		StompReceiptEvent stompReceiptEvent = (StompReceiptEvent) eventMessage.getPayload();
		assertThat(stompReceiptEvent.getStompCommand()).isEqualTo(StompCommand.SUBSCRIBE);
		assertThat(stompReceiptEvent.getDestination()).isEqualTo("/topic/myTopic");

		eventMessage = stompEvents2.receive(10000);
		assertThat(eventMessage).isNotNull();
		assertThat(eventMessage.getPayload()).isInstanceOf(StompSessionConnectedEvent.class);

		eventMessage = stompEvents2.receive(10000);
		assertThat(eventMessage).isNotNull();
		assertThat(eventMessage.getPayload()).isInstanceOf(StompReceiptEvent.class);
		stompReceiptEvent = (StompReceiptEvent) eventMessage.getPayload();
		assertThat(stompReceiptEvent.getStompCommand()).isEqualTo(StompCommand.SUBSCRIBE);
		assertThat(stompReceiptEvent.getDestination()).isEqualTo("/topic/myTopic");

		stompOutputChannel1.send(new GenericMessage<>("Hello, Client#2!".getBytes()));

		Message<?> receive11 = stompInputChannel1.receive(10000);
		Message<?> receive21 = stompInputChannel2.receive(10000);

		assertThat(receive11).isNotNull();
		assertThat(receive21).isNotNull();

		assertThat((byte[]) receive11.getPayload()).isEqualTo("Hello, Client#2!".getBytes());
		assertThat((byte[]) receive21.getPayload()).isEqualTo("Hello, Client#2!".getBytes());

		stompOutputChannel2.send(new GenericMessage<>("Hello, Client#1!".getBytes()));

		Message<?> receive12 = stompInputChannel1.receive(10000);
		Message<?> receive22 = stompInputChannel2.receive(10000);

		assertThat(receive12).isNotNull();
		assertThat(receive22).isNotNull();

		assertThat((byte[]) receive12.getPayload()).isEqualTo("Hello, Client#1!".getBytes());
		assertThat((byte[]) receive22.getPayload()).isEqualTo("Hello, Client#1!".getBytes());

		eventMessage = stompEvents2.receive(10000);
		assertThat(eventMessage).isNotNull();
		assertThat(eventMessage.getPayload()).isInstanceOf(StompReceiptEvent.class);
		stompReceiptEvent = (StompReceiptEvent) eventMessage.getPayload();
		assertThat(stompReceiptEvent.getStompCommand()).isEqualTo(StompCommand.SEND);
		assertThat(stompReceiptEvent.getDestination()).isEqualTo("/topic/myTopic");
		assertThat((byte[]) stompReceiptEvent.getMessage().getPayload()).isEqualTo("Hello, Client#1!".getBytes());

		Lifecycle stompInboundChannelAdapter2 = context2.getBean("stompInboundChannelAdapter", Lifecycle.class);
		stompInboundChannelAdapter2.stop();

		stompOutputChannel1.send(new GenericMessage<>("How do you do?".getBytes()));

		Message<?> receive13 = stompInputChannel1.receive(10000);
		assertThat(receive13).isNotNull();

		Message<?> receive23 = stompInputChannel2.receive(100);
		assertThat(receive23).isNull();

		stompInboundChannelAdapter2.start();

		eventMessage = stompEvents2.receive(10000);
		assertThat(eventMessage).isNotNull();
		assertThat(eventMessage.getPayload()).isInstanceOf(StompReceiptEvent.class);
		stompReceiptEvent = (StompReceiptEvent) eventMessage.getPayload();
		assertThat(stompReceiptEvent.getStompCommand()).isEqualTo(StompCommand.SUBSCRIBE);
		assertThat(stompReceiptEvent.getDestination()).isEqualTo("/topic/myTopic");

		stompOutputChannel1.send(new GenericMessage<>("???".getBytes()));

		Message<?> receive24 = stompInputChannel2.receive(10000);
		assertThat(receive24).isNotNull();
		assertThat((byte[]) receive24.getPayload()).isEqualTo("???".getBytes());

		broker.stop();

		n = 0;
		do {
			eventMessage = stompEvents1.receive(10000);
			assertThat(eventMessage).isNotNull();
		}
		while (!(eventMessage.getPayload() instanceof StompConnectionFailedEvent) && n++ < 100);

		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> stompOutputChannel1.send(new GenericMessage<>("foo".getBytes())))
				.withMessageContaining("could not deliver message");

		broker.start();

		n = 0;
		do {
			eventMessage = stompEvents1.receive(20000);
			assertThat(eventMessage).isNotNull();
		}
		while (!(eventMessage.getPayload() instanceof StompReceiptEvent) && n++ < 100);

		do {
			eventMessage = stompEvents2.receive(10000);
			assertThat(eventMessage).isNotNull();
		}
		while (!(eventMessage.getPayload() instanceof StompReceiptEvent));

		stompOutputChannel1.send(new GenericMessage<>("foo".getBytes()));
		Message<?> receive25 = stompInputChannel2.receive(10000);
		assertThat(receive25).isNotNull();
		assertThat((byte[]) receive25.getPayload()).isEqualTo("foo".getBytes());

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
		public ApplicationListener<ApplicationEvent> stompEventListener() {
			ApplicationEventListeningMessageProducer producer = new ApplicationEventListeningMessageProducer();
			producer.setEventTypes(StompIntegrationEvent.class);
			producer.setOutputChannel(stompEvents());
			return producer;
		}

	}

}
