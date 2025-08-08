/*
 * Copyright © 2023 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2023-present the original author or authors.
 */

package org.springframework.integration.debezium.support;

/**
 * Pre-defined header names to be used when retrieving Debezium Change Event headers.
 *
 * @author Christian Tzolov
 *
 * @since 6.2
 */
public abstract class DebeziumHeaders {

	public static final String PREFIX = "debezium_";

	/**
	 * Debezium's header key.
	 */
	public static final String KEY = PREFIX + "key";

	/**
	 * Debezium's event destination.
	 */
	public static final String DESTINATION = PREFIX + "destination";

}
