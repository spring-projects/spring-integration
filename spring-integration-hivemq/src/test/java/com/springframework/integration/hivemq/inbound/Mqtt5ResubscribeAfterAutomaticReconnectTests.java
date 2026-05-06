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

package com.springframework.integration.hivemq.inbound;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.hivemq.client.internal.mqtt.message.connect.MqttConnectBuilder;
import com.hivemq.client.internal.mqtt.message.disconnect.MqttDisconnect;
import com.hivemq.client.internal.mqtt.message.subscribe.MqttSubscription;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.springframework.integration.hivemq.HiveMQContainer;
import com.springframework.integration.hivemq.event.MqttSubscribedEvent;
import com.springframework.integration.hivemq.support.Mqtt5HeaderMapper;
import org.assertj.core.api.Assertions;
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
class Mqtt5ResubscribeAfterAutomaticReconnectTests implements HiveMQContainer {

	static final String TOPIC = "topic-for-mqtt-v5-automatic-reconnect";

	static final CountDownLatch subscribeFirstLatch = new CountDownLatch(1);

	static final CountDownLatch connectSecondLatch = new CountDownLatch(2);

	@Autowired
	Mqtt5AsyncClient mqtt5AsyncClient;

	@Autowired
	QueueChannel outputChannel;

	@Test
	void messageReceivedAfterAutomaticReConnection() throws InterruptedException {
		// subscribe done
		assertThat(subscribeFirstLatch.await(10, TimeUnit.SECONDS)).isTrue();
		// Given
		mqtt5AsyncClient.publishWith().topic(TOPIC).payload("payload-1".getBytes())
				.send().orTimeout(10000, TimeUnit.MILLISECONDS).join();
		// Then
		Assertions.assertThat(outputChannel.receive(10000)).isNotNull();
		// broken down and up
		HIVEMQ_CONTAINER.stop();
		HIVEMQ_CONTAINER.start();
		// await reconnect, manual resubscribe not need.
		Assertions.assertThat(connectSecondLatch.await(20, TimeUnit.SECONDS)).isTrue();
		// Given
		mqtt5AsyncClient.publishWith().topic(TOPIC).payload("payload-2".getBytes())
				.send().orTimeout(10000, TimeUnit.MILLISECONDS).join();
		// Then
		Assertions.assertThat(outputChannel.receive(10000)).isNotNull();
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	static class Config {

		@Bean
		Mqtt5AsyncClient mqtt5AsyncClient() {
			return Mqtt5Client.builder()
					.serverHost(HIVEMQ_CONTAINER.getHost())
					.serverPort(HIVEMQ_CONTAINER.getFirstMappedPort())
					.automaticReconnect()
					.initialDelay(1, TimeUnit.SECONDS)
					.maxDelay(2, TimeUnit.SECONDS)
					.applyAutomaticReconnect()
					.addConnectedListener(ctx -> connectSecondLatch.countDown())
					.buildAsync();
		}

		@Bean
		QueueChannel outputChannel() {
			return new QueueChannel();
		}

		@Bean
		Mqtt5MessageDrivenChannelAdapter mqtt5InboundChannelAdapter(Mqtt5AsyncClient mqtt5AsyncClient,
				QueueChannel outputChannel) {
			var adapter = new Mqtt5MessageDrivenChannelAdapter(mqtt5AsyncClient, TOPIC);
			adapter.setOutputChannel(outputChannel);
			adapter.setQos(MqttQos.AT_LEAST_ONCE);
			adapter.setMqttConnect(new MqttConnectBuilder.Default()
					.cleanStart(true) // looks even cleanStart is true, resubscribe can automatic happens after reconnect.
					.build());
			// below are default, for line coverage only
			adapter.setHeaderMapper(new Mqtt5HeaderMapper());
			adapter.setNoLocal(MqttSubscription.DEFAULT_NO_LOCAL);
			adapter.setRetainHandling(MqttSubscription.DEFAULT_RETAIN_HANDLING);
			adapter.setRetainAsPublished(MqttSubscription.DEFAULT_RETAIN_AS_PUBLISHED);
			adapter.setMqttDisconnect(MqttDisconnect.DEFAULT);
			return adapter;
		}

		@EventListener
		void mqttEvents(MqttSubscribedEvent event) {
			subscribeFirstLatch.countDown();
		}

	}

}
