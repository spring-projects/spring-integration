/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.mqtt.support;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
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
	 * The default implementation calls {@link #toMessageBuilder(String, MqttMessage)}.
	 * @param topic the topic.
	 * @param mqttMessage the MQTT message.
	 * @return the Message.
	 */
	default Message<?> toMessage(String topic, MqttMessage mqttMessage) {
		AbstractIntegrationMessageBuilder<?> builder = toMessageBuilder(topic, mqttMessage);
		if (builder != null) {
			return builder.build();
		}
		else {
			return null;
		}
	}

	/**
	 * Convert to a message builder.
	 * @param topic the topic.
	 * @param mqttMessage the MQTT message.
	 * @return the builder.
	 */
	AbstractIntegrationMessageBuilder<?> toMessageBuilder(String topic, MqttMessage mqttMessage);

	static MessageProcessor<Integer> defaultQosProcessor() {
		return message -> message.getHeaders().get(MqttHeaders.QOS, Integer.class);
	}

	static MessageProcessor<Boolean> defaultRetainedProcessor() {
		return message -> message.getHeaders().get(MqttHeaders.RETAINED, Boolean.class);
	}

}
