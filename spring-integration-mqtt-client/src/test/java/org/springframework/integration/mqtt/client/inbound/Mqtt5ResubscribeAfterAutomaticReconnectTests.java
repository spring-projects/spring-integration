/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.mqtt.client.inbound;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.connect.Mqtt5Connect;
import com.hivemq.client.mqtt.mqtt5.message.disconnect.Mqtt5Disconnect;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5Subscription;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.mqtt.client.MqttContainerTest;
import org.springframework.integration.mqtt.client.ToxiproxyContainerTest;
import org.springframework.integration.mqtt.client.core.Mqtt5ClientManager;
import org.springframework.integration.mqtt.client.event.MqttSubscribedEvent;
import org.springframework.integration.mqtt.client.support.MqttHeaderMapper;
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
class Mqtt5ResubscribeAfterAutomaticReconnectTests implements MqttContainerTest, ToxiproxyContainerTest {

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
		toxiproxy = proxyClient.createProxy("mqttProxy", "0.0.0.0:" + PROXY_PORT_FOR_MQTT, "mqtt-broker:" + MQTT_PORT);
		toxiproxy.enable();

		mqtt5TestClient = Mqtt5Client.builder()
				.identifier("mqtt5-reconnect-test-client")
				.serverHost(PROXY_CONTAINER.getHost())
				.serverPort(PROXY_CONTAINER.getMappedPort(PROXY_PORT_FOR_MQTT))
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
		assertThat(outputChannel.receive(10000)).isNotNull();

		// broker down and up
		toxiproxy.disable();
		assertThat(disconnectedLatch.await(30, TimeUnit.SECONDS)).isTrue();
		toxiproxy.enable();
		// await reconnect, manual resubscribe does not need.
		assertThat(connectedLatches.await(30, TimeUnit.SECONDS)).isTrue();

		// Given
		mqtt5TestClient.connect();
		mqtt5TestClient.publishWith().topic(TOPIC).payload("payload-2".getBytes()).send();
		// Then
		assertThat(outputChannel.receive(10000)).isNotNull();
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	static class Config {

		@Bean
		Mqtt5ClientManager mqtt5ClientManager() {
			var mqtt5ClientManager = new Mqtt5ClientManager(Mqtt5Client.builder()
					.serverHost(PROXY_CONTAINER.getHost())
					.serverPort(PROXY_CONTAINER.getMappedPort(PROXY_PORT_FOR_MQTT))
					.automaticReconnect()
					.initialDelay(1, TimeUnit.SECONDS)
					.maxDelay(2, TimeUnit.SECONDS)
					.applyAutomaticReconnect()
					.addConnectedListener(ctx -> connectedLatches.countDown())
					.addDisconnectedListener(ctx -> disconnectedLatch.countDown())
					.build()
					.getConfig());
			mqtt5ClientManager.setMqttConnect(Mqtt5Connect.builder()
					.cleanStart(true) // looks even cleanStart is true, resubscribe can automatic happens after reconnect.
					.build());
			mqtt5ClientManager.setMqttDisconnect(Mqtt5Disconnect.builder().build());
			return mqtt5ClientManager;
		}

		@Bean
		QueueChannel outputChannel() {
			return new QueueChannel();
		}

		@Bean
		Mqtt5MessageDrivenChannelAdapter mqtt5InboundChannelAdapter(Mqtt5ClientManager mqtt5ClientManager,
				QueueChannel outputChannel) {

			var adapter = new Mqtt5MessageDrivenChannelAdapter(mqtt5ClientManager, TOPIC);
			adapter.setOutputChannel(outputChannel);
			adapter.setQos(MqttQos.AT_LEAST_ONCE);
			// below are default, for line coverage only
			adapter.setHeaderMapper(new MqttHeaderMapper());
			adapter.setNoLocal(Mqtt5Subscription.DEFAULT_NO_LOCAL);
			adapter.setRetainHandling(Mqtt5Subscription.DEFAULT_RETAIN_HANDLING);
			adapter.setRetainAsPublished(Mqtt5Subscription.DEFAULT_RETAIN_AS_PUBLISHED);
			return adapter;
		}

		@EventListener
		void mqttEvents(MqttSubscribedEvent event) {
			subscribedLatch.countDown();
		}

	}

}
