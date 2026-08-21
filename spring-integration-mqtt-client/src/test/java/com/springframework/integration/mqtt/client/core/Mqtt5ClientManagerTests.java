/*
 * Copyright 2026-present the original author or authors.
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

package com.springframework.integration.mqtt.client.core;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.hivemq.client.internal.mqtt.message.connect.MqttConnect;
import com.hivemq.client.internal.mqtt.message.disconnect.MqttDisconnect;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.springframework.integration.mqtt.client.HiveMQContainerTest;
import com.springframework.integration.mqtt.client.event.MqttSubscribedEvent;
import com.springframework.integration.mqtt.client.inbound.Mqtt5MessageDrivenChannelAdapter;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Jiandong Ma
 *
 * @since 7.2
 */
@SpringJUnitConfig
@DirtiesContext
class Mqtt5ClientManagerTests implements HiveMQContainerTest {

	static final String CAR_DEVICE_TOPIC = "client-manager-mqtt-v5-car-device";

	static final String PET_DEVICE_TOPIC = "client-manager-mqtt-v5-pet-device";

	static final CountDownLatch carDeviceTopicSubscribedLatch = new CountDownLatch(1);

	static final CountDownLatch petDeviceTopicSubscribedLatch = new CountDownLatch(1);

	@Autowired
	QueueChannel carDeviceOutputChannel;

	@Autowired
	QueueChannel petDeviceOutputChannel;

	Mqtt5BlockingClient mqtt5TestClient;

	@BeforeEach
	void setUp() {
		mqtt5TestClient = Mqtt5Client.builder()
				.identifier("client-manager-mqtt5-test-client")
				.serverHost(HIVEMQ_CONTAINER.getHost())
				.serverPort(HIVEMQ_CONTAINER.getFirstMappedPort())
				.buildBlocking();
		mqtt5TestClient.connect();
	}

	@Test
	void testMqtt5ClientManager() throws InterruptedException {
		// Ensure subscription done first.
		Assertions.assertThat(carDeviceTopicSubscribedLatch.await(10000, TimeUnit.MILLISECONDS)).isTrue();
		Assertions.assertThat(petDeviceTopicSubscribedLatch.await(10000, TimeUnit.MILLISECONDS)).isTrue();
		// Given
		mqtt5TestClient.publishWith().topic(CAR_DEVICE_TOPIC).payload("car-payload".getBytes()).send();
		mqtt5TestClient.publishWith().topic(PET_DEVICE_TOPIC).payload("pet-payload".getBytes()).send();
		// When
		Message<?> carDeviceMessage = carDeviceOutputChannel.receive(10000);
		Message<?> petDeviceMessage = petDeviceOutputChannel.receive(10000);
		// Then
		Assertions.assertThat(carDeviceMessage).isNotNull().returns("car-payload".getBytes(), Message::getPayload);
		Assertions.assertThat(petDeviceMessage).isNotNull().returns("pet-payload".getBytes(), Message::getPayload);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	static class Config {

		@Bean
		Mqtt5ClientManager mqtt5ClientManager() {
			var mqtt5ClientManager = new Mqtt5ClientManager(Mqtt5Client.builder()
					.serverHost(HIVEMQ_CONTAINER.getHost())
					.serverPort(HIVEMQ_CONTAINER.getFirstMappedPort()));
			mqtt5ClientManager.setMqttConnect(MqttConnect.DEFAULT);
			mqtt5ClientManager.setMqttDisconnect(MqttDisconnect.DEFAULT);
			return mqtt5ClientManager;
		}

		@Bean
		QueueChannel carDeviceOutputChannel() {
			return new QueueChannel();
		}

		@Bean
		Mqtt5MessageDrivenChannelAdapter carDeviceInboundChannelAdapter(Mqtt5ClientManager mqtt5ClientManager,
				QueueChannel carDeviceOutputChannel) {
			var adapter = new Mqtt5MessageDrivenChannelAdapter(mqtt5ClientManager, CAR_DEVICE_TOPIC);
			adapter.setOutputChannel(carDeviceOutputChannel);
			return adapter;
		}

		@Bean
		QueueChannel petDeviceOutputChannel() {
			return new QueueChannel();
		}

		@Bean
		Mqtt5MessageDrivenChannelAdapter petDeviceInboundChannelAdapter(Mqtt5ClientManager mqtt5ClientManager,
				QueueChannel petDeviceOutputChannel) {
			var adapter = new Mqtt5MessageDrivenChannelAdapter(mqtt5ClientManager, PET_DEVICE_TOPIC);
			adapter.setOutputChannel(petDeviceOutputChannel);
			return adapter;
		}

		@EventListener
		void mqttEvents(MqttSubscribedEvent event) {
			String beanName = ((Mqtt5MessageDrivenChannelAdapter) event.getSource()).getBeanName();
			if (beanName.equals("carDeviceInboundChannelAdapter")) {
				carDeviceTopicSubscribedLatch.countDown();
			}
			else if (beanName.equals("petDeviceInboundChannelAdapter")) {
				petDeviceTopicSubscribedLatch.countDown();
			}
		}

	}

}


