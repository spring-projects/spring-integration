/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.pulsar.support;

import org.springframework.pulsar.support.PulsarHeaders;

/**
 * The headers that Spring Integration adds to the messages from Apache Pulsar, in
 * addition to the {@link PulsarHeaders} of Spring for Apache Pulsar.
 *
 * @author Sharang Gupta
 *
 * @since 7.2
 */
public final class PulsarIntegrationHeaders {

	/**
	 * The {@link org.springframework.pulsar.listener.Acknowledgement} of the message,
	 * which is present when the container uses the
	 * {@link org.springframework.pulsar.listener.AckMode#MANUAL} acknowledgement mode.
	 */
	public static final String ACKNOWLEDGMENT = PulsarHeaders.PREFIX + "acknowledgment";

	private PulsarIntegrationHeaders() {
	}

}
