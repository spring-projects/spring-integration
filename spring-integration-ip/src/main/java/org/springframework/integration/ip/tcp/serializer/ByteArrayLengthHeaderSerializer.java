/*
 * Copyright 2002-2010 the original author or authors.
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
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Reads data in an InputStream to a byte[]; data must be preceded by
 * a 4 byte binary length (network byte order, 
 * not included in resulting byte[]). 
 * Writes a byte[] to an OutputStream after a 4 byte binary length.
 * The length field contains the length of data following the length
 * field.
 * (network byte order).
 * 
 * @author Gary Russell
 * @since 2.0
 */
public class ByteArrayLengthHeaderSerializer extends AbstractByteArraySerializer {

	private Log logger = LogFactory.getLog(this.getClass());
	
	/**
	 * Reads a 4 byte length from the stream and then reads that length
	 * from the stream and returns the data in a byte[]. Throws an
	 * IOException if the length field exceeds the maxMessageSize.
	 * Throws a {@link SoftEndOfStreamException} if the stream
	 * is closed between messages.  
	 */
	public byte[] deserialize(InputStream inputStream) throws IOException {
		byte[] lengthPart = new byte[4];
		int status = read(inputStream, lengthPart, true);
		if (status < 0) {
			throw new SoftEndOfStreamException("Stream closed between payloads");
		}
		int messageLength = ByteBuffer.wrap(lengthPart).getInt();
		if (logger.isDebugEnabled()) {
			logger.debug("Message length is " + messageLength);
		}	
		if (messageLength > this.maxMessageSize) {
			throw new IOException("Message length " + messageLength + 
					" exceeds max message length: " + this.maxMessageSize);
		}
		byte[] messagePart = new byte[messageLength];
		read(inputStream, messagePart, false);
		return messagePart;
	}

	/**
	 * Writes the byte[] to the output stream, preceded by a 4 byte
	 * length in network byte order (big endian).
	 */
	public void serialize(byte[] bytes, OutputStream outputStream) throws IOException {
		ByteBuffer lengthPart = ByteBuffer.allocate(4);
		lengthPart.putInt(bytes.length);
		outputStream.write(lengthPart.array());
		outputStream.write(bytes);
		outputStream.flush();
	}

	/**
	 * Reads data from the socket and puts the data in buffer. Blocks until
	 * buffer is full or a socket timeout occurs.
	 * @param buffer
	 * @param header true if we are reading the header
	 * @return < 0 if socket closed and not in the middle of a message
	 * @throws IOException
	 */
	protected int read(InputStream inputStream, byte[] buffer, boolean header) 
			throws IOException {
		int lengthRead = 0;
		int needed = buffer.length;
		while (lengthRead < needed) {
			int len;
			len = inputStream.read(buffer, lengthRead,
					needed - lengthRead);
			if (len < 0 && header && lengthRead == 0) {
				return len;
			}
			if (len < 0) {
				throw new IOException("Stream closed after " + lengthRead + " of " + needed);
			}
			lengthRead += len;
			if (logger.isDebugEnabled()) {
				logger.debug("Read " + len + " bytes, buffer is now at " + 
							 lengthRead + " of " +
							 needed);
			}
		}
		return 0;
	}

}
