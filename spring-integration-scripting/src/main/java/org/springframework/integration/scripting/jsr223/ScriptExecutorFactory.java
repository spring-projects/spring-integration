/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.scripting.jsr223;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.springframework.integration.scripting.PolyglotScriptExecutor;
import org.springframework.integration.scripting.ScriptExecutor;
import org.springframework.util.Assert;

/**
 * The scripting configuration utilities.
 *
 * @author David Turanski
 * @author Artem Bilan
 *
 * @since 2.1
 */
public final class ScriptExecutorFactory {

	public static ScriptExecutor getScriptExecutor(String language) {
		if (language.equalsIgnoreCase("python") || language.equalsIgnoreCase("jython")) {
			return new PythonScriptExecutor();
		}
		else if (language.equalsIgnoreCase("ruby") || language.equalsIgnoreCase("jruby")) {
			return new RubyScriptExecutor();
		}
		else if (language.equalsIgnoreCase("js") || language.equalsIgnoreCase("javascript")) {
			return new PolyglotScriptExecutor("js");
		}
		return new DefaultScriptExecutor(language);
	}

	/**
	 * Derive a scripting language from the provided script file name.
	 * @param scriptLocation the script file to consult for extension.
	 * @return the language name for the {@link ScriptExecutor}.
	 * @since 5.2
	 */
	public static String deriveLanguageFromFileExtension(String scriptLocation) {
		int index = scriptLocation.lastIndexOf('.') + 1;
		Assert.state(index > 0, () -> "Unable to determine language for script '" + scriptLocation + "'");
		String extension = scriptLocation.substring(index);
		if (extension.equals("kts")) {
			return "kotlin";
		}
		else if (extension.equals("js")) {
			return "js";
		}
		ScriptEngineManager engineManager = new ScriptEngineManager();
		ScriptEngine engine = engineManager.getEngineByExtension(extension);
		Assert.state(engine != null, () -> "No suitable scripting engine found for extension '" + extension + "'");
		return engine.getFactory().getLanguageName();
	}

	private ScriptExecutorFactory() {
	}

}
