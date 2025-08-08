/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.test.support;

/**
 * Validate a message payload. Create an anonymous instance or subclass this
 * to validate a response payload.
 *
 * @author David Turanski
 *
 */
public abstract class PayloadValidator<T> extends AbstractResponseValidator<T> {

	protected final boolean extractPayload() {
		return true;
	}

}
