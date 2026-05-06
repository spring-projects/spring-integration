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

package com.springframework.integration.mqtt.client.inbound;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.hivemq.client.internal.mqtt.message.connect.mqtt3.Mqtt3ConnectViewBuilder;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3BlockingClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.Mqtt3ClientBuilder;
import com.springframework.integration.mqtt.client.HiveMQContainer;
import com.springframework.integration.mqtt.client.event.MqttSubscribedEvent;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Jiandong Ma
 *
 * @since 7.2
 */
@SpringJUnitConfig
@DirtiesContext
class Mqtt3ResubscribeAfterAutomaticReconnectTests implements HiveMQContainer {

	static final String TOPIC = "topic-for-mqtt-v3-automatic-reconnect";

	static final CountDownLatch subscribeFirstLatch = new CountDownLatch(1);

	static final CountDownLatch connectSecondLatch = new CountDownLatch(2);

	@Autowired
	QueueChannel outputChannel;

	Mqtt3BlockingClient mqtt3TestClient;

	@BeforeEach
	void setUp() {
		mqtt3TestClient = Mqtt3Client.builder()
				.identifier("mqtt3-reconnect-test-client")
				.serverHost(HIVEMQ_CONTAINER.getHost())
				.serverPort(HIVEMQ_CONTAINER.getFirstMappedPort())
				.buildBlocking();
		mqtt3TestClient.connect();
	}

	@Test
	void messageReceivedAfterAutomaticReConnection() throws InterruptedException {
		// subscribe done
		assertThat(subscribeFirstLatch.await(10, TimeUnit.SECONDS)).isTrue();
		// Given
		mqtt3TestClient.publishWith().topic(TOPIC).payload("payload-1".getBytes()).send();
		// Then
		Assertions.assertThat(outputChannel.receive(10000)).isNotNull();
		// broken down and up
		HIVEMQ_CONTAINER.stop();
		HIVEMQ_CONTAINER.start();
		// await reconnect, manual resubscribe not need.
		Assertions.assertThat(connectSecondLatch.await(30, TimeUnit.SECONDS)).isTrue();
		// Given
		mqtt3TestClient.connect();
		mqtt3TestClient.publishWith().topic(TOPIC).payload("payload-2".getBytes()).send();
		// Then
		Assertions.assertThat(outputChannel.receive(10000)).isNotNull();
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	static class Config {

		@Bean
		Mqtt3ClientBuilder mqtt3ClientBuilder() {
			return Mqtt3Client.builder()
					.serverHost(HIVEMQ_CONTAINER.getHost())
					.serverPort(HIVEMQ_CONTAINER.getFirstMappedPort())
					.automaticReconnect()
					.initialDelay(1, TimeUnit.SECONDS)
					.maxDelay(2, TimeUnit.SECONDS)
					.applyAutomaticReconnect()
					.addConnectedListener(ctx -> connectSecondLatch.countDown());
		}

		@Bean
		QueueChannel outputChannel() {
			return new QueueChannel();
		}

		@Bean
		Mqtt3MessageDrivenChannelAdapter mqtt3InboundChannelAdapter(Mqtt3ClientBuilder mqtt3ClientBuilder,
				QueueChannel outputChannel) {
			var adapter = new Mqtt3MessageDrivenChannelAdapter(mqtt3ClientBuilder, TOPIC);
			adapter.setOutputChannel(outputChannel);
			adapter.setQos(MqttQos.AT_LEAST_ONCE);
			adapter.setMqttConnect(new Mqtt3ConnectViewBuilder.Default()
					.cleanSession(true) // looks even cleanStart is true, resubscribe can automatic happens after reconnect.
					.build());
			return adapter;
		}

		@EventListener
		void mqttEvents(MqttSubscribedEvent event) {
			subscribeFirstLatch.countDown();
		}

	}

}
