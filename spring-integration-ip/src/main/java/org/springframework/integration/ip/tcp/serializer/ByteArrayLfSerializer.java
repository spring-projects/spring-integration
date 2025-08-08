/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ip.tcp.serializer;

/**
 * @author Gary Russell
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
