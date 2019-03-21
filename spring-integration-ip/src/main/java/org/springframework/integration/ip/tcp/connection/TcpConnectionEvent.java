/*
 * Copyright 2002-2019 the original author or authors.
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
 * ApplicationEvent representing normal operations on a {@link TcpConnection}.
 * @author Gary Russell
 * @since 3.0
 *
 */
@SuppressWarnings("serial")
public abstract class TcpConnectionEvent extends IpIntegrationEvent {

	private final String connectionFactoryName;

	public TcpConnectionEvent(TcpConnection connection,
			String connectionFactoryName) {
		super(connection);
		this.connectionFactoryName = connectionFactoryName;
	}

	public TcpConnectionEvent(TcpConnection connection, String connectionFactoryName,
			Throwable cause) {
		super(connection, cause);
		this.connectionFactoryName = connectionFactoryName;
	}

	public String getConnectionId() {
		return ((TcpConnection) this.getSource()).getConnectionId();
	}

	public String getConnectionFactoryName() {
		return this.connectionFactoryName;
	}

	@Override
	public String toString() {
		return super.toString() +
				", [factory=" + this.connectionFactoryName +
				", connectionId=" + this.getConnectionId() + "]";
	}

}
