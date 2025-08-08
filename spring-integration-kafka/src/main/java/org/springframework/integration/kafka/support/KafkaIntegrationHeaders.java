/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.kafka.support;

import org.springframework.kafka.support.KafkaHeaders;

/**
 * Headers specifically for Spring Integration components.
 *
 * @author Gary Russell
 *
 * @since 5.4
 *
 */
public final class KafkaIntegrationHeaders {

	private KafkaIntegrationHeaders() {
	}

	/**
	 * Set to {@link Boolean#TRUE} to flush after sending.
	 */
	public static final String FLUSH = KafkaHeaders.PREFIX + "flush";

	/**
	 * Set to a token to correlate a send Future.
	 */
	public static final String FUTURE_TOKEN = KafkaHeaders.PREFIX + "futureToken";

}
