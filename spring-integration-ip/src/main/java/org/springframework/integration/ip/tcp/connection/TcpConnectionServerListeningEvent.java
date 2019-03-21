/*
 * Copyright 2015-2019 the original author or authors.
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

/**
 * {@link IpIntegrationEvent} emitted when a server begins listening. Useful
 * when the configured port is zero and the operating system chooses the port.
 * Also useful to avoid polling the {@code isListening()} if you need to wait
 * before starting some other process to connect to the socket.
 *
 * @author Gary Russell
 * @since 4.3
 */
@SuppressWarnings("serial")
public class TcpConnectionServerListeningEvent extends IpIntegrationEvent {

	private final int port;

	public TcpConnectionServerListeningEvent(TcpServerConnectionFactory connectionFactory, int port) {
		super(connectionFactory);
		this.port = port;
	}

	public int getPort() {
		return this.port;
	}

}
