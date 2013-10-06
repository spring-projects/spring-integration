/*
 * Copyright 2002-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.scripting.jsr223;

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.scripting.ScriptExecutor;
import org.springframework.integration.scripting.ScriptingException;
import org.springframework.scripting.ScriptSource;
import org.springframework.util.Assert;

/**
 * Base Class for {@link ScriptExecutor}
 *
 * @author David Turanski
 * @author Mark Fisher
 * @since 2.1
 */
abstract class AbstractScriptExecutor implements ScriptExecutor {

	protected final Log logger = LogFactory.getLog(this.getClass());

	protected final ScriptEngine scriptEngine;

	protected final String language;

	public AbstractScriptExecutor(String language) {
		Assert.hasText(language, "language must not be empty");
		this.language = language;

		scriptEngine = new ScriptEngineManager().getEngineByName(this.language);

		if (logger.isDebugEnabled()) {

			if (scriptEngine == null) {
				logger.error(invlalidLanguageMessage(this.language));
			}
			else {
				logger.debug("using script engine : " + scriptEngine.getFactory().getEngineName());
			}
		}
	}

	public Object executeScript(ScriptSource scriptSource) {
		return this.executeScript(scriptSource, null);
	}

	public Object executeScript(ScriptSource scriptSource, Map<String, Object> variables) {
		Object result = null;

		try {
			if (variables != null) {
				for (Entry<String, Object> entry : variables.entrySet()) {
					scriptEngine.put(entry.getKey(), entry.getValue());
				}
			}
			scriptSource.isModified();
			String script = scriptSource.getScriptAsString();
			Date start = new Date();
			if (logger.isDebugEnabled()) {
				logger.debug("executing script: " + script);
			}

			result = scriptEngine.eval(script);

			result = postProcess(result, scriptEngine, script);

			if (logger.isDebugEnabled()) {
				logger.debug("script executed in " + (new Date().getTime() - start.getTime()) + " ms");
			}
		}

		catch (Exception e) {
			throw new ScriptingException(e.getMessage(), e);
		}

		return result;
	}

	/**
	 * Subclasses may implement this to provide any special handling required
	 * @param result
	 * @param scriptEngine
	 * @param script
	 * @return modified result
	 */
	protected abstract Object postProcess(Object result, ScriptEngine scriptEngine, String script);

	private static String invlalidLanguageMessage(String language) {
		return new StringBuilder().append(ScriptEngineManager.class.getName())
				.append(" is unable to create a script engine for language '").append(language).append("'.\n")
				.append("This may be due to a missing language implementation or an invalid language name.").toString();
	}

}
