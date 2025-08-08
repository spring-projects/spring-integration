/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.zeromq;

/**
 * The message headers constants to repsent ZeroMq message attributes.
 *
 * @author Artem Bilan
 *
 * @since 5.4
 */
public final class ZeroMqHeaders {

	public static final String PREFIX = "zeromq_";

	/**
	 * A ZeroMq pub/sub message topic header.
	 */
	public static final String TOPIC = PREFIX + "topic";

	private ZeroMqHeaders() {
	}

}
