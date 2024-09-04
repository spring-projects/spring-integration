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

package org.springframework.integration.mqtt.event;

import java.io.Serial;

/**
 * An event emitted (when using aysnc) when the client indicates the message
 * was not delivered on publish operation.
 *
 * @author Artem Bilan
 *
 * @since 6.4
 *
 */
public class MqttMessageNotDeliveredEvent extends MqttMessageDeliveryEvent {

	@Serial
	private static final long serialVersionUID = 8983514811627569920L;

	private final Throwable exception;

	public MqttMessageNotDeliveredEvent(Object source, int messageId, String clientId,
			int clientInstance, Throwable exception) {

		super(source, messageId, clientId, clientInstance);
		this.exception = exception;
	}

	public Throwable getException() {
		return this.exception;
	}

}
