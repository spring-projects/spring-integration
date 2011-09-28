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

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import javax.script.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.scripting.ScriptExecutor;
import org.springframework.scripting.ScriptSource;
import org.springframework.util.Assert;

/**
 * Executes Jsr223 scripts
 * 
 * @author David Turanski
 * @since 2.1
 */
public class DefaultScriptExecutor implements ScriptExecutor {
	private static Log log = LogFactory.getLog(DefaultScriptExecutor.class);

	// private ScriptEngine scriptEngine;
	private final String language;

	private ScriptEngineManager manager = new ScriptEngineManager();

	/**
	 * Set the engine name (language)
	 * @param language
	 */
	public DefaultScriptExecutor(String language) {
		this.language = language;
		// ScriptEngineManager manager = new ScriptEngineManager();
		// if (log.isDebugEnabled()){
		// for (ScriptEngineFactory factory: manager.getEngineFactories()) {
		// log.debug(factory.getNames());
		// }
		// }
		//
		// scriptEngine = manager.getEngineByName(language);
		// Assert.notNull(scriptEngine,"JVM cannot create a script engine for name ["
		// + language + "]");
		//
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.dturanski.test.jruby.ScriptExecutor#executeScript(org.springframework
	 * .scripting.ScriptSource)
	 */
	public Object executeScript(ScriptSource scriptSource) {
		return this.executeScript(scriptSource, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.dturanski.test.jruby.ScriptExecutor#bindVariable(java.lang.String,
	 * java.lang.Object)
	 */
	// private void bindVariable(String variableName, Object value) {
	// scriptEngine.put(variableName, value);
	// }

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.integration.jsr233.ScriptExecutor#executeScript(org
	 * .springframework.scripting.ScriptSource, java.util.Map)
	 */
	public synchronized Object executeScript(ScriptSource scriptSource, Map<String, Object> variables) {
		Assert.notNull(scriptSource, "scriptSource must not be null");

		Object obj = null;
		try {
			ScriptEngine scriptEngine = manager.getEngineByName(language);

			Bindings bindings = scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE);

			if (variables != null) {
				for (Entry<String, Object> entry : variables.entrySet()) {
					bindings.put(entry.getKey(), entry.getValue());
				}
			}

			String script = scriptSource.getScriptAsString();
			if (log.isDebugEnabled()) {
				Thread.currentThread().setName(Thread.currentThread().getName().replace("scheduler", "script"));
				log.debug(Long.toHexString(Thread.currentThread().getId()) + " executing script: " + script);
			}
			obj = scriptEngine.eval(script, bindings);

			if (log.isDebugEnabled()) {
				log.debug(Long.toHexString(Thread.currentThread().getId()) + " returned from eval: ");
			}

		}
		catch (Throwable t) {
			t.printStackTrace();
			throw new RuntimeException(t);
		}

		// catch (ScriptException e) {
		// throw new RuntimeException(e);
		// }
		// catch (IOException e) {
		// throw new RuntimeException(e);
		// }

		return obj;
	}

}
