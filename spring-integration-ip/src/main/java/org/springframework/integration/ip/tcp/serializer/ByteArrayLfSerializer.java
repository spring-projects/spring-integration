/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ip.tcp.serializer;

/**
 * The {@link ByteArraySingleTerminatorSerializer} extension for the {@code LF}
 * message delimiter.
 *
 * @author Gary Russell
 *
 * @since 2.2
 *
 */
public class ByteArrayLfSerializer extends ByteArraySingleTerminatorSerializer {

	/**
	 * A single reusable instance.
	 */
	public static final ByteArrayLfSerializer INSTANCE = new ByteArrayLfSerializer();

	public ByteArrayLfSerializer() {
		super((byte) '\n');
	}

}
