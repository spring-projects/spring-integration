/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.integration.scripting.ScriptExecutor;
import org.springframework.integration.scripting.ScriptingException;
import org.springframework.scripting.ScriptSource;
import org.springframework.util.Assert;

/**
 * Base Class for {@link ScriptExecutor}.
 * <p>
 * Per JSR-223 ({@link javax.script.ScriptEngineFactory#getParameter(String)}),
 * a {@code null} "THREADING" parameter means the engine must not be used
 * to evaluate scripts concurrently from multiple threads.
 * Such an engine is guarded with {@code synchronized} in {@link #executeScript(ScriptSource, Map)}.
 *
 * @author David Turanski
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 * @author Ngoc Nhan
 *
 * @since 2.1
 */
public abstract class AbstractScriptExecutor implements ScriptExecutor {

	protected final Log logger = LogFactory.getLog(getClass()); // NOSONAR - final

	private final ScriptEngine scriptEngine;

	private final boolean threadSafeEngine;

	protected AbstractScriptExecutor(String language) {
		Assert.hasText(language, "language must not be empty");
		this.scriptEngine = new ScriptEngineManager().getEngineByName(language);
		Assert.notNull(this.scriptEngine, () -> invalidLanguageMessage(language));
		this.threadSafeEngine = isThreadSafe(this.scriptEngine);
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("Using script engine : " + this.scriptEngine.getFactory().getEngineName());
		}
	}

	protected AbstractScriptExecutor(ScriptEngine scriptEngine) {
		Assert.notNull(scriptEngine, "'scriptEngine' must not be null.");
		this.scriptEngine = scriptEngine;
		this.threadSafeEngine = isThreadSafe(scriptEngine);
	}

	public ScriptEngine getScriptEngine() {
		return this.scriptEngine;
	}

	@Override
	public @Nullable Object executeScript(ScriptSource scriptSource, @Nullable Map<String, Object> variables) {
		try {
			Object result;
			String script = scriptSource.getScriptAsString();
			Date start = new Date();
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("executing script: " + script);
			}

			Bindings bindings = null;
			if (variables != null && !variables.isEmpty()) {
				bindings = new SimpleBindings(variables);
			}

			if (this.threadSafeEngine) {
				result = eval(script, bindings);
			}
			else {
				synchronized (this.scriptEngine) {
					result = eval(script, bindings);
				}
			}

			result = postProcess(result, this.scriptEngine, script, bindings);

			if (this.logger.isDebugEnabled()) {
				this.logger.debug("script executed in " + (new Date().getTime() - start.getTime()) + " ms");
			}
			return result;
		}
		catch (Exception e) {
			throw new ScriptingException(e.getMessage(), e);
		}
	}

	private Object eval(String script, @Nullable Bindings bindings) throws ScriptException {
		return bindings != null ? this.scriptEngine.eval(script, bindings) : this.scriptEngine.eval(script);
	}

	/**
	 * Subclasses may implement this to provide any special handling required
	 * @param result the result.
	 * @param scriptEngine the engine.
	 * @param script the script.
	 * @param bindings the bindings.
	 * @return modified result
	 */
	protected abstract Object postProcess(Object result, ScriptEngine scriptEngine, String script, @Nullable Bindings bindings);

	private static String invalidLanguageMessage(String language) {
		return ScriptEngineManager.class.getName() +
				" is unable to create a script engine for language '" + language + "'.\n" +
				"This may be due to a missing language implementation or an invalid language name.";
	}

	private static boolean isThreadSafe(ScriptEngine scriptEngine) {
		return scriptEngine.getFactory().getParameter("THREADING") != null;
	}

}
