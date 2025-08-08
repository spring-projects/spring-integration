/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ip.tcp.connection;

import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.channels.SocketChannel;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;

/**
 * Implementation of {@link TcpNioConnectionSupport} for non-SSL
 * NIO connections.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.2
 *
 */
public class DefaultTcpNioConnectionSupport extends AbstractTcpConnectionSupport implements TcpNioConnectionSupport {

	@Override
	public TcpNioConnection createNewConnection(SocketChannel socketChannel, boolean server, boolean lookupHost,
			@Nullable ApplicationEventPublisher applicationEventPublisher, String connectionFactoryName) {

		if (isPushbackCapable()) {
			return new PushBackTcpNioConnection(socketChannel, server, lookupHost, applicationEventPublisher,
					connectionFactoryName, getPushbackBufferSize());
		}
		else {
			return new TcpNioConnection(socketChannel, server, lookupHost, applicationEventPublisher,
					connectionFactoryName);
		}
	}

	private static final class PushBackTcpNioConnection extends TcpNioConnection {

		private final int pushbackBufferSize;

		private final String connectionId;

		private volatile PushbackInputStream pushbackStream;

		private volatile InputStream wrapped;

		PushBackTcpNioConnection(SocketChannel socketChannel, boolean server, boolean lookupHost,
				@Nullable ApplicationEventPublisher applicationEventPublisher, @Nullable String connectionFactoryName,
				int bufferSize) {

			super(socketChannel, server, lookupHost, applicationEventPublisher, connectionFactoryName);
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
