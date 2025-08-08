/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.syslog.inbound;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.springframework.core.serializer.Deserializer;
import org.springframework.integration.ip.tcp.serializer.ByteArrayLfSerializer;
import org.springframework.integration.ip.tcp.serializer.SoftEndOfStreamException;
import org.springframework.integration.syslog.RFC5424SyslogParser;
import org.springframework.util.Assert;

/**
 * RFC5424/6587 Deserializer. Implemented as a {@link Deserializer} instead of a
 * transformer because we may receive a mixture of octet counting and non-transparent
 * framing - see RFC 6587.
 *
 * @author Duncan McIntyre
 * @author Gary Russell
 * @since 4.1.1
 *
 */
public class RFC6587SyslogDeserializer implements Deserializer<Map<String, ?>> {

	private final Deserializer<byte[]> delimitedDeserializer;

	private RFC5424SyslogParser parser = new RFC5424SyslogParser();

	/**
	 * Construct an instance using a {@link ByteArrayLfSerializer} for
	 * non-transparent frames.
	 */
	public RFC6587SyslogDeserializer() {
		this.delimitedDeserializer = new ByteArrayLfSerializer();
	}

	/**
	 * Construct an instance using the specified {@link Deserializer} for
	 * non-transparent frames.
	 * @param delimitedDeserializer the Deserializer.
	 */
	public RFC6587SyslogDeserializer(Deserializer<byte[]> delimitedDeserializer) {
		this.delimitedDeserializer = delimitedDeserializer;
	}

	/**
	 * @param parser the parser to set
	 */
	public void setParser(RFC5424SyslogParser parser) {
		this.parser = parser;
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
			Assert.state(octetCount > 0, "Expected length > 0");
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
			c = stream.read();
		}
		return length;
	}

	protected String getCharset() {
		return "UTF-8";
	}

}
