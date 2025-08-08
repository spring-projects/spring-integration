/*
 * Copyright © 2023 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2023-present the original author or authors.
 */

package org.springframework.integration.transformer.support;

/**
 * Pre-defined names and prefixes for Protocol Buffers related headers.
 *
 * @author Christian Tzolov
 *
 * @since 6.1
 */
public final class ProtoHeaders {

	private ProtoHeaders() {
	}

	/**
	 * The prefix for Protocol Buffers specific message headers.
	 */
	public static final String PREFIX = "proto_";

	/**
	 * The {@code com.google.protobuf.Message} type. By default, it's the fully qualified
	 * {@code com.google.protobuf.Message} type but can be a key that is mapped to the actual type.
	 */
	public static final String TYPE = PREFIX + "type";

}
