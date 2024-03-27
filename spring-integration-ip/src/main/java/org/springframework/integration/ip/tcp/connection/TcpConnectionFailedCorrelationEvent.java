/*
 * Copyright 2015-2024 the original author or authors.
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

package org.springframework.integration.ip.tcp.connection;

import org.springframework.integration.ip.event.IpIntegrationEvent;
import org.springframework.messaging.MessagingException;

/**
 * An event emitted when an endpoint cannot correlate a connection id to a
 * connection; the cause is a messaging exception with the failed message.
 *
 * @author Gary Russell
 * @since 4.2
 *
 */
public class TcpConnectionFailedCorrelationEvent extends IpIntegrationEvent {

	private static final long serialVersionUID = -7460880274740273542L;

	private final String connectionId;

	public TcpConnectionFailedCorrelationEvent(Object source, String connectionId, MessagingException cause) {
		super(source, cause);
		this.connectionId = connectionId;
	}

	public String getConnectionId() {
		return this.connectionId;
	}

	@Override
	public String toString() {
		return super.toString() +
				", [connectionId=" + this.connectionId + "]";
	}

}
