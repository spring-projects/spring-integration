/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.ip.tcp.serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.serializer.Deserializer;
import org.springframework.util.StreamUtils;

/**
 * A deserializer that uses a {@link ByteArrayOutputStream} instead of a fixed buffer,
 * allowing the buffer to grow as needed. Completion is indicated by the sender closing
 * the socket.
 *
 * @author Gary Russell
 * @since 5.0
 *
 */
public class ByteArrayElasticRawDeserializer implements Deserializer<byte[]> {

	private static final int DEFAULT_INITIAL_SIZE = 32;

	private final int initialBufferSize;

	/**
	 * Construct an instance that uses {@link ByteArrayOutputStream}s with an initial
	 * buffer size of 32.
	 */
	public ByteArrayElasticRawDeserializer() {
		this(DEFAULT_INITIAL_SIZE);
	}

	/**
	 * Construct an instance that uses {@link ByteArrayOutputStream}s with the provided
	 * initial buffer size.
	 * @param initialBufferSize the initial buffer size.
	 */
	public ByteArrayElasticRawDeserializer(int initialBufferSize) {
		this.initialBufferSize = initialBufferSize;
	}

	@Override
	public byte[] deserialize(InputStream inputStream) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream(this.initialBufferSize);
		if (StreamUtils.copy(inputStream, out) == 0) {
			throw new SoftEndOfStreamException("Stream closed with no data");
		}
		out.close();
		return out.toByteArray();
	}

}
