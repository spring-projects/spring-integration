/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
