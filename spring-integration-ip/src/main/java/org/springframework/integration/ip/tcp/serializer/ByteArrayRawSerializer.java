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

package org.springframework.integration.ip.tcp.serializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;

/**
 * A byte array (de)serializer that does nothing with the payload; sends it raw.
 * Message termination for assembly purposes is signaled by the client closing the
 * connection. The serializer does not, itself, close the connection after
 * writing the bytes.
 * <p>
 * Because the socket must be closed to indicate message end, this (de)serializer
 * can only be used by uni-directional (non-collaborating) channel adapters, and
 * not by gateways.
 * <p>
 * Prior to 4.2.2, when using NIO, a timeout caused whatever had been partially
 * received to be emitted as a message.
 * <p>
 * Now, a {@link SocketTimeoutException} is thrown. To revert to the previous
 * behavior, set the {@code treatTimeoutAsEndOfMessage} constructor argument to true.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0.3
 *
 */
public class ByteArrayRawSerializer extends AbstractPooledBufferByteArraySerializer {

	/**
	 * A single reusable instance that does not treat timeouts as end of message.
	 */
	public static final ByteArrayRawSerializer INSTANCE = new ByteArrayRawSerializer();

	private final boolean treatTimeoutAsEndOfMessage;

	public ByteArrayRawSerializer() {
		this(false);
	}

	/**
	 * Treat socket timeouts as a normal EOF and emit the (possibly partial)
	 * message.
	 * @param treatTimeoutAsEndOfMessage true to emit a message after a timeout.
	 * @since 4.2.2
	 */
	public ByteArrayRawSerializer(boolean treatTimeoutAsEndOfMessage) {
		this.treatTimeoutAsEndOfMessage = treatTimeoutAsEndOfMessage;
	}

	@Override
	public void serialize(byte[] bytes, OutputStream outputStream) throws IOException {
		outputStream.write(bytes);
	}

	@Override
	protected byte[] doDeserialize(InputStream inputStream, byte[] buffer) throws IOException {
		int n = 0;
		int bite;
		int available = inputStream.available();
		logger.debug(() -> "Available to read: " + available);
		try {
			while (true) {
				try {
					bite = inputStream.read();
				}
				catch (SocketTimeoutException e) {
					if (!this.treatTimeoutAsEndOfMessage) {
						throw e;
					}
					bite = -1;
				}
				if (bite < 0) {
					if (n == 0) {
						throw new SoftEndOfStreamException("Stream closed between payloads");
					}
					break;
				}
				int maxMessageSize = getMaxMessageSize();
				if (n >= maxMessageSize) {
					throw new IOException("Socket was not closed before max message length: " + maxMessageSize);
				}
				buffer[n++] = (byte) bite;
			}
			return copyToSizedArray(buffer, n);
		}
		catch (SoftEndOfStreamException e) { // NOSONAR catch and throw
			throw e; // it's an IO exception and we don't want an event for this
		}
		catch (IOException | RuntimeException e) {
			publishEvent(e, buffer, n);
			throw e;
		}
	}

}
