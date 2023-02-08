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

import org.springframework.integration.ip.tcp.connection.TcpNioClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNioConnectionSupport;

/**
 * {@link TcpClientConnectionFactorySpec} for {@link TcpNioClientConnectionFactory}s.
 *
 * @author Gary Russell
 * @since 6.0.3
 */
public class TcpNioClientConnectionFactorySpec
		extends TcpClientConnectionFactorySpec<TcpNioClientConnectionFactorySpec, TcpNioClientConnectionFactory> {

	protected TcpNioClientConnectionFactorySpec(String host, int port) {
		super(new TcpNioClientConnectionFactory(host, port));
	}

	/**
	 * True to use direct buffers.
	 * @param usingDirectBuffers true for direct.
	 * @return the spec.
	 * @see TcpNioClientConnectionFactory#setUsingDirectBuffers(boolean)
	 */
	public TcpNioClientConnectionFactorySpec usingDirectBuffers(boolean usingDirectBuffers) {
		this.target.setUsingDirectBuffers(usingDirectBuffers);
		return this;
	}

	/**
	 * The {@link TcpNioConnectionSupport} to use.
	 * @param tcpNioSupport the {@link TcpNioConnectionSupport}.
	 * @return the spec.
	 * @see TcpNioClientConnectionFactory#setTcpNioConnectionSupport(TcpNioConnectionSupport)
	 */
	public TcpNioClientConnectionFactorySpec tcpNioConnectionSupport(TcpNioConnectionSupport tcpNioSupport) {
		this.target.setTcpNioConnectionSupport(tcpNioSupport);
		return this;
	}

}
