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

import org.springframework.integration.ip.tcp.connection.TcpNioConnectionSupport;
import org.springframework.integration.ip.tcp.connection.TcpNioServerConnectionFactory;

/**
 * {@link TcpServerConnectionFactorySpec} for {@link TcpNioServerConnectionFactory}s.
 *
 * @author Gary Russell
 * @since 6.0.3
 */
public class TcpNioServerConnectionFactorySpec
		extends TcpServerConnectionFactorySpec<TcpNioServerConnectionFactorySpec, TcpNioServerConnectionFactory> {

	protected TcpNioServerConnectionFactorySpec(int port) {
		super(new TcpNioServerConnectionFactory(port));
	}

	/**
	 * True to use direct buffers.
	 * @param usingDirectBuffers true for direct.
	 * @return the spec.
	 * @see TcpNioServerConnectionFactory#setUsingDirectBuffers(boolean)
	 */
	public TcpNioServerConnectionFactorySpec directBuffers(boolean usingDirectBuffers) {
		this.target.setUsingDirectBuffers(usingDirectBuffers);
		return this;
	}

	/**
	 * The {@link TcpNioConnectionSupport} to use.
	 * @param tcpNioSupport the {@link TcpNioConnectionSupport}.
	 * @return the spec.
	 * @see TcpNioServerConnectionFactory#setTcpNioConnectionSupport(TcpNioConnectionSupport)
	 */
	public TcpNioServerConnectionFactorySpec connectionSupport(TcpNioConnectionSupport tcpNioSupport) {
		this.target.setTcpNioConnectionSupport(tcpNioSupport);
		return this;
	}

}
