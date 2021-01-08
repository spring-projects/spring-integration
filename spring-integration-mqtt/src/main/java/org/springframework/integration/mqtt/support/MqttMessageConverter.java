/*
 * Copyright 2002-2021 the original author or authors.
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

import org.eclipse.paho.client.mqttv3.MqttMessage;

import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.MutableMessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConverter;

/**
 * Extension of {@link MessageConverter} allowing the topic to be added as
 * a header.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.0
 *
 */
public interface MqttMessageConverter extends MessageConverter {

	/**
	 * Convert to a Message.
	 * @param topic the topic.
	 * @param mqttMessage the MQTT message.
	 * @return the Message.
	 */
	Message<?> toMessage(String topic, MqttMessage mqttMessage);

	/**
	 * Convert to a message builder.
	 * This method is {@code default} with a delegation to the {@link #toMessage(String, MqttMessage)}
	 * to avoid a breaking change for migrated projects.
	 * The delegation will be swapped in the next version.
	 * @param topic the topic.
	 * @param mqttMessage the MQTT message.
	 * @return the builder.
	 */
	default AbstractIntegrationMessageBuilder<?> toMessageBuilder(String topic, MqttMessage mqttMessage) {
		Message<?> message = toMessage(topic, mqttMessage);
		if (message != null) {
			return MutableMessageBuilder.fromMessage(message);
		}
		else {
			return null;
		}
	}

	static MessageProcessor<Integer> defaultQosProcessor() {
		return message -> message.getHeaders().get(MqttHeaders.QOS, Integer.class);
	}

	static MessageProcessor<Boolean> defaultRetainedProcessor() {
		return message -> message.getHeaders().get(MqttHeaders.RETAINED, Boolean.class);
	}

}
