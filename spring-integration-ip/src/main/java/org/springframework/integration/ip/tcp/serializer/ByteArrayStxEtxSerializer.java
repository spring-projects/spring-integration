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

import org.springframework.integration.mapping.MessageMappingException;

/**
 * Reads data in an InputStream to a byte[]; data must be prefixed by &lt;stx&gt; and
 * terminated by &lt;etx&gt; (not included in resulting byte[]).
 * Writes a byte[] to an OutputStream prefixed by &lt;stx&gt; terminated by &lt;etx&gt;
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class ByteArrayStxEtxSerializer extends AbstractPooledBufferByteArraySerializer {

	/**
	 * A single reusable instance.
	 */
	public static final ByteArrayStxEtxSerializer INSTANCE = new ByteArrayStxEtxSerializer();

	public static final int STX = 0x02;

	public static final int ETX = 0x03;

	/**
	 * Reads the data in the inputStream to a byte[]. Data must be prefixed
	 * with an ASCII STX character, and terminated with an ASCII ETX character.
	 * Throws a {@link SoftEndOfStreamException} if the stream
	 * is closed immediately before the STX (i.e. no data is in the process of
	 * being read).
	 *
	 */
	@Override
	public byte[] doDeserialize(InputStream inputStream, byte[] buffer) throws IOException {
		int bite = inputStream.read();
		if (bite < 0) {
			throw new SoftEndOfStreamException("Stream closed between payloads");
		}
		int n = 0;
		try {
			if (bite != STX) {
				throw new MessageMappingException("Expected STX to begin message");
			}
			while ((bite = inputStream.read()) != ETX) {
				checkClosure(bite);
				buffer[n++] = (byte) bite;
				int maxMessageSize = getMaxMessageSize();
				if (n >= maxMessageSize) {
					throw new IOException("ETX not found before max message length: " + maxMessageSize);
				}
			}
			return copyToSizedArray(buffer, n);
		}
		catch (IOException | RuntimeException ex) {
			publishEvent(ex, buffer, n);
			throw ex;
		}
	}

	/**
	 * Writes the byte[] to the stream, prefixed by an ASCII STX character and
	 * terminated with an ASCII ETX character.
	 */
	@Override
	public void serialize(byte[] bytes, OutputStream outputStream) throws IOException {
		outputStream.write(STX);
		outputStream.write(bytes);
		outputStream.write(ETX);
	}

}
