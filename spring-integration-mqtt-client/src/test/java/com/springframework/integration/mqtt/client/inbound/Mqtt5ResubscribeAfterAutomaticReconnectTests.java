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

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.hivemq.client.internal.mqtt.message.connect.MqttConnectBuilder;
import com.hivemq.client.internal.mqtt.message.disconnect.MqttDisconnect;
import com.hivemq.client.internal.mqtt.message.subscribe.MqttSubscription;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.Mqtt5ClientBuilder;
import com.springframework.integration.mqtt.client.HiveMQContainerTest;
import com.springframework.integration.mqtt.client.ToxiproxyContainerTest;
import com.springframework.integration.mqtt.client.event.MqttSubscribedEvent;
import com.springframework.integration.mqtt.client.support.Mqtt5HeaderMapper;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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
class Mqtt5ResubscribeAfterAutomaticReconnectTests implements HiveMQContainerTest, ToxiproxyContainerTest {

	static final String TOPIC = "topic-for-mqtt-v5-automatic-reconnect";

	static final CountDownLatch subscribedLatch = new CountDownLatch(1);

	static final CountDownLatch connectedLatches = new CountDownLatch(2);

	static final CountDownLatch disconnectedLatch = new CountDownLatch(1);

	@Autowired
	QueueChannel outputChannel;

	static Mqtt5BlockingClient mqtt5TestClient;

	static Proxy toxiproxy;

	@BeforeAll
	static void setup() throws IOException {
		var proxyClient = new ToxiproxyClient(PROXY_CONTAINER.getHost(), PROXY_CONTAINER.getControlPort());
		toxiproxy = proxyClient.createProxy("hivemqProxy", "0.0.0.0:" + PROXY_PORT_FOR_HIVEMQ, "hivemq-broker:" + HIVEMQ_PORT);
		toxiproxy.enable();

		mqtt5TestClient = Mqtt5Client.builder()
				.identifier("mqtt5-reconnect-test-client")
				.serverHost(PROXY_CONTAINER.getHost())
				.serverPort(PROXY_CONTAINER.getMappedPort(PROXY_PORT_FOR_HIVEMQ))
				.buildBlocking();
		mqtt5TestClient.connect();
	}

	@AfterAll
	static void cleanup() throws IOException {
		if (mqtt5TestClient != null && mqtt5TestClient.getState().isConnected()) {
			mqtt5TestClient.disconnect();
		}

		if (toxiproxy != null) {
			toxiproxy.disable();
			toxiproxy.delete();
		}
	}

	@Test
	void messageReceivedAfterAutomaticReConnection() throws InterruptedException, IOException {
		// subscribe done
		assertThat(subscribedLatch.await(10, TimeUnit.SECONDS)).isTrue();
		// Given
		mqtt5TestClient.publishWith().topic(TOPIC).payload("payload-1".getBytes()).send();
		// Then
		Assertions.assertThat(outputChannel.receive(10000)).isNotNull();

		// broker down and up
		toxiproxy.disable();
		Assertions.assertThat(disconnectedLatch.await(30, TimeUnit.SECONDS)).isTrue();
		toxiproxy.enable();
		// await reconnect, manual resubscribe does not need.
		Assertions.assertThat(connectedLatches.await(30, TimeUnit.SECONDS)).isTrue();

		// Given
		mqtt5TestClient.connect();
		mqtt5TestClient.publishWith().topic(TOPIC).payload("payload-2".getBytes()).send();
		// Then
		Assertions.assertThat(outputChannel.receive(10000)).isNotNull();
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	static class Config {

		@Bean
		Mqtt5ClientBuilder mqtt5ClientBuilder() {
			return Mqtt5Client.builder()
					.serverHost(PROXY_CONTAINER.getHost())
					.serverPort(PROXY_CONTAINER.getMappedPort(PROXY_PORT_FOR_HIVEMQ))
					.automaticReconnect()
					.initialDelay(1, TimeUnit.SECONDS)
					.maxDelay(2, TimeUnit.SECONDS)
					.applyAutomaticReconnect()
					.addConnectedListener(ctx -> connectedLatches.countDown())
					.addDisconnectedListener(ctx -> disconnectedLatch.countDown());
		}

		@Bean
		QueueChannel outputChannel() {
			return new QueueChannel();
		}

		@Bean
		Mqtt5MessageDrivenChannelAdapter mqtt5InboundChannelAdapter(Mqtt5ClientBuilder mqtt5ClientBuilder,
				QueueChannel outputChannel) {
			var adapter = new Mqtt5MessageDrivenChannelAdapter(mqtt5ClientBuilder, TOPIC);
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
			subscribedLatch.countDown();
		}

	}

}
