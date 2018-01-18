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

/**
 * Base class for events related to message delivery. Properties {@link #clientId},
 * {@link #clientType} and {@link #topic} can be used to correlate events.
 *
 * @author Subhobrata Dey
 * @since 5.1
 *
 */
@SuppressWarnings("serial")
public abstract class ZeromqMessageDeliveryEvent extends ZeromqIntegrationEvent {

	private final String clientId;

	private final int clientType;

	private final String topic;

	public ZeromqMessageDeliveryEvent(Object source, String clientId, int clientType, String topic) {
		super(source);
		this.clientId = clientId;
		this.clientType = clientType;
		this.topic = topic;
	}

	public String getClientId() {
		return this.clientId;
	}

	public int getClientType() {
		return this.clientType;
	}

	public String getTopic() {
		return this.topic;
	}
}
