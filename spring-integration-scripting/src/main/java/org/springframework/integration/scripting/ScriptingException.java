/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.scripting;

import org.springframework.messaging.MessagingException;

/**
 * @author David Turanski
 * @since 2.1
 */
@SuppressWarnings("serial")

public class ScriptingException extends MessagingException {

	public ScriptingException(String description) {
		super(description);
	}

	public ScriptingException(String description, Throwable cause) {
		super(description, cause);
	}

}
