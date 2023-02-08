/*
 * Copyright 2023 the original author or authors.
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

package org.springframework.integration.ip.dsl;

import org.springframework.integration.ip.tcp.connection.TcpNetConnectionSupport;
import org.springframework.integration.ip.tcp.connection.TcpNetServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpSocketFactorySupport;

/**
 * {@link TcpServerConnectionFactorySpec} for {@link TcpNetServerConnectionFactory}s.
 *
 * @author Gary Russell
 * @since 6.0.3
 */
public class TcpNetServerConnectionFactorySpec
		extends TcpServerConnectionFactorySpec<TcpNetServerConnectionFactorySpec, TcpNetServerConnectionFactory> {

	protected TcpNetServerConnectionFactorySpec(int port) {
		super(new TcpNetServerConnectionFactory(port));
	}

	/**
	 * The {@link TcpNetConnectionSupport} to use to create connection objects.
	 * @param connectionSupport the {@link TcpNetConnectionSupport}.
	 * @return the spec.
	 * @see TcpNetServerConnectionFactory#setTcpNetConnectionSupport(TcpNetConnectionSupport)
	 */
	public TcpNetServerConnectionFactorySpec connectionSupport(TcpNetConnectionSupport connectionSupport) {
		this.target.setTcpNetConnectionSupport(connectionSupport);
		return this;
	}

	/**
	 * Set the {@link TcpSocketFactorySupport} used to create server sockets.
	 * @param tcpSocketFactorySupport the {@link TcpSocketFactorySupport}
	 * @return the spec.
	 * @see TcpNetServerConnectionFactory#setTcpSocketFactorySupport(TcpSocketFactorySupport)
	 */
	public TcpNetServerConnectionFactorySpec socketFactorySupport(TcpSocketFactorySupport tcpSocketFactorySupport) {
		this.target.setTcpSocketFactorySupport(tcpSocketFactorySupport);
		return this;
	}

}
