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
package org.springframework.integration.mqtt.outbound;

import org.springframework.integration.mqtt.core.MqttIntegrationEvent;

/**
 * Base class for events related to message delivery. Properties messageId, clientId and
 * clientInstance can be used to correlate events.
 *
 * @author Gary Russell
 * @since 4.1
 *
 */
@SuppressWarnings("serial")
public abstract class MqttMessageDeliveryEvent extends MqttIntegrationEvent {

	private final int messageId;

	private final String clientId;

	private final int clientInstance;

	public MqttMessageDeliveryEvent(MqttPahoMessageHandler source, int messageId) {
		super(source);
		this.messageId = messageId;
		this.clientId = source.getClientId();
		this.clientInstance = source.getClientInstance();
	}

	public int getMessageId() {
		return messageId;
	}

	public String getClientId() {
		return clientId;
	}

	public int getClientInstance() {
		return clientInstance;
	}

}
