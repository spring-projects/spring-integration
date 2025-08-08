/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
