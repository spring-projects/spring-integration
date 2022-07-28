/*
 * Copyright 2022-2022 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.core.Mqttv3ClientManager;
import org.springframework.integration.mqtt.core.Mqttv5ClientManager;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.inbound.Mqttv5PahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.outbound.Mqttv5PahoMessageHandler;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @author Artem Vozhdayenko
 */
class ClientManagerBackToBackTests implements MosquittoContainerTest {

	static final String SHARED_CLIENT_ID = "shared-client-id";

	public static final String TOPIC_NAME = "test-topic";

	@Test
	void testSameV3ClientIdWorksForPubAndSub() {
		testSubscribeAndPublish(Mqttv3Config.class);
	}

	@Test
	void testSameV5ClientIdWorksForPubAndSub() {
		testSubscribeAndPublish(Mqttv5Config.class);
	}

	private void testSubscribeAndPublish(Class<?> configClass) {
		try (var ctx = new AnnotationConfigApplicationContext(configClass)) {
			// given
			var input = ctx.getBean("mqttOutFlow.input", MessageChannel.class);
			var output = ctx.getBean("fromMqttChannel", PollableChannel.class);
			String testPayload = "foo";

			// when
			input.send(MessageBuilder.withPayload(testPayload).setHeader(MqttHeaders.TOPIC, TOPIC_NAME).build());
			Message<?> receive = output.receive(10_000);

			// then
			assertThat(receive).isNotNull();
			assertThat(receive.getPayload()).isEqualTo(testPayload);
		}
	}

	@Configuration
	@EnableIntegration
	public static class Mqttv3Config {

		@Bean
		public TaskScheduler taskScheduler() {
			return new ThreadPoolTaskScheduler();
		}

		@Bean
		public Mqttv3ClientManager mqttv3ClientManager(MqttPahoClientFactory pahoClientFactory) {
			return new Mqttv3ClientManager(pahoClientFactory, SHARED_CLIENT_ID);
		}

		@Bean
		public MqttPahoClientFactory pahoClientFactory() {
			var pahoClientFactory = new DefaultMqttPahoClientFactory();
			MqttConnectOptions connectionOptions = new MqttConnectOptions();
			connectionOptions.setServerURIs(new String[]{ MosquittoContainerTest.mqttUrl() });
			pahoClientFactory.setConnectionOptions(connectionOptions);
			return pahoClientFactory;
		}

		@Bean
		public IntegrationFlow mqttOutFlow(MqttPahoClientFactory pahoClientFactory,
				Mqttv3ClientManager mqttv3ClientManager) {

			var mqttHandler = new MqttPahoMessageHandler(SHARED_CLIENT_ID, pahoClientFactory);
			mqttHandler.setClientManager(mqttv3ClientManager);
			return f -> f.handle(mqttHandler);
		}

		@Bean
		public IntegrationFlow mqttInFlow(MqttPahoClientFactory pahoClientFactory,
				Mqttv3ClientManager mqttv3ClientManager) {

			var mqttAdapter = new MqttPahoMessageDrivenChannelAdapter(SHARED_CLIENT_ID, pahoClientFactory, TOPIC_NAME);
			mqttAdapter.setClientManager(mqttv3ClientManager);
			return IntegrationFlow.from(mqttAdapter)
					.channel(c -> c.queue("fromMqttChannel"))
					.get();
		}

	}

	@Configuration
	@EnableIntegration
	public static class Mqttv5Config {

		@Bean
		public TaskScheduler taskScheduler() {
			return new ThreadPoolTaskScheduler();
		}

		@Bean
		public Mqttv5ClientManager mqttv5ClientManager() {
			return new Mqttv5ClientManager(MosquittoContainerTest.mqttUrl(), SHARED_CLIENT_ID);
		}

		@Bean
		public IntegrationFlow mqttOutFlow(Mqttv5ClientManager mqttv5ClientManager) {
			var mqttHandler = new Mqttv5PahoMessageHandler(MosquittoContainerTest.mqttUrl(), SHARED_CLIENT_ID);
			mqttHandler.setClientManager(mqttv5ClientManager); // todo: add into ctor
			return f -> f.handle(mqttHandler);
		}

		@Bean
		public IntegrationFlow mqttInFlow(Mqttv5ClientManager mqttv5ClientManager) {
			var mqttAdapter = new Mqttv5PahoMessageDrivenChannelAdapter(MosquittoContainerTest.mqttUrl(), SHARED_CLIENT_ID, TOPIC_NAME);
			mqttAdapter.setClientManager(mqttv5ClientManager);
			return IntegrationFlow.from(mqttAdapter)
					.channel(c -> c.queue("fromMqttChannel"))
					.get();
		}

	}

}
