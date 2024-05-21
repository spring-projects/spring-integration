/*
 * Copyright 2024 the original author or authors.
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

package org.springframework.integration.mqtt.aot;

import java.util.stream.Stream;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link RuntimeHintsRegistrar} for Spring Integration MQTT module.
 *
 * @author Artem Bilan
 *
 * @since 6.1.9
 */
class MqttRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		ReflectionHints reflectionHints = hints.reflection();
		// TODO until the real fix in Paho library.
		Stream.of("org.eclipse.paho.client.mqttv3.MqttAsyncClient", "org.eclipse.paho.mqttv5.client.MqttAsyncClient")
				.filter((typeName) -> ClassUtils.isPresent(typeName, classLoader))
				.map((typeName) -> loadClassByName(typeName, classLoader))
				.flatMap((type) -> Stream.ofNullable(ReflectionUtils.findMethod(type, "stopReconnectCycle")))
				.forEach(method -> reflectionHints.registerMethod(method, ExecutableMode.INVOKE));
	}

	private static Class<?> loadClassByName(String typeName, ClassLoader classLoader) {
		try {
			return ClassUtils.forName(typeName, classLoader);
		}
		catch (ClassNotFoundException ex) {
			throw new IllegalArgumentException(ex);
		}
	}

}
