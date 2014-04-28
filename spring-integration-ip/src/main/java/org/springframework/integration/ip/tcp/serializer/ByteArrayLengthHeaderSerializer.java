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
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Reads data in an InputStream to a byte[]; data must be preceded by
 * a binary length (network byte order, not included in resulting byte[]).
 *
 * Writes a byte[] to an OutputStream after a binary length.
 * The length field contains the length of data following the length
 * field. (network byte order).
 *
 * The default length field is a 4 byte signed integer. During deserialization,
 * negative values will be rejected.
 * Other options are an unsigned byte, and unsigned short.
 *
 * For other header formats, override {@link #readHeader(InputStream)} and
 * {@link #writeHeader(OutputStream, int)}.
 *
 * @author Gary Russell
 * @since 2.0
 */
public class ByteArrayLengthHeaderSerializer extends AbstractByteArraySerializer {


	/**
	 * Default length-header field, allows for data up to 2**31-1 bytes.
	 */
	public static final int HEADER_SIZE_INT = 4; // default

	/**
	 * A single unsigned byte, for data up to 255 bytes.
	 */
	public static final int HEADER_SIZE_UNSIGNED_BYTE = 1;

	/**
	 * An unsigned short, for data up to 2**16 bytes.
	 */
	public static final int HEADER_SIZE_UNSIGNED_SHORT = 2;

	private final int headerSize;

	private final Log logger = LogFactory.getLog(this.getClass());

	/**
	 * Constructs the serializer using {@link #HEADER_SIZE_INT}
	 */
	public ByteArrayLengthHeaderSerializer() {
		this(HEADER_SIZE_INT);
	}

	/**
	 * Constructs the serializer using the supplied header size.
	 * Valid header sizes are {@link #HEADER_SIZE_INT} (default),
	 * {@link #HEADER_SIZE_UNSIGNED_BYTE} and {@link #HEADER_SIZE_UNSIGNED_SHORT}
	 * @param headerSize The header size.
	 */
	public ByteArrayLengthHeaderSerializer(int headerSize) {
		if (headerSize != HEADER_SIZE_INT &&
			headerSize != HEADER_SIZE_UNSIGNED_BYTE &&
			headerSize != HEADER_SIZE_UNSIGNED_SHORT) {
			throw new IllegalArgumentException("Illegal header size:" + headerSize);
		}
		this.headerSize = headerSize;
	}

	/**
	 * Reads the header from the stream and then reads the provided length
	 * from the stream and returns the data in a byte[]. Throws an
	 * IOException if the length field exceeds the maxMessageSize.
	 * Throws a {@link SoftEndOfStreamException} if the stream
	 * is closed between messages.
	 *
	 * @param inputStream The input stream.
	 * @throws IOException Any IOException.
	 */
	@Override
	public byte[] deserialize(InputStream inputStream) throws IOException {
		int messageLength = this.readHeader(inputStream);
		if (logger.isDebugEnabled()) {
			logger.debug("Message length is " + messageLength);
		}
		byte[] messagePart = null;
		try {
			if (messageLength > this.maxMessageSize) {
				throw new IOException("Message length " + messageLength +
						" exceeds max message length: " + this.maxMessageSize);
			}
			messagePart = new byte[messageLength];
			read(inputStream, messagePart, false);
			return messagePart;
		}
		catch (IOException e) {
			publishEvent(e, messagePart, -1);
			throw e;
		}
		catch (RuntimeException e) {
			publishEvent(e, messagePart, -1);
			throw e;
		}
	}

	/**
	 * Writes the byte[] to the output stream, preceded by a 4 byte
	 * length in network byte order (big endian).
	 *
	 * @param bytes The bytes.
	 * @param outputStream The output stream.
	 */
	@Override
	public void serialize(byte[] bytes, OutputStream outputStream) throws IOException {
		this.writeHeader(outputStream, bytes.length);
		outputStream.write(bytes);
		outputStream.flush();
	}

	/**
	 * Reads data from the socket and puts the data in buffer. Blocks until
	 * buffer is full or a socket timeout occurs.
	 *
	 * @param inputStream The input stream.
	 * @param buffer the buffer into which the data should be read
	 * @param header true if we are reading the header
	 * @return {@code < 0} if socket closed and not in the middle of a message
	 * @throws IOException Any IOException.
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

	/**
	 * Writes the header, according to the header format.
	 * @param outputStream The output stream.
	 * @param length The length.
	 * @throws IOException Any IOException.
	 */
	protected void writeHeader(OutputStream outputStream, int length) throws IOException {
		ByteBuffer lengthPart = ByteBuffer.allocate(this.headerSize);
		switch (this.headerSize) {
		case HEADER_SIZE_INT:
			lengthPart.putInt(length);
			break;
		case HEADER_SIZE_UNSIGNED_BYTE:
			if (length > 0xff) {
				throw new IllegalArgumentException("Length header:"
						+ headerSize
						+ " too short to accommodate message length:" + length);
			}
			lengthPart.put((byte) length);
			break;
		case HEADER_SIZE_UNSIGNED_SHORT:
			if (length > 0xffff) {
				throw new IllegalArgumentException("Length header:"
						+ headerSize
						+ " too short to accommodate message length:" + length);
			}
			lengthPart.putShort((short) length);
			break;
		default:
			throw new IllegalArgumentException("Bad header size:" + headerSize);
		}
		outputStream.write(lengthPart.array());
	}

	/**
	 * Reads the header and returns the length of the data part.
	 *
	 * @param inputStream The input stream.
	 * @return The length of the data part.
	 * @throws IOException Any IOException.
	 * @throws SoftEndOfStreamException if socket closes
	 * before any length data read.
	 */
	protected int readHeader(InputStream inputStream) throws IOException {
		byte[] lengthPart = new byte[this.headerSize];
		try {
			int status = read(inputStream, lengthPart, true);
			if (status < 0) {
				throw new SoftEndOfStreamException("Stream closed between payloads");
			}
			int messageLength;
			switch (this.headerSize) {
			case HEADER_SIZE_INT:
				messageLength = ByteBuffer.wrap(lengthPart).getInt();
				if (messageLength < 0) {
					throw new IllegalArgumentException("Length header:"
							+ messageLength
							+ " is negative");
				}
				break;
			case HEADER_SIZE_UNSIGNED_BYTE:
				messageLength = ByteBuffer.wrap(lengthPart).get() & 0xff;
				break;
			case HEADER_SIZE_UNSIGNED_SHORT:
				messageLength = ByteBuffer.wrap(lengthPart).getShort() & 0xffff;
				break;
			default:
				throw new IllegalArgumentException("Bad header size:" + headerSize);
			}
			return messageLength;
		}
		catch (SoftEndOfStreamException e) {
			throw e;
		}
		catch (IOException e) {
			publishEvent(e, lengthPart, -1);
			throw e;
		}
		catch (RuntimeException e) {
			publishEvent(e, lengthPart, -1);
			throw e;
		}
	}
}
