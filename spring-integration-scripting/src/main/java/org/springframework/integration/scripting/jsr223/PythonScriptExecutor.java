/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.scripting.jsr223;

import javax.script.Bindings;
import javax.script.ScriptEngine;

/**
 * A {@link org.springframework.integration.scripting.ScriptExecutor}
 * that implements special handling required for Python to
 * emulate behavior similar to other JSR223 scripting languages.
 * <p>
 * Script evaluation using the Jython implementation results in a <code>null</code> return
 * value for normal variable expressions such as <code>x=2</code>. As a work around, it is
 * necessary to get the value of 'x' explicitly following the script evaluation. This
 * class performs simple parsing on the last line of the script to obtain the variable
 * name, if any, and return its value.
 *
 * @author David Turanski
 * @author Gary Russell
 * @since 2.1
 *
 */
public class PythonScriptExecutor extends AbstractScriptExecutor {

	public PythonScriptExecutor() {
		super("python");
	}

	@Override
	protected Object postProcess(Object result, ScriptEngine scriptEngine, String script, Bindings bindings) {
		Object newResult = result;
		if (newResult == null) {
			String returnVariableName = PythonVariableParser.parseReturnVariable(script);
			if (bindings != null) {
				newResult = bindings.get(returnVariableName);
			}
			if (newResult == null) {
				newResult = scriptEngine.get(returnVariableName);
			}
		}
		return newResult;
	}

	public static class PythonVariableParser {

		public static String parseReturnVariable(String script) {
			String[] lines = script.trim().split("\n");
			String lastLine = lines[lines.length - 1];
			String[] tokens = lastLine.split("=");
			return tokens[0].trim();
		}

	}

}
