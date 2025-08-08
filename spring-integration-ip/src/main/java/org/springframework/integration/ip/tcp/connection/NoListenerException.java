/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ip.tcp.connection;

import org.springframework.core.NestedRuntimeException;

/**
 * @author Gary Russell
 * @since 2.0
 *
 */
public class NoListenerException extends NestedRuntimeException {

	private static final long serialVersionUID = -5644042657316429223L;

	public NoListenerException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public NoListenerException(String msg) {
		super(msg);
	}

}
