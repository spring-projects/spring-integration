/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.ip.tcp.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.net.Socket;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;

/**
 * Default implementation of {@link TcpNetConnectionSupport}.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0
 *
 */
public class DefaultTcpNetConnectionSupport extends AbstractTcpConnectionSupport implements TcpNetConnectionSupport {

	@Override
	public TcpNetConnection createNewConnection(Socket socket, boolean server, boolean lookupHost,
			@Nullable ApplicationEventPublisher applicationEventPublisher, String connectionFactoryName) {

		if (isPushbackCapable()) {
			return new PushBackTcpNetConnection(socket, server, lookupHost, applicationEventPublisher,
					connectionFactoryName, getPushbackBufferSize());
		}
		else {
			return new TcpNetConnection(socket, server, lookupHost, applicationEventPublisher, connectionFactoryName);
		}
	}

	private static final class PushBackTcpNetConnection extends TcpNetConnection {

		private final int pushbackBufferSize;

		private final String connectionId;

		private volatile PushbackInputStream pushbackStream;

		private volatile InputStream wrapped;

		PushBackTcpNetConnection(Socket socket, boolean server, boolean lookupHost,
				@Nullable ApplicationEventPublisher applicationEventPublisher, String connectionFactoryName,
				int bufferSize) {

			super(socket, server, lookupHost, applicationEventPublisher, connectionFactoryName);
			this.pushbackBufferSize = bufferSize;
			this.connectionId = "pushback:" + super.getConnectionId();
		}

		@Override
		protected InputStream inputStream() throws IOException {
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
