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

import org.springframework.integration.dsl.IntegrationComponentSpec;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNetClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNioClientConnectionFactory;

/**
 * An {@link IntegrationComponentSpec} for {@link AbstractClientConnectionFactory}s.
 * @author Gary Russell
 *
 * @param <S> the target {@link TcpClientConnectionFactorySpec} implementation type.
 * @param <C> the target {@link AbstractClientConnectionFactory} implementation type.
 *
 * @since 5.0
 *
 */
public class TcpClientConnectionFactorySpec
			<S extends TcpClientConnectionFactorySpec<S, C>, C extends AbstractClientConnectionFactory>
		extends AbstractConnectionFactorySpec<S, AbstractClientConnectionFactory> {

	TcpClientConnectionFactorySpec(String host, int port) {
		this(host, port, false);
	}

	public TcpClientConnectionFactorySpec(String host, int port, boolean nio) {
		super(nio ? new TcpNioClientConnectionFactory(host, port) : new TcpNetClientConnectionFactory(host, port));
	}

}
