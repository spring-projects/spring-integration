/*
 * Copyright 2002-2020 the original author or authors.
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
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.channels.SocketChannel;
import java.security.GeneralSecurityException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Implementation of {@link TcpNioConnectionSupport} for SSL
 * NIO connections.
 *
 * @author Gary Russell
 *
 * @since 2.2
 *
 */
public class DefaultTcpNioSSLConnectionSupport extends AbstractTcpConnectionSupport implements TcpNioConnectionSupport {

	private final SSLContext sslContext;

	private final boolean sslVerifyHost;

	/**
	 * Create an instance with host verification enabled.
	 * @param sslContextSupport the ssl context support.
	 */
	public DefaultTcpNioSSLConnectionSupport(TcpSSLContextSupport sslContextSupport) {
		this(sslContextSupport, true);
	}

	/**
	 * Create an instance.
	 * @param sslContextSupport the ssl context support.
	 * @param sslVerifyHost true to verify the host during handshake.
	 * @since 5.0.8
	 */
	public DefaultTcpNioSSLConnectionSupport(TcpSSLContextSupport sslContextSupport, boolean sslVerifyHost) {
		Assert.notNull(sslContextSupport, "TcpSSLContextSupport must not be null");
		try {
			this.sslContext = sslContextSupport.getSSLContext();
		}
		catch (GeneralSecurityException | IOException e) {
			throw new IllegalArgumentException("Invalid TcpSSLContextSupport - it failed to provide an SSLContext", e);
		}
		Assert.notNull(this.sslContext, "SSLContext retrieved from context support must not be null");
		this.sslVerifyHost = sslVerifyHost;
	}

	/**
	 * Creates a {@link TcpNioSSLConnection}.
	 */
	@Override
	public TcpNioConnection createNewConnection(SocketChannel socketChannel, boolean server, boolean lookupHost,
			@Nullable ApplicationEventPublisher applicationEventPublisher, String connectionFactoryName) {

		SSLEngine sslEngine = this.sslContext.createSSLEngine();
		postProcessSSLEngine(sslEngine);
		if (this.sslVerifyHost) {
			SSLParameters sslParameters = sslEngine.getSSLParameters();
			// HTTPS works for any TCP connection.
			// It checks SAN (Subject Alternative Name) as well as CN.
			sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
			sslEngine.setSSLParameters(sslParameters);
		}
		TcpNioSSLConnection tcpNioSSLConnection;
		if (isPushbackCapable()) {
			tcpNioSSLConnection = new PushBackTcpNioSSLConnection(socketChannel, server, lookupHost,
					applicationEventPublisher, connectionFactoryName, sslEngine, getPushbackBufferSize());
		}
		else {
			tcpNioSSLConnection = new TcpNioSSLConnection(socketChannel, server, lookupHost, applicationEventPublisher,
					connectionFactoryName, sslEngine);
		}
		tcpNioSSLConnection.init();
		return tcpNioSSLConnection;
	}

	/**
	 * Subclasses can post-process the ssl engine (set properties).
	 * @param sslEngine the engine.
	 * @since 4.3.7
	 */
	protected void postProcessSSLEngine(SSLEngine sslEngine) {
		// NOSONAR (empty)
	}

	private static final class PushBackTcpNioSSLConnection extends TcpNioSSLConnection {

		private final int pushbackBufferSize;

		private final String connectionId;

		private volatile PushbackInputStream pushbackStream;

		private volatile InputStream wrapped;

		PushBackTcpNioSSLConnection(SocketChannel socketChannel, boolean server, boolean lookupHost,
				@Nullable ApplicationEventPublisher applicationEventPublisher, String connectionFactoryName,
				SSLEngine sslEngine,
				int bufferSize) {

			super(socketChannel, server, lookupHost, applicationEventPublisher, connectionFactoryName, sslEngine);
			this.pushbackBufferSize = bufferSize;
			this.connectionId = "pushback:" + super.getConnectionId();
		}

		@Override
		protected InputStream inputStream() {
			InputStream wrappedStream = super.inputStream();
			// It shouldn't be possible for the wrapped stream to change but, just in case...
			if (this.pushbackStream == null || !wrappedStream.equals(this.wrapped)) {
				this.pushbackStream = new PushbackInputStream(wrappedStream, this.pushbackBufferSize);
				this.wrapped = wrappedStream;
			}
			return this.pushbackStream;
		}

		@Override
		public String getConnectionId() {
			return this.connectionId;
		}

	}

}
