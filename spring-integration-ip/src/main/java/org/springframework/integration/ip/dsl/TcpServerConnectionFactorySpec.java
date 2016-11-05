/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.ip.dsl;

import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNetServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNioServerConnectionFactory;

/**
 * An {@link AbstractConnectionFactorySpec} for {@link AbstractServerConnectionFactory}s.
 * @author Gary Russell
 *
 * @since 5.0
 *
 */
public class TcpServerConnectionFactorySpec
		extends AbstractConnectionFactorySpec<TcpServerConnectionFactorySpec, AbstractServerConnectionFactory> {

	TcpServerConnectionFactorySpec(int port) {
		this(port, false);
	}

	TcpServerConnectionFactorySpec(int port, boolean nio) {
		super(nio ? new TcpNioServerConnectionFactory(port) : new TcpNetServerConnectionFactory(port));
	}

	/**
	 * @param localAddress the local address.
	 * @return the spec.
	 * @see AbstractServerConnectionFactory#setLocalAddress(String)
	 */
	public TcpServerConnectionFactorySpec localAddress(String localAddress) {
		this.target.setLocalAddress(localAddress);
		return _this();
	}

	/**
	 * @param backlog the backlog.
	 * @return the spec.
	 * @see AbstractServerConnectionFactory#setBacklog(int)
	 */
	public TcpServerConnectionFactorySpec backlog(int backlog) {
		this.target.setBacklog(backlog);
		return _this();
	}

}
