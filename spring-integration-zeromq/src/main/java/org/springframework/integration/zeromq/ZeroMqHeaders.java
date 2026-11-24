/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.zeromq;

/**
 * The message headers constants to represent ZeroMq message attributes.
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
