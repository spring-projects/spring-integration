/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.scripting;

import org.jspecify.annotations.Nullable;

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

	public ScriptingException(@Nullable String description, Throwable cause) {
		super(description, cause);
	}

}
