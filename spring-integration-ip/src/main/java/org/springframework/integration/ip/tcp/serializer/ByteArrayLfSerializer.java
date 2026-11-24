/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
