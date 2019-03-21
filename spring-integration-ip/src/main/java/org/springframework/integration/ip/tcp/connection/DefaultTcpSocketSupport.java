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

import java.net.ServerSocket;
import java.net.Socket;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;

/**
 * Default implementation of {@link TcpSocketSupport}; makes no
 * changes to sockets.
 *
 * @author Gary Russell
 *
 * @since 2.2
 *
 */
public class DefaultTcpSocketSupport implements TcpSocketSupport {

	private final boolean sslVerifyHost;

	/**
	 * Construct an instance with host verification enabled.
	 */
	public DefaultTcpSocketSupport() {
		this(true);
	}

	/**
	 * Construct an instance with the provided sslVerifyHost.
	 * @param sslVerifyHost true to verify host during SSL handshake.
	 * @since 5.0.8.
	 */
	public DefaultTcpSocketSupport(boolean sslVerifyHost) {
		this.sslVerifyHost = sslVerifyHost;
	}

	/**
	 * No-Op.
	 */
	@Override
	public void postProcessServerSocket(ServerSocket serverSocket) {
	}

	/**
	 * Enables host verification for SSL, if so configured.
	 */
	@Override
	public void postProcessSocket(Socket socket) {
		if (this.sslVerifyHost && socket instanceof SSLSocket) {
			SSLSocket sslSocket = (SSLSocket) socket;
			SSLParameters sslParameters = sslSocket.getSSLParameters();
			// HTTPS works for any TCP connection.
			// It checks SAN (Subject Alternative Name) as well as CN.
			sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
			sslSocket.setSSLParameters(sslParameters);
		}
	}

}
