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
import java.nio.ByteBuffer;


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
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class ByteArrayLengthHeaderSerializer extends AbstractByteArraySerializer {

	/**
	 * Default length-header field, allows for data up to 2**31-1 bytes.
	 */
	public static final int HEADER_SIZE_INT = Integer.BYTES; // default

	/**
	 * An unsigned short, for data up to 2**16 bytes.
	 */
	public static final int HEADER_SIZE_UNSIGNED_SHORT = Short.BYTES;

	/**
	 * A single unsigned byte, for data up to 255 bytes.
	 */
	public static final int HEADER_SIZE_UNSIGNED_BYTE = Byte.BYTES;

	private static final int MAX_UNSIGNED_SHORT = 0xffff;

	private static final int MAX_UNSIGNED_BYTE = 0xff;

	private final int headerSize;

	private int headerAdjust;

	/**
	 * Construct the serializer using {@link #HEADER_SIZE_INT}
	 */
	public ByteArrayLengthHeaderSerializer() {
		this(HEADER_SIZE_INT);
	}

	/**
	 * Construct the serializer using the supplied header size.
	 * Valid header sizes are {@link #HEADER_SIZE_INT} (default),
	 * {@link #HEADER_SIZE_UNSIGNED_BYTE} and {@link #HEADER_SIZE_UNSIGNED_SHORT}
	 * @param headerSize The header size.
	 */
	public ByteArrayLengthHeaderSerializer(int headerSize) {
		if (headerSize != HEADER_SIZE_INT &&
				headerSize != HEADER_SIZE_UNSIGNED_BYTE &&
				headerSize != HEADER_SIZE_UNSIGNED_SHORT) {
			throw new IllegalArgumentException("Illegal header size: " + headerSize);
		}
		this.headerSize = headerSize;
	}

	/**
	 * Return true if the length header value includes its own length.
	 * @return true if the length includes the header length.
	 * @since 5.2
	 */
	protected boolean isInclusive() {
		return this.headerAdjust > 0;
	}

	/**
	 * Set to true to set the length header to include the length of the header in
	 * addition to the payload. Valid header sizes are {@link #HEADER_SIZE_INT} (default),
	 * {@link #HEADER_SIZE_UNSIGNED_BYTE} and {@link #HEADER_SIZE_UNSIGNED_SHORT} and 4, 1
	 * and 2 will be added to the payload length respectively.
	 * @param inclusive true to include the header length.
	 * @since 5.2
	 * @see #inclusive()
	 */
	public void setInclusive(boolean inclusive) {
		this.headerAdjust = inclusive ? this.headerSize : 0;
	}

	/**
	 * Include the length of the header in addition to the payload. Valid header sizes are
	 * {@link #HEADER_SIZE_INT} (default), {@link #HEADER_SIZE_UNSIGNED_BYTE} and
	 * {@link #HEADER_SIZE_UNSIGNED_SHORT} and 4, 1 and 2 will be added to the payload
	 * length respectively. Fluent API form of {@link #setInclusive(boolean)}.
	 * @return the serializer.
	 * @since 5.2
	 * @see #setInclusive(boolean)
	 */
	public ByteArrayLengthHeaderSerializer inclusive() {
		setInclusive(true);
		return this;
	}

	/**
	 * Read the header from the stream and then reads the provided length
	 * from the stream and returns the data in a byte[]. Throws an
	 * IOException if the length field exceeds the maxMessageSize.
	 * Throws a {@link SoftEndOfStreamException} if the stream
	 * is closed between messages.
	 * @param inputStream The input stream.
	 * @throws IOException Any IOException.
	 */
	@Override
	public byte[] deserialize(InputStream inputStream) throws IOException {
		int messageLength = this.readHeader(inputStream) - this.headerAdjust;
		this.logger.debug(() -> "Message length is " + messageLength);
		byte[] messagePart = null;
		try {
			int maxMessageSize = getMaxMessageSize();
			if (messageLength > maxMessageSize) {
				throw new IOException("Message length " + messageLength +
						" exceeds max message length: " + maxMessageSize);
			}
			messagePart = new byte[messageLength];
			read(inputStream, messagePart, false);
			return messagePart;
		}
		catch (IOException | RuntimeException ex) {
			publishEvent(ex, messagePart, -1);
			throw ex;
		}
	}

	/**
	 * Write the byte[] to the output stream, preceded by a 4 byte
	 * length in network byte order (big endian).
	 * @param bytes The bytes.
	 * @param outputStream The output stream.
	 */
	@Override
	public void serialize(byte[] bytes, OutputStream outputStream) throws IOException {
		this.writeHeader(outputStream, bytes.length + this.headerAdjust);
		outputStream.write(bytes);
	}

	/**
	 * Read data from the socket and puts the data in buffer. Blocks until
	 * buffer is full or a socket timeout occurs.
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
			int lengthReadToLog = lengthRead;
			this.logger.debug(() -> "Read " + len + " bytes, buffer is now at " +
					lengthReadToLog + " of " + needed);
		}
		return 0;
	}

	/**
	 * Write the header, according to the header format.
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
				if (length > MAX_UNSIGNED_BYTE) {
					throw new IllegalArgumentException("Length header: "
							+ this.headerSize
							+ " too short to accommodate message length: " + length);
				}
				lengthPart.put((byte) length);
				break;
			case HEADER_SIZE_UNSIGNED_SHORT:
				if (length > MAX_UNSIGNED_SHORT) {
					throw new IllegalArgumentException("Length header: "
							+ this.headerSize
							+ " too short to accommodate message length: " + length);
				}
				lengthPart.putShort((short) length);
				break;
			default:
				throw new IllegalArgumentException("Bad header size: " + this.headerSize);
		}
		outputStream.write(lengthPart.array());
	}

	/**
	 * Read the header and returns the length of the data part.
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
						throw new IllegalArgumentException("Length header: "
								+ messageLength
								+ " is negative");
					}
					break;
				case HEADER_SIZE_UNSIGNED_BYTE:
					messageLength = ByteBuffer.wrap(lengthPart).get() & MAX_UNSIGNED_BYTE;
					break;
				case HEADER_SIZE_UNSIGNED_SHORT:
					messageLength = ByteBuffer.wrap(lengthPart).getShort() & MAX_UNSIGNED_SHORT;
					break;
				default:
					throw new IllegalArgumentException("Bad header size: " + this.headerSize);
			}
			return messageLength;
		}
		catch (SoftEndOfStreamException e) { // NOSONAR catch and throw
			throw e; // it's an IO exception and we don't want an event for this
		}
		catch (IOException | RuntimeException ex) {
			publishEvent(ex, lengthPart, -1);
			throw ex;
		}
	}

}
