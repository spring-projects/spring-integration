/*
 * Copyright © 2018 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2018-present the original author or authors.
 */

package org.springframework.integration.util;

/**
 * Thrown when a pooled item could not be obtained for some reason.
 *
 * @author Gary Russell
 * @since 5.1
 *
 */
@SuppressWarnings("serial")
public class PoolItemNotAvailableException extends RuntimeException {

	public PoolItemNotAvailableException(String message, Throwable cause) {
		super(message, cause);
	}

	public PoolItemNotAvailableException(String message) {
		super(message);
	}

}
