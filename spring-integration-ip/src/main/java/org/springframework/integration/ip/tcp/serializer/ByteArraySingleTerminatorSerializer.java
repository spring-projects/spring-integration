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

package org.springframework.integration.ip.tcp.serializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Reads data in an InputStream to a byte[]; data must be terminated by a single
 * byte (not included in resulting byte[]).
 * Writes a byte[] to an OutputStream and adds the terminator.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.2
 */
public class ByteArraySingleTerminatorSerializer extends AbstractPooledBufferByteArraySerializer {

	private final byte terminator;

	public ByteArraySingleTerminatorSerializer(byte delimiter) {
		this.terminator = delimiter;
	}

	/**
	 * Reads the data in the inputStream to a byte[]. Data must be terminated
	 * by a single byte. Throws a {@link SoftEndOfStreamException} if the stream
	 * is closed immediately after the terminator (i.e. no data is in the process of
	 * being read).
	 */
	@Override
	protected byte[] doDeserialize(InputStream inputStream, byte[] buffer) throws IOException {
		int n = 0;
		int bite;
		int available = inputStream.available();
		logger.debug(() -> "Available to read: " + available);
		try {
			while (true) {
				bite = inputStream.read();
				if (bite < 0 && n == 0) {
					throw new SoftEndOfStreamException("Stream closed between payloads");
				}
				checkClosure(bite);
				if (bite == this.terminator) {
					break;
				}
				buffer[n++] = (byte) bite;
				int maxMessageSize = getMaxMessageSize();
				if (n >= maxMessageSize) {
					throw new IOException("Terminator '0x" + Integer.toHexString(this.terminator & 0xff)
							+ "' not found before max message length: "
							+ maxMessageSize);
				}
			}
			return copyToSizedArray(buffer, n);
		}
		catch (SoftEndOfStreamException e) { // NOSONAR catch and throw
			throw e; // it's an IO exception and we don't want an event for this
		}
		catch (IOException | RuntimeException ex) {
			publishEvent(ex, buffer, n);
			throw ex;
		}
	}

	/**
	 * Writes the byte[] to the stream and appends the terminator.
	 */
	@Override
	public void serialize(byte[] bytes, OutputStream outputStream) throws IOException {
		outputStream.write(bytes);
		outputStream.write(this.terminator);
	}

}
