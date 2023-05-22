/*
 * Copyright 2016-2023 the original author or authors.
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

import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;

/**
 * An {@link AbstractConnectionFactorySpec} for {@link AbstractServerConnectionFactory}s.
 *
 * @param <S> the target {@link TcpServerConnectionFactorySpec} implementation type.
 * @param <C> the target {@link AbstractServerConnectionFactory} implementation type.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0
 *
 */
public abstract class TcpServerConnectionFactorySpec
		<S extends TcpServerConnectionFactorySpec<S, C>, C extends AbstractServerConnectionFactory>
		extends AbstractConnectionFactorySpec<S, C> {

	/**
	 * Create an instance.
	 * @param cf the connection factory.
	 * @since 6.0.3
	 */
	protected TcpServerConnectionFactorySpec(C cf) {
		super(cf);
	}

	/**
	 * @param localAddress the local address.
	 * @return the spec.
	 * @see AbstractServerConnectionFactory#setLocalAddress(String)
	 */
	public S localAddress(String localAddress) {
		this.target.setLocalAddress(localAddress);
		return _this();
	}

	/**
	 * @param backlog the backlog.
	 * @return the spec.
	 * @see AbstractServerConnectionFactory#setBacklog(int)
	 */
	public S backlog(int backlog) {
		this.target.setBacklog(backlog);
		return _this();
	}

}
