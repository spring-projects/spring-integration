/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.zeromq.event;

import org.springframework.messaging.Message;

/**
 * An event emitted when the client indicates that a message
 * has been sent.
 *
 * @author Subhobrata Dey
 * @since 5.1
 *
 */
public class ZeromqMessageSentEvent extends ZeromqMessageDeliveryEvent {
	private static final long serialVersionUID = 1L;

	private final Message<?> message;

	public ZeromqMessageSentEvent(Object source, Message<?> message, String topic, String clientId, int clientType) {
		super(source, clientId, clientType, topic);
		this.message = message;
	}

	public Message<?> getMessage() {
		return this.message;
	}

	@Override
	public String getTopic() {
		return super.getTopic();
	}

	@Override
	public String toString() {
		return "ZeromqMessageSentEvent [message=" + this.message
				+ ", topic=" + getTopic()
				+ ", clientId=" + getClientId()
				+ ", clientType=" + getClientType()
				+ "]";
	}
}
