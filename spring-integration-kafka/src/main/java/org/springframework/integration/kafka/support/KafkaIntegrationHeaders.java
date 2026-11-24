/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
