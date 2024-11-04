/*
 * Copyright 2023-2024 the original author or authors.
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

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptionsBuilder;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.mqtt.event.MqttSubscribedEvent;
import org.springframework.integration.mqtt.inbound.Mqttv5PahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.Mqttv5PahoMessageHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * @author Artem Bilan
 *
 * @since 6.0.3
 */
@SpringJUnitConfig
@DirtiesContext
public class ResubscribeAfterAutomaticReconnectTests implements MosquittoContainerTest {

	@Autowired
	@Qualifier("mqttOutFlow.input")
	private MessageChannel mqttOutFlowInput;

	@Autowired
	private PollableChannel fromMqttChannel;

	@Autowired
	private MqttConnectionOptions connectionOptions;

	@Autowired
	Mqttv5PahoMessageDrivenChannelAdapter pahoMessageDrivenChannelAdapter;

	@Autowired
	Config config;

	@Test
	void messageReceivedAfterResubscriptionOnLostConnection() throws InterruptedException {
		GenericMessage<String> testMessage = new GenericMessage<>("test");

		assertThat(this.config.subscribeFirstLatch.await(10, TimeUnit.SECONDS)).isTrue();

		this.mqttOutFlowInput.send(testMessage);
		assertThat(this.fromMqttChannel.receive(10_000)).isNotNull();

		MOSQUITTO_CONTAINER.stop();
		MOSQUITTO_CONTAINER.start();
		connectionOptions.setServerURIs(new String[] {MosquittoContainerTest.mqttUrl()});

		assertThat(this.config.subscribeSecondLatch.await(10, TimeUnit.SECONDS)).isTrue();

		await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> this.mqttOutFlowInput.send(testMessage));
		assertThat(this.fromMqttChannel.receive(10_000)).isNotNull();

		// Re-subscription on channel adapter restart with cleanStart
		this.pahoMessageDrivenChannelAdapter.stop();
		this.pahoMessageDrivenChannelAdapter.start();

		assertThat(this.config.subscribeThirdLatch.await(10, TimeUnit.SECONDS)).isTrue();

		this.mqttOutFlowInput.send(testMessage);
		assertThat(this.fromMqttChannel.receive(10_000)).isNotNull();
	}

	@Configuration
	@EnableIntegration
	public static class Config {

		CountDownLatch subscribeFirstLatch = new CountDownLatch(1);

		CountDownLatch subscribeSecondLatch = new CountDownLatch(2);

		CountDownLatch subscribeThirdLatch = new CountDownLatch(3);

		@Bean
		public MqttConnectionOptions mqttConnectOptions() {
			return new MqttConnectionOptionsBuilder()
					.serverURI(MosquittoContainerTest.mqttUrl())
					.automaticReconnect(true)
					.cleanStart(true)
					.build();
		}

		@Bean
		public IntegrationFlow mqttOutFlow(MqttConnectionOptions mqttConnectOptions) {
			Mqttv5PahoMessageHandler messageHandler =
					new Mqttv5PahoMessageHandler(mqttConnectOptions, "mqttv5SIout");
			messageHandler.setDefaultTopic("siTest");
			return f -> f.handle(messageHandler);
		}

		@Bean
		public IntegrationFlow mqttInFlow(MqttConnectionOptions mqttConnectOptions) {
			Mqttv5PahoMessageDrivenChannelAdapter messageProducer =
					new Mqttv5PahoMessageDrivenChannelAdapter(mqttConnectOptions, "mqttInClient", "siTest");
			return IntegrationFlow.from(messageProducer)
					.channel(c -> c.queue("fromMqttChannel"))
					.get();
		}

		@EventListener(MqttSubscribedEvent.class)
		public void mqttEvents() {
			this.subscribeFirstLatch.countDown();
			this.subscribeSecondLatch.countDown();
			this.subscribeThirdLatch.countDown();
		}

	}

}
