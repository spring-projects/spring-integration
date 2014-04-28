/*
 * Copyright 2002-2014 the original author or authors.
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
 * @since 2.2
 */
public class ByteArraySingleTerminatorSerializer extends AbstractByteArraySerializer {

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
	public byte[] deserialize(InputStream inputStream) throws IOException {
		byte[] buffer = new byte[this.maxMessageSize];
		int n = 0;
		int bite;
		if (logger.isDebugEnabled()) {
			logger.debug("Available to read:" + inputStream.available());
		}
		try {
			while (true) {
				bite = inputStream.read();
				if (bite < 0 && n == 0) {
					throw new SoftEndOfStreamException("Stream closed between payloads");
				}
				checkClosure(bite);
				if (bite == terminator) {
					break;
				}
				buffer[n++] = (byte) bite;
				if (n >= this.maxMessageSize) {
					throw new IOException("Terminator '0x" + Integer.toHexString(terminator & 0xff)
							+ "' not found before max message length: "
							+ this.maxMessageSize);
				}
			}
			byte[] assembledData = new byte[n];
			System.arraycopy(buffer, 0, assembledData, 0, n);
			return assembledData;
		}
		catch (SoftEndOfStreamException e) {
			throw e;
		}
		catch (IOException e) {
			publishEvent(e, buffer, n);
			throw e;
		}
		catch (RuntimeException e) {
			publishEvent(e, buffer, n);
			throw e;
		}
	}

	/**
	 * Writes the byte[] to the stream and appends the terminator.
	 */
	@Override
	public void serialize(byte[] bytes, OutputStream outputStream) throws IOException {
		outputStream.write(bytes);
		outputStream.write(terminator);
		outputStream.flush();
	}

}
