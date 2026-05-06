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

package com.springframework.integration.hivemq;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import com.hivemq.client.internal.mqtt.message.connect.mqtt3.Mqtt3ConnectView;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.message.connect.Mqtt5Connect;
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAck;
import com.hivemq.client.mqtt.mqtt5.message.disconnect.Mqtt5Disconnect;
import org.jspecify.annotations.Nullable;

/**
 * The {@link MqttClientConnectionCoordinators} for getting a particular {@link MqttClientConnectionCoordinator}.
 *
 * @author Jiandong Ma
 *
 * @since 7.2
 */
public final class MqttClientConnectionCoordinators {

	private static final Mqtt5AsyncClientConnectionCoordinator MQTT_5_ASYNC_CLIENT_CONNECTION_COORDINATOR
			= new Mqtt5AsyncClientConnectionCoordinator();

	private static final Mqtt3AsyncClientConnectionCoordinator MQTT_3_ASYNC_CLIENT_CONNECTION_COORDINATOR
			= new Mqtt3AsyncClientConnectionCoordinator();

	public static MqttClientConnectionCoordinator<Mqtt5AsyncClient, Mqtt5Connect, Mqtt5Disconnect> mqtt5AsyncClient() {
		return MQTT_5_ASYNC_CLIENT_CONNECTION_COORDINATOR;
	}

	public static MqttClientConnectionCoordinator<Mqtt3AsyncClient, Mqtt3ConnectView, Object> mqtt3AsyncClient() {
		return MQTT_3_ASYNC_CLIENT_CONNECTION_COORDINATOR;
	}

	private MqttClientConnectionCoordinators() {
	}

	/**
	 * A {@link MqttClientConnectionCoordinator} implementation for {@link Mqtt5AsyncClient}.
	 */
	static class Mqtt5AsyncClientConnectionCoordinator
			implements MqttClientConnectionCoordinator<Mqtt5AsyncClient, Mqtt5Connect, Mqtt5Disconnect> {

		Map<Mqtt5AsyncClient, CompletableFuture<Mqtt5ConnAck>> CONNECT_FUTURE_MAP = new ConcurrentHashMap<>();

		Map<Mqtt5AsyncClient, CompletableFuture<Void>> DISCONNECT_FUTURE_MAP = new ConcurrentHashMap<>();

		@Override
		public CompletableFuture<Mqtt5ConnAck> connect(Mqtt5AsyncClient mqttClient, Mqtt5Connect mqttConnect) {
			return this.CONNECT_FUTURE_MAP.computeIfAbsent(mqttClient, client -> {
				// Remove from disconnect map, in case dirty cache between lifecycle methods
				this.DISCONNECT_FUTURE_MAP.remove(client);

				return client.connect(mqttConnect);
			});
		}

		@Override
		public CompletableFuture<Void> disconnect(Mqtt5AsyncClient mqttClient, @Nullable Mqtt5Disconnect mqttDisconnect) {
			return this.DISCONNECT_FUTURE_MAP.computeIfAbsent(mqttClient, client -> {
				// Remove from connect map,  in case dirty cache between lifecycle methods
				this.CONNECT_FUTURE_MAP.remove(client);

				return mqttDisconnect != null ? client.disconnect(mqttDisconnect) : client.disconnect();
			});
		}

	}

	/**
	 * A {@link MqttClientConnectionCoordinator} implementation for {@link Mqtt3AsyncClient}.
	 */
	static class Mqtt3AsyncClientConnectionCoordinator
			implements MqttClientConnectionCoordinator<Mqtt3AsyncClient, Mqtt3ConnectView, Object> {

		Map<Mqtt3AsyncClient, CompletableFuture<Mqtt3ConnAck>> CONNECT_FUTURE_MAP = new ConcurrentHashMap<>();

		Map<Mqtt3AsyncClient, CompletableFuture<Void>> DISCONNECT_FUTURE_MAP = new ConcurrentHashMap<>();

		@Override
		public CompletableFuture<Mqtt3ConnAck> connect(Mqtt3AsyncClient mqttClient, Mqtt3ConnectView mqttConnect) {
			return this.CONNECT_FUTURE_MAP.computeIfAbsent(mqttClient, client -> {
				// Remove from disconnect map, in case dirty cache between lifecycle methods
				this.DISCONNECT_FUTURE_MAP.remove(client);

				return client.connect(mqttConnect);
			});
		}

		@Override
		public CompletableFuture<Void> disconnect(Mqtt3AsyncClient mqttClient, @Nullable Object mqttDisconnect) {
			return this.DISCONNECT_FUTURE_MAP.computeIfAbsent(mqttClient, client -> {
				// Remove from connect map,  in case dirty cache between lifecycle methods
				this.CONNECT_FUTURE_MAP.remove(client);

				return client.disconnect();
			});
		}

	}

}
