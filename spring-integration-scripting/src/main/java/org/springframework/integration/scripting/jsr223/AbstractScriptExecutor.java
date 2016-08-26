/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.scripting.jsr223;

import java.util.Date;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;

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
 * @author Artem Bilan
 * @author Gary Russell
 * @since 2.1
 */
public abstract class AbstractScriptExecutor implements ScriptExecutor {

	protected final Log logger = LogFactory.getLog(this.getClass());

	protected final ScriptEngine scriptEngine;

	protected final String language;

	protected AbstractScriptExecutor(String language) {
		Assert.hasText(language, "language must not be empty");
		this.language = language;

		this.scriptEngine = new ScriptEngineManager().getEngineByName(this.language);
		Assert.notNull(this.scriptEngine, invalidLanguageMessage(this.language));
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("Using script engine : " + this.scriptEngine.getFactory().getEngineName());
		}
	}

	@Override
	public Object executeScript(ScriptSource scriptSource, Map<String, Object> variables) {
		Object result;

		try {
			String script = scriptSource.getScriptAsString();
			Date start = new Date();
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("executing script: " + script);
			}

			Bindings bindings = null;
			if (variables != null && variables.size() > 0) {
				bindings = new SimpleBindings(variables);
				result = this.scriptEngine.eval(script, bindings);
			}
			else {
				result = this.scriptEngine.eval(script);
			}

			result = postProcess(result, this.scriptEngine, script, bindings);

			if (this.logger.isDebugEnabled()) {
				this.logger.debug("script executed in " + (new Date().getTime() - start.getTime()) + " ms");
			}
		}

		catch (Exception e) {
			throw new ScriptingException(e.getMessage(), e);
		}

		return result;
	}

	/**
	 * Subclasses may implement this to provide any special handling required
	 * @param result the result.
	 * @param scriptEngine the engine.
	 * @param script the script.
	 * @param bindings the bindings.
	 * @return modified result
	 */
	protected abstract Object postProcess(Object result, ScriptEngine scriptEngine, String script, Bindings bindings);

	private static String invalidLanguageMessage(String language) {
		return ScriptEngineManager.class.getName() +
				" is unable to create a script engine for language '" + language + "'.\n" +
				"This may be due to a missing language implementation or an invalid language name.";
	}

}
