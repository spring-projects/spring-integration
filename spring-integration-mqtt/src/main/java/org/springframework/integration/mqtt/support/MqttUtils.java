/*
 * Copyright 2020-2024 the original author or authors.
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

import java.lang.reflect.Method;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

import org.springframework.beans.BeanUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * MQTT Utilities.
 *
 * @author Gary Russell
 *
 * @since 5.4
 *
 */
public final class MqttUtils {

	private static final boolean PAHO_MQTTV3_PRESENT =
			ClassUtils.isPresent("org.eclipse.paho.client.mqttv3.MqttAsyncClient", null);

	private static final boolean PAHO_MQTTV5_PRESENT =
			ClassUtils.isPresent("org.eclipse.paho.mqttv5.client.MqttAsyncClient", null);

	private static final Method V3_STOP_RECONNECT_CYCLE_METHOD;

	private static final Method V5_STOP_RECONNECT_CYCLE_METHOD;

	static {
		if (PAHO_MQTTV3_PRESENT) {
			V3_STOP_RECONNECT_CYCLE_METHOD =
					ReflectionUtils.findMethod(org.eclipse.paho.client.mqttv3.MqttAsyncClient.class,
							"stopReconnectCycle");
			ReflectionUtils.makeAccessible(V3_STOP_RECONNECT_CYCLE_METHOD);
		}
		else {
			V3_STOP_RECONNECT_CYCLE_METHOD = null;
		}

		if (PAHO_MQTTV5_PRESENT) {
			V5_STOP_RECONNECT_CYCLE_METHOD =
					ReflectionUtils.findMethod(org.eclipse.paho.mqttv5.client.MqttAsyncClient.class,
							"stopReconnectCycle");
			ReflectionUtils.makeAccessible(V5_STOP_RECONNECT_CYCLE_METHOD);
		}
		else {
			V5_STOP_RECONNECT_CYCLE_METHOD = null;
		}
	}

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

	/**
	 * Perform a {@code stopReconnectCycle()} (via reflection) method on the provided client
	 * to clean up resources on client stop.
	 * TODO until the real fix in Paho library.
	 * @param client the MQTTv3 Paho client instance.
	 * @since 6.1.9
	 */
	public static void stopClientReconnectCycle(org.eclipse.paho.client.mqttv3.IMqttAsyncClient client) {
		ReflectionUtils.invokeMethod(V3_STOP_RECONNECT_CYCLE_METHOD, client);
	}

	/**
	 * Perform a {@code stopReconnectCycle()} (via reflection) method on the provided client
	 * to clean up resources on client stop.
	 * TODO until the real fix in Paho library.
	 * @param client the MQTTv5 Paho client instance.
	 * @since 6.1.9
	 */
	public static void stopClientReconnectCycle(org.eclipse.paho.mqttv5.client.IMqttAsyncClient client) {
		ReflectionUtils.invokeMethod(V5_STOP_RECONNECT_CYCLE_METHOD, client);
	}

}
