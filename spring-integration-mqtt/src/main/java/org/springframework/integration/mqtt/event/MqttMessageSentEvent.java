/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.mqtt.event;

import org.springframework.messaging.Message;

/**
 * An event emitted (when using aysnc) when the client indicates that a message
 * has been sent.
 *
 * @author Gary Russell
 * @since 4.1
 *
 */
@SuppressWarnings("serial")
public class MqttMessageSentEvent extends MqttMessageDeliveryEvent {

	private final Message<?> message;

	private final String topic;

	public MqttMessageSentEvent(Object source, Message<?> message, String topic, int messageId,
			String clientId, int clientInstance) {
		super(source, messageId, clientId, clientInstance);
		this.message = message;
		this.topic = topic;
	}

	public Message<?> getMessage() {
		return message;
	}

	public String getTopic() {
		return topic;
	}

	@Override
	public String toString() {
		return "MqttMessageSentEvent [message=" + message
				+ ", topic=" + topic
				+ ", clientId=" + getClientId()
				+ ", clientInstance=" + getClientInstance()
				+ ", messageId=" + getMessageId()
				+ "]";
	}

}
