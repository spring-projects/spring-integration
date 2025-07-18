/*
 * Copyright 2022-present the original author or authors.
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

package org.springframework.integration.mqtt;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.mqtt.core.Mqttv3ClientManager;
import org.springframework.integration.mqtt.core.Mqttv5ClientManager;
import org.springframework.integration.mqtt.event.MqttMessageDeliveryEvent;
import org.springframework.integration.mqtt.event.MqttSubscribedEvent;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.inbound.Mqttv5PahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.outbound.Mqttv5PahoMessageHandler;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * @author Artem Vozhdayenko
 * @author Artem Bilan
 *
 * @since 6.0
 */
class ClientManagerBackToBackTests implements MosquittoContainerTest {

	private static final long QUIESCENT_TIMEOUT = 1L;

	private static final long DISCONNECT_COMPLETION_TIMEOUT = 1L;

	@Test
	void testSameV3ClientIdWorksForPubAndSub() throws Exception {
		testSubscribeAndPublish(Mqttv3Config.class, Mqttv3Config.TOPIC_NAME, Mqttv3Config.subscribedLatch);
	}

	@Test
	void testSameV5ClientIdWorksForPubAndSub() throws Exception {
		testSubscribeAndPublish(Mqttv5Config.class, Mqttv5Config.TOPIC_NAME, Mqttv5Config.subscribedLatch);
	}

	@Test
	void testV3ClientManagerReconnect() throws Exception {
		testSubscribeAndPublish(Mqttv3ConfigWithDisconnect.class, Mqttv3ConfigWithDisconnect.TOPIC_NAME,
				Mqttv3ConfigWithDisconnect.subscribedLatch);
	}

	@Test
	void testV3ClientManagerRuntime() throws Exception {
		testSubscribeAndPublishRuntime(Mqttv3ConfigRuntime.class, Mqttv3ConfigRuntime.TOPIC_NAME,
				Mqttv3ConfigRuntime.subscribedLatch);
	}

	@Test
	void testV5ClientManagerReconnect() throws Exception {
		testSubscribeAndPublish(Mqttv5ConfigWithDisconnect.class, Mqttv5ConfigWithDisconnect.TOPIC_NAME,
				Mqttv5ConfigWithDisconnect.subscribedLatch);
	}

	@Test
	void testV5ClientManagerRuntime() throws Exception {
		testSubscribeAndPublishRuntime(Mqttv5ConfigRuntime.class, Mqttv5ConfigRuntime.TOPIC_NAME,
				Mqttv5ConfigRuntime.subscribedLatch);
	}

