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
