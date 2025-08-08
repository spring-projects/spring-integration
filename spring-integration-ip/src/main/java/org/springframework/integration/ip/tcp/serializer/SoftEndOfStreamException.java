/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ip.tcp.serializer;

/**
 * Used to communicate that a stream has closed, but between logical
 * messages.
 *
 * @author Gary Russell
 *
 * @since 2.0
 *
 */
public class SoftEndOfStreamException extends RuntimeException {

	private static final long serialVersionUID = -2209857413498073058L;

	/**
	 * Default constructor.
	 */
	public SoftEndOfStreamException() {
	}

	/**
	 * Construct an instance with the message.
	 * @param message the message.
	 */
	public SoftEndOfStreamException(String message) {
		super(message);
	}

	/**
	 * Construct an instance with the message and cause.
	 * @param message the message.
	 * @param cause the cause.
	 * @since 4.3.21.
	 */
	public SoftEndOfStreamException(String message, Throwable cause) {
		super(message, cause);
	}

}