	@SuppressWarnings("unchecked")
	private void testSubscribeAndPublish(Class<?> configClass, String topicName, CountDownLatch subscribedLatch)
			throws Exception {

		try (var ctx = new AnnotationConfigApplicationContext(configClass)) {
			// given
			var input = ctx.getBean("mqttOutFlow.input", MessageChannel.class);
			var output = ctx.getBean("fromMqttChannel", PollableChannel.class);
			String testPayload = "foo";
			assertThat(subscribedLatch.await(20, TimeUnit.SECONDS)).isTrue();

			// when
			input.send(MessageBuilder.withPayload(testPayload).setHeader(MqttHeaders.TOPIC, topicName).build());
			Message<?> receive = output.receive(20_000);

			// then
			assertThat(receive).isNotNull();
			Object payload = receive.getPayload();
			if (payload instanceof String sp) {
				assertThat(sp).isEqualTo(testPayload);
			}
			else {
				assertThat(payload).isEqualTo(testPayload.getBytes(StandardCharsets.UTF_8));
			}

			if (ctx.containsBean("deliveryEvents")) {
				List<MqttMessageDeliveryEvent> deliveryEvents = ctx.getBean("deliveryEvents", List.class);
				// MqttMessageSentEvent and  MqttMessageDeliveredEvent
				await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(deliveryEvents).hasSize(2));
			}
		}
	}

	private void testSubscribeAndPublishRuntime(Class<?> configClass, String topicName, CountDownLatch subscribedLatch)
			throws Exception {

		try (var ctx = new AnnotationConfigApplicationContext(configClass)) {
			// given
			var input = ctx.getBean("mqttOutFlow.input", MessageChannel.class);
			var flowContext = ctx.getBean(IntegrationFlowContext.class);
			var factory = ctx.getBean(MessageDrivenChannelAdapterFactory.class);
			var output = new QueueChannel();

			flowContext.registration(IntegrationFlow
					.from(factory.createMessageDrivenAdapter(ctx))
					.channel(output)
					.get()).register();
			String testPayload = "foo";
			assertThat(subscribedLatch.await(20, TimeUnit.SECONDS)).isTrue();

			// when
			input.send(MessageBuilder.withPayload(testPayload).setHeader(MqttHeaders.TOPIC, topicName).build());
			Message<?> receive = output.receive(20_000);

			// then
			assertThat(receive).isNotNull();
			Object payload = receive.getPayload();
			if (payload instanceof String sp) {
				assertThat(sp).isEqualTo(testPayload);
			}
			else {
				assertThat(payload).isEqualTo(testPayload.getBytes(StandardCharsets.UTF_8));
			}
		}
	}

	@Configuration
	@EnableIntegration
	public static class Mqttv3Config {

		static final String TOPIC_NAME = "test-topic-v3";

		static final CountDownLatch subscribedLatch = new CountDownLatch(1);

		@EventListener
		public void onSubscribed(MqttSubscribedEvent e) {
			subscribedLatch.countDown();
		}

		@EventListener
		void mqttEvents(MqttMessageDeliveryEvent event) {
			deliveryEvents().add(event);
		}

		@Bean
		List<MqttMessageDeliveryEvent> deliveryEvents() {
			return new ArrayList<>();
		}

		@Bean
		public Mqttv3ClientManager mqttv3ClientManager() {
			MqttConnectOptions connectionOptions = new MqttConnectOptions();
			connectionOptions.setServerURIs(new String[] {MosquittoContainerTest.mqttUrl()});
			connectionOptions.setAutomaticReconnect(true);
			Mqttv3ClientManager result = new Mqttv3ClientManager(connectionOptions, "client-manager-client-id-v3");
			result.setQuiescentTimeout(QUIESCENT_TIMEOUT);
			result.setDisconnectCompletionTimeout(DISCONNECT_COMPLETION_TIMEOUT);
			return result;
		}

		@Bean
		public IntegrationFlow mqttOutFlow(Mqttv3ClientManager mqttv3ClientManager) {
			MqttPahoMessageHandler mqttPahoMessageHandler = new MqttPahoMessageHandler(mqttv3ClientManager);
			mqttPahoMessageHandler.setAsync(true);
			mqttPahoMessageHandler.setAsyncEvents(true);
			return f -> f.handle(mqttPahoMessageHandler);
		}

		@Bean
		public IntegrationFlow mqttInFlow(Mqttv3ClientManager mqttv3ClientManager) {
			return IntegrationFlow.from(new MqttPahoMessageDrivenChannelAdapter(mqttv3ClientManager, TOPIC_NAME))
					.channel(c -> c.queue("fromMqttChannel"))
					.get();
		}

	}

	@Configuration
	@EnableIntegration
	public static class Mqttv3ConfigWithDisconnect {

		static final String TOPIC_NAME = "test-topic-v3-reconnect";

		static final CountDownLatch subscribedLatch = new CountDownLatch(1);

		@EventListener
		public void onSubscribed(MqttSubscribedEvent e) {
			subscribedLatch.countDown();
		}

		@Bean
		public ClientV3Disconnector disconnector(Mqttv3ClientManager clientManager) {
			return new ClientV3Disconnector(clientManager);
		}

		@Bean
		public Mqttv3ClientManager mqttv3ClientManager() {
			MqttConnectOptions connectionOptions = new MqttConnectOptions();
			connectionOptions.setServerURIs(new String[] {MosquittoContainerTest.mqttUrl()});
			connectionOptions.setAutomaticReconnect(true);
			Mqttv3ClientManager result = new Mqttv3ClientManager(connectionOptions, "client-manager-client-id-v3-reconnect");
			result.setQuiescentTimeout(QUIESCENT_TIMEOUT);
			result.setDisconnectCompletionTimeout(DISCONNECT_COMPLETION_TIMEOUT);
			return result;
		}

		@Bean
		public IntegrationFlow mqttOutFlow() {
			return f -> f.handle(new MqttPahoMessageHandler(MosquittoContainerTest.mqttUrl(), "old-client-v3"));
		}

		@Bean
		public IntegrationFlow mqttInFlow(Mqttv3ClientManager mqttv3ClientManager) {
			return IntegrationFlow.from(new MqttPahoMessageDrivenChannelAdapter(mqttv3ClientManager, TOPIC_NAME))
					.channel(c -> c.queue("fromMqttChannel"))
					.get();
		}

	}

	@Configuration
	@EnableIntegration
	public static class Mqttv3ConfigRuntime implements MessageDrivenChannelAdapterFactory {

		static final String TOPIC_NAME = "test-topic-v3";

		static final CountDownLatch subscribedLatch = new CountDownLatch(1);

		@EventListener
		public void onSubscribed(MqttSubscribedEvent e) {
			subscribedLatch.countDown();
		}

		@Bean
		public Mqttv3ClientManager mqttv3ClientManager() {
			MqttConnectOptions connectionOptions = new MqttConnectOptions();
			connectionOptions.setServerURIs(new String[] {MosquittoContainerTest.mqttUrl()});
			connectionOptions.setAutomaticReconnect(true);
			Mqttv3ClientManager result = new Mqttv3ClientManager(connectionOptions, "client-manager-client-id-v3");
			result.setQuiescentTimeout(QUIESCENT_TIMEOUT);
			result.setDisconnectCompletionTimeout(DISCONNECT_COMPLETION_TIMEOUT);
			return result;
		}

		@Bean
		public IntegrationFlow mqttOutFlow(Mqttv3ClientManager mqttv3ClientManager) {
			return f -> f.handle(new MqttPahoMessageHandler(mqttv3ClientManager));
		}

		@Override
		public MessageProducerSupport createMessageDrivenAdapter(ApplicationContext ctx) {
			var clientManager = ctx.getBean(Mqttv3ClientManager.class);
			return new MqttPahoMessageDrivenChannelAdapter(clientManager, TOPIC_NAME);
		}

	}

	@Configuration
	@EnableIntegration
	public static class Mqttv5Config {

		static final String TOPIC_NAME = "test-topic-v5";

		static final CountDownLatch subscribedLatch = new CountDownLatch(1);

		@EventListener
		public void onSubscribed(MqttSubscribedEvent e) {
			subscribedLatch.countDown();
		}

		@EventListener
		void mqttEvents(MqttMessageDeliveryEvent event) {
			deliveryEvents().add(event);
		}

		@Bean
		List<MqttMessageDeliveryEvent> deliveryEvents() {
			return new ArrayList<>();
		}

		@Bean
		public Mqttv5ClientManager mqttv5ClientManager() {
			return new Mqttv5ClientManager(MosquittoContainerTest.mqttUrl(), "client-manager-client-id-v5");
		}

		@Bean
		@ServiceActivator(inputChannel = "mqttOutFlow.input")
		public Mqttv5PahoMessageHandler mqttv5PahoMessageHandler(Mqttv5ClientManager mqttv5ClientManager) {
			Mqttv5PahoMessageHandler mqttPahoMessageHandler = new Mqttv5PahoMessageHandler(mqttv5ClientManager);
			mqttPahoMessageHandler.setAsync(true);
			mqttPahoMessageHandler.setAsyncEvents(true);
			return mqttPahoMessageHandler;
		}

		@Bean
		public IntegrationFlow mqttInFlow(Mqttv5ClientManager mqttv5ClientManager) {
			return IntegrationFlow.from(new Mqttv5PahoMessageDrivenChannelAdapter(mqttv5ClientManager, TOPIC_NAME))
					.channel(c -> c.queue("fromMqttChannel"))
					.get();
		}

	}

	@Configuration
	@EnableIntegration
	public static class Mqttv5ConfigWithDisconnect {

		static final String TOPIC_NAME = "test-topic-v5-reconnect";

		static final CountDownLatch subscribedLatch = new CountDownLatch(1);

		@EventListener
		public void onSubscribed(MqttSubscribedEvent e) {
			subscribedLatch.countDown();
		}

		@Bean
		public ClientV5Disconnector clientV3Disconnector(Mqttv5ClientManager clientManager) {
			return new ClientV5Disconnector(clientManager);
		}

		@Bean
		public Mqttv5ClientManager mqttv5ClientManager() {
			return new Mqttv5ClientManager(MosquittoContainerTest.mqttUrl(), "client-manager-client-id-v5-reconnect");
		}

		@Bean
		public IntegrationFlow mqttOutFlow(Mqttv5ClientManager mqttv5ClientManager) {
			return f -> f.handle(new Mqttv5PahoMessageHandler(MosquittoContainerTest.mqttUrl(), "old-client-v5"));
		}

		@Bean
		public IntegrationFlow mqttInFlow(Mqttv5ClientManager mqttv5ClientManager) {
			return IntegrationFlow.from(new Mqttv5PahoMessageDrivenChannelAdapter(mqttv5ClientManager, TOPIC_NAME))
					.channel(c -> c.queue("fromMqttChannel"))
					.get();
		}

	}

	@Configuration
	@EnableIntegration
	public static class Mqttv5ConfigRuntime implements MessageDrivenChannelAdapterFactory {

		static final String TOPIC_NAME = "test-topic-v5";

		static final CountDownLatch subscribedLatch = new CountDownLatch(1);

		@EventListener
		public void onSubscribed(MqttSubscribedEvent e) {
			subscribedLatch.countDown();
		}

		@Bean
		public Mqttv5ClientManager mqttv5ClientManager() {
			return new Mqttv5ClientManager(MosquittoContainerTest.mqttUrl(), "client-manager-client-id-v5");
		}

		@Bean
		@ServiceActivator(inputChannel = "mqttOutFlow.input")
		public Mqttv5PahoMessageHandler mqttv5PahoMessageHandler(Mqttv5ClientManager mqttv5ClientManager) {
			return new Mqttv5PahoMessageHandler(mqttv5ClientManager);
		}

		@Override
		public MessageProducerSupport createMessageDrivenAdapter(ApplicationContext ctx) {
			var clientManager = ctx.getBean(Mqttv5ClientManager.class);
			return new Mqttv5PahoMessageDrivenChannelAdapter(clientManager, TOPIC_NAME);
		}

	}

	interface MessageDrivenChannelAdapterFactory {

		MessageProducerSupport createMessageDrivenAdapter(ApplicationContext ctx);

	}

	record ClientV3Disconnector(Mqttv3ClientManager clientManager) {

		@EventListener(MqttSubscribedEvent.class)
		public void handleSubscribedEvent() {
			try {
				this.clientManager.getClient().disconnectForcibly();
			}
			catch (MqttException ex) {
				throw new IllegalStateException("could not disconnect the client!");
			}
		}

	}

	record ClientV5Disconnector(Mqttv5ClientManager clientManager) {

		@EventListener(MqttSubscribedEvent.class)
		public void handleSubscribedEvent() {
			try {
				this.clientManager.getClient().disconnectForcibly();
			}
			catch (org.eclipse.paho.mqttv5.common.MqttException ex) {
				throw new IllegalStateException("could not disconnect the client!");
			}
		}

	}

}
