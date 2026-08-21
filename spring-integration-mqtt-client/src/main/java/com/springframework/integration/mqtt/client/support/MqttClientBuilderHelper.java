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

package com.springframework.integration.mqtt.client.support;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.MqttClientBuilder;
import com.hivemq.client.mqtt.MqttClientConfig;
import com.hivemq.client.mqtt.lifecycle.MqttClientAutoReconnect;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedListener;
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedListener;
import com.hivemq.client.mqtt.mqtt3.Mqtt3ClientBuilder;
import com.hivemq.client.mqtt.mqtt3.Mqtt3ClientConfig;
import com.hivemq.client.mqtt.mqtt5.Mqtt5ClientBuilder;
import com.hivemq.client.mqtt.mqtt5.Mqtt5ClientConfig;

import org.springframework.util.CollectionUtils;

/**
 * The helper class for {@link com.hivemq.client.mqtt.MqttClientBuilder}.
 *
 * @author Jiandong Ma
 *
 * @since 7.2
 */
public final class MqttClientBuilderHelper {

	/**
	 * Create a new {@link Mqtt5ClientBuilder} using the config from the {@link Mqtt5ClientBuilder}.
	 * @param mqtt5ClientBuilder the mqtt5ClientBuilder
	 * @return the {@link Mqtt5ClientBuilder}
	 */
	public static Mqtt5ClientBuilder clone(Mqtt5ClientBuilder mqtt5ClientBuilder) {
		Mqtt5ClientConfig inputConfig = mqtt5ClientBuilder.build().getConfig();

		return cloneBaseConfig(inputConfig, MqttClient.builder())
				.useMqttVersion5()
				.advancedConfig(inputConfig.getAdvancedConfig())
				.willPublish(inputConfig.getWillPublish().orElse(null))
				.simpleAuth(inputConfig.getSimpleAuth().orElse(null))
				.enhancedAuth(inputConfig.getEnhancedAuthMechanism().orElse(null));
	}

	/**
	 * Create a new {@link Mqtt3ClientBuilder} using the config from the {@link Mqtt3ClientBuilder}.
	 * @param mqtt3ClientBuilder the mqtt3ClientBuilder
	 * @return the {@link Mqtt3ClientBuilder}
	 */
	public static Mqtt3ClientBuilder clone(Mqtt3ClientBuilder mqtt3ClientBuilder) {
		Mqtt3ClientConfig inputConfig = mqtt3ClientBuilder.build().getConfig();

		return cloneBaseConfig(inputConfig, MqttClient.builder())
				.useMqttVersion3()
				.willPublish(inputConfig.getWillPublish().orElse(null))
				.simpleAuth(inputConfig.getSimpleAuth().orElse(null));
	}

	private static MqttClientBuilder cloneBaseConfig(MqttClientConfig inputConfig, MqttClientBuilder builder) {
		if (inputConfig.getClientIdentifier().isPresent()) {
			builder = builder.identifier(inputConfig.getClientIdentifier().get());
		}

		builder = builder
				.serverAddress(inputConfig.getServerAddress())
				.serverHost(inputConfig.getServerHost())
				.serverPort(inputConfig.getServerPort())
				.sslConfig(inputConfig.getSslConfig().orElse(null))
				.webSocketConfig(inputConfig.getWebSocketConfig().orElse(null))
				.transportConfig(inputConfig.getTransportConfig())
				.executorConfig(inputConfig.getExecutorConfig())
				// automaticReconnect(if any) will be auto registered in disconnectedListener from the input config.
				// so have to skip appending this in the new built disconnectedListener list
				.automaticReconnect(inputConfig.getAutomaticReconnect().orElse(null));

		if (!CollectionUtils.isEmpty(inputConfig.getConnectedListeners())) {
			for (MqttClientConnectedListener connectedListener : inputConfig.getConnectedListeners()) {
				builder = builder.addConnectedListener(connectedListener);
			}
		}
		if (!CollectionUtils.isEmpty(inputConfig.getDisconnectedListeners())) {
			for (MqttClientDisconnectedListener disconnectedListener : inputConfig.getDisconnectedListeners()) {
				if (disconnectedListener instanceof MqttClientAutoReconnect) {
					continue;
				}
				builder = builder.addDisconnectedListener(disconnectedListener);
			}
		}

		return builder;
	}

	private MqttClientBuilderHelper() {
	}

}
