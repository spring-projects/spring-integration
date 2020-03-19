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

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * Helper for typed access to incoming MQTT message headers.
 *
 * @author Gary Russell
 * @since 5.3
 *
 */
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
