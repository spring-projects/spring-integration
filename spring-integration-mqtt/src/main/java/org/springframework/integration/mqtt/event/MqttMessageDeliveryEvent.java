/*
 * Copyright 2014-2024 the original author or authors.
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

package org.springframework.integration.mqtt.event;

/**
 * Base class for events related to message delivery. Properties {@link #messageId},
 * {@link #clientId} and {@link #clientInstance} can be used to correlate events.
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

	public MqttMessageDeliveryEvent(Object source, int messageId, String clientId, int clientInstance) {
		super(source);
		this.messageId = messageId;
		this.clientId = clientId;
		this.clientInstance = clientInstance;
	}

	public int getMessageId() {
		return this.messageId;
	}

	public String getClientId() {
		return this.clientId;
	}

	public int getClientInstance() {
		return this.clientInstance;
	}

}
