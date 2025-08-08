/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.transformer.support;

/**
 * Pre-defined names and prefixes for Apache Avro related headers.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.2
 */
public final class AvroHeaders {

	private AvroHeaders() {
	}

	/**
	 * The prefix for Apache Avro specific message headers.
	 */
	public static final String PREFIX = "avro_";

	/**
	 * The {@code SpecificRecord} type. By default it's the fully qualified
	 * SpecificRecord type but can be a key that is mapped to the actual type.
	 */
	public static final String TYPE = PREFIX + "type";

}
