/*
 * Copyright © 2024 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2024-present the original author or authors.
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
