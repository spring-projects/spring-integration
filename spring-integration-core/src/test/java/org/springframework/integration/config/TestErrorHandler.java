/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.config;

import org.springframework.util.ErrorHandler;

/**
 * @author Mark Fisher
 */
public class TestErrorHandler implements ErrorHandler {

	private volatile Throwable lastError;

	public void handleError(Throwable t) {
		this.lastError = t;
	}

	public Throwable getLastError() {
		return this.lastError;
	}

}
