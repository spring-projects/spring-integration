/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.integration.ip.tcp.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.net.Socket;

import org.springframework.context.ApplicationEventPublisher;


/**
 * Default implementation of {@link TcpNetConnectionSupport}.
 * @author Gary Russell
 * @since 5.0
 *
 */
public class DefaultTcpNetConnectionSupport extends AbstractTcpConnectionSupport implements TcpNetConnectionSupport {

	@Override
	public TcpNetConnection createNewConnection(Socket socket, boolean server, boolean lookupHost,
			ApplicationEventPublisher applicationEventPublisher, String connectionFactoryName) throws Exception {
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
				ApplicationEventPublisher applicationEventPublisher, String connectionFactoryName, int bufferSize) {
			super(socket, server, lookupHost, applicationEventPublisher, connectionFactoryName);
			this.pushbackBufferSize = bufferSize;
			this.connectionId = "pushback:" + super.getConnectionId();
		}

		@Override
		protected InputStream inputStream() throws IOException {
			InputStream wrapped = super.inputStream();
			// It shouldn't be possible for the wrapped stream to change but, just in case...
			if (this.pushbackStream == null || wrapped != this.wrapped) {
				this.pushbackStream = new PushbackInputStream(wrapped, this.pushbackBufferSize);
				this.wrapped = wrapped;
			}
			return this.pushbackStream;
		}

		@Override
		public String getConnectionId() {
			return this.connectionId;
		}

	}

}
