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

import java.util.concurrent.CompletableFuture;

import org.jspecify.annotations.Nullable;

/**
 * An abstraction for coordinating MQTT client connections and disconnections, ensures thread-safe operations.
 *
 * @param <T> MQTT Client type
 * @param <C> MQTT Connect Message type
 * @param <D> MQTT Disconnect Message type
 *
 * @author Jiandong Ma
 *
 * @since 7.2
 */
public interface MqttClientConnectionCoordinator<T, C, D> {

	/**
	 * Connect to MQTT broker using the mqttClient and mqttConnect.
	 * @param mqttClient the mqttClient
	 * @param mqttConnect the mqttConnect
	 * @return CompletableFuture
	 */
	CompletableFuture<?> connect(T mqttClient, C mqttConnect);

	/**
	 * Disconnect from MQTT broker using the mqttClient and mqttDisconnect.
	 * @param mqttClient the mqttClient
	 * @param mqttDisconnect the mqttDisconnect, for MQTT v3, it is always null.
	 * @return CompletableFuture
	 */
	CompletableFuture<Void> disconnect(T mqttClient, @Nullable D mqttDisconnect);

}
