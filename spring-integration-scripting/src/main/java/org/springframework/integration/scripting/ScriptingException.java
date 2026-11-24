/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
