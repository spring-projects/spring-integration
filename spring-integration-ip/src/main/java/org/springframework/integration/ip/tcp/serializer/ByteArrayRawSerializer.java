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
 * A byte array (de)serializer that does nothing with the payload; sends it raw.
 * Message termination for assembly purposes is signaled by the client closing the
 * connection. The serializer does not, itself, close the connection after
 * writing the bytes.
 * <p>
 * Because the socket must be closed to indicate message end, this (de)serializer
 * can only be used by uni-directional (non-collaborating) channel adapters, and
 * not by gateways.
 *
 * @author Gary Russell
 * @since 2.0.3
 *
 */
public class ByteArrayRawSerializer extends AbstractByteArraySerializer {

	@Override
	public void serialize(byte[] bytes, OutputStream outputStream)
			throws IOException {
		outputStream.write(bytes);
		outputStream.flush();
	}

	@Override
	public byte[] deserialize(InputStream inputStream) throws IOException {
		byte[] buffer = new byte[this.maxMessageSize];
		int n = 0;
		int bite = 0;
		if (logger.isDebugEnabled()) {
			logger.debug("Available to read:" + inputStream.available());
		}
		try {
			while (bite >= 0) {
				bite = inputStream.read();
				if (bite < 0) {
					if (n == 0) {
						throw new SoftEndOfStreamException("Stream closed between payloads");
					}
					break;
				}
				buffer[n++] = (byte) bite;
				if (n >= this.maxMessageSize) {
					throw new IOException("Socket was not closed before max message length: "
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

}
