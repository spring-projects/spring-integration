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

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;

import org.springframework.util.Assert;

/**
 * Implementation of {@link TcpSocketFactorySupport}
 * for SSL sockets {@link javax.net.ssl.SSLServerSocket} and {@link javax.net.ssl.SSLSocket}.
 *
 * @author Gary Russell
 * @since 2.2
 *
 */
public class DefaultTcpNetSSLSocketFactorySupport implements TcpSocketFactorySupport {

	private final SSLContext sslContext;

	public DefaultTcpNetSSLSocketFactorySupport(TcpSSLContextSupport sslContextSupport) {
		Assert.notNull(sslContextSupport, "TcpSSLContextSupport must not be null");
		try {
			this.sslContext = sslContextSupport.getSSLContext();
		}
		catch (GeneralSecurityException | IOException e) {
			throw new IllegalArgumentException("Invalid TcpSSLContextSupport - it failed to provide an SSLContext", e);
		}
		Assert.notNull(this.sslContext, "SSLContext retrieved from context support must not be null");
	}

	public ServerSocketFactory getServerSocketFactory() {
		return this.sslContext.getServerSocketFactory();
	}

	public SocketFactory getSocketFactory() {
		return this.sslContext.getSocketFactory();
	}

}
