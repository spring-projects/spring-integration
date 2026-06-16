/*
 * Copyright 2014-present the original author or authors.
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

package org.springframework.integration.syslog.inbound;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.springframework.core.serializer.Deserializer;
import org.springframework.integration.ip.tcp.serializer.AbstractByteArraySerializer;
import org.springframework.integration.ip.tcp.serializer.ByteArrayLfSerializer;
import org.springframework.integration.ip.tcp.serializer.SoftEndOfStreamException;
import org.springframework.integration.syslog.RFC5424SyslogParser;

/**
 * RFC5424/6587 Deserializer. Implemented as a {@link Deserializer} instead of a
 * transformer because we may receive a mixture of octet counting and non-transparent
 * framing - see RFC 6587.
 *
 * @author Duncan McIntyre
 * @author Gary Russell
 * @author Uwez Khan
 * @since 4.1.1
 *
 */
public class RFC6587SyslogDeserializer implements Deserializer<Map<String, ?>> {

	private final Deserializer<byte[]> delimitedDeserializer;

	private RFC5424SyslogParser parser = new RFC5424SyslogParser();

	private int maxMessageSize = AbstractByteArraySerializer.DEFAULT_MAX_MESSAGE_SIZE;

	/**
	 * Construct an instance using a {@link ByteArrayLfSerializer} for
	 * non-transparent frames.
	 */
	public RFC6587SyslogDeserializer() {
		this(new ByteArrayLfSerializer());
	}

	/**
	 * Construct an instance using the specified {@link Deserializer} for
	 * non-transparent frames.
	 * @param delimitedDeserializer the Deserializer.
	 */
	public RFC6587SyslogDeserializer(Deserializer<byte[]> delimitedDeserializer) {
		this.delimitedDeserializer = delimitedDeserializer;
		if (delimitedDeserializer instanceof AbstractByteArraySerializer byteArraySerializer) {
			this.maxMessageSize = byteArraySerializer.getMaxMessageSize();
		}
	}

	/**
	 * @param parser the parser to set
	 */
	public void setParser(RFC5424SyslogParser parser) {
		this.parser = parser;
	}

	/**
	 * Set the maximum frame size accepted for an octet-counted frame. A frame whose
	 * declared octet count exceeds this value is rejected with an {@link IOException}
	 * before any buffer is allocated, mirroring the {@code maxMessageSize} ceiling the
	 * non-transparent (LF-delimited) framing path already enforces. Defaults to the
	 * {@code maxMessageSize} of the delimited deserializer when it is an
	 * {@link AbstractByteArraySerializer}, otherwise
	 * {@link AbstractByteArraySerializer#DEFAULT_MAX_MESSAGE_SIZE}.
	 * @param maxMessageSize the maximum octet count to accept.
	 * @since 5.5.22
	 */
	public void setMaxMessageSize(int maxMessageSize) {
		this.maxMessageSize = maxMessageSize;
	}

	@Override
	public Map<String, ?> deserialize(InputStream inputStream) throws IOException {
		DataInputStream stream = new DataInputStream(inputStream);
		String line;
		int octetCount = 0;
		boolean shortRead = false;
		int peek = stream.read();
		if (isDigit(peek)) {
			octetCount = calculateLength(stream, peek);
			if (octetCount <= 0) {
				throw new IOException("Expected length > 0");
			}
			byte[] bytes = new byte[octetCount];
			try {
				stream.readFully(bytes);
			}
			catch (EOFException e) {
				shortRead = true;
			}
			line = new String(bytes, getCharset());
		}
		else if (peek == '<') {
			byte[] bytes = this.delimitedDeserializer.deserialize(inputStream);
			line = "<" + new String(bytes, getCharset());
		}
		else if (peek < 0) {
			throw new SoftEndOfStreamException();
		}
		else {
			throw new IllegalStateException("Expected a digit or '<', got 0x" + Integer.toHexString(peek));
		}
		return this.parser.parse(line, octetCount, shortRead);
	}

	private boolean isDigit(int peek) {
		return peek >= 0x30 && peek <= 0x39; // NOSONAR magic number
	}

	private int calculateLength(DataInputStream stream, int peek) throws IOException {
		int length = peek & 0xf; // NOSONAR magic number
		int c = stream.read();
		while (isDigit(c)) {
			length = length * 10 + (c & 0xf); // NOSONAR magic number
			if (length > this.maxMessageSize) {
				throw new IOException("Frame length (" + length + ") exceeds maxMessageSize (" + this.maxMessageSize + ")");
			}
			c = stream.read();
		}
		return length;
	}

	protected String getCharset() {
		return "UTF-8";
	}

}
