/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.scripting;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.scripting.ScriptSource;

/**
 * A script evaluation abstraction against {@link ScriptSource} and optional binding {@code variables}.
 *
 * @author David Turanski
 * @author Artem Bilan
 *
 * @since 2.1
 */
@FunctionalInterface
public interface ScriptExecutor {

	/**
	 * Execute a script from the provided {@link ScriptSource} with an optional binding {@code variables}.
	 * @param scriptSource The script source.
	 * @param variables The variables.
	 * @return The result of the execution.
	 */
	@Nullable
	Object executeScript(ScriptSource scriptSource, @Nullable Map<String, Object> variables);

	/**
	 * Execute a script from the provided {@link ScriptSource}
	 * @param scriptSource The script source.
	 * @return The result of the execution.
	 */
	default @Nullable Object executeScript(ScriptSource scriptSource) {
		return executeScript(scriptSource, null);
	}

}
