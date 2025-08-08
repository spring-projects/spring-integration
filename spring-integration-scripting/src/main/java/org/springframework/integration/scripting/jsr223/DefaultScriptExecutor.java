/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.scripting.jsr223;

import javax.script.Bindings;
import javax.script.ScriptEngine;

/**
 * Default implementation of the {@link AbstractScriptExecutor}.
 * Accepts a scripting language for resolving a target {@code ScriptEngine} for
 * evaluation and does nothing with the {@code result} in the
 * {@link #postProcess(Object, ScriptEngine, String, Bindings)} implementation.
 *
 * @author David Turanski
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.1
 */
public class DefaultScriptExecutor extends AbstractScriptExecutor {

	/**
	 * Create a DefaultScriptExecutor for the specified language name (JSR233 alias).
	 * @param language the scripting language identificator.
	 */
	public DefaultScriptExecutor(String language) {
		super(language);
	}

	@Override
	protected Object postProcess(Object result, ScriptEngine scriptEngine, String script, Bindings bindings) {
		return result;
	}

}
