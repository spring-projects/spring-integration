/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.integration.mqtt.support;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

import org.springframework.beans.BeanUtils;

/**
 * MQTT Utilities
 *
 * @author Gary Russell
 * @since 5.4
 *
 */
public final class MqttUtils {

	private MqttUtils() {
	}

	/**
	 * Clone the {@link MqttConnectOptions}, except the serverUris.
	 * @param options the options to clone.
	 * @return the clone.
	 */
	public static MqttConnectOptions cloneConnectOptions(MqttConnectOptions options) {
		MqttConnectOptions options2 = new MqttConnectOptions();
		BeanUtils.copyProperties(options, options2, "password", "serverURIs");
		if (options.getPassword() != null) {
			options2.setPassword(options.getPassword());
		}
		return options2;
	}

}
