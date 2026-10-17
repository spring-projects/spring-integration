/*
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.mqtt.support;

import org.jspecify.annotations.Nullable;

import org.springframework.messaging.Message;

/**
 * Helper for typed access to incoming MQTT message headers.
 *
 * @author Gary Russell
 * @author Jaehong Park
 * @since 5.3
 * @deprecated since 7.2 in favor of the {@code spring-integration-mqtt-client} module.
 */
@Deprecated(since = "7.2", forRemoval = true)
public final class MqttHeaderAccessor {

	private MqttHeaderAccessor() {
	}

	/**
	 * Return the received topic header.
	 * @param message the message.
	 * @return the header.
	 */
	@Nullable
	public static String receivedTopic(Message<?> message) {
		return message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC, String.class);
	}

	/**
	 * Return the MQTT message id.
	 * @param message the message.
	 * @return the header.
	 */
	@Nullable
	public static Integer id(Message<?> message) {
		return message.getHeaders().get(MqttHeaders.ID, Integer.class);
	}

	/**
	 * Return the received QOS header.
	 * @param message the message.
	 * @return the header.
	 */
	@Nullable
	public static Integer receivedQos(Message<?> message) {
		return message.getHeaders().get(MqttHeaders.RECEIVED_QOS, Integer.class);
	}

	/**
	 * Return the received retained header.
	 * @param message the message.
	 * @return the header.
	 */
	@Nullable
	public static Boolean receivedRetained(Message<?> message) {
		return message.getHeaders().get(MqttHeaders.RECEIVED_RETAINED, Boolean.class);
	}

	/**
	 * Return the duplicate header.
	 * @param message the message.
	 * @return the header.
	 */
	@Nullable
	public static Boolean duplicate(Message<?> message) {
		return message.getHeaders().get(MqttHeaders.DUPLICATE, Boolean.class);
	}

}
