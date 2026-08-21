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

import com.hivemq.client.internal.mqtt.message.connect.mqtt3.Mqtt3ConnectView;
import com.hivemq.client.mqtt.mqtt3.Mqtt3BlockingClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.springframework.integration.mqtt.client.HiveMQContainerTest;
import com.springframework.integration.mqtt.client.event.MqttSubscribedEvent;
import com.springframework.integration.mqtt.client.inbound.Mqtt3MessageDrivenChannelAdapter;
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
class Mqtt3ClientManagerTests implements HiveMQContainerTest {

	static final String CAR_DEVICE_TOPIC = "client-manager-mqtt-v3-car-device";

	static final String PET_DEVICE_TOPIC = "client-manager-mqtt-v3-pet-device";

	static final CountDownLatch carDeviceTopicSubscribedLatch = new CountDownLatch(1);

	static final CountDownLatch petDeviceTopicSubscribedLatch = new CountDownLatch(1);

	@Autowired
	QueueChannel carDeviceOutputChannel;

	@Autowired
	QueueChannel petDeviceOutputChannel;

	Mqtt3BlockingClient mqtt3TestClient;

	@BeforeEach
	void setUp() {
		mqtt3TestClient = Mqtt3Client.builder()
				.identifier("client-manager-mqtt3-test-client")
				.serverHost(HIVEMQ_CONTAINER.getHost())
				.serverPort(HIVEMQ_CONTAINER.getFirstMappedPort())
				.buildBlocking();
		mqtt3TestClient.connect();
	}

	@Test
	void testMqtt3ClientManager() throws InterruptedException {
		// Ensure subscription done first.
		Assertions.assertThat(carDeviceTopicSubscribedLatch.await(10000, TimeUnit.MILLISECONDS)).isTrue();
		Assertions.assertThat(petDeviceTopicSubscribedLatch.await(10000, TimeUnit.MILLISECONDS)).isTrue();
		// Given
		mqtt3TestClient.publishWith().topic(CAR_DEVICE_TOPIC).payload("car-payload".getBytes()).send();
		mqtt3TestClient.publishWith().topic(PET_DEVICE_TOPIC).payload("pet-payload".getBytes()).send();
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
		Mqtt3ClientManager mqtt3ClientManager() {
			var mqtt3ClientManager = new Mqtt3ClientManager(Mqtt3Client.builder()
					.serverHost(HIVEMQ_CONTAINER.getHost())
					.serverPort(HIVEMQ_CONTAINER.getFirstMappedPort()));
			mqtt3ClientManager.setMqttConnect(Mqtt3ConnectView.DEFAULT);
			return mqtt3ClientManager;
		}

		@Bean
		QueueChannel carDeviceOutputChannel() {
			return new QueueChannel();
		}

		@Bean
		Mqtt3MessageDrivenChannelAdapter carDeviceInboundChannelAdapter(Mqtt3ClientManager mqtt3ClientManager,
				QueueChannel carDeviceOutputChannel) {
			var adapter = new Mqtt3MessageDrivenChannelAdapter(mqtt3ClientManager, CAR_DEVICE_TOPIC);
			adapter.setOutputChannel(carDeviceOutputChannel);
			return adapter;
		}

		@Bean
		QueueChannel petDeviceOutputChannel() {
			return new QueueChannel();
		}

		@Bean
		Mqtt3MessageDrivenChannelAdapter petDeviceInboundChannelAdapter(Mqtt3ClientManager mqtt3ClientManager,
				QueueChannel petDeviceOutputChannel) {
			var adapter = new Mqtt3MessageDrivenChannelAdapter(mqtt3ClientManager, PET_DEVICE_TOPIC);
			adapter.setOutputChannel(petDeviceOutputChannel);
			return adapter;
		}

		@EventListener
		void mqttEvents(MqttSubscribedEvent event) {
			String beanName = ((Mqtt3MessageDrivenChannelAdapter) event.getSource()).getBeanName();
			if (beanName.equals("carDeviceInboundChannelAdapter")) {
				carDeviceTopicSubscribedLatch.countDown();
			}
			else if (beanName.equals("petDeviceInboundChannelAdapter")) {
				petDeviceTopicSubscribedLatch.countDown();
			}
		}

	}

}


